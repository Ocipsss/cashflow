package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

data class Wallet(
    val id: String,
    val name: String,
    val balance: Long,
    val accountNumber: String = "",
    val type: String = "Bank",
    val iconName: String = "account_balance_wallet"
)

data class DebtItem(
    val id: String,
    val walletId: String,
    val title: String,
    val targetWalletName: String = "",
    val amount: Long,
    val dueDate: String = ""
)

data class ReceivableItem(
    val id: String,
    val walletId: String,
    val title: String,
    val sourceWalletName: String = "",
    val amount: Long,
    val dueDate: String = ""
)

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER
}

data class TransactionItem(
    val id: String,
    val walletId: String,
    val walletName: String,
    val title: String,
    val category: String,
    val amount: Long,
    val type: TransactionType,
    val date: String,
    val note: String = ""
)

data class ArchitectureFeature(
    val title: String,
    val description: String,
    val status: String
)

data class MainUiState(
    val counter: Int = 0,
    val isDarkThemeOverride: Boolean = false,
    val selectedTab: Int = 1, // Default to Dashboard (index 1)
    val wallets: List<Wallet> = emptyList(),
    val debts: List<DebtItem> = emptyList(),
    val receivables: List<ReceivableItem> = emptyList(),
    val transactions: List<TransactionItem> = emptyList(),
    val selectedWalletDetail: Wallet? = null,
    val searchQuery: String = "",
    val incomeCategories: List<String> = emptyList(),
    val expenseCategories: List<String> = emptyList(),
    val architectureFeatures: List<ArchitectureFeature> = listOf(
        ArchitectureFeature(
            title = "Jetpack Compose",
            description = "UI deklaratif berbasis Material Design 3 yang modern dan responsif.",
            status = "Aktif"
        ),
        ArchitectureFeature(
            title = "Penyimpanan Lokal (SharedPreferences & JSON)",
            description = "Persistensi data lokal yang cepat, efisien, dan aman tanpa memerlukan koneksi internet.",
            status = "Aktif"
        ),
        ArchitectureFeature(
            title = "StateFlow & ViewModel",
            description = "Manajemen state terpusat yang reaktif dengan Coroutines.",
            status = "Aktif"
        )
    ),
    val logs: List<String> = listOf("Aplikasi dasar berhasil diinisialisasi.")
) {
    val totalBalance: Long get() = wallets.sumOf { it.balance }
}

fun formatRupiah(amount: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amount)
}

