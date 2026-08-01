@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gaintrack.personal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import com.gaintrack.personal.MainViewModel
import com.gaintrack.personal.R
import com.gaintrack.personal.data.AppState
import com.gaintrack.personal.data.DaySnapshot
import com.gaintrack.personal.data.Exercise
import com.gaintrack.personal.data.ExtraFoodLog
import com.gaintrack.personal.data.FoodTask
import com.gaintrack.personal.data.SeedPlan
import com.gaintrack.personal.data.activePlanFor
import com.gaintrack.personal.data.calorieTargetFor
import com.gaintrack.personal.data.foodFor
import com.gaintrack.personal.data.proteinTargetFor
import com.gaintrack.personal.data.nextFutureMeal
import com.gaintrack.personal.data.minutesFor
import com.gaintrack.personal.data.overdueMeals
import com.gaintrack.personal.data.targetWeightFor
import com.gaintrack.personal.data.weekStart
import com.gaintrack.personal.data.weeklyWeighInStatus
import com.gaintrack.personal.data.weeklyWeights
import com.gaintrack.personal.data.WeeklyWeighInStatus
import com.gaintrack.personal.data.workoutFor
import com.gaintrack.personal.ui.AppTopBar
import com.gaintrack.personal.ui.GainAmber
import com.gaintrack.personal.ui.GainGreen
import com.gaintrack.personal.ui.GoalRing
import com.gaintrack.personal.ui.LocalMotionEnabled
import com.gaintrack.personal.ui.MetricTile
import com.gaintrack.personal.ui.SectionCard
import com.gaintrack.personal.ui.SectionLabel
import com.gaintrack.personal.ui.ShimmerBlock
import com.gaintrack.personal.ui.StatusPill
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TodayScreen(state: AppState, viewModel: MainViewModel, onSettings: () -> Unit) {
    val date = state.selectedDate
    val snapshot = state.snapshots[date.toString()] ?: DaySnapshot(date)
    val food = state.foodFor(date)
    val workout = state.workoutFor(date)
    val loggedMeditationMinutes = state.meditation.minutesFor(date)
    val completedFood = food.filter { it.id in snapshot.completedFood }
    val extraFoods = state.extraFoodLogs.filter { it.date == date }
    val calories = completedFood.sumOf { it.calories } + extraFoods.sumOf { it.calories }
    val protein = completedFood.sumOf { it.protein } + extraFoods.sumOf { it.protein }
    val plannedCalories = state.calorieTargetFor(date)
    val plannedProtein = state.proteinTargetFor(date)
    val plan = state.activePlanFor(date)
    val planStartDate = plan?.effectiveFrom ?: date
    val planEndDate = plan?.targetDate
        ?: planStartDate.plusMonths(state.profile.goalDurationMonths.toLong())
    val startWeight = plan?.startWeightKg ?: state.profile.currentWeightKg
    val finalTarget = plan?.targetWeightKg ?: state.profile.targetWeightKg
    val goalDays = ChronoUnit.DAYS.between(planStartDate, planEndDate).coerceAtLeast(1)
    val weeklyGainTarget = (finalTarget - startWeight) / goalDays * 7
    val weekTargetDate = minOf(weekStart(date).plusDays(6), planEndDate)
    val weekTargetWeight = state.targetWeightFor(weekTargetDate)
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(date) {
        while (true) {
            now = LocalDateTime.now()
            delay(30_000)
        }
    }
    val nextMeal = state.nextFutureMeal(
        date,
        if (date == now.toLocalDate()) now.toLocalTime() else LocalTime.MIN,
    )
    val overdue = if (date == now.toLocalDate()) {
        overdueMeals(state, now)
    } else {
        com.gaintrack.personal.data.OverdueMealSummary(emptyList(), 0, 0)
    }
    val weighInStatus = weeklyWeighInStatus(state.profile, state.snapshots.values, now)
    val currentWeekWeight = weeklyWeights(state.snapshots.values, now.toLocalDate())
        .lastOrNull()
        ?.takeIf { it.weekStart == weekStart(now.toLocalDate()) }
    var weightSheet by remember { mutableStateOf(false) }
    var sleepSheet by remember { mutableStateOf(false) }
    var workoutSheet by remember { mutableStateOf(false) }
    var meditationLogDialog by remember { mutableStateOf(false) }
    var extraFoodSheet by remember { mutableStateOf(false) }
    var foodExpanded by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppTopBar(
                    title = "${greeting()}, ${state.profile.name.substringBefore(' ')}",
                    subtitle = "$plannedCalories kcal • $plannedProtein g protein",
                    action = {
                        Image(
                            painter = painterResource(R.drawable.art_coach),
                            contentDescription = null,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                        )
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Outlined.Settings, "Settings")
                        }
                    },
                )
            }
            item {
                DateSwitcher(
                    date = date,
                    onSelect = viewModel.repository::selectDate,
                )
            }
            item {
                SectionLabel("Daily perspective")
                MotivationHero(state)
            }
            item {
                NutritionSummary(
                    calories = calories,
                    calorieTarget = plannedCalories,
                    protein = protein,
                    proteinTarget = plannedProtein,
                    weeklyGainTarget = weeklyGainTarget,
                    weekTargetWeight = weekTargetWeight,
                    weekTargetDate = weekTargetDate,
                    onAddExtraFood = { extraFoodSheet = true },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        label = "Weekly weight",
                        value = currentWeekWeight?.weightKg?.let {
                            "%.1f kg".format(it)
                        } ?: when (weighInStatus) {
                            WeeklyWeighInStatus.UPCOMING -> "Upcoming"
                            WeeklyWeighInStatus.DUE -> "Due now"
                            WeeklyWeighInStatus.COMPLETE -> "Complete"
                        },
                        supporting = when (weighInStatus) {
                            WeeklyWeighInStatus.UPCOMING ->
                                "${state.profile.weighInDay.name.lowercase().replaceFirstChar { it.uppercase() }} • ${state.profile.weighInTime}"
                            WeeklyWeighInStatus.DUE -> "Log once for this week"
                            WeeklyWeighInStatus.COMPLETE -> currentWeekWeight?.loggedOn
                                ?.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
                                ?: "Done this week"
                        },
                        icon = Icons.Outlined.Scale,
                        onClick = { weightSheet = true },
                        modifier = Modifier.weight(1f),
                    )
                    MetricTile(
                        label = "Sleep",
                        value = snapshot.sleepHours?.let { "%.1f h".format(it) } ?: "Log",
                        icon = Icons.Outlined.Bedtime,
                        onClick = { sleepSheet = true },
                        modifier = Modifier.weight(1f),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            if (overdue.meals.isNotEmpty()) {
                item {
                    OverdueMealsCard(
                        count = overdue.meals.size,
                        calories = overdue.calories,
                        protein = overdue.protein,
                    )
                }
            }
            item {
                SectionLabel("Up next")
                SectionCard(
                    Modifier.clickable(
                        enabled = nextMeal != null,
                        onClick = {
                            nextMeal?.let {
                                viewModel.repository.toggleFood(date, it.id)
                                scope.launch { snackbar.showSnackbar("${it.title} logged") }
                            }
                        }
                    ),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .48f),
                ) {
                    if (nextMeal == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val allDone = completedFood.size == food.size
                            Icon(
                                if (allDone) Icons.Outlined.Check else Icons.Outlined.Schedule,
                                null,
                                tint = if (allDone) GainGreen else GainAmber,
                            )
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(
                                    if (allDone) "Food plan complete" else "No upcoming timed meal",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    if (allDone) {
                                        "All ${food.size} planned meals are checked in."
                                    } else {
                                        "Review overdue or flexible-time meals in the full list."
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Schedule, null, tint = GainAmber)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(nextMeal.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${nextMeal.time} • ${nextMeal.calories} kcal • ${nextMeal.protein} g protein\n${nextMeal.detail}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text("Log", color = GainGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                SectionLabel(
                    title = "Meals • ${completedFood.size}/${food.size}",
                    action = {
                        TextButton(onClick = { foodExpanded = !foodExpanded }) {
                            Text(if (foodExpanded) "Show less" else "Show all")
                        }
                    },
                )
                SectionCard {
                    val visibleFood = if (foodExpanded) food else food.take(3)
                    visibleFood.forEachIndexed { index, task ->
                        FoodRow(
                            task = task,
                            checked = task.id in snapshot.completedFood,
                            onToggle = {
                                val wasComplete = task.id in snapshot.completedFood
                                viewModel.repository.toggleFood(date, task.id)
                                if (!wasComplete) {
                                    scope.launch { snackbar.showSnackbar("${task.title} logged") }
                                }
                            },
                        )
                        if (index != visibleFood.lastIndex) {
                            HorizontalDivider(Modifier.padding(vertical = 5.dp))
                        }
                    }
                }
            }
            if (extraFoods.isNotEmpty()) {
                item {
                    SectionLabel(
                        title = "Extra food • ${extraFoods.size}",
                        action = {
                            TextButton(onClick = { extraFoodSheet = true }) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Text(" Add")
                            }
                        },
                    )
                    SectionCard {
                        extraFoods.forEachIndexed { index, extra ->
                            ExtraFoodRow(
                                food = extra,
                                onDelete = {
                                    viewModel.repository.removeExtraFood(extra.id)
                                },
                            )
                            if (index != extraFoods.lastIndex) {
                                HorizontalDivider(Modifier.padding(vertical = 5.dp))
                            }
                        }
                    }
                }
            }
            item {
                SectionLabel("Training")
                WorkoutSummary(
                    workoutName = workout?.name,
                    exerciseCount = workout?.exercises?.size ?: 0,
                    done = snapshot.workoutDone,
                    skipped = snapshot.workoutSkipped,
                    onClick = { if (workout != null) workoutSheet = true },
                )
            }
            item {
                SectionLabel("Meditation")
                MeditationLogCard(
                    minutes = loggedMeditationMinutes,
                    referenceDate = date,
                    completedDates = state.meditation.completedDates,
                    onMark = { meditationLogDialog = true },
                    onRemove = {
                        viewModel.repository.setMeditationLog(date, null)
                        scope.launch { snackbar.showSnackbar("Meditation log removed") }
                    },
                )
            }
            item {
                OutlinedTextField(
                    value = snapshot.notes,
                    onValueChange = { viewModel.repository.setNotes(date, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Daily notes") },
                    placeholder = { Text("Energy, appetite, pain, or anything useful") },
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                )
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }

    if (weightSheet) {
        NumberSheet(
            title = "Log weight",
            suffix = "kg",
            initial = snapshot.weightKg,
            onDismiss = { weightSheet = false },
            onSave = {
                viewModel.repository.setWeight(date, it)
                weightSheet = false
                scope.launch {
                    snackbar.showSnackbar(
                        "Weight saved for ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
                    )
                }
            },
        )
    }
    if (extraFoodSheet) {
        ExtraFoodSheet(
            date = date,
            onDismiss = { extraFoodSheet = false },
            onSave = { name, extraCalories, extraProtein ->
                viewModel.repository.addExtraFood(
                    date = date,
                    name = name,
                    calories = extraCalories,
                    protein = extraProtein,
                )
                extraFoodSheet = false
                scope.launch { snackbar.showSnackbar("Extra food added") }
            },
        )
    }
    if (sleepSheet) {
        NumberSheet(
            title = "Sleep duration",
            suffix = "hours",
            initial = snapshot.sleepHours,
            onDismiss = { sleepSheet = false },
            onSave = {
                viewModel.repository.setSleep(date, it)
                sleepSheet = false
                scope.launch { snackbar.showSnackbar("Sleep saved") }
            },
        )
    }
    if (workoutSheet && workout != null) {
        WorkoutSheet(
            state = state,
            date = date,
            name = workout.name,
            exercises = workout.exercises,
            viewModel = viewModel,
            onDismiss = { workoutSheet = false },
            onFinished = {
                viewModel.repository.setWorkoutStatus(date, done = true)
                workoutSheet = false
                scope.launch { snackbar.showSnackbar("Workout complete — strong work") }
            },
            onSkipped = {
                viewModel.repository.setWorkoutStatus(date, done = false, skipped = true)
                workoutSheet = false
                scope.launch { snackbar.showSnackbar("Workout marked skipped") }
            },
        )
    }
    if (meditationLogDialog) {
        MeditationLogDialog(
            date = date,
            initialMinutes = loggedMeditationMinutes,
            onDismiss = { meditationLogDialog = false },
            onSave = { minutes ->
                viewModel.repository.setMeditationLog(date, minutes)
                meditationLogDialog = false
                scope.launch {
                    snackbar.showSnackbar(
                        "$minutes min meditation saved for " +
                            date.format(DateTimeFormatter.ofPattern("d MMM"))
                    )
                }
            },
        )
    }
}

@Composable
private fun DateSwitcher(
    date: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    Column {
        Text(
            date.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (-2L..2L).forEach { offset ->
                val option = date.plusDays(offset)
                val selected = option == date
                Surface(
                    modifier = Modifier.weight(1f).clickable { onSelect(option) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
                    },
                ) {
                    Column(
                        Modifier.padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            option.format(DateTimeFormatter.ofPattern("EEE")).take(2),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            option.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionSummary(
    calories: Int,
    calorieTarget: Int,
    protein: Int,
    proteinTarget: Int,
    weeklyGainTarget: Double,
    weekTargetWeight: Double,
    weekTargetDate: LocalDate,
    onAddExtraFood: () -> Unit,
) {
    SectionCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f)) {
        Text("Today’s nutrition", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GoalRing(
                    progress = if (calorieTarget <= 0) {
                        0f
                    } else {
                        calories.toFloat() / calorieTarget
                    },
                    primaryText = "$calories",
                    secondaryText = "of $calorieTarget kcal",
                    size = 126.dp,
                )
                Text("Calories", style = MaterialTheme.typography.labelLarge)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GoalRing(
                    progress = if (proteinTarget <= 0) {
                        0f
                    } else {
                        protein.toFloat() / proteinTarget
                    },
                    primaryText = "$protein g",
                    secondaryText = "of $proteinTarget g",
                    size = 126.dp,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text("Protein", style = MaterialTheme.typography.labelLarge)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Scale,
                contentDescription = null,
                tint = GainGreen,
            )
            Column(Modifier.padding(start = 10.dp)) {
                Text(
                    "This week’s target: %.1f kg".format(weekTargetWeight),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "+%.2f kg/week plan pace • by %s".format(
                        weeklyGainTarget.coerceAtLeast(0.0),
                        weekTargetDate.format(DateTimeFormatter.ofPattern("EEE")),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FilledTonalButton(
            onClick = onAddExtraFood,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(" Add extra food")
        }
    }
}

@Composable
private fun ExtraFoodRow(
    food: ExtraFoodLog,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${food.calories} kcal • ${food.protein} g protein",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete ${food.name}",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ExtraFoodSheet(
    date: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    val caloriesValue = calories.toIntOrNull()
    val proteinValue = protein.toIntOrNull()
    val valid = (caloriesValue ?: 0) > 0 || (proteinValue ?: 0) > 0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Add extra food • ${date.format(DateTimeFormatter.ofPattern("d MMM"))}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Add anything eaten outside the planned meals. That day’s circles update instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = { Text("Food name (optional)") },
                placeholder = { Text("Peanuts, milk, banana…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it.filter(Char::isDigit).take(4) },
                label = { Text("Calories") },
                suffix = { Text("kcal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = protein,
                onValueChange = { protein = it.filter(Char::isDigit).take(3) },
                label = { Text("Protein") },
                suffix = { Text("g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        name.ifBlank { "Extra food" },
                        caloriesValue ?: 0,
                        proteinValue ?: 0,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add food")
            }
        }
    }
}

@Composable
private fun OverdueMealsCard(count: Int, calories: Int, protein: Int) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text("😔", style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    if (count == 1) "1 meal is overdue" else "$count meals are overdue",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    "You’re $calories kcal and $protein g protein behind schedule. Check off anything you already ate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FoodRow(task: FoodTask, checked: Boolean, onToggle: () -> Unit) {
    val motion = LocalMotionEnabled.current
    val circleColor by animateColorAsState(
        targetValue = if (checked) GainGreen else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(if (motion) 180 else 0),
        label = "meal check",
    )
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(circleColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (checked) Icons.Outlined.Check else Icons.Outlined.Schedule,
                null,
                tint = if (checked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleSmall)
            Text(
                "${task.time} • ${task.detail}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "${task.calories} kcal\n${task.protein} g",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun WorkoutSummary(
    workoutName: String?,
    exerciseCount: Int,
    done: Boolean,
    skipped: Boolean,
    onClick: () -> Unit,
) {
    SectionCard(Modifier.clickable(enabled = workoutName != null, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (workoutName == null) Icons.Outlined.Bedtime else Icons.Outlined.FitnessCenter,
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(workoutName ?: "Recovery day", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (workoutName == null) {
                        "Eat well, sleep well, and keep activity easy."
                    } else {
                        "$exerciseCount exercises • about 45 minutes"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (workoutName != null) {
                StatusPill(
                    when {
                        done -> "Done"
                        skipped -> "Skipped"
                        else -> "Open"
                    },
                    when {
                        done -> GainGreen
                        skipped -> GainAmber
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}

@Composable
private fun MotivationHero(state: AppState) {
    var retry by remember(state.motivation.imageUrl) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val request = remember(state.motivation.imageUrl, retry) {
        ImageRequest.Builder(context)
            .data(state.motivation.imageUrl)
            .memoryCacheKey("${state.motivation.imageUrl}-$retry")
            .build()
    }
    Box(
        Modifier.fillMaxWidth().height(205.dp).clip(MaterialTheme.shapes.large)
    ) {
        if (state.motivation.imageUrl.isBlank()) {
            MotivationFallback()
        } else {
            SubcomposeAsyncImage(
                model = request,
                contentDescription = "Daily motivation",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            ) {
                val painterState by painter.state.collectAsState()
                when (painterState) {
                    is AsyncImagePainter.State.Loading,
                    is AsyncImagePainter.State.Empty -> {
                        ShimmerBlock(Modifier.fillMaxSize())
                    }
                    is AsyncImagePainter.State.Error -> {
                        MotivationFallback()
                        TextButton(
                            onClick = { retry++ },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        ) {
                            Icon(Icons.Outlined.Refresh, null, tint = Color.White)
                            Text("Retry", color = Color.White)
                        }
                    }
                    else -> SubcomposeAsyncImageContent()
                }
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xE1000000)))
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text(
                "“${state.motivation.quote}”",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "— ${state.motivation.author}",
                color = Color.White.copy(alpha = .82f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (state.motivation.imageAuthor.isNotBlank()) {
                Text(
                    "Photo: ${state.motivation.imageAuthor} / Pexels",
                    color = Color.White.copy(alpha = .62f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun MotivationFallback() {
    Image(
        painter = painterResource(R.drawable.art_coach),
        contentDescription = "Daily coach",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
    )
}

@Composable
private fun NumberSheet(
    title: String,
    suffix: String,
    initial: Double?,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial?.toString() ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                suffix = { Text(suffix) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = text.isBlank() || text.toDoubleOrNull() != null,
                onClick = { onSave(text.toDoubleOrNull()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun WorkoutSheet(
    state: AppState,
    date: LocalDate,
    name: String,
    exercises: List<Exercise>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onFinished: () -> Unit,
    onSkipped: () -> Unit,
) {
    val trainingBlocked =
        state.profile.injuries.isNotBlank() && !state.profile.loadedTrainingCleared
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Image(
                    painter = painterResource(R.drawable.art_workout),
                    contentDescription = "Workout guidance",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .clip(MaterialTheme.shapes.large),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                )
                Text(name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Warm up 5 min • keep 2 clean reps in reserve",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (trainingBlocked) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(
                                    "Set logging is on hold",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "You entered: ${state.profile.injuries}. Review the exercises, then enable loaded training only after professional clearance in Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }
            items(exercises, key = { it.id }) { exercise ->
                ExerciseLogger(
                    state = state,
                    date = date,
                    workout = name,
                    exercise = exercise,
                    viewModel = viewModel,
                    canLog = !trainingBlocked,
                )
            }
            item {
                Button(
                    onClick = onFinished,
                    enabled = !trainingBlocked,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Check, null)
                    Text(" Finish workout")
                }
                TextButton(onClick = onSkipped, modifier = Modifier.fillMaxWidth()) {
                    Text("Mark skipped")
                }
            }
        }
    }
}

@Composable
private fun ExerciseLogger(
    state: AppState,
    date: LocalDate,
    workout: String,
    exercise: Exercise,
    viewModel: MainViewModel,
    canLog: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val sets = SeedPlan.prescribedSets(date, exercise)
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${exercise.weight} • $sets × ${exercise.reps}",
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${exercise.muscles} • ${exercise.equipment}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AnimatedVisibility(expanded) {
                Column(
                    Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        exercise.technique,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .6f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                exercise.commonMistake,
                                modifier = Modifier.padding(start = 7.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(exercise.learnUrl) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.OpenInNew, null)
                        Text(" Watch proper form")
                    }
                    if (canLog) {
                        repeat(sets) { index ->
                            val existing = state.exerciseLogs.lastOrNull {
                                it.date == date.toString() &&
                                    it.exerciseId == exercise.id &&
                                    it.setNumber == index + 1
                            }
                            var reps by remember(existing) {
                                mutableStateOf(existing?.reps?.toString() ?: "")
                            }
                            var weight by remember(existing) {
                                mutableStateOf(
                                    existing?.weightKg?.toString() ?: suggestedWeight(exercise.weight)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text("Set ${index + 1}", modifier = Modifier.weight(.7f))
                                OutlinedTextField(
                                    value = reps,
                                    onValueChange = { reps = it },
                                    label = { Text("Reps") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = weight,
                                    onValueChange = { weight = it },
                                    label = { Text("kg") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    singleLine = true,
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.logSet(
                                            date,
                                            workout,
                                            exercise.id,
                                            index + 1,
                                            reps.toIntOrNull() ?: 0,
                                            weight.toDoubleOrNull() ?: 0.0,
                                        )
                                    }
                                ) {
                                    Icon(Icons.Outlined.Check, "Save set", tint = GainGreen)
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
private fun MeditationLogCard(
    minutes: Int?,
    referenceDate: LocalDate,
    completedDates: Set<String>,
    onMark: () -> Unit,
    onRemove: () -> Unit,
) {
    SectionCard(
        containerColor = if (minutes != null) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .36f)
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (minutes != null) {
                    GainGreen.copy(alpha = .16f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.SelfImprovement,
                        contentDescription = null,
                        tint = if (minutes != null) {
                            GainGreen
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp)) {
                Text(
                    if (minutes == null) "Daily meditation" else "Meditation complete",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    minutes?.let {
                        "$it minutes recorded for " +
                            referenceDate.format(DateTimeFormatter.ofPattern("d MMM")) + "."
                    } ?: "When you finish, record the actual time you meditated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            StatusPill(
                text = if (minutes == null) "Not logged" else "Completed",
                color = if (minutes == null) {
                    MaterialTheme.colorScheme.outline
                } else {
                    GainGreen
                },
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 14.dp))
        Text(
            "THIS WEEK",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val weekStart = referenceDate.minusDays(
            (referenceDate.dayOfWeek.value - 1).toLong()
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(7) { offset ->
                val day = weekStart.plusDays(offset.toLong())
                val done = day.toString() in completedDates
                val selected = day == referenceDate
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        day.format(DateTimeFormatter.ofPattern("EE")).take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        modifier = Modifier.padding(top = 5.dp).size(30.dp),
                        shape = CircleShape,
                        color = when {
                            done -> GainGreen
                            selected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f)
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                if (done) "✓" else day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    done -> Color.White
                                    selected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
        if (minutes == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                IconButton(
                    onClick = onMark,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "Log meditation",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            ) {
                IconButton(
                    onClick = onMark,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape,
                        ),
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit meditation minutes",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Remove meditation log",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun MeditationLogDialog(
    date: LocalDate,
    initialMinutes: Int?,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var text by remember(date, initialMinutes) {
        mutableStateOf(initialMinutes?.toString().orEmpty())
    }
    val minutes = text.toIntOrNull()
    val valid = minutes != null && minutes in 1..300
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialMinutes == null) "Log meditation" else "Edit meditation")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter the time you actually meditated on " +
                        date.format(DateTimeFormatter.ofPattern("d MMM yyyy")) + "."
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(3) },
                    label = { Text("Minutes") },
                    suffix = { Text("min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = text.isNotBlank() && !valid,
                    supportingText = { Text("Required • 1 to 300 minutes") },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { minutes?.let(onSave) },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun suggestedWeight(text: String): String =
    Regex("""\d+""").find(text)?.value ?: "0"

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
