package com.example.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.stats.PlayerStats
import com.example.ui.common.UserAvatarImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pasos del flujo guiado (Wizard 1-2-3) para crear una batalla de forma sencilla
 */
enum class BattleWizardStep(val stepNumber: Int, val title: String, val icon: ImageVector) {
    CHOOSE_GAME(1, "1. Juego", Icons.Filled.SportsBasketball),
    CHOOSE_RIVAL(2, "2. Rival", Icons.Filled.Person),
    PREVIEW_PLAY(3, "3. ¡Duelo!", Icons.Filled.Bolt)
}

/**
 * Tipos de rivales para el paso 2
 */
enum class RivalCategory(val title: String, val icon: ImageVector) {
    FRIENDS("Amigos", Icons.Filled.Group),
    BLUETOOTH("Cerca (Bluetooth)", Icons.Filled.Bluetooth),
    RANDOM("Aleatorio", Icons.Filled.Casino)
}

/**
 * Modelo de datos para un rival seleccionado
 */
data class BattleRival(
    val id: String,
    val name: String,
    val detail: String,
    val category: RivalCategory,
    val avatarUrl: String? = null,
    val isOnline: Boolean = true
)

/**
 * Juegos disponibles para duelo con sus recursos gráficos reales
 */
enum class BattleDrillType(
    val title: String,
    val category: String,
    val duration: String,
    val imageResId: Int,
    val description: String,
    val challengeGoalText: String
) {
    BALL_TOUCH("Ball & Touch", "Reflejos", "2 MIN", R.drawable.point, "Toca los puntos en pantalla manteniendo el bote del balón", "tocar más puntos con bote"),
    CROSS_TOUCH("Cross Touch", "Coordinación", "2 MIN", R.drawable.kantera_point, "Cruza manos y toca objetivos iluminados al ritmo", "tocar más objetivos cruzados al ritmo"),
    DEFEND_ZONE("Defend Zone", "Defensa", "1 MIN", R.drawable.kantera_mano, "Bloquea los ataques virtuales con posición defensiva baja", "bloquear más ataques y defender la zona"),
    LASER_ZONE("Laser Zone", "Agilidad", "1 MIN", R.drawable.kantera_laser, "Esquiva los lásers y reacciona a los estímulos visuales", "esquivar más lásers con rapidez"),
    SHOOTING("Shooting Form", "Tiro", "3 MIN", R.drawable.kantera_tiro, "Prueba tu mecánica de tiro a canasta", "encestar más canastas"),
    KIDS_BASKET("Kids Mini Basket", "Aciertos", "1 MIN", R.drawable.kantera_kid, "Encesta el mayor número de canastas virtuales", "encestar más canastas"),
    SPEED_TRAP("Speed Trap BPM", "Velocidad", "20 SEG", R.drawable.kantera_speedtrap, "Alcanza la máxima velocidad de bote por minuto", "alcanzar más velocidad de bote por minuto")
}

data class NearbyPeer(
    val id: String,
    val name: String,
    val deviceType: String,
    val distanceMeters: Int
)

data class LeagueMember(
    val rank: Int,
    val name: String,
    val score: Int,
    val wins: Int,
    val isCurrentUser: Boolean = false
)

/**
 * Pantalla BATTLE ZONE con el diseño guiado en 3 pasos:
 * 1. Elige el juego
 * 2. Elige el rival (Amigo, Bluetooth o Aleatorio)
 * 3. Previa del enfrentamiento y botón gigante para jugar
 *
 * Debajo de esta sección: Invitar por código y Liga de amigos.
 */
