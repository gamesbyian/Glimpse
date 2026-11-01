package io.github.gamesbyian.glimpse.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.gamesbyian.glimpse.domain.CollectionRemovalSafety
import io.github.gamesbyian.glimpse.domain.collectionRemovalSafety
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "collections")
data class CollectionRecord(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long
)

@Entity(tableName = "photos", indices = [Index(value = ["sha256"], unique = true)])
data class PhotoRecord(
    @PrimaryKey val id: String,
    val sha256: String,
    val localPath: String,
    val mime: String,
    val width: Int,
    val height: Int,
    val byteLength: Long,
    val importedAt: Long
)

/** Read-only library overview; computed from actual memberships, never guessed by UI. */
data class CollectionOverview(
    val id: String,
    val title: String,
    val photoCount: Int,
    val coverPath: String?
)

@Entity(tableName = "memberships", primaryKeys = ["collectionId", "photoId"])
data class MembershipRecord(val collectionId: String, val photoId: String)

/** A single global flag for each immutable original, never one per membership. */
@Entity(tableName = "photo_favorites")
data class PhotoFavoriteRecord(
    @PrimaryKey val photoId: String,
    val markedAt: Long
)

/** Aggregate referential diagnostics, deliberately containing no paths or IDs.
 * A broken link is never authorization to delete an original or Room row.
 */
data class LibraryRelationshipAudit(
    val membershipsWithoutPhoto: Int,
    val membershipsWithoutCollection: Int,
    val favoritesWithoutPhoto: Int,
    val photosWithoutMembership: Int,
    val rotationCollectionMissing: Boolean,
    val pendingPhotoMissing: Boolean,
    val acknowledgedPhotoMissing: Boolean
) {
    val issueCount: Int get() =
        membershipsWithoutPhoto + membershipsWithoutCollection +
            favoritesWithoutPhoto + photosWithoutMembership +
            (if (rotationCollectionMissing) 1 else 0) +
            (if (pendingPhotoMissing) 1 else 0) +
            (if (acknowledgedPhotoMissing) 1 else 0)
}

@Entity(tableName = "rotation")
data class RotationRecord(
    @PrimaryKey val id: Int = 1,
    val collectionId: String? = null,
    val active: Boolean = false,
    val remainingJson: String = "[]",
    val lastAcknowledgedPhotoId: String? = null,
    val lastAcknowledgedAt: Long? = null,
    val lastAttemptAt: Long? = null,
    val nextEligibleAt: Long? = null,
    val pendingPhotoId: String? = null,
    val outcome: String = "Not started"
)

@Dao
interface GlimpseDao {
    @Query("SELECT photoId FROM photo_favorites ORDER BY photoId")
    fun watchFavoriteIds(): Flow<List<String>>

    @Query("SELECT * FROM photo_favorites WHERE photoId = :photoId LIMIT 1")
    suspend fun findFavorite(photoId: String): PhotoFavoriteRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(record: PhotoFavoriteRecord): Long

    @Query("DELETE FROM photo_favorites WHERE photoId = :photoId")
    suspend fun deleteFavorite(photoId: String): Int

    /** Idempotent serialized transaction. Null means the source is gone. */
    @Transaction
    suspend fun setFavorite(photoId: String, favorite: Boolean): Boolean? {
        if (findPhoto(photoId) == null) return null
        if (favorite) {
            if (findFavorite(photoId) == null) {
                addFavorite(PhotoFavoriteRecord(photoId, System.currentTimeMillis()))
            }
        } else {
            deleteFavorite(photoId)
        }
        return favorite
    }

    @Query("SELECT * FROM collections ORDER BY createdAt, id")
    fun watchCollections(): Flow<List<CollectionRecord>>

    @Query(
        "SELECT p.* FROM photos p INNER JOIN memberships m ON m.photoId = p.id " +
            "WHERE m.collectionId = :collectionId ORDER BY p.importedAt, p.id"
    )
    fun watchPhotos(collectionId: String): Flow<List<PhotoRecord>>

    /**
     * Actual SQL-backed contact-sheet pagination: only requested rows are
     * materialized. All, Recent and Favorites share one stable ordering.
     * Filter changes generate a new Pager, never mutate the real collection
     * or active wallpaper/rotation state.
     */
    @Query(
        "SELECT p.* FROM photos p " +
            "INNER JOIN memberships m ON m.photoId = p.id " +
            "WHERE m.collectionId = :collectionId " +
            "AND (:filter != 1 OR (p.importedAt >= :recentCutoff " +
                "AND p.importedAt <= :nowMillis)) " +
            "AND (:filter != 2 OR EXISTS (" +
            "SELECT 1 FROM photo_favorites f WHERE f.photoId = p.id)) " +
            "ORDER BY p.importedAt, p.id"
    )
    fun pageContactSheet(
        collectionId: String, filter: Int, recentCutoff: Long, nowMillis: Long
    ): PagingSource<Int, PhotoRecord>

