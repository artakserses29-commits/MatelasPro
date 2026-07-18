package com.matelaspro.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.data.firestore.UserFS
import com.matelaspro.app.databinding.ActivityLoginBinding
import com.matelaspro.app.service.SessionManager
import com.matelaspro.app.ui.MainActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var app: MatelasProApp
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as MatelasProApp
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnLogin.setOnClickListener { login() }
    }

    private fun navigateToMain() {
        val user = auth.currentUser ?: run {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        lifecycleScope.launch {
            try {
                val userDoc = app.firestoreService.getUserById(user.uid)
                SessionManager.login(user.uid, userDoc?.name ?: user.displayName ?: "", userDoc?.isAdmin ?: false)
            } catch (_: CancellationException) {
                return@launch
            } catch (_: Exception) {
                SessionManager.login(user.uid, user.displayName ?: "", false)
            }
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun login() {
        val email = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.etUsername.error = "Veuillez saisir votre email"
            binding.etUsername.requestFocus()
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Veuillez saisir votre mot de passe"
            binding.etPassword.requestFocus()
            return
        }

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Connexion..."

        lifecycleScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: return@launch

                var userDoc = app.firestoreService.getUserById(uid)
                if (userDoc == null) {
                    val isFirst = app.firestoreService.adminCount() == 0
                    val name = email.substringBefore("@")
                    app.firestoreService.setUser(uid, UserFS(
                        id = uid,
                        name = name,
                        email = email,
                        password = password,
                        isAdmin = isFirst
                    ))
                    result.user?.updateProfile(userProfileChangeRequest { displayName = name })?.await()
                    userDoc = UserFS(id = uid, name = name, email = email, isAdmin = isFirst)
                }

                SessionManager.login(uid, userDoc.name, userDoc.isAdmin)
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } catch (e: CancellationException) {
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                binding.etUsername.error = "Email incorrect"
                binding.etUsername.requestFocus()
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                binding.etPassword.error = "Mot de passe incorrect"
                binding.etPassword.requestFocus()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Erreur : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Se connecter"
            }
        }
    }
}
