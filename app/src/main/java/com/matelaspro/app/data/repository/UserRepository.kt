package com.matelaspro.app.data.repository

import androidx.lifecycle.LiveData
import com.matelaspro.app.data.dao.UserDao
import com.matelaspro.app.data.entity.User

class UserRepository(private val userDao: UserDao) {
    val allUsers: LiveData<List<User>> = userDao.getAllUsers()
    suspend fun getAllUsersList(): List<User> = userDao.getAllUsersList()

    suspend fun getUserById(id: Long): User? = userDao.getUserById(id)
    suspend fun getUserByName(name: String): User? = userDao.getUserByName(name)
    suspend fun insert(user: User): Long = userDao.insert(user)
    suspend fun deleteById(id: Long) = userDao.deleteById(id)
    suspend fun adminCount(): Int = userDao.adminCount()
}
