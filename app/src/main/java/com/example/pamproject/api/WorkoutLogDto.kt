package com.example.pamproject.api

import com.google.gson.annotations.SerializedName

/**
 * Data class representing WorkoutLog for API communication with Supabase
 * This matches the database schema in Supabase
 */
data class WorkoutLogDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("date")
    val date: String,

    @SerializedName("time")
    val time: String,

    @SerializedName("workout")
    val workout: String,

    @SerializedName("duration_minutes")
    val durationMinutes: Double,

    @SerializedName("calories")
    val calories: Double,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("image_uri")
    val imageUri: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

/**
 * Request body for creating a new workout log (excludes id and created_at)
 */
data class CreateWorkoutLogRequest(
    @SerializedName("date")
    val date: String,

    @SerializedName("time")
    val time: String,

    @SerializedName("workout")
    val workout: String,

    @SerializedName("duration_minutes")
    val durationMinutes: Double,

    @SerializedName("calories")
    val calories: Double,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("image_uri")
    val imageUri: String? = null
)

