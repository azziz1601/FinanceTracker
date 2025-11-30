package com.vps.financetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Kita tetap biarkan anotasi @Entity agar tidak error di Room (walau nanti Room kita bypass)
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ID Lokal (Legacy)
    val firestoreId: String = "", // ID Unik dari Firebase (Baru)
    val amount: Double = 0.0,
    val category: String = "",
    val note: String? = null,
    val isIncome: Boolean = false,
    val timestamp: Long = 0L,
    val userId: String = "" // Siapa yang menginput data ini? (Penting untuk Tim)
)
