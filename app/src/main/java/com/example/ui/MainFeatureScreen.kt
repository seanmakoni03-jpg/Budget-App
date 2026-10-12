package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankAccount
import com.example.data.remote.OpenBankingService
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SlateText
import com.example.ui.theme.SlateTextDim
import com.example.ui.theme.SlateTextMuted
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    viewModel: BudgetViewModel,
    onSignOutRequested: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val accountsState by viewModel.bankAccountsState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val summaryState by viewModel.spendingSummaryState.collectAsStateWithLifecycle()

    // Modals state
    var showBankDirectoryDialog by remember { mutableStateOf(false) }
    var showLogTransactionDialog by remember { mutableStateOf(false) }
    var selectedAccountForCsv by remember { mutableStateOf<BankAccount?>(null) }

    val currentUser = Firebase.auth.currentUser
    val userDisplayName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VaultFlow",
                        style = MaterialTheme.typography.titleLarge,
                        color = SlateText
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            signOutUser(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOutRequested,
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = SlateTextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF03140F),
                contentColor = SlateText,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = EmeraldBorder.copy(alpha = 0.5f))
            ) {
                NavigationBarItem(
                    selected = currentTab == BudgetAppTab.OVERVIEW,
                    onClick = { viewModel.selectTab(BudgetAppTab.OVERVIEW) },
                    icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Overview") },
                    label = { Text("Overview", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF022C22),
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary,
                        unselectedIconColor = SlateTextDim,
                        unselectedTextColor = SlateTextDim
                    )
                )

                NavigationBarItem(
                    selected = currentTab == BudgetAppTab.ACCOUNTS,
                    onClick = { viewModel.selectTab(BudgetAppTab.ACCOUNTS) },
                    icon = { Icon(imageVector = Icons.Default.AccountBalance, contentDescription = "Bank Vaults") },
                    label = { Text("Vaults", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF022C22),
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary,
                        unselectedIconColor = SlateTextDim,
                        unselectedTextColor = SlateTextDim
                    )
                )

                NavigationBarItem(
                    selected = currentTab == BudgetAppTab.TRANSACTIONS,
                    onClick = { viewModel.selectTab(BudgetAppTab.TRANSACTIONS) },
                    icon = { Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Feed") },
                    label = { Text("Feed", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF022C22),
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary,
                        unselectedIconColor = SlateTextDim,
                        unselectedTextColor = SlateTextDim
                    )
                )

                NavigationBarItem(
                    selected = currentTab == BudgetAppTab.BUDGETS,
                    onClick = { viewModel.selectTab(BudgetAppTab.BUDGETS) },
                    icon = { Icon(imageVector = Icons.Default.Savings, contentDescription = "Envelopes") },
                    label = { Text("Budgets", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF022C22),
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary,
                        unselectedIconColor = SlateTextDim,
                        unselectedTextColor = SlateTextDim
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentTab == BudgetAppTab.OVERVIEW || currentTab == BudgetAppTab.TRANSACTIONS) {
                FloatingActionButton(
                    onClick = { showLogTransactionDialog = true },
                    containerColor = EmeraldPrimary,
                    contentColor = Color(0xFF022C22),
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_log_transaction")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Record Spend")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tabTransition") { tab ->
                when (tab) {
                    BudgetAppTab.OVERVIEW -> {
                        DashboardOverviewScreen(
                            summaryState = summaryState,
                            accountsState = accountsState,
                            onConnectBankClicked = { showBankDirectoryDialog = true },
                            onAddTransactionClicked = { showLogTransactionDialog = true },
                            onViewAllAccounts = { viewModel.selectTab(BudgetAppTab.ACCOUNTS) },
                            onViewAllTransactions = { viewModel.selectTab(BudgetAppTab.TRANSACTIONS) },
                            onManageBudgetsClicked = { viewModel.selectTab(BudgetAppTab.BUDGETS) }
                        )
                    }
                    BudgetAppTab.ACCOUNTS -> {
                        BankAccountsScreen(
                            accountsState = accountsState,
                            isSyncing = isSyncing,
                            onConnectBankClicked = { showBankDirectoryDialog = true },
                            onImportCsvClicked = { acc -> selectedAccountForCsv = acc },
                            onDeleteAccountClicked = { acc -> viewModel.deleteBankAccount(acc) },
                            onCreateManualAccount = { inst, name, type, mask, routing, bal, curr ->
                                viewModel.createManualAccount(inst, name, type, mask, routing, bal, curr)
                            }
                        )
                    }
                    BudgetAppTab.TRANSACTIONS -> {
                        TransactionsScreen(
                            transactionsState = transactionsState,
                            accountsState = accountsState,
                            onAddTransactionClicked = { showLogTransactionDialog = true },
                            onDeleteTransactionClicked = { tx -> viewModel.deleteTransaction(tx) }
                        )
                    }
                    BudgetAppTab.BUDGETS -> {
                        BudgetsScreen(
                            summaryState = summaryState,
                            onSaveBudget = { cat, limit, thresh, roll ->
                                viewModel.saveBudget(cat, limit, thresh, roll)
                            },
                            onDeleteBudget = { id -> viewModel.deleteBudget(id) }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showBankDirectoryDialog) {
        ConnectBankDirectoryDialog(
            directory = OpenBankingService.localBankDirectory,
            onDismiss = { showBankDirectoryDialog = false },
            onSelectBank = { bank ->
                viewModel.connectLocalBank(bank)
                showBankDirectoryDialog = false
            }
        )
    }

    if (showLogTransactionDialog) {
        val accounts = (accountsState as? UiState.Success)?.data ?: emptyList()
        LogTransactionDialog(
            accounts = accounts,
            onDismiss = { showLogTransactionDialog = false },
            onConfirm = { acc, merchant, cat, amount, isExp, note, date, recur ->
                viewModel.logTransaction(acc, merchant, cat, amount, isExp, note, date, recur)
                showLogTransactionDialog = false
            }
        )
    }

    if (selectedAccountForCsv != null) {
        ImportCsvDialog(
            targetAccount = selectedAccountForCsv!!,
            onDismiss = { selectedAccountForCsv = null },
            onConfirmImport = { csv ->
                viewModel.importCsvStatement(selectedAccountForCsv!!, csv)
                selectedAccountForCsv = null
            }
        )
    }
}
