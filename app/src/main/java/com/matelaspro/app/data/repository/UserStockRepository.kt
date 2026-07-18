package com.matelaspro.app.data.repository

import com.matelaspro.app.data.dao.UserStockDao
import com.matelaspro.app.data.entity.UserStock

class UserStockRepository(private val userStockDao: UserStockDao) {
    suspend fun getByUserId(userId: Long): List<UserStock> = userStockDao.getByUserId(userId)
    suspend fun getByUserAndProduct(userId: Long, productId: Long): UserStock? = userStockDao.getByUserAndProduct(userId, productId)
    suspend fun getQuantity(userId: Long, productId: Long): Int = userStockDao.getQuantity(userId, productId)
    suspend fun upsert(userStock: UserStock): Long = userStockDao.upsert(userStock)
    suspend fun addQuantity(userId: Long, productId: Long, delta: Int, now: Long = System.currentTimeMillis()) =
        userStockDao.addQuantity(userId, productId, delta, now)
    suspend fun deleteByUserAndProduct(userId: Long, productId: Long) = userStockDao.deleteByUserAndProduct(userId, productId)
    suspend fun getAll(): List<UserStock> = userStockDao.getAll()
}
