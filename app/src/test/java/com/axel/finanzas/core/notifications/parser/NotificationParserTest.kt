package com.axel.finanzas.core.notifications.parser

import com.axel.finanzas.core.database.DefaultBankRules
import com.axel.finanzas.core.database.entity.NotificationRuleEntity
import com.axel.finanzas.core.database.entity.NotificationRuleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueba el parser contra las MISMAS reglas que se siembran de verdad
 * ([DefaultBankRules]). Algunos bancos tienen más de una regla por
 * banco+tipo (la confirmada con capturas reales primero, la reportada
 * como respaldo) — [parseAsApp] elige la regla igual que lo hace
 * [com.axel.finanzas.core.notifications.FinanzasNotificationListenerService]
 * en producción: probando cada regla activa del paquete hasta que una
 * interprete el texto, sin asumir cuál es "la primera" de la lista.
 */
class NotificationParserTest {

    private val rules = DefaultBankRules.all()

    private fun ruleFor(packageName: String, kind: NotificationRuleKind): NotificationRuleEntity =
        rules.first { it.packageName == packageName && it.kind == kind }

    private fun parseAsApp(packageName: String, text: String): ParsedNotification? =
        rules.filter { it.packageName == packageName }
            .firstNotNullOfOrNull { rule -> NotificationParser.parse(text, rule) }

    // ---- Nu ----

    @Test
    fun `Nu compra simple`() {
        val rule = ruleFor("com.nu.production", NotificationRuleKind.CARGO)
        val result = NotificationParser.parse("Compra de \$150.00 en OXXO aprobada.", rule)

        assertEquals(15000L, result?.amountCents)
        assertEquals("OXXO", result?.merchant)
        assertEquals(NotificationRuleKind.CARGO, result?.kind)
    }

    @Test
    fun `Nu compra con monto con miles`() {
        val rule = ruleFor("com.nu.production", NotificationRuleKind.CARGO)
        val result = NotificationParser.parse("Compra de \$1,200.00 en AMAZON MEXICO procesada.", rule)

        assertEquals(120000L, result?.amountCents)
        assertEquals("AMAZON MEXICO", result?.merchant)
    }

    @Test
    fun `Nu deposito recibido (formato investigado)`() {
        val rule = ruleFor("com.nu.production", NotificationRuleKind.DEPOSITO)
        val result = NotificationParser.parse("Recibiste una transferencia de \$1,500.00 de EMPRESA SA.", rule)

        assertEquals(150000L, result?.amountCents)
        assertEquals("EMPRESA SA", result?.merchant)
        assertEquals(NotificationRuleKind.DEPOSITO, result?.kind)
    }

    @Test
    fun `Nu deposito recibido - formato real confirmado`() {
        // "¡Recibiste una trans... Recibiste $170.00 en tu Cuenta Nu."
        val result = parseAsApp("com.nu.production", "Recibiste \$170.00 en tu Cuenta Nu.")

        assertEquals(17000L, result?.amountCents)
        assertEquals(NotificationRuleKind.DEPOSITO, result?.kind)
        assertNull(result?.merchant) // no hay remitente en este formato, no inventar uno
    }

    @Test
    fun `Nu transferencia enviada - formato real confirmado, antes no matcheaba nada`() {
        // "Transferencia exitosa Enviaste $100.00 a Juan Pérez López."
        val result = parseAsApp(
            "com.nu.production",
            "Transferencia exitosa Enviaste \$100.00 a Juan Pérez López.",
        )

        assertEquals(10000L, result?.amountCents)
        assertEquals(NotificationRuleKind.CARGO, result?.kind)
        assertEquals("Juan Pérez López", result?.merchant)
    }

    // ---- BBVA ----

    @Test
    fun `BBVA cargo a cuenta - formato real confirmado`() {
        // "Aviso de cargo en tu cuenta" + "Tienes un cargo a tu cuenta *3198 de $380.00"
        val result = parseAsApp(
            "com.bancomer.mbanking",
            "Aviso de cargo en tu cuenta Tienes un cargo a tu cuenta *3198 de \$380.00",
        )

        assertEquals(38000L, result?.amountCents)
        assertEquals(NotificationRuleKind.CARGO, result?.kind)
        assertEquals("3198", result?.last4)
        assertNull(result?.merchant) // este formato no trae nombre de comercio
    }

    @Test
    fun `BBVA abono a cuenta - formato real confirmado, last4 con digito extra`() {
        // "BBVA" + "Abono a tu cuenta *73198 de $980.00" -- el numero despues
        // del asterisco trae un digito de mas; debe quedarse con los
        // ultimos 4 (coincide con el *3198 de la captura de cargo arriba,
        // misma cuenta).
        val result = parseAsApp("com.bancomer.mbanking", "BBVA Abono a tu cuenta *73198 de \$980.00")

        assertEquals(98000L, result?.amountCents)
        assertEquals(NotificationRuleKind.DEPOSITO, result?.kind)
        assertEquals("3198", result?.last4)
    }

