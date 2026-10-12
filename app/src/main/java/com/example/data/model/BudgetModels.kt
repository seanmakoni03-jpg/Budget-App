package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AccountType(val label: String, val badgeText: String) {
    CHECKING("Checking", "CHK"),
    SAVINGS("High-Yield Savings", "SAV"),
    CREDIT("Credit Card", "CRD"),
    INVESTMENT("Investment Vault", "INV");

    companion object {
        fun fromString(value: String): AccountType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CHECKING
    }
}

enum class SyncStatus(val label: String) {
    SYNCED("Live Synced"),
    SYNCING("Syncing Feed"),
    RECONNECT_REQUIRED("Attention Needed");

    companion object {
        fun fromString(value: String): SyncStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYNCED
    }
}

enum class SpendingCategory(
    val displayName: String,
    val colorHex: Long,
    val isBudgetable: Boolean = true
) {
    HOUSING("Housing & Rent", 0xFF10B981, true),
    GROCERIES("Groceries & Market", 0xFF34D399, true),
    DINING("Dining & Coffee", 0xFFF59E0B, true),
    TRANSPORT("Transit & Fuel", 0xFF38BDF8, true),
    UTILITIES("Bills & Utilities", 0xFFA78BFA, true),
    SHOPPING("Retail & Gear", 0xFFF472B6, true),
    ENTERTAINMENT("Leisure & Media", 0xFFFB923C, true),
    HEALTH("Health & Wellness", 0xFF2DD4BF, true),
    INCOME("Payroll & Deposits", 0xFF22C55E, false),
    TRANSFER("Internal Transfer", 0xFF94A3B8, false);

    companion object {
        fun fromString(value: String): SpendingCategory =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GROCERIES

        fun budgetableCategories(): List<SpendingCategory> =
            entries.filter { it.isBudgetable }
    }
}

@IgnoreExtraProperties
data class BankAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val institutionName: String = "",
    val accountName: String = "",
    val accountType: String = AccountType.CHECKING.name,
    val accountMask: String = "0000",
    val routingCode: String = "021000021",
    val balanceCents: Long = 0L,
    val currency: String = "USD",
    val syncStatus: String = SyncStatus.SYNCED.name,
    val accentHex: String = "#10B981",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountId: String = "",
    val accountName: String = "",
    val merchantName: String = "",
    val category: String = SpendingCategory.GROCERIES.name,
    val amountCents: Long = 0L,
    val isExpense: Boolean = true,
    val note: String = "",
    val transactionDateIso: String = "2026-10-12",
    val isRecurring: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BudgetEnvelope(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = SpendingCategory.GROCERIES.name,
    val monthlyLimitCents: Long = 50000L,
    val alertThresholdPercent: Long = 80L,
    val rolloverEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class LocalBankDirectoryItem(
    val id: String,
    val institutionName: String,
    val regionLabel: String,
    val routingPrefix: String,
    val defaultCurrency: String,
    val accentHex: String,
    val protocolLabel: String,
    val sampleAccounts: List<SampleBankAccountPreset>
)

data class SampleBankAccountPreset(
    val accountName: String,
    val accountType: AccountType,
    val accountMask: String,
    val initialBalanceCents: Long,
    val sampleTransactions: List<SampleTransactionPreset>
)

data class SampleTransactionPreset(
    val merchantName: String,
    val category: SpendingCategory,
    val amountCents: Long,
    val isExpense: Boolean,
    val note: String,
    val daysAgo: Int,
    val isRecurring: Boolean = false
)
