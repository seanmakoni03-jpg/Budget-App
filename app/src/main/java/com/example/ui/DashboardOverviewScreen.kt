package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccount
import com.example.data.model.SpendingCategory
import com.example.data.model.TransactionRecord
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSurfaceCard
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SlateText
import com.example.ui.theme.SlateTextDim
import com.example.ui.theme.SlateTextMuted
import java.text.NumberFormat
import java.util.Locale

fun formatCentsToCurrency(cents: Long, currencyCode: String = "USD"): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    try {
        format.currency = java.util.Currency.getInstance(currencyCode)
    } catch (_: Exception) {
    }
    return format.format(cents / 100.0)
}

@Composable
fun DashboardOverviewScreen(
    summaryState: UiState<SpendingSummary>,
    accountsState: UiState<List<BankAccount>>,
    onConnectBankClicked: () -> Unit,
    onAddTransactionClicked: () -> Unit,
    onViewAllAccounts: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onManageBudgetsClicked: () -> Unit
) {
    when (summaryState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = EmeraldPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = summaryState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        is UiState.Success -> {
            val summary = summaryState.data
            val accounts = (accountsState as? UiState.Success)?.data ?: emptyList()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("dashboard_overview_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Total Net Worth Vault Card
                item {
                    NetWorthVaultCard(
                        summary = summary,
                        onConnectBankClicked = onConnectBankClicked,
                        onAddTransactionClicked = onAddTransactionClicked
                    )
                }

                // 2. Monthly Inflow vs Outflow Cash-Flow Telemetry
                item {
                    CashFlowTelemetryRow(
                        incomeCents = summary.monthlyIncomeCents,
                        spentCents = summary.monthlySpentCents,
                        netCents = summary.netCashFlowCents
                    )
                }

                // 3. Linked Bank Accounts Carousel
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Linked Bank Accounts",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "View All (${accounts.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                modifier = Modifier
                                    .clickable { onViewAllAccounts() }
                                    .padding(vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (accounts.isEmpty()) {
                            EmptyBankAccountsPrompt(onConnectBankClicked = onConnectBankClicked)
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(accounts) { account ->
                                    MiniBankCardItem(account = account)
                                }
                            }
                        }
                    }
                }

                // 4. Monthly Spending by Category Donut Chart
                item {
                    CategorySpendingChartCard(
                        categorySpending = summary.categorySpending,
                        totalSpentCents = summary.monthlySpentCents
                    )
                }

                // 5. Active Budget Envelopes Progress
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Budget Envelopes",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Configure",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldAccent,
                                modifier = Modifier
                                    .clickable { onManageBudgetsClicked() }
                                    .padding(vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (summary.budgetProgressList.isEmpty()) {
                            EmptyBudgetsPrompt(onManageBudgetsClicked = onManageBudgetsClicked)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                summary.budgetProgressList.take(4).forEach { item ->
                                    BudgetProgressItemCard(item = item)
                                }
                            }
                        }
                    }
                }

                // 6. Recent Spending Feed
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "View All Feed",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                modifier = Modifier
                                    .clickable { onViewAllTransactions() }
                                    .padding(vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (summary.recentTransactions.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No recent spending activity. Connect a local bank or add an entry above.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SlateTextMuted
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                summary.recentTransactions.take(5).forEach { tx ->
                                    TransactionRowItem(transaction = tx)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetWorthVaultCard(
    summary: SpendingSummary,
    onConnectBankClicked: () -> Unit,
    onAddTransactionClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EmeraldDark,
                        Color(0xFF041E17),
                        Color(0xFF02140F)
                    )
                )
            )
            .border(1.dp, EmeraldBorder, RoundedCornerShape(24.dp))
            .padding(22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(EmeraldPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TOTAL LIQUID BALANCE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }
                Text(
                    text = "${summary.totalAccountsCount} Vaults",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = SlateTextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = formatCentsToCurrency(summary.totalBalanceCents),
                style = MaterialTheme.typography.displayLarge,
                color = SlateText,
                modifier = Modifier.testTag("net_worth_total_text")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onConnectBankClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("connect_bank_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color(0xFF022C22)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Link Bank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onAddTransactionClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("log_expense_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SlateText
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(EmeraldBorder, GoldAccent))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Log Spend",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CashFlowTelemetryRow(
    incomeCents: Long,
    spentCents: Long,
    netCents: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Income Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextMuted
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatCentsToCurrency(incomeCents),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
            }
        }

        // Expense Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = ExpenseRose,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextMuted
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatCentsToCurrency(spentCents),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRose
                )
            }
        }

        // Net Flow Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Net Flow",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextMuted
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatCentsToCurrency(netCents),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (netCents >= 0) GoldAccent else ExpenseRose
                )
            }
        }
    }
}

