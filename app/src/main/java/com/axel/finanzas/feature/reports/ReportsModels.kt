package com.axel.finanzas.feature.reports

import java.time.YearMonth

data class CategorySlice(val name: String, val amountCents: Long, val colorHex: String)

data class MonthTotal(val month: YearMonth, val expenseCents: Long, val incomeCents: Long)
