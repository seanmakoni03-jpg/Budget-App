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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountType
import com.example.data.model.BankAccount
import com.example.data.model.LocalBankDirectoryItem
import com.example.data.remote.OpenBankingService
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSurfaceCard
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SlateText
import com.example.ui.theme.SlateTextDim
import com.example.ui.theme.SlateTextMuted

@Composable
fun BankAccountsScreen(
    accountsState: UiState<List<BankAccount>>,
    isSyncing: Boolean,
    onConnectBankClicked: () -> Unit,
    onImportCsvClicked: (BankAccount) -> Unit,
    onDeleteAccountClicked: (BankAccount) -> Unit,
    onCreateManualAccount: (institution: String, name: String, type: AccountType, mask: String, routing: String, balanceCents: Long, currency: String) -> Unit
) {
    var showManualDialog by remember { mutableStateOf(false) }

    when (accountsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = accountsState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val accounts = accountsState.data

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("bank_accounts_screen_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bank Vaults",
                                style = MaterialTheme.typography.displayMedium,
                                color = SlateText
                            )
                            Text(
                                text = "Synced via Open Banking & Statements",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateTextMuted
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onConnectBankClicked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color(0xFF022C22)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Link Bank", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (isSyncing) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = EmeraldSurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmeraldBorder, GoldAccent)))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = GoldAccent
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Syncing transaction feeds from Open Banking ledger...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GoldAccent
                                )
                            }
                        }
                    }
                }

                if (accounts.isEmpty()) {
                    item {
                        EmptyBankAccountsPrompt(onConnectBankClicked = onConnectBankClicked)
                    }
                } else {
                    items(accounts, key = { it.id }) { account ->
                        FullBankCardView(
                            account = account,
                            onImportCsv = { onImportCsvClicked(account) },
                            onDelete = { onDeleteAccountClicked(account) }
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showManualDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateTextMuted)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Custom Local Bank Manually", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualBankAddDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { inst, name, type, mask, routing, bal, curr ->
                onCreateManualAccount(inst, name, type, mask, routing, bal, curr)
                showManualDialog = false
            }
        )
    }
}

@Composable
fun FullBankCardView(
    account: BankAccount,
    onImportCsv: () -> Unit,
    onDelete: () -> Unit
) {
    val cardAccent = try {
        Color(android.graphics.Color.parseColor(account.accentHex))
    } catch (_: Exception) {
        EmeraldPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EmeraldDark.copy(alpha = 0.5f),
                        EmeraldSurfaceCard,
                        Color(0xFF02130E)
                    )
                )
            )
            .border(1.dp, cardAccent.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(20.dp)
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
                            .size(36.dp)
                            .background(cardAccent.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = cardAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = account.institutionName,
                            style = MaterialTheme.typography.titleMedium,
                            color = SlateText
                        )
                        Text(
                            text = account.accountName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateTextMuted
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Account",
                        tint = SlateTextDim
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACCOUNT NUMBER",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = SlateTextDim
                    )
                    Text(
                        text = "•••• •••• •••• ${account.accountMask}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 14.sp,
                        color = SlateText
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ROUTING / SORT",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = SlateTextDim
                    )
                    Text(
                        text = account.routingCode,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = SlateTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "CURRENT BALANCE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = GoldAccent
                    )
                    Text(
                        text = formatCentsToCurrency(account.balanceCents, account.currency),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateText
                    )
                }

                Button(
                    onClick = onImportCsv,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F3329),
                        contentColor = EmeraldPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import CSV", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ConnectBankDirectoryDialog(
    directory: List<LocalBankDirectoryItem>,
    onDismiss: () -> Unit,
    onSelectBank: (LocalBankDirectoryItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Link Local Financial Institution",
                style = MaterialTheme.typography.titleLarge,
                color = SlateText
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select your local institution to establish real-time Open Banking telemetry and import your recent spending feed:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )

                directory.forEach { bank ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectBank(bank) },
                        colors = CardDefaults.cardColors(containerColor = EmeraldSurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmeraldBorder, Color(0xFF0A2B21))))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bank.institutionName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SlateText
                                )
                                Text(
                                    text = bank.regionLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateTextMuted
                                )
                                Text(
                                    text = bank.protocolLabel,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = GoldAccent
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Connect",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SlateTextMuted)
            }
        },
        containerColor = Color(0xFF08261F),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ImportCsvDialog(
    targetAccount: BankAccount,
    onDismiss: () -> Unit,
    onConfirmImport: (String) -> Unit
) {
    val sampleCsv = """
2026-10-10,Whole Foods Market,GROCERIES,78.50,EXPENSE
2026-10-09,Blue Bottle Coffee,DINING,14.20,EXPENSE
2026-10-08,Shell Gasoline,TRANSPORT,48.00,EXPENSE
2026-10-07,Freelance Consulting,INCOME,1200.00,INCOME
2026-10-06,Pacific Gas & Electric,UTILITIES,82.40,EXPENSE
    """.trimIndent()

    var csvText by remember { mutableStateOf(sampleCsv) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Statement (${targetAccount.institutionName})",
                style = MaterialTheme.typography.titleLarge,
                color = SlateText
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Paste standard CSV bank statement rows formatted as Date,Merchant,Category,Amount,Type:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Tip: VaultFlow auto-categorizes entries and recalculates the balance of ${targetAccount.accountName}.",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = GoldAccent
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmImport(csvText) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF022C22))
            ) {
                Text("Import Transactions", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateTextMuted)
            }
        },
        containerColor = Color(0xFF08261F),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ManualBankAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (inst: String, name: String, type: AccountType, mask: String, routing: String, bal: Long, curr: String) -> Unit
) {
    var institution by remember { mutableStateOf("City First Bank") }
    var accountName by remember { mutableStateOf("Primary Checking") }
    var selectedType by remember { mutableStateOf(AccountType.CHECKING) }
    var mask by remember { mutableStateOf("1234") }
    var routing by remember { mutableStateOf("021000021") }
    var balanceText by remember { mutableStateOf("2500.00") }
    var currency by remember { mutableStateOf("USD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Custom Local Bank Account", style = MaterialTheme.typography.titleLarge, color = SlateText)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Institution Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Account Nickname") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mask,
                    onValueChange = { mask = it.take(4) },
                    label = { Text("Last 4 Digits") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Initial Balance ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balDouble = balanceText.toDoubleOrNull() ?: 0.0
                    val balCents = (balDouble * 100.0).toLong()
                    onConfirm(institution, accountName, selectedType, mask, routing, balCents, currency)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF022C22))
            ) {
                Text("Save Account", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SlateTextMuted) }
        },
        containerColor = Color(0xFF08261F),
        shape = RoundedCornerShape(20.dp)
    )
}
