package com.example.pamproject.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.example.pamproject.api.RetrofitClient
import com.example.pamproject.api.SupabaseConfig
import com.example.pamproject.model.DailyProgress
import com.example.pamproject.model.DailyStats
import com.example.pamproject.model.NetworkWorkoutLog
import com.example.pamproject.model.WorkoutLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.concurrent.thread

class WorkoutRepository(private val context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val requestQueue by lazy { Volley.newRequestQueue(context.applicationContext) }

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

    suspend fun fetchLogsWithRetrofit(): List<WorkoutLog> = withContext(Dispatchers.IO) {
        runCatching {
            RetrofitClient.apiService
                .getWorkoutLogs(
                    apiKey = SupabaseConfig.API_KEY,
                    bearer = SupabaseConfig.AUTH_HEADER
                )
                .map(NetworkWorkoutLog::toDomain)
        }.getOrElse { emptyList() }
    }

    fun fetchLogsWithHttpUrlConnection(
        onSuccess: (List<WorkoutLog>) -> Unit,
        onError: (String) -> Unit
    ) {
        thread {
            try {
                val url = URL("${SupabaseConfig.BASE_REST_URL}?select=*")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SupabaseConfig.API_KEY)
                    setRequestProperty("Authorization", SupabaseConfig.AUTH_HEADER)
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }

                connection.inputStream.use { inputStream ->
                    val response = inputStream.bufferedReader().readText()
                    val type = object : TypeToken<List<NetworkWorkoutLog>>() {}.type
                    val parsed = gson.fromJson<List<NetworkWorkoutLog>>(response, type)
                    mainHandler.post { onSuccess(parsed.map(NetworkWorkoutLog::toDomain)) }
                }
            } catch (e: Exception) {
                mainHandler.post { onError(e.message ?: "Unknown error") }
            }
        }
    }

    fun fetchLogsWithVolley(
        onSuccess: (List<WorkoutLog>) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = object : JsonArrayRequest(
            Request.Method.GET,
            "${SupabaseConfig.BASE_REST_URL}?select=*",
            null,
            { response ->
                val type = object : TypeToken<List<NetworkWorkoutLog>>() {}.type
                val parsed: List<NetworkWorkoutLog> = gson.fromJson(response.toString(), type)
                onSuccess(parsed.map(NetworkWorkoutLog::toDomain))
            },
            { error -> onError(error.message ?: "Volley error") }
        ) {
            override fun getHeaders(): MutableMap<String, String> = mutableMapOf(
                "apikey" to SupabaseConfig.API_KEY,
                "Authorization" to SupabaseConfig.AUTH_HEADER,
                "Accept" to "application/json"
            )
        }
        requestQueue.add(request)
    }

    suspend fun uploadLogWithRetrofit(log: WorkoutLog) = withContext(Dispatchers.IO) {
        runCatching {
            RetrofitClient.apiService.postWorkoutLog(
                apiKey = SupabaseConfig.API_KEY,
                bearer = SupabaseConfig.AUTH_HEADER,
                log = NetworkWorkoutLog.fromDomain(log)
            )
        }
    }

    fun uploadLogWithVolley(log: WorkoutLog, onResult: (Boolean) -> Unit = {}) {
        val payload = gson.toJson(NetworkWorkoutLog.fromDomain(log))
        val request = object : JsonArrayRequest(
            Request.Method.POST,
            SupabaseConfig.BASE_REST_URL,
            null,
            { onResult(true) },
            { onResult(false) }
        ) {
            override fun getBody(): ByteArray = payload.toByteArray()

            override fun getBodyContentType(): String = "application/json"

            override fun getHeaders(): MutableMap<String, String> = mutableMapOf(
                "apikey" to SupabaseConfig.API_KEY,
                "Authorization" to SupabaseConfig.AUTH_HEADER,
                "Prefer" to "return=representation"
            )
        }
        requestQueue.add(request)
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

    companion object {
        private const val PREF_NAME = "workout_prefs"
        private const val KEY_WORKOUT_LOGS = "workoutLogs"
    }
}
