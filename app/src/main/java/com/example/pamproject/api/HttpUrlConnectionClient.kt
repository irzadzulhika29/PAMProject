package com.example.pamproject.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTP client using HttpURLConnection for REST API communication
 * Demonstrates thread-based network communication using coroutines
 */
class HttpUrlConnectionClient {

    private val gson = Gson()

    /**
     * GET all workout logs using HttpURLConnection
     * Runs on IO thread/dispatcher for network operations
     */
    suspend fun getAllWorkoutLogs(): Result<List<WorkoutLogDto>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiConfig.SUPABASE_URL}/rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}?select=*&order=timestamp.desc")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                setRequestProperty("apikey", ApiConfig.SUPABASE_KEY)
                setRequestProperty("Authorization", "Bearer ${ApiConfig.SUPABASE_KEY}")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val type = object : TypeToken<List<WorkoutLogDto>>() {}.type
                val logs: List<WorkoutLogDto> = gson.fromJson(response, type)
                Result.success(logs)
            } else {
                Result.failure(Exception("HTTP Error: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * POST a new workout log using HttpURLConnection
     * Runs on IO thread/dispatcher for network operations
     */
    suspend fun createWorkoutLog(request: CreateWorkoutLogRequest): Result<WorkoutLogDto> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiConfig.SUPABASE_URL}/rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("apikey", ApiConfig.SUPABASE_KEY)
                setRequestProperty("Authorization", "Bearer ${ApiConfig.SUPABASE_KEY}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val jsonBody = gson.toJson(request)
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody)
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val type = object : TypeToken<List<WorkoutLogDto>>() {}.type
                val logs: List<WorkoutLogDto> = gson.fromJson(response, type)
                if (logs.isNotEmpty()) {
                    Result.success(logs.first())
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val errorReader = BufferedReader(InputStreamReader(connection.errorStream))
                val errorResponse = errorReader.readText()
                errorReader.close()
                Result.failure(Exception("HTTP Error: $responseCode - $errorResponse"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * DELETE a workout log by timestamp using HttpURLConnection
     * Runs on IO thread/dispatcher for network operations
     */
    suspend fun deleteWorkoutLog(timestamp: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiConfig.SUPABASE_URL}/rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}?timestamp=eq.$timestamp")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", ApiConfig.SUPABASE_KEY)
                setRequestProperty("Authorization", "Bearer ${ApiConfig.SUPABASE_KEY}")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP Error: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * DELETE all workout logs using HttpURLConnection
     * Runs on IO thread/dispatcher for network operations
     */
    suspend fun deleteAllWorkoutLogs(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiConfig.SUPABASE_URL}/rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}?id=neq.null")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", ApiConfig.SUPABASE_KEY)
                setRequestProperty("Authorization", "Bearer ${ApiConfig.SUPABASE_KEY}")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP Error: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

