package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val targetWalletName: String,
    val amount: Long,
    val dueDate: String = ""
)

data class ReceivableItem(
    val id: String,
    val walletId: String,
    val title: String,
    val sourceWalletName: String,
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
    val date: String
)

data class ArchitectureFeature(
    val title: String,
    val description: String,
    val iconName: String,
    val status: String,
    val isEnabled: Boolean = true
)

data class MainUiState(
    val counter: Int = 0,
    val isDarkThemeOverride: Boolean = false,
    val selectedTab: Int = 1, // Default to Dashboard (index 1)
    val wallets: List<Wallet> = listOf(
        Wallet("w1", "BCA Utama", 14_250_000, "7820192812", "Bank", "bank"),
        Wallet("w2", "Gopay", 850_000, "08123456789", "E-Wallet", "phone"),
        Wallet("w3", "Kas Tunai", 500_000, "-", "Cash", "cash"),
        Wallet("w4", "Mandiri Tabungan", 8_400_000, "13200812399", "Bank", "bank"),
        Wallet("w5", "OVO Cash", 320_000, "08123456789", "E-Wallet", "phone")
    ),
    val debts: List<DebtItem> = listOf(
        DebtItem("d1", "w1", "Cicilan Gadget", "Wallet Mandiri", 2_500_000, "15 Aug 2026"),
        DebtItem("d2", "w1", "Pinjam Modal Usaha", "BCA Rekening Mitra", 1_000_000, "20 Aug 2026"),
        DebtItem("d3", "w2", "Paylater Gopay", "Gopay Corporate", 150_000, "10 Aug 2026"),
        DebtItem("d4", "w4", "Utang Pembelian Alat", "Wallet Kas Tunai", 500_000, "25 Aug 2026")
    ),
    val receivables: List<ReceivableItem> = listOf(
        ReceivableItem("r1", "w1", "Pinjaman Budi", "Wallet Tunai Budi", 750_000, "12 Aug 2026"),
        ReceivableItem("r2", "w2", "Reimburse Kantor", "Wallet Gopay Kantor", 300_000, "08 Aug 2026"),
        ReceivableItem("r3", "w4", "Piutang Proyek Freelance", "Wallet BCA Klien", 3_500_000, "30 Aug 2026")
    ),
    val transactions: List<TransactionItem> = listOf(
        TransactionItem("t1", "w1", "BCA Utama", "Transfer Gaji Masuk", "Pemasukan", 12_000_000, TransactionType.INCOME, "03 Aug 2026 09:30"),
        TransactionItem("t2", "w2", "Gopay", "Makan Siang Resto", "Makanan & Minuman", 75_000, TransactionType.EXPENSE, "03 Aug 2026 12:15"),
        TransactionItem("t3", "w1", "BCA Utama", "Top Up Gopay", "Transfer", 200_000, TransactionType.TRANSFER, "02 Aug 2026 18:45"),
        TransactionItem("t4", "w3", "Kas Tunai", "Beli Bahan Makanan", "Belanja", 120_000, TransactionType.EXPENSE, "02 Aug 2026 10:20"),
        TransactionItem("t5", "w4", "Mandiri Tabungan", "Pembayaran Tagihan Listrik", "Utilitas", 450_000, TransactionType.EXPENSE, "01 Aug 2026 14:00"),
        TransactionItem("t6", "w1", "BCA Utama", "Pembayaran Project A", "Pemasukan", 2_500_000, TransactionType.INCOME, "31 Jul 2026 16:30")
    ),
    val selectedWalletDetail: Wallet? = null,
    val searchQuery: String = "",
    val incomeCategories: List<String> = listOf("Gaji", "Bonus", "Proyek Freelance", "Investasi", "Penjualan", "Lainnya"),
    val expenseCategories: List<String> = listOf("Makanan & Minuman", "Belanja", "Utilitas", "Transportasi", "Hiburan", "Kesehatan", "Pendidikan", "Lainnya"),
    val architectureFeatures: List<ArchitectureFeature> = listOf(
        ArchitectureFeature(
            title = "Jetpack Compose",
            description = "Declarative UI framework with modern Material Design 3 components.",
            iconName = "brush",
            status = "Ready"
        ),
        ArchitectureFeature(
            title = "Kotlin Coroutines & Flow",
            description = "Asynchronous, non-blocking state management with MutableStateFlow.",
            iconName = "sync",
            status = "Active"
        ),
        ArchitectureFeature(
            title = "Edge-to-Edge Design",
            description = "Full-bleed immersive layout with dynamic safe area inset handling.",
            iconName = "screen",
            status = "Enabled"
        ),
        ArchitectureFeature(
            title = "Room Database Ready",
            description = "KSP & Room dependencies configured for local persistent storage.",
            iconName = "database",
            status = "Configured"
        )
    ),
    val logs: List<String> = listOf("Aplikasi dasar berhasil diinisialisasi.")
) {
    val totalBalance: Long
        get() = wallets.sumOf { it.balance }
}

