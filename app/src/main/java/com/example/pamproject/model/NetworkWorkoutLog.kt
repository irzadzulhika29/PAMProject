package com.example.pamproject.model

import com.google.gson.annotations.SerializedName

/**
 * DTO yang mencerminkan skema tabel Supabase. Menggunakan snake_case
 * agar sesuai dengan kolom yang diekspos oleh RESTful endpoint Supabase.
 */
data class NetworkWorkoutLog(
    @SerializedName("id") val id: String? = null,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String,
    @SerializedName("workout") val workout: String,
    @SerializedName("duration_minutes") val durationMinutes: Double,
    @SerializedName("calories") val calories: Double,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("image_uri") val imageUri: String? = null
) {
    fun toDomain(): WorkoutLog = WorkoutLog(
        date = date,
        time = time,
        workout = workout,
        durationMinutes = durationMinutes,
        calories = calories,
        timestamp = timestamp,
        imageUri = imageUri
    )

    companion object {
        fun fromDomain(log: WorkoutLog): NetworkWorkoutLog = NetworkWorkoutLog(
            date = log.date,
            time = log.time,
            workout = log.workout,
            durationMinutes = log.durationMinutes,
            calories = log.calories,
            timestamp = log.timestamp,
            imageUri = log.imageUri
        )
    }
}
