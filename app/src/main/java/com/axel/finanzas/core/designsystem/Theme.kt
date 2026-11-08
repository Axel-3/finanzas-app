package com.axel.finanzas.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores que Material 3 no modela de forma nativa pero que la app
 * necesita en todas las pantallas: gasto, ingreso y el borde sutil de
 * las tarjetas. Se exponen igual que `MaterialTheme.colorScheme`.
 */
data class FinExtendedColors(
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val cardBorder: Color,
)

private val LightExtended = FinExtendedColors(
    expense = FinPalette.LightExpense,
    onExpense = Color.White,
    expenseContainer = FinPalette.LightExpenseContainer,
    onExpenseContainer = FinPalette.LightOnExpenseContainer,
    income = FinPalette.LightIncome,
    onIncome = Color.White,
    incomeContainer = FinPalette.LightIncomeContainer,
    onIncomeContainer = FinPalette.LightOnIncomeContainer,
    cardBorder = FinPalette.LightOutline,
)

private val DarkExtended = FinExtendedColors(
    expense = FinPalette.DarkExpense,
    onExpense = FinPalette.DarkOnPrimary,
    expenseContainer = FinPalette.DarkExpenseContainer,
    onExpenseContainer = FinPalette.DarkOnExpenseContainer,
    income = FinPalette.DarkIncome,
    onIncome = FinPalette.DarkOnPrimary,
    incomeContainer = FinPalette.DarkIncomeContainer,
    onIncomeContainer = FinPalette.DarkOnIncomeContainer,
    cardBorder = FinPalette.DarkOutline,
)

val LocalFinExtendedColors = staticCompositionLocalOf { LightExtended }

private val LightScheme = lightColorScheme(
    primary = FinPalette.LightPrimary,
    onPrimary = FinPalette.LightOnPrimary,
    primaryContainer = FinPalette.LightPrimaryContainer,
    onPrimaryContainer = FinPalette.LightOnPrimaryContainer,
    secondary = FinPalette.LightSecondary,
    onSecondary = FinPalette.LightOnSecondary,
    secondaryContainer = FinPalette.LightSecondaryContainer,
    onSecondaryContainer = FinPalette.LightOnSecondaryContainer,
    background = FinPalette.LightBackground,
    onBackground = FinPalette.LightOnBackground,
    surface = FinPalette.LightSurface,
    onSurface = FinPalette.LightOnBackground,
    surfaceVariant = FinPalette.LightSurfaceVariant,
    onSurfaceVariant = FinPalette.LightOnSurfaceVariant,
    outline = FinPalette.LightOutline,
    error = FinPalette.LightError,
)

private val DarkScheme = darkColorScheme(
    primary = FinPalette.DarkPrimary,
    onPrimary = FinPalette.DarkOnPrimary,
    primaryContainer = FinPalette.DarkPrimaryContainer,
    onPrimaryContainer = FinPalette.DarkOnPrimaryContainer,
    secondary = FinPalette.DarkSecondary,
    onSecondary = FinPalette.DarkOnSecondary,
    secondaryContainer = FinPalette.DarkSecondaryContainer,
    onSecondaryContainer = FinPalette.DarkOnSecondaryContainer,
    background = FinPalette.DarkBackground,
    onBackground = FinPalette.DarkOnBackground,
    surface = FinPalette.DarkSurface,
    onSurface = FinPalette.DarkOnBackground,
    surfaceVariant = FinPalette.DarkSurfaceVariant,
    onSurfaceVariant = FinPalette.DarkOnSurfaceVariant,
    outline = FinPalette.DarkOutline,
    error = FinPalette.DarkError,
)

object FinTheme {
    val extendedColors: FinExtendedColors
        @Composable get() = LocalFinExtendedColors.current

    val spacing: FinSpacing
        @Composable get() = LocalFinSpacing.current
}

@Composable
fun FinanzasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val extended = if (darkTheme) DarkExtended else LightExtended

    CompositionLocalProvider(
        LocalFinExtendedColors provides extended,
        LocalFinSpacing provides FinSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FinTypography,
            shapes = FinShapes,
            content = content,
        )
    }
}
