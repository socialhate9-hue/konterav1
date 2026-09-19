package com.example.vision

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.ui.common.UniversalGameFinishedDialog
import com.example.theme.SportOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReactionPointsHUD(
    state: VisionState,
    onHitPoint: (Long) -> Unit,
    onDismissPopup: (Long) -> Unit,
    onRestartDrill: () -> Unit,
    onExitToMain: () -> Unit,
    onToggleShowSkeleton: () -> Unit,
    onToggleShowHoop: () -> Unit,
    onToggleShowBall: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleRecording: () -> Unit,
    onToggleRecordingFormat: () -> Unit = {},
    onSelectRecordingFormat: (VideoRecordingFormat) -> Unit = {},
    onSelectVideoFormat: (ReactionVideoShareFormat) -> Unit = {},
    onToggleMusic: () -> Unit = {},
    onShareVideo: (Context, String) -> Unit = { _, _ -> },
    onRecalibrate: () -> Unit = {},
    onSelectDribbleCombo: () -> Unit = {},
    onSelectReactionPoints: () -> Unit = {},
    onSelectDefendZone: () -> Unit = {},
    onSelectKidsMiniBasket: () -> Unit = {},
    onSelectSpeedTrap: () -> Unit = {},
    onSelectShooting: () -> Unit = {},
    onSelectUploadVideo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        VisionSettingsDialog(
            state = state,
            onDismiss = { showSettingsDialog = false },
            onToggleShowSkeleton = onToggleShowSkeleton,
            onToggleShowHoop = onToggleShowHoop,
            onToggleShowBall = onToggleShowBall,
            onSetHoopPerspective = {},
            onOpenSavedVideos = {},
            onRecalibrateHoop = onRecalibrate,
            onResetSession = onRestartDrill,
            onToggleCamera = onToggleCamera,
            onToggleRecording = onToggleRecording,
            onExitDrill = onExitToMain,
            onSelectDribbleCombo = onSelectDribbleCombo,
            onSelectReactionPoints = onSelectReactionPoints,
            onSelectDefendZone = onSelectDefendZone,
            onSelectKidsMiniBasket = onSelectKidsMiniBasket,
            onSelectSpeedTrap = onSelectSpeedTrap,
            onSelectShooting = onSelectShooting,
            onSelectUploadVideo = onSelectUploadVideo
        )
    }

    val remaining = state.reactionTimerRemainingSec
    val timerFormatted = String.format("%02d:%02d", remaining / 60, remaining % 60)

    // Control de posición y distancia: si hay cualquier infracción (lejos, cerca, descentrado, fuera de plano)
    val positionViolation = state.reactionPositionViolation
    val hasPositionViolation = positionViolation != null || state.isReactionPlayerTooClose

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // 1. TOP HEADER (Arriba a la izquierda contador idéntico a las capturas, arriba a la derecha cuenta regresiva y ajustes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Arriba a la izquierda: CONTADOR DE PUNTOS con animación de rebote y cambio de color a amarillo neón al puntuar
            ReactionScoreCounter(
                score = state.reactionScore
            )

            // Arriba a la derecha: BOTÓN VOZ + BOTÓN AJUSTES + CONTADOR DE TIEMPO
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón de Voz del Entrenador (Silenciar / Activar)
                var isVoiceMuted by remember { mutableStateOf(!VoiceCoachManager.isVoiceEnabled) }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isVoiceMuted) Color(0x33FF4444) else Color(0xFF1E283D))
                        .border(1.5.dp, if (isVoiceMuted) Color(0x88FF4444) else Color(0x33446699), CircleShape)
                        .clickable {
                            isVoiceMuted = !isVoiceMuted
                            VoiceCoachManager.isVoiceEnabled = !isVoiceMuted
                            if (isVoiceMuted) VoiceCoachManager.stop()
                        }
                        .testTag("reaction_voice_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isVoiceMuted) "Activar voz del entrenador" else "Silenciar voz",
                        tint = if (isVoiceMuted) Color(0xFFFF6666) else Color(0xFF2FB2C9),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Botón de Ajustes (arriba a la derecha, a la izquierda del contador de tiempo)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E283D))
                        .border(1.5.dp, Color(0x33446699), CircleShape)
                        .clickable { showSettingsDialog = true }
                        .testTag("reaction_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Badge de cuenta regresiva con animación de suma de tiempo (+5s) en verde llamativo cuando se completa un combo
                ReactionTimerBadge(
                    remainingSec = state.reactionTimerRemainingSec,
                    timeBonusTrigger = state.reactionTimeBonusTrigger,
                    timeBonusAmount = state.reactionTimeBonusAmount
                )
            }
        }

        // 2. ACTIVE REACTION POINTS (Salen simultáneamente en izquierda y/o derecha, hasta 3 en pantalla)
        val activePoints = state.activeReactionPoints.ifEmpty {
            listOfNotNull(state.activeReactionPoint)
        }
        val unhitPoints = activePoints.filter { !it.isHit }
        val currentExpectedNumber = unhitPoints.minOfOrNull { it.number } ?: 1

        if (unhitPoints.isNotEmpty() && state.isReactionTimerRunning && !state.isReactionSessionFinished) {
            val pointSize = 98.dp
            for (point in unhitPoints) {
                val targetX = (screenWidth * point.xNorm) - (pointSize / 2)
                val targetY = (screenHeight * point.yNorm) - (pointSize / 2)
                val isCurrentTarget = (point.number == currentExpectedNumber)

                ReactionPointTarget(
                    number = point.number,
                    spawnTimeMs = point.spawnTimeMs,
                    durationMs = point.durationMs,
                    isCurrentTarget = isCurrentTarget,
                    isHandOnlyBlocked = point.isHandOnlyBlocked,
                    isWrongSequenceAttempt = point.isWrongSequenceAttempt,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = targetX.roundToPx(),
                                y = targetY.roundToPx()
                            )
                        }
                        .size(pointSize)
                        .testTag("reaction_point_target_${point.number}")
                )
            }
        }

        // 1b. PANTALLA COMPLETA EN ROJO TRANSLÚCIDO SI LA POSICIÓN/DISTANCIA ES INCORRECTA (Puntuación bloqueada)
        AnimatedVisibility(
            visible = hasPositionViolation && ((state.isReactionTimerRunning && !state.isReactionSessionFinished) || state.playStartCountdownSec != null),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
            ReactionPositionWarningFullScreenOverlay(violation = positionViolation ?: PlayerPositionViolation.TOO_CLOSE)
        }

        // 1c. AVISO EN PANTALLA SI INTENTA TOCAR SIN BOTAR PRIMERO
        if (state.reactionWarningMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xEE1F0A0A))
                    .border(2.dp, Color(0xFFFF5252), RoundedCornerShape(18.dp))
                    .padding(horizontal = 22.dp, vertical = 14.dp)
                    .zIndex(150f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.reactionWarningMessage,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        // 2c. Barra informativa de regla de juego: sale solo cuando ha empezado el juego activo y desaparece a los 5 segundos
        var showRuleMessage by remember { mutableStateOf(false) }
        val isGameStarted = state.isReactionTimerRunning && !state.isReactionSessionFinished
        LaunchedEffect(isGameStarted) {
            if (isGameStarted) {
                showRuleMessage = true
                delay(5000L)
                showRuleMessage = false
            } else {
                showRuleMessage = false
            }
        }

        AnimatedVisibility(
            visible = showRuleMessage,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(600)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
                .zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xDD0F172A))
                    .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "🏀 BOTA EL BALÓN Y TOCA LOS PUNTOS EN ORDEN (1 ➔ 2 ➔ 3)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // 3. POPUPS DE PUNTOS CON ANIMACIÓN (+1 flotando hacia arriba y desvaneciéndose en la zona del point)
        state.reactionPopups.forEach { popup ->
            ReactionScorePopupItem(
                popup = popup,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                onDismiss = { onDismissPopup(popup.id) }
            )
        }

        // 4. MODAL DE FINALIZACIÓN CUANDO TERMINAN LOS 60 SEGUNDOS (Con Hype justo y diseño para compartir vídeo)
        if (state.isReactionSessionFinished) {
            val comboCount = state.reactionHighlightMoments.count { it.isCombo }
            UniversalGameFinishedDialog(
                gameTitle = "Ball & Touch (60s)",
                score = state.reactionScore,
                scoreLabel = "PUNTOS",
                secondaryStatValue = "$comboCount",
                secondaryStatLabel = "COMBOS x3",
                hypeReward = state.lastHypeReward,
                hasRecordedVideo = state.reactionRecordedVideoUri != null || state.lastRecordedVideoUri != null,
                selectedVideoFormat = state.selectedVideoShareFormat,
                recordingFormat = state.videoRecordingFormat,
                isMusicEnabled = state.isReactionVideoMusicEnabled,
                isGeneratingHighlight = state.isGeneratingHighlight,
                onSelectVideoFormat = onSelectVideoFormat,
                onSelectRecordingFormat = onSelectRecordingFormat,
                onToggleMusic = onToggleMusic,
                onShareVideo = onShareVideo,
                onRestart = onRestartDrill,
                onExit = onExitToMain
            )
        }
    }
}

