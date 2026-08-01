@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gaintrack.personal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import com.gaintrack.personal.MainViewModel
import com.gaintrack.personal.data.AppState
import com.gaintrack.personal.data.Exercise
import com.gaintrack.personal.data.FoodTask
import com.gaintrack.personal.data.MealPlanGroup
import com.gaintrack.personal.data.SeedPlan
import com.gaintrack.personal.data.activePlanFor
import com.gaintrack.personal.data.calorieTargetFor
import com.gaintrack.personal.data.foodFor
import com.gaintrack.personal.data.proteinTargetFor
import com.gaintrack.personal.data.workoutFor
import com.gaintrack.personal.ui.AppTopBar
import com.gaintrack.personal.ui.GainGreen
import com.gaintrack.personal.ui.SectionCard
import com.gaintrack.personal.ui.SectionLabel
import com.gaintrack.personal.ui.StatusPill
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private enum class PlanSection(val label: String) {
    OVERVIEW("Overview"),
    MEALS("Meals"),
    TRAINING("Training"),
}

private data class FoodEditorState(
    val group: MealPlanGroup,
    val task: FoodTask,
    val position: Int,
    val itemCount: Int,
    val isNew: Boolean,
)

private data class ExerciseEditorState(
    val workoutName: String,
    val exercise: Exercise,
    val position: Int,
    val itemCount: Int,
    val isNew: Boolean,
)

