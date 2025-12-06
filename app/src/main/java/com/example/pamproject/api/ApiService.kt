package com.example.pamproject.api

import retrofit2.Response
import retrofit2.http.*

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
        @Header("apikey") apiKey: String = ApiConfig.SUPABASE_KEY,
        @Header("Authorization") authorization: String = "Bearer ${ApiConfig.SUPABASE_KEY}",
        @Query("select") select: String = "*",
        @Query("order") order: String = "timestamp.desc"
    ): Response<List<WorkoutLogDto>>

    /**
     * POST a new workout log to Supabase
     * Uses thread/coroutine for async communication
     */
    @POST("rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}")
    suspend fun createWorkoutLog(
        @Header("apikey") apiKey: String = ApiConfig.SUPABASE_KEY,
        @Header("Authorization") authorization: String = "Bearer ${ApiConfig.SUPABASE_KEY}",
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Body workoutLog: CreateWorkoutLogRequest
    ): Response<List<WorkoutLogDto>>

    /**
     * DELETE a workout log from Supabase by timestamp
     * Uses thread/coroutine for async communication
     */
    @DELETE("rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}")
    suspend fun deleteWorkoutLog(
        @Header("apikey") apiKey: String = ApiConfig.SUPABASE_KEY,
        @Header("Authorization") authorization: String = "Bearer ${ApiConfig.SUPABASE_KEY}",
        @Query("timestamp") timestamp: String
    ): Response<Unit>

    /**
     * DELETE all workout logs from Supabase
     * Uses thread/coroutine for async communication
     */
    @DELETE("rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}")
    suspend fun deleteAllWorkoutLogs(
        @Header("apikey") apiKey: String = ApiConfig.SUPABASE_KEY,
        @Header("Authorization") authorization: String = "Bearer ${ApiConfig.SUPABASE_KEY}",
        @Query("id") idFilter: String = "neq.null"  // This deletes all rows where id is not null
    ): Response<Unit>
}

