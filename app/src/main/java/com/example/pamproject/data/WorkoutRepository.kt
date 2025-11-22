package com.example.pamproject.data

import android.content.Context
import com.example.pamproject.model.DailyStats
import com.example.pamproject.model.WorkoutLog
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

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
        return WorkoutLog(
            date = optString("date"),
            workout = optString("workout"),
            durationMinutes = optDouble("duration"),
            calories = optDouble("calories")
        )
    }

    private fun WorkoutLog.toJson(): JSONObject {
        return JSONObject().apply {
            put("date", date)
            put("workout", workout)
            put("duration", durationMinutes)
            put("calories", calories)
        }
    }

    companion object {
        private const val PREF_NAME = "workout_prefs"
        private const val KEY_WORKOUT_LOGS = "workoutLogs"
    }
}
