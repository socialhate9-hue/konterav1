package com.example.card

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.home.GameTrainToggle
import com.example.stats.PlayerStats
import com.example.ui.common.UserAvatarImage
import kotlin.math.cos
import kotlin.math.sin

/**
 * Estilos / Fondos desbloqueables de la Player Card según nivel de jugador.
 */
enum class PlayerCardStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val requiredLevel: Int,
    val requiredXp: Int,
    val accentColor: Color
) {
    CYBER_2K_PRISM(
        id = "cyber_2k",
        title = "2K Cyber Prism",
        subtitle = "Base • Nivel 1",
        iconEmoji = "🎴",
        requiredLevel = 1,
        requiredXp = 0,
        accentColor = Color(0xFF00E5FF)
    ),
    WORLD_CUP_GOLD(
        id = "cup_gold",
        title = "Copa Oro 2026",
        subtitle = "Desbloquea en Nivel 2",
        iconEmoji = "🏆",
        requiredLevel = 2,
        requiredXp = 750,
        accentColor = Color(0xFFFFD700)
    ),
    BLACK_FIRE_ELITE(
        id = "black_fire",
        title = "Black Fire MVP",
        subtitle = "Desbloquea en Nivel 3",
        iconEmoji = "🔥",
        requiredLevel = 3,
        requiredXp = 1500,
        accentColor = Color(0xFFFF5722)
    ),
    GOAT_EMERALD(
        id = "goat_emerald",
        title = "GOAT Legend",
        subtitle = "Desbloquea en Nivel 4",
        iconEmoji = "👑",
        requiredLevel = 4,
        requiredXp = 3000,
        accentColor = Color(0xFF10B981)
    )
}

/**
 * Silueta geométrica auténtica de la carta estilo FIFA FUT / NBA 2K.
 */
val CardShieldShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, h * 0.045f)
    cubicTo(w * 0.08f, 0f, w * 0.20f, 0f, w * 0.32f, h * 0.022f)
    cubicTo(w * 0.40f, h * 0.038f, w * 0.45f, 0f, w * 0.50f, 0f)
    cubicTo(w * 0.55f, 0f, w * 0.60f, h * 0.038f, w * 0.68f, h * 0.022f)
    cubicTo(w * 0.80f, 0f, w * 0.92f, 0f, w, h * 0.045f)
    lineTo(w, h * 0.77f)
    lineTo(w * 0.50f, h)
    lineTo(0f, h * 0.77f)
    lineTo(0f, h * 0.045f)
    close()
}

/**
 * Silueta geométrica para la edición Copa Oro 2026.
 */
val CupShieldShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.12f, 0f)
    lineTo(w * 0.88f, 0f)
    quadraticBezierTo(w, 0f, w, h * 0.07f)
    lineTo(w, h * 0.68f)
    cubicTo(w, h * 0.84f, w * 0.72f, h * 0.94f, w * 0.50f, h)
    cubicTo(w * 0.28f, h * 0.94f, 0f, h * 0.84f, 0f, h * 0.68f)
    lineTo(0f, h * 0.07f)
    quadraticBezierTo(0f, 0f, w * 0.12f, 0f)
    close()
}

