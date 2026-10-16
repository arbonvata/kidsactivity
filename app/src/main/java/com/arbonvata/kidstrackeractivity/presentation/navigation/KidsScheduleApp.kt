package com.arbonvata.kidstrackeractivity.presentation.navigation

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.presentation.AddActivityScreen
import com.arbonvata.kidstrackeractivity.presentation.AddKidScreen
import com.arbonvata.kidstrackeractivity.presentation.ConflictScreen
import com.arbonvata.kidstrackeractivity.presentation.DailyScheduleScreen
import com.arbonvata.kidstrackeractivity.presentation.EditActivityScreen
import com.arbonvata.kidstrackeractivity.presentation.EditKidScreen
import com.arbonvata.kidstrackeractivity.presentation.KidDetailScreen
import com.arbonvata.kidstrackeractivity.presentation.KidSelectionScreen
import com.arbonvata.kidstrackeractivity.presentation.KidsManagementScreen
import com.arbonvata.kidstrackeractivity.presentation.MonthScreen
import com.arbonvata.kidstrackeractivity.presentation.MoreScreen
import com.arbonvata.kidstrackeractivity.presentation.SchoolOverrideScreen
import com.arbonvata.kidstrackeractivity.presentation.SettingsScreen
import com.arbonvata.kidstrackeractivity.presentation.ShareScreen
import com.arbonvata.kidstrackeractivity.presentation.TodayScreen
import com.arbonvata.kidstrackeractivity.presentation.WeekScreen

// 1. Define all navigation destinations
sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    // Bottom nav items (main tabs)
    object Today : Screen("today", R.string.tab_today, Icons.Default.Today)

    object Week : Screen("week", R.string.tab_week, Icons.Default.DateRange)

    object Month : Screen("month", R.string.tab_month, Icons.Default.CalendarMonth)

    object More : Screen("more", R.string.tab_more, Icons.Default.MoreHoriz)

    // Secondary screens (NOT in bottom nav)
    object Settings : Screen("settings", R.string.screen_title_settings, Icons.Default.Settings)

    object AddActivity : Screen("add_activity?kidId={kidId}", R.string.screen_title_add_activity, Icons.Default.Add)

    object EditActivity : Screen("edit_activity/{activityId}", R.string.screen_title_edit_activity, Icons.Default.Edit)

    object KidSelection : Screen("kid_selection", R.string.screen_title_kid_selection, Icons.Default.People)

    object AddKid : Screen("add_kid", R.string.screen_title_add_kid, Icons.Default.PersonAdd)

    object DailySchedule : Screen("daily_schedule/{kidIds}", R.string.screen_title_daily_schedule, Icons.Default.ViewDay)

    object KidDetail : Screen("kid_detail/{kidId}", R.string.screen_title_kid_detail, Icons.Default.ViewDay)

    object SchoolOverrides : Screen("school_overrides", R.string.screen_title_school_overrides, Icons.Default.Event)

    object Conflicts : Screen("conflicts", R.string.screen_title_conflicts, Icons.Default.Warning)

    object Share : Screen("share", R.string.screen_title_share, Icons.Default.Share)

    object KidsManagement : Screen("kids_management", R.string.screen_title_kids_management, Icons.Default.Person)
}

// List of bottom nav items only
val bottomNavItems = listOf(Screen.Today, Screen.Week, Screen.Month, Screen.More)

