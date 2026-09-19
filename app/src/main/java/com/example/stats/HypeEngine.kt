package com.example.stats

import kotlin.math.roundToInt

/**
 * Categoría y dificultad ponderada de cada modo de juego en Kantera AI.
 *
 * El factor [pointsMultiplier] normaliza los puntos brutos de cada minijuego para que
 * una sesión promedio (en 30-60 segundos) otorgue un rendimiento comparable (15-35 Hype base).
 */
enum class GameDifficultyProfile(
    val gameId: String,
    val title: String,
    val modeType: String, // "GAME" o "PRO"
    val pointsMultiplier: Float,
    val averageScoreBenchmark: Int,
    val proScoreBenchmark: Int,
    val tagEmoji: String
) {
    REACTION_BALL_TOUCH(
        gameId = "REACTION_POINTS",
        title = "Ball & Touch (Reflejos)",
        modeType = "GAME",
        pointsMultiplier = 0.70f,
        averageScoreBenchmark = 35,
        proScoreBenchmark = 65,
        tagEmoji = "⚡"
    ),
    REACTION_CROSS_TOUCH(
        gameId = "REACTION_CROSS",
        title = "Cross Touch (Coordinación)",
        modeType = "GAME",
        pointsMultiplier = 0.90f,
        averageScoreBenchmark = 25,
        proScoreBenchmark = 50,
        tagEmoji = "🎯"
    ),
    DEFEND_ZONE(
        gameId = "DEFEND_ZONE",
        title = "Defend The Zone",
        modeType = "GAME",
        pointsMultiplier = 0.85f,
        averageScoreBenchmark = 30,
        proScoreBenchmark = 60,
        tagEmoji = "🛡️"
    ),
    SPEED_TRAP_FIRE(
        gameId = "SPEED_TRAP_FIRE",
        title = "Speed Trap (BPM Bote)",
        modeType = "GAME",
        pointsMultiplier = 0.08f, // Puntos generados altos (BPM x 10 = ~700-1400), escala a ~25-45 Hype
        averageScoreBenchmark = 350,
        proScoreBenchmark = 800,
        tagEmoji = "🔥"
    ),
    KIDS_MINI_BASKET(
        gameId = "KIDS_MINI_BASKET",
        title = "Kids Mini Basket",
        modeType = "GAME",
        pointsMultiplier = 1.60f,
        averageScoreBenchmark = 14,
        proScoreBenchmark = 30,
        tagEmoji = "🏀"
    ),
    DRIBBLE_COMBO(
        gameId = "DRIBBLE_COMBO",
        title = "Dribble Crossover",
        modeType = "GAME",
        pointsMultiplier = 0.60f,
        averageScoreBenchmark = 40,
        proScoreBenchmark = 80,
        tagEmoji = "🌪️"
    ),
    SHOOTING_PRO(
        gameId = "SHOOTING_PRO",
        title = "Shooting Form & Arc",
        modeType = "PRO",
        pointsMultiplier = 4.0f, // Canastas reales en pista: cada acierto vale mucho más
        averageScoreBenchmark = 6,
        proScoreBenchmark = 14,
        tagEmoji = "🎯"
    );

    companion object {
        fun fromGameMode(gameMode: String): GameDifficultyProfile {
            val upper = gameMode.uppercase()
            return entries.firstOrNull { it.gameId == upper || it.name == upper }
                ?: REACTION_BALL_TOUCH
        }
    }
}

/**
 * Desglose justo y transparente de la recompensa de Hype obtenida.
 */
data class HypeRewardBreakdown(
    val gameTitle: String,
    val modeType: String,
    val rawScore: Int,
    val baseEffortHype: Int = 10,
    val performanceHype: Int,
    val accuracyOrComboBonusHype: Int,
    val isNewRecord: Boolean,
    val recordBonusHype: Int,
    val totalHypeEarned: Int,
    val previousTotalHype: Int,
    val newTotalHype: Int,
    val previousLevel: Int,
    val newLevel: Int,
    val leveledUp: Boolean
)

/**
 * Motor central de cálculo de Hype igualitario de Kantera AI.
 */
object HypeEngine {

    /**
     * Calcula de forma justa el Hype en función del perfil del juego y el rendimiento.
     */
    fun calculateReward(
        gameMode: String,
        rawScore: Int,
        hitsOrCombos: Int = 0,
        accuracyPercent: Int? = null,
        isNewRecord: Boolean = false,
        currentTotalHype: Int = PlayerStatsManager.stats.value.totalXp,
        currentLevel: Int = PlayerStatsManager.stats.value.level
    ): HypeRewardBreakdown {
        val profile = GameDifficultyProfile.fromGameMode(gameMode)

        // 1. Base fija por haber completado la sesión / entreno
        val base = 10

        // 2. Rendimiento escalado según dificultad del juego
        val calculatedPerf = (rawScore * profile.pointsMultiplier).roundToInt()
        // Limitamos para evitar exploits, permitiendo recompensar entrenamientos épicos
        val performance = calculatedPerf.coerceIn(0, 55)

        // 3. Bonus por calidad, racha, combos o precisión
        var bonus = 0
        if (hitsOrCombos >= 3) {
            bonus += minOf(15, hitsOrCombos * 2)
        }
        if (accuracyPercent != null && accuracyPercent >= 60) {
            bonus += 10
        }

        // 4. Bonus de nuevo récord personal
        val recordBonus = if (isNewRecord) 20 else 0

        val totalEarned = (base + performance + bonus + recordBonus).coerceAtLeast(10)
        val newTotal = currentTotalHype + totalEarned

        // Calcular si ha subido de nivel con esta sesión
        val dummyStatsWithNewXp = PlayerStats(totalTrainingMinutes = 0).let {
            // Evaluamos nivel resultante con los puntos proyectados
            getLevelForXp(newTotal)
        }
        val leveledUp = dummyStatsWithNewXp > currentLevel

        return HypeRewardBreakdown(
            gameTitle = profile.title,
            modeType = profile.modeType,
            rawScore = rawScore,
            baseEffortHype = base,
            performanceHype = performance,
            accuracyOrComboBonusHype = bonus,
            isNewRecord = isNewRecord,
            recordBonusHype = recordBonus,
            totalHypeEarned = totalEarned,
            previousTotalHype = currentTotalHype,
            newTotalHype = newTotal,
            previousLevel = currentLevel,
            newLevel = dummyStatsWithNewXp,
            leveledUp = leveledUp
        )
    }

    private fun getLevelForXp(xp: Int): Int {
        return when {
            xp < 50 -> 1
            xp < 120 -> 2
            xp < 200 -> 3
            xp < 300 -> 4
            xp < 450 -> 5
            xp < 600 -> 6
            xp < 800 -> 7
            xp < 1050 -> 8
            xp < 1350 -> 9
            xp < 1700 -> 10
            xp < 2100 -> 11
            xp < 2600 -> 12
            xp < 3200 -> 13
            xp < 4000 -> 14
            else -> 15 + ((xp - 4000) / 800)
        }
    }
}
