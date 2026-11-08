package com.axel.finanzas.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import com.axel.finanzas.core.designsystem.FinTheme
import com.axel.finanzas.core.designsystem.components.AmountSign
import com.axel.finanzas.core.designsystem.components.AmountText
import com.axel.finanzas.core.designsystem.components.AnimatedProgressBar
import com.axel.finanzas.core.designsystem.components.ComingSoonScreen
import com.axel.finanzas.core.designsystem.components.FinCard
import com.axel.finanzas.core.designsystem.components.SectionHeader
import com.axel.finanzas.core.designsystem.components.formatCents
import com.axel.finanzas.domain.model.CardUi
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.min

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = FinTheme.spacing.lg,
            end = FinTheme.spacing.lg,
            top = FinTheme.spacing.lg,
            bottom = FinTheme.spacing.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.xxl),
    ) {
        item(key = "greeting") { GreetingHeader() }
        item(key = "cash") {
            SimpleBalanceCard(label = "Efectivo", caption = "Lo que tienes en mano", amountCents = state.cashBalanceCents)
        }
        item(key = "debit") {
            SimpleBalanceCard(label = "Débito", caption = "Suma de tus tarjetas de débito", amountCents = state.debitBalanceCents)
        }
        item(key = "savings") {
            SimpleBalanceCard(
                label = "Ahorros",
                caption = "Saldo libre, no ligado a una meta",
                amountCents = state.savingsBalanceCents,
                icon = Icons.Outlined.Savings,
            )
        }
        item(key = "balances") {
            BalancesSummaryCard(creditUsedCents = state.creditUsedCents, creditAvailableCents = state.creditAvailableCents)
        }
        item(key = "stats") {
            MonthStatsRow(monthExpensesCents = state.monthExpensesCents, monthIncomeCents = state.monthIncomeCents)
        }
        item(key = "breakdown") {
            PaymentMethodBreakdownCard(
                expensesCash = state.monthExpensesCashCents,
                expensesCard = state.monthExpensesCardCents,
                incomeCash = state.monthIncomeCashCents,
                incomeCard = state.monthIncomeCardCents,
            )
        }

        item(key = "cards_header") {
            SectionHeader(title = "Tus tarjetas", actionLabel = "Ver todas")
        }
        item(key = "cards_pager") {
            if (state.cards.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    ComingSoonScreen(title = "Agrega tu primera tarjeta en la pestaña Tarjetas")
                }
            } else {
                CardsPager(cards = state.cards)
            }
        }

        item(key = "upcoming_header") {
            SectionHeader(title = "Próximos pagos", actionLabel = "Ver todos")
        }
        items(state.upcomingPayments, key = { it.name + it.dueDateLabel }) { payment ->
            UpcomingPaymentRow(payment)
        }

        item(key = "movements_header") {
            SectionHeader(title = "Movimientos recientes", actionLabel = "Ver todos")
        }
        items(state.recentMovements, key = { it.merchant + it.occurredAt }) { movement ->
            MovementRow(movement)
        }
    }
}

@Composable
private fun GreetingHeader() {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        hour < 12 -> "Buenos días"
        hour < 19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    Column(verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.xs)) {
        Text(text = greeting, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Así va tu mes hasta ahora",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Una tarjeta por tipo de cuenta (Efectivo, Débito, Ahorros) en vez de
 * mezclarlos en un solo "Disponible", que generaba confusión al mostrar
 * el saldo de una tarjeta de débito como si fuera efectivo. Cada tipo
 * separado y nombrado tal cual es.
 */
@Composable
private fun SimpleBalanceCard(
    label: String,
    caption: String,
    amountCents: Long,
    icon: ImageVector = Icons.Outlined.Wallet,
) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(FinTheme.spacing.xs))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AmountText(
                amountCents = amountCents,
                sign = AmountSign.NEUTRAL,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

/**
 * Enfocada solo en crédito (usado vs. disponible). Antes mostraba
 * "Disponible" (efectivo + débito + billetera) junto a "Crédito total",
 * pero eso mezclaba el saldo de una tarjeta de débito real con un
 * concepto de "dinero disponible" que confundía. El efectivo/débito ya
 * se ve por cuenta en la pestaña Tarjetas; aquí solo importa cuánto
 * crédito queda.
 */
@Composable
private fun BalancesSummaryCard(creditUsedCents: Long, creditAvailableCents: Long) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            BalanceColumn(
                modifier = Modifier.weight(1f),
                label = "Crédito usado",
                icon = Icons.Outlined.Payments,
                amountCents = creditUsedCents,
                sign = AmountSign.NEGATIVE,
                caption = "En tus tarjetas de crédito",
            )
            VerticalDivider(
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = FinTheme.spacing.md),
                color = FinTheme.extendedColors.cardBorder,
            )
            BalanceColumn(
                modifier = Modifier.weight(1f),
                label = "Crédito disponible",
                icon = Icons.Outlined.Wallet,
                amountCents = creditAvailableCents,
                sign = AmountSign.POSITIVE,
                caption = "Límite menos lo usado",
            )
        }
    }
}

