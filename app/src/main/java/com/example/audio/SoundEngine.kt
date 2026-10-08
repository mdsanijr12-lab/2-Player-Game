package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SfxType {
    CLICK,
    COUNTDOWN_TICK,
    COUNTDOWN_GO,
    HIT,
    JUMP,
    SHOOT,
    SCORE,
    GOAL,
    EXPLOSION,
    COLLISION,
    WIN,
    GAME_OVER
}

class SoundEngine(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var musicJob: Job? = null

    @Volatile
    var musicVolume: Float = 0.65f

    @Volatile
    var sfxVolume: Float = 0.85f

    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var vibrationEnabled: Boolean = true

    @Volatile
    private var musicActive: Boolean = false

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun updateSettings(musicVol: Float, sfxVol: Float, muted: Boolean, vibrate: Boolean) {
        musicVolume = musicVol
        sfxVolume = sfxVol
        isMuted = muted
        vibrationEnabled = vibrate
    }

    fun startMusic() {
        if (musicActive) return
        musicActive = true
        musicJob?.cancel()
        musicJob = scope.launch {
            val sampleRate = 22050
            // Upbeat retro-modern arcade chord progression arpeggio (in Hz)
            val melodyNotes = doubleArrayOf(
                261.63, 329.63, 392.00, 523.25,
                293.66, 369.99, 440.00, 587.33,
                329.63, 392.00, 493.88, 659.25,
                293.66, 349.23, 440.00, 523.25
            )
            var step = 0
            while (isActive && musicActive) {
                val effectiveVol = if (isMuted) 0f else (musicVolume * 0.16f)
                if (effectiveVol <= 0.005f) {
                    delay(250)
                    continue
                }
                val freq = melodyNotes[step % melodyNotes.size]
                val durationMs = 210
                playToneInternal(
                    freqStart = freq,
                    freqEnd = freq * 1.002,
                    durationMs = durationMs,
                    volume = effectiveVol,
                    sampleRate = sampleRate,
                    harmonic = true
                )
                step++
                delay(230)
            }
        }
    }

    fun stopMusic() {
        musicActive = false
        musicJob?.cancel()
        musicJob = null
    }

    fun playSfx(type: SfxType) {
        if (vibrationEnabled) {
            when (type) {
                SfxType.CLICK -> vibrate(12)
                SfxType.HIT, SfxType.COLLISION -> vibrate(22)
                SfxType.GOAL, SfxType.EXPLOSION -> vibrate(45)
                SfxType.WIN -> vibrate(60)
                else -> {}
            }
        }
        val effectiveVol = if (isMuted) 0f else (sfxVolume * 0.45f)
        if (effectiveVol <= 0.01f) return

        scope.launch {
            when (type) {
                SfxType.CLICK -> playToneInternal(680.0, 880.0, 45, effectiveVol)
                SfxType.COUNTDOWN_TICK -> playToneInternal(523.25, 523.25, 95, effectiveVol)
                SfxType.COUNTDOWN_GO -> {
                    playToneInternal(783.99, 1046.50, 220, effectiveVol, harmonic = true)
                }
                SfxType.HIT -> playToneInternal(220.0, 95.0, 85, effectiveVol, noiseMix = 0.35f)
                SfxType.COLLISION -> playToneInternal(180.0, 110.0, 65, effectiveVol, noiseMix = 0.25f)
                SfxType.JUMP -> playToneInternal(320.0, 680.0, 95, effectiveVol)
                SfxType.SHOOT -> playToneInternal(880.0, 290.0, 75, effectiveVol)
                SfxType.SCORE -> playToneInternal(587.33, 880.0, 110, effectiveVol, harmonic = true)
                SfxType.GOAL -> {
                    playToneInternal(523.25, 659.25, 110, effectiveVol, harmonic = true)
                    delay(90)
                    playToneInternal(659.25, 1046.50, 200, effectiveVol, harmonic = true)
                }
                SfxType.EXPLOSION -> playToneInternal(140.0, 45.0, 210, effectiveVol, noiseMix = 0.65f)
                SfxType.WIN -> {
                    val notes = listOf(523.25, 659.25, 783.99, 1046.50)
                    for (n in notes) {
                        playToneInternal(n, n * 1.02, 115, effectiveVol, harmonic = true)
                        delay(100)
                    }
                }
                SfxType.GAME_OVER -> {
                    val notes = listOf(440.0, 392.0, 349.23, 261.63)
                    for (n in notes) {
                        playToneInternal(n, n * 0.98, 125, effectiveVol)
                        delay(110)
                    }
                }
            }
        }
    }

    private fun vibrate(ms: Long) {
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (_: Throwable) {
        }
    }

    private fun playToneInternal(
        freqStart: Double,
        freqEnd: Double,
        durationMs: Int,
        volume: Float,
        sampleRate: Int = 22050,
        harmonic: Boolean = false,
        noiseMix: Float = 0f
    ) {
        try {
            val numSamples = (sampleRate * durationMs / 1000).coerceAtLeast(64)
            val samples = ShortArray(numSamples)
            var phase = 0.0
            var rng = 1234567
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = freqStart + (freqEnd - freqStart) * progress
                phase += 2.0 * PI * freq / sampleRate
                val envelope = exp(-3.2 * progress) * (1.0 - progress * 0.3)
                var wave = sin(phase)
                if (harmonic) {
                    wave = 0.7 * wave + 0.3 * sin(phase * 1.5)
                }
                if (noiseMix > 0f) {
                    rng = (rng * 1103515245 + 12345) and 0x7fffffff
                    val noise = (rng.toDouble() / 0x3fffffff.toDouble()) - 1.0
                    wave = (1f - noiseMix) * wave + noiseMix * noise
                }
                val sampleVal = (wave * envelope * volume * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                samples[i] = sampleVal.toShort()
            }

            val bufferSize = samples.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize.coerceAtLeast(AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 15L)
            track.stop()
            track.release()
        } catch (_: Throwable) {
            // Gracefully ignore audio hardware unavailable in headless/test environments
        }
    }
}
