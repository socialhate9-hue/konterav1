package com.example.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.friends.FriendsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * State Holder en Jetpack Compose para gestionar el flujo de 3 pasos de Battle:
 * 1. Elección de juego (CHOOSE_GAME)
 * 2. Selección de rival (CHOOSE_RIVAL: Amigos, Bluetooth, Aleatorio)
 * 3. Vista previa (PREVIEW_PLAY: Versus y lanzamiento)
 *
 * Encapsula el estado de navegación, las selecciones de juego/rival y las operaciones asíncronas
 * (escaneo de radar Bluetooth y matchmaking aleatorio).
 */
@Stable
class BattleWizardState(
    initialStep: BattleWizardStep = BattleWizardStep.CHOOSE_GAME,
    initialDrill: BattleDrillType = BattleDrillType.BALL_TOUCH,
    val coroutineScope: CoroutineScope
) {
    // ==========================================
    // 1. NAVEGACIÓN Y PASOS DEL STEPPER
    // ==========================================
    var currentStep by mutableStateOf(initialStep)
        private set

    val isFirstStep by derivedStateOf { currentStep == BattleWizardStep.CHOOSE_GAME }
    val isLastStep by derivedStateOf { currentStep == BattleWizardStep.PREVIEW_PLAY }
    val canGoBack by derivedStateOf { currentStep.stepNumber > 1 }

    fun goToStep(step: BattleWizardStep) {
        currentStep = step
    }

    fun nextStep() {
        when (currentStep) {
            BattleWizardStep.CHOOSE_GAME -> currentStep = BattleWizardStep.CHOOSE_RIVAL
            BattleWizardStep.CHOOSE_RIVAL -> currentStep = BattleWizardStep.PREVIEW_PLAY
            BattleWizardStep.PREVIEW_PLAY -> Unit
        }
    }

    fun previousStep() {
        when (currentStep) {
            BattleWizardStep.PREVIEW_PLAY -> currentStep = BattleWizardStep.CHOOSE_RIVAL
            BattleWizardStep.CHOOSE_RIVAL -> currentStep = BattleWizardStep.CHOOSE_GAME
            BattleWizardStep.CHOOSE_GAME -> Unit
        }
    }

    fun reset() {
        currentStep = BattleWizardStep.CHOOSE_GAME
    }

    // ==========================================
    // 2. SELECCIÓN DEL JUEGO (PASO 1)
    // ==========================================
    var selectedDrill by mutableStateOf(initialDrill)
        private set

    fun selectDrill(drill: BattleDrillType) {
        selectedDrill = drill
    }

    // ==========================================
    // 3. SELECCIÓN DE RIVAL Y MODO (PASO 2)
    // ==========================================
    var selectedRivalCategory by mutableStateOf(RivalCategory.FRIENDS)
        private set

    fun selectRivalCategory(category: RivalCategory) {
        selectedRivalCategory = category
    }

    // Amigos disponibles (gestionados centralmente por FriendsManager)
    val friendsList: List<BattleRival>
        get() = if (FriendsManager.friendsList.isNotEmpty()) {
            FriendsManager.friendsList
        } else {
            listOf(
                BattleRival("f1", "Marc_99", "Nivel 4 · 540 Hype", RivalCategory.FRIENDS, "preset:avatarchico", true),
                BattleRival("f2", "Carlos Dribble", "Nivel 3 · 420 Hype", RivalCategory.FRIENDS, "preset:avatarchica", true),
                BattleRival("f3", "Hugo Splash", "Nivel 2 · 380 Hype", RivalCategory.FRIENDS, "preset:avatarchico", true),
                BattleRival("f4", "Lucas Basket", "Nivel 2 · 310 Hype", RivalCategory.FRIENDS, "preset:avatarchico", false)
            )
        }

    var selectedRival by mutableStateOf(
        friendsList.firstOrNull() ?: BattleRival("f1", "Marc_99", "Nivel 4 · 540 Hype", RivalCategory.FRIENDS, "preset:avatarchico", true)
    )
        private set

    fun selectRival(rival: BattleRival) {
        selectedRival = rival
    }

    // ==========================================
    // 4. MODO BLUETOOTH / RADAR CANCHA
    // ==========================================
    var isScanningBluetooth by mutableStateOf(false)
        private set

    val nearbyPeers = mutableStateListOf(
        NearbyPeer("p1", "Tablet de Lucas", "Tablet (Sin SIM)", 3),
        NearbyPeer("p2", "Pablo_Court", "Móvil Cancha", 5),
        NearbyPeer("p3", "Cadete Marcos", "iPad Club", 7)
    )

    fun scanBluetooth(onComplete: (() -> Unit)? = null) {
        if (isScanningBluetooth) return
        isScanningBluetooth = true
        coroutineScope.launch {
            delay(1600)
            isScanningBluetooth = false
            onComplete?.invoke()
        }
    }

    // ==========================================
    // 5. MATCHMAKING ALEATORIO
    // ==========================================
    var isSearchingRandom by mutableStateOf(false)
        private set

    var randomRivalFound by mutableStateOf<BattleRival?>(null)
        private set

    fun searchRandomRival(onFound: ((BattleRival) -> Unit)? = null) {
        if (isSearchingRandom) return
        isSearchingRandom = true
        randomRivalFound = null
        coroutineScope.launch {
            delay(1500)
            val randomRival = BattleRival(
                id = "rand_${System.currentTimeMillis() % 1000}",
                name = "Iker_Bulls",
                detail = "Nivel 3 · 490 Hype · En línea",
                category = RivalCategory.RANDOM,
                avatarUrl = "preset:avatarchico",
                isOnline = true
            )
            randomRivalFound = randomRival
            selectedRival = randomRival
            isSearchingRandom = false
            onFound?.invoke(randomRival)
        }
    }

    // ==========================================
    // 6. MODO DE JUEGO (PASO 3: EN DIRECTO vs PASARLE EL BALÓN)
    // ==========================================
    var selectedPlayMode by mutableStateOf(BattlePlayMode.LIVE_DUEL)
        private set

    fun selectPlayMode(mode: BattlePlayMode) {
        selectedPlayMode = mode
    }

    var isWaitingForLiveRival by mutableStateOf(false)
        private set

    var liveCountdown by mutableStateOf<Int?>(null)
        private set

    fun startLiveLobby(onRivalReady: () -> Unit) {
        if (isWaitingForLiveRival) return
        isWaitingForLiveRival = true
        liveCountdown = null
        coroutineScope.launch {
            delay(1500)
            liveCountdown = 3
            delay(700)
            liveCountdown = 2
            delay(700)
            liveCountdown = 1
            delay(700)
            isWaitingForLiveRival = false
            liveCountdown = null
            onRivalReady()
        }
    }

    fun cancelLiveLobby() {
        isWaitingForLiveRival = false
        liveCountdown = null
    }
}

