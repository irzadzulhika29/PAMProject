package com.example.pamproject.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Headers

/**
 * Retrofit API Service interface for Supabase REST API
 * Uses Representational State Transfer (REST) architecture
 */
interface ApiService {

    /**
     * GET all workout logs from Supabase
     * Uses thread/coroutine for async communication
     */
    @GET("rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}")
    suspend fun getAllWorkoutLogs(
        @Query("select") select: String = "*",
        @Query("order") order: String = "timestamp.desc"
    ): Response<List<WorkoutLogDto>>

    /**
     * POST a new workout log to Supabase
     * Uses thread/coroutine for async communication
     */
    @Headers("Prefer: return=representation")
    @POST("rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}")
    suspend fun createWorkoutLog(
        @Body workoutLog: CreateWorkoutLogRequest
    ): Response<List<WorkoutLogDto>>

    /**
     * DELETE a workout log from Supabase by timestamp
     * Uses thread/coroutine for async communication
     */
    @DELETE("rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}")
    suspend fun deleteWorkoutLog(
        @Query("timestamp") timestampFilter: String
    ): Response<Unit>

    /**
     * DELETE all workout logs from Supabase
     * Uses thread/coroutine for async communication
     */
    @DELETE("rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}")
    suspend fun deleteAllWorkoutLogs(
        @Query("id") idFilter: String = "neq.null"  // This deletes all rows where id is not null
    ): Response<Unit>
}

