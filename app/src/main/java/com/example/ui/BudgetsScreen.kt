package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.model.BudgetEnvelope
import com.example.data.model.SpendingCategory
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSurfaceCard
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SlateText
import com.example.ui.theme.SlateTextDim
import com.example.ui.theme.SlateTextMuted

@Composable
fun BudgetsScreen(
    summaryState: UiState<SpendingSummary>,
    onSaveBudget: (SpendingCategory, Long, Long, Boolean) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<SpendingCategory?>(null) }
    var initialLimitCents by remember { mutableStateOf(50000L) }
    var initialThreshold by remember { mutableStateOf(80L) }
    var initialRollover by remember { mutableStateOf(true) }

    when (summaryState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = summaryState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val progressList = summaryState.data.budgetProgressList

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("budgets_screen_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Screen Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Budget Envelopes",
                                style = MaterialTheme.typography.displayMedium,
                                color = SlateText
                            )
                            Text(
                                text = "Category limits & real-time threshold telemetry",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateTextMuted
                            )
                        }

                        Button(
                            onClick = {
                                selectedCategoryForEdit = SpendingCategory.GROCERIES
                                initialLimitCents = 60000L
                                initialThreshold = 80L
                                initialRollover = true
                                showEditDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color(0xFF451A03)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Envelope", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (progressList.isEmpty()) {
                    item {
                        EmptyBudgetsPrompt(onManageBudgetsClicked = {
                            selectedCategoryForEdit = SpendingCategory.GROCERIES
                            showEditDialog = true
                        })
                    }
                } else {
                    items(progressList, key = { it.budget.id }) { item ->
                        DetailedBudgetEnvelopeCard(
                            item = item,
                            onEdit = {
                                selectedCategoryForEdit = SpendingCategory.fromString(item.budget.category)
                                initialLimitCents = item.budget.monthlyLimitCents
                                initialThreshold = item.budget.alertThresholdPercent
                                initialRollover = item.budget.rolloverEnabled
                                showEditDialog = true
                            },
                            onDelete = {
                                onDeleteBudget(item.budget.id)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog && selectedCategoryForEdit != null) {
        BudgetEditDialog(
            category = selectedCategoryForEdit!!,
            initialLimitCents = initialLimitCents,
            initialThresholdPercent = initialThreshold,
            initialRollover = initialRollover,
            onDismiss = { showEditDialog = false },
            onConfirm = { cat, limit, thresh, roll ->
                onSaveBudget(cat, limit, thresh, roll)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun DetailedBudgetEnvelopeCard(
    item: BudgetProgressItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cat = SpendingCategory.fromString(item.budget.category)
    val indicatorColor = when {
        item.percentageUsed >= 100f -> ExpenseRose
        item.isAlertTriggered -> GoldAccent
        else -> EmeraldPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(if (item.isAlertTriggered) indicatorColor else EmeraldBorder, Color(0xFF041C15))
            )
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(cat.colorHex).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = Color(cat.colorHex),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = cat.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = SlateText
                        )
                        Text(
                            text = "Alert at ${item.budget.alertThresholdPercent}% capacity",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = if (item.isAlertTriggered) GoldAccent else SlateTextDim
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = SlateTextMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = SlateTextDim, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dual Stat Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "CURRENT SPENT", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = SlateTextDim)
                    Text(
                        text = formatCentsToCurrency(item.spentCents),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = indicatorColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "MONTHLY CAP", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = SlateTextDim)
                    Text(
                        text = formatCentsToCurrency(item.budget.monthlyLimitCents),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large Progress Bar
            val fraction = (item.percentageUsed / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF03140F))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = fraction)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(indicatorColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isAlertTriggered) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.percentageUsed >= 100f) "CAP EXCEEDED" else "THRESHOLD WARNING",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.percentageUsed >= 100f) ExpenseRose else GoldAccent
                        )
                    } else {
                        Text(
                            text = "ON TRACK",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                Text(
                    text = "${formatCentsToCurrency(item.remainingCents)} remaining",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = if (item.remainingCents > 0) EmeraldPrimary else ExpenseRose
                )
            }
        }
    }
}

@Composable
fun BudgetEditDialog(
    category: SpendingCategory,
    initialLimitCents: Long,
    initialThresholdPercent: Long,
    initialRollover: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (cat: SpendingCategory, limitCents: Long, thresh: Long, roll: Boolean) -> Unit
) {
    var selectedCat by remember { mutableStateOf(category) }
    var limitText by remember { mutableStateOf((initialLimitCents / 100.0).toString()) }
    var thresholdFloat by remember { mutableFloatStateOf(initialThresholdPercent.toFloat()) }
    var rollover by remember { mutableStateOf(initialRollover) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Configure Budget Envelope", style = MaterialTheme.typography.titleLarge, color = SlateText)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Spending Category:", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SpendingCategory.budgetableCategories()) { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(cat.colorHex),
                                selectedLabelColor = Color(0xFF022C22)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly Spending Cap ($)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "Alert Threshold: ${thresholdFloat.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateText
                    )
                    Slider(
                        value = thresholdFloat,
                        onValueChange = { thresholdFloat = it },
                        valueRange = 25f..95f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldAccent,
                            activeTrackColor = EmeraldPrimary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rollover unspent balance?", style = MaterialTheme.typography.bodyMedium, color = SlateTextMuted)
                    Switch(checked = rollover, onCheckedChange = { rollover = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limitDouble = limitText.toDoubleOrNull() ?: 100.0
                    val cents = (limitDouble * 100.0).toLong().coerceIn(100L, 100000000000L)
                    onConfirm(selectedCat, cents, thresholdFloat.toLong(), rollover)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF022C22))
            ) {
                Text("Save Envelope", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SlateTextMuted) }
        },
        containerColor = Color(0xFF08261F),
        shape = RoundedCornerShape(20.dp)
    )
}
