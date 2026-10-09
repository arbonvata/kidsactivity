package com.arbonvata.kidstrackeractivity.presentation.navigation

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.presentation.AddActivityScreen
import com.arbonvata.kidstrackeractivity.presentation.AddKidScreen
import com.arbonvata.kidstrackeractivity.presentation.DailyScheduleScreen
import com.arbonvata.kidstrackeractivity.presentation.DashboardScreen
import com.arbonvata.kidstrackeractivity.presentation.KidSelectionScreen
import com.arbonvata.kidstrackeractivity.presentation.KidsManagementScreen
import com.arbonvata.kidstrackeractivity.presentation.MonthlyAgendaScreen
import com.arbonvata.kidstrackeractivity.presentation.MoreScreen
import com.arbonvata.kidstrackeractivity.presentation.SettingsScreen
import com.arbonvata.kidstrackeractivity.presentation.SettingsState
import com.arbonvata.kidstrackeractivity.presentation.WeeklyScheduleScreen
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.KidsViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.ScheduleViewModel
import org.koin.androidx.compose.koinViewModel

// 1. Define all navigation destinations
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    // Bottom nav items (main tabs)
    object Today : Screen("today", "Today", Icons.Default.Today)

    object Week : Screen("week", "Week", Icons.Default.DateRange)

    object Month : Screen("month", "Month", Icons.Default.CalendarMonth)

    object More : Screen("more", "More", Icons.Default.MoreHoriz)

    // Secondary screens (NOT in bottom nav)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    object AddActivity : Screen("add_activity", "Add Activity", Icons.Default.Add)

    object EditActivity : Screen("edit_activity/{activityId}", "Edit Activity", Icons.Default.Edit)

    object KidSelection : Screen("kid_selection", "Select Kids", Icons.Default.People)

    object AddKid : Screen("add_kid", "Add Kid", Icons.Default.PersonAdd)

    object DailySchedule : Screen("daily_schedule/{kidIds}", "Daily Schedule", Icons.Default.ViewDay)

    object SchoolOverrides : Screen("school_overrides", "School Overrides", Icons.Default.Event)

    object Conflicts : Screen("conflicts", "Conflicts", Icons.Default.Warning)

    object Share : Screen("share", "Share", Icons.Default.Share)

    object KidsManagement : Screen("kids_management", "Kids", Icons.Default.Person)
}

// List of bottom nav items only
val bottomNavItems = listOf(Screen.Today, Screen.Week, Screen.Month, Screen.More)

@Composable
fun KidsScheduleApp(viewModel: ScheduleViewModel) {
    val navController = rememberNavController()

    // Track current back stack entry to determine selected tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Check if we should show the bottom bar (hide it on secondary screens)
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
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
                            },
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
                // val kids by viewModel.kidsFlow.collectAsState(initial = emptyList())
                // val activities by viewModel.activitiesFlow.collectAsState(initial = emptyList())
                val todayOccurrences =
                    expandActivitiesForDateRange(
                        emptyList(),
                        emptyList(),
                        java.time.LocalDate.now(),
                        java.time.LocalDate.now(),
                    )

                DashboardScreen(
                    kids = emptyList(),
                    todayOccurrences = todayOccurrences,
                    onAddActivityClick = { navController.navigate(Screen.AddActivity.route) },
                    onViewWeekClick = { navController.navigate(Screen.Week.route) },
                    onActivityClick = { activity ->
                        navController.navigate("edit_activity/${activity.id}")
                    },
                    // onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Week.route) {
                // val kids by viewModel.kidsFlow.collectAsState(initial = emptyList())
                // val activities by viewModel.activitiesFlow.collectAsState(initial = emptyList())

                WeeklyScheduleScreen(
                    kids = emptyList(),
                    activities = emptyList(),
                    onEditActivityClick = { activity ->
                        navController.navigate("edit_activity/${activity.id}")
                    },
                )
            }

            composable(Screen.Month.route) {
                // val kids by viewModel.kidsFlow.collectAsState(initial = emptyList())
                // val activities by viewModel.activitiesFlow.collectAsState(initial = emptyList())

                MonthlyAgendaScreen(
                    kids = emptyList(),
                    activities = emptyList(),
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
                    state = SettingsState(), // Get from ViewModel in real app
                    onSchoolAlarmChange = { /* handle */ },
                    onActivityAlarmChange = { /* handle */ },
                    onRequestExactAlarm = { /* handle */ },
                    onRequestNotifications = { /* handle */ },
                    onDisableBatteryOpt = { /* handle */ },
                    onBackupClick = { /* handle */ },
                    onRestoreClick = { /* handle */ },
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(Screen.AddActivity.route) {
                AddActivityScreen(
                    kidId = 0, // Pass selected kid or 0 for new
                    onSaveClick = { name, startTime, endTime, days, isPeriodic ->
                        // viewModel.saveActivity(name, startTime, endTime, days, isPeriodic)
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = "edit_activity/{activityId}",
                arguments = listOf(navArgument("activityId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getLong("activityId") ?: return@composable
                // Load activity from ViewModel and show edit screen
                // EditActivityScreen(...)
            }

            composable(Screen.KidSelection.route) {
                val kidsViewModel: KidsViewModel = koinViewModel()
                // Re-run on every entry so the list reflects kids added on
                // the Add Kid screen.
                LaunchedEffect(Unit) { kidsViewModel.refresh() }
                val kids by kidsViewModel.kids.collectAsState()

                KidSelectionScreen(
                    kids = kids,
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
                // val kids by viewModel.kidsFlow.collectAsState(initial = emptyList())
                // val activities by viewModel.activitiesFlow.collectAsState(initial = emptyList())

                DailyScheduleScreen(
                    kids = emptyList(),
                    activities = emptyList(),
                    selectedKidIds = selectedIds,
                    onEditActivityClick = { activity ->
                        navController.navigate("edit_activity/${activity.id}")
                    },
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(Screen.SchoolOverrides.route) {
                // SchoolOverrideScreen(...)
            }

            composable(Screen.Conflicts.route) {
                // ConflictScreen(...)
            }

            composable(Screen.Share.route) {
                // ShareScreen(...)
            }

            composable(Screen.KidsManagement.route) {
                KidsManagementScreen(
                    onAddKidClick = { navController.navigate(Screen.AddKid.route) },
                    onBackClick = { navController.popBackStack() },
                )
            }
        }
    }
}
