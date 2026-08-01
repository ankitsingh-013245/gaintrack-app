@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gaintrack.personal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gaintrack.personal.MainViewModel
import com.gaintrack.personal.data.AppState
import com.gaintrack.personal.data.PurchaseLog
import com.gaintrack.personal.data.PurchaseStatus
import com.gaintrack.personal.data.budgetCycleFor
import com.gaintrack.personal.ui.AppTopBar
import com.gaintrack.personal.ui.GainGreen
import com.gaintrack.personal.ui.SectionCard
import com.gaintrack.personal.ui.SectionLabel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val purchaseCategories = listOf(
    "Groceries",
    "Protein",
    "Fruit & veg",
    "Dairy",
    "Snacks",
    "Other",
)

@Composable
fun ShoppingScreen(state: AppState, viewModel: MainViewModel) {
    var addingPurchase by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PurchaseLog?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val cycle = budgetCycleFor(today, state.budgetCycleStartDay)
    val purchases = state.purchaseLogs
        .filter {
            it.status == PurchaseStatus.PURCHASED &&
                !it.purchaseDate.isBefore(cycle.start) &&
                !it.purchaseDate.isAfter(cycle.endInclusive)
        }
        .sortedWith(compareByDescending<PurchaseLog> { it.purchaseDate }.thenByDescending { it.id })
    val spent = purchases.sumOf { it.amount }
    val remaining = state.monthlyBudget - spent
    val budgetProgress = if (state.monthlyBudget <= 0.0) {
        0f
    } else {
        (spent / state.monthlyBudget).toFloat().coerceIn(0f, 1f)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppTopBar(
                    title = "Shopping",
                    subtitle = "Add what you bought. Your budget updates instantly.",
                )
            }
            item {
                SectionCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .52f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column {
                            Text("This budget cycle", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "₹${spent.toInt()}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "spent of ₹${state.monthlyBudget.toInt()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                        ) {
                            Icon(
                                Icons.Outlined.Wallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        color = if (remaining >= 0) {
                            GainGreen
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                if (remaining >= 0) {
                                    "₹${remaining.toInt()} remaining"
                                } else {
                                    "₹${(-remaining).toInt()} over budget"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (remaining >= 0) {
                                    GainGreen
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                            Text(
                                "${cycle.start.shortDate()} – ${cycle.endInclusive.shortDate()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { editingBudget = true }) {
                            Text("Change")
                        }
                    }
                    Button(
                        onClick = { addingPurchase = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text(" Add purchase")
                    }
                }
            }
            item {
                SectionLabel("Purchases")
                if (purchases.isEmpty()) {
                    SectionCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Outlined.ShoppingBasket,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "No purchases in this cycle",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            Text(
                                "Add items such as peanuts, milk or rice when you buy them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    SectionCard {
                        purchases.forEachIndexed { index, purchase ->
                            PurchaseRow(
                                purchase = purchase,
                                today = today,
                                onDelete = { deleting = purchase },
                            )
                            if (index != purchases.lastIndex) {
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }

    if (addingPurchase) {
        AddPurchaseSheet(
            onDismiss = { addingPurchase = false },
            onSave = { name, category, amount, date ->
                viewModel.repository.addPurchase(name, category, amount, date)
                addingPurchase = false
                scope.launch { snackbar.showSnackbar("$name added to ${date.shortDate()}") }
            },
        )
    }

    if (editingBudget) {
        BudgetCycleSheet(
            initialBudget = state.monthlyBudget,
            initialStartDay = state.budgetCycleStartDay,
            onDismiss = { editingBudget = false },
            onSave = { budget, startDay ->
                viewModel.repository.setBudget(budget)
                viewModel.repository.setBudgetCycleStartDay(startDay)
                editingBudget = false
                scope.launch { snackbar.showSnackbar("Budget cycle saved") }
            },
        )
    }

    deleting?.let { purchase ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${purchase.itemName}?") },
            text = { Text("₹${purchase.amount.toInt()} will be removed from this cycle.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.repository.removePurchase(purchase.id)
                        deleting = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun PurchaseRow(
    purchase: PurchaseLog,
    today: LocalDate,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    purchase.itemName.firstOrNull()?.uppercase() ?: "•",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(purchase.itemName, style = MaterialTheme.typography.titleSmall)
            Text(
                "${purchase.category} • ${purchase.purchaseDate.dayLabel(today)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "₹${"%.0f".format(purchase.amount)}",
            style = MaterialTheme.typography.titleSmall,
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete ${purchase.itemName}",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AddPurchaseSheet(
    onDismiss: () -> Unit,
    onSave: (String, String, Double, LocalDate) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(purchaseCategories.first()) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var choosingDate by remember { mutableStateOf(false) }
    val parsedAmount = amount.toDoubleOrNull()
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount > 0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add purchase", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Just enter what you bought, its category, price and date.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = { Text("Item") },
                placeholder = { Text("Peanuts") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Category", style = MaterialTheme.typography.labelLarge)
            purchaseCategories.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option, maxLines = 1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.take(10) },
                label = { Text("Price paid") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amount.isNotBlank() && (parsedAmount == null || parsedAmount <= 0),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { choosingDate = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                Text(" ${date.dayLabel(LocalDate.now())}")
            }
            Button(
                enabled = valid,
                onClick = {
                    parsedAmount?.let { onSave(name.trim(), category, it, date) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add to spending")
            }
        }
    }

    if (choosingDate) {
        PurchaseDateDialog(
            initialDate = date,
            onDismiss = { choosingDate = false },
            onSelect = {
                date = it
                choosingDate = false
            },
        )
    }
}

@Composable
private fun PurchaseDateDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun BudgetCycleSheet(
    initialBudget: Double,
    initialStartDay: Int,
    onDismiss: () -> Unit,
    onSave: (Double, Int) -> Unit,
) {
    var budget by remember(initialBudget) { mutableStateOf(initialBudget.toString()) }
    var startDay by remember(initialStartDay) { mutableStateOf(initialStartDay.toString()) }
    val parsedBudget = budget.toDoubleOrNull()
    val parsedDay = startDay.toIntOrNull()
    val valid = parsedBudget != null &&
        parsedBudget > 0 &&
        parsedDay != null &&
        parsedDay in 1..31
    val preview = parsedDay?.takeIf { it in 1..31 }?.let {
        budgetCycleFor(LocalDate.now(), it)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Budget cycle", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Choose when your personal month starts. Spending resets on that cycle date.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it.take(10) },
                label = { Text("Budget") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = budget.isNotBlank() && (parsedBudget == null || parsedBudget <= 0),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = startDay,
                onValueChange = { startDay = it.filter(Char::isDigit).take(2) },
                label = { Text("Cycle start day") },
                supportingText = {
                    Text(
                        preview?.let {
                            "Current cycle: ${it.start.shortDate()} – ${it.endInclusive.shortDate()}"
                        } ?: "Enter a day from 1 to 31"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = startDay.isNotBlank() &&
                    (parsedDay == null || parsedDay !in 1..31),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = valid,
                onClick = {
                    if (parsedBudget != null && parsedDay != null) {
                        onSave(parsedBudget, parsedDay)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save budget cycle")
            }
        }
    }
}

private fun LocalDate.shortDate(): String =
    format(DateTimeFormatter.ofPattern("d MMM"))

private fun LocalDate.dayLabel(today: LocalDate): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> format(DateTimeFormatter.ofPattern("EEE, d MMM"))
}
