package com.example.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.ui.common.UniversalGameFinishedDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * SPEED TRAP / RADAR DE VELOCIDAD DE BOTE (BPM & BOTE DE FUEGO)
 * Estética cómic/manga de cartón con:
 * - Tacómetro estilo carreras (zonas Verde, Amarilla y Roja Fuego).
 * - Aguja de tacómetro reactiva con amortiguación y glow.
 * - Barra de aguante de 20 segundos para el reto "Bote de Fuego".
 * - Efectos manga de chispas, bocanadas de humo y onomatopeyas POP ("¡¡TURBO ON!!", "¡¡EN LLAMAS!!").
 * - Selector de mano (Derecha, Izquierda o Cualquiera).
 * - Botón para simular bote manual inmediato (para testing sin cámara o en pruebas).
 */
@Composable
fun SpeedTrapHUD(
    state: VisionState,
    onTriggerBounce: (Float, Float) -> Unit,
    onSetHand: (SpeedTrapHand) -> Unit,
    onSetTargetBpm: (Float) -> Unit,
    onRestartSession: () -> Unit,
    onExitToMain: () -> Unit,
    onToggleShowSkeleton: () -> Unit,
    onToggleShowBall: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleRecording: () -> Unit,
    onSelectDribbleCombo: () -> Unit,
    onSelectReactionPoints: () -> Unit,
    onSelectDefendZone: () -> Unit,
    onSelectKidsMiniBasket: () -> Unit,
    onSelectShooting: () -> Unit,
    onSelectUploadVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "comic_pulse")
    val comicFlameScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    val sparkGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spark_alpha"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight

        // 1. Efecto viñeta de fuego en los bordes si el modo fuego está activo
        if (state.speedTrapIsFireActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 8.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color(0x66FF3B30), Color(0xCCFF2D55))
                        ),
                        shape = RoundedCornerShape(0.dp)
                    )
            )
        }

        // 2. Partículas de Chispas Cómic (Yellow / Red)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val now = System.currentTimeMillis()
            for (spark in state.speedTrapSparks) {
                val age = (now - spark.timestamp).toFloat().coerceAtLeast(0f)
                val alpha = (1f - (age / 600f)).coerceIn(0f, 1f)
                val x = (spark.xNorm + spark.vx * (age / 35f)) * size.width
                val y = (spark.yNorm + spark.vy * (age / 35f)) * size.height
                val r = spark.sizeDp * (1f - (age / 900f))

                drawCircle(
                    color = Color(spark.colorHex).copy(alpha = alpha * sparkGlowAlpha),
                    radius = r.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // 3. Bocanadas de humo de cartón/manga en los botes rápidos
        for (smoke in state.speedTrapSmokePuffs) {
            Box(
                modifier = Modifier
                    .offset(
                        x = screenW * smoke.xNorm - 24.dp,
                        y = screenH * smoke.yNorm - 24.dp
                    )
                    .size((48 * smoke.scale).dp)
                    .rotate(smoke.rotation)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xDDEEEEEE), Color(0x88CCCCCC), Color.Transparent)
                        )
                    )
                }
            }
        }

        // 4. Popups Cómic de Onomatopeyas ("¡¡TURBO ON!!", "¡¡SPEED DEMON!!")
        for (popup in state.speedTrapComicPopups) {
            Box(
                modifier = Modifier
                    .offset(
                        x = (screenW * popup.xNorm - 80.dp).coerceAtLeast(10.dp),
                        y = (screenH * popup.yNorm).coerceAtLeast(80.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(popup.colorHex))
                        .border(3.dp, Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = popup.text,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 16.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // 5. Capa Principal del HUD de Carrera
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // BARRA SUPERIOR: Badge de Modo, Selector de Mano, STOP y Ajustes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Izquierda: Badge Manga SPEED TRAP
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF141418))
                            .border(2.dp, if (state.speedTrapIsFireActive) Color(0xFFFF3B30) else Color(0xFFFF9500), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (state.speedTrapIsFireActive) Icons.Default.LocalFireDepartment else Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (state.speedTrapIsFireActive) Color(0xFFFF3B30) else Color(0xFFFF9500),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "SPEED TRAP • RADAR BPM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = if (state.speedTrapIsFireActive) Color(0xFFFF3B30) else Color(0xFFFF9500)
                                )
                                Text(
                                    text = if (state.speedTrapIsFireActive) "🔥 ¡¡BOTE DE FUEGO!!" else state.speedTrapZone.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Centro-Derecha: Selector rápido de mano
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xDD1A1A22))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(999.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpeedTrapHand.values().forEach { hand ->
                        val isSelected = state.speedTrapHand == hand
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSelected) Color(0xFFEA580C) else Color.Transparent)
                                .clickable { onSetHand(hand) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (hand) {
                                    SpeedTrapHand.ANY -> "AMBAS"
                                    SpeedTrapHand.RIGHT -> "DERECHA"
                                    SpeedTrapHand.LEFT -> "IZQUIERDA"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.White else Color(0xAAFFFFFF)
                            )
                        }
                    }
                }

                // Derecha: STOP y Ajustes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE50914))
                            .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            .clickable { onExitToMain() }
                            .testTag("speed_trap_exit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Salir",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E283D))
                            .border(1.5.dp, Color(0x33446699), CircleShape)
                            .clickable { showSettingsDialog = true }
                            .testTag("speed_trap_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // CENTRO: TACÓMETRO DE CARRERAS DEPORTIVO Y BARRA DE RETO
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tacómetro semicircular estilo videojuego arcade
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .testTag("speed_trap_tachometer"),
                    contentAlignment = Alignment.Center
                ) {
                    RacingTachometerGauge(
                        currentBpm = state.speedTrapBpm,
                        targetBpm = state.speedTrapTargetBpm,
                        peakBpm = state.speedTrapPeakBpm,
                        isFireActive = state.speedTrapIsFireActive
                    )

                    // Centro del tacómetro: lectura digital grande de BPM
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            text = String.format("%.0f", state.speedTrapBpm),
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = when (state.speedTrapZone) {
                                SpeedTrapZone.IDLE -> Color(0xFF94A3B8)
                                SpeedTrapZone.COLD -> Color(0xFF60A5FA)
                                SpeedTrapZone.GREEN -> Color(0xFF34D399)
                                SpeedTrapZone.YELLOW -> Color(0xFFFBBF24)
                                SpeedTrapZone.RED_FIRE -> Color(0xFFFF3B30)
                            },
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "BPM (BOTES/MIN)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color(0xCCFFFFFF)
                        )
                        Text(
                            text = "RÉCORD: ${String.format("%.0f", state.speedTrapPeakBpm)} BPM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                // BARRA DE AGUANTE DE RETO (20 SEGUNDOS A MÁS DE 110 BPM)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xEE181822))
                        .border(2.dp, if (state.speedTrapIsFireActive) Color(0xFFFF3B30) else Color(0x44FFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (state.speedTrapIsFireActive) "🔥 RETO BOTE DE FUEGO:" else "🎯 OBJETIVO:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (state.speedTrapIsFireActive) Color(0xFFFF3B30) else Color(0xFFFF9500)
                                )
                                Text(
                                    text = "20s sobre ${state.speedTrapTargetBpm.toInt()} BPM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "${String.format("%.1f", state.speedTrapTimeHoldingSec)}s / ${state.speedTrapTargetSeconds}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (state.speedTrapIsFireActive) Color(0xFFFF3B30) else Color(0xFF34D399)
                            )
                        }

                        // Barra de progreso de aguante
                        val progress = (state.speedTrapTimeHoldingSec / state.speedTrapTargetSeconds).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF262634))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFF59E0B), Color(0xFFFF3B30), Color(0xFFFF0055))
                                        )
                                    )
                            )
                        }

                        // Indicador de gracia si pierde el ritmo
                        if (state.speedTrapTimeHoldingSec > 2.0f && !state.speedTrapIsFireActive && !state.speedTrapIsGameOver && !state.speedTrapIsVictory) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ ¡ACELERA! No bajes de ${state.speedTrapTargetBpm.toInt()} BPM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF3B30)
                                )
                                Text(
                                    text = "Gracia: ${String.format("%.1f", state.speedTrapRemainingGraceSec)}s",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF3B30)
                                )
                            }
                        }
                    }
                }
            }

            // BARRA INFERIOR: Botón de bote manual / simulador + Estadísticas de botes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info botes totales y mano
                Column {
                    Text(
                        text = "BOTES: ${state.speedTrapDribbleCount}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = state.speedTrapActiveHandLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xCCFFFFFF)
                    )
                }

                // Botón de simular bote (para testing rápido en streaming o en mano)
                Button(
                    onClick = { onTriggerBounce(0.5f, 0.75f) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.speedTrapIsFireActive) Color(0xFFFF3B30) else Color(0xFFEA580C)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("simulate_speed_trap_bounce")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "⚡", fontSize = 16.sp)
                        Text(
                            text = "BOTAR RÁPIDO (+BOTE)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                // Botón reiniciar reto
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA181822))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onRestartSession() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 6. Modal de Finalización y Compartir Vídeo (Con Hype justo)
        if (state.speedTrapIsVictory || state.speedTrapIsGameOver) {
            UniversalGameFinishedDialog(
                gameTitle = "Speed Trap (Bote a Fuego)",
                score = (state.speedTrapPeakBpm * 10).toInt(),
                scoreLabel = "PUNTOS",
                secondaryStatValue = "${state.speedTrapPeakBpm.toInt()}",
                secondaryStatLabel = "PEAK BPM",
                hypeReward = state.lastHypeReward,
                hasRecordedVideo = state.reactionRecordedVideoUri != null || state.lastRecordedVideoUri != null,
                selectedVideoFormat = state.selectedVideoShareFormat,
                recordingFormat = state.videoRecordingFormat,
                isMusicEnabled = state.isReactionVideoMusicEnabled,
                isGeneratingHighlight = state.isGeneratingHighlight,
                onRestart = { onRestartSession() },
                onExit = { onExitToMain() }
            )
        }
    }

    // Diálogo de ajustes
    if (showSettingsDialog) {
        VisionSettingsDialog(
            state = state,
            onDismiss = { showSettingsDialog = false },
            onToggleShowSkeleton = onToggleShowSkeleton,
            onToggleShowHoop = {},
            onToggleShowBall = onToggleShowBall,
            onSetHoopPerspective = {},
            onOpenSavedVideos = {},
            onToggleCamera = onToggleCamera,
            onToggleRecording = onToggleRecording,
            onExitDrill = onExitToMain,
            onSelectDribbleCombo = onSelectDribbleCombo,
            onSelectReactionPoints = onSelectReactionPoints,
            onSelectDefendZone = onSelectDefendZone,
            onSelectKidsMiniBasket = onSelectKidsMiniBasket,
            onSelectSpeedTrap = { onRestartSession() },
            onSelectShooting = onSelectShooting,
            onSelectUploadVideo = onSelectUploadVideo
        )
    }
}

