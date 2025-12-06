package com.example.pamproject.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pamproject.data.ApiClientType
import com.example.pamproject.data.WorkoutRepository
import com.example.pamproject.model.DailyProgress
import com.example.pamproject.model.DailyStats
import com.example.pamproject.model.Workout
import com.example.pamproject.model.WorkoutData
import com.example.pamproject.model.WorkoutLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {

    companion object {
        private const val TAG = "WorkoutViewModel"

        fun provideFactory(repository: WorkoutRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WorkoutViewModel(repository) as T
                }
            }
    }

    val workouts: List<Workout> = WorkoutData.workouts

    private val _logs = MutableStateFlow(repository.loadLogs())
    val logs: StateFlow<List<WorkoutLog>> = _logs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentApiClient = MutableStateFlow(repository.currentApiClient)
    val currentApiClient: StateFlow<ApiClientType> = _currentApiClient.asStateFlow()

    val todayStats: StateFlow<DailyStats> = _logs
        .map { repository.calculateTodayStats(it) }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.calculateTodayStats(_logs.value)
        )

    val weeklyProgress: StateFlow<List<DailyProgress>> = _logs
        .map { repository.getRecentProgress(it) }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.getRecentProgress(_logs.value)
        )

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    init {
        // Load from local storage first (safe, won't crash)
        // API loading is now manual via refresh button
        Log.d(TAG, "ViewModel initialized with ${_logs.value.size} local logs")
    }

    /**
     * Switch between different API clients (Retrofit, HttpUrlConnection, Volley)
     */
    fun switchApiClient(clientType: ApiClientType) {
        repository.currentApiClient = clientType
        _currentApiClient.value = clientType
        Log.d(TAG, "Switched to API client: $clientType")
        // Reload data with new client
        loadLogsFromApi()
    }

    /**
     * Load workout logs from Supabase API
     * Uses coroutine to run on background thread
     */
    fun loadLogsFromApi() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "Loading logs from API using: ${repository.currentApiClient}")
            try {
                val logs = repository.loadLogsFromApi()
                _logs.value = logs
                Log.d(TAG, "Successfully loaded ${logs.size} logs from API")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading logs: ${e.message}")
                // Keep using local data on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startSession(workoutId: Int) {
        val selectedWorkout = workouts.find { it.id == workoutId }
        _timerState.value = TimerState(selectedWorkout = selectedWorkout)
        timerJob?.cancel()
    }

    fun cancelSession() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }

    fun startTimer() {
        if (_timerState.value.selectedWorkout == null || _timerState.value.isRunning) return
        _timerState.value = _timerState.value.copy(isRunning = true, isPaused = false)
        startTicker()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false, isPaused = true)
    }

    fun resumeTimer() {
        if (_timerState.value.selectedWorkout == null || _timerState.value.isRunning) return
        _timerState.value = _timerState.value.copy(isRunning = true, isPaused = false)
        startTicker()
    }

    fun finishWorkout(imageUri: String? = null): WorkoutLog? {
        val workout = _timerState.value.selectedWorkout ?: return null
        val elapsedSeconds = _timerState.value.elapsedSeconds
        if (elapsedSeconds <= 0) {
            resetTimer()
            return null
        }

        val durationMinutes = elapsedSeconds / 60.0
        val calories = calculateCalories(workout.met, durationMinutes)
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val log = WorkoutLog(
            date = LocalDate.now().toString(),
            time = currentTime,
            workout = workout.name,
            durationMinutes = durationMinutes,
            calories = calories,
            timestamp = System.currentTimeMillis(),
            imageUri = imageUri
        )

        // Add log to API using background thread
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "Adding workout log to API using: ${repository.currentApiClient}")
            try {
                val updatedLogs = repository.addLogToApi(log)
                _logs.value = updatedLogs
                Log.d(TAG, "Successfully added log to API")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding log: ${e.message}")
                // Fallback to local storage
                val updatedLogs = repository.addLog(log)
                _logs.value = updatedLogs
            } finally {
                _isLoading.value = false
            }
        }

        resetTimer()
        return log
    }

    fun deleteLog(log: WorkoutLog) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "Deleting workout log from API using: ${repository.currentApiClient}")
            try {
                val updatedLogs = repository.deleteLogFromApi(log)
                _logs.value = updatedLogs
                Log.d(TAG, "Successfully deleted log from API")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting log: ${e.message}")
                val updatedLogs = repository.deleteLog(log)
                _logs.value = updatedLogs
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "Clearing all logs from API using: ${repository.currentApiClient}")
            try {
                val updatedLogs = repository.clearLogsFromApi()
                _logs.value = updatedLogs
                Log.d(TAG, "Successfully cleared all logs from API")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing logs: ${e.message}")
                val updatedLogs = repository.clearLogs()
                _logs.value = updatedLogs
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getWorkoutById(id: Int): Workout? = workouts.find { it.id == id }

    private fun calculateCalories(met: Double, durationMinutes: Double): Double {
        val defaultWeightKg = 70.0
        return met * 3.5 * defaultWeightKg / 200.0 * durationMinutes
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _timerState.value = _timerState.value.copy(
                    elapsedSeconds = _timerState.value.elapsedSeconds + 1
                )
            }
        }
    }

    private fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }

    data class TimerState(
        val selectedWorkout: Workout? = null,
        val elapsedSeconds: Int = 0,
        val isRunning: Boolean = false,
        val isPaused: Boolean = false
    )
}