    /** Stable selected original, independent of loaded pages or filter. */
    @Query(
        "SELECT p.* FROM photos p " +
            "INNER JOIN memberships m ON m.photoId = p.id " +
            "WHERE m.collectionId = :collectionId " +
            "ORDER BY CASE WHEN p.id = :preferredId THEN 0 ELSE 1 END, " +
            "p.importedAt, p.id LIMIT 1"
    )
    fun watchFocusPhoto(collectionId: String, preferredId: String?): Flow<PhotoRecord?>

    /**
     * SQL count of the selected original's 1-based position in the real
     * collection. This replaces the previous in-memory indexOf(12k photos).
     * Imported-at ties follow the same stable ID secondary order as Pager.
     */
    @Query(
        "SELECT COUNT(*) FROM photos p " +
            "INNER JOIN memberships m ON m.photoId = p.id " +
            "WHERE m.collectionId = :collectionId " +
            "AND (p.importedAt < :selectedImportedAt OR " +
            "(p.importedAt = :selectedImportedAt AND p.id <= :selectedId))"
    )
    fun watchPhotoPosition(
        collectionId: String, selectedImportedAt: Long, selectedId: String
    ): Flow<Int>

    /**
     * Future Reel cursor virtualization: a bounded real-photo window, never
     * fake repeated images at collection ends. Uses the exact same ordered
     * membership key as Paging 3 Grid and the SQL focus ordinal.
     *
     * UI integration must take width/offset from ReelWindowPolicy and keep
     * its stable selected photo ID; this read alone changes no live UI.
     */
    @Query(
        "SELECT p.* FROM photos p " +
            "INNER JOIN memberships m ON m.photoId = p.id " +
            "WHERE m.collectionId = :collectionId " +
            "ORDER BY p.importedAt, p.id LIMIT :limit OFFSET :offset"
    )
    suspend fun readReelWindow(
        collectionId: String, offset: Int, limit: Int
    ): List<PhotoRecord>


    @Query(
        "SELECT p.* FROM photos p INNER JOIN memberships m ON m.photoId = p.id " +
            "WHERE m.collectionId = :collectionId ORDER BY p.importedAt, p.id"
    )
    suspend fun photosIn(collectionId: String): List<PhotoRecord>

    @Query(
        "SELECT c.id AS id, c.title AS title, COUNT(m.photoId) AS photoCount, " +
            "(SELECT p.localPath FROM photos p " +
            "JOIN memberships cm ON cm.photoId = p.id " +
            "WHERE cm.collectionId = c.id ORDER BY p.importedAt, p.id LIMIT 1) AS coverPath " +
            "FROM collections c LEFT JOIN memberships m ON m.collectionId = c.id " +
            "GROUP BY c.id, c.title, c.createdAt ORDER BY c.createdAt, c.id"
    )
    fun watchCollectionOverviews(): Flow<List<CollectionOverview>>

    // This photo can belong to another collection. Home must never substitute
    // the currently previewed photo for the last Android-acknowledged one.
    @Query(
        "SELECT p.* FROM photos p INNER JOIN rotation r " +
            "ON r.lastAcknowledgedPhotoId = p.id WHERE r.id = 1 LIMIT 1"
    )
    fun watchAcknowledgedPhoto(): Flow<PhotoRecord?>

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    suspend fun findCollection(id: String): CollectionRecord?

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun findPhoto(id: String): PhotoRecord?

    // Read-only registered originals, independent of overlapping memberships.
    @Query("SELECT * FROM photos ORDER BY id")
    suspend fun allPhotosForAudit(): List<PhotoRecord>

    // Pre-FK schema v2 may contain historical dangling relationships. Count
    // them with bounded SQL aggregates, without downloading every image row.
    @Query(
        "SELECT COUNT(*) FROM memberships m WHERE NOT EXISTS " +
            "(SELECT 1 FROM photos p WHERE p.id = m.photoId)"
    )
    suspend fun membershipsWithoutPhotoCount(): Int

    @Query(
        "SELECT COUNT(*) FROM memberships m WHERE NOT EXISTS " +
            "(SELECT 1 FROM collections c WHERE c.id = m.collectionId)"
    )
    suspend fun membershipsWithoutCollectionCount(): Int

