package com.example.pamproject.api

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * HTTP client using Volley library for REST API communication
 * Demonstrates thread-based network communication with callbacks
 */
class VolleyClient(context: Context) {

    private val requestQueue: RequestQueue = Volley.newRequestQueue(context.applicationContext)
    private val gson = Gson()

    /**
     * GET all workout logs using Volley
     * Uses callback-based threading for network operations
     */
    suspend fun getAllWorkoutLogs(): Result<List<WorkoutLogDto>> = suspendCancellableCoroutine { continuation ->
        val url = "${ApiConfig.SUPABASE_URL}/rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}?select=*&order=timestamp.desc"

        val request = object : JsonArrayRequest(
            Method.GET,
            url,
            null,
            { response ->
                try {
                    val type = object : TypeToken<List<WorkoutLogDto>>() {}.type
                    val logs: List<WorkoutLogDto> = gson.fromJson(response.toString(), type)
                    continuation.resume(Result.success(logs))
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            },
            { error ->
                continuation.resume(Result.failure(Exception(error.message ?: "Volley error")))
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "apikey" to ApiConfig.SUPABASE_KEY,
                    "Authorization" to "Bearer ${ApiConfig.SUPABASE_KEY}",
                    "Content-Type" to "application/json"
                )
            }
        }

        requestQueue.add(request)

        continuation.invokeOnCancellation {
            request.cancel()
        }
    }

    /**
     * POST a new workout log using Volley
     * Uses callback-based threading for network operations
     */
    suspend fun createWorkoutLog(workoutLog: CreateWorkoutLogRequest): Result<WorkoutLogDto> = suspendCancellableCoroutine { continuation ->
        val url = "${ApiConfig.SUPABASE_URL}/rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}"
        val jsonBody = gson.toJson(workoutLog)

        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    val type = object : TypeToken<List<WorkoutLogDto>>() {}.type
                    val logs: List<WorkoutLogDto> = gson.fromJson(response, type)
                    if (logs.isNotEmpty()) {
                        continuation.resume(Result.success(logs.first()))
                    } else {
                        continuation.resume(Result.failure(Exception("Empty response")))
                    }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            },
            { error ->
                continuation.resume(Result.failure(Exception(error.message ?: "Volley error")))
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "apikey" to ApiConfig.SUPABASE_KEY,
                    "Authorization" to "Bearer ${ApiConfig.SUPABASE_KEY}",
                    "Content-Type" to "application/json",
                    "Prefer" to "return=representation"
                )
            }

            override fun getBody(): ByteArray {
                return jsonBody.toByteArray(Charsets.UTF_8)
            }

            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }
        }

        requestQueue.add(request)

        continuation.invokeOnCancellation {
            request.cancel()
        }
    }

    /**
     * DELETE a workout log by timestamp using Volley
     * Uses callback-based threading for network operations
     */
    suspend fun deleteWorkoutLog(timestamp: Long): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val url = "${ApiConfig.SUPABASE_URL}/rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}?timestamp=eq.$timestamp"

        val request = object : StringRequest(
            Method.DELETE,
            url,
            { _ ->
                continuation.resume(Result.success(Unit))
            },
            { error ->
                continuation.resume(Result.failure(Exception(error.message ?: "Volley error")))
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "apikey" to ApiConfig.SUPABASE_KEY,
                    "Authorization" to "Bearer ${ApiConfig.SUPABASE_KEY}"
                )
            }
        }

        requestQueue.add(request)

        continuation.invokeOnCancellation {
            request.cancel()
        }
    }

    /**
     * DELETE all workout logs using Volley
     * Uses callback-based threading for network operations
     */
    suspend fun deleteAllWorkoutLogs(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val url = "${ApiConfig.SUPABASE_URL}/rest/v1/${ApiConfig.TABLE_WORKOUT_LOGS}?id=neq.null"

        val request = object : StringRequest(
            Method.DELETE,
            url,
            { _ ->
                continuation.resume(Result.success(Unit))
            },
            { error ->
                continuation.resume(Result.failure(Exception(error.message ?: "Volley error")))
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "apikey" to ApiConfig.SUPABASE_KEY,
                    "Authorization" to "Bearer ${ApiConfig.SUPABASE_KEY}"
                )
            }
        }

        requestQueue.add(request)

        continuation.invokeOnCancellation {
            request.cancel()
        }
    }
}

