package com.example.config

import com.example.R
import com.example.home.HeroWorkoutSlide

/**
 * =========================================================================================
 * ⚙️ PANEL DE CONFIGURACIÓN MAESTRO DEL SLIDE Y ENTRENAMIENTOS (GAME & PRO)
 * =========================================================================================
 *
 * ¡EDÍTALO LIBREMENTE DESDE AQUÍ!
 * En este archivo puedes:
 * 1. 🔀 Reordenar, añadir, eliminar o duplicar tarjetas del slide o workouts.
 * 2. 🔒 Poner cualquier juego bloqueado (`isLocked = true`) o desbloqueado (`isLocked = false`).
 * 3. ⚡ Definir el HYPE / XP requerido para desbloquear (`requiredHype = 500`).
 * 4. 📝 Cambiar títulos, subtítulos, nombres, duración y dificultad.
 * 5. 🖼️ Cambiar imágenes fácilmente usando [SlideImage]:
 *    - SlideImage.POINT ("kantera_point")
 *    - SlideImage.POINT2 ("kantera_point2")
 *    - SlideImage.MANO ("kantera_mano")
 *    - SlideImage.LASER ("kantera_laser")
 *    - SlideImage.TIRO ("kantera_tiro")
 *    - SlideImage.KID ("kantera_kid")
 *    - SlideImage.DRIBBLING ("img_drill_dribbling")
 *    - SlideImage.MANITA ("kantera_manita")
 *    - SlideImage.ONBOARDING ("img_onboarding_player")
 *    - SlideImage.AVATAR ("avatarchico")
 * 6. 🎮 Asociar a qué juego real corresponde cada tarjeta mediante [GameTarget]:
 *    - GameTarget.REACTION_BALL_TOUCH -> Reaction Points (Ball & Touch)
 *    - GameTarget.REACTION_CROSS_TOUCH -> Reaction Points (Cross Touch)
 *    - GameTarget.DEFEND_ZONE -> Defend the Zone
 *    - GameTarget.LASER_ZONE -> Defend the Zone (Laser)
 *    - GameTarget.SHOOTING -> Análisis de Tiro
 *    - GameTarget.KIDS_MINI_BASKET -> Kids Mini Basket
 *    - GameTarget.DRIBBLING -> Dribble Training
 *    - GameTarget.CATALOG_ONLY -> Abre el catálogo de entrenamientos
 *    - GameTarget.NONE -> Sin acción (por ejemplo si está bloqueado)
 *
 * Cada modo (MODO GAME y MODO PRO) tiene sus listas independientes tanto para
 * el Slide del Inicio como para el Catálogo de Workouts.
 */

enum class GameTarget {
    REACTION_BALL_TOUCH,
    REACTION_CROSS_TOUCH,
    DEFEND_ZONE,
    LASER_ZONE,
    SHOOTING,
    KIDS_MINI_BASKET,
    DRIBBLING,
    SPEED_TRAP,
    CATALOG_ONLY,
    NONE
}

enum class SlideImage(val resId: Int) {
    POINT(R.drawable.kantera_point),
    POINT2(R.drawable.kantera_point2),
    MANO(R.drawable.kantera_mano),
    LASER(R.drawable.kantera_laser),
    TIRO(R.drawable.kantera_tiro),
    KID(R.drawable.kantera_kid),
    DRIBBLING(R.drawable.img_drill_dribbling),
    SPEED_TRAP(R.drawable.kantera_speedtrap),
    MANITA(R.drawable.kantera_manita),
    ONBOARDING(R.drawable.img_onboarding_player),
    AVATAR(R.drawable.avatarchico)
}

/**
 * Definición individual de una tarjeta/juego configurable.
 */
data class GameSlideItemConfig(
    val id: String,
    val title: String,
    val subtitle: String,
    val drillName: String,
    val duration: String = "2 MIN",
    val difficulty: String = "BEGINNER",
    val image: SlideImage,
    val gameTarget: GameTarget,
    val isLocked: Boolean = false,
    val requiredHype: Int = 0,
    val levelNumber: Int = 1
)

object AppGamesConfig {

