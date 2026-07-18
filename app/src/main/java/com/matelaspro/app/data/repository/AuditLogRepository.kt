package com.matelaspro.app.data.repository

import androidx.lifecycle.LiveData
import com.matelaspro.app.data.dao.AuditLogDao
import com.matelaspro.app.data.entity.AuditLog

class AuditLogRepository(private val dao: AuditLogDao) {
    val allLogs: LiveData<List<AuditLog>> = dao.getAll()

    suspend fun insert(action: String, tableName: String, recordId: Long, detail: String = "") {
        dao.insert(AuditLog(action = action, tableName = tableName, recordId = recordId, detail = detail))
    }
}