/**
 * Contador de puntos en la esquina superior izquierda idéntico a las capturas 1 y 2 del usuario:
 * - Rectángulo redondeado azul marino oscuro con tipografía atlética grande.
 * - Al sumar nuevos puntos se activa una animación de movimiento (rebote de escala y desplazamiento)
 *   y cambia al color amarillo/verde neón característico (captura 2).
 * - Tras un instante, regresa fluidamente a su estado original blanco (captura 1).
 */
@Composable
private fun ReactionScoreCounter(
    score: Int,
    modifier: Modifier = Modifier
) {
    var previousScore by remember { mutableStateOf(score) }
    var isHitHighlighted by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }

    val textColor by animateColorAsState(
        targetValue = if (isHitHighlighted) Color(0xFFE2FF39) else Color.White,
        animationSpec = tween(durationMillis = if (isHitHighlighted) 60 else 400, easing = LinearEasing),
        label = "textColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHitHighlighted) Color(0x88E2FF39) else Color(0x33446699),
        animationSpec = tween(durationMillis = if (isHitHighlighted) 60 else 400, easing = LinearEasing),
        label = "borderColor"
    )

    LaunchedEffect(score) {
        if (score > previousScore) {
            previousScore = score
            isHitHighlighted = true
            // Animación de rebote y desplazamiento en movimiento como solicitado
            launch {
                scale.animateTo(1.24f, tween(110, easing = FastOutSlowInEasing))
                scale.animateTo(1.0f, tween(200, easing = FastOutSlowInEasing))
            }
            launch {
                offsetY.animateTo(-6f, tween(110, easing = FastOutSlowInEasing))
                offsetY.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            }
            delay(280L)
            isHitHighlighted = false
        } else {
            previousScore = score
        }
    }

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E283D))
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 24.dp)
            .testTag("reaction_score_counter"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$score",
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = textColor,
            letterSpacing = 1.sp,
            modifier = Modifier
                .scale(scale.value)
                .offset(y = offsetY.value.dp)
        )
    }
}