@Composable
fun BattleGameScreen(
    playerName: String,
    avatarUrl: String?,
    isGameMode: Boolean,
    playerStats: PlayerStats,
    onToggleMode: (Boolean) -> Unit,
    onProfileClick: () -> Unit,
    onOpenXpBreakdown: () -> Unit,
    onBackToHome: () -> Unit,
    onLaunchReactionPoints: () -> Unit,
    onLaunchDefendZone: () -> Unit,
    onLaunchShooting: () -> Unit,
    onLaunchKidsBasket: () -> Unit,
    onLaunchDribble: () -> Unit,
    onLaunchSpeedTrap: () -> Unit = onLaunchDribble,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Inicializar lista de amigos guardada
    LaunchedEffect(Unit) {
        com.example.friends.FriendsManager.initialize(context)
    }

    // State Holder de los 3 Pasos de la Batalla (Elección de juego, Selección de rival, Vista previa)
    val wizardState = rememberBattleWizardState()

    // Liga de amigos
    var leagueName by remember { mutableStateOf("Los Reyes del Parque") }
    var leagueCode by remember { mutableStateOf("REYES-2026") }
    var showCreateLeagueDialog by remember { mutableStateOf(false) }
    var newLeagueInput by remember { mutableStateOf("") }
    val leagueMembers = remember {
        mutableStateListOf(
            LeagueMember(1, "Marc_99", 540, 12),
            LeagueMember(2, playerName.ifBlank { "Tú" }, 490, 10, isCurrentUser = true),
            LeagueMember(3, "Carlos Dribble", 420, 8),
            LeagueMember(4, "Hugo Splash", 380, 7)
        )
    }

    val launchDrillForType: (BattleDrillType) -> Unit = { type ->
        when (type) {
            BattleDrillType.BALL_TOUCH, BattleDrillType.CROSS_TOUCH -> onLaunchReactionPoints()
            BattleDrillType.DEFEND_ZONE, BattleDrillType.LASER_ZONE -> onLaunchDefendZone()
            BattleDrillType.SHOOTING -> onLaunchShooting()
            BattleDrillType.KIDS_BASKET -> onLaunchKidsBasket()
            BattleDrillType.SPEED_TRAP -> onLaunchSpeedTrap()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 1. CABECERA SUPERIOR IDÉNTICA A TODA LA APP (AVATAR, RACHA, XP, TOGGLE)
        WorkoutHeaderRow(
            playerName = playerName,
            avatarUrl = avatarUrl,
            isGameMode = isGameMode,
            playerStats = playerStats,
            onToggleMode = onToggleMode,
            onProfileClick = onProfileClick,
            onXpClick = onOpenXpBreakdown,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(44.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2. HERO BANNER DE BATALLAS
            item {
                BattleHeroBanner(totalWins = 14)
            }

            // 3. BARRA DE 3 PASOS (STEPPER SIMPLE CON STATE HOLDER)
            item {
                BattleSimpleStepper(
                    state = wizardState
                )
            }

            // 4. CONTENIDO SEGÚN EL PASO ACTUAL
            when (wizardState.currentStep) {
                // ==========================================
                // PASO 1: ELIGE EL JUEGO
                // ==========================================
                BattleWizardStep.CHOOSE_GAME -> {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "PASO 1: ¿A QUÉ QUERÉIS JUGAR?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Toca un minijuego de baloncesto para seleccionarlo",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE0F7FA),
                                    border = BorderStroke(1.dp, Color(0xFF80DEEA))
                                ) {
                                    Text(
                                        text = wizardState.selectedDrill.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00838F),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Cuadrícula / Lista de tarjetas de juegos
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BattleDrillType.values().forEach { drill ->
                                    val isSelected = wizardState.selectedDrill == drill
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { wizardState.selectDrill(drill) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFFF0FDFA) else Color.White
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF00ACC1) else Color(0xFFE2E8F0)
                                        ),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = if (isSelected) 4.dp else 1.dp
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Imagen del juego
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            ) {
                                                Image(
                                                    painter = painterResource(id = drill.imageResId),
                                                    contentDescription = drill.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(3.dp)
                                                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = drill.duration,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = drill.title,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = drill.category,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF475569)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(3.dp))

                                                Text(
                                                    text = drill.description,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B),
                                                    lineHeight = 15.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Indicador de selección
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) Color(0xFF00ACC1) else Color(0xFFF1F5F9)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = "Seleccionado",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .border(1.5.dp, Color(0xFF94A3B8), CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Botón principal de avance
                            Button(
                                onClick = { wizardState.nextStep() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1))
                            ) {
                                Text(
                                    text = "Siguiente: Elegir Rival (Paso 2)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // PASO 2: ELIGE EL RIVAL (AMIGOS / BLUETOOTH / ALEATORIO)
                // ==========================================
                BattleWizardStep.CHOOSE_RIVAL -> {
                    item {
                        Column {
                            Text(
                                text = "PASO 2: ¿CONTRA QUIÉN QUIERES JUGAR?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Elige un amigo, conecta por Bluetooth en la pista o busca a alguien al azar",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Selector de las 3 opciones (Amigos, Bluetooth, Aleatorio) en una sola fila clara
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RivalCategory.values().forEach { category ->
                                    val isSelected = wizardState.selectedRivalCategory == category
                                    val bgCol by animateColorAsState(
                                        targetValue = if (isSelected) Color(0xFF00ACC1) else Color.Transparent,
                                        label = "rival_tab_bg"
                                    )
                                    val textCol by animateColorAsState(
                                        targetValue = if (isSelected) Color.White else Color(0xFF475569),
                                        label = "rival_tab_text"
                                    )

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bgCol)
                                            .clickable { wizardState.selectRivalCategory(category) }
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            tint = textCol,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = category.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = textCol,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Contenido específico de la categoría seleccionada
                            when (wizardState.selectedRivalCategory) {
                                // OPCIÓN A: LISTA DE AMIGOS
                                RivalCategory.FRIENDS -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        wizardState.friendsList.forEach { friend ->
                                            val isSelected = wizardState.selectedRival.id == friend.id
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { wizardState.selectRival(friend) },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFFF0FDFA) else Color.White
                                                ),
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF00ACC1) else Color(0xFFE2E8F0)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    UserAvatarImage(
                                                        avatarUrl = friend.avatarUrl,
                                                        displayName = friend.name,
                                                        fallbackDrawable = R.drawable.avatarchico,
                                                        size = 46.dp,
                                                        borderWidth = if (isSelected) 2.dp else 1.dp
                                                    )

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = friend.name,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(7.dp)
                                                                    .background(
                                                                        if (friend.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8),
                                                                        CircleShape
                                                                    )
                                                            )
                                                            Spacer(modifier = Modifier.width(5.dp))
                                                            Text(
                                                                text = friend.detail,
                                                                fontSize = 11.sp,
                                                                color = Color(0xFF64748B)
                                                            )
                                                        }
                                                    }

                                                    Button(
                                                        onClick = {
                                                            wizardState.selectRival(friend)
                                                            wizardState.nextStep()
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isSelected) Color(0xFF00ACC1) else Color(0xFFF1F5F9),
                                                            contentColor = if (isSelected) Color.White else Color(0xFF0F172A)
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isSelected) "Elegido" else "Elegir",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Acceso a amigos por código en el perfil
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = Color(0xFF00ACC1), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Para retar a más amigos, añádelos con su código desde tu Perfil",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                // OPCIÓN B: BLUETOOTH CANCHA
                                RivalCategory.BLUETOOTH -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        // Banner Offline
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA)),
                                            border = BorderStroke(1.dp, Color(0xFF80DEEA))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(Color(0xFF00ACC1), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Filled.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "MODO CANCHA 100% OFFLINE",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF006064)
                                                    )
                                                    Text(
                                                        text = "Conecta con amigos cercanos sin consumir datos móviles ni necesitar WiFi.",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF00838F),
                                                        lineHeight = 14.sp
                                                    )
                                                }
                                            }
                                        }

                                        // Botón Escanear Radar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "DISPOSITIVOS CERCA (${wizardState.nearbyPeers.size})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                            Button(
                                                onClick = {
                                                    wizardState.scanBluetooth {
                                                        Toast.makeText(context, "Radar actualizado: 3 rivales listos", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (wizardState.isScanningBluetooth) "Buscando..." else "Escanear Radar",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                        }

                                        // Lista de rivales cercanos detectados
                                        wizardState.nearbyPeers.forEach { peer ->
                                            val isSelected = wizardState.selectedRival.name == peer.name
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        wizardState.selectRival(
                                                            BattleRival(
                                                                id = peer.id,
                                                                name = peer.name,
                                                                detail = "${peer.deviceType} · ${peer.distanceMeters}m de ti",
                                                                category = RivalCategory.BLUETOOTH,
                                                                avatarUrl = "preset:avatarchico",
                                                                isOnline = true
                                                            )
                                                        )
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFFF0FDFA) else Color.White
                                                ),
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF00ACC1) else Color(0xFFE2E8F0)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .background(Color(0xFFE0F2FE), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Bluetooth,
                                                            contentDescription = null,
                                                            tint = Color(0xFF0284C7),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = peer.name,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                        Text(
                                                            text = "${peer.deviceType} · ${peer.distanceMeters}m en la cancha",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                    Button(
                                                        onClick = {
                                                            wizardState.selectRival(
                                                                BattleRival(
                                                                    id = peer.id,
                                                                    name = peer.name,
                                                                    detail = "${peer.deviceType} · ${peer.distanceMeters}m",
                                                                    category = RivalCategory.BLUETOOTH,
                                                                    avatarUrl = "preset:avatarchico",
                                                                    isOnline = true
                                                                )
                                                            )
                                                            wizardState.nextStep()
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Elegir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // OPCIÓN C: RIVAL ALEATORIO
                                RivalCategory.RANDOM -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F8)),
                                        border = BorderStroke(1.dp, Color(0xFFFCE7F3))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .background(Color(0xFFD93B98).copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.Casino, contentDescription = null, tint = Color(0xFFD93B98), modifier = Modifier.size(24.dp))
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "Buscador de Rival Exprés",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "Te emparejamos al instante con un jugador de baloncesto de tu mismo nivel",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B),
                                                textAlign = TextAlign.Center
                                            )

                                            Spacer(modifier = Modifier.height(14.dp))

                                            if (wizardState.isSearchingRandom) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.White, RoundedCornerShape(10.dp))
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        color = Color(0xFFD93B98),
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = "Buscando oponente compatible...",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFD93B98)
                                                    )
                                                }
                                            } else if (wizardState.randomRivalFound != null) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.White, RoundedCornerShape(12.dp))
                                                        .border(1.5.dp, Color(0xFFF472B6), RoundedCornerShape(12.dp))
                                                        .padding(12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = "¡RIVAL ENCONTRADO!",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFBE185D)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    UserAvatarImage(
                                                        avatarUrl = wizardState.randomRivalFound?.avatarUrl,
                                                        displayName = wizardState.randomRivalFound?.name ?: "Rival",
                                                        fallbackDrawable = R.drawable.avatarchico,
                                                        size = 48.dp,
                                                        borderWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = wizardState.randomRivalFound?.name ?: "",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = wizardState.randomRivalFound?.detail ?: "",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Button(
                                                        onClick = {
                                                            wizardState.selectRival(wizardState.randomRivalFound!!)
                                                            wizardState.nextStep()
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD93B98))
                                                    ) {
                                                        Text("Elegir a este rival", fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        wizardState.searchRandomRival()
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD93B98))
                                                ) {
                                                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Buscar Rival Aleatorio", fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Botones de navegación del Paso 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { wizardState.previousStep() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Atrás", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                }

                                Button(
                                    onClick = { wizardState.nextStep() },
                                    modifier = Modifier
                                        .weight(1.6f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1))
                                ) {
                                    Text("Ver Previa (Paso 3)", fontWeight = FontWeight.Black, color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // PASO 3: PREVIA DE TODO LO SELECCIONADO Y BOTÓN DE JUGAR
                // ==========================================
                BattleWizardStep.PREVIEW_PLAY -> {
                    item {
                        Column {
                            Text(
                                text = "PASO 3: ¡PREVIA DEL DUELO!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Comprueba el enfrentamiento y pulsa para saltar a la pista",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // TARJETA VERSUS ESTILO VIDEOJUEGO ARCADE
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    // Gradiente de fondo energético
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(190.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF00ACC1).copy(alpha = 0.35f),
                                                        Color(0xFF0F172A),
                                                        Color(0xFFEA580C).copy(alpha = 0.35f)
                                                    )
                                                )
                                            )
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Badge superior
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "🔥 DUELO 1 CONTRA 1",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF00E5FF),
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Fila Cara a Cara: TÚ vs RIVAL
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            // LADO IZQUIERDO: TÚ
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                UserAvatarImage(
                                                    avatarUrl = avatarUrl,
                                                    displayName = playerName,
                                                    fallbackDrawable = R.drawable.avatarchico,
                                                    size = 64.dp,
                                                    borderBrush = Brush.sweepGradient(
                                                        listOf(Color(0xFF00E5FF), Color(0xFF2FB2C9), Color(0xFF00E5FF))
                                                    ),
                                                    borderWidth = 3.dp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "TÚ",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF00E5FF)
                                                )
                                                Text(
                                                    text = playerName.ifBlank { "Jugador" },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // CENTRO: INSIGNIA "VS"
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .shadow(8.dp, CircleShape)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(Color(0xFFFF2A85), Color(0xFFEA580C))
                                                            )
                                                        )
                                                        .border(2.dp, Color.White, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "VS",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            // LADO DERECHO: RIVAL
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                UserAvatarImage(
                                                    avatarUrl = wizardState.selectedRival.avatarUrl,
                                                    displayName = wizardState.selectedRival.name,
                                                    fallbackDrawable = R.drawable.avatarchico,
                                                    size = 64.dp,
                                                    borderBrush = Brush.sweepGradient(
                                                        listOf(Color(0xFFFF8A00), Color(0xFFEA580C), Color(0xFFFF8A00))
                                                    ),
                                                    borderWidth = 3.dp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = wizardState.selectedRival.category.title.uppercase(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFFF8A00)
                                                )
                                                Text(
                                                    text = wizardState.selectedRival.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // TARJETA RESUMEN DEL JUEGO ELEGIDO
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    ) {
                                        Image(
                                            painter = painterResource(id = wizardState.selectedDrill.imageResId),
                                            contentDescription = wizardState.selectedDrill.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "JUEGO SELECCIONADO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = wizardState.selectedDrill.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${wizardState.selectedDrill.category} · Duración: ${wizardState.selectedDrill.duration}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF00838F),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    IconButton(onClick = { wizardState.goToStep(BattleWizardStep.CHOOSE_GAME) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Cambiar juego", tint = Color(0xFF64748B))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // =======================================================
                            // SELECTOR DE MODALIDAD: 1 VS 1 EN DIRECTO vs PASARLE EL BALÓN
                            // =======================================================
                            Text(
                                text = "¿CÓMO QUERÉIS JUGAR EL DUELO?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF475569),
                                letterSpacing = 0.5.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // OPCIÓN 1: 1 VS 1 EN DIRECTO (Simultáneo)
                            val isLiveSelected = wizardState.selectedPlayMode == BattlePlayMode.LIVE_DUEL
                            val rivalDisplayName = wizardState.selectedRival.name

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { wizardState.selectPlayMode(BattlePlayMode.LIVE_DUEL) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLiveSelected) Color(0xFFF0FDFA) else Color.White
                                ),
                                border = BorderStroke(
                                    width = if (isLiveSelected) 2.dp else 1.dp,
                                    color = if (isLiveSelected) Color(0xFF00ACC1) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "⚡🏀", fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "1 VS 1 EN DIRECTO",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isLiveSelected) Color(0xFF00838F) else Color(0xFF0F172A)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isLiveSelected) Color(0xFF00ACC1) else Color(0xFFF1F5F9),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "SIMULTÁNEO",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isLiveSelected) Color.White else Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "¡$rivalDisplayName y tú a la vez en la cancha! Los dos bises y puntos suben al marcador en tiempo real en vuestras pantallas.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // OPCIÓN 2: PASARLE EL BALÓN (Por turnos asíncrono)
                            val isPassSelected = wizardState.selectedPlayMode == BattlePlayMode.PASS_THE_BALL

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { wizardState.selectPlayMode(BattlePlayMode.PASS_THE_BALL) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPassSelected) Color(0xFFFFF7ED) else Color.White
                                ),
                                border = BorderStroke(
                                    width = if (isPassSelected) 2.dp else 1.dp,
                                    color = if (isPassSelected) Color(0xFFEA580C) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "🔥🏀", fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "PASARLE EL BALÓN",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isPassSelected) Color(0xFFC2410C) else Color(0xFF0F172A)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isPassSelected) Color(0xFFEA580C) else Color(0xFFF1F5F9),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "POR TURNOS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isPassSelected) Color.White else Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "$rivalDisplayName no está en la cancha ahora. Juega tu mejor serie ahora y pásale el balón a tu rival para ver si es capaz de ${wizardState.selectedDrill.challengeGoalText}.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // GRAN BOTÓN GIGANTE DE ACCIÓN PARA JUGAR SEGÚN MODO
                            if (isLiveSelected) {
                                Button(
                                    onClick = {
                                        wizardState.startLiveLobby {
                                            launchDrillForType(wizardState.selectedDrill)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp)
                                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1))
                                ) {
                                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "¡SALTAR A LA PISTA JUNTOS!",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        Toast.makeText(
                                            context,
                                            "🔥 ¡A por tu récord! Al terminar le pasaremos el balón a $rivalDisplayName",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        launchDrillForType(wizardState.selectedDrill)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp)
                                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                                ) {
                                    Icon(Icons.Filled.SportsBasketball, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "¡JUGAR Y PASARLE EL BALÓN!",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Botón secundario para cambiar rival o volver
                            OutlinedButton(
                                onClick = { wizardState.previousStep() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "⬅️ Cambiar Rival o Juego",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // SECCIÓN INFERIOR: CLASIFICACIÓN DE LA LIGA DE AMIGOS
            // =================================================================
            item {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 2.dp)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // TARJETA: CLASIFICACIÓN DE LA LIGA DE AMIGOS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFFEF3C7), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = leagueName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Código de Liga: $leagueCode",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Button(
                                onClick = { showCreateLeagueDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Nueva Liga", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Filas de miembros de la liga
                        leagueMembers.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (member.isCurrentUser) Color(0xFFE0F7FA).copy(alpha = 0.5f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${member.rank}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when (member.rank) {
                                            1 -> Color(0xFFF59E0B)
                                            2 -> Color(0xFF94A3B8)
                                            3 -> Color(0xFFB45309)
                                            else -> Color(0xFF64748B)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = member.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (member.isCurrentUser) FontWeight.Black else FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${member.score} pts",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00838F)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${member.wins}V",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diálogo para crear nueva liga
        if (showCreateLeagueDialog) {
            AlertDialog(
                onDismissRequest = { showCreateLeagueDialog = false },
                title = { Text("Crear Nueva Liga", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column {
                        Text("Introduce el nombre de tu grupo o club:", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newLeagueInput,
                            onValueChange = { newLeagueInput = it },
                            placeholder = { Text("Ej: Warriors Cadete B") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newLeagueInput.isNotBlank()) {
                                leagueName = newLeagueInput
                                leagueCode = newLeagueInput.uppercase().take(6) + "-2026"
                                showCreateLeagueDialog = false
                                newLeagueInput = ""
                                Toast.makeText(context, "¡Liga creada con éxito!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1))
                    ) {
                        Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateLeagueDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo de Sala de Espera / Lobby 1 VS 1 EN DIRECTO
        if (wizardState.isWaitingForLiveRival) {
            AlertDialog(
                onDismissRequest = { wizardState.cancelLiveLobby() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡🏀", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1 VS 1 EN DIRECTO",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        if (wizardState.liveCountdown != null) {
                            Text(
                                text = "${wizardState.liveCountdown}",
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEA580C)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "¡PREPÁRATE PARA BOTAR!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                        } else {
                            CircularProgressIndicator(
                                color = Color(0xFF00ACC1),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Avisando a ${wizardState.selectedRival.name}...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sincronizando marcador de pista en tiempo real.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            wizardState.cancelLiveLobby()
                            launchDrillForType(wizardState.selectedDrill)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("¡Entrar a la cancha ya!", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { wizardState.cancelLiveLobby() }) {
                        Text("Cancelar", color = Color(0xFF64748B))
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White
            )
        }
    }
}

/**
 * Componente Stepper de los 3 Pasos del Duelo
 */
@Composable
private fun BattleWizardStepper(
    currentStep: BattleWizardStep,
    onStepClick: (BattleWizardStep) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val steps = BattleWizardStep.values()
            steps.forEachIndexed { index, step ->
                val isCompleted = step.stepNumber < currentStep.stepNumber
                val isCurrent = step.stepNumber == currentStep.stepNumber

                // Paso individual (icono + número/check + título)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStepClick(step) }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> Color(0xFF00ACC1)
                                    isCompleted -> Color(0xFF10B981)
                                    else -> Color(0xFFCBD5E1)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                text = "${step.stepNumber}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isCurrent) Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = step.title,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.SemiBold,
                        color = if (isCurrent) Color(0xFF00838F) else if (isCompleted) Color(0xFF0F172A) else Color(0xFF94A3B8)
                    )
                }

                // Separador conector entre pasos
                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(2.dp)
                            .background(
                                if (step.stepNumber < currentStep.stepNumber) Color(0xFF10B981) else Color(0xFFE2E8F0)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Banner superior decorativo de la sección Batallas
 */
@Composable
private fun BattleHeroBanner(totalWins: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.banner1),
                contentDescription = "Battle Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradiente oscuro suave para legibilidad del texto
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "VS 1v1 BATTLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Whatshot, contentDescription = null, tint = Color(0xFFFF6B1A), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$totalWins Victorias",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "DESAFÍA A TUS AMIGOS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = "Compite en reflejos, bote y tiro en 3 sencillos pasos",
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
