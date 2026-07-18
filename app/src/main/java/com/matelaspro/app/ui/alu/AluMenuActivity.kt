package com.matelaspro.app.ui.alu

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.matelaspro.app.databinding.ActivityAluMenuBinding

class AluMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        binding.cardListesProduits.setOnClickListener {
            startActivity(Intent(this, AluProductListActivity::class.java))
        }

        binding.cardNote.setOnClickListener {
            startActivity(Intent(this, AluNoteListActivity::class.java))
        }

        binding.cardDevis.setOnClickListener {
            startActivity(Intent(this, AluDevisActivity::class.java))
        }

        binding.cardDevisEnregistres.setOnClickListener {
            startActivity(Intent(this, AluDevisListActivity::class.java))
        }
    }
}