/**
 * DIANA ATRACTIVA Y DINÁMICA DE REACTION POINTS
 * - Temporizador circular exterior que se consume en la oleada.
 * - Secuencia numérica: indica el orden que debe seguirse (1, luego 2, luego 3).
 * - Diseño limpio: número grande y visible, estilo icónico amarillo/naranja de baloncesto.
 * - Sin textos confusos debajo de los números; solo el número deportivo de alto contraste.
 * - Alerta roja y animación si se intenta tocar fuera de orden o con mano vacía.
 */
@Composable
private fun ReactionPointTarget(
    number: Int,
    spawnTimeMs: Long,
    durationMs: Long,
    isCurrentTarget: Boolean = true,
    isHandOnlyBlocked: Boolean = false,
    isWrongSequenceAttempt: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = remember(spawnTimeMs) { Animatable(1f) }

    LaunchedEffect(spawnTimeMs) {
        val now = System.currentTimeMillis()
        val elapsed = (now - spawnTimeMs).coerceAtLeast(0L)
        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
        val initialFraction = if (durationMs > 0) (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 1f
        progress.snapTo(initialFraction)
        if (remaining > 0L) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = remaining.toInt(),
                    easing = LinearEasing
                )
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "target_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrentTarget) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .scale(if (isCurrentTarget) pulseScale else 0.95f),
        contentAlignment = Alignment.Center
    ) {
        // Envoltorio exterior: Barra circular blanca que desaparece recorriendo el contorno en el tiempo de la oleada
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)

            // Pista de fondo
            drawArc(
                color = when {
                    isHandOnlyBlocked || isWrongSequenceAttempt -> Color(0xFFFF2222)
                    isCurrentTarget -> Color(0xFF334155)
                    else -> Color(0xFF1E293B)
                },
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )

            // Barra blanca que se consume
            val currentSweep = 360f * progress.value.coerceIn(0f, 1f)
            if (currentSweep > 0f) {
                drawArc(
                    color = when {
                        isHandOnlyBlocked || isWrongSequenceAttempt -> Color(0xFFFF8888)
                        isCurrentTarget -> Color.White
                        else -> Color(0xAA94A3B8)
                    },
                    startAngle = -90f,
                    sweepAngle = -currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
            }
        }

        // Círculo interior: degradado amarillo a naranja icónico en todos los números (sin azul)
        Box(
            modifier = Modifier
                .fillMaxSize(0.74f)
                .clip(CircleShape)
                .background(
                    if (isHandOnlyBlocked || isWrongSequenceAttempt) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF4444),
                                Color(0xFFB71C1C)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFEE33), // Amarillo vibrante superior
                                Color(0xFFFF9900), // Ámbar medio
                                Color(0xFFFF5722)  // Naranja intenso inferior
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    }
                )
                .border(
                    width = if (isCurrentTarget) 2.5.dp else 1.5.dp,
                    color = if (isHandOnlyBlocked || isWrongSequenceAttempt) Color(0xFFFFCCCC) else Color(0x33222222),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Solo el número atlético grande, sin subtítulos debajo
            Text(
                text = when {
                    isHandOnlyBlocked -> "🚫"
                    isWrongSequenceAttempt -> "⚠️"
                    else -> "$number"
                },
                fontSize = if (isHandOnlyBlocked || isWrongSequenceAttempt) 28.sp else 40.sp,
                fontWeight = FontWeight.Black,
                color = if (isHandOnlyBlocked || isWrongSequenceAttempt) Color.White else Color(0xFF26262B),
                letterSpacing = (-1).sp
            )
        }
    }
}

