package com.axel.finanzas.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.axel.finanzas.core.designsystem.AmountTextStyle
import java.text.NumberFormat
import java.util.Locale

/** Signo semántico de un monto, independiente de su representación numérica. */
enum class AmountSign { NEUTRAL, POSITIVE, NEGATIVE }

private val mxnFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("es", "MX"))
}

/** Formatea centavos (Long) a "$1,234.50" en MXN. */
fun formatCents(amountCents: Long): String {
    return mxnFormat.format(amountCents / 100.0)
}

/**
 * Texto de monto con cifras tabulares, que cuenta animadamente desde su
 * valor anterior cuando [amountCents] cambia (saldos y totales "suben" o
 * "bajan" en vez de saltar). Para contenido estático dentro de una lista
 * larga, considera desactivar [animate] si el costo de recomposición
 * importa.
 */
@Composable
fun AmountText(
    amountCents: Long,
    sign: AmountSign = AmountSign.NEUTRAL,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    animate: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val color = when (sign) {
        AmountSign.NEUTRAL -> LocalContentColor.current
        AmountSign.POSITIVE -> com.axel.finanzas.core.designsystem.FinTheme.extendedColors.income
        AmountSign.NEGATIVE -> com.axel.finanzas.core.designsystem.FinTheme.extendedColors.expense
    }

    val animated by animateFloatAsState(
        targetValue = amountCents.toFloat(),
        animationSpec = tween(durationMillis = 320),
        label = "amountCount",
    )
    val shown = if (animate) animated.toLong() else amountCents

    Text(
        text = formatCents(shown),
        style = style.merge(AmountTextStyle),
        color = color,
        modifier = modifier,
    )
}
