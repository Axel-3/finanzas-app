package com.axel.finanzas.core.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.axel.finanzas.core.designsystem.FinTheme

/**
 * Card base de la app: fondo surface, borde sutil (no sombra agresiva) y
 * esquinas moderadamente redondeadas. Úsala en vez de `Card` directamente
 * para que todas las superficies elevadas luzcan igual.
 */
@Composable
fun FinCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(FinTheme.spacing.lg),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.border(
            width = 1.dp,
            color = FinTheme.extendedColors.cardBorder,
            shape = shape,
        ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