    // =====================================================================================
    // 🕹️ 1. MODO GAME - TARJETAS DEL SLIDE DE INICIO (HOME)
    // MVP: 4 Juegos Top Jugables + Próximamente / Bloqueados por Hype
    // =====================================================================================
    val gameModeHomeSlide: List<GameSlideItemConfig> = listOf(
        // 1. SPEED TRAP (Radar de Velocidad BPM y Bote de Fuego) - MVP
        GameSlideItemConfig(
            id = "game_speed_trap",
            title = "¡RITMO EXTREMO! SPEED TRAP",
            subtitle = "Tacómetro de carreras, radar BPM y aguante de 20s en llamas",
            drillName = "SPEED TRAP",
            duration = "20 SEG",
            difficulty = "ARCADE",
            image = SlideImage.SPEED_TRAP,
            gameTarget = GameTarget.SPEED_TRAP,
            isLocked = false,
            requiredHype = 0,
            levelNumber = 1
        ),
        // 2. REACTION POINTS (Ball & Touch) - MVP
        GameSlideItemConfig(
            id = "game_reaction_touch",
            title = "NEXT UP: DRIBBLE RUSH",
            subtitle = "Toca los objetivos luminosos mientras mantienes el bote",
            drillName = "Ball & Touch",
            duration = "1 MIN",
            difficulty = "BEGINNER",
            image = SlideImage.POINT,
            gameTarget = GameTarget.REACTION_BALL_TOUCH,
            isLocked = false,
            requiredHype = 0,
            levelNumber = 2
        ),
        // 3. DEFEND THE ZONE - MVP
        GameSlideItemConfig(
            id = "game_defend_zone",
            title = "¡NUEVO! DEFEND THE ZONE",
            subtitle = "Esquiva los defensores fantasma y protege tu bote (3 vidas)",
            drillName = "DEFEND ZONE",
            duration = "1 MIN",
            difficulty = "PRO",
            image = SlideImage.MANO,
            gameTarget = GameTarget.DEFEND_ZONE,
            isLocked = false,
            requiredHype = 0,
            levelNumber = 3
        ),
        // 4. KIDS MINI BASKET (Canasta en Casa) - MVP
        GameSlideItemConfig(
            id = "game_kids_mini",
            title = "¡EN CASA! KIDS MINI BASKET",
            subtitle = "Tiro infantil en casa con calibración de aro y pelota por foto",
            drillName = "MINI BASKET",
            duration = "1 MIN",
            difficulty = "KIDS / CASA",
            image = SlideImage.KID,
            gameTarget = GameTarget.KIDS_MINI_BASKET,
            isLocked = false,
            requiredHype = 0,
            levelNumber = 4
        ),
        // 5. BLOQUEADO: CROSS TOUCH (Desbloqueable con 250 Hype)
        GameSlideItemConfig(
            id = "game_reaction_cross",
            title = "CROSSOVER REACTION",
            subtitle = "Desbloquea con 250 Hype o Nivel 2",
            drillName = "Cross Touch",
            duration = "2 MIN",
            difficulty = "NIVEL 2",
            image = SlideImage.POINT2,
            gameTarget = GameTarget.REACTION_CROSS_TOUCH,
            isLocked = true,
            requiredHype = 250,
            levelNumber = 5
        ),
        // 6. BLOQUEADO: LASER ZONE (Desbloqueable con 500 Hype)
        GameSlideItemConfig(
            id = "game_laser_zone",
            title = "LASER ZONE DEFENSE",
            subtitle = "Desbloquea con 500 Hype o Nivel 3",
            drillName = "LASER ZONE",
            duration = "1 MIN",
            difficulty = "NIVEL 3",
            image = SlideImage.LASER,
            gameTarget = GameTarget.LASER_ZONE,
            isLocked = true,
            requiredHype = 500,
            levelNumber = 6
        ),
        // 7. BLOQUEADO: DRIBBLING COMBO MASTER
        GameSlideItemConfig(
            id = "game_dribbling",
            title = "DRIBBLE COMBO MASTER",
            subtitle = "Desbloquea con 750 Hype o Nivel 4",
            drillName = "DRIBBLING",
            duration = "2 MIN",
            difficulty = "NIVEL 4",
            image = SlideImage.DRIBBLING,
            gameTarget = GameTarget.DRIBBLING,
            isLocked = true,
            requiredHype = 750,
            levelNumber = 7
        ),
        // 8. BLOQUEADO: STEP-BACK SHOOTING
        GameSlideItemConfig(
            id = "game_stepback_locked",
            title = "STEP-BACK SHOOTING",
            subtitle = "Desbloquea con 1000 Hype o Nivel 5",
            drillName = "STEP-BACK",
            duration = "3 MIN",
            difficulty = "ALL-STAR",
            image = SlideImage.ONBOARDING,
            gameTarget = GameTarget.NONE,
            isLocked = true,
            requiredHype = 1000,
            levelNumber = 8
        )
    )

