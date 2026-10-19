package com.kayasetu.asha.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "village_visits")
data class VillageVisitEntity(
    @PrimaryKey val visitId: String,
    val patientName: String,
    val ageGroup: String,
    val age: Int,
    val bodyPart: String,
    val badgeColor: String,
    val riskPercentage: Double,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Dao
interface VillageVisitDao {
    @Query("SELECT * FROM village_visits ORDER BY timestampMillis DESC")
    fun observeAllVisits(): Flow<List<VillageVisitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVisit(visit: VillageVisitEntity)

    @Query("DELETE FROM village_visits WHERE visitId = :id")
    suspend fun deleteVisit(id: String)
}

@Database(entities = [VillageVisitEntity::class], version = 3, exportSchema = false)
abstract class KayaSetuDatabase : RoomDatabase() {
    abstract fun villageVisitDao(): VillageVisitDao

    companion object {
        @Volatile
        private var INSTANCE: KayaSetuDatabase? = null

        fun getInstance(context: Context): KayaSetuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KayaSetuDatabase::class.java,
                    "kayasetu_records_v3.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class VillageVisitRepository(private val dao: VillageVisitDao) {
    val allVisits: Flow<List<VillageVisitEntity>> = dao.observeAllVisits()

    suspend fun saveVisit(visit: VillageVisitEntity) {
        dao.upsertVisit(visit)
    }

    suspend fun deleteVisit(id: String) {
        dao.deleteVisit(id)
    }
}