/**
 * PANTALLA COMPLETA "MI CARTA" (Player Card Showcase):
 * Trofeo principal del jugador en alta resolución, brillo holográfico,
 * giro 3D interactivo para telemetría, selector de fondos desbloqueables por nivel,
 * y botón directo para compartir en WhatsApp / Instagram Stories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerCardShowcaseScreen(
    playerStats: PlayerStats,
    avatarUrl: String?,
    isGameMode: Boolean,
    onToggleMode: (Boolean) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFlipped by remember { mutableStateOf(false) }
    var currentStyle by remember { mutableStateOf(PlayerCardStyle.CYBER_2K_PRISM) }
    var showShareSheet by remember { mutableStateOf(false) }

    // Animación de giro 3D en el eje Y
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "showcase_card_rotation_3d"
    )

    // Shimmer holográfico infinito que recorre la carta
    val infiniteTransition = rememberInfiniteTransition(label = "player_card_showcase_shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_hologram"
    )

    // Cálculo dinámico de atributos
    val reactionVal = if (playerStats.reactionPointsBest > 0) {
        (76 + (playerStats.reactionPointsBest * 2)).coerceIn(75, 99)
    } else 88

    val shootingVal = if (playerStats.shootingShots > 0) {
        (65 + (playerStats.shootingMakes * 100 / playerStats.shootingShots * 0.35f).toInt()).coerceIn(68, 99)
    } else 74

    val dribbleVal = if (playerStats.dribbleComboBest > 0) {
        (80 + (playerStats.dribbleComboBest * 2)).coerceIn(78, 99)
    } else 91

    val streakVal = if (playerStats.streakDays > 0) playerStats.streakDays else 12
    val overallScore = ((reactionVal * 0.35f) + (shootingVal * 0.30f) + (dribbleVal * 0.35f)).toInt()

    val shareText = "🔥 Mi Carta Oficial de Jugador en HoopStars:\n" +
        "⭐ OVR: $overallScore · Base (PG) · ${playerStats.rankTitle}\n" +
        "⚡ Velocidad Reacción: $reactionVal\n" +
        "🎯 Puntería Tiro: $shootingVal\n" +
        "🏀 Control de Bote: $dribbleVal\n" +
        "🔥 Racha: $streakVal días\n" +
        "🏆 Nivel: ${playerStats.level} • ${playerStats.totalXp} Hype\n" +
        "¡Entrena y supera mis estadísticas de visión artificial en HoopStars!"

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isGameMode) Color(0xFFF8FAFC) else Color(0xFF0D1117))
            .testTag("player_card_showcase_screen")
    ) {
        // ==========================================
        // 1. CABECERA CON BOTÓN ATRÁS, BADGES Y SWITCH GAME/PRO
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isGameMode) Color(0xFFE2E8F0) else Color(0xFF1E2433))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver a Inicio",
                        tint = if (isGameMode) Color(0xFF0F172A) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Badges de Racha
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isGameMode) Color(0xFFFFF7ED) else Color(0xFF1E2433))
                        .border(1.dp, Color(0xFFFF6B1A).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Racha",
                            tint = Color(0xFFFF6B1A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${playerStats.streakDays}d",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isGameMode) Color(0xFFC2410C) else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Badges de Hype / XP
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isGameMode) Color(0xFFE0F7FA) else Color(0xFF1E2433))
                        .border(1.dp, Color(0xFF00BCD4).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isGameMode) {
                            Image(
                                painter = painterResource(id = R.drawable.cohete),
                                contentDescription = "Hype",
                                modifier = Modifier.size(15.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${playerStats.totalXp} Hype",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00838F)
                            )
                        } else {
                            Text(
                                text = "${playerStats.totalXp} XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }

            // Lado derecho: Switch GAME / PRO
            GameTrainToggle(
                isGameMode = isGameMode,
                onToggle = onToggleMode
            )
        }

        // ==========================================
        // CONTENIDO CON DESPLAZAMIENTO VERTICAL
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título de la pantalla
            Text(
                text = "MI CARTA DE JUGADOR",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = if (isGameMode) Color(0xFF0F172A) else Color.White,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "PLAYER CARD SHOWCASE • NIVEL ${playerStats.level}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 2. CARTA EN ALTA RESOLUCIÓN CON GIRO 3D
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.rotationY = rotationY
                        cameraDistance = 16f * density
                    }
                    .clickable { isFlipped = !isFlipped }
                    .testTag("showcase_player_card"),
                contentAlignment = Alignment.Center
            ) {
                if (rotationY <= 90f) {
                    // FRONTAL DE LA CARTA
                    when (currentStyle) {
                        PlayerCardStyle.CYBER_2K_PRISM -> {
                            CyberCardFrontView(
                                playerName = playerStats.playerName,
                                avatarUrl = avatarUrl,
                                overallScore = overallScore,
                                reactionVal = reactionVal,
                                shootingVal = shootingVal,
                                dribbleVal = dribbleVal,
                                streakVal = streakVal,
                                shimmerTranslate = shimmerTranslate
                            )
                        }
                        PlayerCardStyle.WORLD_CUP_GOLD -> {
                            GoldCupCardFrontView(
                                playerName = playerStats.playerName,
                                avatarUrl = avatarUrl,
                                overallScore = overallScore,
                                reactionVal = reactionVal,
                                shootingVal = shootingVal,
                                dribbleVal = dribbleVal,
                                streakVal = streakVal,
                                shimmerTranslate = shimmerTranslate
                            )
                        }
                        PlayerCardStyle.BLACK_FIRE_ELITE -> {
                            BlackFireCardFrontView(
                                playerName = playerStats.playerName,
                                avatarUrl = avatarUrl,
                                overallScore = overallScore,
                                reactionVal = reactionVal,
                                shootingVal = shootingVal,
                                dribbleVal = dribbleVal,
                                streakVal = streakVal,
                                shimmerTranslate = shimmerTranslate
                            )
                        }
                        PlayerCardStyle.GOAT_EMERALD -> {
                            GoatEmeraldCardFrontView(
                                playerName = playerStats.playerName,
                                avatarUrl = avatarUrl,
                                overallScore = overallScore,
                                reactionVal = reactionVal,
                                shootingVal = shootingVal,
                                dribbleVal = dribbleVal,
                                streakVal = streakVal,
                                shimmerTranslate = shimmerTranslate
                            )
                        }
                    }
                } else {
                    // TRASERA CON TELEMETRÍA (Giro corregido 180°)
                    Box(
                        modifier = Modifier.graphicsLayer { this.rotationY = 180f }
                    ) {
                        CardBackTelemetryView(
                            playerName = playerStats.playerName,
                            reactionVal = reactionVal,
                            shootingVal = shootingVal,
                            dribbleVal = dribbleVal,
                            streakVal = streakVal,
                            level = playerStats.level,
                            rankTitle = playerStats.rankTitle,
                            onFlipBack = { isFlipped = false }
                        )
                    }
                }
            }

            // Indicador de toque interactivo
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isGameMode) Color(0xFFF1F5F9) else Color(0xFF1E2433),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clickable { isFlipped = !isFlipped }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Girar",
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFlipped) "Toca para ver Frontal" else "Toca la carta para ver Telemetría 3D",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isGameMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // 3. SELECTOR DE FONDOS / ESTILOS DESBLOQUEABLES
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTILOS DESBLOQUEABLES",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = if (isGameMode) Color(0xFF0F172A) else Color.White
                    )
                    Text(
                        text = "NIVEL ${playerStats.level}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00BCD4)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(PlayerCardStyle.values()) { style ->
                        val isUnlocked = playerStats.level >= style.requiredLevel || playerStats.totalXp >= style.requiredXp
                        val isSelected = currentStyle == style

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                if (isGameMode) Color(0xFF0F172A) else Color(0xFF1E293B)
                            } else {
                                if (isGameMode) Color.White else Color(0xFF141926)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) style.accentColor else Color(0xFFE2E8F0).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .width(135.dp)
                                .clickable {
                                    if (isUnlocked) {
                                        currentStyle = style
                                    } else {
                                        Toast
                                            .makeText(
                                                context,
                                                "🔒 Requiere Nivel ${style.requiredLevel} o ${style.requiredXp} Hype para desbloquear",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = style.iconEmoji, fontSize = 20.sp)
                                    if (!isUnlocked) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Bloqueado",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Activo",
                                            tint = style.accentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = style.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) Color.White else if (isGameMode) Color(0xFF0F172A) else Color.White
                                )

                                Text(
                                    text = if (isUnlocked) "Desbloqueado" else "Nv. ${style.requiredLevel} Requerido",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isUnlocked) Color(0xFF10B981) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // 4. BOTÓN DIRECTO "COMPARTIR CARTA"
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Botón Girar
                Button(
                    onClick = { isFlipped = !isFlipped },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGameMode) Color(0xFF1E293B) else Color(0xFF1E2433)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFlipped) "Frontal" else "Girar 3D",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                // Botón destacado Hero de Compartir
                Button(
                    onClick = { showShareSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COMPARTIR CARTA",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    // ==========================================
    // MODAL BOTTOM SHEET DE COMPARTIR RÁPIDO
    // ==========================================
    if (showShareSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "COMPARTIR MI CARTA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Presume tu media OVR $overallScore y tus estadísticas oficiales",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 1. WhatsApp
                Button(
                    onClick = {
                        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage("com.whatsapp")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        try {
                            context.startActivity(whatsappIntent)
                        } catch (e: Exception) {
                            // Fallback al chooser general
                            val chooser = Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                },
                                "Compartir Ficha de Jugador"
                            )
                            context.startActivity(chooser)
                        }
                        showShareSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "💬 Enviar por WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Instagram Stories / General
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Publicar en Redes / Historia"))
                        showShareSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "📸 Historia / Redes Sociales", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Copiar al Portapapeles
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Ficha HoopStars", shareText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "¡Ficha de jugador copiada al portapapeles!", Toast.LENGTH_SHORT).show()
                        showShareSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "📋 Copiar Resumen de Ficha", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * ESTILO 1: 2K CYBER PRISM
 */