@Composable
fun PlanScreen(state: AppState, viewModel: MainViewModel) {
    val locale = LocalConfiguration.current.locales[0]
    val uriHandler = LocalUriHandler.current
    var editingFood by remember { mutableStateOf<FoodEditorState?>(null) }
    var editingExercise by remember { mutableStateOf<ExerciseEditorState?>(null) }
    var section by remember { mutableStateOf(PlanSection.OVERVIEW) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val weekdayFood = state.foodFor(dateForMealGroup(MealPlanGroup.WEEKDAY))
    val sundayFood = state.foodFor(dateForMealGroup(MealPlanGroup.SUNDAY))
    val trainingDays = state.activePlanFor()?.trainingDays
        ?.sortedBy { it.value }
        ?: SeedPlan.workouts.keys.toList()

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppTopBar(
                    title = "Your plan",
                    subtitle = "A focused weight-gain plan",
                )
            }
            item {
                TabRow(
                    selectedTabIndex = PlanSection.entries.indexOf(section),
                    containerColor = MaterialTheme.colorScheme.background,
                    divider = {},
                ) {
                    PlanSection.entries.forEach { option ->
                        Tab(
                            selected = section == option,
                            onClick = { section = option },
                            text = { Text(option.label) },
                        )
                    }
                }
            }
            when (section) {
                PlanSection.OVERVIEW -> {
                    item { TargetOverview(state) }
                    item {
                        SectionCard(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = .46f
                            )
                        ) {
                            Text("How to use this plan", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Log each meal, complete the scheduled training, and add one weight check-in each week. The target line follows your chosen ${state.profile.goalDurationMonths}-month timeline.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                PlanSection.MEALS -> {
                    item {
                        SectionLabel("Monday–Saturday")
                        MealGroup(
                            food = weekdayFood,
                            onEdit = { task, position ->
                                editingFood = FoodEditorState(
                                    group = MealPlanGroup.WEEKDAY,
                                    task = task,
                                    position = position,
                                    itemCount = weekdayFood.size,
                                    isNew = false,
                                )
                            },
                            onAdd = {
                                editingFood = FoodEditorState(
                                    group = MealPlanGroup.WEEKDAY,
                                    task = viewModel.repository.newFoodTask(),
                                    position = weekdayFood.size + 1,
                                    itemCount = weekdayFood.size,
                                    isNew = true,
                                )
                            },
                        )
                    }
                    item {
                        SectionLabel("Sunday")
                        MealGroup(
                            food = sundayFood,
                            onEdit = { task, position ->
                                editingFood = FoodEditorState(
                                    group = MealPlanGroup.SUNDAY,
                                    task = task,
                                    position = position,
                                    itemCount = sundayFood.size,
                                    isNew = false,
                                )
                            },
                            onAdd = {
                                editingFood = FoodEditorState(
                                    group = MealPlanGroup.SUNDAY,
                                    task = viewModel.repository.newFoodTask(),
                                    position = sundayFood.size + 1,
                                    itemCount = sundayFood.size,
                                    isNew = true,
                                )
                            },
                        )
                    }
                }
                PlanSection.TRAINING -> {
                    item { SectionLabel("Strength schedule") }
                    items(trainingDays, key = { it.name }) { day ->
                        val workout = state.workoutFor(dateFor(day)) ?: return@items
                        SectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.FitnessCenter, null, tint = GainGreen)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(
                                        day.getDisplayName(TextStyle.FULL, locale),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(workout.name, style = MaterialTheme.typography.titleMedium)
                                }
                                StatusPill("${workout.exercises.size} moves")
                            }
                            workout.exercises.forEachIndexed { index, exercise ->
                                if (index == 0) {
                                    HorizontalDivider(
                                        Modifier.padding(top = 14.dp, bottom = 5.dp)
                                    )
                                } else {
                                    HorizontalDivider(Modifier.padding(vertical = 5.dp))
                                }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editingExercise = ExerciseEditorState(
                                                workoutName = workout.name,
                                                exercise = exercise,
                                                position = index + 1,
                                                itemCount = workout.exercises.size,
                                                isNew = false,
                                            )
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            exercise.name,
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            "${exercise.weight} • ${exercise.sets} × ${exercise.reps}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(
                                        onClick = { uriHandler.openUri(exercise.learnUrl) }
                                    ) {
                                        Icon(
                                            Icons.Outlined.OpenInNew,
                                            "Watch exercise form",
                                            tint = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            editingExercise = ExerciseEditorState(
                                                workoutName = workout.name,
                                                exercise = exercise,
                                                position = index + 1,
                                                itemCount = workout.exercises.size,
                                                isNew = false,
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Edit, "Edit exercise")
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    editingExercise = ExerciseEditorState(
                                        workoutName = workout.name,
                                        exercise = viewModel.repository.newExercise(),
                                        position = workout.exercises.size + 1,
                                        itemCount = workout.exercises.size,
                                        isNew = true,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Text(" Add exercise")
                            }
                        }
                    }
                    item {
                        SectionCard(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = .46f
                            )
                        ) {
                            Text("Progression", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Week 1 uses 2 sets, week 2 uses 3, then the full prescription. Add reps with clean form; raise weight only after reaching the top of the range twice.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }

    editingFood?.let { editor ->
        FoodEditSheet(
            task = editor.task,
            initialPosition = editor.position,
            maxPosition = editor.itemCount + if (editor.isNew) 1 else 0,
            isNew = editor.isNew,
            onDismiss = { editingFood = null },
            onSave = { task, position ->
                viewModel.repository.upsertFoodTask(editor.group, task, position)
                editingFood = null
                scope.launch {
                    snackbar.showSnackbar(if (editor.isNew) "Meal added" else "Meal saved")
                }
            },
            onDelete = {
                viewModel.repository.deleteFoodTask(editor.group, editor.task.id)
                editingFood = null
                scope.launch { snackbar.showSnackbar("Meal deleted") }
            },
        )
    }
    editingExercise?.let { editor ->
        ExerciseEditSheet(
            exercise = editor.exercise,
            initialPosition = editor.position,
            maxPosition = editor.itemCount + if (editor.isNew) 1 else 0,
            isNew = editor.isNew,
            onDismiss = { editingExercise = null },
            onSave = { exercise, position ->
                viewModel.repository.upsertExercise(
                    editor.workoutName,
                    exercise,
                    position,
                )
                editingExercise = null
                scope.launch {
                    snackbar.showSnackbar(
                        if (editor.isNew) "Exercise added" else "Exercise saved"
                    )
                }
            },
            onDelete = {
                viewModel.repository.deleteExercise(
                    editor.workoutName,
                    editor.exercise.id,
                )
                editingExercise = null
                scope.launch { snackbar.showSnackbar("Exercise deleted") }
            },
        )
    }
}

@Composable
private fun TargetOverview(state: AppState) {
    val plan = state.activePlanFor()
    val startWeight = plan?.startWeightKg ?: state.profile.currentWeightKg
    val targetWeight = plan?.targetWeightKg ?: state.profile.targetWeightKg
    val targetDate = plan?.targetDate
    val weeks = plan?.let {
        ChronoUnit.DAYS.between(it.effectiveFrom, it.targetDate).coerceAtLeast(1) / 7.0
    } ?: (state.profile.goalDurationMonths * 4.345)
    val weeklyGain = (targetWeight - startWeight) / weeks
    SectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .46f)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    "%.1f kg".format(startWeight),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("starting point", style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.1f kg".format(targetWeight),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    targetDate?.let { "goal by ${it.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))}" }
                        ?: "${state.profile.goalDurationMonths}-month goal",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 14.dp))
        PlanLine("Weekly target", "%.2f kg".format(weeklyGain.coerceAtLeast(0.0)))
        PlanLine("Daily intake", "${state.calorieTargetFor()} kcal")
        PlanLine("Protein", "${state.proteinTargetFor()} g")
        PlanLine("Training", "${trainingDaysCount(state)} days weekly")
        PlanLine("Sleep", "At least 7 hours")
    }
}