    // =====================================================================================
    // 🏀 2. MODO PRO - TARJETAS DEL SLIDE DE INICIO (HOME)
    // MVP: 4 Entrenamientos Técnicos Top + Bloqueados por Hype / Próximamente
    // =====================================================================================
    val proModeHomeSlide: List<GameSlideItemConfig> = listOf(
        // 1. SPEED TRAP PRO (Cadencia máxima y velocidad de bote) - MVP
        GameSlideItemConfig(
            id = "pro_speed_trap",
            title = "SPEED TRAP (CADENCE BPM)",
            subtitle = "Control de cadencia máxima y aceleración de bote a +120 BPM",
            drillName = "SPEED TRAP",
            duration = "20 SEG",
            difficulty = "PRO",
            image = SlideImage.SPEED_TRAP,
            gameTarget = GameTarget.SPEED_TRAP,
            isLocked = false,
            requiredHype = 0,
            levelNumber = 1
        ),
        // 2. SHOOTING LAB (Análisis de tiro profesional en Modo PRO) - MVP
        GameSlideItemConfig(
            id = "pro_shooting",
            title = "PRO SHOOTING LAB",
            subtitle = "Métricas avanzadas de tiro, arco de entrada y porcentaje real",
            drillName = "TIRO DE 3",
            duration = "SIN TIEMPO",
            difficulty = "PRO",
            image = SlideImage.TIRO,
            gameTarget = GameTarget.SHOOTING,
            isLocked = false,
            requiredHype = 0,
            levelNumber = 2
        ),
        // 3. DEFEND ZONE - MVP
        GameSlideItemConfig(
            id = "pro_defend_zone",
            title = "DEFENSIVE REACTION & FOOTWORK",
            subtitle = "Defensa de zona de alto impacto con tracking corporal en tiempo real",
            drillName = "DEFEND ZONE",
            duration = "1 MIN",
            difficulty = "ELITE",
            image = SlideImage.MANO,
            gameTarget = GameTarget.DEFEND_ZONE,
            isLocked = false,
            requiredHype = 0,
            levelNumber = 3
        ),
        // 4. DRIBBLING COMBO PRO - MVP
        GameSlideItemConfig(
            id = "pro_dribbling",
            title = "BALL HANDLING & CADENCE",
            subtitle = "Control de bote a doble ritmo y aceleración explosiva",
            drillName = "DRIBBLING PRO",
            duration = "45 SEG",
            difficulty = "PRO",
            image = SlideImage.DRIBBLING,
            gameTarget = GameTarget.DRIBBLING,
            isLocked = false,
            requiredHype = 0,
            levelNumber = 4
        ),
        // 5. BLOQUEADO PRO: REACTION POINTS (Ball & Touch)
        GameSlideItemConfig(
            id = "pro_reaction_point",
            title = "PERIPHERAL VISION DRILL",
            subtitle = "Desbloquea con 300 Hype o Nivel PRO 2",
            drillName = "Ball & Touch",
            duration = "2 MIN",
            difficulty = "PRO 2",
            image = SlideImage.POINT,
            gameTarget = GameTarget.REACTION_BALL_TOUCH,
            isLocked = true,
            requiredHype = 300,
            levelNumber = 5
        ),
        // 6. BLOQUEADO PRO: LASER ZONE
        GameSlideItemConfig(
            id = "pro_laser_zone",
            title = "AGILITY LASER GRID",
            subtitle = "Desbloquea con 600 Hype o Nivel PRO 3",
            drillName = "LASER ZONE",
            duration = "3 MIN",
            difficulty = "ELITE",
            image = SlideImage.LASER,
            gameTarget = GameTarget.LASER_ZONE,
            isLocked = true,
            requiredHype = 600,
            levelNumber = 6
        )
    )

