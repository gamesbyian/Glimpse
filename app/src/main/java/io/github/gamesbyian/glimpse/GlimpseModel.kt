package io.github.gamesbyian.glimpse

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gamesbyian.glimpse.data.CollectionZipRestorer
import io.github.gamesbyian.glimpse.data.CollectionZipWriter
import io.github.gamesbyian.glimpse.data.CollectionOverview
import io.github.gamesbyian.glimpse.data.CollectionRecord
import io.github.gamesbyian.glimpse.data.GlimpseDatabase
import io.github.gamesbyian.glimpse.data.ImportResult
import io.github.gamesbyian.glimpse.data.inspectRegisteredOriginals
import io.github.gamesbyian.glimpse.data.LocalOriginalIntegritySummary
import io.github.gamesbyian.glimpse.data.PhotoImporter
import io.github.gamesbyian.glimpse.domain.PhotoEditGeometry
import io.github.gamesbyian.glimpse.data.verifyPrivateOriginal
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.RotationRecord
import io.github.gamesbyian.glimpse.domain.WallpaperCommands
import io.github.gamesbyian.glimpse.domain.BrowseCursor
import io.github.gamesbyian.glimpse.domain.CollectionRemovalSafety
import io.github.gamesbyian.glimpse.domain.checkedCollectionTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
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
    val retryableFailures: Int = 0,
    val message: String = "Create a collection to begin."
) {
    val current: PhotoRecord?
        get() = photos.firstOrNull { it.id == selectedPhotoId } ?: photos.firstOrNull()
}

/** Defend against asynchronous Room emissions when Android's acknowledged ID changes. */
internal fun matchedAcknowledgedPhoto(
    rotation: RotationRecord?,
    candidate: PhotoRecord?
): PhotoRecord? = candidate?.takeIf { it.id == rotation?.lastAcknowledgedPhotoId }