/**
 * Tacómetro analógico semicircular estilo videojuego de carreras:
 * Verde (70-100 BPM), Amarillo (100-110 BPM), Rojo/Fuego (110-200+ BPM).
 */
@Composable
private fun RacingTachometerGauge(
    currentBpm: Float,
    targetBpm: Float,
    peakBpm: Float,
    isFireActive: Boolean
) {
    val animatedBpm by animateFloatAsState(
        targetValue = currentBpm.coerceIn(0f, 200f),
        animationSpec = tween(120, easing = LinearEasing),
        label = "tachometer_needle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f + 10.dp.toPx()
        val radius = size.width * 0.42f
        val strokeW = 16.dp.toPx()

        val startAngle = 150f
        val sweepTotal = 240f

        // 1. Arco de Fondo (Gris carbón)
        drawArc(
            color = Color(0xFF22222E),
            startAngle = startAngle,
            sweepAngle = sweepTotal,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // 2. Zonas: Azul (0-70), Verde (70-100), Amarillo (100-110), Rojo Fuego (110-200)
        // Azul / Frío: 0-70 BPM (35% del recorrido)
        val sweepCold = sweepTotal * (70f / 200f)
        drawArc(
            color = Color(0xFF3B82F6),
            startAngle = startAngle,
            sweepAngle = sweepCold,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Verde: 70-100 BPM (15% del recorrido)
        val sweepGreen = sweepTotal * (30f / 200f)
        drawArc(
            color = Color(0xFF10B981),
            startAngle = startAngle + sweepCold,
            sweepAngle = sweepGreen,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeW)
        )

        // Amarillo: 100-110 BPM (5% del recorrido)
        val sweepYellow = sweepTotal * (10f / 200f)
        drawArc(
            color = Color(0xFFF59E0B),
            startAngle = startAngle + sweepCold + sweepGreen,
            sweepAngle = sweepYellow,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeW)
        )

        // Rojo Fuego: 110-200 BPM (45% del recorrido)
        val sweepRed = sweepTotal * (90f / 200f)
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(Color(0xFFFF3B30), Color(0xFFFF0055), Color(0xFFFF4500))
            ),
            startAngle = startAngle + sweepCold + sweepGreen + sweepYellow,
            sweepAngle = sweepRed,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // 3. Marca / Indicador de meta (Target BPM)
        val targetFraction = (targetBpm / 200f).coerceIn(0f, 1f)
        val targetAngleRad = Math.toRadians((startAngle + sweepTotal * targetFraction).toDouble())
        val tx = cx + (radius + 12.dp.toPx()) * cos(targetAngleRad).toFloat()
        val ty = cy + (radius + 12.dp.toPx()) * sin(targetAngleRad).toFloat()
        drawCircle(
            color = Color(0xFFFFD700),
            radius = 5.dp.toPx(),
            center = Offset(tx, ty)
        )

        // 4. Aguja del Tacómetro
        val currentFraction = (animatedBpm / 200f).coerceIn(0f, 1f)
        val needleAngleRad = Math.toRadians((startAngle + sweepTotal * currentFraction).toDouble())
        val needleLength = radius * 0.85f
        val nx = cx + needleLength * cos(needleAngleRad).toFloat()
        val ny = cy + needleLength * sin(needleAngleRad).toFloat()

        // Línea de aguja con punta afilada
        drawLine(
            color = if (isFireActive) Color(0xFFFF3B30) else Color.White,
            start = Offset(cx, cy),
            end = Offset(nx, ny),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Punto pivote central
        drawCircle(
            color = if (isFireActive) Color(0xFFFF3B30) else Color(0xFFEA580C),
            radius = 9.dp.toPx(),
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}
