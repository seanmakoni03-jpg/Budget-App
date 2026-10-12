package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountType
import com.example.data.model.BankAccount
import com.example.data.model.BudgetEnvelope
import com.example.data.model.LocalBankDirectoryItem
import com.example.data.model.SpendingCategory
import com.example.data.model.TransactionRecord
import com.example.data.remote.OpenBankingService
import com.example.data.remote.ParsedCsvRow
import com.example.data.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class SpendingSummary(
    val totalBalanceCents: Long = 0L,
    val monthlySpentCents: Long = 0L,
    val monthlyIncomeCents: Long = 0L,
    val netCashFlowCents: Long = 0L,
    val categorySpending: Map<SpendingCategory, Long> = emptyMap(),
    val budgetProgressList: List<BudgetProgressItem> = emptyList(),
    val recentTransactions: List<TransactionRecord> = emptyList(),
    val totalAccountsCount: Int = 0
)

data class BudgetProgressItem(
    val budget: BudgetEnvelope,
    val spentCents: Long,
    val remainingCents: Long,
    val percentageUsed: Float,
    val isAlertTriggered: Boolean
)

enum class BudgetAppTab(val label: String) {
    OVERVIEW("Overview"),
    ACCOUNTS("Bank Vaults"),
    TRANSACTIONS("Spending Feed"),
    BUDGETS("Envelopes")
}

