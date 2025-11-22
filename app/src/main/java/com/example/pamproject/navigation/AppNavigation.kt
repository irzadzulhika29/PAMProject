package com.example.pamproject.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pamproject.data.WorkoutRepository
import com.example.pamproject.ui.screen.DashboardScreen
import com.example.pamproject.ui.screen.WorkoutSessionScreen
import com.example.pamproject.viewmodel.WorkoutViewModel
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { WorkoutRepository(context) }
    val workoutViewModel: WorkoutViewModel = viewModel(
        factory = WorkoutViewModel.provideFactory(repository)
    )

    val stats by workoutViewModel.todayStats.collectAsState()
    val logs by workoutViewModel.logs.collectAsState()
    val timerState by workoutViewModel.timerState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                stats = stats,
                workouts = workoutViewModel.workouts,
                latestLog = logs.lastOrNull(),
                onWorkoutClick = { workoutId ->
                    workoutViewModel.startSession(workoutId)
                    navController.navigate(Screen.Session.createRoute(workoutId))
                }
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
                onFinish = workoutViewModel::finishWorkout,
                onBack = {
                    workoutViewModel.cancelSession()
                    navController.navigateUp()
                }
            )
        }
    }
}