    @Test
    fun `BBVA compra con tarjeta digital extrae last4 (formato investigado, respaldo)`() {
        val result = parseAsApp("com.bancomer.mbanking", "Compra por \$340.50 en STARBUCKS con tu tarjeta digital *1234.")

        assertEquals(34050L, result?.amountCents)
        assertEquals("STARBUCKS", result?.merchant)
        assertEquals("1234", result?.last4)
    }

    @Test
    fun `BBVA retiro sin tarjeta tambien es cargo (formato investigado, respaldo)`() {
        val result = parseAsApp("com.bancomer.mbanking", "Retiro sin tarjeta por \$1,000.00 en Cajero BBVA.")

        assertEquals(100000L, result?.amountCents)
        assertNull(result?.last4)
    }

    @Test
    fun `BBVA traspaso recibido (formato investigado, respaldo)`() {
        val result = parseAsApp("com.bancomer.mbanking", "Recibiste un traspaso por \$2,000.00 de MARIA LOPEZ.")

        assertEquals(200000L, result?.amountCents)
        assertEquals("MARIA LOPEZ", result?.merchant)
    }

    // ---- Mercado Pago ----

    @Test
    fun `Mercado Pago pagaste es un cargo, no un pago de tarjeta`() {
        // Este es justo el caso que rompía la heurística anterior basada en
        // palabras clave: "Pagaste" aquí significa gasto, no abono a tarjeta.
        val result = parseAsApp("com.mercadopago.wallet", "Pagaste \$75.00 a OXXO con Dinero en cuenta.")

        assertEquals(7500L, result?.amountCents)
        assertEquals("OXXO", result?.merchant)
        assertEquals(NotificationRuleKind.CARGO, result?.kind)
    }

    @Test
    fun `Mercado Pago transferencia enviada`() {
        val result = parseAsApp("com.mercadopago.wallet", "Hiciste una transferencia de \$300.00 a CARLOS SANCHEZ.")

        assertEquals(30000L, result?.amountCents)
        assertEquals("CARLOS SANCHEZ", result?.merchant)
    }

    @Test
    fun `Mercado Pago dinero recibido - formato real confirmado, verbo Ingresaste con espacio en el monto`() {
        // "Tu dinero ya está disponible" + "Ingresaste $ 720.00 desde tu
        // cuenta de BBVA MEXICO." -- el verbo real es "Ingresaste", no
        // "Ingresaron" (investigado), y el monto trae un espacio tras el "$".
        val result = parseAsApp(
            "com.mercadopago.wallet",
            "Tu dinero ya está disponible Ingresaste \$ 720.00 desde tu cuenta de BBVA MEXICO.",
        )

        assertEquals(72000L, result?.amountCents)
        assertEquals(NotificationRuleKind.DEPOSITO, result?.kind)
        assertEquals("tu cuenta de BBVA MEXICO", result?.merchant)
    }

    // ---- RappiCard ----

    @Test
    fun `RappiCard compra`() {
        val result = parseAsApp(
            "com.rappi.bank",
            "Compraste \$230.00 en FARMACIAS GUADALAJARA y acumulaste \$4.60 de cashback.",
        )

        assertEquals(23000L, result?.amountCents)
        assertEquals("FARMACIAS GUADALAJARA", result?.merchant)
    }

    @Test
    fun `RappiCard pago de tarjeta - formato real confirmado`() {
        // "Tu pago de $681.00 a tu Rappicard fue exitoso"
        val result = parseAsApp("com.rappi.bank", "Tu pago de \$681.00 a tu Rappicard fue exitoso")

        assertEquals(68100L, result?.amountCents)
        assertEquals(NotificationRuleKind.PAGO_TARJETA, result?.kind)
    }

    @Test
    fun `RappiCard pre-autorizacion - formato real confirmado, antes no matcheaba nada`() {
        // "Se realizó una compra AUTOB METROTAP usó $5.00 para pre-autorizar una compra con tu RappiCard digital."
        val result = parseAsApp(
            "com.grability.rappi",
            "Se realizo una compra AUTOB METROTAP uso \$5.00 para pre-autorizar una compra con tu RappiCard digital.",
        )

        assertEquals(500L, result?.amountCents)
        assertEquals(NotificationRuleKind.CARGO, result?.kind)
    }

    // ---- Citibanamex ----