@OptIn(ExperimentalCoroutinesApi::class)
class GlimpseModel(application: Application) : AndroidViewModel(application) {
    private val dao = GlimpseDatabase.open(application).dao()
    private val importer = PhotoImporter(application, dao)
    private val wallpaper = WallpaperCommands(application, dao)
    private val zipRestorer = CollectionZipRestorer(application, dao)

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
    // The picker grant is ephemeral. Keep failed sources only in ViewModel RAM;
    // never SharedPreferences, Room, import journal, diagnostics or logcat.
    private data class PendingRetry(val collectionId: String, val uris: List<Uri>)
    private val pendingRetry = MutableStateFlow<PendingRetry?>(null)

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
        val provenPhoto = matchedAcknowledgedPhoto(rotation, acknowledged)
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
    val ui: StateFlow<GlimpseUiState> = combine(catalog, grid, busy, notice, pendingRetry) {
        data, isGrid, isBusy, message, pending ->
        data.copy(
            grid = isGrid,
            busy = isBusy,
            message = message,
            retryableFailures = if (pending != null &&
                data.collectionId == pending.collectionId) pending.uris.size else 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GlimpseUiState())

    init {
        // An Android process kill can bypass PhotoImporter's finally block.
        // Reclaim *only* day-old, importer-named .partial files off the UI
        // thread. SHA originals and provider grants are never touched.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                importer.pruneStalePartialImports()
                val unfinished = importer.reconcileInterruptedImports()
                if (unfinished > 0) {
                    notice.value = "An earlier import did not record completion for " +
                        unfinished + " photo(s). Check your collection; re-select any " +
                        "missing photos using Add photos. Existing copies are preserved."
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // Noncritical housekeeping cannot block gallery startup.
            }
        }
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

    /**
     * Explicit user-triggered exact-byte audit. Never repairs, deletes, logs
     * file paths or stores photo identifiers in support diagnostics.
     */
    suspend fun auditPrivateOriginals(): LocalOriginalIntegritySummary =
        withContext(Dispatchers.IO) {
            val originals = java.io.File(getApplication<Application>().filesDir, "originals")
            val registered = dao.allPhotosForAudit()
            val checkContext = coroutineContext
            inspectRegisteredOriginals(originals, registered) {
                checkContext.ensureActive()
            }
        }

    /**
     * Explicit SAF export. Does not promise restore, cloud synchronization,
     * or transactionality across an external document provider.
     */
    fun exportCollectionZip(uri: Uri, requestedCollectionId: String?) = action {
        val id = requestedCollectionId ?: error(
            "The original export collection was lost. Choose Export again."
        )
        require(uri.scheme == "content") { "Android did not provide a document destination." }
        try {
            val summary = withContext(Dispatchers.IO) {
                val collection = dao.findCollection(id)
                    ?: error("The collection is no longer available for export.")
                val records = dao.photosIn(id)
                val destination = getApplication<Application>()
                    .contentResolver.openOutputStream(uri, "wt")
                    ?: error("Android could not open the selected export destination.")
                destination.use { output ->
                    val exportContext = coroutineContext
                    CollectionZipWriter.write(
                        output, java.io.File(getApplication<Application>().filesDir, "originals"),
                        collection.title, records
                    ) { exportContext.ensureActive() }
                }
            }
            "Android accepted a ZIP with ${summary.count} original photos " +
                "(${summary.bytes} uncompressed image bytes). Verify the chosen " +
                "destination. Use Restore collection ZIP in Help & privacy to import " +
                "these photos into a new collection. Shuffle and wallpaper settings " +
                "are not included."
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            "ZIP export failed or was interrupted. A partial destination file may " +
                "exist; delete it using its Android document provider. Original " +
                "photos in Glimpse were not changed."
        }
    }

    /**
     * The system document chooser grants one archive temporarily. Once every
     * original has been validated, a distinct new collection is committed.
     * No wallpaper or active rotation is changed by restoration.
     */
    fun restoreCollectionZip(uri: Uri) = action {
        val result = zipRestorer.restore(uri)
        browse.value = browse.value.open(result.collectionId, null)
        browsePrefs.edit().putString("active_collection", result.collectionId).apply()
        "Restored " + result.count + " original photos into a new collection, " +
            result.title + ". Existing collections and wallpaper were unchanged."
    }

    fun createCollection(name: String) = action {
        val title = checkedCollectionTitle(name)
        val id = UUID.randomUUID().toString()
        withContext(Dispatchers.IO) {
            dao.insertCollection(CollectionRecord(id, title, System.currentTimeMillis()))
        }
        browse.value = browse.value.open(id, null)
        browsePrefs.edit().putString("active_collection", id).apply()
        "Created " + title + ". Use Add photos to import."
    }

    fun renameCollection(id: String?, proposedName: String) = action {
        val target = id ?: error("Choose a collection to rename.")
        val title = checkedCollectionTitle(proposedName)
        val changed = withContext(Dispatchers.IO) {
            dao.renameCollection(target, title)
        }
        require(changed == 1) {
            "The original collection is unavailable. Choose a collection again."
        }
        // Stable CollectionId and current photo selection are unchanged.
        // Active rotation uses that same ID and follows the renamed title.
        "Collection renamed to " + title + ". Wallpapers and photo order were unchanged."
    }

    /**
     * Remove only a redundant collection container, never a user's last copy
     * of an image. This also refuses to sever the saved shuffle source.
     */
    fun removeCollectionOnly(id: String) = action {
        val (decision, nextId) = withContext(Dispatchers.IO) {
            val result = dao.removeCollectionIfRedundant(id)
            result to if (result == CollectionRemovalSafety.SAFE)
                dao.watchCollections().firstOrNull().orEmpty().firstOrNull()?.id
            else null
        }
        when (decision) {
            CollectionRemovalSafety.NOT_FOUND -> "This collection was already removed."
            CollectionRemovalSafety.HAS_EXCLUSIVE_ORIGINALS ->
                "Cannot remove this collection: some originals belong only here. " +
                    "First add those photos to another collection."
            CollectionRemovalSafety.OWNS_ROTATION ->
                "This is the saved shuffle collection. Choose another shuffle " +
                    "collection before removing it."
            CollectionRemovalSafety.UNCERTAIN_WALLPAPER ->
                "Review the uncertain wallpaper attempt before removing collections."
            CollectionRemovalSafety.SAFE -> {
                browsePrefs.edit().remove("photo:$id")
                    .putString("active_collection", nextId).apply()
                browse.value = browse.value.open(nextId, nextId?.let(::rememberedPhoto))
                "Removed the redundant collection. Every original remains " +
                    "accessible elsewhere; no wallpaper or shuffle was changed."
            }
        }
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

    private fun importNotice(report: ImportResult): String =
        "Selected " + report.selected + "; imported " + report.imported +
            "; duplicates " + report.duplicates + "; failed " + report.failures + "." +
            (if (report.failedSources.isNotEmpty())
                " Retry failed photos while Glimpse stays open, or reselect them " +
                    "if the provider no longer allows access."
            else "") +
            (if (report.journalWarnings > 0)
                " Import history could not be fully saved (" + report.journalWarnings +
                    " events); existing imported photos remain available."
            else "")

    /**
     * The destination is captured *before* launching Android Photo Picker and
     * restored with the Activity. Never substitute whichever collection is
     * currently being browsed when the external provider returns.
     */
    fun importSelected(uris: List<Uri>, pickerCollectionId: String?) {
        if (uris.isEmpty()) return
        action {
            val id = pickerCollectionId ?: error(
                "Photo selection destination was lost. Choose a collection and Add photos again."
            )
            require(withContext(Dispatchers.IO) { dao.collectionExists(id) }) {
                "The originally selected collection is unavailable. Select photos again."
            }
            val report = importer.import(uris, id)
            pendingRetry.value = report.failedSources.takeIf { it.isNotEmpty() }
                ?.let { PendingRetry(id, it) }
            val prefix = if (id != browse.value.collectionId)
                "Photos were imported into the original collection, not the one now shown. "
            else ""
            prefix + importNotice(report)
        }
    }

    /** Only retries the previously failed provider selections in RAM.
     * A revoked grant stays failed and the user must reselect through Picker. */
    fun retryFailedImports() = action {
        val pending = pendingRetry.value ?: error("No failed imports to retry.")
        require(pending.collectionId == browse.value.collectionId) {
            "Return to the original collection to retry failed imports."
        }
        val report = importer.import(pending.uris, pending.collectionId)
        pendingRetry.value = report.failedSources.takeIf { it.isNotEmpty() }
            ?.let { PendingRetry(pending.collectionId, it) }
        importNotice(report)
    }

    /**
     * Intentionally a non-destructive membership copy. A different collection
     * acquires this verified original; wallpaper/shuffle and selection do not move.
     */
    fun addPhotoToCollection(photoId: String, sourceId: String, targetId: String) = action {
        val targetName = withContext(Dispatchers.IO) {
            val target = dao.watchCollections().firstOrNull()
                ?.firstOrNull { it.id == targetId }
                ?: error("The destination collection is no longer available.")
            val original = dao.findPhoto(photoId)
                ?: error("The selected photo is no longer available.")
            // Do not propagate a damaged/missing source to another collection.
            verifyPrivateOriginal(
                java.io.File(getApplication<Application>().filesDir, "originals"), original
            )
            val result = dao.copyMembership(sourceId, targetId, photoId)
            when (result) {
                -1 -> error("The source photo or destination is no longer available.")
                0 -> "This photo is already in " + target.title + "."
                else -> "Added photo to " + target.title +
                    ". Original and current wallpaper were unchanged."
            }
        }
        targetName
    }

    /**
     * Non-destructive collection unlink: never strand the original with zero
     * memberships, and never touch Home wallpaper or rotation state.
     */
    fun removePhotoFromCollection(photoId: String, collectionId: String) = action {
        require(browse.value.collectionId == collectionId) {
            "You changed collections. Open the original collection and try again."
        }
        val removed = withContext(Dispatchers.IO) {
            dao.detachOnlyIfElsewhere(collectionId, photoId)
        }
        if (!removed) {
            "Cannot remove this photo here. It must also belong to another " +
                "collection to keep the original accessible."
        } else {
            "Removed from this collection. The intact original stays in another " +
                "collection. Home wallpaper and shuffle settings were not changed."
        }
    }

    /**
     * Baked edited PNG becomes an ordinary additional offline photo, never
     * mutating the parent original. Focus follows the newly attached variant.
     */
    fun saveEditedToCollection(
        original: PhotoRecord, collectionId: String, geometry: PhotoEditGeometry,
        setHomeNow: Boolean = false
    ) = action {
        require(browse.value.collectionId == collectionId) {
            "Return to the original collection before saving the edit."
        }
        val imported = EditedPhotoOutput.addEditedVersion(
            getApplication(), dao, original, geometry, collectionId
        )
        val ids = ui.value.photos.map { it.id } + imported.record.id
        val changed = browse.value.select(collectionId, imported.record.id, ids)
        browse.value = changed
        retainPhoto(changed)
        val saveMessage = if (imported.newlyAttached)
            "Saved a separate edited PNG in this collection. The untouched " +
                "original is still available."
        else
            "This edited PNG already belongs here; the saved copy was selected."
        if (setHomeNow) {
            // The existing serialized gateway owns the only OS side effect.
            // If Android cannot confirm, its pending token safeguards rotation.
            saveMessage + " " + wallpaper.applySelected(imported.record.id)
        } else {
            saveMessage + " Set Home wallpaper now to apply this edited photo."
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
