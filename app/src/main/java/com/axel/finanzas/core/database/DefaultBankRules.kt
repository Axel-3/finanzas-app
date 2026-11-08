package com.axel.finanzas.core.database

import com.axel.finanzas.core.database.entity.NotificationRuleEntity
import com.axel.finanzas.core.database.entity.NotificationRuleKind

/**
 * Reglas para bancos mexicanos comunes. Los formatos de notificación
 * cambian entre bancos y con el tiempo, así que esto se ajusta en
 * cuanto una regla deja de matchear (pantalla "Reglas de notificaciones"
 * en Más).
 *
 * Separado de [DefaultDataSeeder] para que [com.axel.finanzas.core.notifications.parser.NotificationParser]
 * se pruebe contra EXACTAMENTE las reglas que se siembran de verdad, sin
 * copias que se puedan desincronizar.
 *
 * Ninguna de estas apps menciona los últimos 4 dígitos en sus
 * notificaciones de depósito/cargo salvo BBVA, Citibanamex y Banco
 * Azteca — Nu, Mercado Pago y RappiCard mandan todo a "Por revisar"
 * para elegir la cuenta a mano, porque sin last4 no hay forma segura de
 * adivinar a cuál tarjeta pertenece.
 */
object DefaultBankRules {

    /**
     * Sube cada vez que el contenido de [all] cambia de forma que valga
     * la pena resembrar en los teléfonos que ya tienen reglas instaladas
     * — [com.axel.finanzas.core.database.DefaultDataSeeder] compara esto
     * contra lo último sembrado y, si no coincide, borra y vuelve a
     * sembrar `notification_rules` por completo. Sin esto, una corrección
     * de regex aquí en el código nunca le llegaría a un teléfono que ya
     * sembró la versión anterior (el seeder original solo sembraba "si la
     * tabla está vacía", una sola vez en la vida del teléfono).
     *
     * Notas de versiones relevantes:
     * - Los `packageName` iniciales de BBVA/Citibanamex/Banco Azteca eran
     *   nombres de paquete inventados que no existen de verdad. Con el
     *   nombre equivocado, `getActiveForPackage` nunca encontraba ninguna
     *   regla sin importar qué tan bien escrito estuviera el regex — todo
     *   caía a "Por revisar" sin monto. Corregidos a los paquetes reales:
     *   BBVA es `com.bancomer.mbanking` (la app sigue con la marca vieja
     *   "Bancomer"), Citibanamex es `com.citibanamex.banamexmobile`,
     *   Banco Azteca es `mx.com.bancoazteca.bazdigitalmovil`.
     * - Nu no tenía ninguna regla para dinero que SALE de la cuenta
     *   ("Enviaste $X a NOMBRE"), solo para compras y depósitos.
     * - RappiCard manda pre-autorizaciones ("NEGOCIO usó $X para
     *   pre-autorizar...") con wording distinto a "Compraste"/"Compra
     *   de", sin ninguna regla que las cubriera antes.
     */
    const val VERSION = 5

