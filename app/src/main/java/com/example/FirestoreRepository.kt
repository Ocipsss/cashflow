package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings

class FirestoreRepository(private val context: Context) {

    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    private var transactionListener: ListenerRegistration? = null
    private var walletListener: ListenerRegistration? = null
    private var debtListener: ListenerRegistration? = null
    private var receivableListener: ListenerRegistration? = null
    private var categoryListener: ListenerRegistration? = null

    init {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
            db = firestore
            auth = FirebaseAuth.getInstance()
            Log.d("FirestoreRepository", "Firebase Firestore & Auth initialized with offline persistence.")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Firebase initialization deferred or unavailable: ${e.message}")
        }
    }

    fun getCurrentUserUid(): String? {
        return try {
            auth?.currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    fun startRealtimeSync(
        uid: String,
        onTransactionsUpdated: (List<TransactionItem>) -> Unit,
        onWalletsUpdated: (List<Wallet>) -> Unit,
        onDebtsUpdated: (List<DebtItem>) -> Unit,
        onReceivablesUpdated: (List<ReceivableItem>) -> Unit,
        onCategoriesUpdated: (List<String>, List<String>) -> Unit
    ) {
        stopRealtimeSync()
        val firestore = db ?: return

        val userRef = firestore.collection("users").document(uid)

        // 1. Transactions Listener (Real-time listener for addSnapshotListener)
        transactionListener = userRef.collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepository", "Error listening to transactions: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val walletId = doc.getString("walletId") ?: ""
                            val walletName = doc.getString("walletName") ?: ""
                            val title = doc.getString("title") ?: ""
                            val category = doc.getString("category") ?: ""
                            val amount = doc.getLong("amount") ?: 0L
                            val typeStr = doc.getString("type") ?: "EXPENSE"
                            val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                            val date = doc.getString("date") ?: ""
                            TransactionItem(id, walletId, walletName, title, category, amount, type, date)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onTransactionsUpdated(list)
                }
            }

        // 2. Wallets Listener
        walletListener = userRef.collection("wallets")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val name = doc.getString("name") ?: ""
                            val balance = doc.getLong("balance") ?: 0L
                            val accountNumber = doc.getString("accountNumber") ?: ""
                            val type = doc.getString("type") ?: ""
                            val icon = doc.getString("icon") ?: "wallet"
                            Wallet(id, name, balance, accountNumber, type, icon)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onWalletsUpdated(list)
                }
            }

        // 3. Debts Listener
        debtListener = userRef.collection("debts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val walletId = doc.getString("walletId") ?: ""
                            val title = doc.getString("title") ?: ""
                            val lender = doc.getString("lender") ?: ""
                            val amount = doc.getLong("amount") ?: 0L
                            val dueDate = doc.getString("dueDate") ?: ""
                            DebtItem(id, walletId, title, lender, amount, dueDate)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onDebtsUpdated(list)
                }
            }

        // 4. Receivables Listener
        receivableListener = userRef.collection("receivables")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val walletId = doc.getString("walletId") ?: ""
                            val title = doc.getString("title") ?: ""
                            val borrower = doc.getString("borrower") ?: ""
                            val amount = doc.getLong("amount") ?: 0L
                            val dueDate = doc.getString("dueDate") ?: ""
                            ReceivableItem(id, walletId, title, borrower, amount, dueDate)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onReceivablesUpdated(list)
                }
            }

        // 5. Categories Listener
        categoryListener = userRef.collection("settings").document("categories")
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener
                if (doc != null && doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val inc = (doc.get("incomeCategories") as? List<String>) ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val exp = (doc.get("expenseCategories") as? List<String>) ?: emptyList()
                    onCategoriesUpdated(inc, exp)
                }
            }
    }

    fun stopRealtimeSync() {
        transactionListener?.remove()
        walletListener?.remove()
        debtListener?.remove()
        receivableListener?.remove()
        categoryListener?.remove()

        transactionListener = null
        walletListener = null
        debtListener = null
        receivableListener = null
        categoryListener = null
    }

    fun saveTransaction(uid: String, transaction: TransactionItem) {
        val firestore = db ?: return
        val docData = hashMapOf(
            "id" to transaction.id,
            "walletId" to transaction.walletId,
            "walletName" to transaction.walletName,
            "title" to transaction.title,
            "category" to transaction.category,
            "amount" to transaction.amount,
            "type" to transaction.type.name,
            "date" to transaction.date,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(uid)
            .collection("transactions").document(transaction.id)
            .set(docData)
    }

    fun deleteTransaction(uid: String, transactionId: String) {
        val firestore = db ?: return
        firestore.collection("users").document(uid)
            .collection("transactions").document(transactionId)
            .delete()
    }

    fun saveWallet(uid: String, wallet: Wallet) {
        val firestore = db ?: return
        val docData = hashMapOf(
            "id" to wallet.id,
            "name" to wallet.name,
            "balance" to wallet.balance,
            "accountNumber" to wallet.accountNumber,
            "type" to wallet.type,
            "iconName" to wallet.iconName
        )
        firestore.collection("users").document(uid)
            .collection("wallets").document(wallet.id)
            .set(docData)
    }

    fun deleteWallet(uid: String, walletId: String) {
        val firestore = db ?: return
        firestore.collection("users").document(uid)
            .collection("wallets").document(walletId)
            .delete()
    }

    fun saveDebt(uid: String, debt: DebtItem) {
        val firestore = db ?: return
        val docData = hashMapOf(
            "id" to debt.id,
            "walletId" to debt.walletId,
            "title" to debt.title,
            "targetWalletName" to debt.targetWalletName,
            "amount" to debt.amount,
            "dueDate" to debt.dueDate
        )
        firestore.collection("users").document(uid)
            .collection("debts").document(debt.id)
            .set(docData)
    }

    fun saveReceivable(uid: String, receivable: ReceivableItem) {
        val firestore = db ?: return
        val docData = hashMapOf(
            "id" to receivable.id,
            "walletId" to receivable.walletId,
            "title" to receivable.title,
            "sourceWalletName" to receivable.sourceWalletName,
            "amount" to receivable.amount,
            "dueDate" to receivable.dueDate
        )
        firestore.collection("users").document(uid)
            .collection("receivables").document(receivable.id)
            .set(docData)
    }

    fun saveCategories(uid: String, incomeCategories: List<String>, expenseCategories: List<String>) {
        val firestore = db ?: return
        val docData = hashMapOf(
            "incomeCategories" to incomeCategories,
            "expenseCategories" to expenseCategories
        )
        firestore.collection("users").document(uid)
            .collection("settings").document("categories")
            .set(docData)
    }
}
