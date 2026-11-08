package com.axel.finanzas.widget

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.axel.finanzas.core.database.entity.MovementType
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.FinanzasTheme
import com.axel.finanzas.core.designsystem.components.FinCard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Ventana flotante que abren los botones del widget (ver FinanzasWidget).
 * A propósito NO es la app completa: solo pide el monto, guarda el
 * movimiento en efectivo y se cierra. El tema `Theme.Finanzas.Dialog`
 * (AndroidManifest) es lo que la hace verse flotante sobre lo que estabas
 * haciendo en vez de taparlo con un splash completo.
 */
@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {

    private val viewModel: QuickAddViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val typeExtra = intent.getStringExtra(QUICK_ADD_TYPE_EXTRA)
        val type = if (typeExtra == QuickAddType.INCOME.name) MovementType.INCOME else MovementType.EXPENSE

        setContent {
            FinanzasTheme {
                QuickAddDialogContent(
                    type = type,
                    onSave = { amountCents ->
                        lifecycleScope.launch {
                            viewModel.saveQuickMovement(type, amountCents)
                            finish()
                        }
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}

@Composable
private fun QuickAddDialogContent(
    type: MovementType,
    onSave: (amountCents: Long) -> Unit,
    onCancel: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    val amountCents = (amountText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FinTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        FinCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.lg)) {
                Text(
                    text = if (type == MovementType.INCOME) "Ingreso en efectivo" else "Gasto en efectivo",
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Monto (MXN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onCancel) { Text("Cancelar") }
                    Spacer(Modifier.width(FinTheme.spacing.sm))
                    Button(onClick = { onSave(amountCents) }, enabled = amountCents > 0) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
