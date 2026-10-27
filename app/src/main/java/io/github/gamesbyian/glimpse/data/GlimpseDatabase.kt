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
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
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
    @Query("SELECT * FROM collections ORDER BY createdAt, id")
    fun watchCollections(): Flow<List<CollectionRecord>>

    @Query(
        "SELECT p.* FROM photos p INNER JOIN memberships m ON m.photoId = p.id " +
            "WHERE m.collectionId = :collectionId ORDER BY p.importedAt, p.id"
    )
    fun watchPhotos(collectionId: String): Flow<List<PhotoRecord>>

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

    // The Android picker is external to this Activity. Never attach a photo
    // to a collection that disappeared during provider selection/restoration.
    @Query("SELECT EXISTS(SELECT 1 FROM collections WHERE id = :id)")
    suspend fun collectionExists(id: String): Boolean

    @Query("SELECT * FROM memberships WHERE collectionId = :collectionId AND photoId = :photoId LIMIT 1")
    suspend fun findMembership(collectionId: String, photoId: String): MembershipRecord?

    @Query("SELECT COUNT(*) FROM memberships WHERE photoId = :photoId")
    suspend fun membershipCount(photoId: String): Int

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

    @Query("SELECT * FROM rotation WHERE id = 1 LIMIT 1")
    fun watchRotation(): Flow<RotationRecord?>

    @Query("SELECT * FROM rotation WHERE id = 1 LIMIT 1")
    suspend fun readRotation(): RotationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRotation(record: RotationRecord)
}

@Database(
    entities = [CollectionRecord::class, PhotoRecord::class, MembershipRecord::class, RotationRecord::class],
    version = 1,
    exportSchema = true
)
abstract class GlimpseDatabase : RoomDatabase() {
    abstract fun dao(): GlimpseDao

    companion object {
        @Volatile private var instance: GlimpseDatabase? = null

        fun open(context: Context): GlimpseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GlimpseDatabase::class.java,
                    "glimpse.db"
                ).build().also { instance = it }
            }
    }
}
