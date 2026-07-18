package com.matelaspro.app.ui.alu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.matelaspro.app.databinding.ActivityAluNoteBinding

class AluNoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluNoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
    }
}
