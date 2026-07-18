package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alu_notes")
data class AluNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientName: String,
    val description: String,
    val montantTotal: Double,
    val montantPaye: Double = 0.0,
    val resteAPaye: Double = montantTotal,
    val createdAt: Long = System.currentTimeMillis()
)
