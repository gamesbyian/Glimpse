package io.github.gamesbyian.glimpse

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gamesbyian.glimpse.data.CollectionRecord
import io.github.gamesbyian.glimpse.data.GlimpseDatabase
import io.github.gamesbyian.glimpse.data.PhotoImporter
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.RotationRecord
import io.github.gamesbyian.glimpse.domain.WallpaperCommands
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class GlimpseUiState(
    val collections: List<CollectionRecord> = emptyList(),
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

    private val collectionId = MutableStateFlow<String?>(null)
    private val selectedPhoto = MutableStateFlow<String?>(null)
    private val grid = MutableStateFlow(false)
    private val busy = MutableStateFlow(false)
    private val notice = MutableStateFlow("Create a collection, import photos and choose a wallpaper.")

    val activeCollectionId: StateFlow<String?> = collectionId

    private val visiblePhotos = collectionId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.watchPhotos(id)
    }
    private val catalog = combine(
        dao.watchCollections(),
        visiblePhotos,
        dao.watchRotation(),
        selectedPhoto
    ) { collections, photos, rotation, id ->
        GlimpseUiState(
            collections = collections,
            photos = photos,
            rotation = rotation,
            selectedPhotoId = id
        )
    }
    val ui: StateFlow<GlimpseUiState> = combine(catalog, grid, busy, notice) { data, isGrid, isBusy, message ->
        data.copy(grid = isGrid, busy = isBusy, message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GlimpseUiState())

    init {
        viewModelScope.launch {
            val initial = withContext(Dispatchers.IO) {
                dao.watchCollections().firstOrNull()?.firstOrNull()?.id
            }
            if (collectionId.value == null) collectionId.value = initial
        }
    }

    fun createCollection(name: String) = action {
        val title = name.trim()
        require(title.isNotEmpty()) { "Enter a collection name." }
        val id = UUID.randomUUID().toString()
        withContext(Dispatchers.IO) {
            dao.insertCollection(CollectionRecord(id, title, System.currentTimeMillis()))
        }
        collectionId.value = id
        selectedPhoto.value = null
        "Created " + title + ". Use Add photos to import."
    }

    fun browseCollection(id: String) {
        collectionId.value = id
        selectedPhoto.value = null
        notice.value = "Browsing this collection does not change the wallpaper."
    }

    fun browsePhoto(id: String) {
        selectedPhoto.value = id
        notice.value = "Preview only. Use Set Home wallpaper to apply."
    }

    fun stepPhoto(delta: Int) {
        val photos = ui.value.photos
        if (photos.isEmpty()) return
        val position = photos.indexOfFirst { it.id == ui.value.current?.id }.coerceAtLeast(0)
        selectedPhoto.value = photos[(position + delta).coerceIn(0, photos.lastIndex)].id
        notice.value = "Preview only. No device wallpaper changed."
    }

    fun toggleGrid() { grid.value = !grid.value }

    fun importSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        action {
            val id = collectionId.value ?: error("Create or choose a collection first.")
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
        val id = collectionId.value ?: error("Choose a collection first.")
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
