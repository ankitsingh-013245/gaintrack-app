@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gaintrack.personal.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gaintrack.personal.MainViewModel
import com.gaintrack.personal.R
import com.gaintrack.personal.data.DietPreference
import com.gaintrack.personal.data.Gender
import com.gaintrack.personal.data.UserProfile
import com.gaintrack.personal.data.parseClock
import com.gaintrack.personal.data.toPlanVersion
import com.gaintrack.personal.ui.GainGreen
import com.gaintrack.personal.ui.SectionCard
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private val onboardingTitles = listOf(
    "Let’s make GainTrack yours",
    "Your starting point",
    "Set your gain target",
    "Food that fits you",
    "Build around your day",
    "Your training setup",
    "Train safely",
    "Your plan is ready",
)

@Composable
fun OnboardingScreen(profile: UserProfile, viewModel: MainViewModel) {
    var draft by remember(profile) { mutableStateOf(profile) }
    var step by remember(profile.onboardingStep) {
        mutableIntStateOf(profile.onboardingStep.coerceIn(0, 7))
    }
    val valid = when (step) {
        0 -> draft.name.isNotBlank()
        1 -> draft.age in 13..100 &&
            draft.heightCm in 120.0..230.0 &&
            draft.currentWeightKg in 30.0..300.0
        2 -> draft.targetWeightKg in 30.0..300.0 &&
            draft.targetWeightKg > draft.currentWeightKg &&
            draft.goalDurationMonths in setOf(1, 2, 3, 6, 9, 12)
        4 -> parseClock(draft.wakeTime) != null &&
            parseClock(draft.workoutTime) != null &&
            parseClock(draft.sleepTime) != null &&
            parseClock(draft.weighInTime) != null
        5 -> draft.trainingDays.isNotEmpty()
        7 -> draft.monthlyFoodBudget > 0
        else -> true
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                IconButton(
                    onClick = {
                        step--
                        draft = draft.copy(onboardingStep = step)
                        viewModel.repository.saveProfileDraft(draft)
                    }
                ) {
                    Icon(Icons.Outlined.ArrowBack, "Previous step")
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            LinearProgressIndicator(
                progress = { (step + 1) / 8f },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text(
                "${step + 1}/8",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding step",
        ) { current ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (current == 0) {
                    item {
                        Image(
                            painter = painterResource(R.drawable.art_coach),
                            contentDescription = "GainTrack coach",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(MaterialTheme.shapes.extraLarge),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                        )
                    }
                }
                item {
                    Text(
                        onboardingTitles[current],
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        onboardingSubtitle(current),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                when (current) {
                    0 -> item {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { draft = draft.copy(name = it.take(32)) },
                            label = { Text("Your name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    1 -> {
                        item {
                            IntField(
                                label = "Age",
                                value = draft.age,
                                range = 13..100,
                                onValue = { draft = draft.copy(age = it) },
                            )
                        }
                        item {
                            DecimalField(
                                label = "Height",
                                suffix = "cm",
                                value = draft.heightCm,
                                onValue = { draft = draft.copy(heightCm = it) },
                            )
                        }
                        item {
                            DecimalField(
                                label = "Current weight",
                                suffix = "kg",
                                value = draft.currentWeightKg,
                                onValue = { draft = draft.copy(currentWeightKg = it) },
                            )
                        }
                        item {
                            Text("Gender (for energy estimate)", style = MaterialTheme.typography.titleSmall)
                            ChoiceRows(
                                choices = Gender.entries,
                                selected = draft.gender,
                                label = {
                                    when (it) {
                                        Gender.FEMALE -> "Female"
                                        Gender.MALE -> "Male"
                                        Gender.NON_BINARY -> "Non-binary"
                                        Gender.PREFER_NOT_TO_SAY -> "Prefer not to say"
                                    }
                                },
                                onSelect = { draft = draft.copy(gender = it) },
                            )
                        }
                    }
                    2 -> {
                        item {
                            DecimalField(
                                label = "Target weight",
                                suffix = "kg",
                                value = maxOf(draft.targetWeightKg, draft.currentWeightKg + 0.1),
                                onValue = { draft = draft.copy(targetWeightKg = it) },
                            )
                            if (draft.targetWeightKg <= draft.currentWeightKg) {
                                Text(
                                    "Your target must be above your current weight.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 5.dp),
                                )
                            }
                        }
                        item {
                            Text("Goal duration", style = MaterialTheme.typography.titleSmall)
                            ChoiceRows(
                                choices = listOf(1, 2, 3, 6, 9, 12),
                                selected = draft.goalDurationMonths,
                                label = { "$it months" },
                                onSelect = {
                                    draft = draft.copy(goalDurationMonths = it)
                                },
                            )
                        }
                        item {
                            val weeklyGain =
                                (draft.targetWeightKg - draft.currentWeightKg) /
                                    (draft.goalDurationMonths * 4.345)
                            InfoCard(
                                icon = Icons.Outlined.FitnessCenter,
                                title = "Weight gain only",
                                text = "Your target path is about %.2f kg per week. GainTrack will compare each weekly check-in with this path."
                                    .format(weeklyGain.coerceAtLeast(0.0)),
                            )
                        }
                    }
                    3 -> {
                        item {
                            ChoiceRows(
                                choices = DietPreference.entries,
                                selected = draft.diet,
                                label = {
                                    when (it) {
                                        DietPreference.EVERYTHING -> "Everything"
                                        DietPreference.VEGETARIAN -> "Vegetarian"
                                        DietPreference.VEGAN -> "Vegan"
                                    }
                                },
                                onSelect = { draft = draft.copy(diet = it) },
                            )
                        }
                        item {
                            Text("Meals per day", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (3..6).forEach { count ->
                                    FilterChip(
                                        selected = draft.mealsPerDay == count,
                                        onClick = { draft = draft.copy(mealsPerDay = count) },
                                        label = { Text(count.toString()) },
                                    )
                                }
                            }
                        }
                        item {
                            InfoCard(
                                icon = Icons.Outlined.Restaurant,
                                title = "Editable, not rigid",
                                text = "Your calorie and protein targets are generated now. Every meal remains editable in Plan.",
                            )
                        }
                    }
                    4 -> {
                        item {
                            TimePickerField(
                                label = "Wake time",
                                value = draft.wakeTime,
                                fallback = LocalTime.of(7, 0),
                                onValue = { draft = draft.copy(wakeTime = it) },
                            )
                        }
                        item {
                            Text(
                                "Weekly weigh-in",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            val locale = LocalConfiguration.current.locales[0]
                            ChoiceRows(
                                choices = DayOfWeek.entries,
                                selected = draft.weighInDay,
                                label = {
                                    it.getDisplayName(TextStyle.SHORT, locale)
                                },
                                onSelect = { draft = draft.copy(weighInDay = it) },
                            )
                        }
                        item {
                            TimePickerField(
                                label = "Weigh-in time",
                                value = draft.weighInTime,
                                fallback = LocalTime.of(8, 0),
                                onValue = { draft = draft.copy(weighInTime = it) },
                            )
                        }
                        item {
                            TimePickerField(
                                label = "Preferred workout time",
                                value = draft.workoutTime,
                                fallback = LocalTime.of(20, 30),
                                onValue = { draft = draft.copy(workoutTime = it) },
                            )
                        }
                        item {
                            TimePickerField(
                                label = "Bed time",
                                value = draft.sleepTime,
                                fallback = LocalTime.of(23, 30),
                                onValue = { draft = draft.copy(sleepTime = it) },
                            )
                        }
                    }
                    5 -> {
                        item {
                            Text("Training days", style = MaterialTheme.typography.titleSmall)
                            DayPicker(
                                selected = draft.trainingDays,
                                onToggle = { day ->
                                    val days = draft.trainingDays.toMutableSet().apply {
                                        if (!add(day)) remove(day)
                                    }
                                    draft = draft.copy(trainingDays = days)
                                },
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = draft.equipment,
                                onValueChange = { draft = draft.copy(equipment = it.take(120)) },
                                label = { Text("Equipment available") },
                                placeholder = { Text("Dumbbells, mat, chair…") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item {
                            InfoCard(
                                icon = Icons.Outlined.FitnessCenter,
                                title = "${draft.trainingDays.size} training days",
                                text = "GainTrack rotates balanced sessions across the days you selected.",
                            )
                        }
                    }
                    6 -> {
                        item {
                            OutlinedTextField(
                                value = draft.injuries,
                                onValueChange = {
                                    draft = draft.copy(
                                        injuries = it.take(240),
                                        loadedTrainingCleared = it.isBlank(),
                                    )
                                },
                                label = { Text("Injuries, pain or medical restrictions") },
                                placeholder = { Text("Leave blank if none") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item {
                            InfoCard(
                                icon = Icons.Outlined.HealthAndSafety,
                                title = if (draft.injuries.isBlank()) {
                                    "No restrictions added"
                                } else {
                                    "Loaded workouts will be held"
                                },
                                text = if (draft.injuries.isBlank()) {
                                    "You can update this any time in Settings."
                                } else {
                                    "GainTrack will keep the plan visible, but set logging stays locked until you confirm professional clearance in Settings."
                                },
                            )
                        }
                    }
                    7 -> {
                        item {
                            DecimalField(
                                label = "Monthly food budget",
                                prefix = "₹",
                                value = draft.monthlyFoodBudget,
                                onValue = { draft = draft.copy(monthlyFoodBudget = it) },
                            )
                        }
                        item {
                            val preview = draft.toPlanVersion(id = "preview")
                            SectionCard(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)
                            ) {
                                Text("Your starting plan", style = MaterialTheme.typography.titleMedium)
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    PlanPreview("${preview.calorieTarget}", "kcal")
                                    PlanPreview("${preview.proteinTarget} g", "protein")
                                    PlanPreview("${preview.trainingDays.size}×", "training")
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) {
                    TextButton(
                        onClick = {
                            step--
                            draft = draft.copy(onboardingStep = step)
                            viewModel.repository.saveProfileDraft(draft)
                        }
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    enabled = valid,
                    onClick = {
                        if (step == 7) {
                            viewModel.repository.completeOnboarding(draft)
                        } else {
                            step++
                            draft = draft.copy(onboardingStep = step)
                            viewModel.repository.saveProfileDraft(draft)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        if (step == 7) Icons.Outlined.Check else Icons.Outlined.ArrowForward,
                        null,
                    )
                    Text(if (step == 7) " Build my plan" else " Continue")
                }
            }
        }
    }
}

@Composable
private fun TimePickerField(
    label: String,
    value: String,
    fallback: LocalTime,
    onValue: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val time = parseClock(value) ?: fallback
    val pickerState = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        is24Hour = true,
    )

    OutlinedButton(
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = String.format(Locale.ROOT, "%02d:%02d", time.hour, time.minute),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(Icons.Outlined.Schedule, contentDescription = "Choose $label")
        }
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValue(
                            String.format(
                                Locale.ROOT,
                                "%02d:%02d",
                                pickerState.hour,
                                pickerState.minute,
                            )
                        )
                        open = false
                    },
                ) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun <T> ChoiceRows(
    choices: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { choice ->
                    FilterChip(
                        selected = selected == choice,
                        onClick = { onSelect(choice) },
                        label = {
                            Text(
                                label(choice),
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DayPicker(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { day ->
                    FilterChip(
                        selected = day in selected,
                        onClick = { onToggle(day) },
                        label = {
                            Text(day.getDisplayName(TextStyle.SHORT, locale))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DecimalField(
    label: String,
    value: Double,
    onValue: (Double) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    prefix: String? = null,
) {
    var text by remember(value) {
        mutableStateOf(
            if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        )
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toDoubleOrNull()?.let(onValue)
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        prefix = prefix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun IntField(
    label: String,
    value: Int,
    range: IntRange,
    onValue: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.takeIf { number -> number in range }?.let(onValue)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(9.dp),
                )
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanPreview(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = GainGreen,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun onboardingSubtitle(step: Int): String = when (step) {
    0 -> "A short setup creates your meals, workouts and weight-gain targets."
    1 -> "These numbers create a practical energy and protein estimate."
    2 -> "GainTrack is built only for gaining weight. Choose your target and timeline."
    3 -> "Choose a pattern you can actually repeat."
    4 -> "Meals, training and one weekly weigh-in should fit your real schedule."
    5 -> "Pick realistic days and tell us what you can train with."
    6 -> "Pain and restrictions change what is safe to recommend."
    else -> "Review the essentials. You can edit everything later."
}
