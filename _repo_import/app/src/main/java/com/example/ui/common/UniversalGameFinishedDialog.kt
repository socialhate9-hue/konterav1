package com.example.ui.common

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.stats.HypeRewardBreakdown
import com.example.vision.ReactionVideoShareFormat
import com.example.vision.VideoRecordingFormat

// Colores del Modo Game de la app
private val GameOrange = Color(0xFFE15F25)   // Rookie / Botón principal
private val GameCyan = Color(0xFF00ACC1)     // Streak / Aciertos
private val GameBlue = Color(0xFF1E88E5)     // Hype / Nivel
private val TextDark = Color(0xFF0F172A)     // Título negro deportivo
private val TextMuted = Color(0xFF64748B)    // Gris de subtítulo y labels
private val BgCardLight = Color(0xFFF8FAFC)  // Fondo de tarjetas secundarias
private val BorderLight = Color(0xFFE2E8F0)  // Bordes limpios y sutiles

/**
 * Popup Universal y Moderno de Final de Partida.
 *
 * Sigue estrictamente la línea de diseño claro, plano y deportivo del Modo Game:
 * - Fondo blanco limpio con esquinas redondeadas generosas.
 * - Las 3 tarjetas características del Modo Game (Naranja #E15F25, Cyan #00ACC1, Azul Real #1E88E5).
 * - Botones planos, sólidos, sin sombras de neón ni degradados fluorescentes.
 * - Flujo en 2 pasos: 1. Resumen & Hype, 2. Compartir Vídeo.
 */
