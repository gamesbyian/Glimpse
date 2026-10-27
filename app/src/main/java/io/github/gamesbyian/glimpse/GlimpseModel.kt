package io.github.gamesbyian.glimpse

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gamesbyian.glimpse.data.CollectionOverview
import io.github.gamesbyian.glimpse.data.CollectionRecord
import io.github.gamesbyian.glimpse.data.GlimpseDatabase
import io.github.gamesbyian.glimpse.data.PhotoImporter
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.RotationRecord
import io.github.gamesbyian.glimpse.domain.WallpaperCommands
import io.github.gamesbyian.glimpse.domain.BrowseCursor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class GlimpseUiState(
    val collections: List<CollectionRecord> = emptyList(),
    val collectionOverviews: List<CollectionOverview> = emptyList(),
    val acknowledgedPhoto: PhotoRecord? = null,
    val collectionId: String? = null,
    val photos: List<PhotoRecord> = emptyList(),
    val rotation: RotationRecord? = null,
    val selectedPhotoId: String? = null,
    val grid: Boolean = false,
    val busy: Boolean = false,
    val message: String = "Create a collection to begin."
) {
    val current: PhotoRecord?
        get() = photos.firstOrNull { it.id == selectedPhotoId } ?: photos.firstOrNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GlimpseModel(application: Application) : AndroidViewModel(application) {
    private val dao = GlimpseDatabase.open(application).dao()
    private val importer = PhotoImporter(application, dao)
    private val wallpaper = WallpaperCommands(application, dao)

    private val browse = MutableStateFlow(BrowseCursor())
    // Durable per-collection photo IDs; originals, hashes and source paths are
    // deliberately not persisted in preferences.
    private val browsePrefs = application.getSharedPreferences(
        "glimpse_browsing", Context.MODE_PRIVATE
    )
    private fun rememberedPhoto(collection: String): String? =
        browsePrefs.getString("photo:$collection", null)

    private fun retainPhoto(cursor: BrowseCursor) {
        val collection = cursor.collectionId ?: return
        val photo = cursor.selectedPhotoId ?: return
        browsePrefs.edit().putString("photo:$collection", photo).apply()
    }
    private val grid = MutableStateFlow(false)
    private val busy = MutableStateFlow(false)
    private val notice = MutableStateFlow("Create a collection, import photos and choose a wallpaper.")

    private data class ScopedPhotos(val collectionId: String?, val items: List<PhotoRecord>)

    private val visiblePhotos = browse.map { it.collectionId }.distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) flowOf(ScopedPhotos(null, emptyList()))
            else dao.watchPhotos(id).map { ScopedPhotos(id, it) }
        }
    private val wallpaperStatus = combine(
        dao.watchRotation(),
        dao.watchAcknowledgedPhoto()
    ) { rotation, acknowledged -> rotation to acknowledged }

    private val catalog = combine(
        dao.watchCollections(),
        visiblePhotos,
        dao.watchCollectionOverviews(),
        wallpaperStatus,
        browse
    ) { collections, loaded, overviews, wallpaperState, cursor ->
        // A switch must not briefly expose photos from the previous collection.
        val photos = if (loaded.collectionId == cursor.collectionId) loaded.items else emptyList()
        val (rotation, acknowledged) = wallpaperState
        // Room flows can emit independently. Reject transient mismatched pairs.
        val provenPhoto = acknowledged?.takeIf { it.id == rotation?.lastAcknowledgedPhotoId }
        GlimpseUiState(
            collections = collections,
            collectionOverviews = overviews,
            acknowledgedPhoto = provenPhoto,
            collectionId = cursor.collectionId,
            photos = photos,
            rotation = rotation,
            selectedPhotoId = cursor.resolvedId(photos.map { it.id })
        )
    }
    val ui: StateFlow<GlimpseUiState> = combine(catalog, grid, busy, notice) { data, isGrid, isBusy, message ->
        data.copy(grid = isGrid, busy = isBusy, message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GlimpseUiState())

    init {
        viewModelScope.launch {
            val initial = withContext(Dispatchers.IO) {
                val collections = dao.watchCollections().firstOrNull().orEmpty()
                BrowseCursor.restoredCollection(
                    collections.map { it.id },
                    browsePrefs.getString("active_collection", null)
                )
            }
            if (browse.value.collectionId == null) {
                browse.value = browse.value.open(initial, initial?.let(::rememberedPhoto))
            }
        }
    }

    fun createCollection(name: String) = action {
        val title = name.trim()
        require(title.isNotEmpty()) { "Enter a collection name." }
        val id = UUID.randomUUID().toString()
        withContext(Dispatchers.IO) {
            dao.insertCollection(CollectionRecord(id, title, System.currentTimeMillis()))
        }
        browse.value = browse.value.open(id, null)
        browsePrefs.edit().putString("active_collection", id).apply()
        "Created " + title + ". Use Add photos to import."
    }

    fun browseCollection(id: String) {
        if (ui.value.collections.none { it.id == id }) return
        browse.value = browse.value.open(id, rememberedPhoto(id))
        browsePrefs.edit().putString("active_collection", id).apply()
        notice.value = "Browsing this collection does not change the wallpaper."
    }

    fun browsePhotoInCollection(sourceCollectionId: String, id: String) {
        val state = ui.value
        if (state.collectionId != sourceCollectionId) return
        val updated = browse.value.select(sourceCollectionId, id, state.photos.map { it.id })
        if (updated == browse.value) return
        browse.value = updated
        retainPhoto(updated)
        notice.value = "Preview only. Use Set Home wallpaper to apply."
    }

    fun stepPhoto(delta: Int) {
        val state = ui.value
        if (state.collectionId == null || state.collectionId != browse.value.collectionId) return
        val updated = browse.value.step(delta, state.photos.map { it.id })
        if (updated == browse.value) return
        browse.value = updated
        retainPhoto(updated)
        notice.value = "Preview only. No device wallpaper changed."
    }

    fun toggleGrid() { grid.value = !grid.value }

    fun importSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        action {
            val id = browse.value.collectionId ?: error("Create or choose a collection first.")
            val report = importer.import(uris, id)
            "Selected " + report.selected + "; imported " + report.imported +
                "; duplicates " + report.duplicates + "; failed " + report.failures + "."
        }
    }

    fun applyPreview() = action {
        val photo = ui.value.current ?: error("Choose a photo first.")
        wallpaper.applySelected(photo.id)
    }

    fun startRotation() = action {
        val id = browse.value.collectionId ?: error("Choose a collection first.")
        wallpaper.start(id)
    }

    fun pauseRotation() = action { wallpaper.pause() }
    fun changeWallpaperNow() = action { wallpaper.changeNow() }
    fun resolveUncertain() = action { wallpaper.acknowledgeUncertain() }

    suspend fun tickVisible() {
        try { wallpaper.pollWhileVisible() }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { notice.value = "Could not evaluate rotation; no change claimed." }
    }

    private fun action(block: suspend () -> String) {
        viewModelScope.launch {
            if (busy.value) return@launch
            busy.value = true
            try {
                notice.value = block()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                notice.value = failure.message?.take(180) ?: "Action failed."
            } finally {
                busy.value = false
            }
        }
    }
}
