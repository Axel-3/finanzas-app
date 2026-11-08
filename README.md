# Finanzas

App de finanzas personales para Android. Registra gastos e ingresos automáticamente leyendo las notificaciones de tus tarjetas y bancos, sin depender de ningún backend ni API externa — todo vive cifrado en el teléfono.

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room-SQLCipher-00C853)
![Hilt](https://img.shields.io/badge/DI-Hilt-FF6F00)
![minSdk](https://img.shields.io/badge/minSdk-26-informational)

## Qué hace

- **Captura automática de movimientos**: un `NotificationListenerService` interpreta las notificaciones de banco en tiempo real (BBVA, Citibanamex, Banco Azteca, Mercado Pago, RappiCard, Nu) y las asigna a la tarjeta correcta por los últimos 4 dígitos — sin servidor, sin SMS, sin acceso a datos bancarios.
- **"Por revisar"**: cuando una notificación no se puede interpretar con certeza, cae a una bandeja para clasificarla a mano en vez de adivinar.
- **Tarjetas**: débito y crédito con límite, día de corte, fecha límite de pago y alerta de uso. Una misma cuenta puede tener varios números de tarjeta (física + digital) compartiendo el mismo saldo.
- **Cuentas de efectivo, débito y ahorro** por separado, cada una visible en su propio resumen — sin mezclar conceptos distintos en un solo "saldo total".
- **Pagos recurrentes y futuros** con recordatorios, calendario de próximos pagos y proyección de saldo a 30 días.
- **Metas de ahorro y presupuestos** por categoría, con alertas al 80 % y 100 %.
- **Reportes**: gasto por categoría, comparativa de los últimos 6 meses, exportación a CSV.
- **Widget de pantalla de inicio** para capturar un gasto en efectivo sin abrir la app.

## Seguridad

- **Base de datos cifrada en reposo** con [SQLCipher](https://www.zetetic.net/sqlcipher/). La llave (256 bits) se genera una sola vez con `SecureRandom` y se guarda protegida por Android Keystore vía `EncryptedSharedPreferences` — nunca se le pide al usuario, no es una contraseña de la app.
- **Bloqueo de la app** con PIN o huella (independiente del cifrado de la base de datos).
- **`FLAG_SECURE`** bloquea capturas de pantalla y grabación en las pantallas con datos sensibles.
- **Respaldo manual cifrado** (AES-256-GCM, clave derivada con PBKDF2) a un archivo que el usuario controla.
- Sin backend, sin analítica de terceros, sin `allowBackup` automático de Android.

## Stack

- **UI**: Kotlin + Jetpack Compose (Material 3), un solo `Activity` con `NavHost`.
- **Datos**: Room sobre SQLCipher, migraciones versionadas (nunca destructivas sobre datos reales).
- **DI**: Hilt.
- **Background**: WorkManager (recordatorios) + `NotificationListenerService` (captura).
- **Widget**: Jetpack Glance.

## Arquitectura

```
core/
  database/       Entities, DAOs, migraciones, reglas de notificación, cifrado
  designsystem/    Tema, tipografía, espaciados, componentes base
  security/        Bloqueo de app, llave de cifrado de la base de datos
  backup/          Exportar/restaurar cifrado
  notifications/   Listener, parser de notificaciones, simulador de reglas
domain/
  model/           Modelos de dominio independientes de Room
  repository/      Interfaces de repositorios
data/
  repository/      Implementación sobre Room
feature/
  home/ movements/ cards/ recurring/ futurepayments/
  goals/ budgets/ reports/ settings/ inbox/
navigation/        NavHost + bottom nav
widget/            Widget de captura rápida
```

El parser de notificaciones es una función pura (sin Android, sin Room) que matchea el texto contra reglas declarativas (regex) almacenadas en base de datos y editables desde la app, sin necesidad de recompilar. Cada regla declara explícitamente si es un cargo, depósito, pago de tarjeta o devolución — nunca se adivina por palabras clave.

## Compilar

```bash
export JAVA_HOME="<ruta a tu JDK 17>"
./gradlew :app:assembleDebug
```

Necesita un `local.properties` con `sdk.dir` apuntando a tu Android SDK.
