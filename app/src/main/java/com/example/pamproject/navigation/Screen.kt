package com.example.pamproject.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Session : Screen("session/{workoutId}") {
        fun createRoute(workoutId: Int) = "session/$workoutId"
    }
}