/**
 * Modos de juego para la batalla:
 * - LIVE_DUEL: 1 VS 1 EN DIRECTO (ambos al mismo tiempo con marcador simultáneo)
 * - PASS_THE_BALL: PASARLE EL BALÓN (juegas tu turno y le mandas tu marca para que intente superarte)
 */
enum class BattlePlayMode(
    val title: String,
    val shortBadge: String
) {
    LIVE_DUEL("1 VS 1 EN DIRECTO", "SIMULTÁNEO"),
    PASS_THE_BALL("PASARLE EL BALÓN", "POR TURNOS")
}

/**
 * Función composable de ayuda que crea y recuerda una instancia de [BattleWizardState].
 */
@Composable
fun rememberBattleWizardState(
    initialStep: BattleWizardStep = BattleWizardStep.CHOOSE_GAME,
    initialDrill: BattleDrillType = BattleDrillType.BALL_TOUCH,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): BattleWizardState {
    return remember(coroutineScope) {
        BattleWizardState(
            initialStep = initialStep,
            initialDrill = initialDrill,
            coroutineScope = coroutineScope
        )
    }
}

/**
 * Componente de UI tipo Stepper simple para el flujo de 3 pasos de Battle:
 * 1. Elección de juego
 * 2. Selección de rival
 * 3. Vista previa
 *
 * Utiliza [BattleWizardState] como State Holder.
 */
@Composable
fun BattleSimpleStepper(
    state: BattleWizardState,
    modifier: Modifier = Modifier
) {
    BattleSimpleStepper(
        currentStep = state.currentStep,
        onStepClick = { step -> state.goToStep(step) },
        modifier = modifier
    )
}

/**
 * Sobrecarga desacoplada del Stepper simple que recibe el paso actual y callback.
 */
@Composable
fun BattleSimpleStepper(
    currentStep: BattleWizardStep,
    onStepClick: (BattleWizardStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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

                val stepColor by animateColorAsState(
                    targetValue = when {
                        isCurrent -> Color(0xFF00ACC1)
                        isCompleted -> Color(0xFF10B981)
                        else -> Color(0xFFCBD5E1)
                    },
                    label = "stepColor"
                )

                // Ítem del paso individual
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            role = Role.Tab,
                            onClick = { onStepClick(step) }
                        )
                        .semantics {
                            contentDescription = "${step.title}, paso ${step.stepNumber} de 3"
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(stepColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Completado",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
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
                        text = when (step) {
                            BattleWizardStep.CHOOSE_GAME -> "Juego"
                            BattleWizardStep.CHOOSE_RIVAL -> "Rival"
                            BattleWizardStep.PREVIEW_PLAY -> "Vista previa"
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.SemiBold,
                        color = if (isCurrent) Color(0xFF00838F) else if (isCompleted) Color(0xFF0F172A) else Color(0xFF94A3B8)
                    )
                }

                // Línea conectora entre pasos
                if (index < steps.size - 1) {
                    val lineColor by animateColorAsState(
                        targetValue = if (step.stepNumber < currentStep.stepNumber) Color(0xFF10B981) else Color(0xFFE2E8F0),
                        label = "lineColor"
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(2.dp)
                            .background(lineColor)
                    )
                }
            }
        }
    }
}
