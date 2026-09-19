package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import android.view.Surface
import com.example.R
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BitmapVideoRecorder"

/**
 * Grabador de vídeo por hardware acelerado que codifica Bitmaps directamente en MP4
 * utilizando MediaCodec (H.264 / AVC) y MediaMuxer.
 *
 * Superpone directamente en cada fotograma del vídeo:
 * 1. El CLON TOTAL del marcador de Puntos del juego (píldora oscura sin etiqueta "PUNTOS", solo número atlético grande).
 * 2. El CLON TOTAL del marcador de Tiempo del juego (píldora oscura sin etiqueta "TIEMPO", solo cronómetro MM:SS y badge flotante +5s).
 * 3. El CLON TOTAL de las Dianas de Reacción (#1, #2, #3) (anillo exterior con consumo de tiempo circular blanco, círculo interior con degradado amarillo-ámbar-naranja, número oscuro deportivo de alto contraste, sin texto "TOCA").
 * 4. Popups flotantes de puntuación (+1, ¡COMBO!) idénticos al juego (sin badge ni píldora de fondo).
 * 5. Marca de agua broadcast con logo de marcaagua.png en la esquina inferior izquierda.
 */
class BitmapVideoRecorder(
    private val outputFile: File,
    private val width: Int = 1280,
    private val height: Int = 720,
    private val frameRate: Int = 30,
    private val context: Context? = null
) {
    private var codec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var muxer: MediaMuxer? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private val isRecording = AtomicBoolean(false)
    private var frameCount = 0L
    private var firstPtsUs = -1L
    private val bufferInfo = MediaCodec.BufferInfo()
    private val destRect = Rect(0, 0, width, height)

    // Bitmap de marca de agua cargado desde res/drawable/marcaagua.png
    private val watermarkBitmap: Bitmap? by lazy {
        context?.let { ctx ->
            try {
                BitmapFactory.decodeResource(ctx.resources, R.drawable.marcaagua)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo cargar marcaagua.png: ${e.message}")
                null
            }
        }
    }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Timestamp del primer frame grabado en tiempo de reloj
    var recordingStartWallClockMs: Long = 0L
        private set

    // --- RECTÁNGULOS REUTILIZABLES PARA EL CANVAS (EVITAR GC LAG EN GRABACIÓN) ---
    private val hudCardRect = RectF()
    private val tempPillRect = RectF()
    private val arcBounds = RectF()
    private val watermarkRect = RectF()

    // --- 1. CLON TOTAL MARCADOR DE PUNTOS (ARRIBA IZQUIERDA) ---
    // Píldora redondeada de fondo oscuro idéntica a ReactionScoreCounter
    private val scoreBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 40, 61) // 0xFF1E283D
        style = Paint.Style.FILL
    }
    private val scoreBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 68, 102, 153) // 0x33446699
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val scoreHitBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 226, 255, 57) // Amarillo neón al anotar
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val scoreValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val scoreHitValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 255, 57) // Volt neón
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(10f, 0f, 0f, Color.argb(220, 226, 255, 57))
    }

    // --- 2. CLON TOTAL MARCADOR DE TIEMPO (ARRIBA DERECHA) ---
    // Píldora redondeada idéntica a ReactionTimerBadge
    private val timerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 40, 61) // 0xFF1E283D
        style = Paint.Style.FILL
    }
    private val timerBonusBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(6, 58, 29) // 0xFF063A1D verde oscuro en bonus
        style = Paint.Style.FILL
    }
    private val timerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 68, 102, 153) // 0x33446699
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val timerUrgentBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 82, 82) // Rojo <= 10s
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val timerBonusBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 230, 118) // Verde neón 0xFF00E676 en bonus
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }
    private val timerValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val timerUrgentValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 82, 82)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val timerBonusValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 230, 118)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 0f, 0f, Color.argb(220, 0, 230, 118))
    }

    // Tag flotante "+5s ⏱️"
    private val timeBonusPillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(240, 6, 56, 28) // 0xF006381C
        style = Paint.Style.FILL
    }
    private val timeBonusPillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 230, 118)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val timeBonusTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 230, 118)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 0f, 0f, Color.argb(220, 0, 230, 118))
    }

    // --- 4. CLON TOTAL DE LAS DIANAS DE REACCIÓN (#1, #2, #3) ---
    // Anillo exterior de tiempo: pista oscura
    private val targetOuterTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 41, 59) // 0xFF1E293B
        style = Paint.Style.STROKE
    }
    // Barra circular blanca que se consume con la oleada
    private val targetOuterProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    // Círculo interior con degradado amarillo a naranja idéntico a ReactionPointTarget
    private val targetInnerGradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val targetInnerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 34, 34, 34) // Borde suave
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    // Número atlético deportivo grande en color gris muy oscuro/negro idéntico al juego (0xFF26262B)
    private val targetNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(38, 38, 43) // 0xFF26262B
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // --- 5. POPUPS FLOTANTES (+1, ¡COMBO! +5s) IDÉNTICOS AL JUEGO ---
    // En el juego: texto puro flotante con sombra, sin badges ni rectángulos de fondo
    private val popupRegularTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 255, 57) // Amarillo Volt neón idéntico al juego (#E2FF39)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(12f, 2f, 4f, Color.argb(200, 0, 0, 0))
    }
    private val popupComboTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 230, 118) // Verde Esmeralda neón idéntico al juego (#00E676)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(14f, 0f, 0f, Color.argb(200, 0, 230, 118))
    }

    // --- 6. MARCA DE AGUA BROADCAST KANTERA ---
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(225, 14, 20, 32)
        style = Paint.Style.FILL
    }
    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 152, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 17f
    }
    private val brandHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 255, 57)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 17f
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 200, 215, 235)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 10.5f
    }

    // --- 7. OVERLAY DEMASIADO CERCA (PANTALLA ROJA + ICONO + DA UN PASO ATRÁS + PUNTUACIÓN EN PAUSA) ---
    private val tooCloseOverlayPaint = Paint().apply {
        color = Color.argb(205, 220, 38, 38) // Fondo rojo translúcido en toda la pantalla (0xCCDC2626)
        style = Paint.Style.FILL
    }
    private val tooCloseIconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val tooCloseIconFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val tooCloseTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val tooCloseSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 235, 59) // 0xFFFFEB3B Amarillo neón
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val tooClosePillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val tooClosePillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 235, 59)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val tooClosePillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private var lastScoreSeen = -1
    private var lastScoreChangeTime = 0L

    @Synchronized
    fun start(): Boolean {
        if (isRecording.get()) return true
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 3_500_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                try {
                    setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                } catch (_: Exception) {}
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()
            codec = encoder

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxerStarted = false
            trackIndex = -1
            frameCount = 0L
            firstPtsUs = -1L
            recordingStartWallClockMs = System.currentTimeMillis()
            lastScoreSeen = -1
            lastScoreChangeTime = 0L
            isRecording.set(true)
            Log.i(TAG, "BitmapVideoRecorder started (${width}x${height}) -> ${outputFile.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BitmapVideoRecorder: ${e.message}", e)
            release()
            return false
        }
    }

    @Synchronized
    fun recordFrame(bitmap: Bitmap, overlayData: VideoGameOverlayData? = null) {
        if (!isRecording.get()) return
        val surface = inputSurface ?: return
        try {
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                surface.lockHardwareCanvas()
            } else {
                surface.lockCanvas(null)
            }

            val targetRatio = width.toFloat() / height.toFloat()
            val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

            if (width > height && srcRatio < 1.0f) {
                // Caso: Cámara vertical montada grabando en apaisado (16:9):
                // Ajustar al alto completo centrando el jugador para que no quede recortado
                val scaledWidth = (height * srcRatio).toInt()
                val left = (width - scaledWidth) / 2
                canvas.drawColor(Color.rgb(10, 14, 20))
                canvas.drawBitmap(bitmap, null, Rect(left, 0, left + scaledWidth, height), null)
            } else {
                // Center-crop estándar conservando proporciones
                val srcRect = if (srcRatio > targetRatio) {
                    val cropWidth = (bitmap.height * targetRatio).toInt().coerceAtMost(bitmap.width)
                    val left = ((bitmap.width - cropWidth) / 2).coerceAtLeast(0)
                    Rect(left, 0, left + cropWidth, bitmap.height)
                } else {
                    val cropHeight = (bitmap.width / targetRatio).toInt().coerceAtMost(bitmap.height)
                    val top = ((bitmap.height - cropHeight) / 2).coerceAtLeast(0)
                    Rect(0, top, bitmap.width, top + cropHeight)
                }
                canvas.drawBitmap(bitmap, srcRect, destRect, null)
            }

            // Dibuja el CLON TOTAL del HUD del juego
            if (overlayData != null) {
                drawGameOverlay(canvas, overlayData)
            }

            // Marca de agua sutil
            drawBrandingWatermark(canvas)

            surface.unlockCanvasAndPost(canvas)
            frameCount++
            drainEncoder(endOfStream = false)
        } catch (e: Exception) {
            Log.w(TAG, "Error recording frame to MP4: ${e.message}")
        }
    }

    /**
     * Dibuja los marcadores de Puntos y Tiempo exactamente iguales a como se ven en la app,
     * las Dianas con anillo de tiempo y degradado amarillo-naranja idénticas al juego,
     * y los popups flotantes de puntuación.
     */
    private fun drawGameOverlay(canvas: android.graphics.Canvas, data: VideoGameOverlayData) {
        val w = width.toFloat()
        val h = height.toFloat()
        val isHorizontal = w > h
        val now = System.currentTimeMillis()

        // Factor de escala dinámico:
        // En una pantalla de móvil real el ancho suele rondar 390-410dp y el alto 840-900dp (densidad ~2.5-3).
        // Si el vídeo se graba a 1280x720 horizontal o 720x1280 vertical, calculamos el factor de escala
        // relativo a la dimensión corta del viewport para que los puntos y marcadores ocupen
        // EXACTAMENTE la misma proporción visual respecto al área de pantalla que en la app en vivo.
        val baseDim = if (isHorizontal) h else w
        val scale = baseDim / 390f // ~1.85x para 720p

        // Detectar si la puntuación cambió recientemente para animar el marcador en amarillo volt
        if (lastScoreSeen != -1 && data.score > lastScoreSeen) {
            lastScoreChangeTime = now
        }
        lastScoreSeen = data.score
        val isScoreHitActive = (now - lastScoreChangeTime) < 320L

        // Detectar si hubo bonus de tiempo (+5s) reciente
        val hasRecentBonus = (now - data.timeBonusTrigger) < 1400L
        val isTimeUrgent = data.remainingTimeSec in 1..10

        // =========================================================================
        // 1. CLON TOTAL DEL CONTADOR DE PUNTOS (ARRIBA A LA IZQUIERDA)
        // En pantalla real: padding horizontal 16dp, top 18dp, altura 64dp, ancho ~110-130dp.
        // =========================================================================
        val scoreBadgeW = 120f * scale
        val scoreBadgeH = 64f * scale
        val scoreBadgeLeft = 16f * scale
        val scoreBadgeTop = (if (isHorizontal) 18f else 28f) * scale
        val cornerRadius = 18f * scale

        hudCardRect.set(scoreBadgeLeft, scoreBadgeTop, scoreBadgeLeft + scoreBadgeW, scoreBadgeTop + scoreBadgeH)
        canvas.drawRoundRect(hudCardRect, cornerRadius, cornerRadius, scoreBgPaint)
        val scoreBorder = if (isScoreHitActive) scoreHitBorderPaint else scoreBorderPaint
        scoreBorder.strokeWidth = if (isScoreHitActive) 3f * scale else 2f * scale
        canvas.drawRoundRect(hudCardRect, cornerRadius, cornerRadius, scoreBorder)

        // Número grande perfectamente centrado horizontal y verticalmente (40sp en app)
        val scoreTextPaint = if (isScoreHitActive) scoreHitValuePaint else scoreValuePaint
        scoreTextPaint.textSize = 40f * scale
        val scoreTextY = (scoreBadgeTop + (scoreBadgeH / 2f)) - ((scoreTextPaint.descent() + scoreTextPaint.ascent()) / 2f)
        canvas.drawText("${data.score}", scoreBadgeLeft + (scoreBadgeW / 2f), scoreTextY, scoreTextPaint)

        // =========================================================================
        // 2. CLON TOTAL DEL CONTADOR DE TIEMPO (ARRIBA A LA DERECHA)
        // En pantalla real: padding right 16dp, top 18dp, altura 64dp, ancho ~140-155dp.
        // =========================================================================
        val timerBadgeW = 145f * scale
        val timerBadgeH = 64f * scale
        val timerBadgeRight = w - (16f * scale)
        val timerBadgeLeft = timerBadgeRight - timerBadgeW
        val timerBadgeTop = (if (isHorizontal) 18f else 28f) * scale

        hudCardRect.set(timerBadgeLeft, timerBadgeTop, timerBadgeRight, timerBadgeTop + timerBadgeH)
        val timerBg = if (hasRecentBonus) timerBonusBgPaint else timerBgPaint
        val timerBorder = when {
            hasRecentBonus -> timerBonusBorderPaint
            isTimeUrgent -> timerUrgentBorderPaint
            else -> timerBorderPaint
        }
        timerBorder.strokeWidth = 2.5f * scale
        canvas.drawRoundRect(hudCardRect, cornerRadius, cornerRadius, timerBg)
        canvas.drawRoundRect(hudCardRect, cornerRadius, cornerRadius, timerBorder)

        val timerStr = String.format("%02d:%02d", data.remainingTimeSec / 60, data.remainingTimeSec % 60)
        val timerTextPaint = when {
            hasRecentBonus -> timerBonusValuePaint
            isTimeUrgent -> timerUrgentValuePaint
            else -> timerValuePaint
        }
        timerTextPaint.textSize = 32f * scale
        val timerTextY = (timerBadgeTop + (timerBadgeH / 2f)) - ((timerTextPaint.descent() + timerTextPaint.ascent()) / 2f)
        canvas.drawText(timerStr, timerBadgeLeft + (timerBadgeW / 2f), timerTextY, timerTextPaint)

        // Tag flotante "+5s ⏱️" sobre el contador de tiempo al lograr combo
        if (hasRecentBonus) {
            val bonusTagW = 90f * scale
            val bonusTagH = 34f * scale
            val bonusTagLeft = timerBadgeLeft + ((timerBadgeW - bonusTagW) / 2f)
            val bonusTagTop = timerBadgeTop - bonusTagH - (6f * scale)
            tempPillRect.set(bonusTagLeft, bonusTagTop, bonusTagLeft + bonusTagW, bonusTagTop + bonusTagH)
            timeBonusPillBorderPaint.strokeWidth = 2f * scale
            canvas.drawRoundRect(tempPillRect, 10f * scale, 10f * scale, timeBonusPillBgPaint)
            canvas.drawRoundRect(tempPillRect, 10f * scale, 10f * scale, timeBonusPillBorderPaint)

            timeBonusTextPaint.textSize = 20f * scale
            val bonusTextY = (bonusTagTop + (bonusTagH / 2f)) - ((timeBonusTextPaint.descent() + timeBonusTextPaint.ascent()) / 2f)
            canvas.drawText("+${data.timeBonusAmount}s", bonusTagLeft + (bonusTagW / 2f), bonusTextY, timeBonusTextPaint)
        }

        // =========================================================================
        // 4. CLON TOTAL DE LAS DIANAS DE REACCIÓN (#1, #2, #3)
        // En la pantalla en vivo: pointSize = 98dp.
        // En el vídeo: el radio exterior es exactamente (98dp / 2) * scale.
        // Para 720p horizontal/vertical scale = 720 / 390 = ~1.85, radio = 49 * 1.85 = ~90px (diámetro 180px).
        // Se ve exactamente en el mismo tamaño relativo al plano que en el móvil.
        // =========================================================================
        val unhitPoints = data.activePoints.filter { !it.isHit }
        if (unhitPoints.isNotEmpty() && data.isTimerRunning) {
            val targetOuterRadius = 49f * scale
            val targetInnerRadius = targetOuterRadius * 0.74f
            val strokeW = 10f * scale

            targetOuterTrackPaint.strokeWidth = strokeW
            targetOuterProgressPaint.strokeWidth = strokeW
            targetInnerBorderPaint.strokeWidth = 2.5f * scale
            targetNumberPaint.textSize = 40f * scale

            for (point in unhitPoints) {
                val cx = point.xNorm * w
                val cy = point.yNorm * h

                // 4.1. Anillo exterior de tiempo
                arcBounds.set(cx - targetOuterRadius, cy - targetOuterRadius, cx + targetOuterRadius, cy + targetOuterRadius)
                // Pista circular oscura
                canvas.drawArc(arcBounds, 0f, 360f, false, targetOuterTrackPaint)

                // Barra circular blanca que se consume con la oleada
                val elapsed = (now - point.spawnTimeMs).coerceAtLeast(0L)
                val remainingFraction = if (point.durationMs > 0) {
                    ((point.durationMs - elapsed).toFloat() / point.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 1f

                val sweep = 360f * remainingFraction
                if (sweep > 0f) {
                    canvas.drawArc(arcBounds, -90f, -sweep, false, targetOuterProgressPaint)
                }

                // 4.2. Círculo interior con degradado vertical amarillo-ámbar-naranja
                targetInnerGradientPaint.shader = LinearGradient(
                    cx, cy - targetInnerRadius,
                    cx, cy + targetInnerRadius,
                    intArrayOf(
                        Color.rgb(255, 238, 51), // 0xFFFFEE33 Amarillo vibrante superior
                        Color.rgb(255, 153, 0),  // 0xFFFF9900 Ámbar medio
                        Color.rgb(255, 87, 34)   // 0xFFFF5722 Naranja intenso inferior
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx, cy, targetInnerRadius, targetInnerGradientPaint)
                canvas.drawCircle(cx, cy, targetInnerRadius, targetInnerBorderPaint)

                // 4.3. Número atlético deportivo grande (#1, #2, #3) perfectamente centrado
                val numY = cy - ((targetNumberPaint.descent() + targetNumberPaint.ascent()) / 2f)
                canvas.drawText("${point.number}", cx, numY, targetNumberPaint)
            }
        }

        // =========================================================================
        // 5. POPUPS FLOTANTES (+1, ¡COMBO! +5s)
        // Idéntico al juego en vivo: texto atlético grande sin ningún badge ni caja verde
        // con animación de elevación (-65dp * scale) y fade-out suave
        // =========================================================================
        for (popup in data.popups) {
            val ageMs = now - popup.timestamp
            if (ageMs in 0..1000L) {
                // Curva de elevación suave hacia arriba como en ReactionScorePopupItem
                val progress = (ageMs / 1000f).coerceIn(0f, 1f)
                val floatUp = progress * (65f * scale)
                val alpha = if (ageMs > 650L) (1f - ((ageMs - 650L) / 350f)).coerceIn(0f, 1f) else 1f
                val px = popup.xNorm * w
                val py = (popup.yNorm * h) - floatUp

                val isCombo = popup.text.contains("COMBO") || popup.text.contains("+5") || popup.text.contains("🔥")
                val textPaint = if (isCombo) popupComboTextPaint else popupRegularTextPaint
                // Escala de tamaño equivalente a 60sp para +1 y 34sp para combos
                textPaint.textSize = (if (isCombo) 34f else 60f) * scale
                textPaint.alpha = (alpha * 255).toInt()

                val textY = py - ((textPaint.descent() + textPaint.ascent()) / 2f)
                canvas.drawText(popup.text, px, textY, textPaint)
            }
        }

        // =========================================================================
        // 6. AVISO DE ADVERTENCIA EN PANTALLA COMPLETA ROJA (DEMASIADO CERCA)
        // Idéntico al juego en vivo: fondo rojo en toda la pantalla, icono de advertencia,
        // texto ¡DEMASIADO CERCA!, DA UN PASO ATRÁS y píldora PUNTUACIÓN EN PAUSA.
        // Sin el texto de abajo final conforme a las indicaciones del usuario.
        // =========================================================================
        if (data.isTooClose) {
            drawTooCloseWarning(canvas, w, h, scale)
        }
    }

    private fun drawTooCloseWarning(canvas: android.graphics.Canvas, w: Float, h: Float, scale: Float) {
        // 1. Fondo completo rojo translúcido
        canvas.drawRect(0f, 0f, w, h, tooCloseOverlayPaint)

        val cx = w / 2f
        val cy = h / 2f

        // 2. Icono triangular de advertencia con signo de exclamación
        val iconSize = 64f * scale
        val iconTop = cy - (90f * scale)
        val path = Path().apply {
            moveTo(cx, iconTop)
            lineTo(cx + (iconSize / 2f), iconTop + iconSize)
            lineTo(cx - (iconSize / 2f), iconTop + iconSize)
            close()
        }
        tooCloseIconStrokePaint.strokeWidth = 4.5f * scale
        canvas.drawPath(path, tooCloseIconStrokePaint)

        // Signo de exclamación
        val exclTop = iconTop + (iconSize * 0.35f)
        val exclBottom = iconTop + (iconSize * 0.68f)
        canvas.drawLine(cx, exclTop, cx, exclBottom, tooCloseIconStrokePaint)
        canvas.drawCircle(cx, iconTop + (iconSize * 0.85f), 3f * scale, tooCloseIconFillPaint)

        // 3. Texto grande: ¡DEMASIADO CERCA!
        tooCloseTitlePaint.textSize = 30f * scale
        val titleY = iconTop + iconSize + (34f * scale)
        canvas.drawText("¡DEMASIADO CERCA!", cx, titleY, tooCloseTitlePaint)

        // 4. Texto: ⬅️ DA UN PASO ATRÁS
        tooCloseSubtitlePaint.textSize = 19f * scale
        val subY = titleY + (28f * scale)
        canvas.drawText("⬅️ DA UN PASO ATRÁS", cx, subY, tooCloseSubtitlePaint)

        // 5. Píldora: 🚫 PUNTUACIÓN EN PAUSA 🚫
        val pillW = 270f * scale
        val pillH = 42f * scale
        val pillLeft = cx - (pillW / 2f)
        val pillTop = subY + (18f * scale)
        tempPillRect.set(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH)
        tooClosePillBorderPaint.strokeWidth = 2f * scale
        canvas.drawRoundRect(tempPillRect, 14f * scale, 14f * scale, tooClosePillBgPaint)
        canvas.drawRoundRect(tempPillRect, 14f * scale, 14f * scale, tooClosePillBorderPaint)

        tooClosePillTextPaint.textSize = 13.5f * scale
        val pillTextY = (pillTop + (pillH / 2f)) - ((tooClosePillTextPaint.descent() + tooClosePillTextPaint.ascent()) / 2f)
        canvas.drawText("🚫 PUNTUACIÓN EN PAUSA 🚫", cx, pillTextY, tooClosePillTextPaint)
    }

    private fun drawBrandingWatermark(canvas: android.graphics.Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val isHorizontal = w > h
        val baseDim = if (isHorizontal) h else w
        val scale = baseDim / 390f

        // Sello inferior izquierdo: imagen oficial marcaagua.png
        val bmp = watermarkBitmap
        if (bmp != null) {
            // Dimensiones originales: 331 x 68 (ratio 4.86:1)
            val desiredH = 34f * scale
            val desiredW = desiredH * (bmp.width.toFloat() / bmp.height.toFloat())
            val left = (if (isHorizontal) 24f else 20f) * scale
            val bottom = h - ((if (isHorizontal) 18f else 34f) * scale)
            val top = bottom - desiredH
            val right = left + desiredW
            watermarkRect.set(left, top, right, bottom)
            bitmapPaint.alpha = 240
            canvas.drawBitmap(bmp, null, watermarkRect, bitmapPaint)
        } else {
            // Fallback en caso de que la imagen no cargue
            val tagW = 140f * scale
            val tagH = 26f * scale
            val tagLeft = (if (isHorizontal) 24f else 20f) * scale
            val tagBottom = h - ((if (isHorizontal) 18f else 34f) * scale)
            val tagTop = tagBottom - tagH
            val tagRight = tagLeft + tagW
            watermarkRect.set(tagLeft, tagTop, tagRight, tagBottom)
            subtitlePaint.textSize = 11f * scale
            canvas.drawRoundRect(watermarkRect, 8f * scale, 8f * scale, badgeBgPaint)
            canvas.drawText("🏀 KANTERA.APP", tagLeft + (10f * scale), tagBottom - (7f * scale), subtitlePaint)
        }

        // Distintivo inferior derecho KANTERA AI • BALL & TOUCH
        val badgeW = 210f * scale
        val badgeH = 46f * scale
        val badgeRight = w - ((if (isHorizontal) 24f else 20f) * scale)
        val badgeLeft = badgeRight - badgeW
        val badgeBottom = h - ((if (isHorizontal) 18f else 34f) * scale)
        val badgeTop = badgeBottom - badgeH
        watermarkRect.set(badgeLeft, badgeTop, badgeRight, badgeBottom)

        badgeBorderPaint.strokeWidth = 1.5f * scale
        canvas.drawRoundRect(watermarkRect, 10f * scale, 10f * scale, badgeBgPaint)
        canvas.drawRoundRect(watermarkRect, 10f * scale, 10f * scale, badgeBorderPaint)

        titlePaint.textSize = 17f * scale
        brandHighlightPaint.textSize = 17f * scale
        canvas.drawText("KANTERA", badgeLeft + (14f * scale), badgeTop + (21f * scale), titlePaint)
        canvas.drawText("AI", badgeLeft + (100f * scale), badgeTop + (21f * scale), brandHighlightPaint)
        canvas.drawText("BALL & TOUCH CHALLENGE", badgeLeft + (14f * scale), badgeTop + (38f * scale), subtitlePaint)
    }

    @Synchronized
    fun stop(): File? {
        if (!isRecording.get()) return null
        isRecording.set(false)
        try {
            drainEncoder(endOfStream = true)
        } catch (e: Exception) {
            Log.w(TAG, "Error draining encoder on stop: ${e.message}")
        }
        release()
        return if (outputFile.exists() && outputFile.length() > 0) outputFile else null
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val encoder = codec ?: return
        val mux = muxer ?: return

        if (endOfStream) {
            try {
                encoder.signalEndOfInputStream()
            } catch (e: Exception) {
                Log.w(TAG, "Failed signaling EOS: ${e.message}")
            }
        }

        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) {
                    Log.w(TAG, "Format changed twice; ignoring")
                } else {
                    val newFormat = encoder.outputFormat
                    trackIndex = mux.addTrack(newFormat)
                    mux.start()
                    muxerStarted = true
                    Log.i(TAG, "Muxer started with trackIndex $trackIndex")
                }
            } else if (outIndex >= 0) {
                try {
                    val encodedData = encoder.getOutputBuffer(outIndex)
                    if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (muxerStarted && bufferInfo.size > 0) {
                            if (firstPtsUs < 0L) {
                                firstPtsUs = bufferInfo.presentationTimeUs
                            }
                            // Normalizar presentationTimeUs para que empiece exactamente en 0
                            val normalizedPts = (bufferInfo.presentationTimeUs - firstPtsUs).coerceAtLeast(0L)
                            bufferInfo.presentationTimeUs = normalizedPts

                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            mux.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                } catch (e: Exception) {
                    Log.w(TAG, "Error writing video sample: ${e.message}")
                }

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private fun release() {
        try {
            codec?.stop()
        } catch (_: Exception) {}
        try {
            codec?.release()
        } catch (_: Exception) {}
        codec = null

        inputSurface?.release()
        inputSurface = null

        if (muxerStarted) {
            try {
                muxer?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping muxer: ${e.message}")
            }
            muxerStarted = false
        }
        try {
            muxer?.release()
        } catch (_: Exception) {}
        muxer = null
    }
}
