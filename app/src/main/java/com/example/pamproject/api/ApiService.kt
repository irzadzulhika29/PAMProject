package com.example.pamproject.api

import com.example.pamproject.model.WorkoutLog
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("workout_logs")
    suspend fun getWorkoutLogs(
        @Header("Prefer") prefer: String = "return=representation"
    ): List<WorkoutLog>

    @POST("workout_logs")
    suspend fun insertWorkoutLog(
        @Body workoutLog: WorkoutLog,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<WorkoutLog>

    @DELETE("workout_logs")
    suspend fun deleteWorkoutLog(
        @Query("timestamp") timestampFilter: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<Unit>
}
