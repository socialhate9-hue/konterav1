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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
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
 * Overlay a pantalla completa sobre la cámara activa (sin marcos cerrados ni neón).
 *
 * Flujo:
 * 1. MÓVIL EN VERTICAL:
 *    - Pantalla completa oscura con el icono de rotar pantalla + textos concisos.
 *    - Botón de salir (X) arriba a la derecha.
 *
 * 2. MÓVIL EN HORIZONTAL:
 *    - Dos casillas:
 *      1. "Gira pantalla": activada en verde (✓).
 *      2. "Apóyalo": se activa en verde (✓) mediante acelerómetro cuando el móvil está quieto.
 *    - Botón "¡JUGAR AHORA!" en azul deportivo: DESHABILITADO hasta que ambas casillas estén en check.
 *    - Al pulsar el botón: arranca de inmediato la cuenta atrás de 5 segundos con voz.
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

    // 1. Detección de estabilidad del teléfono (Casilla 2: "Apóyalo")
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

                // Móvil apoyado / fijo sin temblores de mano (durante al menos 350ms)
                if (delta < 0.40f) {
                    stillDurationMs += dt
                    if (stillDurationMs >= 350L) {
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
            isPhoneStill = true
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
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
            // Fondo oscuro opaco a pantalla completa sobre la cámara activa
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xC4000000))
                    .testTag("awaiting_play_start_overlay")
            ) {
                // Botón de salir / cerrar (X) arriba a la derecha
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 20.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable {
                            onExitToMain()
                        }
                        .testTag("exit_play_now_to_main_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar y volver al inicio",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (!isLandscape) {
                    // ==========================================
                    // PASO 1: MÓVIL EN VERTICAL
                    // Solo el icono de rotar pantalla + texto
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "rotation_icon_pulse")
                        val iconScale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.08f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "rotation_scale"
                        )

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .scale(iconScale)
                                .clip(CircleShape)
                                .background(Color(0x26FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Girar pantalla en horizontal",
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "COLOCA EL MÓVIL",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Gira la pantalla en horizontal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // ==========================================
                    // PASO 2: MÓVIL EN HORIZONTAL
                    // 2 casillas + botón bloqueado hasta cumplir ambas
                    // Al pulsar -> Directo a onPlayNow() (cuenta atrás 5 seg)
                    // ==========================================
                    val isReadyToPress = isLandscape && isPhoneStill

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "COLOCA EL MÓVIL",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Fila con las 2 casillas de preparación
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Casilla 1: Gira pantalla (ya en horizontal -> ✓ verde)
                            FloatingCheckItem(
                                icon = Icons.Default.ScreenRotation,
                                title = "Gira pantalla",
                                subtitle = "Listo",
                                isActivated = true,
                                tag = "step_box_rotation"
                            )

                            // Casilla 2: Apóyalo (acelerómetro quieto -> ✓ verde)
                            FloatingCheckItem(
                                icon = Icons.Default.Smartphone,
                                title = "Apóyalo",
                                subtitle = if (isPhoneStill) "Fijo" else "Suelo o mesa",
                                isActivated = isPhoneStill,
                                tag = "step_box_still"
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Botón "¡JUGAR AHORA!": DESHABILITADO hasta que las 2 casillas estén en check
                        if (isReadyToPress) {
                            val infiniteTransition = rememberInfiniteTransition(label = "btn_bouncer")
                            val bounceScale by infiniteTransition.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 1.06f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bouncer_scale"
                            )
                            val bounceOffsetY by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = -4f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bouncer_offset_y"
                            )

                            val buttonGradient = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF2FB2C9),
                                    Color(0xFF0F869B)
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .offset(y = bounceOffsetY.dp)
                                    .scale(bounceScale)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = RoundedCornerShape(50),
                                        ambientColor = Color(0x662FB2C9),
                                        spotColor = Color(0x990F869B)
                                    )
                                    .clip(RoundedCornerShape(50))
                                    .background(buttonGradient)
                                    .clickable {
                                        onPlayNow()
                                    }
                                    .padding(horizontal = 38.dp, vertical = 18.dp)
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
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = if (isGameMode) "¡JUGAR AHORA!" else "¡ENTRENAR AHORA!",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            // Modo no activo / deshabilitado hasta apoyar el móvil
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0x1AFFFFFF))
                                    .padding(horizontal = 32.dp, vertical = 16.dp)
                                    .testTag("play_now_disabled_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "APOYA EL MÓVIL PARA JUGAR",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0x77FFFFFF)
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
 * Item de check flotante sobre el fondo oscuro de pantalla completa.
 */
@Composable
private fun FloatingCheckItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isActivated: Boolean,
    tag: String,
    customWidth: androidx.compose.ui.unit.Dp = 120.dp
) {
    Column(
        modifier = Modifier
            .width(customWidth)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActivated) Color(0x3316A34A) else Color(0x22FFFFFF)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (isActivated) Color(0x3322C55E) else Color(0x1EFFFFFF)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActivated) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (isActivated) Color(0xFF22C55E) else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActivated) Color(0xFF86EFAC) else Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            fontSize = 10.5.sp,
            fontWeight = if (isActivated) FontWeight.Bold else FontWeight.Normal,
            color = if (isActivated) Color(0xFF4ADE80) else Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}
