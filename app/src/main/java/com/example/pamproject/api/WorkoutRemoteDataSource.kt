package com.example.pamproject.api

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.pamproject.model.WorkoutLog
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WorkoutRemoteDataSource(
    context: Context,
    private val gson: Gson = Gson()
) {
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context.applicationContext)

    suspend fun fetchWithRetrofit(): Result<List<WorkoutLog>> = withContext(Dispatchers.IO) {
        runCatching { RetrofitClient.apiService.getWorkoutLogs() }
    }

    suspend fun pushWithRetrofit(log: WorkoutLog): Result<List<WorkoutLog>> = withContext(Dispatchers.IO) {
        runCatching { RetrofitClient.apiService.insertWorkoutLog(log) }
    }

    suspend fun deleteWithRetrofit(timestamp: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            RetrofitClient.apiService.deleteWorkoutLog("eq.$timestamp")
            Unit
        }
    }

    fun pushWithHttpUrlConnection(log: WorkoutLog, callback: (Result<Unit>) -> Unit) {
        Thread {
            runCatching {
                val url = URL("${RetrofitClient.BASE_URL}workout_logs")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("apikey", RetrofitClient.API_KEY)
                    setRequestProperty("Authorization", "Bearer ${RetrofitClient.API_KEY}")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "return=minimal")
                    doOutput = true
                }

                connection.outputStream.use { outputStream ->
                    outputStream.write(gson.toJson(log).toByteArray())
                }

                val code = connection.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP error $code while sending log")
                }
            }.let(callback)
        }.start()
    }

    fun pushWithVolley(log: WorkoutLog, callback: (Result<Unit>) -> Unit) {
        val jsonBody = JSONObject(gson.toJson(log))
        val request = object : JsonObjectRequest(
            Request.Method.POST,
            "${RetrofitClient.BASE_URL}workout_logs",
            jsonBody,
            { callback(Result.success(Unit)) },
            { error -> callback(Result.failure(error)) }
        ) {
            override fun getHeaders(): MutableMap<String, String> = mutableMapOf(
                "apikey" to RetrofitClient.API_KEY,
                "Authorization" to "Bearer ${RetrofitClient.API_KEY}",
                "Content-Type" to "application/json",
                "Prefer" to "return=minimal"
            )
        }

        requestQueue.add(request)
    }
}
