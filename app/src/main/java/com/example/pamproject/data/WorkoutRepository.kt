package com.example.pamproject.data

import android.content.Context
import android.util.Log
import com.example.pamproject.api.ApiService
import com.example.pamproject.api.CreateWorkoutLogRequest
import com.example.pamproject.api.HttpUrlConnectionClient
import com.example.pamproject.api.RetrofitClient
import com.example.pamproject.api.VolleyClient
import com.example.pamproject.api.WorkoutLogDto
import com.example.pamproject.model.DailyProgress
import com.example.pamproject.model.DailyStats
import com.example.pamproject.model.WorkoutLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Repository for workout logs that supports multiple REST API clients
 * Implements client-server communication using threads
 */
enum class ApiClientType {
    RETROFIT,           // Uses Retrofit library with GSON
    HTTP_URL_CONNECTION, // Uses HttpURLConnection
    VOLLEY              // Uses Volley library
}

class WorkoutRepository(private val context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // API Clients for REST communication - using lazy initialization to prevent crash
    private val retrofitService: ApiService by lazy {
        try {
            RetrofitClient.apiService
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Retrofit: ${e.message}")
            throw e
        }
    }
    private val httpUrlConnectionClient by lazy { HttpUrlConnectionClient() }
    private val volleyClient by lazy { VolleyClient(context) }

    // Current API client type - can be switched at runtime
    var currentApiClient: ApiClientType = ApiClientType.RETROFIT

    companion object {
        private const val TAG = "WorkoutRepository"
        private const val PREF_NAME = "workout_prefs"
        private const val KEY_WORKOUT_LOGS = "workoutLogs"
    }

    // ==================== REST API Methods ====================

    /**
     * Load logs from Supabase REST API using current client
     * Runs on IO thread for network operations
     */
    suspend fun loadLogsFromApi(): List<WorkoutLog> = withContext(Dispatchers.IO) {
        try {
            when (currentApiClient) {
                ApiClientType.RETROFIT -> loadLogsWithRetrofit()
                ApiClientType.HTTP_URL_CONNECTION -> loadLogsWithHttpUrlConnection()
                ApiClientType.VOLLEY -> loadLogsWithVolley()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading logs from API: ${e.message}")
            // Fallback to local storage
            loadLogs()
        }
    }

    private suspend fun loadLogsWithRetrofit(): List<WorkoutLog> {
        Log.d(TAG, "Loading logs with Retrofit on thread: ${Thread.currentThread().name}")
        val response = retrofitService.getAllWorkoutLogs()
        return if (response.isSuccessful) {
            val dtoList = response.body() ?: emptyList()
            dtoList.map { it.toWorkoutLog() }.also {
                // Cache locally
                saveLogs(it)
            }
        } else {
            Log.e(TAG, "Retrofit error: ${response.code()} - ${response.message()}")
            loadLogs()
        }
    }

    private suspend fun loadLogsWithHttpUrlConnection(): List<WorkoutLog> {
        Log.d(TAG, "Loading logs with HttpUrlConnection on thread: ${Thread.currentThread().name}")
        val result = httpUrlConnectionClient.getAllWorkoutLogs()
        return result.fold(
            onSuccess = { dtoList ->
                dtoList.map { it.toWorkoutLog() }.also {
                    saveLogs(it)
                }
            },
            onFailure = { e ->
                Log.e(TAG, "HttpUrlConnection error: ${e.message}")
                loadLogs()
            }
        )
    }

    private suspend fun loadLogsWithVolley(): List<WorkoutLog> {
        Log.d(TAG, "Loading logs with Volley on thread: ${Thread.currentThread().name}")
        val result = volleyClient.getAllWorkoutLogs()
        return result.fold(
            onSuccess = { dtoList ->
                dtoList.map { it.toWorkoutLog() }.also {
                    saveLogs(it)
                }
            },
            onFailure = { e ->
                Log.e(TAG, "Volley error: ${e.message}")
                loadLogs()
            }
        )
    }

    /**
     * Add log to Supabase REST API using current client
     * Runs on IO thread for network operations
     */
    suspend fun addLogToApi(log: WorkoutLog): List<WorkoutLog> = withContext(Dispatchers.IO) {
        try {
            val request = log.toCreateRequest()
            when (currentApiClient) {
                ApiClientType.RETROFIT -> addLogWithRetrofit(request, log)
                ApiClientType.HTTP_URL_CONNECTION -> addLogWithHttpUrlConnection(request, log)
                ApiClientType.VOLLEY -> addLogWithVolley(request, log)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding log to API: ${e.message}")
            // Fallback to local storage
            addLog(log)
        }
    }

    private suspend fun addLogWithRetrofit(request: CreateWorkoutLogRequest, log: WorkoutLog): List<WorkoutLog> {
        Log.d(TAG, "Adding log with Retrofit on thread: ${Thread.currentThread().name}")
        val response = retrofitService.createWorkoutLog(workoutLog = request)
        return if (response.isSuccessful) {
            loadLogsFromApi()
        } else {
            Log.e(TAG, "Retrofit error: ${response.code()} - ${response.message()}")
            addLog(log)
        }
    }

    private suspend fun addLogWithHttpUrlConnection(request: CreateWorkoutLogRequest, log: WorkoutLog): List<WorkoutLog> {
        Log.d(TAG, "Adding log with HttpUrlConnection on thread: ${Thread.currentThread().name}")
        val result = httpUrlConnectionClient.createWorkoutLog(request)
        return result.fold(
            onSuccess = { loadLogsFromApi() },
            onFailure = { e ->
                Log.e(TAG, "HttpUrlConnection error: ${e.message}")
                addLog(log)
            }
        )
    }

    private suspend fun addLogWithVolley(request: CreateWorkoutLogRequest, log: WorkoutLog): List<WorkoutLog> {
        Log.d(TAG, "Adding log with Volley on thread: ${Thread.currentThread().name}")
        val result = volleyClient.createWorkoutLog(request)
        return result.fold(
            onSuccess = { loadLogsFromApi() },
            onFailure = { e ->
                Log.e(TAG, "Volley error: ${e.message}")
                addLog(log)
            }
        )
    }

    /**
     * Delete log from Supabase REST API using current client
     * Runs on IO thread for network operations
     */
    suspend fun deleteLogFromApi(log: WorkoutLog): List<WorkoutLog> = withContext(Dispatchers.IO) {
        try {
            when (currentApiClient) {
                ApiClientType.RETROFIT -> deleteLogWithRetrofit(log)
                ApiClientType.HTTP_URL_CONNECTION -> deleteLogWithHttpUrlConnection(log)
                ApiClientType.VOLLEY -> deleteLogWithVolley(log)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting log from API: ${e.message}")
            deleteLog(log)
        }
    }

    private suspend fun deleteLogWithRetrofit(log: WorkoutLog): List<WorkoutLog> {
        Log.d(TAG, "Deleting log with Retrofit on thread: ${Thread.currentThread().name}")
        val response = retrofitService.deleteWorkoutLog(timestampFilter = "eq.${log.timestamp}")
        return if (response.isSuccessful) {
            loadLogsFromApi()
        } else {
            Log.e(TAG, "Retrofit error: ${response.code()}")
            deleteLog(log)
        }
    }

    private suspend fun deleteLogWithHttpUrlConnection(log: WorkoutLog): List<WorkoutLog> {
        Log.d(TAG, "Deleting log with HttpUrlConnection on thread: ${Thread.currentThread().name}")
        val result = httpUrlConnectionClient.deleteWorkoutLog(log.timestamp)
        return result.fold(
            onSuccess = { loadLogsFromApi() },
            onFailure = { e ->
                Log.e(TAG, "HttpUrlConnection error: ${e.message}")
                deleteLog(log)
            }
        )
    }

    private suspend fun deleteLogWithVolley(log: WorkoutLog): List<WorkoutLog> {
        Log.d(TAG, "Deleting log with Volley on thread: ${Thread.currentThread().name}")
        val result = volleyClient.deleteWorkoutLog(log.timestamp)
        return result.fold(
            onSuccess = { loadLogsFromApi() },
            onFailure = { e ->
                Log.e(TAG, "Volley error: ${e.message}")
                deleteLog(log)
            }
        )
    }

    /**
     * Clear all logs from Supabase REST API using current client
     * Runs on IO thread for network operations
     */
    suspend fun clearLogsFromApi(): List<WorkoutLog> = withContext(Dispatchers.IO) {
        try {
            when (currentApiClient) {
                ApiClientType.RETROFIT -> clearLogsWithRetrofit()
                ApiClientType.HTTP_URL_CONNECTION -> clearLogsWithHttpUrlConnection()
                ApiClientType.VOLLEY -> clearLogsWithVolley()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing logs from API: ${e.message}")
            clearLogs()
        }
    }

    private suspend fun clearLogsWithRetrofit(): List<WorkoutLog> {
        Log.d(TAG, "Clearing logs with Retrofit on thread: ${Thread.currentThread().name}")
        val response = retrofitService.deleteAllWorkoutLogs()
        return if (response.isSuccessful) {
            clearLogs()
        } else {
            Log.e(TAG, "Retrofit error: ${response.code()}")
            clearLogs()
        }
    }

    private suspend fun clearLogsWithHttpUrlConnection(): List<WorkoutLog> {
        Log.d(TAG, "Clearing logs with HttpUrlConnection on thread: ${Thread.currentThread().name}")
        val result = httpUrlConnectionClient.deleteAllWorkoutLogs()
        return result.fold(
            onSuccess = { clearLogs() },
            onFailure = { e ->
                Log.e(TAG, "HttpUrlConnection error: ${e.message}")
                clearLogs()
            }
        )
    }

    private suspend fun clearLogsWithVolley(): List<WorkoutLog> {
        Log.d(TAG, "Clearing logs with Volley on thread: ${Thread.currentThread().name}")
        val result = volleyClient.deleteAllWorkoutLogs()
        return result.fold(
            onSuccess = { clearLogs() },
            onFailure = { e ->
                Log.e(TAG, "Volley error: ${e.message}")
                clearLogs()
            }
        )
    }

    // ==================== DTO Conversion ====================

    private fun WorkoutLogDto.toWorkoutLog(): WorkoutLog {
        return WorkoutLog(
            date = this.date,
            time = this.time,
            workout = this.workout,
            durationMinutes = this.durationMinutes,
            calories = this.calories,
            timestamp = this.timestamp,
            imageUri = this.imageUri
        )
    }

    private fun WorkoutLog.toCreateRequest(): CreateWorkoutLogRequest {
        return CreateWorkoutLogRequest(
            date = this.date,
            time = this.time,
            workout = this.workout,
            durationMinutes = this.durationMinutes,
            calories = this.calories,
            timestamp = this.timestamp,
            imageUri = this.imageUri
        )
    }

    // ==================== Local Storage Methods (Fallback) ====================

    fun loadLogs(): List<WorkoutLog> {
        val raw = sharedPreferences.getString(KEY_WORKOUT_LOGS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            List(jsonArray.length()) { index ->
                jsonArray.getJSONObject(index).toWorkoutLog()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLogs(logs: List<WorkoutLog>) {
        val jsonArray = JSONArray().apply {
            logs.forEach { log ->
                put(log.toJson())
            }
        }
        sharedPreferences.edit().putString(KEY_WORKOUT_LOGS, jsonArray.toString()).apply()
    }

    fun addLog(log: WorkoutLog): List<WorkoutLog> {
        val updated = loadLogs() + log
        saveLogs(updated)
        return updated
    }

    fun deleteLog(log: WorkoutLog): List<WorkoutLog> {
        val updated = loadLogs().filterNot { it.timestamp == log.timestamp }
        saveLogs(updated)
        return updated
    }

    fun clearLogs(): List<WorkoutLog> {
        saveLogs(emptyList())
        return emptyList()
    }

    fun calculateTodayStats(logs: List<WorkoutLog>): DailyStats {
        val today = LocalDate.now().toString()
        val todayLogs = logs.filter { it.date == today }
        val totalDuration = todayLogs.sumOf { it.durationMinutes }
        val totalCalories = todayLogs.sumOf { it.calories }
        return DailyStats(
            totalDurationMinutes = totalDuration,
            totalCalories = totalCalories,
            streak = calculateStreak(logs)
        )
    }

    fun getRecentProgress(logs: List<WorkoutLog>, days: Int = 7): List<DailyProgress> {
        val today = LocalDate.now()
        return (0 until days).map { index ->
            val date = today.minusDays((days - 1 - index).toLong())
            val label = when (date.dayOfWeek.value) {
                1 -> "Mon"
                2 -> "Tue"
                3 -> "Wed"
                4 -> "Thu"
                5 -> "Fri"
                6 -> "Sat"
                7 -> "Sun"
                else -> "---"
            }
            val logsForDate = logs.filter { it.date == date.toString() }

            DailyProgress(
                dateLabel = label,
                totalCalories = logsForDate.sumOf { it.calories },
                totalDurationMinutes = logsForDate.sumOf { it.durationMinutes }
            )
        }
    }

    private fun calculateStreak(logs: List<WorkoutLog>): Int {
        if (logs.isEmpty()) return 0
        val uniqueDates = logs.mapNotNull {
            runCatching { LocalDate.parse(it.date) }.getOrNull()
        }.distinct().sortedDescending()

        var streak = 0
        var cursor = LocalDate.now()
        for (date in uniqueDates) {
            if (date == cursor) {
                streak++
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    private fun JSONObject.toWorkoutLog(): WorkoutLog {
        val date = optString("date", LocalDate.now().toString())
        val time = optString("time", "00:00")
        val storedTimestamp = optLong("timestamp", -1L)
        val imageUri = optString("imageUri", null)

        // If no timestamp stored, create one from date + time
        val timestamp = if (storedTimestamp == -1L) {
            try {
                val localDate = LocalDate.parse(date)
                val localTime = LocalTime.parse(time)
                val dateTime = LocalDateTime.of(localDate, localTime)
                dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) {
                0L // Very old timestamp so it appears last
            }
        } else {
            storedTimestamp
        }

        return WorkoutLog(
            date = date,
            time = time,
            workout = optString("workout"),
            durationMinutes = optDouble("duration"),
            calories = optDouble("calories"),
            timestamp = timestamp,
            imageUri = imageUri
        )
    }

    private fun WorkoutLog.toJson(): JSONObject {
        return JSONObject().apply {
            put("date", date)
            put("time", time)
            put("workout", workout)
            put("duration", durationMinutes)
            put("calories", calories)
            put("timestamp", timestamp)
            if (imageUri != null) {
                put("imageUri", imageUri)
            }
        }
    }
}