@Composable
private fun CyberCardFrontView(
    playerName: String,
    avatarUrl: String?,
    overallScore: Int,
    reactionVal: Int,
    shootingVal: Int,
    dribbleVal: Int,
    streakVal: Int,
    shimmerTranslate: Float
) {
    val cardWidth = 310.dp
    val cardHeight = 465.dp

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        contentAlignment = Alignment.Center
    ) {
        // Shards
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                color = Color(0x99C084FC),
                topLeft = Offset(size.width - 24.dp.toPx(), 4.dp.toPx()),
                size = Size(18.dp.toPx(), 22.dp.toPx())
            )
            drawRect(
                color = Color(0xBB00E5FF),
                topLeft = Offset(size.width - 8.dp.toPx(), 28.dp.toPx()),
                size = Size(12.dp.toPx(), 16.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CardShieldShape)
                .border(2.5.dp, Color(0xFF00E5FF), CardShieldShape)
                .background(Color(0xFF0B041A))
        ) {
            Image(
                painter = painterResource(id = R.drawable.fut_card_crystal_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x660F051D),
                                Color(0xDD0B041A),
                                Color(0xFF080214)
                            ),
                            startY = 200f
                        )
                    )
            )

            // Shimmer holográfico
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.07f),
                            Color(0xFF00E5FF).copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerTranslate - 100f, 0f),
                        end = Offset(shimmerTranslate + 100f, size.height)
                    )
                )
            }

            // OVR y PG
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 22.dp, top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$overallScore",
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = 46.sp,
                    letterSpacing = (-1.5).sp
                )
                Text(
                    text = "PG",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE2E8F0),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlagSnippet(modifier = Modifier.size(width = 28.dp, height = 18.dp))
            }

            // Busto del Jugador
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp, start = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fut_player_cutout),
                    contentDescription = playerName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Barra inferior con Nombre y Stats
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playerName.uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Canvas(modifier = Modifier.fillMaxWidth().height(1.5.dp)) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF00E5FF), Color.Transparent)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShowcaseStatPill(valStr = "$reactionVal", label = "REC")
                    ShowcaseStatPill(valStr = "$shootingVal", label = "SHT")
                    ShowcaseStatPill(valStr = "$dribbleVal", label = "DRI")
                    ShowcaseStatPill(valStr = "${streakVal}d", label = "STR")
                }
            }
        }
    }
}

