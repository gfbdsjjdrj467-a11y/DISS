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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.game.model.GameModeType
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
    val totalPlayTimeSec: Int = 0,
    val rankPoints: Int = 0,
    val peakRankPoints: Int = 0,
    val rankedMatches: Int = 0,
    val rankedWins: Int = 0
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
    val durationSec: Int,
    val rankDelta: Int = 0
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
    version = 2,
    exportSchema = false
)
abstract class StrikeArenaDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile
        private var INSTANCE: StrikeArenaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE career_stats ADD COLUMN rankPoints INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE career_stats ADD COLUMN peakRankPoints INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE career_stats ADD COLUMN rankedMatches INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE career_stats ADD COLUMN rankedWins INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE match_history ADD COLUMN rankDelta INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): StrikeArenaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StrikeArenaDatabase::class.java,
                    "strike_arena_stats.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
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
        val isRanked = summary.mode == GameModeType.RANKED_2V2
        val newRp = if (isRanked) (current.rankPoints + summary.rankDelta).coerceAtLeast(0) else current.rankPoints
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
            totalPlayTimeSec = current.totalPlayTimeSec + summary.durationSec,
            rankPoints = newRp,
            peakRankPoints = maxOf(current.peakRankPoints, newRp),
            rankedMatches = current.rankedMatches + if (isRanked) 1 else 0,
            rankedWins = current.rankedWins + if (isRanked && summary.victory) 1 else 0
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
                durationSec = summary.durationSec,
                rankDelta = if (isRanked) summary.rankDelta else 0
            )
        )
    }

    suspend fun resetAllStats() {
        statsDao.clearMatchHistory()
        statsDao.clearCareerStats()
    }
}
