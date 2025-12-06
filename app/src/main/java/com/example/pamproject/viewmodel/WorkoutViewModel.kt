package com.example.pamproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

    val workouts: List<Workout> = WorkoutData.workouts

    private val _logs = MutableStateFlow(repository.loadLogs())
    val logs: StateFlow<List<WorkoutLog>> = _logs.asStateFlow()

    init {
        refreshFromServer()
    }

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
        val updatedLogs = repository.addLog(log)
        _logs.value = updatedLogs
        viewModelScope.launch {
            repository.uploadLogWithRetrofit(log)
            repository.uploadLogWithVolley(log)
        }
        resetTimer()
        return log
    }

    fun deleteLog(log: WorkoutLog) {
        val updatedLogs = repository.deleteLog(log)
        _logs.value = updatedLogs
    }

    fun clearLogs() {
        val updatedLogs = repository.clearLogs()
        _logs.value = updatedLogs
    }

    fun getWorkoutById(id: Int): Workout? = workouts.find { it.id == id }

    private fun refreshFromServer() {
        viewModelScope.launch {
            val remoteLogs = repository.fetchLogsWithRetrofit()
            if (remoteLogs.isNotEmpty()) {
                repository.saveLogs(remoteLogs)
                _logs.value = remoteLogs
            }

            repository.fetchLogsWithHttpUrlConnection(
                onSuccess = { logsFromHttp ->
                    viewModelScope.launch {
                        repository.saveLogs(logsFromHttp)
                        _logs.value = logsFromHttp
                    }
                },
                onError = { /* Ignored for demo usage */ }
            )

            repository.fetchLogsWithVolley(
                onSuccess = { volleyLogs ->
                    _logs.value = volleyLogs
                    repository.saveLogs(volleyLogs)
                },
                onError = { /* Ignored for demo usage */ }
            )
        }
    }

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

    companion object {
        fun provideFactory(repository: WorkoutRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WorkoutViewModel(repository) as T
                }
            }
    }
}
