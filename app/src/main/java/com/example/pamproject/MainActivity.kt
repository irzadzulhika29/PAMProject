package com.example.pamproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.pamproject.data.WorkoutRepository
import com.example.pamproject.navigation.AppNavigation
import com.example.pamproject.ui.theme.PAMProjectTheme
import com.example.pamproject.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {

    private val workoutRepository: WorkoutRepository by lazy { WorkoutRepository(this) }
    private val workoutViewModel: WorkoutViewModel by viewModels {
        WorkoutViewModel.provideFactory(workoutRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PAMProjectTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                    workoutViewModel = workoutViewModel
                )
            }
        }
    }
}
