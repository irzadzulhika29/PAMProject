package com.example.pamproject.model

data class Account(
    val id: Int,
    val nama: String,
    val nim: String,
    val alamat: String,
    val email: String,
    val nomorTelepon: String,
    val jurusan: String
)

// Sample data
object AccountData {
    val accountList = listOf(
        Account(
            id = 1,
            nama = "Ahmad Rizki",
            nim = "20220101001",
            alamat = "Jl. Merdeka No. 123, Jakarta",
            email = "ahmad.rizki@email.com",
            nomorTelepon = "081234567890",
            jurusan = "Teknik Informatika"
        ),
        Account(
            id = 2,
            nama = "Siti Nurhaliza",
            nim = "20220101002",
            alamat = "Jl. Sudirman No. 45, Bandung",
            email = "siti.nurhaliza@email.com",
            nomorTelepon = "082345678901",
            jurusan = "Sistem Informasi"
        ),
        Account(
            id = 3,
            nama = "Budi Santoso",
            nim = "20220101003",
            alamat = "Jl. Gatot Subroto No. 67, Surabaya",
            email = "budi.santoso@email.com",
            nomorTelepon = "083456789012",
            jurusan = "Teknik Komputer"
        ),
        Account(
            id = 4,
            nama = "Dewi Lestari",
            nim = "20220101004",
            alamat = "Jl. Ahmad Yani No. 89, Yogyakarta",
            email = "dewi.lestari@email.com",
            nomorTelepon = "084567890123",
            jurusan = "Teknik Informatika"
        ),
        Account(
            id = 5,
            nama = "Eko Prasetyo",
            nim = "20220101005",
            alamat = "Jl. Diponegoro No. 12, Semarang",
            email = "eko.prasetyo@email.com",
            nomorTelepon = "085678901234",
            jurusan = "Sistem Informasi"
        )
    )
}