private fun trainingDaysCount(state: AppState): Int =
    state.activePlanFor()?.trainingDays?.size ?: SeedPlan.workouts.size

@Composable
private fun MealGroup(
    food: List<FoodTask>,
    onEdit: (FoodTask, Int) -> Unit,
    onAdd: () -> Unit,
) {
    SectionCard {
        if (food.isEmpty()) {
            Text(
                "No meals in this plan yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        food.forEachIndexed { index, task ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(task, index + 1) }
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Restaurant, null, tint = GainGreen)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(task.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${task.time} • ${task.detail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${task.calories} kcal\n${task.protein} g",
                    style = MaterialTheme.typography.labelMedium,
                    color = GainGreen,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            if (index != food.lastIndex) {
                HorizontalDivider(Modifier.padding(vertical = 3.dp))
            }
        }
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(" Add meal")
        }
    }
}

@Composable
private fun PlanLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun dateFor(day: DayOfWeek): LocalDate {
    var date = LocalDate.now()
    while (date.dayOfWeek != day) date = date.plusDays(1)
    return date
}

private fun dateForMealGroup(group: MealPlanGroup): LocalDate {
    var date = LocalDate.now()
    while (
        (group == MealPlanGroup.SUNDAY) !=
        (date.dayOfWeek == DayOfWeek.SUNDAY)
    ) {
        date = date.plusDays(1)
    }
    return date
}

@Composable
private fun FoodEditSheet(
    task: FoodTask,
    initialPosition: Int,
    maxPosition: Int,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (FoodTask, Int) -> Unit,
    onDelete: () -> Unit,
) {
    var time by remember(task) { mutableStateOf(task.time) }
    var title by remember(task) { mutableStateOf(task.title) }
    var detail by remember(task) { mutableStateOf(task.detail) }
    var calories by remember(task) { mutableStateOf(task.calories.toString()) }
    var protein by remember(task) { mutableStateOf(task.protein.toString()) }
    var position by remember(task) { mutableStateOf(initialPosition.toString()) }
    var confirmingDelete by remember(task) { mutableStateOf(false) }
    val positionLimit = maxPosition.coerceAtLeast(1)
    val caloriesValue = calories.toIntOrNull()
    val proteinValue = protein.toIntOrNull()
    val positionValue = position.toIntOrNull()
    val valid = title.isNotBlank() &&
        caloriesValue != null && caloriesValue in 0..5000 &&
        proteinValue != null && proteinValue in 0..500 &&
        positionValue != null && positionValue in 1..positionLimit

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    if (isNew) "Add meal" else "Edit meal",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Set its position to place it anywhere in this meal plan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedTextField(
                    time,
                    { time = it },
                    label = { Text("Time") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    title,
                    { title = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    detail,
                    { detail = it },
                    label = { Text("Food and amount") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
            item {
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it.filter(Char::isDigit).take(2) },
                    label = { Text("Position (1-$positionLimit)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = position.isNotBlank() &&
                        (positionValue == null || positionValue !in 1..positionLimit),
                    supportingText = {
                        Text("1 places it first; $positionLimit places it last.")
                    },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        calories,
                        { calories = it },
                        label = { Text("kcal") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        protein,
                        { protein = it },
                        label = { Text("Protein g") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            }
            item {
                Button(
                    enabled = valid,
                    onClick = {
                        onSave(
                            task.copy(
                                time = time.trim(),
                                title = title.trim(),
                                detail = detail.trim(),
                                calories = caloriesValue ?: 0,
                                protein = proteinValue ?: 0,
                            ),
                            positionValue ?: 1,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isNew) "Add meal" else "Save meal")
                }
            }
            if (!isNew) {
                item {
                    OutlinedButton(
                        onClick = { confirmingDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(" Delete meal", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete ${task.title}?") },
            text = { Text("This meal will be removed from this plan.") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun ExerciseEditSheet(
    exercise: Exercise,
    initialPosition: Int,
    maxPosition: Int,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Exercise, Int) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(exercise) { mutableStateOf(exercise.name) }
    var weight by remember(exercise) { mutableStateOf(exercise.weight) }
    var sets by remember(exercise) { mutableStateOf(exercise.sets.toString()) }
    var reps by remember(exercise) { mutableStateOf(exercise.reps) }
    var technique by remember(exercise) { mutableStateOf(exercise.technique) }
    var position by remember(exercise) { mutableStateOf(initialPosition.toString()) }
    var confirmingDelete by remember(exercise) { mutableStateOf(false) }
    val positionLimit = maxPosition.coerceAtLeast(1)
    val setsValue = sets.toIntOrNull()
    val positionValue = position.toIntOrNull()
    val valid = name.isNotBlank() &&
        setsValue != null && setsValue in 1..20 &&
        reps.isNotBlank() &&
        positionValue != null && positionValue in 1..positionLimit

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    if (isNew) "Add exercise" else "Edit exercise",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Choose a position, then keep the prescription clear and specific.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("Exercise") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    weight,
                    { weight = it },
                    label = { Text("Starting weight") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        sets,
                        { sets = it },
                        label = { Text("Sets") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        reps,
                        { reps = it },
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }
            item {
                OutlinedTextField(
                    technique,
                    { technique = it },
                    label = { Text("Technique") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
            item {
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it.filter(Char::isDigit).take(2) },
                    label = { Text("Position (1-$positionLimit)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = position.isNotBlank() &&
                        (positionValue == null || positionValue !in 1..positionLimit),
                    supportingText = {
                        Text("1 places it first; $positionLimit places it last.")
                    },
                )
            }
            item {
                Button(
                    enabled = valid,
                    onClick = {
                        onSave(
                            exercise.copy(
                                name = name.trim(),
                                weight = weight.trim(),
                                sets = setsValue ?: 1,
                                reps = reps.trim(),
                                technique = technique.trim(),
                            ),
                            positionValue ?: 1,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isNew) "Add exercise" else "Save exercise")
                }
            }
            if (!isNew) {
                item {
                    OutlinedButton(
                        onClick = { confirmingDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(" Delete exercise", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete ${exercise.name}?") },
            text = { Text("This exercise will be removed from this workout.") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
