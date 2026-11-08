package com.axel.finanzas.feature.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.axel.finanzas.core.designsystem.FinTheme

/**
 * Barras agrupadas gasto/ingreso por mes, dibujadas con Canvas (no hay
 * librería de gráficas en el proyecto — para esta cantidad de datos un
 * Canvas simple es más barato que agregar una dependencia). Se "dibuja"
 * al entrar animando la altura de 0 al valor real.
 */
@Composable
fun MonthlyBarChart(data: List<MonthTotal>, modifier: Modifier = Modifier) {
    val expenseColor = FinTheme.extendedColors.expense
    val incomeColor = FinTheme.extendedColors.income
    val trackColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant

    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }
    val animatedProgress = progress.value

    val maxValue = (data.maxOfOrNull { maxOf(it.expenseCents, it.incomeCents) } ?: 1L).coerceAtLeast(1L).toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        if (data.isEmpty()) return@Canvas
        val groupWidth = size.width / data.size
        val barWidth = groupWidth / 4f

        drawRect(color = trackColor, topLeft = Offset.Zero, size = Size(size.width, size.height))

        data.forEachIndexed { index, month ->
            val groupX = index * groupWidth
            val expenseHeight = (month.expenseCents / maxValue) * size.height * animatedProgress
            val incomeHeight = (month.incomeCents / maxValue) * size.height * animatedProgress

            drawRect(
                color = expenseColor,
                topLeft = Offset(groupX + barWidth, size.height - expenseHeight),
                size = Size(barWidth, expenseHeight),
            )
            drawRect(
                color = incomeColor,
                topLeft = Offset(groupX + barWidth * 2.2f, size.height - incomeHeight),
                size = Size(barWidth, incomeHeight),
            )
        }
    }
}
