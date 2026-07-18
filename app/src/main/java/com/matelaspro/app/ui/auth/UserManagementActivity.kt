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
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.entity.User
import com.matelaspro.app.databinding.ActivityUserManagementBinding
import kotlinx.coroutines.launch

class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var app: MatelasProApp
    private var userList: List<User> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as MatelasProApp

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAddUser.setOnClickListener { showAddUserDialog() }
        loadUsers()
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            userList = app.userRepository.getAllUsersList()
            binding.recyclerView.layoutManager = LinearLayoutManager(this@UserManagementActivity)
            binding.recyclerView.adapter = UserCardAdapter(userList,
                onEdit = { user -> showEditUserDialog(user) },
                onDelete = { user -> confirmDeleteUser(user) }
            )
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
        val etPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val cbAdmin = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.cbAdmin)

        AlertDialog.Builder(this)
            .setTitle("Ajouter un utilisateur")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val name = etName.text.toString().trim()
                val password = etPassword.text.toString().trim()
                if (name.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    if (app.userRepository.getUserByName(name) != null) {
                        Toast.makeText(this@UserManagementActivity, "Cet utilisateur existe déjà", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    app.userRepository.insert(User(name = name, password = password, isAdmin = cbAdmin.isChecked))
                    loadUsers()
                    Toast.makeText(this@UserManagementActivity, "Utilisateur ajouté", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showEditUserDialog(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
        val etPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val cbAdmin = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.cbAdmin)

        etName.setText(user.name)
        etPassword.setText(user.password)
        cbAdmin.isChecked = user.isAdmin

        AlertDialog.Builder(this)
            .setTitle("Modifier l'utilisateur")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { _, _ ->
                val name = etName.text.toString().trim()
                val password = etPassword.text.toString().trim()
                if (name.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val existing = app.userRepository.getUserByName(name)
                    if (existing != null && existing.id != user.id) {
                        Toast.makeText(this@UserManagementActivity, "Ce nom est déjà pris", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    app.userRepository.insert(user.copy(name = name, password = password, isAdmin = cbAdmin.isChecked))
                    loadUsers()
                    Toast.makeText(this@UserManagementActivity, "Utilisateur modifié", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun confirmDeleteUser(user: User) {
        if (user.isAdmin) {
            Toast.makeText(this, "Impossible de supprimer l'administrateur", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Supprimer l'utilisateur")
            .setMessage("Voulez-vous vraiment supprimer ${user.name} ?")
            .setPositiveButton("Oui") { _, _ ->
                lifecycleScope.launch {
                    app.userRepository.deleteById(user.id)
                    loadUsers()
                    Toast.makeText(this@UserManagementActivity, "Utilisateur supprimé", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private class UserCardAdapter(
        private val users: List<User>,
        private val onEdit: (User) -> Unit,
        private val onDelete: (User) -> Unit
    ) : RecyclerView.Adapter<UserCardAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: android.widget.TextView = view.findViewById(R.id.tvUserName)
            val tvRole: android.widget.TextView = view.findViewById(R.id.tvUserRole)
            val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
            val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_card, parent, false)
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
