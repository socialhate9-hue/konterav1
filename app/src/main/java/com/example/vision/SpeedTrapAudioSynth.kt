package com.example.vision

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Sintetizador de audio nativo de motor y efectos de carrera (Bote de Fuego / Speed Trap).
 * No requiere archivos de audio externos (100% nativo y en tiempo real usando AudioTrack PCM).
 * Modula la frecuencia del motor de acuerdo a los BPM actuales.
 */
object SpeedTrapAudioSynth {
    private const val TAG = "SpeedTrapAudioSynth"
    private const val SAMPLE_RATE = 22050

    private var audioTrack: AudioTrack? = null
    private var engineJob: Job? = null
    @Volatile private var isRunning = false
    @Volatile private var targetBpm = 0f
    @Volatile private var isMuted = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun updateBpm(bpm: Float) {
        targetBpm = bpm
    }

    fun startEngine(scope: CoroutineScope) {
        if (isRunning) return
        isRunning = true

        engineJob = scope.launch(Dispatchers.Default) {
            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(1024)

            try {
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
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                var phase = 0.0
                var currentFreq = 75.0 // RPM ralentí
                val pcmBuffer = ShortArray(bufferSize)

                while (isActive && isRunning) {
                    if (isMuted) {
                        pcmBuffer.fill(0)
                        track.write(pcmBuffer, 0, bufferSize)
                        delay(20)
                        continue
                    }

                    // Calcular frecuencia base según BPM (ralentí a 75 Hz, hasta 360 Hz en zona fuego)
                    val desiredFreq = when {
                        targetBpm <= 0f -> 65.0
                        targetBpm < 60f -> 75.0 + (targetBpm / 60f) * 45.0
                        targetBpm < 110f -> 120.0 + ((targetBpm - 60f) / 50f) * 110.0
                        targetBpm < 150f -> 230.0 + ((targetBpm - 110f) / 40f) * 100.0
                        else -> 330.0 + ((targetBpm - 150f) / 50f).coerceAtMost(1f) * 50.0
                    }

                    // Suavizado de frecuencia (pitch)
                    currentFreq += (desiredFreq - currentFreq) * 0.12

                    val amplitude = when {
                        targetBpm <= 0f -> 0.20
                        targetBpm < 70f -> 0.35
                        targetBpm < 110f -> 0.55
                        else -> 0.85 // Ruge a tope en zona fuego
                    }

                    for (i in 0 until bufferSize) {
                        // Síntesis de tono de motor: fundamental + 2º armónico + subarmónico áspero
                        val fundamental = sin(phase)
                        val harmonic2 = 0.45 * sin(phase * 2.0)
                        val subHarmonic = 0.25 * sin(phase * 0.5)
                        val combined = (fundamental + harmonic2 + subHarmonic) * amplitude * 26000.0

                        pcmBuffer[i] = combined.toInt().coerceIn(-32767, 32767).toShort()

                        val phaseInc = (2.0 * PI * currentFreq) / SAMPLE_RATE
                        phase = (phase + phaseInc) % (2.0 * PI)
                    }

                    track.write(pcmBuffer, 0, bufferSize)
                }
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack engine error: ${e.message}")
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {}
                audioTrack = null
            }
        }
    }

    fun stopEngine() {
        isRunning = false
        engineJob?.cancel()
        engineJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    /**
     * Efecto de chirrido / fuego turbo al subir de zona
     */
    fun playTurboBoost(scope: CoroutineScope) {
        if (isMuted) return
        scope.launch(Dispatchers.Default) {
            try {
                val samples = 3500
                val buffer = ShortArray(samples)
                var phase = 0.0
                for (i in 0 until samples) {
                    val progress = i.toDouble() / samples
                    val freq = 450.0 + progress * 850.0
                    val amp = sin(progress * PI) * 24000.0
                    buffer[i] = (sin(phase) * amp).toInt().coerceIn(-32767, 32767).toShort()
                    phase += (2.0 * PI * freq) / SAMPLE_RATE
                }
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
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, samples)
                track.play()
                delay(200)
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }
    }
}
