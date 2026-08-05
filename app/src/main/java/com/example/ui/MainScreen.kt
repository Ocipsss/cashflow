package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.ArchitectureFeature
import com.example.DebtItem
import com.example.MainUiState
import com.example.MainViewModel
import com.example.ReceivableItem
import com.example.TransactionItem
import com.example.TransactionType
import com.example.Wallet
import com.example.formatRupiah
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "App Icon",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = when (uiState.selectedTab) {
                                0 -> "Riwayat Transaksi"
                                1 -> "Beranda Utama"
                                else -> "Pengaturan Aplikasi"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleThemeOverride() },
                        modifier = Modifier.testTag("toggle_theme_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkThemeOverride) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Riwayat") },
                    label = { Text("Riwayat") },
                    modifier = Modifier.testTag("nav_item_history")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Beranda") },
                    label = { Text("Beranda") },
                    modifier = Modifier.testTag("nav_item_dashboard")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Pengaturan") },
                    modifier = Modifier.testTag("nav_item_setting")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (uiState.selectedTab) {
                0 -> HistoryTab(uiState = uiState, viewModel = viewModel)
                1 -> DashboardTab(uiState = uiState, viewModel = viewModel)
                2 -> SettingsTab(uiState = uiState, viewModel = viewModel)
            }

            DraggableFloatingActionButton(
                onClick = { showAddTransactionDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )

            if (showAddTransactionDialog) {
                AddTransactionFullScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDismiss = { showAddTransactionDialog = false }
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Card: Total Saldo Semua Wallet
        item {
            TotalBalanceCard(
                totalBalance = uiState.totalBalance,
                walletCount = uiState.wallets.size
            )
        }

        // 2. Daftar Wallet (Card-Card Kecil)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Wallet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Klik card untuk detail",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(uiState.wallets) { wallet ->
                        SmallWalletCard(
                            wallet = wallet,
                            onClick = { viewModel.selectWalletForDetail(wallet) }
                        )
                    }
                }
            }
        }

        // 3. History Transaksi Terbaru
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History Transaksi Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.selectTab(0) }) {
                    Text("Lihat Semua")
                }
            }
        }

        items(uiState.transactions.take(5)) { transaction ->
            TransactionItemRow(transaction = transaction)
        }
    }

    // Modal / Full Screen Detail Wallet saat card wallet di-klik
    uiState.selectedWalletDetail?.let { selectedWallet ->
        val walletTransactions = uiState.transactions.filter {
            it.walletId == selectedWallet.id ||
            it.title.contains(selectedWallet.name, ignoreCase = true) ||
            it.note.contains(selectedWallet.name, ignoreCase = true)
        }
        WalletDetailDialog(
            wallet = selectedWallet,
            debts = uiState.debts.filter { it.walletId == selectedWallet.id },
            receivables = uiState.receivables.filter { it.walletId == selectedWallet.id },
            transactions = walletTransactions,
            onDismiss = { viewModel.selectWalletForDetail(null) },
            onDeleteWallet = { id ->
                viewModel.deleteWallet(id)
                viewModel.selectWalletForDetail(null)
            }
        )
    }
}