@Composable
fun MiniBankCardItem(account: BankAccount) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(account.accentHex))
    } catch (_: Exception) {
        EmeraldPrimary
    }

    Box(
        modifier = Modifier
            .width(220.dp)
            .height(128.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        cardColor.copy(alpha = 0.25f),
                        EmeraldSurfaceCard,
                        Color(0xFF041A14)
                    )
                )
            )
            .border(1.dp, cardColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.institutionName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .background(cardColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = account.accountType.take(4),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = cardColor
                    )
                }
            }

            Text(
                text = "•••• ${account.accountMask}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                color = SlateTextMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "BALANCE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = SlateTextDim
                    )
                    Text(
                        text = formatCentsToCurrency(account.balanceCents, account.currency),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateText
                    )
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Synced",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CategorySpendingChartCard(
    categorySpending: Map<SpendingCategory, Long>,
    totalSpentCents: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmeraldBorder, Color(0xFF0D332A))))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatCentsToCurrency(totalSpentCents),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRose
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categorySpending.isEmpty() || totalSpentCents <= 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded expense data for this period yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextMuted
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Donut Chart on Canvas
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val strokeWidth = 24.dp.toPx()
                            val sorted = categorySpending.entries.sortedByDescending { it.value }

                            for (entry in sorted) {
                                val sweep = (entry.value.toFloat() / totalSpentCents.toFloat()) * 360f
                                val color = Color(entry.key.colorHex)
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                    size = Size(size.width - strokeWidth, size.height - strokeWidth)
                                )
                                startAngle += sweep
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categorySpending.entries
                            .sortedByDescending { it.value }
                            .take(4)
                            .forEach { (cat, cents) ->
                                val percent = ((cents.toFloat() / totalSpentCents.toFloat()) * 100).toInt()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(cat.colorHex), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SlateText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "$percent%",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = SlateTextMuted
                                    )
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetProgressItemCard(item: BudgetProgressItem) {
    val progress = (item.percentageUsed / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(600), label = "budgetProgress")
    val cat = SpendingCategory.fromString(item.budget.category)
    val indicatorColor by animateColorAsState(
        targetValue = when {
            item.percentageUsed >= 100f -> ExpenseRose
            item.isAlertTriggered -> GoldAccent
            else -> EmeraldPrimary
        },
        label = "indicatorColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(if (item.isAlertTriggered) indicatorColor.copy(alpha = 0.5f) else EmeraldBorder, Color(0xFF0B241C))
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(cat.colorHex), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cat.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        color = SlateText
                    )
                }
                Text(
                    text = "${item.percentageUsed.toInt()}% used",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = indicatorColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF041A14))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(indicatorColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent ${formatCentsToCurrency(item.spentCents)} of ${formatCentsToCurrency(item.budget.monthlyLimitCents)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = SlateTextMuted
                )
                Text(
                    text = "${formatCentsToCurrency(item.remainingCents)} left",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.remainingCents > 0) EmeraldPrimary else ExpenseRose
                )
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionRecord,
    onDeleteClicked: (() -> Unit)? = null
) {
    val cat = SpendingCategory.fromString(transaction.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmeraldBorder, Color(0xFF08261F))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(cat.colorHex).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transaction.isExpense) Icons.Default.ReceiptLong else Icons.Default.Savings,
                        contentDescription = null,
                        tint = Color(cat.colorHex),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.merchantName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${transaction.accountName} • ${transaction.transactionDateIso}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = SlateTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (transaction.isExpense) "-" else "+"
                val amountColor = if (transaction.isExpense) ExpenseRose else IncomeGreen
                Text(
                    text = "$prefix${formatCentsToCurrency(transaction.amountCents)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                if (transaction.isRecurring) {
                    Text(
                        text = "RECURRING",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = GoldAccent
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyBankAccountsPrompt(onConnectBankClicked: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmeraldBorder, Color(0xFF064E3B))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Local Bank Accounts Linked",
                style = MaterialTheme.typography.titleMedium,
                color = SlateText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connect Chase, Monzo, Revolut or upload a statement to track transactions.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onConnectBankClicked,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF022C22)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Select Local Bank", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyBudgetsPrompt(onManageBudgetsClicked: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Budget Envelopes Configured",
                style = MaterialTheme.typography.titleMedium,
                color = SlateText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Set monthly spending limits for Groceries, Dining, Rent, and Transit.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onManageBudgetsClicked,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color(0xFF451A03)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Set Category Limits", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
