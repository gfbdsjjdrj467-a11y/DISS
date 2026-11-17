package com.example

import android.app.Application
import com.example.audio.SoundEngine
import com.example.data.SettingsRepository
import com.example.data.StatsRepository
import com.example.data.StrikeArenaDatabase

class StrikeArenaApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var statsRepository: StatsRepository
        private set
    lateinit var soundEngine: SoundEngine
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        val db = StrikeArenaDatabase.getInstance(this)
        statsRepository = StatsRepository(db.statsDao())
        soundEngine = SoundEngine(this)
    }

    override fun onTerminate() {
        soundEngine.release()
        super.onTerminate()
    }
}