@Composable
fun TotalBalanceCard(
    totalBalance: Long,
    walletCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("total_balance_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Total Saldo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "TOTAL SEMUA DOMPET",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$walletCount Wallet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = formatRupiah(totalBalance),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("total_balance_text")
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terhitung otomatis dari seluruh saldo wallet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Valid",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SmallWalletCard(
    wallet: Wallet,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() }
            .testTag("wallet_card_${wallet.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (wallet.type) {
                                "Bank" -> MaterialTheme.colorScheme.primaryContainer
                                "E-Wallet" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (wallet.type) {
                            "Bank" -> Icons.Default.AccountBalance
                            "E-Wallet" -> Icons.Default.Smartphone
                            else -> Icons.Default.Payments
                        },
                        contentDescription = wallet.type,
                        tint = when (wallet.type) {
                            "Bank" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "E-Wallet" -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = wallet.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (wallet.accountNumber != "-") wallet.accountNumber else "Tunai",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Text(
                text = formatRupiah(wallet.balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Lihat Detail",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Detail",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailDialog(
    wallet: Wallet,
    debts: List<DebtItem>,
    receivables: List<ReceivableItem>,
    transactions: List<TransactionItem> = emptyList(),
    onDismiss: () -> Unit,
    onDeleteWallet: ((String) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${wallet.type} • ${wallet.accountNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_wallet_detail_fullscreen")) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Kembali"
                            )
                        }
                    },
                    actions = {
                        if (onDeleteWallet != null) {
                            IconButton(
                                onClick = { onDeleteWallet(wallet.id) },
                                modifier = Modifier.testTag("delete_wallet_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Wallet",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // 1. Hero Saldo Card
                item {
                    val totalDebtAmount = debts.sumOf { it.amount }
                    val totalReceivableAmount = receivables.sumOf { it.amount }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("full_wallet_balance_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SALDO AKTIF SAAT INI",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = wallet.type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = formatRupiah(wallet.balance),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total Utang (Talangan/Tanggungan)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = if (totalDebtAmount > 0) "- ${formatRupiah(totalDebtAmount)}" else "Rp 0",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalDebtAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Piutang (Talangan Diberikan)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = if (totalReceivableAmount > 0) "+ ${formatRupiah(totalReceivableAmount)}" else "Rp 0",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalReceivableAmount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Section Rincian Split / Utang (Debts)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Utang",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Catatan Utang / Talangan Diterima (${debts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (debts.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Tidak ada catatan utang / talangan di wallet ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            debts.forEach { debt ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(14.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = debt.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Harus dibayar ke: ${debt.targetWalletName} • ${debt.dueDate}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "- ${formatRupiah(debt.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Section Rincian Piutang (Receivables)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Piutang",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Catatan Piutang / Talangan Diberikan (${receivables.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        if (receivables.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Tidak ada catatan piutang / talangan yang diberikan dari wallet ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            receivables.forEach { receivable ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(14.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = receivable.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Talangan dari: ${receivable.sourceWalletName} • ${receivable.dueDate}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "+ ${formatRupiah(receivable.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Section Riwayat Transaksi Wallet Ini (Termasuk Split Nominal Details)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Riwayat Transaksi Wallet Ini (${transactions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (transactions.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Belum ada transaksi yang tercatat pada wallet ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }

                items(transactions) { tx ->
                    TransactionItemRow(transaction = tx)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("close_wallet_detail_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Tutup Detail Wallet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionItem,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (transaction.type) {
                                TransactionType.INCOME -> MaterialTheme.colorScheme.tertiaryContainer
                                TransactionType.EXPENSE -> MaterialTheme.colorScheme.errorContainer
                                TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (transaction.type) {
                            TransactionType.INCOME -> Icons.Default.CallReceived
                            TransactionType.EXPENSE -> Icons.Default.CallMade
                            TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                        },
                        contentDescription = transaction.category,
                        tint = when (transaction.type) {
                            TransactionType.INCOME -> MaterialTheme.colorScheme.onTertiaryContainer
                            TransactionType.EXPENSE -> MaterialTheme.colorScheme.onErrorContainer
                            TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = transaction.walletName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = transaction.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = when (transaction.type) {
                            TransactionType.INCOME -> "+ ${formatRupiah(transaction.amount)}"
                            TransactionType.EXPENSE -> "- ${formatRupiah(transaction.amount)}"
                            TransactionType.TRANSFER -> formatRupiah(transaction.amount)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (transaction.type) {
                            TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
                            TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                            TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondary
                        }
                    )

                    if (onEdit != null || onDelete != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onEdit != null) {
                                IconButton(
                                    onClick = onEdit,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("edit_tx_${transaction.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Transaksi",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            if (onDelete != null) {
                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("delete_tx_${transaction.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Transaksi",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (transaction.note.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Rincian Note",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    var txToEdit by remember { mutableStateOf<TransactionItem?>(null) }
    var txToDelete by remember { mutableStateOf<TransactionItem?>(null) }

    val filteredTransactions = remember(uiState.transactions, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.transactions
        } else {
            val query = uiState.searchQuery.trim().lowercase()
            uiState.transactions.filter {
                it.title.lowercase().contains(query) ||
                it.walletName.lowercase().contains(query) ||
                it.category.lowercase().contains(query) ||
                it.date.lowercase().contains(query) ||
                it.amount.toString().contains(query)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Semua Riwayat Transaksi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total ${uiState.transactions.size} transaksi tercatat di seluruh wallet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_history_input"),
                placeholder = {
                    Text("cari riwayat...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari Transaksi"
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Hapus Pencarian"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Search Result Indicator
        if (uiState.searchQuery.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ditemukan ${filteredTransactions.size} hasil untuk \"${uiState.searchQuery}\"",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { viewModel.setSearchQuery("") }) {
                        Text("Reset Filter")
                    }
                }
            }
        }

        // List Transactions or Empty State
        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "Tidak Ditemukan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Tidak Ada Transaksi Ditemukan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Coba kata kunci pencarian lain seperti 'BCA', 'Gaji', 'Listrik', atau 'Makan'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { transaction ->
                TransactionItemRow(
                    transaction = transaction,
                    onEdit = { txToEdit = transaction },
                    onDelete = { txToDelete = transaction }
                )
            }
        }
    }

    txToEdit?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            wallets = uiState.wallets,
            incomeCategories = uiState.incomeCategories,
            expenseCategories = uiState.expenseCategories,
            onDismiss = { txToEdit = null },
            onSave = { newTitle, newAmount, newCategory, newWalletId, newDate ->
                viewModel.editTransaction(
                    id = tx.id,
                    newTitle = newTitle,
                    newAmount = newAmount,
                    newCategory = newCategory,
                    newWalletId = newWalletId,
                    newDate = newDate
                )
                txToEdit = null
            }
        )
    }

    txToDelete?.let { tx ->
        ConfirmDeleteDialog(
            title = "Hapus Transaksi?",
            message = "Apakah Anda yakin ingin menghapus transaksi '${tx.title}' (${formatRupiah(tx.amount)})?",
            onDismiss = { txToDelete = null },
            onConfirm = {
                viewModel.deleteTransaction(tx.id)
                txToDelete = null
            }
        )
    }
}@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var walletToEdit by remember { mutableStateOf<Wallet?>(null) }
    var walletToDelete by remember { mutableStateOf<Wallet?>(null) }

    var categoryToEdit by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var categoryToDelete by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showResetDatabaseDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Pengaturan Aplikasi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Section: Kelola Dompet (Tambah, Edit, Hapus)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_wallet_setting_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Wallet",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column {
                                Text(
                                    text = "Kelola Dompet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${uiState.wallets.size} dompet aktif (Bisa Edit & Hapus)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = { showAddWalletDialog = true },
                            modifier = Modifier.testTag("open_add_wallet_dialog_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Dompet"
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Daftar Wallet dengan tombol Edit & Hapus
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.wallets.forEach { wallet ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wallet_setting_item_${wallet.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when (wallet.type) {
                                                        "Bank" -> MaterialTheme.colorScheme.primaryContainer
                                                        "E-Wallet" -> MaterialTheme.colorScheme.secondaryContainer
                                                        else -> MaterialTheme.colorScheme.tertiaryContainer
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (wallet.type) {
                                                    "Bank" -> Icons.Default.AccountBalance
                                                    "E-Wallet" -> Icons.Default.Smartphone
                                                    else -> Icons.Default.Payments
                                                },
                                                contentDescription = wallet.type,
                                                tint = when (wallet.type) {
                                                    "Bank" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    "E-Wallet" -> MaterialTheme.colorScheme.onSecondaryContainer
                                                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = wallet.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${wallet.type} • ${if (wallet.accountNumber != "-") wallet.accountNumber else "Tunai"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatRupiah(wallet.balance),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { walletToEdit = wallet },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("edit_wallet_${wallet.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Dompet",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { walletToDelete = wallet },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("delete_wallet_${wallet.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Dompet",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Kelola Kategori (Pemasukan & Pengeluaran)
        item {
            ManageCategoriesCard(
                incomeCategories = uiState.incomeCategories,
                expenseCategories = uiState.expenseCategories,
                onAddIncomeCategory = { viewModel.addIncomeCategory(it) },
                onAddExpenseCategory = { viewModel.addExpenseCategory(it) },
                onEditIncomeCategory = { cat -> categoryToEdit = Pair(cat, true) },
                onDeleteIncomeCategory = { cat -> categoryToDelete = Pair(cat, true) },
                onEditExpenseCategory = { cat -> categoryToEdit = Pair(cat, false) },
                onDeleteExpenseCategory = { cat -> categoryToDelete = Pair(cat, false) }
            )
        }

        // Section: Pengaturan Database (Reset, Backup, Restore)
        item {
            DatabaseManagementCard(
                onBackup = { showBackupDialog = true },
                onRestore = { showRestoreDialog = true },
                onReset = { showResetDatabaseDialog = true }
            )
        }
    }

    if (showAddWalletDialog) {
        AddEditWalletDialog(
            walletToEdit = null,
            onDismiss = { showAddWalletDialog = false },
            onSave = { name, balance, accNum, type ->
                viewModel.addWallet(name, balance, accNum, type)
                showAddWalletDialog = false
            }
        )
    }

    walletToEdit?.let { wallet ->
        AddEditWalletDialog(
            walletToEdit = wallet,
            onDismiss = { walletToEdit = null },
            onSave = { name, balance, accNum, type ->
                viewModel.editWallet(wallet.id, name, balance, accNum, type)
                walletToEdit = null
            }
        )
    }

    walletToDelete?.let { wallet ->
        ConfirmDeleteDialog(
            title = "Hapus Dompet?",
            message = "Apakah Anda yakin ingin menghapus dompet '${wallet.name}'?",
            onDismiss = { walletToDelete = null },
            onConfirm = {
                viewModel.deleteWallet(wallet.id)
                walletToDelete = null
            }
        )
    }

    categoryToEdit?.let { (catName, isIncome) ->
        EditCategoryDialog(
            categoryName = catName,
            isIncome = isIncome,
            onDismiss = { categoryToEdit = null },
            onSave = { newName ->
                if (isIncome) {
                    viewModel.editIncomeCategory(catName, newName)
                } else {
                    viewModel.editExpenseCategory(catName, newName)
                }
                categoryToEdit = null
            }
        )
    }

    categoryToDelete?.let { (catName, isIncome) ->
        ConfirmDeleteDialog(
            title = "Hapus Kategori?",
            message = "Apakah Anda yakin ingin menghapus kategori '$catName'?",
            onDismiss = { categoryToDelete = null },
            onConfirm = {
                if (isIncome) {
                    viewModel.deleteIncomeCategory(catName)
                } else {
                    viewModel.deleteExpenseCategory(catName)
                }
                categoryToDelete = null
            }
        )
    }

    if (showBackupDialog) {
        BackupDatabaseDialog(
            jsonString = viewModel.exportBackupJson(),
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showRestoreDialog) {
        RestoreDatabaseDialog(
            onDismiss = { showRestoreDialog = false },
            onRestore = { json ->
                viewModel.restoreDatabaseFromJson(json)
            }
        )
    }

    if (showResetDatabaseDialog) {
        ConfirmDeleteDialog(
            title = "Reset Seluruh Database?",
            message = "Perhatian: Semua data dompet, riwayat transaksi, utang, dan piutang akan dikembalikan ke kondisi awal. Apakah Anda yakin ingin melanjutkan?",
            onDismiss = { showResetDatabaseDialog = false },
            onConfirm = {
                viewModel.resetDatabase()
                showResetDatabaseDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionItem,
    wallets: List<Wallet>,
    incomeCategories: List<String>,
    expenseCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Long, category: String, walletId: String, date: String) -> Unit
) {
    var title by remember { mutableStateOf(transaction.title) }
    var amountStr by remember { mutableStateOf(transaction.amount.toString()) }
    var selectedWalletId by remember { mutableStateOf(transaction.walletId.ifBlank { wallets.firstOrNull()?.id ?: "" }) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var dateStr by remember { mutableStateOf(transaction.date) }

    val availableCategories = if (transaction.type == TransactionType.INCOME) incomeCategories else expenseCategories

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("edit_transaction_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Transaksi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul / Catatan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_tx_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Nominal (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_tx_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Tanggal & Waktu") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_tx_date_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Dompet Terkait:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    wallets.forEach { wallet ->
                        FilterChip(
                            selected = wallet.id == selectedWalletId,
                            onClick = { selectedWalletId = wallet.id },
                            label = { Text(wallet.name) }
                        )
                    }
                }

                if (availableCategories.isNotEmpty()) {
                    Text(
                        text = "Kategori:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableCategories.forEach { cat ->
                            FilterChip(
                                selected = cat == selectedCategory,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountStr.toLongOrNull() ?: 0L
                            onSave(title, amount, selectedCategory, selectedWalletId, dateStr)
                        },
                        enabled = title.isNotBlank() && (amountStr.toLongOrNull() ?: 0L) > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_edit_tx_button")
                    ) {
                        Text("Simpan Transaksi")
                    }
                }
            }
        }
    }
}

@Composable
fun DatabaseManagementCard(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("database_management_setting_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Database",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column {
                    Text(
                        text = "Database & Cadangan Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cadangkan, pulihkan, atau reset seluruh data aplikasi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBackup,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("backup_db_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Backup", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("restore_db_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontSize = 12.sp)
                }

                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_db_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun BackupDatabaseDialog(
    jsonString: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("backup_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backup Database (JSON)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Text(
                    text = "Salin data format JSON ini untuk disimpan sebagai cadangan:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        item {
                            Text(
                                text = jsonString,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (copied) {
                        Text(
                            text = "✓ Berhasil disalin!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(jsonString))
                            copied = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("copy_backup_json_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin JSON")
                    }
                }
            }
        }
    }
}

@Composable
fun RestoreDatabaseDialog(
    onDismiss: () -> Unit,
    onRestore: (jsonString: String) -> Boolean
) {
    var inputJson by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("restore_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Restore Database (JSON)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Text(
                    text = "Tempelkan teks cadangan format JSON yang valid untuk memulihkan data database Anda:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inputJson,
                    onValueChange = {
                        inputJson = it
                        errorMessage = null
                    },
                    placeholder = { Text("Tempelkan data JSON di sini...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("restore_json_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (inputJson.isBlank()) {
                                errorMessage = "Harap masukkan teks JSON terlebih dahulu."
                            } else {
                                val success = onRestore(inputJson)
                                if (success) {
                                    onDismiss()
                                } else {
                                    errorMessage = "Format JSON tidak valid. Pastikan data berasal dari hasil backup."
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_restore_button")
                    ) {
                        Text("Proses Restore")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditWalletDialog(
    walletToEdit: Wallet? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, balance: Long, accNumber: String, type: String) -> Unit
) {
    var walletName by remember { mutableStateOf(walletToEdit?.name ?: "") }
    var initialBalanceStr by remember { mutableStateOf(walletToEdit?.balance?.toString() ?: "") }
    var accountNumber by remember { mutableStateOf(walletToEdit?.accountNumber?.takeIf { it != "-" } ?: "") }
    var selectedType by remember { mutableStateOf(walletToEdit?.type ?: "Bank") }
    val types = listOf("Bank", "E-Wallet", "Cash")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("add_wallet_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (walletToEdit != null) "Edit Dompet" else "Tambah Dompet Baru",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                OutlinedTextField(
                    value = walletName,
                    onValueChange = { walletName = it },
                    label = { Text("Nama Dompet *") },
                    placeholder = { Text("Contoh: BCA Utama, Gopay, Kas Tunai") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_wallet_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = initialBalanceStr,
                    onValueChange = { initialBalanceStr = it.filter { char -> char.isDigit() } },
                    label = { Text("Saldo (Rp)") },
                    placeholder = { Text("Contoh: 1000000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_wallet_balance"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Nomor Rekening / HP (Opsional)") },
                    placeholder = { Text("Contoh: 08123456789 atau 12345678") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_wallet_acc_num"),
                    shape = RoundedCornerShape(12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Tipe Dompet",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type) },
                                leadingIcon = if (selectedType == type) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val balance = initialBalanceStr.toLongOrNull() ?: 0L
                            onSave(walletName, balance, accountNumber, selectedType)
                        },
                        enabled = walletName.isNotBlank(),
                        modifier = Modifier.testTag("save_wallet_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (walletToEdit != null) "Simpan Perubahan" else "Simpan Dompet")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageCategoriesCard(
    incomeCategories: List<String>,
    expenseCategories: List<String>,
    onAddIncomeCategory: (String) -> Unit,
    onAddExpenseCategory: (String) -> Unit,
    onEditIncomeCategory: (String) -> Unit,
    onDeleteIncomeCategory: (String) -> Unit,
    onEditExpenseCategory: (String) -> Unit,
    onDeleteExpenseCategory: (String) -> Unit
) {
    var newIncomeCategoryName by remember { mutableStateOf("") }
    var newExpenseCategoryName by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("manage_categories_setting_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Kategori",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column {
                    Text(
                        text = "Kelola Kategori",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tambah, edit nama, atau hapus kategori transaksi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Sub-Section 1: Kategori Pemasukan
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Pemasukan",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Kategori Pemasukan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Existing Income Category Chips with Edit & Delete actions
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    incomeCategories.forEach { category ->
                        InputChip(
                            selected = false,
                            onClick = { onEditIncomeCategory(category) },
                            label = { Text(category) },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit $category",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onEditIncomeCategory(category) },
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus $category",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onDeleteIncomeCategory(category) },
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                // Add Income Category Form Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newIncomeCategoryName,
                        onValueChange = { newIncomeCategoryName = it },
                        placeholder = { Text("Kategori Pemasukan Baru") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_new_income_category"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (newIncomeCategoryName.isNotBlank()) {
                                onAddIncomeCategory(newIncomeCategoryName)
                                newIncomeCategoryName = ""
                            }
                        },
                        enabled = newIncomeCategoryName.isNotBlank(),
                        modifier = Modifier.testTag("add_income_category_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tambah")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Sub-Section 2: Kategori Pengeluaran
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Pengeluaran",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Kategori Pengeluaran",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Existing Expense Category Chips with Edit & Delete actions
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    expenseCategories.forEach { category ->
                        InputChip(
                            selected = false,
                            onClick = { onEditExpenseCategory(category) },
                            label = { Text(category) },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit $category",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onEditExpenseCategory(category) },
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus $category",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onDeleteExpenseCategory(category) },
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                // Add Expense Category Form Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newExpenseCategoryName,
                        onValueChange = { newExpenseCategoryName = it },
                        placeholder = { Text("Kategori Pengeluaran Baru") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_new_expense_category"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (newExpenseCategoryName.isNotBlank()) {
                                onAddExpenseCategory(newExpenseCategoryName)
                                newExpenseCategoryName = ""
                            }
                        },
                        enabled = newExpenseCategoryName.isNotBlank(),
                        modifier = Modifier.testTag("add_expense_category_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tambah")
                    }
                }
            }
        }
    }
}

@Composable
fun EditCategoryDialog(
    categoryName: String,
    isIncome: Boolean,
    onDismiss: () -> Unit,
    onSave: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(categoryName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("edit_category_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isIncome) "Edit Kategori Pemasukan" else "Edit Kategori Pengeluaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_category_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name) },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan Perubahan")
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DraggableFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .testTag("draggable_fab"),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Tambah Floating",
            modifier = Modifier.size(28.dp)
        )
    }
}

fun formatThousandSeparator(amount: Long): String {
    if (amount == 0L) return ""
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID"))
    return formatter.format(amount)
}

@Composable
fun SelectionFieldCard(
    label: String,
    value: String,
    placeholder: String = "Pilih",
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value.ifBlank { placeholder },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (value.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                        color = if (value.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Pilih",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class PickerOption(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleSelectPickerModal(
    title: String,
    options: List<PickerOption>,
    selectedId: String,
    onOptionSelected: (PickerOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options) { option ->
                    val isSelected = option.id == selectedId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                option.icon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    option.subtitle?.let { sub ->
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

enum class PickerType {
    INCOME_WALLET,
    INCOME_CATEGORY,
    EXPENSE_WALLET,
    EXPENSE_CATEGORY,
    BAILOUT_WALLET,
    TRANSFER_MODE,
    SOURCE_WALLET,
    DEST_WALLET
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionFullScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pemasukan, 1: Pengeluaran, 2: Transfer
    val tabs = listOf("Pemasukan", "Pengeluaran", "Transfer")

    // Common fields
    var amountRaw by remember { mutableLongStateOf(0L) }
    var note by remember { mutableStateOf("") }
    val currentDateStr = remember { com.example.getCurrentFormattedDate() }

    // Picker state
    var activePickerType by remember { mutableStateOf<PickerType?>(null) }

    // Pemasukan state
    var incomeWalletId by remember { mutableStateOf(uiState.wallets.firstOrNull()?.id ?: "") }
    var incomeCategory by remember { mutableStateOf(uiState.incomeCategories.firstOrNull() ?: "Gaji") }

    // Pengeluaran state
    var expenseWalletId by remember { mutableStateOf(uiState.wallets.firstOrNull()?.id ?: "") }
    var expenseCategory by remember { mutableStateOf(uiState.expenseCategories.firstOrNull() ?: "Makanan & Minuman") }
    var bailoutWalletId by remember { mutableStateOf(uiState.wallets.getOrNull(1)?.id ?: "") }

    // Transfer state
    var transferMode by remember { mutableStateOf("Pemindahan Dana") }
    val transferModes = listOf("Pemindahan Dana", "Bayar Utang", "Piutang (Meminjamkan)")
    var sourceWalletId by remember { mutableStateOf(uiState.wallets.firstOrNull()?.id ?: "") }
    var destinationWalletId by remember { mutableStateOf(uiState.wallets.getOrNull(1)?.id ?: "") }

    // Selected objects
    val selectedIncomeWallet = uiState.wallets.find { it.id == incomeWalletId }
    val selectedExpenseWallet = uiState.wallets.find { it.id == expenseWalletId }
    val selectedBailoutWallet = uiState.wallets.find { it.id == bailoutWalletId }
    val selectedSourceWallet = uiState.wallets.find { it.id == sourceWalletId }
    val selectedDestWallet = uiState.wallets.find { it.id == destinationWalletId }

    val expenseWalletBalance = selectedExpenseWallet?.balance ?: 0L
    val isSplitExpense = selectedTab == 1 && amountRaw > expenseWalletBalance

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Tambah Transaksi",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            when (selectedTab) {
                                0 -> { // PEMASUKAN
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        // 1. Pilih Dompet (Button)
                                        SelectionFieldCard(
                                            label = "Pilih Dompet",
                                            value = selectedIncomeWallet?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Dompet",
                                            icon = Icons.Default.AccountBalanceWallet,
                                            onClick = { activePickerType = PickerType.INCOME_WALLET },
                                            testTag = "btn_select_income_wallet"
                                        )

                                        // 2. Input Nominal
                                        OutlinedTextField(
                                            value = if (amountRaw == 0L) "" else formatThousandSeparator(amountRaw),
                                            onValueChange = { input ->
                                                val digits = input.filter { it.isDigit() }
                                                amountRaw = digits.toLongOrNull() ?: 0L
                                            },
                                            label = { Text("Nominal Pemasukan (Rp) *") },
                                            placeholder = { Text("0") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().testTag("input_income_amount"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        // 3. Kategori Pemasukan (Button)
                                        SelectionFieldCard(
                                            label = "Kategori Pemasukan",
                                            value = incomeCategory,
                                            icon = Icons.Default.Category,
                                            onClick = { activePickerType = PickerType.INCOME_CATEGORY },
                                            testTag = "btn_select_income_category"
                                        )

                                        // 4. Catatan / Note
                                        OutlinedTextField(
                                            value = note,
                                            onValueChange = { note = it },
                                            label = { Text("Catatan / Note") },
                                            placeholder = { Text("Contoh: Gaji Bulan Ini") },
                                            modifier = Modifier.fillMaxWidth().testTag("input_income_note"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        // 5. Tanggal & Waktu Otomatis
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Tanggal & Waktu Otomatis: $currentDateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Submit Button
                                        Button(
                                            onClick = {
                                                viewModel.addIncome(
                                                    walletId = incomeWalletId,
                                                    amount = amountRaw,
                                                    category = incomeCategory,
                                                    note = note
                                                )
                                                onDismiss()
                                            },
                                            enabled = amountRaw > 0 && incomeWalletId.isNotBlank(),
                                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_income_button"),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Simpan Pemasukan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                1 -> { // PENGELUARAN
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        // 1. Pilih Dompet (Button)
                                        SelectionFieldCard(
                                            label = "Pilih Dompet",
                                            value = selectedExpenseWallet?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Dompet",
                                            icon = Icons.Default.AccountBalanceWallet,
                                            onClick = { activePickerType = PickerType.EXPENSE_WALLET },
                                            testTag = "btn_select_expense_wallet"
                                        )

                                        // 2. Input Nominal
                                        OutlinedTextField(
                                            value = if (amountRaw == 0L) "" else formatThousandSeparator(amountRaw),
                                            onValueChange = { input ->
                                                val digits = input.filter { it.isDigit() }
                                                amountRaw = digits.toLongOrNull() ?: 0L
                                            },
                                            label = { Text("Nominal Pengeluaran (Rp) *") },
                                            placeholder = { Text("0") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().testTag("input_expense_amount"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        // Split Mode Alert & Button
                                        if (isSplitExpense) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                                ),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Text(
                                                        text = "⚠️ Nominal melebihi saldo dompet (${formatRupiah(expenseWalletBalance)}). Mode split otomatis aktif!",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.error
                                                    )

                                                    SelectionFieldCard(
                                                        label = "Pilih Dompet Talangan",
                                                        value = selectedBailoutWallet?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Dompet Talangan",
                                                        icon = Icons.Default.Payments,
                                                        onClick = { activePickerType = PickerType.BAILOUT_WALLET },
                                                        testTag = "btn_select_bailout_wallet"
                                                    )

                                                    val bailoutAmount = amountRaw - expenseWalletBalance
                                                    Text(
                                                        text = "Status: Punya utang sebesar ${formatRupiah(bailoutAmount)} ke ${selectedBailoutWallet?.name ?: "Dompet Talangan"}.",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        }

                                        // 3. Kategori Pengeluaran (Button)
                                        SelectionFieldCard(
                                            label = "Kategori Pengeluaran",
                                            value = expenseCategory,
                                            icon = Icons.Default.Category,
                                            onClick = { activePickerType = PickerType.EXPENSE_CATEGORY },
                                            testTag = "btn_select_expense_category"
                                        )

                                        // 4. Catatan / Note
                                        OutlinedTextField(
                                            value = note,
                                            onValueChange = { note = it },
                                            label = { Text("Catatan / Note") },
                                            placeholder = { Text("Contoh: Beli Bahan Makanan") },
                                            modifier = Modifier.fillMaxWidth().testTag("input_expense_note"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        // 5. Tanggal & Waktu Otomatis
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Tanggal & Waktu Otomatis: $currentDateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Submit Button
                                        Button(
                                            onClick = {
                                                viewModel.addExpense(
                                                    walletId = expenseWalletId,
                                                    amount = amountRaw,
                                                    category = expenseCategory,
                                                    note = note,
                                                    bailoutWalletId = if (isSplitExpense) bailoutWalletId else null
                                                )
                                                onDismiss()
                                            },
                                            enabled = amountRaw > 0 && expenseWalletId.isNotBlank(),
                                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_expense_button"),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text(if (isSplitExpense) "Simpan Pengeluaran (Split)" else "Simpan Pengeluaran", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                2 -> { // TRANSFER
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        // 1. Input Nominal
                                        OutlinedTextField(
                                            value = if (amountRaw == 0L) "" else formatThousandSeparator(amountRaw),
                                            onValueChange = { input ->
                                                val digits = input.filter { it.isDigit() }
                                                amountRaw = digits.toLongOrNull() ?: 0L
                                            },
                                            label = { Text("Nominal Transfer (Rp) *") },
                                            placeholder = { Text("0") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().testTag("input_transfer_amount"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        // 2. Mode Transfer (Button)
                                        SelectionFieldCard(
                                            label = "Pilih Mode Transfer",
                                            value = transferMode,
                                            icon = Icons.Default.SwapHoriz,
                                            onClick = { activePickerType = PickerType.TRANSFER_MODE },
                                            testTag = "btn_select_transfer_mode"
                                        )

                                        // 3. Dompet Asal (Button)
                                        SelectionFieldCard(
                                            label = "Pilih Dompet Asal",
                                            value = selectedSourceWallet?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Dompet Asal",
                                            icon = Icons.Default.AccountBalanceWallet,
                                            onClick = { activePickerType = PickerType.SOURCE_WALLET },
                                            testTag = "btn_select_source_wallet"
                                        )

                                        // 4. Dompet Tujuan (Button)
                                        SelectionFieldCard(
                                            label = "Pilih Dompet Tujuan",
                                            value = selectedDestWallet?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Dompet Tujuan",
                                            icon = Icons.Default.AccountBalance,
                                            onClick = { activePickerType = PickerType.DEST_WALLET },
                                            testTag = "btn_select_dest_wallet"
                                        )

                                        // 5. Catatan / Note
                                        OutlinedTextField(
                                            value = note,
                                            onValueChange = { note = it },
                                            label = { Text("Catatan / Note") },
                                            placeholder = { Text("Contoh: Transfer ke Rekening BCA") },
                                            modifier = Modifier.fillMaxWidth().testTag("input_transfer_note"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        // 6. Tanggal & Waktu Otomatis
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Tanggal & Waktu Otomatis: $currentDateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Submit Button
                                        Button(
                                            onClick = {
                                                viewModel.addTransfer(
                                                    amount = amountRaw,
                                                    transferMode = transferMode,
                                                    sourceWalletId = sourceWalletId,
                                                    destinationWalletId = destinationWalletId,
                                                    note = note
                                                )
                                                onDismiss()
                                            },
                                            enabled = amountRaw > 0 && sourceWalletId.isNotBlank() && destinationWalletId.isNotBlank() && sourceWalletId != destinationWalletId,
                                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_transfer_button"),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Simpan Transfer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet Handler according to activePickerType
    when (activePickerType) {
        PickerType.INCOME_WALLET -> {
            SingleSelectPickerModal(
                title = "Pilih Dompet Pemasukan",
                options = uiState.wallets.map { w ->
                    PickerOption(id = w.id, title = w.name, subtitle = "Saldo: ${formatRupiah(w.balance)}", icon = Icons.Default.AccountBalanceWallet)
                },
                selectedId = incomeWalletId,
                onOptionSelected = { incomeWalletId = it.id },
                onDismiss = { activePickerType = null }
            )
        }
        PickerType.INCOME_CATEGORY -> {
            SingleSelectPickerModal(
                title = "Pilih Kategori Pemasukan",
                options = uiState.incomeCategories.map { cat ->
                    PickerOption(id = cat, title = cat, icon = Icons.Default.Category)
                },
                selectedId = incomeCategory,
                onOptionSelected = { incomeCategory = it.id },
                onDismiss = { activePickerType = null }
            )
        }
        PickerType.EXPENSE_WALLET -> {
            SingleSelectPickerModal(
                title = "Pilih Dompet Pengeluaran",
                options = uiState.wallets.map { w ->
                    PickerOption(id = w.id, title = w.name, subtitle = "Saldo: ${formatRupiah(w.balance)}", icon = Icons.Default.AccountBalanceWallet)
                },
                selectedId = expenseWalletId,
                onOptionSelected = { expenseWalletId = it.id },
                onDismiss = { activePickerType = null }
            )
        }
        PickerType.EXPENSE_CATEGORY -> {
            SingleSelectPickerModal(
                title = "Pilih Kategori Pengeluaran",
                options = uiState.expenseCategories.map { cat ->
                    PickerOption(id = cat, title = cat, icon = Icons.Default.Category)
                },
                selectedId = expenseCategory,
                onOptionSelected = { expenseCategory = it.id },
                onDismiss = { activePickerType = null }
            )
        }
        PickerType.BAILOUT_WALLET -> {
            SingleSelectPickerModal(
                title = "Pilih Dompet Talangan",
                options = uiState.wallets.filter { it.id != expenseWalletId }.map { w ->
                    PickerOption(id = w.id, title = w.name, subtitle = "Saldo: ${formatRupiah(w.balance)}", icon = Icons.Default.Payments)
                },
                selectedId = bailoutWalletId ?: "",
                onOptionSelected = { bailoutWalletId = it.id },
                onDismiss = { activePickerType = null }
            )
        }
        PickerType.TRANSFER_MODE -> {
            SingleSelectPickerModal(
                title = "Pilih Mode Transfer",
                options = transferModes.map { mode ->
                    PickerOption(id = mode, title = mode, icon = Icons.Default.SwapHoriz)
                },
                selectedId = transferMode,
                onOptionSelected = { transferMode = it.id },
                onDismiss = { activePickerType = null }
            )
        }
        PickerType.SOURCE_WALLET -> {
            SingleSelectPickerModal(
                title = "Pilih Dompet Asal",
                options = uiState.wallets.map { w ->
                    PickerOption(id = w.id, title = w.name, subtitle = "Saldo: ${formatRupiah(w.balance)}", icon = Icons.Default.AccountBalanceWallet)
                },
                selectedId = sourceWalletId,
                onOptionSelected = {
                    sourceWalletId = it.id
                    if (destinationWalletId == it.id) {
                        destinationWalletId = uiState.wallets.firstOrNull { w -> w.id != it.id }?.id ?: ""
                    }
                },
                onDismiss = { activePickerType = null }
            )
        }
        PickerType.DEST_WALLET -> {
            SingleSelectPickerModal(
                title = "Pilih Dompet Tujuan",
                options = uiState.wallets.filter { it.id != sourceWalletId }.map { w ->
                    PickerOption(id = w.id, title = w.name, subtitle = "Saldo: ${formatRupiah(w.balance)}", icon = Icons.Default.AccountBalance)
                },
                selectedId = destinationWalletId,
                onOptionSelected = { destinationWalletId = it.id },
                onDismiss = { activePickerType = null }
            )
        }
        null -> {}
    }
}

