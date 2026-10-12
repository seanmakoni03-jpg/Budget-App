package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.AccountType
import com.example.data.model.LocalBankDirectoryItem
import com.example.data.model.SampleBankAccountPreset
import com.example.data.model.SampleTransactionPreset
import com.example.data.model.SpendingCategory
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class FrankfurterRatesResponse(
    @Json(name = "amount") val amount: Double = 1.0,
    @Json(name = "base") val base: String = "USD",
    @Json(name = "date") val date: String = "",
    @Json(name = "rates") val rates: Map<String, Double> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class PlaidSandboxTokenRequest(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "secret") val secret: String,
    @Json(name = "institution_id") val institutionId: String,
    @Json(name = "initial_products") val initialProducts: List<String> = listOf("transactions")
)

@JsonClass(generateAdapter = true)
data class PlaidSandboxTokenResponse(
    @Json(name = "public_token") val publicToken: String = "",
    @Json(name = "request_id") val requestId: String = ""
)

interface FrankfurterCurrencyApi {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") baseCurrency: String = "USD",
        @Query("to") symbols: String = "EUR,GBP,CAD,AUD,ZAR"
    ): FrankfurterRatesResponse
}

interface PlaidOpenBankingApi {
    @POST("sandbox/public_token/create")
    suspend fun createSandboxPublicToken(
        @Body request: PlaidSandboxTokenRequest
    ): PlaidSandboxTokenResponse
}

