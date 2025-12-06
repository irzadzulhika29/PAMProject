package com.example.pamproject.api

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.pamproject.model.WorkoutLog
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class WorkoutRemoteDataSource(
    context: Context,
    private val gson: Gson = Gson()
) {
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context.applicationContext)
    private val appContext = context.applicationContext

    private val uploadClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

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

    suspend fun uploadImageToSupabase(imageUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val contentResolver = appContext.contentResolver
                val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType)
                    ?: "jpg"

                val fileName = "workout-${System.currentTimeMillis()}-${UUID.randomUUID()}.$extension"
                val bytes = contentResolver.openInputStream(imageUri)
                    ?.use { it.readBytes() }
                    ?: error("Tidak dapat membaca file gambar")

                val requestBody = bytes.toRequestBody(mimeType.toMediaType())
                val request = OkHttpRequest.Builder()
                    .url("${RetrofitClient.STORAGE_BASE_URL}/object/${RetrofitClient.WORKOUT_IMAGE_BUCKET}/$fileName")
                    .addHeader("apikey", RetrofitClient.API_KEY)
                    .addHeader("Authorization", "Bearer ${RetrofitClient.API_KEY}")
                    .addHeader("Content-Type", mimeType)
                    .post(requestBody)
                    .build()

                uploadClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Upload gagal: ${response.code} ${response.message}")
                    }
                }

                "${RetrofitClient.PUBLIC_STORAGE_URL}/${RetrofitClient.WORKOUT_IMAGE_BUCKET}/$fileName"
            }
        }
}
