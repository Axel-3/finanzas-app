package com.axel.finanzas.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Paleta cruda de la app. Nunca se usa directamente en las pantallas: se
 * consume a través de [FinanzasTheme] (MaterialTheme.colorScheme +
 * [LocalFinExtendedColors]). Mantener esta paleta como única fuente de
 * verdad facilita ajustarla sin tocar cada pantalla.
 *
 * Criterios de diseño (ver brief original):
 * - Nada de gradientes morados/azules, glassmorphism ni colores saturados.
 * - Fondo cálido (crema), nunca blanco puro; texto carbón, nunca negro puro.
 * - Modo oscuro con grises cálidos, nunca negro absoluto.
 */
object FinPalette {

    // ---- Light ----
    val LightBackground = Color(0xFFFBF8F3)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFF3EEE5)
    val LightOnBackground = Color(0xFF2B2A28)
    val LightOnSurfaceVariant = Color(0xFF6B6660)
    val LightOutline = Color(0xFFE6DFD3)

    val LightPrimary = Color(0xFF4F7A6C)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFDCE6DF)
    val LightOnPrimaryContainer = Color(0xFF1F3A32)

    val LightSecondary = Color(0xFFBC8468)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightSecondaryContainer = Color(0xFFF0E0D3)
    val LightOnSecondaryContainer = Color(0xFF4A2F1F)

    val LightExpense = Color(0xFFC97B5F)
    val LightExpenseContainer = Color(0xFFF2DCD3)
    val LightOnExpenseContainer = Color(0xFF6B2E1C)

    val LightIncome = Color(0xFF5E9478)
    val LightIncomeContainer = Color(0xFFDCEAE1)
    val LightOnIncomeContainer = Color(0xFF1E3B2C)

    val LightError = Color(0xFFB3473A)

    // ---- Dark (grises cálidos, no negro absoluto) ----
    val DarkBackground = Color(0xFF1E1C1A)
    val DarkSurface = Color(0xFF262320)
    val DarkSurfaceVariant = Color(0xFF2E2B27)
    val DarkOnBackground = Color(0xFFEDE8E0)
    val DarkOnSurfaceVariant = Color(0xFFB3ACA2)
    val DarkOutline = Color(0xFF44403A)

    val DarkPrimary = Color(0xFF8FB3A1)
    val DarkOnPrimary = Color(0xFF17302A)
    val DarkPrimaryContainer = Color(0xFF2E473E)
    val DarkOnPrimaryContainer = Color(0xFFCFE6DC)

    val DarkSecondary = Color(0xFFD9A480)
    val DarkOnSecondary = Color(0xFF3C2414)
    val DarkSecondaryContainer = Color(0xFF4A3424)
    val DarkOnSecondaryContainer = Color(0xFFF0DCC8)

    val DarkExpense = Color(0xFFE2A08E)
    val DarkExpenseContainer = Color(0xFF4A2A1F)
    val DarkOnExpenseContainer = Color(0xFFF5DCD2)

    val DarkIncome = Color(0xFF8FC3A4)
    val DarkIncomeContainer = Color(0xFF27402F)
    val DarkOnIncomeContainer = Color(0xFFD4ECDD)

    val DarkError = Color(0xFFCF6B5E)
}
