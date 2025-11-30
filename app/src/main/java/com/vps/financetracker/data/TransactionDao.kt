package com.vps.financetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // --- TRANSAKSI ---
    
    // Ambil semua di bulan ini (Tanpa Search)
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    // FITUR BARU: Pencarian (Search)
    // Mencari text di Category ATAU Note, tapi TETAP dalam range tanggal bulan ini
    @Query("""
        SELECT * FROM transactions 
        WHERE (timestamp BETWEEN :startDate AND :endDate) 
        AND (category LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') 
        ORDER BY timestamp DESC
    """)
    fun searchTransactions(startDate: Long, endDate: Long, query: String): Flow<List<Transaction>>

    // Hitung Total (Tetap hitung total bulan ini meski sedang di-search, atau mau ikut terfilter? 
    // Biasanya Dashboard ringkasan tetap menunjukkan total sebulan penuh, search hanya memfilter list.
    // Jadi query total ini tidak berubah).
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1 AND timestamp BETWEEN :startDate AND :endDate")
    fun getIncomeTotal(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0 AND timestamp BETWEEN :startDate AND :endDate")
    fun getExpenseTotal(startDate: Long, endDate: Long): Flow<Double?>

    // --- CRUD ---
    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    // --- KATEGORI ---
    @Query("SELECT * FROM categories WHERE isIncome = :isIncome ORDER BY name ASC")
    fun getCategories(isIncome: Boolean): Flow<List<Category>>

    @Insert
    suspend fun insertCategory(category: Category)
    
    @Delete
    suspend fun deleteCategory(category: Category)
}