    @Query(
        "SELECT COUNT(*) FROM photo_favorites f WHERE NOT EXISTS " +
            "(SELECT 1 FROM photos p WHERE p.id = f.photoId)"
    )
    suspend fun favoritesWithoutPhotoCount(): Int

    // A registered original can become invisible in all collections while
    // its private bytes still exist. Never infer that it may be deleted.
    @Query(
        "SELECT COUNT(*) FROM photos p WHERE NOT EXISTS " +
            "(SELECT 1 FROM memberships m WHERE m.photoId = p.id)"
    )
    suspend fun photosWithoutMembershipCount(): Int

    /**
     * Read-only, transactional integrity snapshot. Do not repair or delete
     * historical rows: the user may still hold the sole intact SHA original.
     * The three rotation flags only check IDs that are currently populated.
     */
    @Transaction
    suspend fun auditLibraryRelationships(): LibraryRelationshipAudit {
        val rotation = readRotation()
        return LibraryRelationshipAudit(
            membershipsWithoutPhoto = membershipsWithoutPhotoCount(),
            membershipsWithoutCollection = membershipsWithoutCollectionCount(),
            favoritesWithoutPhoto = favoritesWithoutPhotoCount(),
            photosWithoutMembership = photosWithoutMembershipCount(),
            rotationCollectionMissing = rotation?.collectionId?.let {
                !collectionExists(it)
            } ?: false,
            pendingPhotoMissing = rotation?.pendingPhotoId?.let {
                findPhoto(it) == null
            } ?: false,
            acknowledgedPhotoMissing = rotation?.lastAcknowledgedPhotoId?.let {
                findPhoto(it) == null
            } ?: false
        )
    }

    // The Android picker is external to this Activity. Never attach a photo
    // to a collection that disappeared during provider selection/restoration.
    @Query("SELECT EXISTS(SELECT 1 FROM collections WHERE id = :id)")
    suspend fun collectionExists(id: String): Boolean

    @Query("SELECT * FROM memberships WHERE collectionId = :collectionId AND photoId = :photoId LIMIT 1")
    suspend fun findMembership(collectionId: String, photoId: String): MembershipRecord?

    @Query("SELECT COUNT(*) FROM memberships WHERE photoId = :photoId")
    suspend fun membershipCount(photoId: String): Int

    /** Count actual eligible records; historical dangling memberships do not count. */
    @Query(
        "SELECT COUNT(*) FROM memberships m INNER JOIN photos p ON p.id = m.photoId " +
            "WHERE m.collectionId = :collectionId"
    )
    suspend fun widgetSourcePhotoCount(collectionId: String): Int

    /** Only queried when exactly one live source exists, to avoid a futile repeat. */
    @Query(
        "SELECT p.id FROM photos p INNER JOIN memberships m ON m.photoId = p.id " +
            "WHERE m.collectionId = :collectionId LIMIT 1"
    )
    suspend fun widgetSoleSourcePhotoId(collectionId: String): String?

    @Query("DELETE FROM memberships WHERE collectionId = :collectionId AND photoId = :photoId")
    suspend fun deleteMembership(collectionId: String, photoId: String): Int

    /**
     * A membership may be detached only if the original remains accessible
     * through another collection. Never delete PhotoRecord or image bytes.
     */
    @Transaction
    suspend fun detachOnlyIfElsewhere(collectionId: String, photoId: String): Boolean {
        if (findMembership(collectionId, photoId) == null ||
            membershipCount(photoId) < 2) return false
        return deleteMembership(collectionId, photoId) == 1
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: CollectionRecord): Long

