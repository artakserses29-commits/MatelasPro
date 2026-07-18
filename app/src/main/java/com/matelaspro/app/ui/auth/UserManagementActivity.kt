package com.matelaspro.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.firestore.UserFS
import com.matelaspro.app.databinding.ActivityUserManagementBinding
import com.matelaspro.app.service.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var app: MatelasProApp
    private var userList: List<UserFS> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as MatelasProApp

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAddUser.setOnClickListener { showAddUserDialog() }
        showSkeleton()
        loadUsers()
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            userList = app.firestoreService.getAllUsers()
            binding.recyclerView.layoutManager = LinearLayoutManager(this@UserManagementActivity)
            binding.recyclerView.adapter = UserCardAdapter(userList,
                onEdit = { user -> showEditUserDialog(user) },
                onDelete = { user -> confirmDeleteUser(user) }
            )
            hideSkeleton()
        }
    }

    private fun showSkeleton() {
        binding.skeleton.root.apply {
            visibility = android.view.View.VISIBLE
            startShimmer()
        }
    }

    private fun hideSkeleton() {
        binding.skeleton.root.apply {
            stopShimmer()
            visibility = android.view.View.GONE
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val etEmail = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)
        val etPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val cbAdmin = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.cbAdmin)
        val layoutEmail = etEmail.parent as? com.google.android.material.textfield.TextInputLayout
        val layoutPassword = etPassword.parent as? com.google.android.material.textfield.TextInputLayout

        AlertDialog.Builder(this)
            .setTitle("Ajouter un utilisateur")
            .setView(dialogView)
            .setPositiveButton("Ajouter", null)
            .setNegativeButton("Annuler", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        layoutEmail?.error = null
                        layoutPassword?.error = null
                        val email = etEmail.text.toString().trim()
                        val password = etPassword.text.toString().trim()
                        if (email.isEmpty()) {
                            layoutEmail?.error = "L'email est requis"
                            etEmail.requestFocus()
                            return@setOnClickListener
                        }
                        if (password.isEmpty()) {
                            layoutPassword?.error = "Le mot de passe est requis"
                            etPassword.requestFocus()
                            return@setOnClickListener
                        }
                        if (password.length < 6) {
                            layoutPassword?.error = "Minimum 6 caractères"
                            etPassword.requestFocus()
                            return@setOnClickListener
                        }
                        lifecycleScope.launch {
                            try {
                                val result = FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
                                val uid = result.user?.uid ?: return@launch
                                val name = email.substringBefore("@")
                                result.user?.updateProfile(userProfileChangeRequest { displayName = name })?.await()
                                app.firestoreService.setUser(uid, UserFS(
                                    id = uid, name = name, email = email, password = password, isAdmin = cbAdmin.isChecked
                                ))
                                loadUsers()
                                Toast.makeText(this@UserManagementActivity, "Utilisateur '$name' créé", Toast.LENGTH_SHORT).show()
                                dismiss()
                            } catch (e: Exception) {
                                layoutEmail?.error = "Email déjà utilisé ou invalide"
                            }
                        }
                    }
                }
                show()
            }
    }

    private fun showEditUserDialog(user: UserFS) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val etEmail = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)
        val etPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val cbAdmin = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.cbAdmin)
        val layoutPassword = etPassword.parent as? com.google.android.material.textfield.TextInputLayout

        etEmail.setText(user.email.ifEmpty { user.name + "@matelaspro.com" })
        etEmail.isEnabled = false
        etPassword.setText(user.password)
        cbAdmin.isChecked = user.isAdmin

        AlertDialog.Builder(this)
            .setTitle("Modifier l'utilisateur")
            .setView(dialogView)
            .setPositiveButton("Enregistrer", null)
            .setNegativeButton("Annuler", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        layoutPassword?.error = null
                        val password = etPassword.text.toString().trim()
                        if (password.isEmpty()) {
                            layoutPassword?.error = "Le mot de passe est requis"
                            etPassword.requestFocus()
                            return@setOnClickListener
                        }
                        lifecycleScope.launch {
                            try {
                                app.firestoreService.setUser(user.id, user.copy(password = password, isAdmin = cbAdmin.isChecked))
                                loadUsers()
                                Toast.makeText(this@UserManagementActivity, "Utilisateur modifié", Toast.LENGTH_SHORT).show()
                                dismiss()
                            } catch (e: Exception) {
                                layoutPassword?.error = "Erreur lors de l'enregistrement"
                            }
                        }
                    }
                }
                show()
            }
    }

    private fun confirmDeleteUser(user: UserFS) {
        if (user.isAdmin) {
            Toast.makeText(this, "Impossible de supprimer l'administrateur", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Supprimer l'utilisateur")
            .setMessage("Voulez-vous vraiment supprimer ${user.name} ?")
            .setPositiveButton("Oui") { _, _ ->
                lifecycleScope.launch {
                    app.firestoreService.deleteUser(user.id)
                    loadUsers()
                    Toast.makeText(this@UserManagementActivity, "Utilisateur supprimé", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private class UserCardAdapter(
        private val users: List<UserFS>,
        private val onEdit: (UserFS) -> Unit,
        private val onDelete: (UserFS) -> Unit
    ) : RecyclerView.Adapter<UserCardAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: android.widget.TextView = view.findViewById(R.id.tvUserName)
            val tvRole: android.widget.TextView = view.findViewById(R.id.tvUserRole)
            val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
            val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.tvName.text = user.name
            holder.tvRole.text = if (user.isAdmin) "Administrateur" else "Utilisateur"
            holder.btnEdit.setOnClickListener { onEdit(user) }
            holder.btnDelete.setOnClickListener { onDelete(user) }
        }

        override fun getItemCount() = users.size
    }
}
