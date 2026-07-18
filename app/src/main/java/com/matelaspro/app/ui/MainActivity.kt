package com.matelaspro.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.databinding.ActivityMainBinding
import com.matelaspro.app.service.SessionManager
import com.matelaspro.app.ui.alu.AluMenuActivity
import com.matelaspro.app.ui.auth.LoginActivity
import com.matelaspro.app.ui.auth.UserManagementActivity
import com.matelaspro.app.ui.dashboard.DashboardActivity
import com.matelaspro.app.ui.expense.ExpenseDashboardActivity
import com.matelaspro.app.ui.product.FournisseurListActivity
import com.matelaspro.app.ui.product.ProductActivity
import com.matelaspro.app.ui.stock.AdminStockMovementActivity
import com.matelaspro.app.ui.stock.UserStockActivity
import com.matelaspro.app.ui.profit.ProfitDashboardActivity
import com.matelaspro.app.ui.sale.SaleActivity
import com.matelaspro.app.ui.sale.SalesDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var app: MatelasProApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app = application as MatelasProApp

        if (SessionManager.isAdmin) {
            showSoldeInitialIfNeeded()
            binding.cardStock.setOnClickListener {
                startActivity(Intent(this, ProductActivity::class.java))
            }
            binding.cardSales.setOnClickListener {
                startActivity(Intent(this, SaleActivity::class.java))
            }
            binding.cardVentes.visibility = View.VISIBLE
            binding.cardVentes.setOnClickListener {
                showUserSelectionDialog { userId ->
                    val intent = Intent(this, SalesDashboardActivity::class.java)
                    intent.putExtra("selectedUserId", userId)
                    startActivity(intent)
                }
            }
            binding.cardDashboard.setOnClickListener {
                startActivity(Intent(this, DashboardActivity::class.java))
            }
            binding.cardFournisseurs.setOnClickListener {
                startActivity(Intent(this, FournisseurListActivity::class.java))
            }
            binding.cardAlu.setOnClickListener {
                startActivity(Intent(this, AluMenuActivity::class.java))
            }
            binding.cardUsers.visibility = View.VISIBLE
            binding.cardUsers.setOnClickListener {
                startActivity(Intent(this, UserManagementActivity::class.java))
            }
            binding.cardExpenses.visibility = View.VISIBLE
            binding.cardExpenses.setOnClickListener {
                showUserSelectionDialog { userId ->
                    val intent = Intent(this, ExpenseDashboardActivity::class.java)
                    intent.putExtra("selectedUserId", userId)
                    startActivity(intent)
                }
            }
            binding.cardProfit.visibility = View.VISIBLE
            binding.cardProfit.setOnClickListener {
                showUserSelectionDialog { userId ->
                    val intent = Intent(this, ProfitDashboardActivity::class.java)
                    intent.putExtra("selectedUserId", userId)
                    startActivity(intent)
                }
            }
            binding.cardDeplacement.visibility = View.VISIBLE
            binding.cardDeplacement.setOnClickListener {
                startActivity(Intent(this, AdminStockMovementActivity::class.java))
            }
        } else {
            binding.cardStock.visibility = View.GONE
            binding.cardFournisseurs.visibility = View.GONE
            binding.cardAlu.visibility = View.GONE
            binding.cardUsers.visibility = View.GONE
            binding.cardExpenses.visibility = View.VISIBLE
            binding.cardProfit.visibility = View.VISIBLE
            binding.cardVentes.visibility = View.VISIBLE
            binding.cardUserStock.visibility = View.VISIBLE

            binding.cardSales.setOnClickListener {
                startActivity(Intent(this, SaleActivity::class.java))
            }
            binding.cardVentes.setOnClickListener {
                startActivity(Intent(this, SalesDashboardActivity::class.java))
            }
            binding.cardDashboard.setOnClickListener {
                startActivity(Intent(this, DashboardActivity::class.java))
            }
            binding.cardExpenses.setOnClickListener {
                startActivity(Intent(this, ExpenseDashboardActivity::class.java))
            }
            binding.cardProfit.setOnClickListener {
                startActivity(Intent(this, ProfitDashboardActivity::class.java))
            }
            binding.cardUserStock.setOnClickListener {
                startActivity(Intent(this, UserStockActivity::class.java))
            }
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    SessionManager.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Non", null)
                .show()
        }

        binding.btnDarkMode.setOnClickListener {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val current = prefs.getBoolean("dark_mode", false)
            val editor = prefs.edit()
            if (current) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor.putBoolean("dark_mode", false)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor.putBoolean("dark_mode", true)
            }
            editor.apply()
        }

        binding.cardAbout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("À propos")
                .setMessage("Version : 1.0\n\n" +
                        "Application de gestion d'entreprise destinée aux matelassiers.\n" +
                        "Gestion de stock, ventes, dépenses, fournisseurs et ALU.\n\n" +
                        "Copyright 2026, by Jecolia SR")
                .setPositiveButton("OK", null)
                .show()
        }

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun showUserSelectionDialog(onUserSelected: (String) -> Unit) {
        lifecycleScope.launch {
            val users = app.firestoreService.getAllUsers()
            val names = mutableListOf("Tout")
            names.addAll(users.map { it.name })
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Choisir un utilisateur")
                .setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, names)) { _, which ->
                    if (which == 0) onUserSelected("")
                    else onUserSelected(users[which - 1].id)
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    private fun showSoldeInitialIfNeeded() {
        val prefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("solde_initial")) {
            val input = android.widget.EditText(this).apply {
                hint = "Montant du solde initial"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            android.app.AlertDialog.Builder(this)
                .setTitle("Solde initial")
                .setMessage("Entrez le solde initial de votre caisse :")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Enregistrer") { _, _ ->
                    val value = input.text.toString().toDoubleOrNull() ?: 0.0
                    prefs.edit().putFloat("solde_initial", value.toFloat()).apply()
                }
                .show()
        }
    }
}
