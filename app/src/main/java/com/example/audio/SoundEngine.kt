package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import com.example.game.model.GameSettings
import com.example.game.model.SurfaceType
import com.example.game.model.WeaponId
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(16)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = HashMap<String, Int>()
    private var loaded = false

    @Volatile private var masterVol = 0.85f
    @Volatile private var musicVol = 0.45f
    @Volatile private var sfxVol = 0.90f
    @Volatile private var uiVol = 0.75f

    @Volatile private var musicRunning = false
    @Volatile private var musicPlaying = false
    private var musicThread: Thread? = null

    init {
        thread(name = "SoundSynthInit") {
            try {
                val cacheDir = File(context.cacheDir, "synth_sfx").apply { mkdirs() }
                loadSynthSound(cacheDir, "shot_pistol", synthGunshot(0.14f, 190f, 95f, 0.75f))
                loadSynthSound(cacheDir, "shot_ar", synthGunshot(0.16f, 165f, 70f, 0.85f))
                loadSynthSound(cacheDir, "shot_smg", synthGunshot(0.10f, 240f, 110f, 0.70f))
                loadSynthSound(cacheDir, "shot_shotgun", synthGunshot(0.28f, 110f, 45f, 1.0f))
                loadSynthSound(cacheDir, "shot_sniper", synthGunshot(0.36f, 130f, 38f, 1.0f))
                loadSynthSound(cacheDir, "shot_lmg", synthGunshot(0.18f, 145f, 60f, 0.92f))
                loadSynthSound(cacheDir, "reload", synthClickSequence(listOf(0.0f to 620f, 0.16f to 880f)))
                loadSynthSound(cacheDir, "empty", synthTone(0.06f, 1100f, 0.4f, 35f))
                loadSynthSound(cacheDir, "hit", synthTone(0.05f, 1450f, 0.55f, 42f))
                loadSynthSound(cacheDir, "headshot", synthClickSequence(listOf(0.0f to 1760f, 0.045f to 2340f)))
                loadSynthSound(cacheDir, "hurt", synthGunshot(0.15f, 95f, 55f, 0.65f))
                loadSynthSound(cacheDir, "step_sand", synthNoiseBurst(0.09f, 420f, 0.35f))
                loadSynthSound(cacheDir, "step_concrete", synthTone(0.07f, 190f, 0.35f, 32f))
                loadSynthSound(cacheDir, "step_metal", synthTone(0.11f, 520f, 0.40f, 22f))
                loadSynthSound(cacheDir, "step_snow", synthNoiseBurst(0.11f, 880f, 0.32f))
                loadSynthSound(cacheDir, "door", synthTone(0.24f, 160f, 0.45f, 10f))
                loadSynthSound(cacheDir, "pickup", synthClickSequence(listOf(0.0f to 587f, 0.08f to 880f)))
                loadSynthSound(cacheDir, "ui_click", synthTone(0.04f, 980f, 0.35f, 48f))
                loadSynthSound(cacheDir, "victory", synthClickSequence(listOf(0.0f to 523f, 0.12f to 659f, 0.24f to 784f)))
                loadSynthSound(cacheDir, "defeat", synthClickSequence(listOf(0.0f to 330f, 0.16f to 262f, 0.32f to 196f)))
                loaded = true
            } catch (_: Throwable) {
            }
        }
    }

    fun applySettings(settings: GameSettings) {
        masterVol = settings.masterVolume.coerceIn(0f, 1f)
        musicVol = settings.musicVolume.coerceIn(0f, 1f)
        sfxVol = settings.sfxVolume.coerceIn(0f, 1f)
        uiVol = settings.uiVolume.coerceIn(0f, 1f)
    }

    fun startMenuMusic() {
        if (musicRunning) {
            musicPlaying = true
            return
        }
        musicRunning = true
        musicPlaying = true
        musicThread = thread(name = "TacticalMusicLoop") {
            val sampleRate = 22050
            val bufSamples = 2048
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufSamples * 4)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return@thread

            val pcm = ShortArray(bufSamples)
            var phase = 0.0
            var lfoPhase = 0.0
            runCatching { track.play() }

            while (musicRunning) {
                val vol = if (musicPlaying) (masterVol * musicVol * 0.22f) else 0f
                for (i in 0 until bufSamples) {
                    val bassFreq = if (((lfoPhase / (2 * PI)) % 4.0) < 2.0) 55.0 else 65.41
                    val sub = sin(phase) * 0.55 + sin(phase * 2.0) * 0.25 + sin(phase * 1.498) * 0.15
                    val pulse = 0.65 + 0.35 * sin(lfoPhase * 2.0)
                    pcm[i] = (sub * pulse * vol * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
                    phase += 2.0 * PI * bassFreq / sampleRate
                    lfoPhase += 2.0 * PI * 0.45 / sampleRate
                    if (phase > 1e6) phase -= 1e6
                }
                runCatching { track.write(pcm, 0, bufSamples) }
            }
            runCatching {
                track.stop()
                track.release()
            }
        }
    }

    fun setMenuMusicPaused(paused: Boolean) {
        musicPlaying = !paused
    }

    fun playWeaponShot(weaponId: WeaponId, volumeScale: Float = 1.0f) {
        val key = when (weaponId) {
            WeaponId.PISTOL_P9 -> "shot_pistol"
            WeaponId.AR_M4A1 -> "shot_ar"
            WeaponId.SMG_MPX -> "shot_smg"
            WeaponId.SHOTGUN_M870 -> "shot_shotgun"
            WeaponId.SNIPER_AWP -> "shot_sniper"
            WeaponId.LMG_M249 -> "shot_lmg"
        }
        playSfx(key, volumeScale)
    }

    fun playReload() = playSfx("reload", 0.85f)
    fun playEmptyClick() = playSfx("empty", 0.7f)
    fun playHitmarker(headshot: Boolean) = playSfx(if (headshot) "headshot" else "hit", 0.9f)
    fun playHurt() = playSfx("hurt", 0.85f)
    fun playDoor() = playSfx("door", 0.75f)
    fun playPickup() = playSfx("pickup", 0.85f)
    fun playVictory() = playSfx("victory", 0.95f)
    fun playDefeat() = playSfx("defeat", 0.95f)
    fun playUiClick() = playUi("ui_click", 0.65f)

    fun playFootstep(surface: SurfaceType) {
        val key = when (surface) {
            SurfaceType.SAND -> "step_sand"
            SurfaceType.CONCRETE -> "step_concrete"
            SurfaceType.METAL -> "step_metal"
            SurfaceType.SNOW -> "step_snow"
            SurfaceType.WOOD -> "step_concrete"
        }
        playSfx(key, 0.48f)
    }

    private fun playSfx(key: String, scale: Float) {
        if (!loaded) return
        val id = soundIds[key] ?: return
        val vol = (masterVol * sfxVol * scale).coerceIn(0f, 1f)
        if (vol > 0.01f) {
            soundPool.play(id, vol, vol, 1, 0, 1.0f)
        }
    }

    private fun playUi(key: String, scale: Float) {
        if (!loaded) return
        val id = soundIds[key] ?: return
        val vol = (masterVol * uiVol * scale).coerceIn(0f, 1f)
        if (vol > 0.01f) {
            soundPool.play(id, vol, vol, 1, 0, 1.0f)
        }
    }

    fun release() {
        musicRunning = false
        musicPlaying = false
        runCatching { soundPool.release() }
    }

    private fun loadSynthSound(dir: File, name: String, samples: ShortArray) {
        val file = File(dir, "$name.wav")
        writeWav(file, samples, 22050)
        soundIds[name] = soundPool.load(file.absolutePath, 1)
    }

    private fun synthGunshot(durationSec: Float, startFreq: Float, endFreq: Float, gain: Float): ShortArray {
        val sr = 22050
        val count = (sr * durationSec).toInt()
        val out = ShortArray(count)
        val rng = Random(startFreq.toInt())
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / sr
            val norm = i.toFloat() / count
            val freq = startFreq + (endFreq - startFreq) * norm
            phase += 2.0 * PI * freq / sr
            val body = sin(phase) * exp(-t * 18.0)
            val crack = (rng.nextFloat() * 2f - 1f) * exp(-t * 28.0)
            val tail = (rng.nextFloat() * 2f - 1f) * 0.35 * exp(-t * 9.0)
            val v = (body * 0.55 + crack * 0.75 + tail) * gain
            out[i] = (v * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthTone(durationSec: Float, freq: Float, gain: Float, decay: Float): ShortArray {
        val sr = 22050
        val count = (sr * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sr
            val v = sin(2.0 * PI * freq * t) * exp(-t * decay) * gain
            out[i] = (v * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthNoiseBurst(durationSec: Float, centerFreq: Float, gain: Float): ShortArray {
        val sr = 22050
        val count = (sr * durationSec).toInt()
        val out = ShortArray(count)
        val rng = Random(centerFreq.toInt())
        for (i in 0 until count) {
            val t = i.toDouble() / sr
            val env = sin(PI * i / count)
            val tone = sin(2.0 * PI * centerFreq * t) * 0.35
            val noise = (rng.nextFloat() * 2f - 1f) * 0.65
            val v = (tone + noise) * env * gain
            out[i] = (v * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthClickSequence(events: List<Pair<Float, Float>>): ShortArray {
        val sr = 22050
        val totalSec = (events.maxOfOrNull { it.first } ?: 0f) + 0.18f
        val count = (sr * totalSec).toInt()
        val out = ShortArray(count)
        for ((offsetSec, freq) in events) {
            val startIdx = (offsetSec * sr).toInt()
            val len = (0.14f * sr).toInt()
            for (j in 0 until len) {
                val idx = startIdx + j
                if (idx >= count) break
                val t = j.toDouble() / sr
                val v = sin(2.0 * PI * freq * t) * exp(-t * 22.0) * 0.55
                val mixed = out[idx] + (v * 32767.0).toInt()
                out[idx] = mixed.coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }

    private fun writeWav(file: File, samples: ShortArray, sampleRate: Int) {
        val dataSize = samples.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(dataSize)
        }
        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            val dataBuf = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) dataBuf.putShort(s)
            fos.write(dataBuf.array())
        }
    }
}
