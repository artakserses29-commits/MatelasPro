package com.matelaspro.app.ui.expense

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.firestore.ExpenseFS
import com.matelaspro.app.data.firestore.UserFS
import com.matelaspro.app.databinding.ActivityUserExpenseListBinding
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.launch

class UserExpenseListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserExpenseListBinding
    private lateinit var app: MatelasProApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as MatelasProApp

        binding.btnBack.setOnClickListener { finish() }
        loadUsers()
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val users = app.firestoreService.getAllUsers()
            val allExpenses = app.firestoreService.getAllExpenses()
            val byUser = allExpenses.groupBy { it.userId }

            val items = users.filter { byUser.containsKey(it.id) }
            binding.recyclerView.layoutManager = LinearLayoutManager(this@UserExpenseListActivity)
            binding.recyclerView.adapter = UserExpenseAdapter(items, byUser) { user ->
                val intent = Intent(this@UserExpenseListActivity, UserExpenseDetailActivity::class.java)
                intent.putExtra("userId", user.id)
                intent.putExtra("userName", user.name)
                startActivity(intent)
            }
        }
    }

    private class UserExpenseAdapter(
        private val users: List<UserFS>,
        private val byUser: Map<String, List<ExpenseFS>>,
        private val onClick: (UserFS) -> Unit
    ) : RecyclerView.Adapter<UserExpenseAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.cardUserExpense)
            val tvName: TextView = view.findViewById(R.id.tvUserName)
            val tvTotal: TextView = view.findViewById(R.id.tvUserTotal)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_expense_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            val expenses = byUser[user.id] ?: emptyList()
            val total = expenses.sumOf { it.amount }
            holder.tvName.text = user.name
            holder.tvTotal.text = FormatUtil.montant(total)
            holder.card.setOnClickListener { onClick(user) }
        }

        override fun getItemCount() = users.size
    }
}
