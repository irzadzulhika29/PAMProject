package com.example.pamproject.data

import android.content.Context
import com.example.pamproject.model.DailyStats
import com.example.pamproject.model.WorkoutLog
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class WorkoutRepository(private val context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

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
            timestamp = timestamp
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
        }
    }

    companion object {
        private const val PREF_NAME = "workout_prefs"
        private const val KEY_WORKOUT_LOGS = "workoutLogs"
    }
}
