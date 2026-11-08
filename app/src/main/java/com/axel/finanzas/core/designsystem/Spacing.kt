package com.axel.finanzas.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala de espaciados. Úsala en vez de valores de `.dp` sueltos para que
 * el espaciado de toda la app quede consistente.
 *
 * Ajustada tras la primera versión ("todo se siente muy pegado"): se
 * subieron sm/md/lg/xl/xxl/xxxl un poco respecto a la escala original
 * de 4dp para dar más aire sin cambiar cada pantalla.
 */
data class FinSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 10.dp,
    val md: Dp = 14.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 28.dp,
    val xxl: Dp = 36.dp,
    val xxxl: Dp = 56.dp,
)

val LocalFinSpacing = staticCompositionLocalOf { FinSpacing() }
