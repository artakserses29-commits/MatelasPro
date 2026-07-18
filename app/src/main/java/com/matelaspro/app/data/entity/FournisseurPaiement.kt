package com.matelaspro.app.data.entity

import androidx.room.*

@Entity(
    tableName = "fournisseur_paiements",
    foreignKeys = [ForeignKey(
        entity = Fournisseur::class,
        parentColumns = ["id"],
        childColumns = ["fournisseurId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fournisseurId")]
)
data class FournisseurPaiement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fournisseurId: Long,
    val montant: Double,
    val createdAt: Long = System.currentTimeMillis()
)