fun getCurrentFormattedDate(): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
    return sdf.format(java.util.Date())
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("base_app_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadStateFromPrefs()
    }

    private fun loadStateFromPrefs() {
        val savedJson = prefs.getString("saved_data_json", null)
        val savedDarkTheme = prefs.getBoolean("is_dark_theme", false)

        if (!savedJson.isNullOrBlank()) {
            val success = restoreDatabaseFromJsonInternal(savedJson)
            if (success) {
                _uiState.update { it.copy(isDarkThemeOverride = savedDarkTheme) }
            }
        } else {
            saveStateToPrefs()
        }
    }

    private fun saveStateToPrefs() {
        try {
            val jsonStr = exportBackupJson()
            val state = _uiState.value

            prefs.edit()
                .putString("saved_data_json", jsonStr)
                .putBoolean("is_dark_theme", state.isDarkThemeOverride)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectWalletForDetail(wallet: Wallet?) {
        _uiState.update { state ->
            state.copy(selectedWalletDetail = wallet)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(searchQuery = query)
        }
    }

    fun addIncome(
        walletId: String,
        amount: Long,
        category: String,
        note: String
    ) {
        if (amount <= 0 || walletId.isBlank()) return
        val dateStr = getCurrentFormattedDate()

        var createdTx: TransactionItem? = null
        var updatedTargetWallet: Wallet? = null

        _uiState.update { state ->
            val updatedWallets = state.wallets.map { w ->
                if (w.id == walletId) {
                    val updated = w.copy(balance = w.balance + amount)
                    updatedTargetWallet = updated
                    updated
                } else w
            }
            val targetWallet = updatedWallets.find { it.id == walletId }
            val walletName = targetWallet?.name ?: "Wallet"
            val titleText = if (note.isNotBlank()) note else "Pemasukan - $category"

            val newTransaction = TransactionItem(
                id = "t_${System.currentTimeMillis()}",
                walletId = walletId,
                walletName = walletName,
                title = titleText,
                category = category,
                amount = amount,
                type = TransactionType.INCOME,
                date = dateStr,
                note = note
            )
            createdTx = newTransaction

            state.copy(
                wallets = updatedWallets,
                transactions = listOf(newTransaction) + state.transactions,
                logs = listOf("Pemasukan ${formatRupiah(amount)} ke $walletName berhasil dicatat.") + state.logs
            )
        }

        saveStateToPrefs()
    }

    fun addExpense(
        walletId: String,
        amount: Long,
        category: String,
        note: String,
        bailoutWalletId: String? = null
    ) {
        if (amount <= 0 || walletId.isBlank()) return
        val dateStr = getCurrentFormattedDate()

        var createdTx: TransactionItem? = null

        _uiState.update { state ->
            val mainWallet = state.wallets.find { it.id == walletId } ?: return@update state
            val titleText = if (note.isNotBlank()) note else "Pengeluaran - $category"

            if (amount <= mainWallet.balance || bailoutWalletId == null || bailoutWalletId == walletId) {
                // Normal Expense
                val updatedWallets = state.wallets.map { w ->
                    if (w.id == walletId) w.copy(balance = (w.balance - amount).coerceAtLeast(0)) else w
                }

                val newTransaction = TransactionItem(
                    id = "t_${System.currentTimeMillis()}",
                    walletId = walletId,
                    walletName = mainWallet.name,
                    title = titleText,
                    category = category,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    date = dateStr,
                    note = note
                )
                createdTx = newTransaction

                state.copy(
                    wallets = updatedWallets,
                    transactions = listOf(newTransaction) + state.transactions,
                    logs = listOf("Pengeluaran ${formatRupiah(amount)} dari ${mainWallet.name} berhasil dicatat.") + state.logs
                )
            } else {
                // Split Mode
                val mainWalletBalanceCovered = mainWallet.balance
                val bailoutAmount = amount - mainWalletBalanceCovered
                val bailoutWallet = state.wallets.find { it.id == bailoutWalletId }

                val updatedWallets = state.wallets.map { w ->
                    when (w.id) {
                        walletId -> w.copy(balance = 0)
                        bailoutWalletId -> w.copy(balance = (w.balance - bailoutAmount).coerceAtLeast(0))
                        else -> w
                    }
                }

                val newDebt = DebtItem(
                    id = "d_${System.currentTimeMillis()}",
                    walletId = walletId,
                    title = "Talangan Pengeluaran ($category)",
                    targetWalletName = bailoutWallet?.name ?: "Wallet Talangan",
                    amount = bailoutAmount,
                    dueDate = "Otomatis (Split)"
                )

                val newReceivable = bailoutWallet?.let { bWallet ->
                    ReceivableItem(
                        id = "r_${System.currentTimeMillis()}",
                        walletId = bWallet.id,
                        title = "Talangan Pengeluaran ($category)",
                        sourceWalletName = mainWallet.name,
                        amount = bailoutAmount,
                        dueDate = "Otomatis (Split)"
                    )
                }

                val splitInfo = "Dipotong ${mainWallet.name}: ${formatRupiah(mainWalletBalanceCovered)} | Talangan ${bailoutWallet?.name ?: "Talangan"}: ${formatRupiah(bailoutAmount)}"
                val fullNote = if (note.isNotBlank()) "$note • $splitInfo" else splitInfo

                val newTransaction = TransactionItem(
                    id = "t_${System.currentTimeMillis()}",
                    walletId = walletId,
                    walletName = mainWallet.name,
                    title = "$titleText (Split dengan ${bailoutWallet?.name ?: "Talangan"})",
                    category = category,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    date = dateStr,
                    note = fullNote
                )
                createdTx = newTransaction

                state.copy(
                    wallets = updatedWallets,
                    debts = listOf(newDebt) + state.debts,
                    receivables = if (newReceivable != null) listOf(newReceivable) + state.receivables else state.receivables,
                    transactions = listOf(newTransaction) + state.transactions,
                    logs = listOf("Pengeluaran Split ${formatRupiah(amount)} (${mainWallet.name} + Talangan ${bailoutWallet?.name}) berhasil dicatat. Utang ${formatRupiah(bailoutAmount)} dibuat.") + state.logs
                )
            }
        }

        saveStateToPrefs()
    }

    fun addTransfer(
        amount: Long,
        transferMode: String,
        sourceWalletId: String,
        destinationWalletId: String,
        note: String
    ) {
        if (amount <= 0 || sourceWalletId.isBlank() || destinationWalletId.isBlank() || sourceWalletId == destinationWalletId) return
        val dateStr = getCurrentFormattedDate()

        var createdTx: TransactionItem? = null

        _uiState.update { state ->
            val sourceWallet = state.wallets.find { it.id == sourceWalletId } ?: return@update state
            val destWallet = state.wallets.find { it.id == destinationWalletId } ?: return@update state

            val updatedWallets = state.wallets.map { w ->
                when (w.id) {
                    sourceWalletId -> w.copy(balance = (w.balance - amount).coerceAtLeast(0))
                    destinationWalletId -> w.copy(balance = w.balance + amount)
                    else -> w
                }
            }

            val titleText = if (note.isNotBlank()) note else "$transferMode: ${sourceWallet.name} ➔ ${destWallet.name}"

            val newTransaction = TransactionItem(
                id = "t_${System.currentTimeMillis()}",
                walletId = sourceWalletId,
                walletName = sourceWallet.name,
                title = titleText,
                category = transferMode,
                amount = amount,
                type = TransactionType.TRANSFER,
                date = dateStr,
                note = note
            )
            createdTx = newTransaction

            var newDebts = state.debts
            var newReceivables = state.receivables

            when (transferMode) {
                "Bayar Utang" -> {
                    val existingDebt = state.debts.find { it.walletId == sourceWalletId && it.targetWalletName == destWallet.name }
                    if (existingDebt != null) {
                        if (existingDebt.amount <= amount) {
                            newDebts = state.debts.filter { it.id != existingDebt.id }
                        } else {
                            newDebts = state.debts.map {
                                if (it.id == existingDebt.id) it.copy(amount = it.amount - amount) else it
                            }
                        }
                    }
                }
                "Piutang (Meminjamkan)" -> {
                    val newReceivable = ReceivableItem(
                        id = "r_${System.currentTimeMillis()}",
                        walletId = sourceWalletId,
                        title = if (note.isNotBlank()) note else "Pinjaman ke ${destWallet.name}",
                        sourceWalletName = destWallet.name,
                        amount = amount,
                        dueDate = "Otomatis"
                    )
                    newReceivables = listOf(newReceivable) + state.receivables
                }
            }

            state.copy(
                wallets = updatedWallets,
                debts = newDebts,
                receivables = newReceivables,
                transactions = listOf(newTransaction) + state.transactions,
                logs = listOf("Transfer $transferMode ${formatRupiah(amount)} dari ${sourceWallet.name} ke ${destWallet.name} berhasil.") + state.logs
            )
        }

        saveStateToPrefs()
    }

    fun addWallet(name: String, balance: Long, accountNumber: String, type: String) {
        if (name.isBlank()) return
        val newWallet = Wallet(
            id = "w_${System.currentTimeMillis()}",
            name = name.trim(),
            balance = balance,
            accountNumber = accountNumber.ifBlank { "-" },
            type = type
        )
        _uiState.update { state ->
            state.copy(
                wallets = state.wallets + newWallet,
                logs = listOf("Wallet baru '${newWallet.name}' berhasil ditambahkan.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun editWallet(
        id: String,
        newName: String,
        newBalance: Long,
        newAccountNumber: String,
        newType: String
    ) {
        val nameTrimmed = newName.trim()
        if (nameTrimmed.isBlank()) return
        _uiState.update { state ->
            val updatedWallets = state.wallets.map { w ->
                if (w.id == id) {
                    w.copy(
                        name = nameTrimmed,
                        balance = newBalance,
                        accountNumber = newAccountNumber.ifBlank { "-" },
                        type = newType
                    )
                } else w
            }
            val updatedTransactions = state.transactions.map { t ->
                if (t.walletId == id) t.copy(walletName = nameTrimmed) else t
            }
            val updatedSelectedWallet = if (state.selectedWalletDetail?.id == id) {
                updatedWallets.find { it.id == id }
            } else state.selectedWalletDetail

            state.copy(
                wallets = updatedWallets,
                transactions = updatedTransactions,
                selectedWalletDetail = updatedSelectedWallet,
                logs = listOf("Wallet '$nameTrimmed' berhasil diperbarui.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun deleteWallet(id: String) {
        _uiState.update { state ->
            val targetWallet = state.wallets.find { it.id == id }
            val walletName = targetWallet?.name ?: "Wallet"
            val updatedWallets = state.wallets.filter { it.id != id }
            val updatedSelected = if (state.selectedWalletDetail?.id == id) null else state.selectedWalletDetail

            state.copy(
                wallets = updatedWallets,
                selectedWalletDetail = updatedSelected,
                logs = listOf("Wallet '$walletName' berhasil dihapus.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun addIncomeCategory(categoryName: String) {
        val trimmed = categoryName.trim()
        if (trimmed.isBlank()) return
        _uiState.update { state ->
            if (state.incomeCategories.contains(trimmed)) state
            else state.copy(
                incomeCategories = state.incomeCategories + trimmed,
                logs = listOf("Kategori Pemasukan '$trimmed' berhasil ditambahkan.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun editIncomeCategory(oldCategory: String, newCategory: String) {
        val trimmed = newCategory.trim()
        if (trimmed.isBlank() || oldCategory == trimmed) return
        _uiState.update { state ->
            val updatedCategories = state.incomeCategories.map {
                if (it == oldCategory) trimmed else it
            }
            val updatedTransactions = state.transactions.map { t ->
                if (t.type == TransactionType.INCOME && t.category == oldCategory) {
                    t.copy(category = trimmed)
                } else t
            }
            state.copy(
                incomeCategories = updatedCategories,
                transactions = updatedTransactions,
                logs = listOf("Kategori Pemasukan '$oldCategory' diubah menjadi '$trimmed'.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun deleteIncomeCategory(categoryName: String) {
        _uiState.update { state ->
            val updatedCategories = state.incomeCategories.filter { it != categoryName }
            state.copy(
                incomeCategories = updatedCategories,
                logs = listOf("Kategori Pemasukan '$categoryName' berhasil dihapus.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun addExpenseCategory(categoryName: String) {
        val trimmed = categoryName.trim()
        if (trimmed.isBlank()) return
        _uiState.update { state ->
            if (state.expenseCategories.contains(trimmed)) state
            else state.copy(
                expenseCategories = state.expenseCategories + trimmed,
                logs = listOf("Kategori Pengeluaran '$trimmed' berhasil ditambahkan.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun editExpenseCategory(oldCategory: String, newCategory: String) {
        val trimmed = newCategory.trim()
        if (trimmed.isBlank() || oldCategory == trimmed) return
        _uiState.update { state ->
            val updatedCategories = state.expenseCategories.map {
                if (it == oldCategory) trimmed else it
            }
            val updatedTransactions = state.transactions.map { t ->
                if (t.type == TransactionType.EXPENSE && t.category == oldCategory) {
                    t.copy(category = trimmed)
                } else t
            }
            state.copy(
                expenseCategories = updatedCategories,
                transactions = updatedTransactions,
                logs = listOf("Kategori Pengeluaran '$oldCategory' diubah menjadi '$trimmed'.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun deleteExpenseCategory(categoryName: String) {
        _uiState.update { state ->
            val updatedCategories = state.expenseCategories.filter { it != categoryName }
            state.copy(
                expenseCategories = updatedCategories,
                logs = listOf("Kategori Pengeluaran '$categoryName' berhasil dihapus.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun incrementCounter() {
        _uiState.update { state ->
            val nextVal = state.counter + 1
            state.copy(
                counter = nextVal,
                logs = listOf("Penghitung ditingkatkan ke $nextVal") + state.logs
            )
        }
    }

    fun decrementCounter() {
        _uiState.update { state ->
            val nextVal = (state.counter - 1).coerceAtLeast(0)
            state.copy(
                counter = nextVal,
                logs = listOf("Penghitung dikurangi ke $nextVal") + state.logs
            )
        }
    }

    fun resetCounter() {
        _uiState.update { state ->
            state.copy(
                counter = 0,
                logs = listOf("Penghitung direset ke 0") + state.logs
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { state ->
            state.copy(selectedTab = index)
        }
    }

    fun toggleThemeOverride() {
        _uiState.update { state ->
            val nextState = !state.isDarkThemeOverride
            state.copy(
                isDarkThemeOverride = nextState,
                logs = listOf("Mode tema diubah ke ${if (nextState) "Gelap" else "Terang"}") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun clearLogs() {
        _uiState.update { state ->
            state.copy(logs = listOf("Log dibersihkan."))
        }
    }

    fun editTransaction(
        id: String,
        newTitle: String,
        newAmount: Long,
        newCategory: String,
        newWalletId: String,
        newDate: String
    ) {
        if (newAmount <= 0 || newTitle.isBlank()) return
        var editedTx: TransactionItem? = null
        _uiState.update { state ->
            val oldTx = state.transactions.find { it.id == id } ?: return@update state
            val targetWallet = state.wallets.find { it.id == newWalletId } ?: return@update state

            var tempWallets = state.wallets.map { w ->
                if (w.id == oldTx.walletId) {
                    val revertedBalance = when (oldTx.type) {
                        TransactionType.INCOME -> w.balance - oldTx.amount
                        TransactionType.EXPENSE -> w.balance + oldTx.amount
                        TransactionType.TRANSFER -> w.balance + oldTx.amount
                    }
                    w.copy(balance = revertedBalance.coerceAtLeast(0))
                } else w
            }

            tempWallets = tempWallets.map { w ->
                if (w.id == newWalletId) {
                    val newBalance = when (oldTx.type) {
                        TransactionType.INCOME -> w.balance + newAmount
                        TransactionType.EXPENSE -> (w.balance - newAmount).coerceAtLeast(0)
                        TransactionType.TRANSFER -> (w.balance - newAmount).coerceAtLeast(0)
                    }
                    w.copy(balance = newBalance)
                } else w
            }

            val updatedTransactions = state.transactions.map { t ->
                if (t.id == id) {
                    val updated = t.copy(
                        title = newTitle.trim(),
                        amount = newAmount,
                        category = newCategory,
                        walletId = newWalletId,
                        walletName = targetWallet.name,
                        date = newDate.ifBlank { t.date }
                    )
                    editedTx = updated
                    updated
                } else t
            }

            state.copy(
                wallets = tempWallets,
                transactions = updatedTransactions,
                logs = listOf("Transaksi '${newTitle.trim()}' berhasil diperbarui.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun deleteTransaction(id: String) {
        _uiState.update { state ->
            val tx = state.transactions.find { it.id == id } ?: return@update state
            val updatedWallets = state.wallets.map { w ->
                if (w.id == tx.walletId) {
                    val revertedBalance = when (tx.type) {
                        TransactionType.INCOME -> w.balance - tx.amount
                        TransactionType.EXPENSE -> w.balance + tx.amount
                        TransactionType.TRANSFER -> w.balance + tx.amount
                    }
                    w.copy(balance = revertedBalance.coerceAtLeast(0))
                } else w
            }

            val updatedTransactions = state.transactions.filter { it.id != id }

            state.copy(
                wallets = updatedWallets,
                transactions = updatedTransactions,
                logs = listOf("Transaksi '${tx.title}' (${formatRupiah(tx.amount)}) berhasil dihapus.") + state.logs
            )
        }
        saveStateToPrefs()
    }

    fun exportBackupJson(): String {
        val state = _uiState.value
        val root = JSONObject()

        val walletsArr = JSONArray()
        state.wallets.forEach { w ->
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("name", w.name)
            obj.put("balance", w.balance)
            obj.put("accountNumber", w.accountNumber)
            obj.put("type", w.type)
            obj.put("iconName", w.iconName)
            walletsArr.put(obj)
        }
        root.put("wallets", walletsArr)

        val txArr = JSONArray()
        state.transactions.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("walletId", t.walletId)
            obj.put("walletName", t.walletName)
            obj.put("title", t.title)
            obj.put("category", t.category)
            obj.put("amount", t.amount)
            obj.put("type", t.type.name)
            obj.put("date", t.date)
            obj.put("note", t.note)
            txArr.put(obj)
        }
        root.put("transactions", txArr)

        val debtsArr = JSONArray()
        state.debts.forEach { d ->
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("walletId", d.walletId)
            obj.put("title", d.title)
            obj.put("targetWalletName", d.targetWalletName)
            obj.put("amount", d.amount)
            obj.put("dueDate", d.dueDate)
            debtsArr.put(obj)
        }
        root.put("debts", debtsArr)

        val recArr = JSONArray()
        state.receivables.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("walletId", r.walletId)
            obj.put("title", r.title)
            obj.put("sourceWalletName", r.sourceWalletName)
            obj.put("amount", r.amount)
            obj.put("dueDate", r.dueDate)
            recArr.put(obj)
        }
        root.put("receivables", recArr)

        val incCatArr = JSONArray()
        state.incomeCategories.forEach { incCatArr.put(it) }
        root.put("incomeCategories", incCatArr)

        val expCatArr = JSONArray()
        state.expenseCategories.forEach { expCatArr.put(it) }
        root.put("expenseCategories", expCatArr)

        return root.toString(2)
    }

    private fun restoreDatabaseFromJsonInternal(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)

            val walletsList = mutableListOf<Wallet>()
            if (root.has("wallets")) {
                val arr = root.getJSONArray("wallets")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    walletsList.add(
                        Wallet(
                            id = obj.optString("id", "w_$i"),
                            name = obj.optString("name", "Wallet"),
                            balance = obj.optLong("balance", 0L),
                            accountNumber = obj.optString("accountNumber", "-"),
                            type = obj.optString("type", "Bank"),
                            iconName = obj.optString("iconName", "account_balance_wallet")
                        )
                    )
                }
            }

            val txList = mutableListOf<TransactionItem>()
            if (root.has("transactions")) {
                val arr = root.getJSONArray("transactions")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val typeStr = obj.optString("type", "EXPENSE")
                    val typeEnum = try {
                        TransactionType.valueOf(typeStr)
                    } catch (e: Exception) {
                        TransactionType.EXPENSE
                    }
                    txList.add(
                        TransactionItem(
                            id = obj.optString("id", "t_$i"),
                            walletId = obj.optString("walletId", ""),
                            walletName = obj.optString("walletName", ""),
                            title = obj.optString("title", ""),
                            category = obj.optString("category", ""),
                            amount = obj.optLong("amount", 0L),
                            type = typeEnum,
                            date = obj.optString("date", ""),
                            note = obj.optString("note", "")
                        )
                    )
                }
            }

            val debtsList = mutableListOf<DebtItem>()
            if (root.has("debts")) {
                val arr = root.getJSONArray("debts")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    debtsList.add(
                        DebtItem(
                            id = obj.optString("id", "d_$i"),
                            walletId = obj.optString("walletId", ""),
                            title = obj.optString("title", ""),
                            targetWalletName = obj.optString("targetWalletName", ""),
                            amount = obj.optLong("amount", 0L),
                            dueDate = obj.optString("dueDate", "")
                        )
                    )
                }
            }

            val recList = mutableListOf<ReceivableItem>()
            if (root.has("receivables")) {
                val arr = root.getJSONArray("receivables")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    recList.add(
                        ReceivableItem(
                            id = obj.optString("id", "r_$i"),
                            walletId = obj.optString("walletId", ""),
                            title = obj.optString("title", ""),
                            sourceWalletName = obj.optString("sourceWalletName", ""),
                            amount = obj.optLong("amount", 0L),
                            dueDate = obj.optString("dueDate", "")
                        )
                    )
                }
            }

            val incCatList = mutableListOf<String>()
            if (root.has("incomeCategories")) {
                val arr = root.getJSONArray("incomeCategories")
                for (i in 0 until arr.length()) {
                    incCatList.add(arr.getString(i))
                }
            }

            val expCatList = mutableListOf<String>()
            if (root.has("expenseCategories")) {
                val arr = root.getJSONArray("expenseCategories")
                for (i in 0 until arr.length()) {
                    expCatList.add(arr.getString(i))
                }
            }

            _uiState.update { state ->
                state.copy(
                    wallets = walletsList,
                    transactions = txList,
                    debts = debtsList,
                    receivables = recList,
                    incomeCategories = if (root.has("incomeCategories")) incCatList else state.incomeCategories,
                    expenseCategories = if (root.has("expenseCategories")) expCatList else state.expenseCategories,
                    logs = listOf("Database berhasil di-restore dari data JSON.") + state.logs
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreDatabaseFromJson(jsonStr: String): Boolean {
        val success = restoreDatabaseFromJsonInternal(jsonStr)
        if (success) {
            saveStateToPrefs()
        }
        return success
    }

    fun resetDatabase() {
        _uiState.update { state ->
            state.copy(
                wallets = emptyList(),
                transactions = emptyList(),
                debts = emptyList(),
                receivables = emptyList(),
                incomeCategories = emptyList(),
                expenseCategories = emptyList(),
                logs = listOf("Database telah di-reset. Semua data sampel & template kategori telah dibersihkan.")
            )
        }
        saveStateToPrefs()
    }
}
