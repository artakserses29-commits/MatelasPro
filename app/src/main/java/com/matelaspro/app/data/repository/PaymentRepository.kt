package com.matelaspro.app.data.repository

import androidx.lifecycle.LiveData
import com.matelaspro.app.data.dao.PaymentDao
import com.matelaspro.app.data.entity.Payment

class PaymentRepository(private val paymentDao: PaymentDao) {
    val allPayments: LiveData<List<Payment>> = paymentDao.getAllPayments()

    suspend fun getPaymentById(id: Long): Payment? = paymentDao.getPaymentById(id)

    fun getPaymentsByType(type: String): LiveData<List<Payment>> = paymentDao.getPaymentsByType(type)

    suspend fun insert(payment: Payment): Long = paymentDao.insert(payment)

    suspend fun update(payment: Payment) = paymentDao.update(payment)

    suspend fun delete(payment: Payment) = paymentDao.delete(payment)

    val totalVersements: LiveData<Double> = paymentDao.getTotalVersements()
    val totalDepenses: LiveData<Double> = paymentDao.getTotalDepenses()
}
