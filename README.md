# Aplikasi Pelacak Workout

Aplikasi Android sederhana berbasis Jetpack Compose untuk mencatat sesi latihan harian. Pengguna dapat memilih jenis workout, menjalankan timer, menghitung kalori otomatis, menyimpan riwayat ke local storage, dan melihat statistik harian di dashboard utama.

## Fitur Utama

- **Dashboard Harian**
  - Ringkasan durasi total dan kalori yang terbakar hari ini.
  - Streak harian berdasarkan hari berturut-turut yang memiliki catatan latihan.
  - Grid pilihan workout (Yoga, Running, Stretching, HIIT, Cycling, Walking).

- **Halaman Timer**
  - Menampilkan nama workout yang dipilih.
  - Timer sederhana (count-up) dengan mode Start, Stop/Pause, Resume, dan Finish.
  - Perhitungan kalori otomatis menggunakan rumus `MET × 3.5 × berat badan / 200 × durasi (menit)` dengan berat default 70kg.
  - Menyimpan riwayat ke `SharedPreferences` dalam format JSON dan memperbarui dashboard setelah selesai.

## Arsitektur dan Teknologi

- **UI**: Jetpack Compose + Material 3
- **Navigation**: Compose Navigation dengan dua layar utama (Dashboard dan Session)
- **State Management**: `StateFlow` pada `WorkoutViewModel`
- **Penyimpanan Lokal**: `SharedPreferences` untuk menyimpan riwayat latihan (tanpa backend)

## Struktur Folder

```
app/src/main/java/com/example/pamproject/
├── data/
│   └── WorkoutRepository.kt     # Baca/tulis riwayat workout di SharedPreferences
├── model/
│   ├── Workout.kt               # Data class workout + daftar MET
│   └── WorkoutLog.kt            # Catatan riwayat & ringkasan statistik harian
├── navigation/
│   ├── AppNavigation.kt         # Setup nav controller & routes
│   └── Screen.kt                # Definisi route Dashboard & Session
├── ui/screen/
│   ├── DashboardScreen.kt       # Statistik harian + grid pilihan workout
│   └── WorkoutSessionScreen.kt  # Timer sesi, kontrol start/pause/resume/finish
└── viewmodel/
    └── WorkoutViewModel.kt      # Logika timer, perhitungan kalori, dan statistik
```

## Menjalankan Proyek

1. **Sync Gradle** di Android Studio.
2. **Build & Run** pada emulator atau perangkat fisik (minSdk 24).
3. Pilih workout di Dashboard, jalankan timer, lalu tekan **Finish Workout** untuk menyimpan hasil dan memperbarui statistik harian.