@Composable
fun KidsScheduleApp(deepLinkIntent: Intent? = null) {
    val navController = rememberNavController()

    // Notification deep links (kidstracker://edit_activity/{id}) arrive via
    // the Activity's intent, both on cold start and through onNewIntent.
    LaunchedEffect(deepLinkIntent) {
        deepLinkIntent?.let(navController::handleDeepLink)
    }

    // Track current back stack entry to determine selected tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Check if we should show the bottom bar (hide it on secondary screens)
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    /**
     * The single way to navigate to a bottom-bar tab. Every tab navigation
     * must use the same options (also the in-content ones like "View Full
     * Week"), otherwise the save/restore bookkeeping of the back stack
     * becomes asymmetric and tab clicks can silently stop navigating.
     */
    fun navigateToTab(screen: Screen) {
        navController.navigate(screen.route) {
            // Pop up to the start destination to avoid building a massive stack
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination
            launchSingleTop = true
            // Restore state when re-selecting already selected item
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                            label = { Text(stringResource(screen.titleRes)) },
                            selected = selected,
                            onClick = { navigateToTab(screen) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            // --- MAIN TABS ---
            composable(Screen.Today.route) {
                TodayScreen(
                    onAddActivityClick = { navController.navigate("add_activity") },
                    onViewWeekClick = { navigateToTab(Screen.Week) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onAddKidClick = { navController.navigate(Screen.AddKid.route) },
                    onActivityClick = { activity ->
                        navController.navigate("edit_activity/${activity.id}")
                    },
                )
            }

            composable(Screen.Week.route) {
                WeekScreen(
                    onEditActivityClick = { activity ->
                        navController.navigate("edit_activity/${activity.id}")
                    },
                )
            }

            composable(Screen.Month.route) {
                MonthScreen(
                    onEditActivityClick = { activity ->
                        navController.navigate("edit_activity/${activity.id}")
                    },
                )
            }

            composable(Screen.More.route) {
                MoreScreen(
                    onKidsClick = { navController.navigate(Screen.KidsManagement.route) },
                    onSchoolOverridesClick = { navController.navigate(Screen.SchoolOverrides.route) },
                    onConflictsClick = { navController.navigate(Screen.Conflicts.route) },
                    onShareClick = { navController.navigate(Screen.Share.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onKidSelectionClick = { navController.navigate(Screen.KidSelection.route) },
                )
            }

            // --- SECONDARY SCREENS ---
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackupClick = { navController.navigate(Screen.Share.route) },
                    onRestoreClick = { navController.navigate(Screen.Share.route) },
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.AddActivity.route,
                arguments =
                    listOf(
                        navArgument("kidId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                    ),
            ) { backStackEntry ->
                val kidId = backStackEntry.arguments?.getLong("kidId") ?: 0L
                AddActivityScreen(
                    kidId = kidId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "edit_activity/{activityId}",
                arguments = listOf(navArgument("activityId") { type = NavType.LongType }),
                deepLinks =
                    listOf(
                        navDeepLink { uriPattern = "kidstracker://edit_activity/{activityId}" },
                    ),
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getLong("activityId") ?: return@composable
                EditActivityScreen(
                    activityId = activityId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.KidSelection.route) {
                KidSelectionScreen(
                    onAddKidClick = { navController.navigate(Screen.AddKid.route) },
                    onContinueClick = { selectedIds ->
                        val idsString = selectedIds.joinToString(",")
                        navController.navigate("daily_schedule/$idsString")
                    },
                )
            }

            composable(Screen.AddKid.route) {
                AddKidScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = "daily_schedule/{kidIds}",
                arguments = listOf(navArgument("kidIds") { type = NavType.StringType }),
            ) { backStackEntry ->
                val idsString = backStackEntry.arguments?.getString("kidIds") ?: ""
                val selectedIds =
                    idsString
                        .split(",")
                        .filter { it.isNotBlank() }
                        .map { it.toLong() }
                        .toSet()

                DailyScheduleScreen(
                    selectedKidIds = selectedIds,
                    onEditActivityClick = { activity ->
                        navController.navigate("edit_activity/${activity.id}")
                    },
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(Screen.SchoolOverrides.route) {
                SchoolOverrideScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Screen.Conflicts.route) {
                ConflictScreen(
                    // Resolve by editing the earlier of the two activities;
                    // shifting its times there clears the overlap.
                    onResolveClick = { conflict ->
                        navController.navigate("edit_activity/${conflict.activity1.activity.id}")
                    },
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(Screen.Share.route) {
                ShareScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Screen.KidsManagement.route) {
                KidsManagementScreen(
                    onAddKidClick = { navController.navigate(Screen.AddKid.route) },
                    onKidClick = { kid -> navController.navigate("kid_detail/${kid.id}") },
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.KidDetail.route,
                arguments = listOf(navArgument("kidId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val kidId = backStackEntry.arguments?.getLong("kidId") ?: return@composable
                KidDetailScreen(
                    kidId = kidId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditKidClick = { navController.navigate("edit_kid/$kidId") },
                    onActivityClick = { activityId ->
                        navController.navigate("edit_activity/$activityId")
                    },
                    onAddActivityClick = { navController.navigate("add_activity?kidId=$kidId") },
                )
            }

            composable(
                route = "edit_kid/{kidId}",
                arguments = listOf(navArgument("kidId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val kidId = backStackEntry.arguments?.getLong("kidId") ?: return@composable
                EditKidScreen(
                    kidId = kidId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
