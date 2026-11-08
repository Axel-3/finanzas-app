package com.axel.finanzas

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.axel.finanzas.core.database.DefaultDataSeeder
import com.axel.finanzas.core.reminders.scheduleDaily
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Punto de entrada de Hilt. Sin esto, @AndroidEntryPoint en MainActivity
 * (y el NotificationListenerService) no tiene de dónde colgarse.
 *
 * También es el punto de entrada de WorkManager: implementar
 * `Configuration.Provider` con [HiltWorkerFactory] es lo que le permite a
 * [com.axel.finanzas.core.reminders.PaymentReminderWorker] recibir sus
 * dependencias por Hilt en vez de tener que construirlas a mano.
 */
@HiltAndroidApp
class FinanzasApp : Application(), Configuration.Provider {

    @Inject lateinit var applicationScope: CoroutineScope
    @Inject lateinit var defaultDataSeeder: DefaultDataSeeder
    @Inject lateinit var hiltWorkerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { defaultDataSeeder.seedIfNeeded() }
        scheduleDaily(WorkManager.getInstance(this))
    }
}