class BudgetViewModel(
    private val repository: BudgetRepository,
    val currentUserId: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(BudgetAppTab.OVERVIEW)
    val selectedTab: StateFlow<BudgetAppTab> = _selectedTab.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // 1. Bank Accounts Stream
    val bankAccountsState: StateFlow<UiState<List<BankAccount>>> = repository.observeBankAccounts(currentUserId)
        .map<List<BankAccount>, UiState<List<BankAccount>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to observe bank accounts")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 2. Transactions Stream
    val transactionsState: StateFlow<UiState<List<TransactionRecord>>> = repository.observeTransactions(currentUserId)
        .map<List<TransactionRecord>, UiState<List<TransactionRecord>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to observe transactions")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 3. Budgets Stream
    val budgetsState: StateFlow<UiState<List<BudgetEnvelope>>> = repository.observeBudgets(currentUserId)
        .map<List<BudgetEnvelope>, UiState<List<BudgetEnvelope>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to observe budgets")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 4. Combined Spending Telemetry & Summary Stream
    val spendingSummaryState: StateFlow<UiState<SpendingSummary>> = combine(
        repository.observeBankAccounts(currentUserId),
        repository.observeTransactions(currentUserId),
        repository.observeBudgets(currentUserId)
    ) { accounts, txs, budgets ->
        val totalBalance = accounts.sumOf { it.balanceCents }

        // Filter transactions for current calendar month
        val currentYearMonth = BudgetRepository.currentIsoDate().take(7)
        val monthTxs = txs.filter { it.transactionDateIso.startsWith(currentYearMonth) }

        var monthSpent = 0L
        var monthIncome = 0L
        val categoryMap = mutableMapOf<SpendingCategory, Long>()

        for (tx in monthTxs) {
            val cat = SpendingCategory.fromString(tx.category)
            if (tx.isExpense) {
                monthSpent += tx.amountCents
                categoryMap[cat] = (categoryMap[cat] ?: 0L) + tx.amountCents
            } else {
                monthIncome += tx.amountCents
            }
        }

        val budgetProgress = budgets.map { b ->
            val cat = SpendingCategory.fromString(b.category)
            val spent = categoryMap[cat] ?: 0L
            val limit = b.monthlyLimitCents.coerceAtLeast(1L)
            val ratio = spent.toFloat() / limit.toFloat()
            val remaining = (limit - spent).coerceAtLeast(0L)
            val alertThresholdRatio = b.alertThresholdPercent.toFloat() / 100f
            BudgetProgressItem(
                budget = b,
                spentCents = spent,
                remainingCents = remaining,
                percentageUsed = (ratio * 100f).coerceIn(0f, 200f),
                isAlertTriggered = ratio >= alertThresholdRatio
            )
        }

        SpendingSummary(
            totalBalanceCents = totalBalance,
            monthlySpentCents = monthSpent,
            monthlyIncomeCents = monthIncome,
            netCashFlowCents = monthIncome - monthSpent,
            categorySpending = categoryMap,
            budgetProgressList = budgetProgress,
            recentTransactions = txs.take(15),
            totalAccountsCount = accounts.size
        )
    }
        .map<SpendingSummary, UiState<SpendingSummary>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to compute financial metrics")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        // Automatically check if starter budgets should be seeded
        viewModelScope.launch {
            repository.observeBudgets(currentUserId).collect { existing ->
                if (existing.isEmpty()) {
                    repository.seedStarterBudgetsIfEmpty(existing)
                }
            }
        }
    }

    fun selectTab(tab: BudgetAppTab) {
        _selectedTab.value = tab
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun connectLocalBank(bankItem: LocalBankDirectoryItem) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.linkInstitutionFromDirectory(bankItem)
            _isSyncing.value = false
            result.onSuccess { count ->
                _userMessage.value = "Connected ${bankItem.institutionName}! Synced $count live transactions."
            }.onFailure { err ->
                _userMessage.value = "Failed to link bank: ${err.localizedMessage}"
            }
        }
    }

    fun importCsvStatement(account: BankAccount, rawCsv: String) {
        viewModelScope.launch {
            val parsed = OpenBankingService.parseBankStatementCsv(rawCsv)
            if (parsed.isEmpty()) {
                _userMessage.value = "No valid transactions found in CSV. Expected: Date,Merchant,Category,Amount"
                return@launch
            }
            _isSyncing.value = true
            val result = repository.importCsvTransactions(account, parsed)
            _isSyncing.value = false
            result.onSuccess { count ->
                _userMessage.value = "Successfully imported $count transactions into ${account.accountName}!"
            }.onFailure { err ->
                _userMessage.value = "CSV import error: ${err.localizedMessage}"
            }
        }
    }

    fun createManualAccount(
        institution: String,
        name: String,
        type: AccountType,
        mask: String,
        routing: String,
        balanceCents: Long,
        currency: String
    ) {
        viewModelScope.launch {
            repository.createBankAccount(
                institutionName = institution,
                accountName = name,
                accountType = type,
                accountMask = mask,
                routingCode = routing,
                balanceCents = balanceCents,
                currency = currency
            ).onSuccess {
                _userMessage.value = "Account $name created successfully."
            }.onFailure {
                _userMessage.value = "Failed to create account: ${it.localizedMessage}"
            }
        }
    }

    fun logTransaction(
        account: BankAccount,
        merchant: String,
        category: SpendingCategory,
        amountCents: Long,
        isExpense: Boolean,
        note: String,
        dateIso: String,
        isRecurring: Boolean
    ) {
        viewModelScope.launch {
            repository.recordTransaction(
                accountId = account.id,
                accountName = "${account.institutionName} ••${account.accountMask}",
                merchantName = merchant,
                category = category,
                amountCents = amountCents,
                isExpense = isExpense,
                note = note,
                transactionDateIso = dateIso,
                isRecurring = isRecurring,
                adjustAccountBalance = true
            ).onSuccess {
                _userMessage.value = "Transaction saved."
            }.onFailure {
                _userMessage.value = "Failed to record transaction: ${it.localizedMessage}"
            }
        }
    }

    fun deleteTransaction(tx: TransactionRecord) {
        viewModelScope.launch {
            repository.deleteTransaction(tx).onSuccess {
                _userMessage.value = "Transaction removed."
            }.onFailure {
                _userMessage.value = "Failed to delete: ${it.localizedMessage}"
            }
        }
    }

    fun deleteBankAccount(account: BankAccount) {
        viewModelScope.launch {
            repository.deleteBankAccount(account.id).onSuccess {
                _userMessage.value = "Account ${account.accountName} removed."
            }.onFailure {
                _userMessage.value = "Failed to remove account: ${it.localizedMessage}"
            }
        }
    }

    fun saveBudget(
        category: SpendingCategory,
        monthlyLimitCents: Long,
        thresholdPercent: Long,
        rollover: Boolean
    ) {
        viewModelScope.launch {
            repository.upsertBudgetEnvelope(
                category = category,
                monthlyLimitCents = monthlyLimitCents,
                alertThresholdPercent = thresholdPercent,
                rolloverEnabled = rollover
            ).onSuccess {
                _userMessage.value = "${category.displayName} budget updated."
            }.onFailure {
                _userMessage.value = "Failed to save budget: ${it.localizedMessage}"
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deleteBudgetEnvelope(budgetId).onSuccess {
                _userMessage.value = "Budget removed."
            }.onFailure {
                _userMessage.value = "Failed to delete budget: ${it.localizedMessage}"
            }
        }
    }
}
