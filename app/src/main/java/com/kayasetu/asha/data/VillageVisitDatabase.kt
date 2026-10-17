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
    val sampleId: String,
    val patientName: String,
    val villageName: String,
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

    @Query("SELECT COUNT(*) FROM village_visits")
    suspend fun countVisits(): Int
}

@Database(entities = [VillageVisitEntity::class], version = 1, exportSchema = false)
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
                    "kayasetu_offline_visits.db"
                ).build()
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

    suspend fun seedInitialCoastalVisitsIfEmpty() {
        if (dao.countVisits() == 0) {
            dao.upsertVisit(
                VillageVisitEntity(
                    visitId = "PHC-8A41F920",
                    sampleId = "case_high_scc_ulcer",
                    patientName = "Ramesh Poojary",
                    villageName = "Bantwal PHC",
                    age = 61,
                    bodyPart = "Lower Leg",
                    badgeColor = "RED",
                    riskPercentage = 97.4,
                    timestampMillis = System.currentTimeMillis() - 3600_000L
                )
            )
            dao.upsertVisit(
                VillageVisitEntity(
                    visitId = "PHC-3C92E114",
                    sampleId = "case_mod_infective",
                    patientName = "Lakshmi Salian",
                    villageName = "Ullal PHC",
                    age = 47,
                    bodyPart = "Arm / Hand",
                    badgeColor = "AMBER",
                    riskPercentage = 55.2,
                    timestampMillis = System.currentTimeMillis() - 1800_000L
                )
            )
            dao.upsertVisit(
                VillageVisitEntity(
                    visitId = "PHC-7F10B442",
                    sampleId = "case_low_benign_tinea",
                    patientName = "Karthik Gowda",
                    villageName = "Puttur PHC",
                    age = 22,
                    bodyPart = "Trunk / Back",
                    badgeColor = "GREEN",
                    riskPercentage = 14.8,
                    timestampMillis = System.currentTimeMillis() - 900_000L
                )
            )
        }
    }
}
