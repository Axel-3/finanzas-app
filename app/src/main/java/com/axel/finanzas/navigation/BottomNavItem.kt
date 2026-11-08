package com.axel.finanzas.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.ui.graphics.vector.ImageVector
import com.axel.finanzas.R

/** Las 5 secciones de la navegación inferior. Un solo set de iconos de línea. */
enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
) {
    Home(route = "home", icon = Icons.Outlined.Home, labelRes = R.string.nav_home),
    Movements(route = "movements", icon = Icons.AutoMirrored.Outlined.ReceiptLong, labelRes = R.string.nav_movements),
    Cards(route = "cards", icon = Icons.Outlined.CreditCard, labelRes = R.string.nav_cards),
    Goals(route = "goals", icon = Icons.Outlined.Savings, labelRes = R.string.nav_goals),
    More(route = "more", icon = Icons.Outlined.MoreHoriz, labelRes = R.string.nav_more),
}
