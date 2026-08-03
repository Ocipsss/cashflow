package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
                        Column {
                            Text(
                                text = "Dompet Digital & Keuangan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (uiState.selectedTab) {
                                    0 -> "Riwayat Transaksi"
                                    1 -> "Dashboard Utama"
                                    else -> "Pengaturan Aplikasi"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_item_history")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_item_dashboard")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Setting") },
                    label = { Text("Setting") },
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

    // Modal / Dialog Detail Wallet saat card wallet di-klik
    uiState.selectedWalletDetail?.let { selectedWallet ->
        WalletDetailDialog(
            wallet = selectedWallet,
            debts = uiState.debts.filter { it.walletId == selectedWallet.id },
            receivables = uiState.receivables.filter { it.walletId == selectedWallet.id },
            onDismiss = { viewModel.selectWalletForDetail(null) }
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
                            text = "TOTAL SALDO ALL WALLETS",
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

@Composable
fun WalletDetailDialog(
    wallet: Wallet,
    debts: List<DebtItem>,
    receivables: List<ReceivableItem>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("wallet_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title Header
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (wallet.type) {
                                    "Bank" -> Icons.Default.AccountBalance
                                    "E-Wallet" -> Icons.Default.Smartphone
                                    else -> Icons.Default.Payments
                                },
                                contentDescription = wallet.type,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${wallet.type} • ${wallet.accountNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Balance Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Total Saldo Wallet Ini",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatRupiah(wallet.balance),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Daftar Utang
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Utang",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Utang (Ke Wallet / Target Lain)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (debts.isEmpty()) {
                        Text(
                            text = "Tidak ada catatan utang di wallet ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                        )
                    } else {
                        debts.forEach { debt ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = debt.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Utang ke: ${debt.targetWalletName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "- ${formatRupiah(debt.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Daftar Piutang
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Piutang",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Piutang (Dari Wallet / Target Lain)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    if (receivables.isEmpty()) {
                        Text(
                            text = "Tidak ada catatan piutang di wallet ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                        )
                    } else {
                        receivables.forEach { receivable ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = receivable.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Piutang dari: ${receivable.sourceWalletName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "+ ${formatRupiah(receivable.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_wallet_detail_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tutup Detail")
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(transaction: TransactionItem) {
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
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
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
        }
    }
}

@Composable
fun HistoryTab(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
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
                    Text("Cari judul, wallet, kategori, atau tanggal...")
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
                TransactionItemRow(transaction = transaction)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    var showAddWalletDialog by remember { mutableStateOf(false) }

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

        // Section: Tambah Dompet
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
                                    text = "Tambah Dompet Baru",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Saat ini ada ${uiState.wallets.size} dompet aktif",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showAddWalletDialog = true },
                            modifier = Modifier.testTag("open_add_wallet_dialog_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Dompet",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Preview Chips of Wallets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.wallets.forEach { wallet ->
                            AssistChip(
                                onClick = { viewModel.selectWalletForDetail(wallet) },
                                label = { Text(wallet.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (wallet.type) {
                                            "Bank" -> Icons.Default.AccountBalance
                                            "E-Wallet" -> Icons.Default.Smartphone
                                            else -> Icons.Default.Payments
                                        },
                                        contentDescription = wallet.type,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section: Tambah Kategori (Pemasukan & Pengeluaran)
        item {
            ManageCategoriesCard(
                incomeCategories = uiState.incomeCategories,
                expenseCategories = uiState.expenseCategories,
                onAddIncomeCategory = { viewModel.addIncomeCategory(it) },
                onAddExpenseCategory = { viewModel.addExpenseCategory(it) }
            )
        }
    }

    if (showAddWalletDialog) {
        AddWalletDialog(
            onDismiss = { showAddWalletDialog = false },
            onSave = { name, balance, accNum, type ->
                viewModel.addWallet(name, balance, accNum, type)
                showAddWalletDialog = false
            }
        )
    }
}

@Composable
fun AddWalletDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, balance: Long, accNumber: String, type: String) -> Unit
) {
    var walletName by remember { mutableStateOf("") }
    var initialBalanceStr by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Bank") }
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
                        text = "Tambah Dompet Baru",
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
                    label = { Text("Saldo Awal (Rp)") },
                    placeholder = { Text("Contoh: 1000000") },
                    singleLine = true,
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
                        Text("Simpan Dompet")
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
    onAddExpenseCategory: (String) -> Unit
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
                        text = "Tambah Kategori",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Atur kategori transaksi pemasukan & pengeluaran",
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

                // Existing Income Category Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    incomeCategories.forEach { category ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(category) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
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

                // Existing Expense Category Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    expenseCategories.forEach { category ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(category) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
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

