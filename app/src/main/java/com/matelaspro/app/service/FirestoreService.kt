package com.matelaspro.app.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.matelaspro.app.data.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    private val usersRef = db.collection("users")
    private val productsRef = db.collection("products")
    private val salesRef = db.collection("sales")
    private val paymentsRef = db.collection("payments")
    private val expensesRef = db.collection("expenses")
    private val userStockRef = db.collection("user_stock")
    private val fournisseursRef = db.collection("fournisseurs")
    private val fournisseurPaiementsRef = db.collection("fournisseur_paiements")
    private val creditEntriesRef = db.collection("credit_entries")
    private val auditLogRef = db.collection("audit_log")
    private val aluProductsRef = db.collection("alu_products")
    private val aluDevisRef = db.collection("alu_devis")
    private val aluNotesRef = db.collection("alu_notes")
    private val aluNotePaymentsRef = db.collection("alu_note_payments")
    private val aluNoteExpensesRef = db.collection("alu_note_expenses")

    // ── Helpers ──────────────────────────────────────────────
    private fun <T> getFlow(ref: Query, transform: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
        val reg = ref.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) trySend(snap.documents.mapNotNull { d -> d?.let { transform(it) } })
        }
        awaitClose { reg.remove() }
    }

    private suspend fun <T> getList(ref: Query, transform: (DocumentSnapshot) -> T): List<T> =
        ref.get().await().documents.mapNotNull { d -> d?.let { transform(it) } }

    private fun <T> allFlow(ref: CollectionReference, orderByField: String, direction: Query.Direction, transform: (DocumentSnapshot) -> T): Flow<List<T>> =
        getFlow(ref.orderBy(orderByField, direction), transform)

    private suspend fun <T> allList(ref: CollectionReference, orderByField: String, direction: Query.Direction, transform: (DocumentSnapshot) -> T): List<T> =
        getList(ref.orderBy(orderByField, direction), transform)

    private suspend fun set(ref: CollectionReference, id: String?, data: MutableMap<String, Any?>): String {
        return if (id.isNullOrEmpty()) ref.add(data).await().id
        else { ref.document(id).set(data).await(); id }
    }

    private suspend fun del(ref: CollectionReference, id: String) {
        ref.document(id).delete().await()
    }

    // ── Users ────────────────────────────────────────────────
    suspend fun getUserById(id: String): UserFS? {
        val snap = usersRef.document(id).get().await()
        return if (snap.exists()) UserFS.fromMap(id, snap.data!!) else null
    }

    suspend fun getAllUsers(): List<UserFS> = allList(usersRef, "name", Query.Direction.ASCENDING) {
        UserFS.fromMap(it.id, it.data ?: emptyMap())
    }

    suspend fun getUserByName(name: String): UserFS? {
        val snap = usersRef.whereEqualTo("name", name).get().await()
        return if (snap.documents.isNotEmpty()) {
            val d = snap.documents[0]
            UserFS.fromMap(d.id, d.data ?: emptyMap())
        } else null
    }

    suspend fun setUser(id: String?, data: UserFS): String = set(usersRef, id, data.toMap())
    suspend fun deleteUser(id: String) = del(usersRef, id)

    suspend fun adminCount(): Int =
        getAllUsers().count { it.isAdmin }

    // ── Products ─────────────────────────────────────────────
    fun productsFlow(): Flow<List<ProductFS>> =
        allFlow(productsRef, "createdAt", Query.Direction.DESCENDING) {
            ProductFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getAllProducts(): List<ProductFS> =
        allList(productsRef, "createdAt", Query.Direction.DESCENDING) {
            ProductFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getProductById(id: String): ProductFS? {
        val snap = productsRef.document(id).get().await()
        return if (snap.exists()) ProductFS.fromMap(id, snap.data!!) else null
    }

    fun getProductsByCategory(category: String): Flow<List<ProductFS>> = callbackFlow {
        val reg = productsRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val filtered = snap.documents
                    .map { ProductFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.category == category }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun getAllCategories(): List<String> =
        getAllProducts().map { it.category }.filter { it.isNotEmpty() }.distinct().sorted()

    fun categoriesFlow(): Flow<List<String>> = callbackFlow {
        val reg = productsRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val cats = snap.documents.mapNotNull { it.getString("category") }.filter { it.isNotEmpty() }.distinct().sorted()
                trySend(cats)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun setProduct(id: String?, data: ProductFS): String = set(productsRef, id, data.toMap())
    suspend fun deleteProduct(id: String) = del(productsRef, id)

    fun searchProductsFlow(query: String): Flow<List<ProductFS>> = callbackFlow {
        val reg = productsRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val q = query.lowercase()
                val filtered = snap.documents.map { ProductFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.name.lowercase().contains(q) || it.description.lowercase().contains(q) }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun getLowStockProducts(threshold: Int = 5): List<ProductFS> =
        getAllProducts().filter { it.quantity <= threshold }.sortedBy { it.quantity }

    fun lowStockFlow(threshold: Int = 5): Flow<List<ProductFS>> = callbackFlow {
        val reg = productsRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val filtered = snap.documents.map { ProductFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.quantity <= threshold }.sortedBy { it.quantity }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun getProductsByFournisseurAndCategory(fournisseur: String, category: String): List<ProductFS> =
        getAllProducts().filter { it.fournisseur == fournisseur && it.category == category }

    // ── Sales ────────────────────────────────────────────────
    fun allSalesFlow(): Flow<List<SaleFS>> =
        allFlow(salesRef, "saleDate", Query.Direction.DESCENDING) {
            SaleFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getAllSales(): List<SaleFS> =
        allList(salesRef, "saleDate", Query.Direction.DESCENDING) {
            SaleFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getSaleById(id: String): SaleFS? {
        val snap = salesRef.document(id).get().await()
        return if (snap.exists()) SaleFS.fromMap(id, snap.data!!) else null
    }

    fun getSalesByProductId(productId: String): Flow<List<SaleFS>> = callbackFlow {
        val reg = salesRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val filtered = snap.documents
                    .map { SaleFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.productId == productId }
                    .sortedByDescending { it.saleDate?.toDate()?.time ?: 0L }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun setSale(id: String?, data: SaleFS): String = set(salesRef, id, data.toMap())
    suspend fun deleteSale(id: String) = del(salesRef, id)

    suspend fun getUserSalesTotalInRange(userId: String, start: Long, end: Long): Double =
        getAllSales().filter { it.userId == userId && it.saleDate?.toDate()?.time in start until end }.sumOf { it.totalAmount }

    suspend fun getUserProfitInRange(userId: String, start: Long, end: Long): Double =
        getAllSales().filter { it.userId == userId && it.saleDate?.toDate()?.time in start until end }.sumOf { it.profit }

    suspend fun getAllSalesTotalInRange(start: Long, end: Long): Double =
        getAllSales().filter { it.saleDate?.toDate()?.time in start until end }.sumOf { it.totalAmount }

    suspend fun getAllProfitInRange(start: Long, end: Long): Double =
        getAllSales().filter { it.saleDate?.toDate()?.time in start until end }.sumOf { it.profit }

    suspend fun getTotalSalesAmount(): Double = getAllSales().sumOf { it.totalAmount }
    suspend fun getTotalProfit(): Double = getAllSales().sumOf { it.profit }
    suspend fun getTotalQuantitySold(): Int = getAllSales().sumOf { it.quantity }

    fun getSalesInRangeFlow(start: Long, end: Long): Flow<List<SaleFS>> = callbackFlow {
        val reg = salesRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val filtered = snap.documents
                    .map { SaleFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.saleDate?.toDate()?.time in start until end }
                    .sortedByDescending { it.saleDate?.toDate()?.time ?: 0L }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    fun getUserSalesInRangeFlow(userId: String, start: Long, end: Long): Flow<List<SaleFS>> = callbackFlow {
        val reg = salesRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val filtered = snap.documents
                    .map { SaleFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.userId == userId && it.saleDate?.toDate()?.time in start until end }
                    .sortedByDescending { it.saleDate?.toDate()?.time ?: 0L }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    // ── Payments ─────────────────────────────────────────────
    fun allPaymentsFlow(): Flow<List<PaymentFS>> =
        allFlow(paymentsRef, "date", Query.Direction.DESCENDING) {
            PaymentFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getAllPayments(): List<PaymentFS> =
        allList(paymentsRef, "date", Query.Direction.DESCENDING) {
            PaymentFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getPaymentById(id: String): PaymentFS? {
        val snap = paymentsRef.document(id).get().await()
        return if (snap.exists()) PaymentFS.fromMap(id, snap.data!!) else null
    }

    fun getPaymentsByTypeFlow(type: String): Flow<List<PaymentFS>> = callbackFlow {
        val reg = paymentsRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val filtered = snap.documents
                    .map { PaymentFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.type == type }
                    .sortedByDescending { it.date?.toDate()?.time ?: 0L }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun setPayment(id: String?, data: PaymentFS): String = set(paymentsRef, id, data.toMap())
    suspend fun deletePayment(id: String) = del(paymentsRef, id)

    suspend fun getTotalVersements(): Double =
        getAllPayments().filter { it.type == "VERSEMENT" }.sumOf { it.amount }

    suspend fun getTotalDepenses(): Double =
        getAllPayments().filter { it.type == "DEPENSE" }.sumOf { it.amount }

    // ── Expenses ─────────────────────────────────────────────
    fun allExpensesFlow(): Flow<List<ExpenseFS>> =
        allFlow(expensesRef, "createdAt", Query.Direction.DESCENDING) {
            ExpenseFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getAllExpenses(): List<ExpenseFS> =
        allList(expensesRef, "createdAt", Query.Direction.DESCENDING) {
            ExpenseFS.fromMap(it.id, it.data ?: emptyMap())
        }

    fun getExpensesByUserFlow(userId: String): Flow<List<ExpenseFS>> = callbackFlow {
        val reg = expensesRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val filtered = snap.documents
                    .map { ExpenseFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.userId == userId }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun getExpensesByUserList(userId: String): List<ExpenseFS> =
        getAllExpenses().filter { it.userId == userId }

    suspend fun setExpense(id: String?, data: ExpenseFS): String = set(expensesRef, id, data.toMap())
    suspend fun deleteExpense(id: String) = del(expensesRef, id)

    suspend fun getUserExpensesInRange(userId: String, start: Long, end: Long): Double =
        getAllExpenses().filter { it.userId == userId && it.createdAt?.toDate()?.time in start until end }.sumOf { it.amount }

    suspend fun getAllExpensesInRange(start: Long, end: Long): Double =
        getAllExpenses().filter { it.createdAt?.toDate()?.time in start until end }.sumOf { it.amount }

    // ── UserStock ────────────────────────────────────────────
    suspend fun getUserStockByUserId(userId: String): List<UserStockFS> =
        getAllUserStock().filter { it.userId == userId }

    suspend fun getAllUserStock(): List<UserStockFS> =
        getList(userStockRef) { UserStockFS.fromMap(it.id, it.data ?: emptyMap()) }

    suspend fun getUserStock(userId: String, productId: String): UserStockFS? =
        getAllUserStock().find { it.userId == userId && it.productId == productId }

    suspend fun getUserStockQuantity(userId: String, productId: String): Int =
        getUserStock(userId, productId)?.quantity ?: 0

    suspend fun setUserStock(id: String?, data: UserStockFS): String = set(userStockRef, id, data.toMap())

    suspend fun addUserStockQuantity(userId: String, productId: String, delta: Int) {
        val existing = getUserStock(userId, productId)
        if (existing != null) {
            setUserStock(existing.id, existing.copy(quantity = existing.quantity + delta, updatedAt = Timestamp.now()))
        } else if (delta > 0) {
            setUserStock(null, UserStockFS(userId = userId, productId = productId, quantity = delta, updatedAt = Timestamp.now()))
        }
    }

    suspend fun deleteUserStock(userId: String, productId: String) {
        val existing = getUserStock(userId, productId)
        if (existing != null) del(userStockRef, existing.id)
    }

    suspend fun deleteAllUserStockByUser(userId: String) {
        for (item in getUserStockByUserId(userId)) del(userStockRef, item.id)
    }

    // ── Fournisseurs ─────────────────────────────────────────
    fun allFournisseursFlow(): Flow<List<FournisseurFS>> =
        allFlow(fournisseursRef, "name", Query.Direction.ASCENDING) {
            FournisseurFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getFournisseurById(id: String): FournisseurFS? {
        val snap = fournisseursRef.document(id).get().await()
        return if (snap.exists()) FournisseurFS.fromMap(id, snap.data!!) else null
    }

    suspend fun getFournisseurByName(name: String): FournisseurFS? {
        val snap = fournisseursRef.whereEqualTo("name", name).get().await()
        return if (snap.documents.isNotEmpty()) {
            val d = snap.documents[0]
            FournisseurFS.fromMap(d.id, d.data ?: emptyMap())
        } else null
    }

    suspend fun setFournisseur(id: String?, data: FournisseurFS): String = set(fournisseursRef, id, data.toMap())
    suspend fun deleteFournisseur(id: String) = del(fournisseursRef, id)

    // ── FournisseurPaiements ─────────────────────────────────
    suspend fun getAllPaiements(): List<FournisseurPaiementFS> =
        allList(fournisseurPaiementsRef, "createdAt", Query.Direction.DESCENDING) {
            FournisseurPaiementFS.fromMap(it.id, it.data ?: emptyMap())
        }

    fun getPaiementsByFournisseurFlow(fournisseurId: String): Flow<List<FournisseurPaiementFS>> = callbackFlow {
        val reg = fournisseurPaiementsRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val filtered = snap.documents
                    .map { FournisseurPaiementFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.fournisseurId == fournisseurId }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun getTotalPayeByFournisseur(fournisseurId: String): Double =
        getAllPaiements().filter { it.fournisseurId == fournisseurId }.sumOf { it.montant }

    suspend fun setPaiement(id: String?, data: FournisseurPaiementFS): String = set(fournisseurPaiementsRef, id, data.toMap())

    // ── CreditEntries ────────────────────────────────────────
    suspend fun getAllCreditEntries(): List<CreditEntryFS> =
        allList(creditEntriesRef, "createdAt", Query.Direction.DESCENDING) {
            CreditEntryFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getCreditEntriesByFournisseur(fournisseur: String): List<CreditEntryFS> =
        getAllCreditEntries().filter { it.fournisseur == fournisseur }

    suspend fun getActiveCreditEntriesByFournisseur(fournisseur: String): List<CreditEntryFS> =
        getCreditEntriesByFournisseur(fournisseur).filter { it.quantity > 0 && !it.isOverridden }

    suspend fun setCreditEntry(id: String?, data: CreditEntryFS): String = set(creditEntriesRef, id, data.toMap())
    suspend fun deleteCreditEntry(id: String) = del(creditEntriesRef, id)

    // ── AuditLog ─────────────────────────────────────────────
    suspend fun getAllAuditLogs(): List<AuditLogFS> =
        getList(auditLogRef.orderBy("createdAt", Query.Direction.DESCENDING).limit(100)) {
            AuditLogFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun setAuditLog(id: String?, data: AuditLogFS): String {
        val docId = set(auditLogRef, id, data.toMap())
        cleanupOldAuditLogs()
        return docId
    }

    suspend fun cleanupOldAuditLogs() {
        val sixMonthsAgo = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000
        val all = getList(auditLogRef) { AuditLogFS.fromMap(it.id, it.data ?: emptyMap()) }
        val old = all.filter { it.createdAt?.toDate()?.time ?: 0L < sixMonthsAgo }
        for (log in old) del(auditLogRef, log.id)
    }

    // ── ALU Products ─────────────────────────────────────────
    fun aluProductsFlow(): Flow<List<AluProductFS>> =
        allFlow(aluProductsRef, "name", Query.Direction.ASCENDING) {
            AluProductFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getAluProductById(id: String): AluProductFS? {
        val snap = aluProductsRef.document(id).get().await()
        return if (snap.exists()) AluProductFS.fromMap(id, snap.data!!) else null
    }

    suspend fun setAluProduct(id: String?, data: AluProductFS): String = set(aluProductsRef, id, data.toMap())
    suspend fun deleteAluProduct(id: String) = del(aluProductsRef, id)

    // ── ALU Devis ────────────────────────────────────────────
    fun allAluDevisFlow(): Flow<List<AluDevisFS>> =
        allFlow(aluDevisRef, "createdAt", Query.Direction.DESCENDING) {
            AluDevisFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getAluDevisById(id: String): AluDevisFS? {
        val snap = aluDevisRef.document(id).get().await()
        return if (snap.exists()) AluDevisFS.fromMap(id, snap.data!!) else null
    }

    suspend fun setAluDevis(id: String?, data: AluDevisFS): String = set(aluDevisRef, id, data.toMap())
    suspend fun deleteAluDevis(id: String) = del(aluDevisRef, id)

    // ── ALU Notes ────────────────────────────────────────────
    fun allAluNotesFlow(): Flow<List<AluNoteFS>> =
        allFlow(aluNotesRef, "createdAt", Query.Direction.DESCENDING) {
            AluNoteFS.fromMap(it.id, it.data ?: emptyMap())
        }

    suspend fun getAluNoteById(id: String): AluNoteFS? {
        val snap = aluNotesRef.document(id).get().await()
        return if (snap.exists()) AluNoteFS.fromMap(id, snap.data!!) else null
    }

    suspend fun setAluNote(id: String?, data: AluNoteFS): String = set(aluNotesRef, id, data.toMap())
    suspend fun deleteAluNote(id: String) = del(aluNotesRef, id)

    // ── ALU Note Payments ────────────────────────────────────
    fun getAluNotePaymentsFlow(noteId: String): Flow<List<AluNotePaymentFS>> = callbackFlow {
        val reg = aluNotePaymentsRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val filtered = snap.documents
                    .map { AluNotePaymentFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.noteId == noteId }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun setAluNotePayment(id: String?, data: AluNotePaymentFS): String = set(aluNotePaymentsRef, id, data.toMap())

    // ── ALU Note Expenses ────────────────────────────────────
    fun getAluNoteExpensesFlow(noteId: String): Flow<List<AluNoteExpenseFS>> = callbackFlow {
        val reg = aluNoteExpensesRef.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snap != null) {
                val filtered = snap.documents
                    .map { AluNoteExpenseFS.fromMap(it.id, it.data ?: emptyMap()) }
                    .filter { it.noteId == noteId }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                trySend(filtered)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun getAluNoteExpenseById(id: String): AluNoteExpenseFS? {
        val snap = aluNoteExpensesRef.document(id).get().await()
        return if (snap.exists()) AluNoteExpenseFS.fromMap(id, snap.data!!) else null
    }

    suspend fun setAluNoteExpense(id: String?, data: AluNoteExpenseFS): String = set(aluNoteExpensesRef, id, data.toMap())
    suspend fun deleteAluNoteExpense(id: String) = del(aluNoteExpensesRef, id)
}
