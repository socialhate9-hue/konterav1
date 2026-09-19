package com.example.vision

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

private const val TAG = "HighlightVideoGenerator"

/**
 * Motor de generación de Highlights y multiplexado de audio/música para Ball & Touch.
 *
 * Selecciona EXCLUSIVAMENTE los momentos donde el jugador anota puntos y realiza combos,
 * cortando ventanas dinámicas alrededor de cada acierto (1.2s antes para ver la aproximación/bote,
 * y 0.8s después para ver el toque, la diana desapareciendo y el marcador sumando).
 */
object HighlightVideoGenerator {

    private const val BEAT_ASSET_NAME = "audio/kantera_trap_beat.m4a"

    /**
     * Copia el asset musical a la caché si aún no existe.
     */
    fun getOrExtractBeatFile(context: Context): File? {
        val cacheFile = File(context.cacheDir, "kantera_trap_beat.m4a")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return cacheFile
        }
        return try {
            context.assets.open(BEAT_ASSET_NAME).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting beat audio asset: ${e.message}", e)
            null
        }
    }

    /**
     * Genera el vídeo compacto de Highlights centrado estrictamente en los puntos y combos conseguidos.
     */
    suspend fun createHighlightVideo(
        context: Context,
        sourceVideoFile: File,
        moments: List<ReactionHighlightMoment>,
        withMusic: Boolean
    ): File = withContext(Dispatchers.IO) {
        if (!sourceVideoFile.exists() || sourceVideoFile.length() == 0L) {
            Log.w(TAG, "Source video missing or empty, cannot generate highlights")
            return@withContext sourceVideoFile
        }

        val outputFile = File(
            context.cacheDir,
            "kantera_highlights_${System.currentTimeMillis()}.mp4"
        )

        try {
            val totalDurationMs = getVideoDurationMs(sourceVideoFile).coerceAtLeast(3000L)
            val intervals = calculateHighlightIntervals(moments, totalDurationMs)
            Log.i(TAG, "Highlight intervals count: ${intervals.size}, from moments: ${moments.size}, totalDuration: $totalDurationMs")

            val success = remuxSegments(
                context = context,
                sourceFile = sourceVideoFile,
                outputFile = outputFile,
                intervalsMs = intervals,
                withMusic = withMusic
            )

            if (success && outputFile.exists() && outputFile.length() > 0) {
                Log.i(TAG, "Highlight video created successfully: ${outputFile.length()} bytes")
                outputFile
            } else {
                Log.w(TAG, "Remuxing highlights failed, falling back to source video")
                sourceVideoFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating highlight video: ${e.message}", e)
            sourceVideoFile
        }
    }

    /**
     * Muxea la música en el vídeo completo si el usuario eligió Vídeo Completo con Música.
     */
    suspend fun createFullVideoWithMusic(
        context: Context,
        sourceVideoFile: File
    ): File = withContext(Dispatchers.IO) {
        if (!sourceVideoFile.exists() || sourceVideoFile.length() == 0L) {
            return@withContext sourceVideoFile
        }
        val beatFile = getOrExtractBeatFile(context) ?: return@withContext sourceVideoFile
        val outputFile = File(
            context.cacheDir,
            "kantera_full_music_${System.currentTimeMillis()}.mp4"
        )

        try {
            val totalDurationMs = getVideoDurationMs(sourceVideoFile).coerceAtLeast(3000L)
            val fullInterval = listOf(Pair(0L, totalDurationMs))

            val success = remuxSegments(
                context = context,
                sourceFile = sourceVideoFile,
                outputFile = outputFile,
                intervalsMs = fullInterval,
                withMusic = true
            )

            if (success && outputFile.exists() && outputFile.length() > 0) {
                outputFile
            } else {
                sourceVideoFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed adding music to full video: ${e.message}", e)
            sourceVideoFile
        }
    }

    /**
     * Calcula los intervalos de tiempo exactos donde se consiguieron puntos.
     * CADA INTERVALO ESTÁ CENTRADO EN UN PUNTO ANOTADO O COMBO.
     */
    private fun calculateHighlightIntervals(
        moments: List<ReactionHighlightMoment>,
        totalDurationMs: Long
    ): List<Pair<Long, Long>> {
        if (moments.isEmpty()) {
            // Si no se registraron puntos, tomar los primeros 12 segundos o la duración completa
            val end = min(totalDurationMs, 12000L)
            return listOf(Pair(0L, end))
        }

        // Ordenar cronológicamente todos los puntos conseguidos
        val validMoments = moments.filter { it.timestampMs in 0..totalDurationMs }
        val pointsToUse = if (validMoments.isNotEmpty()) validMoments else moments

        // Separar combos de puntos individuales
        val combos = pointsToUse.filter { it.isCombo }
        val singles = pointsToUse.filter { !it.isCombo }

        // Crear ventanas por cada hit: 1.2s antes para ver el bote/movimiento y 0.8s después para ver el acierto
        val candidateIntervals = mutableListOf<Pair<Long, Long>>()

        // Si hay combos, incluirlos todos prioritariamente
        for (combo in combos) {
            val start = max(0L, combo.timestampMs - 1400L)
            val end = min(totalDurationMs, combo.timestampMs + 900L)
            candidateIntervals.add(Pair(start, end))
        }

        // Añadir los mejores aciertos individuales
        for (hit in singles) {
            val start = max(0L, hit.timestampMs - 1200L)
            val end = min(totalDurationMs, hit.timestampMs + 800L)
            candidateIntervals.add(Pair(start, end))
        }

        // Ordenar intervalos por tiempo de inicio
        candidateIntervals.sortBy { it.first }

        // Fusionar intervalos superpuestos o muy cercanos (< 600ms de diferencia, p. ej. secuencia de un combo)
        val merged = mutableListOf<Pair<Long, Long>>()
        for (interval in candidateIntervals) {
            if (merged.isEmpty()) {
                merged.add(interval)
            } else {
                val last = merged.last()
                if (interval.first <= last.second + 600L) {
                    // Se solapan o son contiguos: fusionar en un único clip continuo
                    merged[merged.size - 1] = Pair(last.first, max(last.second, interval.second))
                } else {
                    merged.add(interval)
                }
            }
        }

        // Limitar la duración total a un máximo de ~22 segundos para que sea un highlight dinámico
        var accumulatedMs = 0L
        val selectedIntervals = mutableListOf<Pair<Long, Long>>()
        for (interval in merged) {
            val duration = interval.second - interval.first
            if (accumulatedMs + duration <= 24000L || selectedIntervals.isEmpty()) {
                selectedIntervals.add(interval)
                accumulatedMs += duration
            } else {
                // Si el último intervalo cabe parcialmente y aporta al menos 1.5s
                val remainingAllowance = 24000L - accumulatedMs
                if (remainingAllowance >= 1500L) {
                    selectedIntervals.add(Pair(interval.first, interval.first + remainingAllowance))
                }
                break
            }
        }

        return selectedIntervals
    }

    private fun getVideoDurationMs(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Remuxing con MediaExtractor y MediaMuxer.
     * Intercala vídeo y audio AAC fotograma a fotograma en orden cronológico estricto.
     */
    private fun remuxSegments(
        context: Context,
        sourceFile: File,
        outputFile: File,
        intervalsMs: List<Pair<Long, Long>>,
        withMusic: Boolean
    ): Boolean {
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            videoExtractor = MediaExtractor().apply {
                setDataSource(sourceFile.absolutePath)
            }

            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                    break
                }
            }

            if (videoTrackIndex < 0 || videoFormat == null) {
                Log.e(TAG, "No video track found in source file")
                return false
            }

            videoExtractor.selectTrack(videoTrackIndex)

            // Configurar pista de Audio si se solicita música
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            if (withMusic) {
                val beatFile = getOrExtractBeatFile(context)
                if (beatFile != null && beatFile.exists() && beatFile.length() > 0) {
                    try {
                        val aExt = MediaExtractor().apply {
                            setDataSource(beatFile.absolutePath)
                        }
                        for (i in 0 until aExt.trackCount) {
                            val format = aExt.getTrackFormat(i)
                            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                            if (mime.startsWith("audio/")) {
                                audioTrackIndex = i
                                audioFormat = format
                                audioExtractor = aExt
                                break
                            }
                        }
                        if (audioExtractor != null && audioTrackIndex >= 0) {
                            audioExtractor.selectTrack(audioTrackIndex)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed preparing audio extractor: ${e.message}")
                    }
                }
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxVideoTrack = muxer.addTrack(videoFormat)
            val muxAudioTrack = if (audioExtractor != null && audioFormat != null) {
                muxer.addTrack(audioFormat)
            } else {
                -1
            }

            muxer.start()

            val videoBufferSize = 1024 * 1024
            val videoBuffer = ByteBuffer.allocateDirect(videoBufferSize)
            val videoInfo = MediaCodec.BufferInfo()

            val audioBufferSize = 256 * 1024
            val audioBuffer = ByteBuffer.allocateDirect(audioBufferSize)
            val audioInfo = MediaCodec.BufferInfo()

            var currentVideoPtsUs = 0L
            var currentAudioPtsUs = 0L
            val videoFrameDurationUs = 33_333L // ~30 fps

            val audioSampleRate = if (audioFormat != null && audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
            } else {
                44100
            }
            val audioFrameDurationUs = (1024L * 1_000_000L) / audioSampleRate

            fun writeAudioUpTo(targetPtsUs: Long) {
                val aExt = audioExtractor ?: return
                if (muxAudioTrack < 0) return

                while (currentAudioPtsUs <= targetPtsUs) {
                    audioBuffer.clear()
                    var sampleSize = aExt.readSampleData(audioBuffer, 0)
                    if (sampleSize <= 0) {
                        aExt.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        sampleSize = aExt.readSampleData(audioBuffer, 0)
                        if (sampleSize <= 0) break
                    }

                    audioBuffer.position(0)
                    audioBuffer.limit(sampleSize)
                    audioInfo.offset = 0
                    audioInfo.size = sampleSize
                    audioInfo.presentationTimeUs = currentAudioPtsUs
                    audioInfo.flags = aExt.sampleFlags

                    muxer.writeSampleData(muxAudioTrack, audioBuffer, audioInfo)
                    currentAudioPtsUs += audioFrameDurationUs

                    if (!aExt.advance()) {
                        aExt.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    }
                }
            }

            // Remuxing e intercalado estricto fotograma a fotograma
            for (interval in intervalsMs) {
                val startUs = interval.first * 1000L
                val endUs = interval.second * 1000L

                videoExtractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                while (true) {
                    val sampleTimeUs = videoExtractor.sampleTime
                    if (sampleTimeUs < 0 || sampleTimeUs > endUs) {
                        break
                    }

                    videoBuffer.clear()
                    val sampleSize = videoExtractor.readSampleData(videoBuffer, 0)
                    if (sampleSize < 0) {
                        break
                    }

                    videoBuffer.position(0)
                    videoBuffer.limit(sampleSize)
                    videoInfo.offset = 0
                    videoInfo.size = sampleSize
                    videoInfo.presentationTimeUs = currentVideoPtsUs
                    var flags = videoExtractor.sampleFlags
                    if (currentVideoPtsUs == 0L) {
                        flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }
                    videoInfo.flags = flags

                    writeAudioUpTo(currentVideoPtsUs)
                    muxer.writeSampleData(muxVideoTrack, videoBuffer, videoInfo)
                    currentVideoPtsUs += videoFrameDurationUs

                    if (!videoExtractor.advance()) {
                        break
                    }
                }
            }

            writeAudioUpTo(currentVideoPtsUs)
            muxer.stop()
            Log.i(TAG, "Remuxing completed successfully: ${outputFile.length()} bytes, durationUs=$currentVideoPtsUs")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error during remuxing: ${e.message}", e)
            return false
        } finally {
            try {
                videoExtractor?.release()
            } catch (_: Exception) {}
            try {
                audioExtractor?.release()
            } catch (_: Exception) {}
            try {
                muxer?.release()
            } catch (_: Exception) {}
        }
    }

    fun shareToWhatsApp(context: Context, videoFile: File, score: Int) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )
            val text = "🏀 ¡Mira mi sesión en Ball & Touch de Kantera AI! He logrado $score puntos 🔥 #KanteraAI"

            val waIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.whatsapp")
            }

            val pm = context.packageManager
            if (waIntent.resolveActivity(pm) != null) {
                context.startActivity(waIntent)
            } else {
                val waBusinessIntent = Intent(waIntent).apply {
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (waBusinessIntent.resolveActivity(pm) != null) {
                    context.startActivity(waBusinessIntent)
                } else {
                    Toast.makeText(context, "WhatsApp no encontrado, abriendo selector...", Toast.LENGTH_SHORT).show()
                    val chooser = Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, text)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        "Compartir en WhatsApp u otra aplicación"
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing to WhatsApp: ${e.message}", e)
            Toast.makeText(context, "Error al compartir en WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareToInstagram(context: Context, videoFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )

            val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("content_url", "https://kantera.app")
            }

            val pm = context.packageManager
            if (storiesIntent.resolveActivity(pm) != null) {
                context.startActivity(storiesIntent)
                return
            }

            val igIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (igIntent.resolveActivity(pm) != null) {
                context.startActivity(igIntent)
                return
            }

            Toast.makeText(context, "Instagram no encontrado, abriendo opciones...", Toast.LENGTH_SHORT).show()
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "🏀 Mi sesión de Ball & Touch en Kantera AI #KanteraAI")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Compartir en Instagram u otra aplicación"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing to Instagram: ${e.message}", e)
            Toast.makeText(context, "Error al compartir en Instagram: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareGeneral(context: Context, videoFile: File, title: String = "Compartir vídeo Kantera") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "🏀 Mira mi entrenamiento en Kantera AI #KanteraAI")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening general share: ${e.message}", e)
        }
    }
}
