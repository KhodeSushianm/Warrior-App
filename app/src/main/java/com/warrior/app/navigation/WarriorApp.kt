package com.warrior.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.warrior.app.AppViewModel
import com.warrior.app.BuildConfig
import com.warrior.app.R
import com.warrior.core.designsystem.icons.WarriorIconHistory
import com.warrior.core.designsystem.icons.WarriorIconHome
import com.warrior.core.designsystem.icons.WarriorIconProfile
import com.warrior.core.designsystem.icons.WarriorIconProgress
import com.warrior.core.designsystem.theme.Background
import com.warrior.core.designsystem.theme.Surface
import com.warrior.core.designsystem.theme.SurfaceVariant
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.core.designsystem.theme.TextPrimary
import com.warrior.feature.auth.AuthScreen
import com.warrior.feature.history.HistoryDetailScreen
import com.warrior.feature.history.HistoryScreen
import com.warrior.feature.home.HomeScreen
import com.warrior.feature.profile.BodyScreen
import com.warrior.feature.profile.ProfileScreen
import com.warrior.feature.progress.ProgressScreen
import com.warrior.feature.workout.WorkoutLoggingScreen

private val MAIN_ROUTES = setOf("HomeRoute", "HistoryRoute", "ProgressRoute", "ProfileRoute")

/**
 * Root of the app: the device session (DataStore) decides the entry point
 * (Architecture v2.1 §14.2). Login/register start the session, logout clears
 * it — both swap the tree automatically, no manual navigation needed.
 */
@Composable
fun WarriorApp() {
    val appViewModel: AppViewModel = hiltViewModel()
    val currentUserId by appViewModel.currentUserId.collectAsStateWithLifecycle(initialValue = null)

    if (currentUserId == null) {
        AuthScreen()
    } else {
        MainScreen()
    }
}

@Composable
private fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringAfterLast('.')
    val showBottomBar = currentRoute in MAIN_ROUTES

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
                    NavigationBarItem(
                        selected = currentRoute == "HomeRoute",
                        onClick = { navController.navigateTab(HomeRoute) },
                        icon = { Icon(WarriorIconHome, contentDescription = stringResource(R.string.nav_home)) },
                        label = { Text(stringResource(R.string.nav_home)) },
                        colors = warriorNavItemColors(),
                    )
                    NavigationBarItem(
                        selected = currentRoute == "HistoryRoute",
                        onClick = { navController.navigateTab(HistoryRoute) },
                        icon = { Icon(WarriorIconHistory, contentDescription = stringResource(R.string.nav_history)) },
                        label = { Text(stringResource(R.string.nav_history)) },
                        colors = warriorNavItemColors(),
                    )
                    NavigationBarItem(
                        selected = currentRoute == "ProgressRoute",
                        onClick = { navController.navigateTab(ProgressRoute) },
                        icon = { Icon(WarriorIconProgress, contentDescription = stringResource(R.string.nav_progress)) },
                        label = { Text(stringResource(R.string.nav_progress)) },
                        colors = warriorNavItemColors(),
                    )
                    NavigationBarItem(
                        selected = currentRoute == "ProfileRoute",
                        onClick = { navController.navigateTab(ProfileRoute) },
                        icon = { Icon(WarriorIconProfile, contentDescription = stringResource(R.string.nav_profile)) },
                        label = { Text(stringResource(R.string.nav_profile)) },
                        colors = warriorNavItemColors(),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding),
            // Phase 9 polish: uniform, subtle transitions for every destination.
            enterTransition = {
                fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 }
            },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = {
                fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 12 }
            },
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onStartWorkout = { navController.navigate(WorkoutRoute()) },
                    onOpenSession = { id -> navController.navigate(HistoryDetailRoute(id)) },
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(
                    onOpenSession = { id -> navController.navigate(HistoryDetailRoute(id)) },
                    onStartWorkout = { navController.navigate(WorkoutRoute()) },
                )
            }
            composable<HistoryDetailRoute> { entry ->
                val route = entry.toRoute<HistoryDetailRoute>()
                HistoryDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(WorkoutRoute(id)) },
                )
            }
            composable<ProgressRoute> { ProgressScreen() }
            composable<ProfileRoute> {
                ProfileScreen(
                    version = BuildConfig.VERSION_NAME,
                    onOpenBody = { navController.navigate(BodyRoute) },
                )
            }
            composable<BodyRoute> {
                BodyScreen(onBack = { navController.popBackStack() })
            }
            composable<WorkoutRoute> { entry ->
                val route = entry.toRoute<WorkoutRoute>()
                WorkoutLoggingScreen(
                    sessionId = route.sessionId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun warriorNavItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = TextPrimary,
    selectedTextColor = TextPrimary,
    unselectedIconColor = TextMuted,
    unselectedTextColor = TextMuted,
    indicatorColor = SurfaceVariant,
)

private fun NavHostController.navigateTab(route: Any) {
    navigate(route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
