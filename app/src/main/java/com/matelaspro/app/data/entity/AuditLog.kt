package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val tableName: String,
    val recordId: Long,
    val detail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
