package com.matelaspro.app.data.repository

import com.matelaspro.app.data.dao.AuditLogDao
import com.matelaspro.app.data.entity.AuditLog

class AuditLogRepository(private val dao: AuditLogDao) {
    suspend fun getAllSync(): List<AuditLog> = dao.getAllSync()

    suspend fun deleteOlderThan(before: Long) = dao.deleteOlderThan(before)

    suspend fun insert(action: String, tableName: String, recordId: Long, detail: String = "") {
        dao.insert(AuditLog(action = action, tableName = tableName, recordId = recordId, detail = detail))
        cleanupOldLogs()
    }

    private suspend fun cleanupOldLogs() {
        val sixMonthsAgo = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000
        dao.deleteOlderThan(sixMonthsAgo)
    }

    suspend fun cleanupOnStartup() {
        val sixMonthsAgo = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000
        dao.deleteOlderThan(sixMonthsAgo)
    }
}
