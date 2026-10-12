package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AccountType
import com.example.data.model.SpendingCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BudgetRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createBankAccountAndTransaction_validPayload_succeedsAndEmitsUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = BudgetRepository(firestore)

        val createAccResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBankAccount(
                institutionName = "Chase Bank",
                accountName = "Premier Checking",
                accountType = AccountType.CHECKING,
                accountMask = "4821",
                routingCode = "021000021",
                balanceCents = 250000L,
                currency = "USD",
                accentHex = "#10B981"
            )
        }
        assertTrue("Expected bank account creation to succeed: ${createAccResult.exceptionOrNull()}", createAccResult.isSuccess)
        val accountId = createAccResult.getOrThrow()
        assertTrue(accountId.isNotEmpty())

        val createTxResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordTransaction(
                accountId = accountId,
                accountName = "Chase Bank ••4821",
                merchantName = "Whole Foods Market",
                category = SpendingCategory.GROCERIES,
                amountCents = 8500L,
                isExpense = true,
                note = "Weekly groceries",
                transactionDateIso = "2026-10-12",
                isRecurring = false,
                adjustAccountBalance = true
            )
        }
        assertTrue("Expected transaction creation to succeed: ${createTxResult.exceptionOrNull()}", createTxResult.isSuccess)

        val emittedAccounts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBankAccounts(aliceUid).first { list -> list.any { it.id == accountId } }
        }
        val updatedAccount = emittedAccounts.first { it.id == accountId }
        assertEquals(241500L, updatedAccount.balanceCents)

        val budgetResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertBudgetEnvelope(
                category = SpendingCategory.GROCERIES,
                monthlyLimitCents = 60000L,
                alertThresholdPercent = 80L,
                rolloverEnabled = true
            )
        }
        assertTrue("Expected budget upsert to succeed: ${budgetResult.exceptionOrNull()}", budgetResult.isSuccess)
    }

    @Test
    fun getBankAccountById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = BudgetRepository(firestore)
        val accountId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createBankAccount(
                institutionName = "Monzo Bank",
                accountName = "Personal Current",
                accountType = AccountType.CHECKING,
                accountMask = "9012",
                routingCode = "04-00-04",
                balanceCents = 100000L,
                currency = "GBP"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = BudgetRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getBankAccountById(aliceUid, accountId)
        }
        assertTrue("Expected cross-user access to fail", crossReadResult.isFailure)
        val ex = crossReadResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull("Expected FirebaseFirestoreException", ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun observeBankAccounts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = BudgetRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeBankAccounts(aliceUid).first()
            }
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            val firestoreEx = (e as? FirebaseFirestoreException)
                ?: (e.cause as? FirebaseFirestoreException)
                ?: (e.cause?.cause as? FirebaseFirestoreException)
            assertNotNull("Expected FirebaseFirestoreException in cause chain: $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@vaultflow.test"
        const val BOB_EMAIL = "bob@vaultflow.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
