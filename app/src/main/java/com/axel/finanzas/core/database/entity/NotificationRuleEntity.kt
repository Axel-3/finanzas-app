package com.axel.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Qué tipo de movimiento ES una notificación que matchea esta regla.
 * Se declara explícito en la regla — NO se adivina buscando palabras
 * como "pago"/"abono" en el texto. Esa heurística se intentó primero y
 * resultó incorrecta: en Mercado Pago "Pagaste" significa que gastaste,
 * no que pagaste una tarjeta de crédito.
 */
enum class NotificationRuleKind {
    /** Gasto: compra, cargo, retiro de cajero, transferencia que envías. */
    CARGO,

    /** Dinero que entra a una cuenta de efectivo/débito/billetera: depósito, transferencia recibida. */
    DEPOSITO,

    /** Pago que reduce la deuda de una tarjeta de crédito específicamente. */
    PAGO_TARJETA,

    /** Devolución de un cargo (reembolso de un comercio). */
    DEVOLUCION,
}

/**
 * Regla por banco para interpretar sus notificaciones. Editable desde la
 * app (pantalla "Reglas de notificaciones" en Más), nunca hardcodeada.
 *
 * `amountRegex` debe traer un grupo de captura `(...)` con el monto
 * (solo dígitos, comas y punto decimal). `merchantRegex` y `last4Regex`
 * son opcionales, igual con un grupo de captura.
 */
@Entity(tableName = "notification_rules")
data class NotificationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val bankName: String,
    val kind: NotificationRuleKind,
    val amountRegex: String,
    val merchantRegex: String? = null,
    val last4Regex: String? = null,
    val balanceRegex: String? = null,
    val isActive: Boolean = true,
    val priority: Int = 0,
    val isExample: Boolean = false,
)
