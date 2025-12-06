package com.example.pamproject.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.pamproject.model.DailyProgress
import com.example.pamproject.model.DailyStats
import com.example.pamproject.model.Workout
import com.example.pamproject.model.WorkoutLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    stats: DailyStats,
    progress: List<DailyProgress>,
    workouts: List<Workout>,
    logs: List<WorkoutLog>,
    onWorkoutClick: (Int) -> Unit,
    onDeleteLog: (WorkoutLog) -> Unit,
    onDeleteAllLogs: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var logToDelete by remember { mutableStateOf<WorkoutLog?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<WorkoutLog?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Workit",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Stay consistent, stay strong",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh data",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                GreetingCard()

                StatsCard(
                    stats = stats,
                    progress = progress
                )

                Text(
                    text = "Pilih Workout",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((workouts.size / 3 + if (workouts.size % 3 != 0) 1 else 0) * 140).dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false
                ) {
                    items(workouts, key = { it.id }) { workout ->
                        WorkoutOptionCard(
                            workout = workout,
                            onClick = { onWorkoutClick(workout.id) }
                        )
                    }
                }

                WorkoutHistory(
                    logs = logs,
                    onDeleteLogRequested = { log ->
                        selectedLog = null
                        showDeleteAllDialog = false
                        logToDelete = log
                    },
                    onDeleteAllRequested = {
                        selectedLog = null
                        logToDelete = null
                        showDeleteAllDialog = true
                    },
                    onLogClick = { log ->
                        logToDelete = null
                        showDeleteAllDialog = false
                        selectedLog = log
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "${log.workout} • ${formatMinutes(log.durationMinutes)} • ${log.calories.toInt()} kcal • ${log.date} ${log.time}"
                            )
                        }
                    }
                )
            }
        }

        DashboardDialogs(
            logToDelete = logToDelete,
            onDismissDeleteLog = { logToDelete = null },
            onDeleteLog = {
                logToDelete?.let { onDeleteLog(it) }
                logToDelete = null
            },
            showDeleteAllDialog = showDeleteAllDialog,
            onDismissDeleteAll = { showDeleteAllDialog = false },
            onDeleteAll = {
                onDeleteAllLogs()
                showDeleteAllDialog = false
            },
            selectedLog = selectedLog,
            onDismissSelectedLog = { selectedLog = null }
        )
    }
}
