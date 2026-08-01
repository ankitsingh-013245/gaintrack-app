@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gaintrack.personal.ui.screens

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gaintrack.personal.MainViewModel
import com.gaintrack.personal.BuildConfig
import com.gaintrack.personal.data.AppState
import com.gaintrack.personal.reminder.MotivationWorker
import com.gaintrack.personal.ui.GainGreen
import com.gaintrack.personal.ui.SectionCard
import com.gaintrack.personal.ui.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(state: AppState, viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var budgetSheet by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                    Column {
                        Text("Settings", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Preferences and privacy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                SectionLabel("Personalization")
                SectionCard(Modifier.clickable { viewModel.repository.restartOnboarding() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AccountCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                state.profile.name.ifBlank { "Your profile" },
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "${state.profile.currentWeightKg.toInt()} → " +
                                    "${state.profile.targetWeightKg.toInt()} kg • " +
                                    "${state.profile.goalDurationMonths} months • " +
                                    "${state.profile.weighInDay.name.lowercase().replaceFirstChar { it.uppercase() }} weigh-in",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, "Edit profile")
                    }
                }
            }
            if (state.profile.injuries.isNotBlank()) {
                item {
                    SectionCard {
                        SettingToggleRow(
                            icon = Icons.Outlined.HealthAndSafety,
                            title = "Loaded training clearance",
                            description = "Turn on only after a qualified professional has cleared the restriction you entered.",
                            checked = state.profile.loadedTrainingCleared,
                            onCheckedChange = viewModel.repository::setLoadedTrainingCleared,
                        )
                    }
                }
            }
            item {
                SectionCard(
                    Modifier.clickable {
                        viewModel.repository.resetTours()
                        onBack()
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Replay,
                            null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("Replay screen tours", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Show the first-visit guide again.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, "Replay")
                    }
                }
            }
            item {
                SectionLabel("Reminders")
                SectionCard {
                    SettingToggleRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Motivation and meal reminders",
                        description = "Daily perspective plus one alert when a meal is 30 minutes overdue.",
                        checked = state.dailyNotificationEnabled,
                        onCheckedChange = viewModel::setDailyNotifications,
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    SettingToggleRow(
                        icon = Icons.Outlined.PhoneAndroid,
                        title = "Gentle unlock check-in",
                        description = "Optional first-unlock health check-in after 7 a.m.",
                        checked = state.unlockReminderEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !Settings.canDrawOverlays(context)) {
                                context.startActivity(viewModel.overlaySettingsIntent())
                            } else {
                                viewModel.setUnlockReminder(enabled)
                            }
                        },
                    )
                    if (!Settings.canDrawOverlays(context)) {
                        Button(
                            onClick = { context.startActivity(viewModel.overlaySettingsIntent()) },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        ) {
                            Icon(Icons.Outlined.OpenInNew, null)
                            Text(" Allow display over apps")
                        }
                    }
                    Button(
                        onClick = {
                            WorkManager.getInstance(context).enqueue(
                                OneTimeWorkRequestBuilder<MotivationWorker>().build()
                            )
                            scope.launch { snackbar.showSnackbar("Motivation refresh requested") }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Text("Refresh motivation now")
                    }
                }
            }
            item {
                SectionLabel("Planning")
                SectionCard(Modifier.clickable { budgetSheet = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Payments, null, tint = GainGreen)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("Monthly food budget", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "₹${state.monthlyBudget.toInt()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Cycle starts on day ${state.budgetCycleStartDay}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, "Edit")
                    }
                }
            }
            item {
                SectionLabel("About your data")
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, null, tint = GainGreen)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Stored on this phone", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Profile, food, weight, exercise, sleep, notes, shopping, and meditation logs stay local. " +
                                    "Only motivation image requests use the internet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "GainTrack ${BuildConfig.VERSION_NAME} • Personal healthy-bulk tracker",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }

    if (budgetSheet) {
        BudgetSheet(
            initial = state.monthlyBudget,
            onDismiss = { budgetSheet = false },
            onSave = {
                viewModel.repository.setBudget(it)
                budgetSheet = false
                scope.launch { snackbar.showSnackbar("Budget saved") }
            },
        )
    }
}

@Composable
private fun SettingToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = GainGreen)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BudgetSheet(
    initial: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var budget by remember(initial) { mutableStateOf(initial.toInt().toString()) }
    val amount = budget.toDoubleOrNull()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Monthly food budget", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Shopping compares the purchases you add with this limit. Change the cycle start day from Shopping.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it },
                label = { Text("Budget") },
                prefix = { Text("₹") },
                isError = budget.isNotBlank() && (amount == null || amount <= 0),
                supportingText = {
                    if (amount == null || amount <= 0) {
                        Text("Enter an amount greater than zero.")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = amount != null && amount > 0,
                onClick = { onSave(amount!!) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save budget")
            }
        }
    }
}