/**
 * Badge de cuenta regresiva con animación de suma de tiempo (+5s) en verde llamativo cuando se completa un combo
 */
@Composable
private fun ReactionTimerBadge(
    remainingSec: Int,
    timeBonusTrigger: Long,
    timeBonusAmount: Int,
    modifier: Modifier = Modifier
) {
    var lastTrigger by remember { mutableStateOf(timeBonusTrigger) }
    var isBonusHighlighted by remember { mutableStateOf(false) }
    val bonusAnimY = remember { Animatable(0f) }
    val bonusAnimAlpha = remember { Animatable(0f) }
    val bonusAnimScale = remember { Animatable(0.6f) }
    val timerScale = remember { Animatable(1f) }

    val timerFormatted = String.format("%02d:%02d", remainingSec / 60, remainingSec % 60)

    LaunchedEffect(timeBonusTrigger) {
        if (timeBonusTrigger > 0L && timeBonusTrigger != lastTrigger) {
            lastTrigger = timeBonusTrigger
            isBonusHighlighted = true

            // Animación de rebote del badge
            launch {
                timerScale.animateTo(1.22f, tween(130, easing = FastOutSlowInEasing))
                timerScale.animateTo(1.0f, tween(260, easing = FastOutSlowInEasing))
            }

            // Animación del tag flotante +5s en verde neón que resalta claramente
            launch {
                bonusAnimY.snapTo(10f)
                bonusAnimAlpha.snapTo(1f)
                bonusAnimScale.snapTo(0.6f)

                launch {
                    bonusAnimScale.animateTo(1.3f, tween(180, easing = FastOutSlowInEasing))
                    bonusAnimScale.animateTo(1.0f, tween(220, easing = LinearEasing))
                }
                launch {
                    bonusAnimY.animateTo(-45f, tween(1300, easing = FastOutSlowInEasing))
                }
                delay(750L)
                bonusAnimAlpha.animateTo(0f, tween(500, easing = LinearEasing))
            }

            delay(1100L)
            isBonusHighlighted = false
        } else {
            lastTrigger = timeBonusTrigger
        }
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            isBonusHighlighted -> Color(0xFF00E676) // Verde neón brillante al sumar tiempo
            remainingSec <= 10 -> Color(0xFFFF5252)
            else -> Color(0x33446699)
        },
        animationSpec = tween(durationMillis = if (isBonusHighlighted) 80 else 350),
        label = "timerBorderColor"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isBonusHighlighted -> Color(0xFF00E676) // Texto verde brillante
            remainingSec <= 10 -> Color(0xFFFF5252)
            else -> Color.White
        },
        animationSpec = tween(durationMillis = if (isBonusHighlighted) 80 else 350),
        label = "timerTextColor"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Badge de cuenta regresiva
        Box(
            modifier = Modifier
                .height(64.dp)
                .scale(timerScale.value)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isBonusHighlighted) Color(0xFF063A1D) else Color(0xFF1E283D))
                .border(2.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 24.dp)
                .testTag("reaction_countdown_timer"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timerFormatted,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                letterSpacing = 1.sp
            )
        }

        // Animación flotante destacada "+5s" en verde que resalta sobre el contador
        if (bonusAnimAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .offset(y = bonusAnimY.value.dp)
                    .scale(bonusAnimScale.value)
                    .alpha(bonusAnimAlpha.value)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xF006381C))
                    .border(2.dp, Color(0xFF00E676), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .zIndex(20f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "+${timeBonusAmount}s",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E676),
                        letterSpacing = 0.5.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF00E676),
                                offset = Offset(0f, 0f),
                                blurRadius = 14f
                            )
                        )
                    )
                    Text(
                        text = "⏱️",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Animación del popup de puntos (+1 o COMBO +5s):
 * - Emerge exactamente en la zona del point alcanzado o centro de pantalla
 * - Si es combo o bonus de tiempo, resalta en verde neón con sombra brillante
 * - Se eleva hacia arriba y se desvanece suavemente
 */
@Composable
private fun ReactionScorePopupItem(
    popup: ReactionPopup,
    screenWidth: Dp,
    screenHeight: Dp,
    onDismiss: () -> Unit
) {
    val animOffset = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }
    val animScale = remember { Animatable(0.6f) }

    val isBonusOrCombo = popup.text.contains("COMBO") || popup.text.contains("+5") || popup.text.contains("⏱️")

    LaunchedEffect(popup.id) {
        animScale.animateTo(if (isBonusOrCombo) 1.25f else 1.4f, tween(160, easing = FastOutSlowInEasing))
        animScale.animateTo(1.0f, tween(120, easing = LinearEasing))
        animOffset.animateTo(-65f, tween(if (isBonusOrCombo) 850 else 650, easing = FastOutSlowInEasing))
        animAlpha.animateTo(0f, tween(250, easing = LinearEasing))
        onDismiss()
    }

    val popupSize = if (isBonusOrCombo) 260.dp else 140.dp
    val posX = (screenWidth * popup.xNorm) - (popupSize / 2)
    val posY = (screenHeight * popup.yNorm) - (popupSize / 2) + animOffset.value.dp

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = posX.roundToPx(),
                    y = posY.roundToPx()
                )
            }
            .size(popupSize)
            .scale(animScale.value)
            .alpha(animAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = popup.text,
            fontSize = if (isBonusOrCombo) 34.sp else 60.sp,
            fontWeight = FontWeight.Black,
            color = if (isBonusOrCombo) Color(0xFF00E676) else Color(0xFFE2FF39),
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = if (isBonusOrCombo) Color(0xCC00E676) else Color(0xCC000000),
                    offset = Offset(2f, 4f),
                    blurRadius = 12f
                )
            )
        )
    }
}

