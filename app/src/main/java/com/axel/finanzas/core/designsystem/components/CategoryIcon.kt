package com.axel.finanzas.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Las categorías (y las cuentas) guardan un `icon: String` en Room, no un
 * [ImageVector] — Room no sabe serializar eso. Este mapeo central
 * traduce la clave guardada al ícono real. Si agregas una clave nueva en
 * [com.axel.finanzas.core.database.DefaultDataSeeder] o en el formulario
 * de alta, regístrala aquí también.
 */
fun iconForKey(key: String): ImageVector = when (key) {
    "shopping_bag" -> Icons.Outlined.ShoppingBag
    "dining" -> Icons.Outlined.LocalDining
    "car" -> Icons.Outlined.DirectionsCar
    "bolt" -> Icons.Outlined.Bolt
    "games" -> Icons.Outlined.SportsEsports
    "health" -> Icons.Outlined.Favorite
    "home" -> Icons.Outlined.House
    "shirt" -> Icons.Outlined.Checkroom
    "payroll" -> Icons.AutoMirrored.Outlined.TrendingUp
    "sales" -> Icons.Outlined.AttachMoney
    "gift" -> Icons.Outlined.Redeem
    "cash" -> Icons.Outlined.Wallet
    "card" -> Icons.Outlined.Payments
    "savings" -> Icons.Outlined.Savings
    else -> Icons.Outlined.Category
}