    @Query("UPDATE collections SET title = :name WHERE id = :id")
    suspend fun renameCollection(id: String, name: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhoto(photo: PhotoRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMembership(membership: MembershipRecord): Long

    /**
     * Guard selection races in long-running edits: a deleted destination must
     * never receive a new dangling membership.
     */
    @Transaction
    suspend fun attachIfCollectionExists(photo: PhotoRecord, collectionId: String): Boolean? {
        if (!collectionExists(collectionId)) return null
        return attach(photo, collectionId)
    }

    /**
     * A recovery stage must not silently reuse an existing photo ID whose
     * immutable metadata disagrees. Check current Room state and guarded
     * membership in ONE transaction, so another importer cannot insert a
     * conflicting PhotoRecord between the check and attach.
     *
     * Null: vanished collection or conflicting registered photo.
     * True: newly added membership. False: exact membership already exists.
     * No original file or database record is ever replaced or deleted.
     */
    @Transaction
    suspend fun attachRecoveredOriginalIfMatching(
        photo: PhotoRecord, collectionId: String
    ): Boolean? {
        if (!collectionExists(collectionId)) return null
        val known = findPhoto(photo.id)
        if (known != null && (
            known.sha256 != photo.sha256 ||
            known.localPath != photo.localPath ||
            known.mime != photo.mime ||
            known.width != photo.width ||
            known.height != photo.height ||
            known.byteLength != photo.byteLength
        )) return null
        return attach(photo, collectionId)
    }

    @Transaction
    suspend fun attach(photo: PhotoRecord, collectionId: String): Boolean {
        insertPhoto(photo)
        return insertMembership(MembershipRecord(collectionId, photo.id)) != -1L
    }

    /**
     * Link an existing original to another valid collection without copying
     * image bytes or mutating source membership. -1 invalid, 0 duplicate, 1 added.
     * This is atomic even if collection state changes during an external UI.
     */
    @Transaction
    suspend fun copyMembership(
        sourceCollectionId: String, targetCollectionId: String, photoId: String
    ): Int {
        if (sourceCollectionId == targetCollectionId ||
            !collectionExists(targetCollectionId) ||
            findMembership(sourceCollectionId, photoId) == null ||
            findPhoto(photoId) == null
        ) return -1
        return if (insertMembership(MembershipRecord(targetCollectionId, photoId)) == -1L) 0 else 1
    }

    /**
     * The archive is fully validated before this transaction. Either its new
     * collection and every membership commit together, or none commit.
     */
    @Transaction
    suspend fun insertRestoredCollection(
        collection: CollectionRecord, originals: List<PhotoRecord>
    ) {
        require(insertCollection(collection) != -1L) { "New collection ID already exists." }
        for (photo in originals) {
            val known = findPhoto(photo.id)
            require(known == null ||
                (known.sha256 == photo.sha256 && known.mime == photo.mime &&
                    known.byteLength == photo.byteLength && known.localPath == photo.localPath)
            ) { "A stored image record disagrees with the archive." }
            insertPhoto(photo)
            require(insertMembership(MembershipRecord(collection.id, photo.id)) != -1L) {
                "The archive contains a duplicate collection membership."
            }
        }
    }

    @Query(
        "SELECT COUNT(*) FROM memberships m WHERE m.collectionId = :collectionId " +
            "AND NOT EXISTS (SELECT 1 FROM memberships other " +
            "WHERE other.photoId = m.photoId AND other.collectionId != :collectionId)"
    )
    suspend fun exclusiveOriginalCount(collectionId: String): Int

    @Query("DELETE FROM memberships WHERE collectionId = :collectionId")
    suspend fun dropCollectionMemberships(collectionId: String): Int

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun dropCollectionRecord(collectionId: String): Int

    /** Non-destructive: no PhotoRecord, byte deletion or rotation mutation. */
    @Transaction
    suspend fun removeCollectionIfRedundant(collectionId: String): CollectionRemovalSafety {
        val rotation = readRotation()
        val policy = collectionRemovalSafety(
            exists = collectionExists(collectionId),
            exclusiveOriginals = exclusiveOriginalCount(collectionId),
            ownsRotation = rotation?.collectionId == collectionId,
            uncertainWallpaper = rotation?.pendingPhotoId != null
        )
        if (policy != CollectionRemovalSafety.SAFE) return policy
        dropCollectionMemberships(collectionId)
        check(dropCollectionRecord(collectionId) == 1) {
            "Collection disappeared during removal."
        }
        return CollectionRemovalSafety.SAFE
    }

    @Query("SELECT * FROM rotation WHERE id = 1 LIMIT 1")
    fun watchRotation(): Flow<RotationRecord?>

    @Query("SELECT * FROM rotation WHERE id = 1 LIMIT 1")
    suspend fun readRotation(): RotationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRotation(record: RotationRecord)
}

@Database(
    entities = [CollectionRecord::class, PhotoRecord::class, MembershipRecord::class,
        RotationRecord::class, PhotoFavoriteRecord::class],
    version = 2,
    exportSchema = true
)
abstract class GlimpseDatabase : RoomDatabase() {
    abstract fun dao(): GlimpseDao

    companion object {
        /** Upgrade existing owner-installed v1 databases without deleting any photos. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `photo_favorites` (" +
                        "`photoId` TEXT NOT NULL, `markedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`photoId`))"
                )
            }
        }

        @Volatile private var instance: GlimpseDatabase? = null

        fun open(context: Context): GlimpseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GlimpseDatabase::class.java,
                    "glimpse.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