object OpenBankingService {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    val currencyApi: FrankfurterCurrencyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.frankfurter.app/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FrankfurterCurrencyApi::class.java)
    }

    val plaidApi: PlaidOpenBankingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://sandbox.plaid.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PlaidOpenBankingApi::class.java)
    }

    fun isPlaidConfigured(): Boolean {
        return try {
            val fieldId = BuildConfig::class.java.getField("PLAID_CLIENT_ID")
            val fieldSec = BuildConfig::class.java.getField("PLAID_SECRET")
            val clientId = fieldId.get(null) as? String ?: ""
            val secret = fieldSec.get(null) as? String ?: ""
            clientId.isNotBlank() &&
                !clientId.contains("PLACEHOLDER") &&
                secret.isNotBlank() &&
                !secret.contains("PLACEHOLDER")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Curated directory of local & regional banks supporting Open Banking (Plaid / PSD2 / ACH / Faster Payments)
     * as well as statement CSV import.
     */
    val localBankDirectory: List<LocalBankDirectoryItem> = listOf(
        LocalBankDirectoryItem(
            id = "ins_chase_us",
            institutionName = "Chase Bank",
            regionLabel = "United States • ACH / FedNow",
            routingPrefix = "021000021",
            defaultCurrency = "USD",
            accentHex = "#10B981",
            protocolLabel = "Open Banking OAuth 2.0",
            sampleAccounts = listOf(
                SampleBankAccountPreset(
                    accountName = "Premier Everyday Checking",
                    accountType = AccountType.CHECKING,
                    accountMask = "4821",
                    initialBalanceCents = 468540L,
                    sampleTransactions = listOf(
                        SampleTransactionPreset(
                            merchantName = "TechCorp Bi-Weekly Payroll",
                            category = SpendingCategory.INCOME,
                            amountCents = 345000L,
                            isExpense = false,
                            note = "Direct Deposit • ACH Credit",
                            daysAgo = 4,
                            isRecurring = true
                        ),
                        SampleTransactionPreset(
                            merchantName = "Avalon Bay Communities",
                            category = SpendingCategory.HOUSING,
                            amountCents = 165000L,
                            isExpense = true,
                            note = "Monthly Apartment Rent",
                            daysAgo = 3,
                            isRecurring = true
                        ),
                        SampleTransactionPreset(
                            merchantName = "Whole Foods Market",
                            category = SpendingCategory.GROCERIES,
                            amountCents = 14280L,
                            isExpense = true,
                            note = "Organic groceries & household",
                            daysAgo = 2,
                            isRecurring = false
                        ),
                        SampleTransactionPreset(
                            merchantName = "Blue Bottle Coffee",
                            category = SpendingCategory.DINING,
                            amountCents = 1850L,
                            isExpense = true,
                            note = "Espresso & pastry",
                            daysAgo = 1,
                            isRecurring = false
                        ),
                        SampleTransactionPreset(
                            merchantName = "Con Edison Electric",
                            category = SpendingCategory.UTILITIES,
                            amountCents = 11420L,
                            isExpense = true,
                            note = "Monthly electric utility bill",
                            daysAgo = 1,
                            isRecurring = true
                        )
                    )
                ),
                SampleBankAccountPreset(
                    accountName = "Emerald Reserve Savings",
                    accountType = AccountType.SAVINGS,
                    accountMask = "9044",
                    initialBalanceCents = 1280000L,
                    sampleTransactions = listOf(
                        SampleTransactionPreset(
                            merchantName = "Monthly Vault Yield (4.65% APY)",
                            category = SpendingCategory.INCOME,
                            amountCents = 4960L,
                            isExpense = false,
                            note = "High-Yield Savings Interest",
                            daysAgo = 5,
                            isRecurring = true
                        )
                    )
                )
            )
        ),
        LocalBankDirectoryItem(
            id = "ins_monzo_uk",
            institutionName = "Monzo Local Bank",
            regionLabel = "United Kingdom • Faster Payments",
            routingPrefix = "04-00-04",
            defaultCurrency = "GBP",
            accentHex = "#F59E0B",
            protocolLabel = "UK Open Banking PSD2",
            sampleAccounts = listOf(
                SampleBankAccountPreset(
                    accountName = "Personal Current Account",
                    accountType = AccountType.CHECKING,
                    accountMask = "7719",
                    initialBalanceCents = 312450L,
                    sampleTransactions = listOf(
                        SampleTransactionPreset(
                            merchantName = "Waitrose & Partners",
                            category = SpendingCategory.GROCERIES,
                            amountCents = 8640L,
                            isExpense = true,
                            note = "Weekly fresh groceries",
                            daysAgo = 2,
                            isRecurring = false
                        ),
                        SampleTransactionPreset(
                            merchantName = "Transport for London (TfL)",
                            category = SpendingCategory.TRANSPORT,
                            amountCents = 3420L,
                            isExpense = true,
                            note = "Contactless Tube & Rail",
                            daysAgo = 1,
                            isRecurring = false
                        )
                    )
                )
            )
        ),
        LocalBankDirectoryItem(
            id = "ins_revolut_eu",
            institutionName = "Revolut Bank EU",
            regionLabel = "Eurozone • SEPA Instant",
            routingPrefix = "REVOGB21",
            defaultCurrency = "EUR",
            accentHex = "#38BDF8",
            protocolLabel = "PSD2 AIS Feed",
            sampleAccounts = listOf(
                SampleBankAccountPreset(
                    accountName = "Multi-Currency Euro Vault",
                    accountType = AccountType.CHECKING,
                    accountMask = "3310",
                    initialBalanceCents = 589000L,
                    sampleTransactions = listOf(
                        SampleTransactionPreset(
                            merchantName = "Carrefour City Market",
                            category = SpendingCategory.GROCERIES,
                            amountCents = 7490L,
                            isExpense = true,
                            note = "Weekly groceries",
                            daysAgo = 3,
                            isRecurring = false
                        ),
                        SampleTransactionPreset(
                            merchantName = "Spotify Family Plan",
                            category = SpendingCategory.ENTERTAINMENT,
                            amountCents = 1799L,
                            isExpense = true,
                            note = "Monthly music subscription",
                            daysAgo = 2,
                            isRecurring = true
                        )
                    )
                )
            )
        ),
        LocalBankDirectoryItem(
            id = "ins_wells_us",
            institutionName = "Wells Fargo Local",
            regionLabel = "United States • FedWire / ACH",
            routingPrefix = "121000248",
            defaultCurrency = "USD",
            accentHex = "#A78BFA",
            protocolLabel = "FDX Open Finance API",
            sampleAccounts = listOf(
                SampleBankAccountPreset(
                    accountName = "Autograph Cash Credit",
                    accountType = AccountType.CREDIT,
                    accountMask = "6152",
                    initialBalanceCents = 145200L,
                    sampleTransactions = listOf(
                        SampleTransactionPreset(
                            merchantName = "Shell Service Station",
                            category = SpendingCategory.TRANSPORT,
                            amountCents = 5240L,
                            isExpense = true,
                            note = "Unleaded fuel fill-up",
                            daysAgo = 2,
                            isRecurring = false
                        ),
                        SampleTransactionPreset(
                            merchantName = "Equinox Fitness Club",
                            category = SpendingCategory.HEALTH,
                            amountCents = 21500L,
                            isExpense = true,
                            note = "Monthly gym membership",
                            daysAgo = 4,
                            isRecurring = true
                        )
                    )
                )
            )
        ),
        LocalBankDirectoryItem(
            id = "ins_stanbic_af",
            institutionName = "Standard Local Bank",
            regionLabel = "Regional • Instant EFT / RTGS",
            routingPrefix = "051001",
            defaultCurrency = "USD",
            accentHex = "#2DD4BF",
            protocolLabel = "Open Banking Direct Connect",
            sampleAccounts = listOf(
                SampleBankAccountPreset(
                    accountName = "Prestige Local Checking",
                    accountType = AccountType.CHECKING,
                    accountMask = "8840",
                    initialBalanceCents = 394000L,
                    sampleTransactions = listOf(
                        SampleTransactionPreset(
                            merchantName = "FreshMart Supermarket",
                            category = SpendingCategory.GROCERIES,
                            amountCents = 9650L,
                            isExpense = true,
                            note = "Household provisions & produce",
                            daysAgo = 1,
                            isRecurring = false
                        ),
                        SampleTransactionPreset(
                            merchantName = "City Fiber Broadband",
                            category = SpendingCategory.UTILITIES,
                            amountCents = 6500L,
                            isExpense = true,
                            note = "Unlimited Home Fiber 200Mbps",
                            daysAgo = 3,
                            isRecurring = true
                        )
                    )
                )
            )
        )
    )

    /**
     * Parses CSV bank statements in standard formats:
     * Date,Merchant,Category,Amount,Type (or Date,Description,Amount)
     * Example line: 2026-10-10,Trader Joes,GROCERIES,-64.25,EXPENSE
     */
    fun parseBankStatementCsv(rawCsv: String): List<ParsedCsvRow> {
        val lines = rawCsv.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isEmpty()) return emptyList()

        val result = mutableListOf<ParsedCsvRow>()
        for ((index, line) in lines.withIndex()) {
            val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
            if (index == 0 && parts.firstOrNull()?.equals("date", ignoreCase = true) == true) {
                continue
            }
            if (parts.size >= 3) {
                val rawDate = parts[0].take(10).ifBlank { "2026-10-12" }
                val dateIso = if (rawDate.length == 10) rawDate else "2026-10-12"
                val merchant = parts[1].ifBlank { "Bank Statement Entry" }.take(95)

                val (category, amountDouble, explicitIsExpense) = if (parts.size >= 4) {
                    val cat = SpendingCategory.fromString(parts[2])
                    val amt = parts[3].replace("$", "").replace("£", "").replace("€", "").toDoubleOrNull() ?: 0.0
                    val typeFlag = parts.getOrNull(4)?.uppercase()
                    val isExp = when {
                        typeFlag == "INCOME" || typeFlag == "CREDIT" -> false
                        typeFlag == "EXPENSE" || typeFlag == "DEBIT" -> true
                        cat == SpendingCategory.INCOME -> false
                        else -> amt < 0 || amt > 0
                    }
                    Triple(cat, kotlin.math.abs(amt), isExp)
                } else {
                    val amt = parts[2].replace("$", "").replace("£", "").replace("€", "").toDoubleOrNull() ?: 0.0
                    val isExp = amt < 0
                    val inferredCat = inferCategoryFromMerchant(merchant, isExp)
                    Triple(inferredCat, kotlin.math.abs(amt), isExp)
                }

                val cents = (amountDouble * 100.0).toLong().coerceAtLeast(1L)
                result.add(
                    ParsedCsvRow(
                        dateIso = dateIso,
                        merchantName = merchant,
                        category = category,
                        amountCents = cents,
                        isExpense = explicitIsExpense
                    )
                )
            }
        }
        return result
    }

    private fun inferCategoryFromMerchant(merchant: String, isExpense: Boolean): SpendingCategory {
        if (!isExpense) return SpendingCategory.INCOME
        val lower = merchant.lowercase()
        return when {
            lower.contains("market") || lower.contains("whole foods") || lower.contains("trader") || lower.contains("grocer") -> SpendingCategory.GROCERIES
            lower.contains("coffee") || lower.contains("cafe") || lower.contains("restaurant") || lower.contains("grill") -> SpendingCategory.DINING
            lower.contains("rent") || lower.contains("mortgage") || lower.contains("apartments") -> SpendingCategory.HOUSING
            lower.contains("shell") || lower.contains("uber") || lower.contains("transit") || lower.contains("fuel") -> SpendingCategory.TRANSPORT
            lower.contains("electric") || lower.contains("water") || lower.contains("fiber") || lower.contains("utility") -> SpendingCategory.UTILITIES
            lower.contains("spotify") || lower.contains("netflix") || lower.contains("cinema") -> SpendingCategory.ENTERTAINMENT
            lower.contains("gym") || lower.contains("pharmacy") || lower.contains("clinic") -> SpendingCategory.HEALTH
            else -> SpendingCategory.SHOPPING
        }
    }
}

data class ParsedCsvRow(
    val dateIso: String,
    val merchantName: String,
    val category: SpendingCategory,
    val amountCents: Long,
    val isExpense: Boolean
)
