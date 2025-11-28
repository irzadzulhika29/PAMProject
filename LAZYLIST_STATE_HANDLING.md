# Implementasi LazyList State Handling

## Ringkasan
Aplikasi ini telah mengimplementasikan state handling yang komprehensif untuk LazyList sehingga data dan posisi scroll tetap terjaga saat terjadi configuration changes (misalnya rotasi layar).

## Komponen State Handling

### 1. ViewModel dengan StateFlow
**File**: `viewmodel/WorkoutViewModel.kt`

```kotlin
class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {
    // StateFlow untuk data logs - survive configuration changes
    private val _logs = MutableStateFlow(repository.loadLogs())
    val logs: StateFlow<List<WorkoutLog>> = _logs.asStateFlow()
    
    // StateFlow lainnya untuk stats dan progress
    val todayStats: StateFlow<DailyStats> = _logs
        .map { repository.calculateTodayStats(it) }
        .stateIn(...)
    
    val weeklyProgress: StateFlow<List<DailyProgress>> = _logs
        .map { repository.getRecentProgress(it) }
        .stateIn(...)
}
```

**Cara Kerja:**
- `ViewModel` bertahan saat configuration changes (rotasi layar)
- `StateFlow` menyimpan data secara reactive dan dapat di-observe
- Data tidak hilang karena ViewModel tidak di-destroy saat rotasi

### 2. LazyListState untuk Scroll Position
**File**: `ui/screen/DashboardComponents.kt`

```kotlin
@Composable
fun WorkoutHistory(
    logs: List<WorkoutLog>,
    onDeleteLogRequested: (WorkoutLog) -> Unit,
    onDeleteAllRequested: () -> Unit,
    onLogClick: (WorkoutLog) -> Unit = {}
) {
    // rememberLazyListState menyimpan scroll position
    val listState = rememberLazyListState()
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ... UI components
        
        LazyColumn(
            state = listState, // State handling untuk scroll position
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sortedLogs, key = { it.timestamp }) { log ->
                WorkoutLogCard(
                    log = log,
                    onLogClick = onLogClick,
                    onDeleteLogRequested = onDeleteLogRequested
                )
            }
        }
    }
}
```

**Cara Kerja:**
- `rememberLazyListState()` menyimpan posisi scroll (firstVisibleItemIndex, firstVisibleItemScrollOffset)
- `remember` mempertahankan state selama composable dalam composition tree
- Scroll position otomatis di-restore saat configuration changes
- Key parameter pada items() memastikan efficient recomposition

### 3. Observing State di Navigation
**File**: `navigation/AppNavigation.kt`

```kotlin
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { WorkoutRepository(context) }
    
    // ViewModel scope - survive configuration changes
    val workoutViewModel: WorkoutViewModel = viewModel(
        factory = WorkoutViewModel.provideFactory(repository)
    )

    // Collect StateFlow as Compose State
    val stats by workoutViewModel.todayStats.collectAsState()
    val weeklyProgress by workoutViewModel.weeklyProgress.collectAsState()
    val logs by workoutViewModel.logs.collectAsState()
    val timerState by workoutViewModel.timerState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                stats = stats,
                progress = weeklyProgress,
                workouts = workoutViewModel.workouts,
                logs = logs,
                onWorkoutClick = { workoutId ->
                    workoutViewModel.startSession(workoutId)
                    navController.navigate(Screen.Session.createRoute(workoutId))
                },
                onDeleteLog = workoutViewModel::deleteLog,
                onDeleteAllLogs = workoutViewModel::clearLogs
            )
        }
    }
}
```

**Cara Kerja:**
- `viewModel()` composable function memastikan ViewModel instance bertahan
- `collectAsState()` mengkonversi StateFlow menjadi Compose State
- State otomatis trigger recomposition saat data berubah
- Data persists melalui configuration changes

## Alur State Handling

```
[Configuration Change (Rotasi)] 
        ↓
[ViewModel TIDAK di-destroy]
        ↓
[StateFlow tetap mempertahankan data]
        ↓
[rememberLazyListState mempertahankan scroll position]
        ↓
[UI di-recompose dengan data dan scroll position yang sama]
        ↓
[User tidak kehilangan data atau posisi scroll]
```

## Fitur Tambahan

### 1. SnackBar pada Item Click
**File**: `ui/screen/DashboardScreen.kt`

```kotlin
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
```

Saat user mengklik card workout, SnackBar menampilkan informasi lengkap dari WorkoutLog yang diklik.

### 2. Unique Keys untuk Efficient Updates
```kotlin
items(sortedLogs, key = { it.timestamp }) { log ->
    // Item composable
}
```

Menggunakan timestamp sebagai unique key memastikan:
- Recomposition hanya terjadi pada item yang berubah
- Animasi list updates lebih smooth
- Performance lebih optimal

## Testing State Handling

Untuk memverifikasi bahwa state handling bekerja dengan benar:

1. **Test Rotasi Layar:**
   - Buka aplikasi dan tambahkan beberapa workout logs
   - Scroll ke posisi tertentu dalam list
   - Rotasi layar (portrait ↔ landscape)
   - **Expected**: Data tetap ada dan scroll position terjaga

2. **Test Data Persistence:**
   - Tambahkan workout log
   - Tutup dan buka kembali aplikasi
   - **Expected**: Data tetap tersimpan (via WorkoutRepository)

3. **Test SnackBar:**
   - Klik pada salah satu workout log card
   - **Expected**: SnackBar muncul dengan informasi workout yang diklik

## Kode Program yang Mengimplementasikan State Handling

### 1. ViewModel State Declaration
```kotlin
// File: WorkoutViewModel.kt
private val _logs = MutableStateFlow(repository.loadLogs())
val logs: StateFlow<List<WorkoutLog>> = _logs.asStateFlow()
```

### 2. LazyList State Declaration
```kotlin
// File: DashboardComponents.kt
@Composable
fun WorkoutHistory(...) {
    val listState = rememberLazyListState()
    
    LazyColumn(state = listState, ...) {
        items(sortedLogs, key = { it.timestamp }) { log ->
            // ...
        }
    }
}
```

### 3. State Observation
```kotlin
// File: AppNavigation.kt
val logs by workoutViewModel.logs.collectAsState()

DashboardScreen(
    logs = logs,
    // ...
)
```

## Kesimpulan

Implementasi state handling yang dilakukan memastikan:
✅ Data tidak hilang saat rotasi layar
✅ Scroll position terjaga saat configuration changes
✅ SnackBar menampilkan informasi item yang diklik
✅ Performance optimal dengan unique keys
✅ Reactive updates menggunakan StateFlow
✅ Separation of concerns dengan ViewModel pattern

