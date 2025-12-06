package com.example.pamproject.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pamproject.ui.screen.DashboardScreen
import com.example.pamproject.ui.screen.WorkoutSessionScreen
import com.example.pamproject.viewmodel.WorkoutViewModel
import androidx.compose.runtime.LaunchedEffect

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    workoutViewModel: WorkoutViewModel
) {
    val stats by workoutViewModel.todayStats.collectAsState()
    val weeklyProgress by workoutViewModel.weeklyProgress.collectAsState()
    val logs by workoutViewModel.logs.collectAsState()
    val timerState by workoutViewModel.timerState.collectAsState()
    val isLoading by workoutViewModel.isLoading.collectAsState()
    val currentApiClient by workoutViewModel.currentApiClient.collectAsState()

    LaunchedEffect(Unit) {
        workoutViewModel.loadLogsFromApi()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                stats = stats,
                progress = weeklyProgress,
                workouts = workoutViewModel.workouts,
                logs = logs,
                isLoading = isLoading,
                currentApiClient = currentApiClient,
                onApiClientChange = workoutViewModel::switchApiClient,
                onRefresh = workoutViewModel::loadLogsFromApi,
                onWorkoutClick = { workoutId ->
                    workoutViewModel.startSession(workoutId)
                    navController.navigate(Screen.Session.createRoute(workoutId))
                },
                onDeleteLog = workoutViewModel::deleteLog,
                onDeleteAllLogs = workoutViewModel::clearLogs
            )
        }

        composable(
            route = Screen.Session.route,
            arguments = listOf(
                navArgument("workoutId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getInt("workoutId")
            val workout = workoutId?.let { workoutViewModel.getWorkoutById(it) }

            WorkoutSessionScreen(
                workout = workout,
                timerState = timerState,
                onStart = workoutViewModel::startTimer,
                onPause = workoutViewModel::pauseTimer,
                onResume = workoutViewModel::resumeTimer,
                onFinish = { uri ->
                    val result = workoutViewModel.finishWorkout(uri)
                    result
                },
                onBack = {
                    workoutViewModel.cancelSession()
                    navController.navigateUp()
                }
            )
        }
    }
}
