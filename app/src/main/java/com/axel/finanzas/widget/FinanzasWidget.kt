package com.axel.finanzas.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Widget de pantalla de inicio para registrar un gasto o ingreso en
 * efectivo sin abrir la app. Cada botón lanza [QuickAddActivity] como
 * ventana flotante mínima (ver tema `Theme.Finanzas.Dialog`).
 *
 * Nota: usa colores fijos del tema claro (no day/night) — el overload de
 * [ColorProvider] con variante oscura no está disponible en la versión de
 * Glance de esta app. Se puede revisar más adelante si vale la pena.
 */
class FinanzasWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(Color(0xFFFBF8F3)))
                        .cornerRadius(16.dp)
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Finanzas",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFF2B2A28)),
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        QuickActionButton(
                            context = context,
                            label = "+ Gasto",
                            type = QuickAddType.EXPENSE,
                            background = Color(0xFFF2DCD3),
                            textColor = Color(0xFF6B2E1C),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        QuickActionButton(
                            context = context,
                            label = "+ Ingreso",
                            type = QuickAddType.INCOME,
                            background = Color(0xFFDCEAE1),
                            textColor = Color(0xFF1E3B2C),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun QuickActionButton(
    context: Context,
    label: String,
    type: QuickAddType,
    background: Color,
    textColor: Color,
    modifier: GlanceModifier,
) {
    val intent = Intent(context, QuickAddActivity::class.java).apply {
        putExtra(QUICK_ADD_TYPE_EXTRA, type.name)
    }
    Box(
        modifier = modifier
            .background(ColorProvider(background))
            .cornerRadius(12.dp)
            .padding(vertical = 12.dp)
            .clickable(actionStartActivity(intent)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(fontWeight = FontWeight.Medium, color = ColorProvider(textColor)),
        )
    }
}
