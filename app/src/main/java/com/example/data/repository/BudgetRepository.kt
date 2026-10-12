package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccountType
import com.example.data.model.BankAccount
import com.example.data.model.BudgetEnvelope
import com.example.data.model.LocalBankDirectoryItem
import com.example.data.model.SpendingCategory
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionRecord
import com.example.data.remote.OperationType
import com.example.data.remote.ParsedCsvRow
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class BudgetRepository(
    private val db: FirebaseFirestore
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Real-Time Observation Flows ---

    fun observeBankAccounts(userId: String = requireUserId()): Flow<List<BankAccount>> = flow {
        val path = "users/$userId/bank_accounts"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("bank_accounts")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BankAccount::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String = requireUserId()): Flow<List<TransactionRecord>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TransactionRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<TransactionRecord> { it.transactionDateIso }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeBudgets(userId: String = requireUserId()): Flow<List<BudgetEnvelope>> = flow {
        val path = "users/$userId/budgets"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("budgets")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BudgetEnvelope::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.monthlyLimitCents }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    // --- Direct Queries (Used by ViewModel & Rule Tests) ---

    suspend fun getBankAccounts(): Result<List<BankAccount>> = runCatching {
        val uid = requireUserId()
        val path = "users/$uid/bank_accounts"
        try {
            val snap = db.collection("users")
                .document(uid)
                .collection("bank_accounts")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snap.toObjects(BankAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getBankAccountById(ownerUid: String, accountId: String): Result<BankAccount?> = runCatching {
        val path = "users/$ownerUid/bank_accounts/$accountId"
        try {
            val snap = db.collection("users")
                .document(ownerUid)
                .collection("bank_accounts")
                .document(accountId)
                .get()
                .await()
            snap.toObject(BankAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    // --- Mutations ---

    suspend fun createBankAccount(
        institutionName: String,
        accountName: String,
        accountType: AccountType,
        accountMask: String,
        routingCode: String,
        balanceCents: Long,
        currency: String = "USD",
        accentHex: String = "#10B981"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val accountId = "acc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users")
            .document(uid)
            .collection("bank_accounts")
            .document(accountId)

        val payload = mapOf(
            "userId" to uid,
            "institutionName" to institutionName.trim().take(80),
            "accountName" to accountName.trim().take(80),
            "accountType" to accountType.name,
            "accountMask" to accountMask.trim().takeLast(4).padStart(4, '0'),
            "routingCode" to routingCode.trim().take(34).ifBlank { "021000021" },
            "balanceCents" to balanceCents,
            "currency" to currency.trim().uppercase().take(3).padEnd(3, 'D'),
            "syncStatus" to SyncStatus.SYNCED.name,
            "accentHex" to accentHex.take(9),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            accountId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun recordTransaction(
        accountId: String,
        accountName: String,
        merchantName: String,
        category: SpendingCategory,
        amountCents: Long,
        isExpense: Boolean,
        note: String,
        transactionDateIso: String,
        isRecurring: Boolean = false,
        adjustAccountBalance: Boolean = true
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val txRef = db.collection("users")
            .document(uid)
            .collection("transactions")
            .document(txId)

        val cleanDate = if (transactionDateIso.length == 10) transactionDateIso else currentIsoDate()
        val safeAmount = amountCents.coerceAtLeast(1L)

        val payload = mapOf(
            "userId" to uid,
            "accountId" to accountId,
            "accountName" to accountName.trim().take(80).ifBlank { "Primary Account" },
            "merchantName" to merchantName.trim().take(100).ifBlank { "Merchant" },
            "category" to category.name,
            "amountCents" to safeAmount,
            "isExpense" to isExpense,
            "note" to note.trim().take(300),
            "transactionDateIso" to cleanDate,
            "isRecurring" to isRecurring,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            txRef.set(payload).await()
            if (adjustAccountBalance && accountId.isNotBlank()) {
                val accRef = db.collection("users")
                    .document(uid)
                    .collection("bank_accounts")
                    .document(accountId)
                val accSnap = accRef.get().await()
                if (accSnap.exists()) {
                    val currentBalance = accSnap.getLong("balanceCents") ?: 0L
                    val delta = if (isExpense) -safeAmount else safeAmount
                    accRef.update(
                        mapOf(
                            "balanceCents" to (currentBalance + delta),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            txId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, txRef.path)
            throw e
        }
    }

    suspend fun deleteTransaction(transaction: TransactionRecord): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(transaction.id.isNotBlank()) { "Transaction ID cannot be blank" }
        val txRef = db.collection("users")
            .document(uid)
            .collection("transactions")
            .document(transaction.id)

        try {
            txRef.delete().await()
            if (transaction.accountId.isNotBlank()) {
                val accRef = db.collection("users")
                    .document(uid)
                    .collection("bank_accounts")
                    .document(transaction.accountId)
                val accSnap = accRef.get().await()
                if (accSnap.exists()) {
                    val currentBalance = accSnap.getLong("balanceCents") ?: 0L
                    val reverseDelta = if (transaction.isExpense) transaction.amountCents else -transaction.amountCents
                    accRef.update(
                        mapOf(
                            "balanceCents" to (currentBalance + reverseDelta),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, txRef.path)
            throw e
        }
    }

    suspend fun deleteBankAccount(accountId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val accRef = db.collection("users")
            .document(uid)
            .collection("bank_accounts")
            .document(accountId)
        try {
            accRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, accRef.path)
            throw e
        }
    }

    suspend fun upsertBudgetEnvelope(
        category: SpendingCategory,
        monthlyLimitCents: Long,
        alertThresholdPercent: Long = 80L,
        rolloverEnabled: Boolean = true
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val budgetId = "budget_${category.name.lowercase()}"
        val docRef = db.collection("users")
            .document(uid)
            .collection("budgets")
            .document(budgetId)

        try {
            val existingSnap = docRef.get().await()
            val safeLimit = monthlyLimitCents.coerceIn(100L, 100000000000L)
            val safeThreshold = alertThresholdPercent.coerceIn(10L, 100L)

            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "category" to category.name,
                        "monthlyLimitCents" to safeLimit,
                        "alertThresholdPercent" to safeThreshold,
                        "rolloverEnabled" to rolloverEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "category" to category.name,
                    "monthlyLimitCents" to safeLimit,
                    "alertThresholdPercent" to safeThreshold,
                    "rolloverEnabled" to rolloverEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            budgetId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBudgetEnvelope(budgetId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("budgets")
            .document(budgetId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    /**
     * Connects a bank from the Local Bank Directory and imports its initial feed + budgets if none exist.
     */
    suspend fun linkInstitutionFromDirectory(bankItem: LocalBankDirectoryItem): Result<Int> = runCatching {
        var importedTxCount = 0
        for (preset in bankItem.sampleAccounts) {
            val accountId = createBankAccount(
                institutionName = bankItem.institutionName,
                accountName = preset.accountName,
                accountType = preset.accountType,
                accountMask = preset.accountMask,
                routingCode = bankItem.routingPrefix,
                balanceCents = preset.initialBalanceCents,
                currency = bankItem.defaultCurrency,
                accentHex = bankItem.accentHex
            ).getOrThrow()

            for (tx in preset.sampleTransactions) {
                recordTransaction(
                    accountId = accountId,
                    accountName = "${bankItem.institutionName} ••${preset.accountMask}",
                    merchantName = tx.merchantName,
                    category = tx.category,
                    amountCents = tx.amountCents,
                    isExpense = tx.isExpense,
                    note = tx.note,
                    transactionDateIso = daysAgoIsoDate(tx.daysAgo),
                    isRecurring = tx.isRecurring,
                    adjustAccountBalance = false
                ).getOrThrow()
                importedTxCount++
            }
        }
        importedTxCount
    }

    /**
     * Imports parsed CSV rows into a selected BankAccount and updates its balance accordingly.
     */
    suspend fun importCsvTransactions(
        targetAccount: BankAccount,
        rows: List<ParsedCsvRow>
    ): Result<Int> = runCatching {
        var count = 0
        for (row in rows) {
            recordTransaction(
                accountId = targetAccount.id,
                accountName = "${targetAccount.institutionName} ••${targetAccount.accountMask}",
                merchantName = row.merchantName,
                category = row.category,
                amountCents = row.amountCents,
                isExpense = row.isExpense,
                note = "Imported via CSV Bank Statement",
                transactionDateIso = row.dateIso,
                isRecurring = false,
                adjustAccountBalance = true
            ).getOrThrow()
            count++
        }
        count
    }

    /**
     * Seeds default monthly category budgets so the user immediately has rich budget envelopes.
     */
    suspend fun seedStarterBudgetsIfEmpty(existingBudgets: List<BudgetEnvelope>): Result<Unit> = runCatching {
        if (existingBudgets.isNotEmpty()) return@runCatching
        val defaults = listOf(
            SpendingCategory.HOUSING to 180000L,
            SpendingCategory.GROCERIES to 65000L,
            SpendingCategory.DINING to 35000L,
            SpendingCategory.TRANSPORT to 25000L,
            SpendingCategory.UTILITIES to 22000L,
            SpendingCategory.ENTERTAINMENT to 18000L
        )
        for ((cat, limitCents) in defaults) {
            upsertBudgetEnvelope(
                category = cat,
                monthlyLimitCents = limitCents,
                alertThresholdPercent = 80L,
                rolloverEnabled = true
            ).getOrThrow()
        }
    }

    companion object {
        fun currentIsoDate(): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return formatter.format(Calendar.getInstance().time)
        }

        fun daysAgoIsoDate(daysAgo: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return formatter.format(cal.time)
        }
    }
}
