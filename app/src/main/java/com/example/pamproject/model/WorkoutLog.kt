package com.example.pamproject.model

data class WorkoutLog(
    val date: String,
    val workout: String,
    val durationMinutes: Double,
    val calories: Double
)

data class DailyStats(
    val totalDurationMinutes: Double,
    val totalCalories: Double,
    val streak: Int
)
