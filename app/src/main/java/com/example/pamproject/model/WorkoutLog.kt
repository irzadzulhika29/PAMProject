package com.example.pamproject.model

import com.google.gson.annotations.SerializedName

data class WorkoutLog(
    val date: String,
    val time: String,
    val workout: String,
    @SerializedName("duration_minutes") val durationMinutes: Double,
    val calories: Double,
    val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("image_uri") val imageUri: String? = null
)

data class DailyStats(
    val totalDurationMinutes: Double,
    val totalCalories: Double,
    val streak: Int
)
