package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.model.CataclysmType
import com.example.model.CosmicScale
import kotlinx.coroutines.*
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural generative audio synthesizer for Phylogenesis.
 * Creates real-time scale-dependent ambient soundscapes using Android AudioTrack.
 */
class AudioSynthesizer {

    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    var currentScale: CosmicScale = CosmicScale.COSMIC

    @Volatile
    var isEnabled: Boolean = true

    @Volatile
    private var cataclysmStingerPending: CataclysmType? = null

    fun start() {
        if (isRunning) return
        isRunning = true

        synthJob = scope.launch {
            try {
                val sampleRate = 22050
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(2048)

                audioTrack = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val buffer = ShortArray(1024)
                var phase1 = 0.0
                var phase2 = 0.0
                var popTimer = 0

                while (isRunning && isActive) {
                    if (!isEnabled) {
                        // Silent output when muted
                        buffer.fill(0)
                        audioTrack?.write(buffer, 0, buffer.size)
                        delay(40)
                        continue
                    }

                    // Handle cataclysm stinger
                    val stinger = cataclysmStingerPending
                    if (stinger != null) {
                        cataclysmStingerPending = null
                        // Generate aggressive distorted rumble & impact wave
                        for (i in buffer.indices) {
                            val decay = (buffer.size - i).toDouble() / buffer.size
                            val noise = (Random.nextDouble() * 2.0 - 1.0) * 16000.0 * decay
                            val bass = sin(phase1 * 0.5) * 12000.0 * decay
                            buffer[i] = (noise + bass).toInt().coerceIn(-32000, 32000).toShort()
                            phase1 += 0.05
                        }
                        audioTrack?.write(buffer, 0, buffer.size)
                        continue
                    }

                    // Scale-dependent synthesis
                    when (currentScale) {
                        CosmicScale.COSMIC -> {
                            // Deep resonant cosmic drone (55 Hz + 110 Hz overtone)
                            val f1 = 55.0
                            val f2 = 110.0
                            val delta1 = (2.0 * Math.PI * f1) / sampleRate
                            val delta2 = (2.0 * Math.PI * f2) / sampleRate

                            for (i in buffer.indices) {
                                phase1 += delta1
                                phase2 += delta2
                                val s1 = sin(phase1) * 3500.0
                                val s2 = sin(phase2) * 1200.0
                                val staticNoise = (Random.nextDouble() - 0.5) * 400.0
                                buffer[i] = (s1 + s2 + staticNoise).toInt().toShort()
                            }
                        }
                        CosmicScale.ECOSYSTEM -> {
                            // Aeolian wind turbulence & bio-rustle (filtered noise + soft melodic hum)
                            val f1 = 164.8 // E3
                            val delta1 = (2.0 * Math.PI * f1) / sampleRate

                            for (i in buffer.indices) {
                                phase1 += delta1
                                val s1 = sin(phase1) * 1800.0
                                val wind = (Random.nextDouble() - 0.5) * 1500.0
                                buffer[i] = (s1 + wind).toInt().toShort()
                            }
                        }
                        CosmicScale.SPECIMEN -> {
                            // Rhythmic muscular heartbeat & vascular pulse
                            popTimer += buffer.size
                            val beatPhase = (popTimer % (sampleRate / 2)).toDouble() / (sampleRate / 2)
                            val pulseAmp = if (beatPhase < 0.15) sin(beatPhase / 0.15 * Math.PI) * 4500.0 else 0.0

                            val f1 = 80.0
                            val delta1 = (2.0 * Math.PI * f1) / sampleRate

                            for (i in buffer.indices) {
                                phase1 += delta1
                                val s1 = sin(phase1) * pulseAmp
                                val breathNoise = (Random.nextDouble() - 0.5) * 500.0
                                buffer[i] = (s1 + breathNoise).toInt().toShort()
                            }
                        }
                        CosmicScale.CELLULAR -> {
                            // Low-frequency fluid Brownian turbulence with sporadic ion channel pops
                            popTimer += buffer.size
                            val popChance = Random.nextFloat() < 0.18f

                            for (i in buffer.indices) {
                                val brownian = (Random.nextDouble() - 0.5) * 900.0
                                val pop = if (popChance && i < 120) sin(i * 0.3) * 3200.0 else 0.0
                                buffer[i] = (brownian + pop).toInt().toShort()
                            }
                        }
                    }

                    audioTrack?.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.w("PhylogenesisAudio", "AudioTrack unavailable or stopped: ${e.message}")
            }
        }
    }

    fun playCataclysmStinger(type: CataclysmType) {
        cataclysmStingerPending = type
    }

    fun stop() {
        isRunning = false
        synthJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
