package com.axel.finanzas

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.axel.finanzas.core.designsystem.FinanzasTheme
import com.axel.finanzas.core.security.AppLockGate
import com.axel.finanzas.navigation.FinanzasNavGraph
import dagger.hilt.android.AndroidEntryPoint

/**
 * Extiende FragmentActivity (no ComponentActivity) porque BiometricPrompt
 * lo exige para sobrevivir cambios de configuración — ver AppLockGate.kt.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Saldos y movimientos nunca deben quedar en una captura de
        // pantalla, grabación o la miniatura de "apps recientes".
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            FinanzasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppLockGate {
                        FinanzasNavGraph()
                    }
                }
            }
        }
    }
}