    // =====================================================================================
    // 📋 3. MODO GAME - CATÁLOGO DE WORKOUTS / JUEGOS
    // =====================================================================================
    // Si quieres que el catálogo del modo GAME tenga los mismos o una lista personalizada:
    val gameModeWorkoutsCatalog: List<GameSlideItemConfig> = gameModeHomeSlide

    // =====================================================================================
    // 📋 4. MODO PRO - CATÁLOGO DE WORKOUTS PROFESIONAL
    // =====================================================================================
    // Catálogo específico para la pestaña de Workouts en Modo PRO:
    val proModeWorkoutsCatalog: List<GameSlideItemConfig> = proModeHomeSlide

    // =====================================================================================
    // 🛠️ MÉTODOS DE CONVERSIÓN (No necesitas tocar esto, se encarga de conectar con la UI)
    // =====================================================================================
    fun resolveAction(
        target: GameTarget,
        onLaunchDefendZoneDrill: () -> Unit,
        onLaunchDribbleDrill: () -> Unit,
        onLaunchReactionPointsDrill: () -> Unit,
        onLaunchShootingDrill: () -> Unit,
        onLaunchKidsMiniBasketDrill: () -> Unit,
        onLaunchSpeedTrapDrill: () -> Unit = {},
        onNavigateToWorkouts: () -> Unit
    ): () -> Unit = {
        when (target) {
            GameTarget.REACTION_BALL_TOUCH -> onLaunchReactionPointsDrill()
            GameTarget.REACTION_CROSS_TOUCH -> onLaunchReactionPointsDrill()
            GameTarget.DEFEND_ZONE -> onLaunchDefendZoneDrill()
            GameTarget.LASER_ZONE -> onLaunchDefendZoneDrill()
            GameTarget.SHOOTING -> onLaunchShootingDrill()
            GameTarget.KIDS_MINI_BASKET -> onLaunchKidsMiniBasketDrill()
            GameTarget.DRIBBLING -> onLaunchDribbleDrill()
            GameTarget.SPEED_TRAP -> onLaunchSpeedTrapDrill()
            GameTarget.CATALOG_ONLY -> onNavigateToWorkouts()
            GameTarget.NONE -> { /* Bloqueado o sin acción */ }
        }
    }

    fun buildHeroSlides(
        configs: List<GameSlideItemConfig>,
        currentXp: Int,
        onLaunchDefendZoneDrill: () -> Unit,
        onLaunchDribbleDrill: () -> Unit,
        onLaunchReactionPointsDrill: () -> Unit,
        onLaunchShootingDrill: () -> Unit,
        onLaunchKidsMiniBasketDrill: () -> Unit,
        onLaunchSpeedTrapDrill: () -> Unit = {},
        onNavigateToWorkouts: () -> Unit
    ): List<HeroWorkoutSlide> {
        return configs.map { item ->
            // Un item está bloqueado si `isLocked` es true y el XP del usuario no alcanza `requiredHype`
            val actuallyLocked = item.isLocked && (item.requiredHype > 0 && currentXp < item.requiredHype)
            HeroWorkoutSlide(
                title = item.title,
                subtitle = item.subtitle,
                drillName = item.drillName,
                duration = item.duration,
                difficulty = item.difficulty,
                isLocked = actuallyLocked,
                requiredXp = item.requiredHype,
                currentXp = currentXp,
                levelNumber = item.levelNumber,
                imageResId = item.image.resId,
                onAction = if (actuallyLocked) { {} } else {
                    resolveAction(
                        target = item.gameTarget,
                        onLaunchDefendZoneDrill = onLaunchDefendZoneDrill,
                        onLaunchDribbleDrill = onLaunchDribbleDrill,
                        onLaunchReactionPointsDrill = onLaunchReactionPointsDrill,
                        onLaunchShootingDrill = onLaunchShootingDrill,
                        onLaunchKidsMiniBasketDrill = onLaunchKidsMiniBasketDrill,
                        onLaunchSpeedTrapDrill = onLaunchSpeedTrapDrill,
                        onNavigateToWorkouts = onNavigateToWorkouts
                    )
                }
            )
        }
    }
}