/**
 * ESTILO 2: COPA ORO 2026 (GOLD CHAMPIONS)
 */
@Composable
private fun GoldCupCardFrontView(
    playerName: String,
    avatarUrl: String?,
    overallScore: Int,
    reactionVal: Int,
    shootingVal: Int,
    dribbleVal: Int,
    streakVal: Int,
    shimmerTranslate: Float
) {
    val cardWidth = 310.dp
    val cardHeight = 465.dp

    val goldBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFDF00),
            Color(0xFFB8860B),
            Color(0xFFFFE47A),
            Color(0xFF8B6508),
            Color(0xFFFFD700)
        )
    )

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CupShieldShape)
                .background(goldBorderBrush)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CupShieldShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1D4ED8),
                                Color(0xFF1E3A8A),
                                Color(0xFF0B194F)
                            ),
                            center = Offset(200f, 200f),
                            radius = 500f
                        )
                    )
            ) {
                // OVR y Posición
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 22.dp, top = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$overallScore",
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        lineHeight = 46.sp
                    )
                    Text(
                        text = "PG",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Busto del Jugador
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp, start = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.fut_player_cutout),
                        contentDescription = playerName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Shimmer
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x33FFD700),
                                Color.Transparent
                            ),
                            start = Offset(shimmerTranslate - 80f, 0f),
                            end = Offset(shimmerTranslate + 80f, size.height)
                        )
                    )
                }

                // Estadísticas inferiores
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = playerName.uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        letterSpacing = 1.5.sp,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ShowcaseStatPill(valStr = "$reactionVal", label = "REC", color = Color(0xFFFFD700))
                        ShowcaseStatPill(valStr = "$shootingVal", label = "SHT", color = Color(0xFFFFD700))
                        ShowcaseStatPill(valStr = "$dribbleVal", label = "DRI", color = Color(0xFFFFD700))
                        ShowcaseStatPill(valStr = "${streakVal}d", label = "STR", color = Color(0xFFFFD700))
                    }
                }
            }
        }
    }
}

