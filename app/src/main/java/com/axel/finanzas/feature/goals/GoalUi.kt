package com.axel.finanzas.feature.goals

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class GoalUi(
    val id: Long,
    val name: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long,
    val targetDate: LocalDate?,
) {
    val progress: Float
        get() = if (targetAmountCents > 0) (currentAmountCents.toFloat() / targetAmountCents.toFloat()).coerceIn(0f, 1f) else 0f

    val remainingCents: Long get() = (targetAmountCents - currentAmountCents).coerceAtLeast(0)

    /** Cuánto falta ahorrar por semana y por mes para llegar a tiempo. Null si ya no hay fecha o ya se cumplió. */
    val perWeekCents: Long?
        get() {
            val date = targetDate ?: return null
            val weeksLeft = ChronoUnit.WEEKS.between(LocalDate.now(), date).coerceAtLeast(1)
            return if (remainingCents > 0) remainingCents / weeksLeft else 0
        }

    val perMonthCents: Long?
        get() {
            val date = targetDate ?: return null
            val monthsLeft = ChronoUnit.MONTHS.between(LocalDate.now(), date).coerceAtLeast(1)
            return if (remainingCents > 0) remainingCents / monthsLeft else 0
        }
}
