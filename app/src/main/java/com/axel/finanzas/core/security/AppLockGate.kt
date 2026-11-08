package com.axel.finanzas.core.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.designsystem.FinTheme
import kotlinx.coroutines.launch

/**
 * Envuelve el contenido real de la app. Mientras no sepamos si el
 * bloqueo está activo (null) no mostramos nada real — eso sería un
 * flash de contenido sin protección. Si está activo y no se ha
 * desbloqueado en esta sesión del proceso, muestra [LockScreen] encima.
 */
@Composable
fun AppLockGate(viewModel: LockViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val lockEnabled by viewModel.isLockEnabled.collectAsState()
    var unlocked by remember { mutableStateOf(false) }

    when {
        lockEnabled == null -> Box(modifier = Modifier.fillMaxSize())
        lockEnabled == false || unlocked -> content()
        else -> LockScreen(viewModel = viewModel, onUnlocked = { unlocked = true })
    }
}

@Composable
private fun LockScreen(viewModel: LockViewModel, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (activity != null && biometricAvailable(activity)) {
            showBiometricPrompt(activity, onSuccess = onUnlocked, onError = { /* se queda en PIN */ })
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(FinTheme.spacing.xl), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.lg),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = "Finanzas está bloqueada", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input -> pin = input.filter(Char::isDigit).take(6); error = false },
                    label = { Text("PIN") },
                    isError = error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error) {
                    Text(
                        text = "PIN incorrecto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            if (viewModel.verifyPin(pin)) onUnlocked() else error = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Entrar")
                }
                if (activity != null && biometricAvailable(activity)) {
                    Button(
                        onClick = { showBiometricPrompt(activity, onSuccess = onUnlocked, onError = {}) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Usar huella")
                    }
                }
            }
        }
    }
}