fun formatRupiah(amount: Long): String {
    val localeID = Locale("in", "ID")
    val formatter = NumberFormat.getCurrencyInstance(localeID)
    formatter.maximumFractionDigits = 0
    return formatter.format(amount).replace("Rp", "Rp ")
}

fun getCurrentFormattedDate(): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", Locale("in", "ID"))
    return sdf.format(java.util.Date())
}

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

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

        _uiState.update { state ->
            val updatedWallets = state.wallets.map { w ->
                if (w.id == walletId) w.copy(balance = w.balance + amount) else w
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
                date = dateStr
            )

            state.copy(
                wallets = updatedWallets,
                transactions = listOf(newTransaction) + state.transactions,
                logs = listOf("Pemasukan ${formatRupiah(amount)} ke $walletName berhasil dicatat.") + state.logs
            )
        }
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
                    date = dateStr
                )

                state.copy(
                    wallets = updatedWallets,
                    transactions = listOf(newTransaction) + state.transactions,
                    logs = listOf("Pengeluaran ${formatRupiah(amount)} dari ${mainWallet.name} berhasil dicatat.") + state.logs
                )
            } else {
                // Split Mode (Main wallet insufficient -> bailout wallet covers remaining)
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

                val newTransaction = TransactionItem(
                    id = "t_${System.currentTimeMillis()}",
                    walletId = walletId,
                    walletName = mainWallet.name,
                    title = "$titleText (Split dengan ${bailoutWallet?.name})",
                    category = category,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    date = dateStr
                )

                state.copy(
                    wallets = updatedWallets,
                    debts = listOf(newDebt) + state.debts,
                    transactions = listOf(newTransaction) + state.transactions,
                    logs = listOf("Pengeluaran Split ${formatRupiah(amount)} (${mainWallet.name} + Talangan ${bailoutWallet?.name}) berhasil dicatat. Utang ${formatRupiah(bailoutAmount)} dibuat.") + state.logs
                )
            }
        }
    }

    fun addTransfer(
        amount: Long,
        transferMode: String, // "Pemindahan Dana", "Bayar Utang", "Piutang (Meminjamkan)"
        sourceWalletId: String,
        destinationWalletId: String,
        note: String
    ) {
        if (amount <= 0 || sourceWalletId.isBlank() || destinationWalletId.isBlank() || sourceWalletId == destinationWalletId) return
        val dateStr = getCurrentFormattedDate()

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
                date = dateStr
            )

            var newDebts = state.debts
            var newReceivables = state.receivables

            when (transferMode) {
                "Bayar Utang" -> {
                    // Reduce or settle existing debt from sourceWallet to destWallet if exists
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
                    // Record receivable item
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
    }

    fun clearLogs() {
        _uiState.update { state ->
            state.copy(logs = listOf("Log dibersihkan."))
        }
    }
}

