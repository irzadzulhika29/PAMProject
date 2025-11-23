package com.example.pamproject.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pamproject.model.Workout
import com.example.pamproject.model.WorkoutLog
import com.example.pamproject.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    workout: Workout?,
    timerState: WorkoutViewModel.TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> WorkoutLog?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFinishDialog by remember { mutableStateOf(false) }

    val finishSession = {
        val result = onFinish()
        result?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Durasi: ${"%.1f".format(it.durationMinutes)} menit · ${it.calories.toInt()} kcal"
                )
            }
            onBack()
        }
        showFinishDialog = false
    }

    LaunchedEffect(workout?.id) {
        if (workout == null) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = workout?.name ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimerDisplay(workout, timerState)

            ControlButtons(
                isRunning = timerState.isRunning,
                isPaused = timerState.isPaused,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onFinish = { showFinishDialog = true }
            )
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(text = "Selesaikan sesi?") },
            text = { Text(text = "Pastikan kamu sudah menyelesaikan workout ini.") },
            confirmButton = {
                TextButton(onClick = finishSession) {
                    Text(text = "Ya, selesai")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text(text = "Batal")
                }
            }
        )
    }
}

@Composable
private fun ControlButtons(
    isRunning: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Button(
            onClick = { if (isPaused) onResume() else onStart() },
            enabled = !isRunning,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isPaused) "Resume" else "Mulai Sekarang", fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPause,
                enabled = isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Pause", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onFinish,
                enabled = isRunning || isPaused,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Selesai", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TimerDisplay(workout: Workout?, timerState: WorkoutViewModel.TimerState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .shadow(24.dp, CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = CircleShape
                )
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = formatTime(timerState.elapsedSeconds),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = when {
                        timerState.isRunning -> "Tracking in progress"
                        timerState.isPaused -> "Paused, ready to resume"
                        else -> "Siap mulai sesi"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = workout?.name ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusPill(text = "MET ${workout?.met ?: "-"}")
                StatusPill(text = if (timerState.isRunning) "Live" else "Idle")
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
