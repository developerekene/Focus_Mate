package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.FocusSessionEntity
import com.example.model.ProductivitySummary
import com.example.model.TimerSettings
import com.example.model.TimerState
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.viewmodel.FocusViewModel

const val ROUTE_TIMER = "timer"
const val ROUTE_STATS = "stats"
const val ROUTE_SETTINGS = "settings"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val DESTINATIONS = listOf(
    NavDestination(
        route = ROUTE_TIMER,
        labelRes = R.string.nav_timer,
        selectedIcon = Icons.Rounded.Timer,
        unselectedIcon = Icons.Outlined.Timer,
        testTag = "nav_timer"
    ),
    NavDestination(
        route = ROUTE_STATS,
        labelRes = R.string.nav_stats,
        selectedIcon = Icons.Rounded.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        testTag = "nav_stats"
    ),
    NavDestination(
        route = ROUTE_SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)

@Composable
fun FocusMateAppScaffold(
    timerState: TimerState,
    settings: TimerSettings,
    summary: ProductivitySummary,
    sessionHistory: List<FocusSessionEntity>,
    viewModel: FocusViewModel,
    onStartOrPauseRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TIMER

    val hideNavChrome = currentRoute == ROUTE_TIMER && timerState.distractionFreeMode

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavigationRail) {
                    AnimatedVisibility(
                        visible = !hideNavChrome,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            DESTINATIONS.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && !hideNavChrome) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DESTINATIONS.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_TIMER,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_TIMER) {
                        TimerScreen(
                            timerState = timerState,
                            settings = settings,
                            summary = summary,
                            onStartOrPause = onStartOrPauseRequested,
                            onReset = viewModel::resetTimer,
                            onSkip = viewModel::skipPhase,
                            onSelectMode = viewModel::selectSessionType,
                            onApplyCustomDuration = viewModel::applyCustomDuration,
                            onUpdateTag = viewModel::updateSessionTag,
                            onToggleZenMode = viewModel::toggleDistractionFreeMode,
                            onExitZenMode = { viewModel.setDistractionFreeMode(false) }
                        )
                    }

                    composable(ROUTE_STATS) {
                        StatsScreen(
                            summary = summary,
                            settings = settings,
                            sessions = sessionHistory,
                            onDeleteSession = viewModel::deleteSession,
                            onClearAllHistory = viewModel::clearAllHistory
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            onFocusMinutesChange = viewModel::updateFocusMinutes,
                            onShortBreakMinutesChange = viewModel::updateShortBreakMinutes,
                            onLongBreakMinutesChange = viewModel::updateLongBreakMinutes,
                            onSessionsUntilLongBreakChange = viewModel::updateSessionsUntilLongBreak,
                            onDailyGoalChange = viewModel::updateDailyGoalSessions,
                            onAutoStartBreaksChange = viewModel::updateAutoStartBreaks,
                            onAutoStartFocusChange = viewModel::updateAutoStartFocus,
                            onKeepScreenOnChange = viewModel::updateKeepScreenOn,
                            onSoundEnabledChange = viewModel::updateSoundEnabled,
                            onVibrationEnabledChange = viewModel::updateVibrationEnabled,
                            onThemeModeChange = viewModel::updateThemeMode,
                            onTestAlert = viewModel::testAlertFeedback
                        )
                    }
                }
            }
        }
    }
}
