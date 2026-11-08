package com.axel.finanzas.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.axel.finanzas.R

/**
 * Manrope es una variable font (un solo archivo, eje "wght"). Se registra
 * una vez por peso usado, con [FontVariation.Settings] indicando qué
 * instancia pedirle — no son archivos distintos.
 */
@OptIn(ExperimentalTextApi::class)
private fun manropeWeight(weight: Int, fontWeight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = fontWeight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val ManropeFamily = FontFamily(
    manropeWeight(400, FontWeight.Normal),
    manropeWeight(500, FontWeight.Medium),
    manropeWeight(600, FontWeight.SemiBold),
    manropeWeight(700, FontWeight.Bold),
    manropeWeight(800, FontWeight.ExtraBold),
)

/**
 * Tipografía Material 3 sobre Manrope. Interlineados un poco más abiertos
 * que el default de Material (la primera versión se sentía "muy
 * pegada"), para que el texto respire sin perder jerarquía.
 */
val FinTypography = Typography(
    displayLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 58.sp, letterSpacing = (-0.3).sp),
    displayMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 48.sp, letterSpacing = (-0.2).sp),
    displaySmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 42.sp),

    headlineLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 30.sp),

    titleLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 22.sp),

    bodyLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 20.sp),

    labelLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 18.sp),
)

/** Estilo para montos: SemiBold + cifras tabulares para que alineen en columnas. */
val AmountTextStyle = TextStyle(
    fontFamily = ManropeFamily,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
)