@Composable
private fun BalanceColumn(
    label: String,
    icon: ImageVector,
    amountCents: Long,
    sign: AmountSign,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(FinTheme.spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(FinTheme.spacing.xs))
        AmountText(
            amountCents = amountCents,
            sign = sign,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(FinTheme.spacing.xs))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonthStatsRow(monthExpensesCents: Long, monthIncomeCents: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FinTheme.spacing.md),
    ) {
        StatChip(
            modifier = Modifier.weight(1f),
            label = "Gastos del mes",
            amountCents = monthExpensesCents,
            icon = Icons.AutoMirrored.Outlined.TrendingDown,
            sign = AmountSign.NEGATIVE,
            containerColor = FinTheme.extendedColors.expenseContainer,
            onContainerColor = FinTheme.extendedColors.onExpenseContainer,
        )
        StatChip(
            modifier = Modifier.weight(1f),
            label = "Ingresos del mes",
            amountCents = monthIncomeCents,
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            sign = AmountSign.POSITIVE,
            containerColor = FinTheme.extendedColors.incomeContainer,
            onContainerColor = FinTheme.extendedColors.onIncomeContainer,
        )
    }
}

@Composable
private fun StatChip(
    label: String,
    amountCents: Long,
    icon: ImageVector,
    sign: AmountSign,
    containerColor: androidx.compose.ui.graphics.Color,
    onContainerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, MaterialTheme.shapes.large)
            .padding(FinTheme.spacing.md),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onContainerColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(FinTheme.spacing.xs))
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = onContainerColor)
            }
            Spacer(Modifier.height(FinTheme.spacing.sm))
            AmountText(
                amountCents = amountCents,
                sign = sign,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/**
 * Desglose de gastos e ingresos del mes por método de pago (efectivo vs.
 * tarjetas), para distinguir qué tanto del movimiento del mes es en
 * efectivo y qué tanto va por tarjeta.
 */
@Composable
private fun PaymentMethodBreakdownCard(expensesCash: Long, expensesCard: Long, incomeCash: Long, incomeCard: Long) {
    FinCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(FinTheme.spacing.lg)) {
            Text(text = "Por método de pago", style = MaterialTheme.typography.titleSmall)
            BreakdownRow(label = "Efectivo", icon = Icons.Outlined.Wallet, expenseCents = expensesCash, incomeCents = incomeCash)
            BreakdownRow(label = "Tarjetas", icon = Icons.Outlined.Payments, expenseCents = expensesCard, incomeCents = incomeCard)
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    icon: ImageVector,
    expenseCents: Long,
    incomeCents: Long,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(FinTheme.spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        AmountText(
            amountCents = expenseCents,
            sign = AmountSign.NEGATIVE,
            style = MaterialTheme.typography.labelLarge,
            animate = false,
        )
        Spacer(Modifier.width(FinTheme.spacing.md))
        AmountText(
            amountCents = incomeCents,
            sign = AmountSign.POSITIVE,
            style = MaterialTheme.typography.labelLarge,
            animate = false,
        )
    }
}

/**
 * Carrusel de tarjetas con una tarjeta principal centrada: la que entra
 * al centro "crece" a escala 1f y la que sale se "encoge" a 0.85f, todo
 * interpolado en vivo con el gesto de swipe. HorizontalPager + graphicsLayer.
 */
@Composable
private fun CardsPager(cards: List<CardUi>) {
    val pagerState = rememberPagerState(pageCount = { cards.size })

    HorizontalPager(
        state = pagerState,
        pageSize = PageSize.Fixed(220.dp),
        pageSpacing = FinTheme.spacing.md,
        contentPadding = PaddingValues(horizontal = FinTheme.spacing.lg),
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
    ) { page ->
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val scale = lerp(0.85f, 1f, 1f - min(1f, abs(pageOffset)))
        val alpha = lerp(0.6f, 1f, 1f - min(1f, abs(pageOffset)))

        CardItem(
            card = cards[page],
            modifier = Modifier
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
        )
    }
}

@Composable
private fun CardItem(card: CardUi, modifier: Modifier = Modifier) {
    FinCard(modifier = modifier.width(220.dp)) {
        Column {
            Text(text = card.alias, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${card.bank ?: ""} •••• ${card.last4 ?: "----"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(FinTheme.spacing.md))
            AmountText(
                amountCents = card.balanceCents,
                sign = if (card.isCredit) AmountSign.NEGATIVE else AmountSign.NEUTRAL,
                style = MaterialTheme.typography.titleLarge,
                animate = false,
            )
            Text(
                text = if (card.isCredit) "usado" else "disponible",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (card.isCredit && card.creditLimitCents != null) {
                Spacer(Modifier.height(FinTheme.spacing.sm))
                AnimatedProgressBar(progress = card.usagePercent, color = FinTheme.extendedColors.expense)
                Spacer(Modifier.height(FinTheme.spacing.xs))
                Text(
                    text = "Límite ${formatCents(card.creditLimitCents)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (card.cutOffDay != null && card.paymentDueDay != null) {
                Spacer(Modifier.height(FinTheme.spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(FinTheme.spacing.xs))
                    Text(
                        text = "Corte día ${card.cutOffDay} · Límite día ${card.paymentDueDay}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingPaymentRow(payment: HomeUpcomingUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = payment.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Vence el ${payment.dueDateLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountText(amountCents = payment.amountCents, style = MaterialTheme.typography.titleMedium, animate = false)
    }
}

private val movementDateFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@Composable
private fun MovementRow(movement: HomeMovementUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = movement.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(FinTheme.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = movement.merchant, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            val dateLabel = movementDateFormatter.format(movement.occurredAt.atZone(ZoneId.systemDefault()))
            Text(
                text = "${movement.category} · ${movement.accountLabel} · $dateLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountText(
            amountCents = movement.amountCents,
            sign = if (movement.isExpense) AmountSign.NEGATIVE else AmountSign.POSITIVE,
            style = MaterialTheme.typography.titleMedium,
            animate = false,
        )
    }
}
