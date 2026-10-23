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

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun findPhoto(id: String): PhotoRecord?

    @Query("SELECT * FROM memberships WHERE collectionId = :collectionId AND photoId = :photoId LIMIT 1")
    suspend fun findMembership(collectionId: String, photoId: String): MembershipRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: CollectionRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhoto(photo: PhotoRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMembership(membership: MembershipRecord): Long

    @Transaction
    suspend fun attach(photo: PhotoRecord, collectionId: String): Boolean {
        insertPhoto(photo)
        return insertMembership(MembershipRecord(collectionId, photo.id)) != -1L
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