/**
 * ESTILO 3: BLACK FIRE ELITE (LAVA & PARTICLES)
 */
@Composable
private fun BlackFireCardFrontView(
    playerName: String,
    avatarUrl: String?,
    overallScore: Int,
    reactionVal: Int,
    shootingVal: Int,
    dribbleVal: Int,
    streakVal: Int,
    shimmerTranslate: Float
) {
    val cardWidth = 310.dp
    val cardHeight = 465.dp

    val fireBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF3D00),
            Color(0xFFFF9100),
            Color(0xFFDD2C00),
            Color(0xFFFFD600)
        )
    )

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CardShieldShape)
                .border(2.5.dp, fireBorderBrush, CardShieldShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1F0804),
                            Color(0xFF0F0402),
                            Color(0xFF000000)
                        )
                    )
                )
        ) {
            // Embers de fuego en Canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(color = Color(0x44FF3D00), radius = 60.dp.toPx(), center = Offset(size.width * 0.8f, size.height * 0.3f))
                drawCircle(color = Color(0x33FF9100), radius = 40.dp.toPx(), center = Offset(size.width * 0.2f, size.height * 0.7f))
            }

            // OVR
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 22.dp, top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$overallScore",
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF6D00),
                    lineHeight = 46.sp
                )
                Text(
                    text = "MVP",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F)
                )
            }

            // Busto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp, start = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fut_player_cutout),
                    contentDescription = playerName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Shimmer
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33FF9100),
                            Color.Transparent
                        ),
                        start = Offset(shimmerTranslate - 80f, 0f),
                        end = Offset(shimmerTranslate + 80f, size.height)
                    )
                )
            }

            // Estadísticas
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playerName.uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9100),
                    letterSpacing = 1.5.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShowcaseStatPill(valStr = "$reactionVal", label = "REC", color = Color(0xFFFFAB40))
                    ShowcaseStatPill(valStr = "$shootingVal", label = "SHT", color = Color(0xFFFFAB40))
                    ShowcaseStatPill(valStr = "$dribbleVal", label = "DRI", color = Color(0xFFFFAB40))
                    ShowcaseStatPill(valStr = "${streakVal}d", label = "STR", color = Color(0xFFFFAB40))
                }
            }
        }
    }
}

/**
 * ESTILO 4: GOAT EMERALD LEGEND
 */
@Composable
private fun GoatEmeraldCardFrontView(
    playerName: String,
    avatarUrl: String?,
    overallScore: Int,
    reactionVal: Int,
    shootingVal: Int,
    dribbleVal: Int,
    streakVal: Int,
    shimmerTranslate: Float
) {
    val cardWidth = 310.dp
    val cardHeight = 465.dp

    val emeraldBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00E676),
            Color(0xFF00B0FF),
            Color(0xFF1DE9B6),
            Color(0xFF69F0AE)
        )
    )

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CardShieldShape)
                .border(2.5.dp, emeraldBorderBrush, CardShieldShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF022B18),
                            Color(0xFF011A0E),
                            Color(0xFF000A05)
                        )
                    )
                )
        ) {
            // OVR
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 22.dp, top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$overallScore",
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF69F0AE),
                    lineHeight = 46.sp
                )
                Text(
                    text = "GOAT",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E676)
                )
            }

            // Busto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp, start = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fut_player_cutout),
                    contentDescription = playerName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Shimmer
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x4400E676),
                            Color.Transparent
                        ),
                        start = Offset(shimmerTranslate - 80f, 0f),
                        end = Offset(shimmerTranslate + 80f, size.height)
                    )
                )
            }

            // Estadísticas
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playerName.uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF69F0AE),
                    letterSpacing = 1.5.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShowcaseStatPill(valStr = "$reactionVal", label = "REC", color = Color(0xFFB9F6CA))
                    ShowcaseStatPill(valStr = "$shootingVal", label = "SHT", color = Color(0xFFB9F6CA))
                    ShowcaseStatPill(valStr = "$dribbleVal", label = "DRI", color = Color(0xFFB9F6CA))
                    ShowcaseStatPill(valStr = "${streakVal}d", label = "STR", color = Color(0xFFB9F6CA))
                }
            }
        }
    }
}

