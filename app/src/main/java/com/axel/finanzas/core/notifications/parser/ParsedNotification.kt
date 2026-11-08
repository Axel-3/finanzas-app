package com.axel.finanzas.core.notifications.parser

import com.axel.finanzas.core.database.entity.NotificationRuleKind

data class ParsedNotification(
    val amountCents: Long,
    val kind: NotificationRuleKind,
    val merchant: String?,
    val last4: String?,
    val balanceCents: Long?,
)
