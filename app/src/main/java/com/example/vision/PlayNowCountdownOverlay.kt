package com.example.vision

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Overlay flotante minimalista sobre la cámara (sin bordes llamativos, sin neón).
 *
 * Características:
 * - Icono de cerrar (X) integrado dentro del popup arriba a la derecha.
 * - 3 casillas interactivas que se activan en verde de forma independiente:
 *   1. "Gira pantalla": se activa en verde al rotar a horizontal.
 *   2. "Apóyalo": se activa en verde cuando el sensor detecta que el móvil está quieto y estable.
 *   3. "Aléjate 2m": se activa en verde cuando detecta el cuerpo a ~2 metros de distancia.
 * - Sin badges redundantes ni texto sobrante.
 * - Botón "¡JUGAR AHORA!" en el azul deportivo característico (Color(0xFF2FB2C9)),
 *   habilitado en cuanto el móvil está en su orientación correcta.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayNowCountdownOverlay(
    isAwaitingPlayStart: Boolean,
    countdownSec: Int?,
    isGameMode: Boolean,
    isReactionPointsMode: Boolean = false,
    skeleton: PoseSkeleton? = null,
    isPlayerTooClose: Boolean = false,
    onPlayNow: () -> Unit,
    onExitToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isAwaitingPlayStart && countdownSec == null) return

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    // Detección de estabilidad del teléfono (Casilla 2: "Apóyalo")
    var isPhoneStill by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastTimestamp = 0L
        var stillDurationMs = 0L
        var lastX = 0f
        var lastY = 0f
        var lastZ = 0f
        var hasSample = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val now = System.currentTimeMillis()
                val dt = if (lastTimestamp == 0L) 80L else (now - lastTimestamp)
                if (dt < 70L) return
                lastTimestamp = now

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                if (!hasSample) {
                    lastX = x
                    lastY = y
                    lastZ = z
                    hasSample = true
                    return
                }

                val dx = kotlin.math.abs(x - lastX)
                val dy = kotlin.math.abs(y - lastY)
                val dz = kotlin.math.abs(z - lastZ)
                val delta = dx + dy + dz

                lastX = x
                lastY = y
                lastZ = z

                // Umbral de estabilidad (móvil apoyado/fijo sin temblores de mano)
                if (delta < 0.45f) {
                    stillDurationMs += dt
                    if (stillDurationMs >= 400L) {
                        isPhoneStill = true
                    }
                } else {
                    stillDurationMs = 0L
                    isPhoneStill = false
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        } else {
            // Si el dispositivo/emulador carece de acelerómetro, se asume estable
            isPhoneStill = true
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Detección de distancia a 2 metros (Casilla 3: "Aléjate 2m")
    val isDistanceReady = remember(skeleton, isPlayerTooClose) {
        if (skeleton != null && skeleton.landmarks.isNotEmpty()) {
            val lShoulder = skeleton.landmarks.getOrNull(11)
            val rShoulder = skeleton.landmarks.getOrNull(12)
            val hasShoulders = lShoulder != null && rShoulder != null &&
                    lShoulder.visibility > 0.3f && rShoulder.visibility > 0.3f
            val shoulderDist = if (hasShoulders) {
                kotlin.math.hypot(lShoulder!!.x - rShoulder!!.x, lShoulder.y - rShoulder.y)
            } else 0f

            val lHip = skeleton.landmarks.getOrNull(23)
            val rHip = skeleton.landmarks.getOrNull(24)
            val hasHips = lHip != null && rHip != null &&
                    (lHip.visibility > 0.3f || rHip.visibility > 0.3f)

            // A 2 metros de distancia, el cuerpo se detecta completo y no está pegado a la cámara
            (!isPlayerTooClose) && (
                (hasShoulders && shoulderDist in 0.04f..0.32f) ||
                (hasHips && (!hasShoulders || shoulderDist <= 0.34f))
            )
        } else {
            false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("play_now_countdown_overlay")
    ) {
        if (countdownSec != null) {
            // Cuenta regresiva limpia: números gigantes blancos sobre fondo oscuro
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .testTag("countdown_overlay_active"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = countdownSec,
                        transitionSpec = {
                            (scaleIn(initialScale = 1.35f, animationSpec = tween(300)) + fadeIn(animationSpec = tween(200))) with
                                (scaleOut(targetScale = 0.65f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(200)))
                        },
                        label = "countdown_number_anim"
                    ) { sec ->
                        Text(
                            text = "$sec",
                            fontSize = 140.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("countdown_number_$sec")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "¡PREPÁRATE!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (isAwaitingPlayStart) {
            // Fondo semitransparente sutil sin líneas de borde
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
                    .testTag("awaiting_play_start_overlay"),
                contentAlignment = Alignment.Center
            ) {
                // Popup flotante limpio, sutil y sin contorno
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 460.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xE6111827))
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                        .testTag("minimal_setup_popup"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cabecera con título y botón de cierre dentro del popup a la derecha
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "COLOCA EL MÓVIL",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Botón de salir dentro del popup a la derecha arriba
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .clickable { onExitToMain() }
                                .testTag("exit_play_now_to_main_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color(0xCCFFFFFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Fila de 3 casillas interactivas: se activan en verde según cada acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Casilla 1: Gira pantalla (se activa en verde al rotar a horizontal)
                        SetupStepBox(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.ScreenRotation,
                            title = "Gira pantalla",
                            subtitle = if (isLandscape) "Listo" else "Horizontal",
                            isActivated = isLandscape,
                            tag = "step_box_rotation"
                        )

                        // Casilla 2: Apóyalo (se activa en verde cuando el móvil está fijo y no se mueve)
                        SetupStepBox(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Smartphone,
                            title = "Apóyalo",
                            subtitle = if (isPhoneStill) "Fijo" else "Suelo o mesa",
                            isActivated = isPhoneStill,
                            tag = "step_box_still"
                        )

                        // Casilla 3: Aléjate 2m (se activa en verde al colocarse a 2 metros de distancia)
                        SetupStepBox(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Person,
                            title = "Aléjate 2m",
                            subtitle = if (isDistanceReady) "Cuerpo entero" else "2 metros",
                            isActivated = isDistanceReady,
                            tag = "step_box_distance"
                        )
                    }

                    // Botón "¡JUGAR AHORA!" en el azul deportivo original (Color(0xFF2FB2C9))
                    val blueGradient = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF2FB2C9),
                            Color(0xFF0F869B)
                        )
                    )

                    if (isLandscape) {
                        // Habilitado en cuanto el móvil está en su posición correcta (horizontal)
                        val infiniteTransition = rememberInfiniteTransition(label = "btn_ready_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.04f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "btn_ready_pulse_scale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(pulseScale)
                                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x662FB2C9), spotColor = Color(0x880F869B))
                                .clip(RoundedCornerShape(16.dp))
                                .background(blueGradient)
                                .clickable { onPlayNow() }
                                .padding(vertical = 15.dp)
                                .testTag("play_now_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = if (isGameMode) "¡JUGAR AHORA!" else "¡ENTRENAR AHORA!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Bloqueado hasta girar el móvil a horizontal
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1FFFFFFF))
                                .padding(vertical = 15.dp)
                                .testTag("play_now_disabled_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = null,
                                    tint = Color(0x88FFFFFF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "GIRA EL MÓVIL PARA JUGAR",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0x88FFFFFF)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Casilla de paso interactiva:
 * - Cuando la acción se cumple, se ilumina en verde con icono de check.
 * - Diseño limpio, sin bordes llamativos.
 */
@Composable
private fun SetupStepBox(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isActivated: Boolean,
    tag: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isActivated) Color(0x3316A34A) else Color(0x14FFFFFF)
            )
            .padding(vertical = 12.dp, horizontal = 6.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (isActivated) Color(0x3322C55E) else Color(0x1AFFFFFF)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActivated) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (isActivated) Color(0xFF22C55E) else Color(0xCCFFFFFF),
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActivated) Color(0xFF86EFAC) else Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            fontSize = 10.sp,
            fontWeight = if (isActivated) FontWeight.Bold else FontWeight.Normal,
            color = if (isActivated) Color(0xFF4ADE80) else Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}