    fun all(): List<NotificationRuleEntity> = listOf(
        // ---- Nu México ----
        NotificationRuleEntity(
            packageName = "com.nu.production",
            bankName = "Nu",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """(?:Compra de|Compraste)\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """en\s+(.+?)(?:\s+aprobada|\s+procesada|\.|$)""",
        ),
        NotificationRuleEntity(
            packageName = "com.nu.production",
            bankName = "Nu",
            kind = NotificationRuleKind.DEPOSITO,
            amountRegex = """Recibiste(?:\s+una\s+transferencia\s+de)?\s+\$\s?([0-9,]+\.[0-9]{2})""",
            // "de" aparece dos veces en "transferencia de $X de NOMBRE" — hay que
            // anclar al "de" que sigue al monto, no al primero que aparece.
            merchantRegex = """\$[0-9,]+\.[0-9]{2}\s+de\s+(.+?)(?:\.|$)""",
        ),
        // "Transferencia exitosa Enviaste $100.00 a Juan Pérez López." —
        // formato confirmado con notificación real. Dinero que SALE de la
        // cuenta; cae en CARGO igual que una compra (ver
        // NotificationRuleKind.CARGO: "transferencia que envías" ya
        // estaba contemplado ahí).
        NotificationRuleEntity(
            packageName = "com.nu.production",
            bankName = "Nu",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """Enviaste\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """\ba\b\s+(.+?)(?:\.|$)""",
        ),

        // ---- BBVA México ----
        // Formato real confirmado con capturas de notificación:
        // "Tienes un cargo a tu cuenta *3198 de $380.00" / "Abono a
        // tu cuenta *73198 de $980.00" — sin nombre de comercio/remitente,
        // el campo "cuenta" puede traer más de 4 dígitos (regla de last4
        // siempre se queda con los últimos 4 vía \d*(\d{4})). Por eso van
        // PRIMERO: son las que de verdad llegan en la práctica.
        NotificationRuleEntity(
            packageName = "com.bancomer.mbanking",
            bankName = "BBVA",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """cargo\s+a\s+tu\s+cuenta\s+\*\d+\s+de\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = null,
            last4Regex = """cargo\s+a\s+tu\s+cuenta\s+\*\d*(\d{4})""",
        ),
        NotificationRuleEntity(
            packageName = "com.bancomer.mbanking",
            bankName = "BBVA",
            kind = NotificationRuleKind.DEPOSITO,
            amountRegex = """abono\s+a\s+tu\s+cuenta\s+\*\d+\s+de\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = null,
            last4Regex = """abono\s+a\s+tu\s+cuenta\s+\*\d*(\d{4})""",
        ),
        // Formatos de compra/traspaso con nombre de comercio o remitente —
        // reportados, nunca vistos en capturas reales. Se dejan por si
        // BBVA manda este wording para otro tipo de
        // movimiento (p.ej. compra con tarjeta física distinta de los
        // avisos de cuenta de arriba); si nunca aparecen, se pueden quitar.
        NotificationRuleEntity(
            packageName = "com.bancomer.mbanking",
            bankName = "BBVA",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """(?:Compra|Cargo|Retiro(?:\s+sin\s+tarjeta)?)\s+por\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """en\s+(.+?)\s+con\s+tu\s+tarjeta""",
            last4Regex = """tarjeta(?:\s+digital)?\s+\*\d*(\d{4})""",
        ),
        NotificationRuleEntity(
            packageName = "com.bancomer.mbanking",
            bankName = "BBVA",
            kind = NotificationRuleKind.DEPOSITO,
            amountRegex = """(?:Recibiste un traspaso por|Abono por)\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """de\s+(.+?)\.""",
            last4Regex = """cuenta\s+\*\d*(\d{4})""",
        ),

        // ---- Mercado Pago ----
        // Sin últimos 4 dígitos en estos formatos -> siempre cae a "Por revisar".
        NotificationRuleEntity(
            packageName = "com.mercadopago.wallet",
            bankName = "Mercado Pago",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """(?:Pagaste|Hiciste una transferencia de)\s+\$\s?([0-9,]+\.[0-9]{2})""",
            // \b en los límites de "a"/"en": sin esto, matchea dentro de
            // palabras como "una" (la "a" final de "una transferencia").
            merchantRegex = """\b(?:a|en)\b\s+(.+?)(?:\s+con|\.|$)""",
        ),
        // "Ingresaste $ 720.00 desde tu cuenta de BBVA MEXICO." — confirmado
        // con captura real 2026-11-06. El verbo real es "Ingresaste", NO
        // "Ingresaron" (ese seguía sin matchear nada); el monto también
        // puede traer un espacio después del "$".
        NotificationRuleEntity(
            packageName = "com.mercadopago.wallet",
            bankName = "Mercado Pago",
            kind = NotificationRuleKind.DEPOSITO,
            amountRegex = """(?:Ingresaste|Ingresaron|te env[ií]o)\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """desde\s+(.+?)(?:\.|$)""",
        ),

        // ---- RappiCard (nombre de paquete varía según integración) ----
        NotificationRuleEntity(
            packageName = "com.rappi.bank",
            bankName = "RappiCard",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """(?:Compraste|Compra de)\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """en\s+(.+?)(?:\s+y\s+acumulaste|\s+aprobada|\.|$)""",
        ),
        // "Tu pago de $681.00 a tu Rappicard fue exitoso" — confirmado con
        // captura real 2026-11-06, ya matcheaba con la regex anterior.
        NotificationRuleEntity(
            packageName = "com.rappi.bank",
            bankName = "RappiCard",
            kind = NotificationRuleKind.PAGO_TARJETA,
            amountRegex = """[Pp]ago\s+de\s+\$\s?([0-9,]+\.[0-9]{2})""",
        ),
        NotificationRuleEntity(
            packageName = "com.grability.rappi",
            bankName = "RappiCard (app Rappi)",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """(?:Compraste|Compra de)\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """en\s+(.+?)(?:\s+y\s+acumulaste|\s+aprobada|\.|$)""",
        ),
        NotificationRuleEntity(
            packageName = "com.grability.rappi",
            bankName = "RappiCard (app Rappi)",
            kind = NotificationRuleKind.PAGO_TARJETA,
            amountRegex = """[Pp]ago\s+de\s+\$\s?([0-9,]+\.[0-9]{2})""",
        ),
        // "Se realizó una compra 🤑 AUTOB METROTAP usó $5.00 para
        // pre-autorizar una compra con tu RappiCard digital." — confirmado
        // con notificación real 2026-11-07, una pre-autorización (no
        // "Compraste"/"Compra de"). Sin merchantRegex: el nombre del
        // comercio viene ANTES del monto en vez de después de "en", y con
        // el emoji de por medio no vale la pena un regex frágil — el
        // texto completo igual se ve en "Por revisar".
        NotificationRuleEntity(
            packageName = "com.grability.rappi",
            bankName = "RappiCard (app Rappi)",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """us[oó]\s+\$\s?([0-9,]+\.[0-9]{2})\s+para\s+pre-autorizar""",
        ),

        // ---- Citibanamex ----
        // Formato real confirmado con capturas de notificación:
        // "Retiro/Compra DLO*Uber rides CDMX CMXMX Tarjeta M.N. 693
        // monto $21.38 el 13/04/26 12:05:00 PM. Auto. 934221" / "Deposito
        // Cheques M.N. 848 monto $140.00 el 11/04/26 06:39:00 PM No.
        // Autorizacion 1089266...". Nada que ver con "terminación NNNN" —
        // el número después de "M.N." (693, 848) solo trae 3 dígitos, no
        // 4, así que no es seguro tratarlo como last4 (podría ser número
        // de cheque, no de tarjeta); se deja sin last4Regex y cae a "Por
        // revisar" a propósito, como Nu/Mercado Pago/RappiCard.
        NotificationRuleEntity(
            packageName = "com.citibanamex.banamexmobile",
            bankName = "Citibanamex",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """Retiro/Compra\s+.+?\s+monto\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """Retiro/Compra\s+(.+?)\s+M\.N\.""",
        ),
        NotificationRuleEntity(
            packageName = "com.citibanamex.banamexmobile",
            bankName = "Citibanamex",
            kind = NotificationRuleKind.DEPOSITO,
            amountRegex = """Deposito\s+.+?\s+monto\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """Deposito\s+(.+?)\s+M\.N\.""",
        ),
        // Formatos reportados, nunca vistos en capturas reales — se
        // dejan como respaldo por si Banamex manda este wording para
        // otro tipo de movimiento (p.ej. notificaciones push distintas
        // de las de SMS/legacy de arriba).
        NotificationRuleEntity(
            packageName = "com.citibanamex.banamexmobile",
            bankName = "Citibanamex",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """(?:Compra aprobada por|Cargo de)\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """en\s+(.+?)(?:\s+con\s+tarjeta|\s+el\s+\d|\.|$)""",
            last4Regex = """terminaci[oó]n\s+\d*(\d{4})""",
        ),
        NotificationRuleEntity(
            packageName = "com.citibanamex.banamexmobile",
            bankName = "Citibanamex",
            kind = NotificationRuleKind.DEPOSITO,
            amountRegex = """(?:Dep[oó]sito recibido por|Transferencia recibida por)\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """de\s+(.+?)(?:\.|$)""",
            last4Regex = """terminaci[oó]n\s+\d*(\d{4})""",
        ),

        // ---- Banco Azteca ----
        NotificationRuleEntity(
            packageName = "mx.com.bancoazteca.bazdigitalmovil",
            bankName = "Banco Azteca",
            kind = NotificationRuleKind.CARGO,
            amountRegex = """(?:compra por|Retiro de efectivo por|Enviaste)\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """en\s+(.+?)\s+con\s+tu\s+tarjeta""",
            last4Regex = """tarjeta\s+\*\d*(\d{4})""",
        ),
        NotificationRuleEntity(
            packageName = "mx.com.bancoazteca.bazdigitalmovil",
            bankName = "Banco Azteca",
            kind = NotificationRuleKind.DEPOSITO,
            amountRegex = """(?:Recibiste|Abono de)\s+\$\s?([0-9,]+\.[0-9]{2})""",
            merchantRegex = """de\s+(.+?)(?:\.|$)""",
        ),
    )
}