/**
 * CARA TRASERA DE LA CARTA: Desglose completo de telemetría, visión por computador y radar
 */
@Composable
private fun CardBackTelemetryView(
    playerName: String,
    reactionVal: Int,
    shootingVal: Int,
    dribbleVal: Int,
    streakVal: Int,
    level: Int,
    rankTitle: String,
    onFlipBack: () -> Unit
) {
    val cardWidth = 310.dp
    val cardHeight = 465.dp

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(CardShieldShape)
            .border(2.5.dp, Color(0xFF00E5FF), CardShieldShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF090D16),
                        Color(0xFF04060A)
                    )
                )
            )
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TELEMETRÍA OFICIAL IA",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00E5FF),
                letterSpacing = 1.sp
            )
            Text(
                text = "$playerName • $rankTitle (NIVEL $level)",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Radar Spider Chart
            RadarChartSnippet(
                reaction = reactionVal,
                shooting = shootingVal,
                dribble = dribbleVal,
                defense = 82,
                physical = 85,
                modifier = Modifier.size(130.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Desglose de telemetría de sensores
            TelemetryMetricItem(label = "Velocidad de Reacción", value = "$reactionVal", detail = "Detector MediaPipe 60 FPS")
            TelemetryMetricItem(label = "Puntería de Tiro", value = "$shootingVal", detail = "Arco y release calibrados")
            TelemetryMetricItem(label = "Control y Bote", value = "$dribbleVal", detail = "Cadencia y crossovers")
            TelemetryMetricItem(label = "Racha Activa", value = "$streakVal días", detail = "Constancia deportiva")

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onFlipBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "VOLVER AL FRONTAL",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricItem(label: String, value: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = detail, fontSize = 8.sp, color = Color(0xFF64748B))
        }
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E5FF))
    }
}

@Composable
private fun ShowcaseStatPill(valStr: String, label: String, color: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = valStr, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
    }
}

@Composable
private fun FlagSnippet(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFB91C1C))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFFB91C1C)))
            Box(modifier = Modifier.fillMaxWidth().weight(2f).background(Color(0xFFFBBF24)))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFFB91C1C)))
        }
    }
}

@Composable
private fun RadarChartSnippet(
    reaction: Int,
    shooting: Int,
    dribble: Int,
    defense: Int,
    physical: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 10.dp.toPx()
        val numAxes = 5
        val angleStep = (2 * Math.PI / numAxes).toFloat()

        // Red concéntrica
        for (step in 1..3) {
            val r = radius * (step / 3f)
            val path = Path()
            for (i in 0 until numAxes) {
                val angle = (i * angleStep - Math.PI / 2).toFloat()
                val x = center.x + r * cos(angle)
                val y = center.y + r * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = Color(0x3338BDF8), style = Stroke(width = 1.dp.toPx()))
        }

        // Datos
        val values = listOf(
            reaction / 100f,
            shooting / 100f,
            dribble / 100f,
            defense / 100f,
            physical / 100f
        )
        val dataPath = Path()
        for (i in 0 until numAxes) {
            val r = radius * values[i]
            val angle = (i * angleStep - Math.PI / 2).toFloat()
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        drawPath(dataPath, color = Color(0x4400E5FF), style = Fill)
        drawPath(dataPath, color = Color(0xFF00E5FF), style = Stroke(width = 2.dp.toPx()))
    }
}
