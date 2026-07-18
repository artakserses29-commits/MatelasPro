package com.matelaspro.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.matelaspro.app.data.dao.AluDevisDao
import com.matelaspro.app.data.dao.AluNoteDao
import com.matelaspro.app.data.dao.AluNoteExpenseDao
import com.matelaspro.app.data.dao.AluNotePaymentDao
import com.matelaspro.app.data.dao.AluProductDao
import com.matelaspro.app.data.dao.AuditLogDao
import com.matelaspro.app.data.dao.CreditEntryDao
import com.matelaspro.app.data.dao.FournisseurDao
import com.matelaspro.app.data.dao.FournisseurPaiementDao
import com.matelaspro.app.data.dao.PaymentDao
import com.matelaspro.app.data.dao.ProductDao
import com.matelaspro.app.data.dao.SaleDao
import com.matelaspro.app.data.dao.ExpenseDao
import com.matelaspro.app.data.dao.UserDao
import com.matelaspro.app.data.dao.UserStockDao
import com.matelaspro.app.data.entity.AluDevis
import com.matelaspro.app.data.entity.AluNote
import com.matelaspro.app.data.entity.AluNoteExpense
import com.matelaspro.app.data.entity.AluNotePayment
import com.matelaspro.app.data.entity.AluProduct
import com.matelaspro.app.data.entity.AuditLog
import com.matelaspro.app.data.entity.CreditEntry
import com.matelaspro.app.data.entity.Expense
import com.matelaspro.app.data.entity.Fournisseur
import com.matelaspro.app.data.entity.UserStock
import com.matelaspro.app.data.entity.FournisseurPaiement
import com.matelaspro.app.data.entity.Payment
import com.matelaspro.app.data.entity.Product
import com.matelaspro.app.data.entity.Sale
import com.matelaspro.app.data.entity.User

@Database(
    entities = [Product::class, Sale::class, Payment::class, AluProduct::class, AluDevis::class, AluNote::class, AluNotePayment::class, AluNoteExpense::class, Fournisseur::class, FournisseurPaiement::class, CreditEntry::class, AuditLog::class, User::class, Expense::class, UserStock::class],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun aluProductDao(): AluProductDao
    abstract fun aluDevisDao(): AluDevisDao
    abstract fun aluNoteDao(): AluNoteDao
    abstract fun aluNotePaymentDao(): AluNotePaymentDao
    abstract fun aluNoteExpenseDao(): AluNoteExpenseDao
    abstract fun fournisseurDao(): FournisseurDao
    abstract fun fournisseurPaiementDao(): FournisseurPaiementDao
    abstract fun creditEntryDao(): CreditEntryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun userStockDao(): UserStockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_9_10 = Migration(9, 10) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS credit_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fournisseur TEXT NOT NULL DEFAULT '', productName TEXT NOT NULL, category TEXT NOT NULL DEFAULT '', epaisseur REAL NOT NULL DEFAULT 0, longueur REAL NOT NULL DEFAULT 0, description TEXT NOT NULL DEFAULT '', quantity INTEGER NOT NULL DEFAULT 0, sellingPrice REAL NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL DEFAULT 0)")
            copyProductsToCreditEntries(db)
        }

        val MIGRATION_10_11 = Migration(10, 11) { db ->
            copyProductsToCreditEntries(db)
        }

        val MIGRATION_11_12 = Migration(11, 12) { db ->
            db.execSQL("ALTER TABLE credit_entries ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        }

        val MIGRATION_12_13 = Migration(12, 13) { db ->
            db.execSQL("ALTER TABLE credit_entries ADD COLUMN productId INTEGER")
            db.execSQL("ALTER TABLE credit_entries ADD COLUMN isOverridden INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE credit_entries ADD COLUMN correctionCount INTEGER NOT NULL DEFAULT 0")
        }

        val MIGRATION_13_14 = Migration(13, 14) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS audit_log (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, action TEXT NOT NULL, tableName TEXT NOT NULL, recordId INTEGER NOT NULL, detail TEXT NOT NULL DEFAULT '', createdAt INTEGER NOT NULL DEFAULT 0)")
        }

        val MIGRATION_14_15 = Migration(14, 15) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, password TEXT NOT NULL, isAdmin INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("CREATE TABLE IF NOT EXISTS expenses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL, amount REAL NOT NULL DEFAULT 0, description TEXT NOT NULL DEFAULT '', createdAt INTEGER NOT NULL DEFAULT 0, FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_userId ON expenses(userId)")
            db.execSQL("ALTER TABLE sales ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
        }

        val MIGRATION_15_16 = Migration(15, 16) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS user_stock (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL, productId INTEGER NOT NULL, quantity INTEGER NOT NULL DEFAULT 0, updatedAt INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_user_stock_userId ON user_stock(userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_user_stock_productId ON user_stock(productId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_stock_userId_productId ON user_stock(userId, productId)")
        }

        private fun copyProductsToCreditEntries(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT OR IGNORE INTO credit_entries (fournisseur, productName, category, epaisseur, longueur, description, quantity, sellingPrice, createdAt) SELECT fournisseur, name, category, epaisseur, longueur, description, quantity, sellingPrice, createdAt FROM products")
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "matelaspro_database"
                )
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
