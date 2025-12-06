package com.example.pamproject.api

import com.example.pamproject.model.NetworkWorkoutLog
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("rest/v1/workout_logs")
    suspend fun getWorkoutLogs(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("select") select: String = "*"
    ): List<NetworkWorkoutLog>

    @POST("rest/v1/workout_logs")
    suspend fun postWorkoutLog(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body log: NetworkWorkoutLog
    ): List<NetworkWorkoutLog>
}
