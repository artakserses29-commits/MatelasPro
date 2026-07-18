package com.matelaspro.app.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.ui.MainActivity
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var app: MatelasProApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as MatelasProApp

        binding.btnLogin.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre nom", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre mot de passe", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val user = app.userRepository.getUserByName(username)
            if (user == null) {
                Toast.makeText(this@LoginActivity, "Utilisateur introuvable", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (password != user.password) {
                Toast.makeText(this@LoginActivity, "Mot de passe incorrect", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val prefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("currentUserId", user.id)
                .putBoolean("isAdmin", user.isAdmin)
                .putString("userName", user.name)
                .apply()

            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }
}
