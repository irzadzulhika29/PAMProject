# Aplikasi Daftar Akun Mahasiswa

Aplikasi Android yang menampilkan daftar akun mahasiswa dalam bentuk card list. Ketika card diklik, aplikasi akan berpindah ke halaman detail yang menampilkan informasi lengkap mahasiswa.

## Fitur yang Telah Dibuat

### 1. Model Data (Account.kt)
- **Account**: Data class yang berisi informasi mahasiswa
  - ID
  - Nama
  - NIM
  - Alamat
  - Email
  - Nomor Telepon
  - Jurusan
- **AccountData**: Object yang berisi sample data 5 mahasiswa

### 2. Halaman Daftar Akun (AccountListScreen.kt)
- Menampilkan daftar akun mahasiswa dalam bentuk cards
- Setiap card menampilkan:
  - Icon person
  - Nama mahasiswa
  - NIM
  - Alamat (singkat)
- Card dapat diklik untuk membuka detail
- Menggunakan LazyColumn untuk performa optimal

### 3. Halaman Detail Akun (AccountDetailScreen.kt)
- Menampilkan informasi lengkap mahasiswa:
  - Profile header dengan icon dan nama
  - NIM
  - Jurusan
  - Alamat lengkap
  - Email
  - Nomor telepon
- Tombol back untuk kembali ke daftar
- Setiap informasi ditampilkan dengan icon yang sesuai

### 4. Navigation (Screen.kt & AppNavigation.kt)
- Navigation menggunakan Jetpack Compose Navigation
- Dua route:
  - AccountList: Halaman daftar
  - AccountDetail: Halaman detail dengan parameter accountId
- Navigation menggunakan NavController untuk berpindah halaman

## Dependencies yang Ditambahkan

Di `gradle/libs.versions.toml`:
```toml
navigationCompose = "2.7.7"
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
```

Di `app/build.gradle.kts`:
```kotlin
implementation(libs.androidx.navigation.compose)
```

## Cara Menjalankan

1. **Sync Gradle**: Klik "Sync Now" pada notifikasi di Android Studio atau pilih File > Sync Project with Gradle Files
2. **Build Project**: Build > Make Project
3. **Run**: Jalankan aplikasi di emulator atau device

## Struktur File yang Dibuat

```
app/src/main/java/com/example/pamproject/
├── model/
│   └── Account.kt                 # Data model dan sample data
├── ui/
│   └── screen/
│       ├── AccountListScreen.kt   # Halaman daftar akun
│       └── AccountDetailScreen.kt # Halaman detail akun
└── navigation/
    ├── Screen.kt                  # Definisi routes
    └── AppNavigation.kt           # Setup navigation
```

## Catatan

- Aplikasi ini menggunakan Material 3 Design
- Sample data berisi 5 mahasiswa
- Anda dapat menambahkan lebih banyak data di `AccountData.accountList`
- Aplikasi sudah siap dijalankan setelah Gradle sync selesai