    @Test
    fun `Citibanamex Retiro-Compra con comercio - formato real confirmado`() {
        // "Retiro/Compra DLO*Uber rides CDMX CMXMX Tarjeta M.N. 693 monto
        // $21.38 el 13/04/26 12:05:00 PM. Auto. 934221" -- nada que ver con
        // "Compra aprobada por $X en COMERCIO" (investigado). El numero
        // tras "M.N." trae solo 3 dígitos, no es seguro tratarlo como
        // last4 de tarjeta.
        val result = parseAsApp(
            "com.citibanamex.banamexmobile",
            "Notificacion Retiro/Compra DLO*Uber rides CDMX CMXMX Tarjeta M.N. 693 monto \$21.38 el 13/04/26 12:05:00 PM. Auto. 934221",
        )

        assertEquals(2138L, result?.amountCents)
        assertEquals(NotificationRuleKind.CARGO, result?.kind)
        assertNull(result?.last4)
        assertTrue(result?.merchant?.contains("Uber") == true)
    }

    @Test
    fun `Citibanamex Retiro-Compra de cheques, sin comercio real - formato real confirmado`() {
        // "Retiro/Compra Cheques M.N. 848 monto $50.00 el 12/04/26 04:50:00 PM. Auto. 49853"
        val result = parseAsApp(
            "com.citibanamex.banamexmobile",
            "Notificacion Retiro/Compra Cheques M.N. 848 monto \$50.00 el 12/04/26 04:50:00 PM. Auto. 49853",
        )

        assertEquals(5000L, result?.amountCents)
        assertEquals(NotificationRuleKind.CARGO, result?.kind)
    }

    @Test
    fun `Citibanamex Deposito de cheques - formato real confirmado`() {
        // "Deposito Cheques M.N. 848 monto $140.00 el 11/04/26 06:39:00 PM No. Autorizacion 1089266..."
        val result = parseAsApp(
            "com.citibanamex.banamexmobile",
            "Notificacion Deposito Cheques M.N. 848 monto \$140.00 el 11/04/26 06:39:00 PM No. Autorizacion 1089266. En operaciones con cheque valida tu saldo antes de realizar cualquier operacion.",
        )

        assertEquals(14000L, result?.amountCents)
        assertEquals(NotificationRuleKind.DEPOSITO, result?.kind)
    }

    @Test
    fun `Citibanamex compra con terminacion (formato investigado, respaldo)`() {
        val result = parseAsApp(
            "com.citibanamex.banamexmobile",
            "Compra aprobada por \$620.00 en CHEDRAUI con tarjeta terminación 4321.",
        )

        assertEquals(62000L, result?.amountCents)
        assertEquals("CHEDRAUI", result?.merchant)
        assertEquals("4321", result?.last4)
    }

    @Test
    fun `Citibanamex deposito recibido (formato investigado, respaldo)`() {
        val result = parseAsApp(
            "com.citibanamex.banamexmobile",
            "Depósito recibido por \$3,500.00 en tu cuenta terminación 8765.",
        )

        assertEquals(350000L, result?.amountCents)
        assertEquals("8765", result?.last4)
    }

    // ---- Banco Azteca ----

    @Test
    fun `Banco Azteca compra con tarjeta`() {
        val result = parseAsApp(
            "mx.com.bancoazteca.bazdigitalmovil",
            "Realizaste una compra por \$120.00 en 7-ELEVEN con tu tarjeta *3412.",
        )

        assertEquals(12000L, result?.amountCents)
        assertEquals("7-ELEVEN", result?.merchant)
        assertEquals("3412", result?.last4)
    }

    @Test
    fun `Banco Azteca deposito recibido`() {
        val result = parseAsApp(
            "mx.com.bancoazteca.bazdigitalmovil",
            "Recibiste \$800.00 en tu cuenta Guardadito de JOSE PEREZ.",
        )

        assertEquals(80000L, result?.amountCents)
    }

    // ---- Casos generales del motor (no específicos de un banco) ----

    @Test
    fun `sin match de monto devuelve null`() {
        val rule = ruleFor("com.nu.production", NotificationRuleKind.CARGO)
        val result = NotificationParser.parse("Tu estado de cuenta ya está disponible", rule)

        assertNull(result)
    }

    @Test
    fun `regex invalido en la regla no revienta, solo no matchea`() {
        val rule = ruleFor("com.nu.production", NotificationRuleKind.CARGO).copy(amountRegex = "(\\$[")
        val result = NotificationParser.parse("Compra de \$10.00 en Tienda aprobada.", rule)

        assertNull(result)
    }

    @Test
    fun `last4 invalido se ignora en vez de romper el parseo`() {
        val rule = ruleFor("mx.com.bancoazteca.bazdigitalmovil", NotificationRuleKind.CARGO)
            .copy(last4Regex = """tarjeta \*(\d{2})""") // a propósito mal (solo 2 dígitos)
        val result = NotificationParser.parse("Realizaste una compra por \$10.00 en Tienda con tu tarjeta *34.", rule)

        assertEquals(1000L, result?.amountCents)
        assertNull(result?.last4)
    }
}
