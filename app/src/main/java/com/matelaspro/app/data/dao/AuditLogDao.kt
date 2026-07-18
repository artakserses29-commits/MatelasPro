package com.matelaspro.app.data.dao

import androidx.room.*
import com.matelaspro.app.data.entity.AuditLog

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY createdAt DESC LIMIT 100")
    suspend fun getAllSync(): List<AuditLog>

    @Insert
    suspend fun insert(log: AuditLog)

    @Query("DELETE FROM audit_log WHERE createdAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
