package com.axel.finanzas.feature.calendar

import java.time.LocalDate

data class AgendaItem(
    val date: LocalDate,
    val label: String,
    val amountCents: Long,
    val isIncome: Boolean,
    val source: Source,
) {
    enum class Source { RECURRING, FUTURE_PAYMENT }
}
