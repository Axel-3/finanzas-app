# Finanzas

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room-SQLCipher-00C853)
![Hilt](https://img.shields.io/badge/DI-Hilt-FF6F00)
![minSdk](https://img.shields.io/badge/minSdk-26-informational)

App de finanzas personales para Android. La hice porque me cansé de anotar cada gasto a mano: la app lee las notificaciones que mandan el banco y las tarjetas, y con eso registra los movimientos sola.

No usa servidor ni APIs externas. Todo se guarda en el teléfono, cifrado.

## Qué hace

- Lee las notificaciones de BBVA, Citibanamex, Banco Azteca, Mercado Pago, RappiCard y Nu, y asigna cada movimiento a su tarjeta por los últimos 4 dígitos. No necesita SMS ni acceso a la banca en línea.
- Si una notificación no se entiende bien, no intenta adivinar: la manda a una bandeja "Por revisar" para clasificarla a mano.
- Tarjetas de débito y crédito con límite, día de corte, fecha límite de pago y alerta de uso. Una cuenta puede tener varias tarjetas (por ejemplo la física y la digital) con el mismo saldo.
- Efectivo, débito y ahorro van por separado, cada uno con su resumen. No los junto en un solo "saldo total" porque no son lo mismo.
- Pagos recurrentes y pagos futuros, con recordatorios, calendario y una proyección del saldo a 30 días.
- Metas de ahorro y presupuestos por categoría, con avisos al 80 % y al 100 %.
- Reportes de gasto por categoría, comparación de los últimos 6 meses y exportación a CSV.
- Un widget para registrar un gasto en efectivo sin abrir la app.

## Seguridad

Como la app guarda información financiera, esta parte fue la que más cuidé:

- La base de datos está cifrada con [SQLCipher](https://www.zetetic.net/sqlcipher/). La llave es de 256 bits, se genera una sola vez con `SecureRandom` y se guarda protegida por el Android Keystore (con `EncryptedSharedPreferences`). El usuario nunca la ve ni la escribe.
- La app se puede bloquear con PIN o huella. Ese bloqueo es aparte del cifrado de la base.
- Las pantallas con datos sensibles usan `FLAG_SECURE`, así que no se pueden capturar ni grabar.
- Los respaldos son manuales y van cifrados con AES-256-GCM, con una clave derivada con PBKDF2. El archivo queda donde el usuario decida.
- No hay backend, ni analítica de terceros, y está desactivado el `allowBackup` automático de Android.

## Cómo está hecha

Kotlin con Jetpack Compose (Material 3), una sola `Activity` con `NavHost`. Room sobre SQLCipher para los datos, Hilt para inyección de dependencias, WorkManager para los recordatorios, un `NotificationListenerService` para leer las notificaciones y Jetpack Glance para el widget.

```
core/
  database/        Entidades, DAOs, migraciones, reglas de notificación, cifrado
  designsystem/    Tema, tipografía, espaciados, componentes base
  security/        Bloqueo de la app, llave de cifrado
  backup/          Exportar y restaurar respaldos cifrados
  notifications/   Listener, parser de notificaciones, simulador de reglas
domain/
  model/           Modelos de dominio, sin dependencia de Room
  repository/      Interfaces de repositorios
data/
  repository/      Implementación sobre Room
feature/
  home/ movements/ cards/ recurring/ futurepayments/
  goals/ budgets/ reports/ settings/ inbox/
navigation/        NavHost y barra inferior
widget/            Widget de captura rápida
```

El parser de notificaciones es una función pura: no depende de Android ni de Room, así que se puede probar por separado. Compara el texto contra reglas con expresiones regulares que están guardadas en la base de datos y se pueden editar desde la misma app, sin recompilar. Cada regla dice explícitamente si el movimiento es cargo, depósito, pago de tarjeta o devolución.

Las migraciones de la base de datos están versionadas y nunca borran datos.

## Compilar

Necesitas JDK 17 y un `local.properties` con `sdk.dir` apuntando a tu Android SDK.

```bash
export JAVA_HOME="<ruta a tu JDK 17>"
./gradlew :app:assembleDebug
```
