package com.example.pamproject.model

data class Workout(
    val id: Int,
    val name: String,
    val met: Double
)

object WorkoutData {
    val workouts = listOf(
        Workout(id = 1, name = "Yoga", met = 3.0),
        Workout(id = 2, name = "Running", met = 8.0),
        Workout(id = 3, name = "Stretching", met = 2.5),
        Workout(id = 4, name = "HIIT", met = 10.0),
        Workout(id = 5, name = "Cycling", met = 6.0),
        Workout(id = 6, name = "Walking", met = 3.5)
    )
}

