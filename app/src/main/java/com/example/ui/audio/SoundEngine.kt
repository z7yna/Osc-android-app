package com.example.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin

class SoundEngine {
    private var isEnabled = true

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    private fun playTone(frequency: Double, durationMs: Int, amplitude: Double = 0.5, type: ToneType = ToneType.GLASS) {
        if (!isEnabled) return
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * durationMs) / 1000
                val bufferSize = maxOf(numSamples * 2, AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ))
                val buffer = ByteArray(bufferSize)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = when (type) {
                        ToneType.GLASS -> exp(-t * 25.0)
                        ToneType.CLICK -> exp(-t * 120.0)
                        ToneType.RADAR_PING -> exp(-t * 8.0) * (1.0 - t * (1000.0 / durationMs))
                        ToneType.SWEEP -> sin(t * Math.PI * (1000.0 / durationMs))
                        ToneType.VELVET_TYPING -> exp(-t * 180.0) * 0.4
                        ToneType.TICK -> exp(-t * 220.0) * 0.5
                    }

                    val sample = when (type) {
                        ToneType.GLASS -> (sin(2.0 * Math.PI * frequency * t) * 0.7 + sin(2.0 * Math.PI * frequency * 1.5 * t) * 0.3)
                        ToneType.CLICK -> sin(2.0 * Math.PI * frequency * t) + (Math.random() - 0.5) * 0.2
                        ToneType.RADAR_PING -> sin(2.0 * Math.PI * frequency * t) * exp(-t * 5.0)
                        ToneType.SWEEP -> sin(2.0 * Math.PI * frequency * t)
                        ToneType.VELVET_TYPING -> (Math.random() - 0.5) * 0.6 + sin(2.0 * Math.PI * frequency * 0.5 * t) * 0.4
                        ToneType.TICK -> sin(2.0 * Math.PI * frequency * t)
                    }

                    val valSample = (sample * envelope * amplitude * 32767.0).toInt().coerceIn(-32768, 32767)
                    val idx = 2 * i
                    if (idx + 1 < buffer.size) {
                        buffer[idx] = (valSample and 0xff).toByte()
                        buffer[idx + 1] = ((valSample shr 8) and 0xff).toByte()
                    }
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                    .setBufferSizeInBytes(buffer.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 20)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Safe fallback for restricted audio environments
            }
        }
    }

    fun playTick() {
        playTone(2400.0, 25, 0.3, ToneType.TICK)
    }

    fun playClick() {
        playTone(1800.0, 35, 0.4, ToneType.CLICK)
    }

    fun playType() {
        playTone(920.0, 18, 0.25, ToneType.VELVET_TYPING)
    }

    fun playGlassConfirmation() {
        playTone(3200.0, 75, 0.5, ToneType.GLASS)
    }

    fun playSuccess() {
        playTone(2093.0, 120, 0.6, ToneType.GLASS)
        GlobalScope.launch(Dispatchers.IO) {
            Thread.sleep(80)
            playTone(3136.0, 180, 0.6, ToneType.GLASS)
        }
    }

    fun playError() {
        playTone(320.0, 150, 0.5, ToneType.CLICK)
    }

    fun playDrawerOpen() {
        playTone(1200.0, 90, 0.35, ToneType.SWEEP)
    }

    fun playDrawerClose() {
        playTone(850.0, 90, 0.35, ToneType.SWEEP)
    }

    fun playRadarPing() {
        playTone(2450.0, 160, 0.5, ToneType.RADAR_PING)
    }

    enum class ToneType {
        GLASS, CLICK, RADAR_PING, SWEEP, VELVET_TYPING, TICK
    }

    companion object {
        val instance by lazy { SoundEngine() }
    }
}