@Composable
fun UniversalGameFinishedDialog(
    gameTitle: String,
    score: Int,
    scoreLabel: String = "PUNTOS",
    secondaryStatValue: String? = null,
    secondaryStatLabel: String? = null,
    hypeReward: HypeRewardBreakdown?,
    // Opciones de vídeo y compartir
    hasRecordedVideo: Boolean = false,
    selectedVideoFormat: ReactionVideoShareFormat = ReactionVideoShareFormat.HIGHLIGHTS,
    recordingFormat: VideoRecordingFormat = VideoRecordingFormat.VERTICAL,
    isMusicEnabled: Boolean = true,
    isGeneratingHighlight: Boolean = false,
    onSelectVideoFormat: (ReactionVideoShareFormat) -> Unit = {},
    onSelectRecordingFormat: (VideoRecordingFormat) -> Unit = {},
    onToggleMusic: () -> Unit = {},
    onShareVideo: (Context, String) -> Unit = { _, _ -> },
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) } // 1: Resumen & Hype, 2: Vídeo & Compartir

    val earnedHype = hypeReward?.totalHypeEarned ?: (score / 2).coerceAtLeast(15)
    val isRecord = hypeReward?.isNewRecord == true
    val leveledUp = hypeReward?.leveledUp == true

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Fondo semi-transparente oscuro suave para enfocar el diálogo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x77000000))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Contenedor blanco principal del diálogo estilo Modo Game
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("universal_game_finished_dialog"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // 1. Selector plano de pasos superior (1. RESUMEN / 2. COMPARTIR VÍDEO)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FlatStepTab(
                            title = "1. RESUMEN",
                            icon = Icons.Default.SportsBasketball,
                            isSelected = currentStep == 1,
                            modifier = Modifier.weight(1f),
                            onClick = { currentStep = 1 }
                        )

                        FlatStepTab(
                            title = "2. COMPARTIR",
                            icon = Icons.Default.Videocam,
                            isSelected = currentStep == 2,
                            modifier = Modifier.weight(1f),
                            onClick = { currentStep = 2 }
                        )
                    }

                    // Encabezado del diálogo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isRecord) "¡NUEVO RÉCORD!" else "¡PARTIDAZO!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = gameTitle.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // =========================================================================
                    // CONTENIDO DEL PASO 1: RESUMEN DE PUNTOS Y HYPE
                    // =========================================================================
                    if (currentStep == 1) {

                        // 3 TARJETAS IDÉNTICAS AL MODO GAME DE LA APP (Naranja, Cyan, Azul)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Tarjeta 1: PUNTOS (Color Naranja #E15F25 con Rookie Shield)
                            GameSummaryCard(
                                value = "$score",
                                label = scoreLabel.uppercase(),
                                bgColor = GameOrange,
                                iconRes = R.drawable.rookie,
                                modifier = Modifier.weight(1f)
                            )

                            // Tarjeta 2: ACIERTOS / STAT SECUNDARIO (Color Cyan #00ACC1 con Fuego)
                            val secVal = secondaryStatValue ?: "${(score / 15).coerceAtLeast(1)}"
                            val secLbl = (secondaryStatLabel ?: "ACIERTOS").uppercase()
                            GameSummaryCard(
                                value = secVal,
                                label = secLbl,
                                bgColor = GameCyan,
                                iconRes = R.drawable.fuego,
                                modifier = Modifier.weight(1f)
                            )

                            // Tarjeta 3: HYPE (Color Azul #1E88E5 con Cohete)
                            GameSummaryCard(
                                value = "+$earnedHype",
                                label = "HYPE",
                                bgColor = GameBlue,
                                iconRes = R.drawable.cohete,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Banner de Nuevo Récord o Subida de Nivel si aplica
                        if (isRecord || leveledUp) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (leveledUp) Color(0xFFEFF6FF) else Color(0xFFFFF7ED))
                                    .border(
                                        1.dp,
                                        if (leveledUp) Color(0xFFBFDBFE) else Color(0xFFFED7AA),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (leveledUp) "🚀" else "🏆",
                                    fontSize = 22.sp
                                )
                                Column {
                                    Text(
                                        text = if (leveledUp) "¡SUBIDA DE NIVEL!" else "¡MEJOR MARCA PERSONAL!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (leveledUp) GameBlue else GameOrange
                                    )
                                    Text(
                                        text = if (leveledUp)
                                            "Has alcanzado el Nivel ${hypeReward?.newLevel ?: 2} en tu carrera"
                                        else
                                            "Superaste tu récord anterior en esta sesión",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        // Tarjeta limpia con el desglose de Hype
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BgCardLight)
                                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DESGLOSE DE HYPE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )

                            if (hypeReward != null) {
                                CleanBreakdownRow(
                                    label = "Base de entrenamiento",
                                    value = "+${hypeReward.baseEffortHype} Hype"
                                )
                                CleanBreakdownRow(
                                    label = "Rendimiento y aciertos",
                                    value = "+${hypeReward.performanceHype} Hype"
                                )
                                if (hypeReward.accuracyOrComboBonusHype > 0) {
                                    CleanBreakdownRow(
                                        label = "Bonus de combo / racha",
                                        value = "+${hypeReward.accuracyOrComboBonusHype} Hype",
                                        valueColor = Color(0xFF16A34A)
                                    )
                                }
                                if (isRecord) {
                                    CleanBreakdownRow(
                                        label = "Bonus nuevo récord",
                                        value = "+${hypeReward.recordBonusHype} Hype",
                                        valueColor = GameOrange
                                    )
                                }
                            } else {
                                CleanBreakdownRow(
                                    label = "Puntos conseguidos",
                                    value = "$score pts"
                                )
                                CleanBreakdownRow(
                                    label = "Hype total ganado",
                                    value = "+$earnedHype Hype",
                                    valueColor = GameBlue
                                )
                            }
                        }

                        // Botón plano principal para pasar a Compartir Vídeo
                        Button(
                            onClick = { currentStep = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("game_finished_next_step_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GameOrange,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "COMPARTIR VÍDEO & JUGADAS",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                    } else {
                        // =========================================================================
                        // CONTENIDO DEL PASO 2: COMPARTIR VÍDEO CON ESTILO LIMPIO Y PLANO
                        // =========================================================================

                        // Cabecera de vídeo y aviso de marca de agua
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
                                    tint = GameOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Vídeo del Entrenamiento",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark
                                )
                            }

                            // Badge plano de marca de agua
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFF7ED))
                                    .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "KANTERA AI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GameOrange,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Aviso de privacidad limpio
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF0FDF4))
                                .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Tu vídeo solo se guarda o comparte si pulsas un botón. Si sales, se borra de la memoria temporal.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF166534),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Selector de Formato: Highlights vs Vídeo Íntegro
                        val isHighlight = selectedVideoFormat == ReactionVideoShareFormat.HIGHLIGHTS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CleanOptionCard(
                                title = "🔥 Highlights",
                                subtitle = "Mejores botes & combos",
                                isSelected = isHighlight,
                                activeColor = GameOrange,
                                modifier = Modifier.weight(1f),
                                onClick = { onSelectVideoFormat(ReactionVideoShareFormat.HIGHLIGHTS) }
                            )

                            CleanOptionCard(
                                title = "🎬 Vídeo Íntegro",
                                subtitle = "Sesión completa",
                                isSelected = !isHighlight,
                                activeColor = GameBlue,
                                modifier = Modifier.weight(1f),
                                onClick = { onSelectVideoFormat(ReactionVideoShareFormat.FULL_VIDEO) }
                            )
                        }

                        // Selector de Orientación: Vertical 9:16 (Reels/TikTok) vs Horizontal 16:9
                        val isHorizontal = recordingFormat == VideoRecordingFormat.HORIZONTAL
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(BgCardLight)
                                .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Formato:",
                                fontSize = 12.sp,
                                color = TextDark,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = !isHorizontal,
                                    onClick = { onSelectRecordingFormat(VideoRecordingFormat.VERTICAL) },
                                    label = { Text("📱 Vertical 9:16", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GameOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = isHorizontal,
                                    onClick = { onSelectRecordingFormat(VideoRecordingFormat.HORIZONTAL) },
                                    label = { Text("🖥️ Horizontal 16:9", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GameOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Toggle de Música de Fondo
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(BgCardLight)
                                .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isMusicEnabled) GameOrange else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Música de fondo en el vídeo",
                                    fontSize = 12.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Switch(
                                checked = isMusicEnabled,
                                onCheckedChange = { onToggleMusic() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GameOrange,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }

                        // Indicador si está preparando el Highlight
                        if (isGeneratingHighlight) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = GameOrange,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Preparando vídeo y marca de agua...",
                                    fontSize = 12.sp,
                                    color = GameOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Botones de Redes Sociales: WhatsApp e Instagram
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // WhatsApp plano
                            Button(
                                onClick = { onShareVideo(context, "whatsapp") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("reaction_share_whatsapp_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                enabled = !isGeneratingHighlight
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "WhatsApp",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Instagram plano
                            Button(
                                onClick = { onShareVideo(context, "instagram") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("reaction_share_instagram_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                enabled = !isGeneratingHighlight
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Instagram",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Botón plano para guardar en galería u otras apps
                        OutlinedButton(
                            onClick = { onShareVideo(context, "general") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("reaction_share_general_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                            enabled = !isGeneratingHighlight
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = TextDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Guardar en Galería / Más apps",
                                    fontSize = 12.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Enlace para volver a ver los puntos
                        TextButton(
                            onClick = { currentStep = 1 },
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "← Volver a ver mis puntos y Hype",
                                fontSize = 12.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // =========================================================================
                    // BOTONES FINALES DE ACCIÓN: REPETIR / SALIR AL MENÚ
                    // =========================================================================
                    HorizontalDivider(color = BorderLight, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Botón Repetir (Plano Naranja Modo Game)
                        Button(
                            onClick = onRestart,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("universal_game_restart_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GameOrange,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "REPETIR",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Botón Salir (Plano Gris claro con texto oscuro)
                        Button(
                            onClick = onExit,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("universal_game_exit_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = TextDark
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = "SALIR",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pestaña plana para cambiar entre el Paso 1 (Resumen) y el Paso 2 (Compartir).
 */
@Composable
private fun FlatStepTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) GameOrange else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = if (isSelected) TextDark else TextMuted
            )
        }
    }
}

/**
 * Tarjeta idéntica a las 3 tarjetas de cabecera del Modo Game (Rookie, Fuego, Cohete).
 */
@Composable
private fun GameSummaryCard(
    value: String,
    label: String,
    bgColor: Color,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // Cuerpo de la tarjeta redondeada
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(82.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(bgColor)
                .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xEEFFFFFF),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.3.sp
                )
            }
        }

        // Insignia / Imagen oficial que sobresale en la parte superior
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(width = 36.dp, height = 40.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Tarjeta de opción plana para la selección de formato de vídeo.
 */
@Composable
private fun CleanOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) activeColor else BgCardLight)
            .border(
                1.dp,
                if (isSelected) activeColor else BorderLight,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color.White else TextDark
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = if (isSelected) Color(0xEEFFFFFF) else TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Fila limpia para el desglose del Hype.
 */
@Composable
private fun CleanBreakdownRow(
    label: String,
    value: String,
    valueColor: Color = GameBlue
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF334155),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = valueColor,
            fontWeight = FontWeight.Black
        )
    }
}
