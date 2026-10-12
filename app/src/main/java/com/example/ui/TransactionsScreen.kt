package com.example.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccount
import com.example.data.model.SpendingCategory
import com.example.data.model.TransactionRecord
import com.example.data.repository.BudgetRepository
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSurfaceCard
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SlateText
import com.example.ui.theme.SlateTextDim
import com.example.ui.theme.SlateTextMuted

@Composable
fun TransactionsScreen(
    transactionsState: UiState<List<TransactionRecord>>,
    accountsState: UiState<List<BankAccount>>,
    onAddTransactionClicked: () -> Unit,
    onDeleteTransactionClicked: (TransactionRecord) -> Unit
) {
    var selectedCategoryFilter by remember { mutableStateOf<SpendingCategory?>(null) }
    var filterOnlyExpenses by remember { mutableStateOf(false) }

    when (transactionsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = transactionsState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val allTransactions = transactionsState.data
            val accounts = (accountsState as? UiState.Success)?.data ?: emptyList()

            val filteredList = allTransactions.filter { tx ->
                val matchesCategory = selectedCategoryFilter == null || tx.category == selectedCategoryFilter?.name
                val matchesExpense = !filterOnlyExpenses || tx.isExpense
                matchesCategory && matchesExpense
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("transactions_screen_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Add Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Spending Feed",
                                style = MaterialTheme.typography.displayMedium,
                                color = SlateText
                            )
                            Text(
                                text = "${allTransactions.size} total entries recorded across banks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateTextMuted
                            )
                        }

                        Button(
                            onClick = onAddTransactionClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color(0xFF022C22)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Spend", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Category Filter Pills
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Filter by Category",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = SlateTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == null,
                                    onClick = { selectedCategoryFilter = null },
                                    label = { Text("All Categories") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color(0xFF022C22)
                                    )
                                )
                            }
                            items(SpendingCategory.entries) { cat ->
                                FilterChip(
                                    selected = selectedCategoryFilter == cat,
                                    onClick = {
                                        selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                                    },
                                    label = { Text(cat.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(cat.colorHex),
                                        selectedLabelColor = Color(0xFF022C22)
                                    )
                                )
                            }
                        }
                    }
                }

                if (filteredList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = EmeraldSurfaceCard),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions match this category filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateTextMuted
                                )
                            }
                        }
                    }
                } else {
                    items(filteredList, key = { it.id }) { tx ->
                        DetailedTransactionCard(
                            transaction = tx,
                            onDelete = { onDeleteTransactionClicked(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedTransactionCard(
    transaction: TransactionRecord,
    onDelete: () -> Unit
) {
    val cat = SpendingCategory.fromString(transaction.category)
    val amountColor = if (transaction.isExpense) ExpenseRose else IncomeGreen
    val prefix = if (transaction.isExpense) "-" else "+"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmeraldBorder, Color(0xFF07211A))))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(42.dp)
                            .background(Color(cat.colorHex).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(cat.colorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = transaction.merchantName,
                            style = MaterialTheme.typography.titleMedium,
                            color = SlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${cat.displayName} • ${transaction.transactionDateIso}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = SlateTextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$prefix${formatCentsToCurrency(transaction.amountCents)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = SlateTextDim,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (transaction.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF041913), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bank: ${transaction.accountName}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = SlateTextDim
                )

                if (transaction.isRecurring) {
                    Text(
                        text = "RECURRING BILL",
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
fun LogTransactionDialog(
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onConfirm: (account: BankAccount, merchant: String, cat: SpendingCategory, amountCents: Long, isExpense: Boolean, note: String, dateIso: String, isRecurring: Boolean) -> Unit
) {
    if (accounts.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Accounts Available", color = SlateText) },
            text = { Text("Please link at least one bank account before logging transactions.", color = SlateTextMuted) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
            containerColor = Color(0xFF08261F)
        )
        return
    }

    var selectedAccount by remember { mutableStateOf(accounts.first()) }
    var merchant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SpendingCategory.GROCERIES) }
    var isExpense by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var dateIso by remember { mutableStateOf(BudgetRepository.currentIsoDate()) }
    var isRecurring by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Financial Transaction", style = MaterialTheme.typography.titleLarge, color = SlateText) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bank Selector
                Text("Select Bank Account:", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts) { acc ->
                        FilterChip(
                            selected = selectedAccount.id == acc.id,
                            onClick = { selectedAccount = acc },
                            label = { Text("${acc.institutionName} (••${acc.accountMask})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color(0xFF022C22)
                            )
                        )
                    }
                }

                // Type Toggle (Expense vs Income)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpense) "Type: Expense (Debit)" else "Type: Income (Deposit)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isExpense) ExpenseRose else IncomeGreen
                    )
                    Switch(
                        checked = isExpense,
                        onCheckedChange = { isExpense = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ExpenseRose,
                            uncheckedThumbColor = IncomeGreen
                        )
                    )
                }

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Counterparty") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category:", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SpendingCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(cat.colorHex),
                                selectedLabelColor = Color(0xFF022C22)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = dateIso,
                    onValueChange = { dateIso = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Memo") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recurring bill / subscription?", style = MaterialTheme.typography.bodyMedium, color = SlateTextMuted)
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtDouble = amountText.toDoubleOrNull() ?: 0.0
                    val amtCents = (amtDouble * 100.0).toLong().coerceAtLeast(1L)
                    onConfirm(
                        selectedAccount,
                        merchant.ifBlank { "Merchant" },
                        selectedCategory,
                        amtCents,
                        isExpense,
                        note,
                        dateIso,
                        isRecurring
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF022C22))
            ) {
                Text("Save Transaction", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SlateTextMuted) }
        },
        containerColor = Color(0xFF08261F),
        shape = RoundedCornerShape(20.dp)
    )
}
