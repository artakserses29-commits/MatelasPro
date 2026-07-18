package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alu_note_expenses",
    foreignKeys = [ForeignKey(
        entity = AluNote::class,
        parentColumns = ["id"],
        childColumns = ["noteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("noteId")]
)
data class AluNoteExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val description: String,
    val montant: Double,
    val createdAt: Long = System.currentTimeMillis()
)
