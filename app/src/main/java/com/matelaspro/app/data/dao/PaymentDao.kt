package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.Payment

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): LiveData<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentById(id: Long): Payment?

    @Query("SELECT * FROM payments WHERE type = :type ORDER BY date DESC")
    fun getPaymentsByType(type: String): LiveData<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment): Long

    @Update
    suspend fun update(payment: Payment)

    @Delete
    suspend fun delete(payment: Payment)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE type = 'VERSEMENT'")
    fun getTotalVersements(): LiveData<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE type = 'DEPENSE'")
    fun getTotalDepenses(): LiveData<Double>
}
