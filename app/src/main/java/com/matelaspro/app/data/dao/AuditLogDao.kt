package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.AuditLog

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY createdAt DESC LIMIT 100")
    fun getAll(): LiveData<List<AuditLog>>

    @Insert
    suspend fun insert(log: AuditLog)
}
