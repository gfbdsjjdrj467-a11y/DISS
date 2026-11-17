package com.example.data

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
import com.example.game.model.MatchResultSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "career_stats")
data class CareerStatsEntity(
    @PrimaryKey val id: Int = 1,
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalKills: Int = 0,
    val totalDeaths: Int = 0,
    val totalHeadshots: Int = 0,
    val totalShotsFired: Int = 0,
    val totalShotsHit: Int = 0,
    val bestScore: Int = 0,
    val highestSurvivalWave: Int = 0,
    val totalPlayTimeSec: Int = 0
) {
    val kdRatio: Float
        get() = if (totalDeaths > 0) totalKills.toFloat() / totalDeaths else totalKills.toFloat()

    val accuracyPercent: Float
        get() = if (totalShotsFired > 0) (totalShotsHit.toFloat() / totalShotsFired) * 100f else 0f

    val winRatePercent: Float
        get() = if (matchesPlayed > 0) (wins.toFloat() / matchesPlayed) * 100f else 0f
}

@Entity(tableName = "match_history")
data class MatchRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val mapName: String,
    val modeName: String,
    val victory: Boolean,
    val kills: Int,
    val deaths: Int,
    val accuracyPercent: Float,
    val score: Int,
    val wavesCleared: Int,
    val durationSec: Int
)

@Dao
interface StatsDao {
    @Query("SELECT * FROM career_stats WHERE id = 1")
    fun observeCareerStats(): Flow<CareerStatsEntity?>

    @Query("SELECT * FROM career_stats WHERE id = 1")
    suspend fun getCareerStatsOnce(): CareerStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCareerStats(stats: CareerStatsEntity)

    @Query("SELECT * FROM match_history ORDER BY timestampMs DESC LIMIT 30")
    fun observeMatchHistory(): Flow<List<MatchRecordEntity>>

    @Insert
    suspend fun insertMatchRecord(record: MatchRecordEntity)

    @Query("DELETE FROM match_history")
    suspend fun clearMatchHistory()

    @Query("DELETE FROM career_stats")
    suspend fun clearCareerStats()
}

@Database(
    entities = [CareerStatsEntity::class, MatchRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StrikeArenaDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile
        private var INSTANCE: StrikeArenaDatabase? = null

        fun getInstance(context: Context): StrikeArenaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StrikeArenaDatabase::class.java,
                    "strike_arena_stats.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class StatsRepository(private val statsDao: StatsDao) {
    val careerStats: Flow<CareerStatsEntity> = statsDao.observeCareerStats().map {
        it ?: CareerStatsEntity()
    }

    val matchHistory: Flow<List<MatchRecordEntity>> = statsDao.observeMatchHistory()

    suspend fun recordMatchResult(summary: MatchResultSummary) {
        val current = statsDao.getCareerStatsOnce() ?: CareerStatsEntity()
        val matchScore = summary.playerKills * 100 + summary.headshots * 35 + summary.wavesCleared * 150
        val updated = current.copy(
            matchesPlayed = current.matchesPlayed + 1,
            wins = current.wins + if (summary.victory) 1 else 0,
            losses = current.losses + if (!summary.victory) 1 else 0,
            totalKills = current.totalKills + summary.playerKills,
            totalDeaths = current.totalDeaths + summary.playerDeaths,
            totalHeadshots = current.totalHeadshots + summary.headshots,
            totalShotsFired = current.totalShotsFired + summary.shotsFired,
            totalShotsHit = current.totalShotsHit + summary.shotsHit,
            bestScore = maxOf(current.bestScore, matchScore),
            highestSurvivalWave = maxOf(current.highestSurvivalWave, summary.wavesCleared),
            totalPlayTimeSec = current.totalPlayTimeSec + summary.durationSec
        )
        statsDao.upsertCareerStats(updated)
        statsDao.insertMatchRecord(
            MatchRecordEntity(
                mapName = summary.mapId.displayName,
                modeName = summary.mode.displayName,
                victory = summary.victory,
                kills = summary.playerKills,
                deaths = summary.playerDeaths,
                accuracyPercent = summary.accuracyPercent,
                score = matchScore,
                wavesCleared = summary.wavesCleared,
                durationSec = summary.durationSec
            )
        )
    }

    suspend fun resetAllStats() {
        statsDao.clearMatchHistory()
        statsDao.clearCareerStats()
    }
}
