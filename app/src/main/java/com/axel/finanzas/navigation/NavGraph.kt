package com.axel.finanzas.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.axel.finanzas.core.backup.BackupScreen
import com.axel.finanzas.core.security.SecurityScreen
import com.axel.finanzas.feature.budgets.BudgetsScreen
import com.axel.finanzas.feature.calendar.CalendarScreen
import com.axel.finanzas.feature.cards.CardsScreen
import com.axel.finanzas.feature.futurepayments.FuturePaymentsScreen
import com.axel.finanzas.feature.goals.GoalsScreen
import com.axel.finanzas.feature.home.HomeScreen
import com.axel.finanzas.feature.inbox.NotificationInboxScreen
import com.axel.finanzas.feature.movements.MovementsScreen
import com.axel.finanzas.feature.recurring.RecurringScreen
import com.axel.finanzas.feature.reports.ReportsScreen
import com.axel.finanzas.feature.settings.NotificationRulesScreen
import com.axel.finanzas.feature.settings.SettingsScreen

private object Routes {
    const val NOTIFICATION_INBOX = "notification_inbox"
    const val NOTIFICATION_RULES = "notification_rules"
    const val RECURRING = "recurring_payments"
    const val FUTURE_PAYMENTS = "future_payments"
    const val CALENDAR = "calendar"
    const val BUDGETS = "budgets"
    const val REPORTS = "reports"
    const val SECURITY = "security"
    const val BACKUP = "backup"
}

/** Fade + slide sutil (200-350ms) para las pantallas secundarias que se "empujan" sobre la actual. */
private const val TRANSITION_MS = 280

@Composable
fun FinanzasNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { FinanzasBottomBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(tween(TRANSITION_MS)) + slideInHorizontally(tween(TRANSITION_MS)) { it / 6 }
            },
            exitTransition = { fadeOut(tween(TRANSITION_MS)) },
            popEnterTransition = { fadeIn(tween(TRANSITION_MS)) },
            popExitTransition = {
                fadeOut(tween(TRANSITION_MS)) + slideOutHorizontally(tween(TRANSITION_MS)) { it / 6 }
            },
        ) {
            composable(BottomNavItem.Home.route) { HomeScreen() }
            composable(BottomNavItem.Movements.route) { MovementsScreen() }
            composable(BottomNavItem.Cards.route) {
                CardsScreen(onNavigateToInbox = { navController.navigate(Routes.NOTIFICATION_INBOX) })
            }
            composable(BottomNavItem.Goals.route) { GoalsScreen() }
            composable(BottomNavItem.More.route) {
                SettingsScreen(
                    onNavigateToNotificationRules = { navController.navigate(Routes.NOTIFICATION_RULES) },
                    onNavigateToRecurring = { navController.navigate(Routes.RECURRING) },
                    onNavigateToFuturePayments = { navController.navigate(Routes.FUTURE_PAYMENTS) },
                    onNavigateToCalendar = { navController.navigate(Routes.CALENDAR) },
                    onNavigateToBudgets = { navController.navigate(Routes.BUDGETS) },
                    onNavigateToReports = { navController.navigate(Routes.REPORTS) },
                    onNavigateToSecurity = { navController.navigate(Routes.SECURITY) },
                    onNavigateToBackup = { navController.navigate(Routes.BACKUP) },
                )
            }

            composable(Routes.NOTIFICATION_INBOX) {
                NotificationInboxScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.NOTIFICATION_RULES) {
                NotificationRulesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.RECURRING) {
                RecurringScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FUTURE_PAYMENTS) {
                FuturePaymentsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BUDGETS) {
                BudgetsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.REPORTS) {
                ReportsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SECURITY) {
                SecurityScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BACKUP) {
                BackupScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun FinanzasBottomBar(navController: androidx.navigation.NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        BottomNavItem.entries.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                label = { Text(text = stringResource(item.labelRes)) },
            )
        }
    }
}