/**
 * Modal de resultado tras los 60 segundos
 */
@Composable
private fun ReactionFinishedDialog(
    state: VisionState,
    onSelectVideoFormat: (ReactionVideoShareFormat) -> Unit,
    onSelectRecordingFormat: (VideoRecordingFormat) -> Unit,
    onToggleMusic: () -> Unit,
    onShareVideo: (Context, String) -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val comboCount = state.reactionHighlightMoments.count { it.isCombo }
    val isHorizontal = state.videoRecordingFormat == VideoRecordingFormat.HORIZONTAL
    val isHighlightFormat = state.selectedVideoShareFormat == ReactionVideoShareFormat.HIGHLIGHTS

    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141926))
                .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("reaction_finished_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // TÍTULO
                Text(
                    text = "🏀 ¡TIEMPO COMPLETADO!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9800)
                )

                Text(
                    text = "Sesión de Bote & Reaction (60s)",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )

                // TARJETA DE PUNTOS Y COMBOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Puntos
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E2638))
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.reactionScore}",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "PUNTOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300),
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Combos
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E2638))
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$comboCount",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E676)
                            )
                            Text(
                                text = "COMBOS x3",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // AVISO DE PRIVACIDAD TOTAL DEL VÍDEO
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1A00E676))
                        .border(1.2.dp, Color(0x6600E676), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .testTag("reaction_privacy_info_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x3300E676)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privacidad y seguridad",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "🔒 PRIVACIDAD TOTAL DEL VÍDEO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E676),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Si quieres compartir el juego, hazlo a continuación. Si no lo deseas, el vídeo se elimina automáticamente: no se guarda en ningún servidor ni en ningún sitio.",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE0E6ED),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // SECCIÓN VÍDEO DE LA PARTIDA CON MARCA DE AGUA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF192233))
                        .border(1.dp, Color(0x33446699), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color(0xFF2FB2C9),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "VÍDEO DE LA PARTIDA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Badge Marca de agua Kantera
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x332FB2C9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MARCA DE AGUA KANTERA",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2FB2C9),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Selector de Formato: Vídeo Completo vs Highlights
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Opción Vídeo Completo
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isHighlightFormat) Color(0xFF2FB2C9) else Color(0xFF243048))
                                .clickable { onSelectVideoFormat(ReactionVideoShareFormat.FULL_VIDEO) }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🎬 Vídeo Completo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isHighlightFormat) Color.Black else Color.White
                                )
                                Text(
                                    text = "60s partida íntegra",
                                    fontSize = 9.sp,
                                    color = if (!isHighlightFormat) Color(0xCC000000) else Color(0xFFAAAAAA)
                                )
                            }
                        }

                        // Opción Highlights
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isHighlightFormat) Color(0xFFFF9800) else Color(0xFF243048))
                                .clickable { onSelectVideoFormat(ReactionVideoShareFormat.HIGHLIGHTS) }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🔥 Highlights",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHighlightFormat) Color.Black else Color.White
                                )
                                Text(
                                    text = "Mejores jugadas/combos",
                                    fontSize = 9.sp,
                                    color = if (isHighlightFormat) Color(0xCC000000) else Color(0xFFAAAAAA)
                                )
                            }
                        }
                    }

                    // Selector de Orientación: Horizontal vs Vertical
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Orientación:",
                            fontSize = 11.sp,
                            color = Color(0xFFAAAAAA)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isHorizontal) Color(0xFF2FB2C9) else Color(0xFF243048))
                                .clickable { onSelectRecordingFormat(VideoRecordingFormat.HORIZONTAL) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🖥️ 16:9",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHorizontal) Color.Black else Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isHorizontal) Color(0xFFFF9800) else Color(0xFF243048))
                                .clickable { onSelectRecordingFormat(VideoRecordingFormat.VERTICAL) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📱 9:16",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isHorizontal) Color.Black else Color.White
                            )
                        }
                    }

                    // Toggle de Música con ritmo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (state.isReactionVideoMusicEnabled) Color(0xFFE2FF39) else Color(0xFFAAAAAA),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Música Rítmica (Trap Beat)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Base de ritmo sincronizada",
                                    fontSize = 9.sp,
                                    color = Color(0xFFAAAAAA)
                                )
                            }
                        }

                        Switch(
                            checked = state.isReactionVideoMusicEnabled,
                            onCheckedChange = { onToggleMusic() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFE2FF39),
                                checkedTrackColor = Color(0xFF4A5568),
                                uncheckedThumbColor = Color(0xFFAAAAAA),
                                uncheckedTrackColor = Color(0xFF1E283D)
                            )
                        )
                    }

                    // Indicador si está generando highlight o procesando
                    if (state.isGeneratingHighlight) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFFFF9800),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preparando vídeo y música...",
                                fontSize = 11.sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // BOTONES DE COMPARTIR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Compartir en WhatsApp
                        Button(
                            onClick = { onShareVideo(context, "whatsapp") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("reaction_share_whatsapp_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            enabled = !state.isGeneratingHighlight
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "WhatsApp",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Compartir en Instagram
                        Button(
                            onClick = { onShareVideo(context, "instagram") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("reaction_share_instagram_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                            enabled = !state.isGeneratingHighlight
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Instagram",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Botón Más opciones / Guardar
                    Button(
                        onClick = { onShareVideo(context, "general") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("reaction_share_general_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222B3D)),
                        enabled = !state.isGeneratingHighlight
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Guardar en Galería / Más opciones",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // BOTONES DE REPETIR Y SALIR
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("reaction_restart_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Text(
                            text = "REPETIR RETO (60s)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }

                Button(
                    onClick = onExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("reaction_exit_menu_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638))
                ) {
                    Text(
                        text = "SALIR AL MENÚ PRINCIPAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }
        }
    }
}

/**
 * Banner flotante en vivo durante la partida:
 * Si el jugador se mueve demasiado hacia atrás, muestra un aviso para que se acerque a la marca.
 */
@Composable
fun ReactionDistanceLiveBanner(
    distanceStatus: PlayerDistanceStatus,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = distanceStatus == PlayerDistanceStatus.TOO_FAR,
        enter = fadeIn(tween(250)) + scaleIn(tween(250)),
        exit = fadeOut(tween(300)) + scaleOut(tween(300)),
        modifier = modifier
    ) {
        val bgGrad = Color(0xF0075985)
        val borderColor = Color(0xFF38BDF8)
        val title = "⚠️ ¡DEMASIADO LEJOS!"
        val detail = "Acércate un poco a la marca"

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(bgGrad)
                .border(2.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = detail,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * PANTALLA COMPLETA EN ROJO TRANSLÚCIDO DE AVISO DE POSICIÓN
 * Ocupa toda la pantalla con un rojo translúcido ligero (alpha ~0.30 - 0.38) para que NO resulte abusivo,
 * permitiendo ver perfectamente la cámara y al jugador mientras se muestra el mensaje de advertencia.
 */
@Composable
private fun ReactionPositionWarningFullScreenOverlay(
    violation: PlayerPositionViolation?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_red_overlay")
    val overlayAlpha by infiniteTransition.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overlay_alpha"
    )
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    val (title, subtitle) = when (violation) {
        PlayerPositionViolation.NO_PLAYER -> Pair("¡VUELVE A TU POSICIÓN!", "👤 COLÓCATE FRENTE A LA CÁMARA")
        PlayerPositionViolation.INCOMPLETE_BODY -> Pair("¡ALÉJATE UN PASO!", "🦵 DEBEN VERSE TUS PIERNAS Y BRAZOS")
        PlayerPositionViolation.TOO_CLOSE -> Pair("¡DEMASIADO CERCA!", "⬅️ DA UN PASO ATRÁS")
        PlayerPositionViolation.TOO_FAR -> Pair("¡DEMASIADO LEJOS!", "➡️ DA UN PASO ADELANTE")
        PlayerPositionViolation.OFF_CENTER_LEFT -> Pair("¡CÉNTRATE!", "➡️ MUÉVETE A LA DERECHA")
        PlayerPositionViolation.OFF_CENTER_RIGHT -> Pair("¡CÉNTRATE!", "⬅️ MUÉVETE A LA IZQUIERDA")
        null -> Pair("¡POSICIÓN INCORRECTA!", "COLÓCATE FRENTE A LA CÁMARA")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFDC2626).copy(alpha = overlayAlpha))
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("reaction_position_warning_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(72.dp)
                    .scale(iconScale)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 1.2.sp,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFEB3B),
                textAlign = TextAlign.Center,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC000000))
                    .border(2.dp, Color(0xFFFFEB3B), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "🚫 PUNTUACIÓN EN PAUSA 🚫",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
