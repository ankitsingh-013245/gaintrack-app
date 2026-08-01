package com.gaintrack.personal.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gaintrack.personal.MainViewModel
import com.gaintrack.personal.data.AppState
import com.gaintrack.personal.data.DaySnapshot
import com.gaintrack.personal.data.WeeklyWeighInStatus
import com.gaintrack.personal.data.activePlanFor
import com.gaintrack.personal.data.foodFor
import com.gaintrack.personal.data.targetWeightFor
import com.gaintrack.personal.data.weekStart
import com.gaintrack.personal.data.weeklyWeighInStatus
import com.gaintrack.personal.data.weeklyWeights
import com.gaintrack.personal.data.workoutFor
import com.gaintrack.personal.ui.AppTopBar
import com.gaintrack.personal.ui.GainBlue
import com.gaintrack.personal.ui.GainGreen
import com.gaintrack.personal.ui.Metric
import com.gaintrack.personal.ui.SectionCard
import com.gaintrack.personal.ui.SectionLabel
import com.gaintrack.personal.ui.StatusPill
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ProgressScreen(state: AppState, viewModel: MainViewModel) {
    var loggingWeight by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val plan = state.activePlanFor(today)
    val startWeight = plan?.startWeightKg ?: state.profile.currentWeightKg
    val firstLoggedDate = state.snapshots.values
        .filter { it.weightKg != null && !it.date.isAfter(today) }
        .minByOrNull { it.date }
        ?.date
    val requestedGraphStart = plan?.effectiveFrom ?: firstLoggedDate ?: today
    val graphStartDate = if (requestedGraphStart.isAfter(today)) today else requestedGraphStart
    val dailyWeightPoints = state.snapshots.values
        .filter {
            it.weightKg != null &&
                !it.date.isBefore(graphStartDate) &&
                !it.date.isAfter(today)
        }
        .sortedBy { it.date }
    val graphWeightPoints = buildList {
        val dayOneLog = dailyWeightPoints.firstOrNull { it.date == graphStartDate }
        add(dayOneLog ?: DaySnapshot(date = graphStartDate, weightKg = startWeight))
        addAll(dailyWeightPoints.filterNot { it.date == graphStartDate })
    }
    val allWeeklyWeights = weeklyWeights(state.snapshots.values, today)
    val currentWeight = state.snapshots.values
        .filter { it.weightKg != null && !it.date.isAfter(today) }
        .maxByOrNull { it.date }
        ?.weightKg
    val todayWeight = state.snapshots[today.toString()]?.weightKg
    val currentWeekWeight = allWeeklyWeights.lastOrNull()
        ?.takeIf { it.weekStart == weekStart(today) }
    val goalWeight = maxOf(
        plan?.targetWeightKg ?: state.profile.targetWeightKg,
        startWeight + .1,
    )
    val gained = (currentWeight ?: startWeight) - startWeight
    val remaining = goalWeight - (currentWeight ?: startWeight)
    val goalProgress = (
        ((currentWeight ?: startWeight) - startWeight) / (goalWeight - startWeight)
        ).toFloat().coerceIn(0f, 1f)
    val checkInStatus = weeklyWeighInStatus(
        state.profile,
        state.snapshots.values,
        LocalDateTime.now(),
    )
    val thisWeek = (0L..6L).map { weekStart(today).plusDays(it) }
    val elapsedWeek = thisWeek.filterNot { it.isAfter(today) }
    val plannedMeals = elapsedWeek.sumOf { state.foodFor(it).size }
    val completedMeals = elapsedWeek.sumOf { date ->
        val validIds = state.foodFor(date).mapTo(mutableSetOf()) { it.id }
        state.snapshots[date.toString()]?.completedFood?.count { it in validIds } ?: 0
    }
    val plannedWorkouts = elapsedWeek.count { state.workoutFor(it) != null }
    val completedWorkouts = elapsedWeek.count {
        state.workoutFor(it) != null && state.snapshots[it.toString()]?.workoutDone == true
    }
    val sleepDays = elapsedWeek.count {
        (state.snapshots[it.toString()]?.sleepHours ?: 0.0) >= 7.0
    }
    val meditationDays = elapsedWeek.count {
        it.toString() in state.meditation.completedDates
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppTopBar(
                title = "Progress",
                subtitle = "Your goal and weekly habits, in one clear view.",
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
                        Text("Weight goal", style = MaterialTheme.typography.labelLarge)
                        Text(
                            currentWeight?.let { "%.1f kg".format(it) } ?: "No weight yet",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        "Goal %.1f kg".format(goalWeight),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                LinearProgressIndicator(
                    progress = { goalProgress },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    color = GainGreen,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = .72f),
                    strokeCap = StrokeCap.Round,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Metric(
                        label = "started",
                        value = "%.1f kg".format(startWeight),
                        modifier = Modifier.weight(1f),
                    )
                    Metric(
                        label = "gained",
                        value = "%+.1f kg".format(gained),
                        modifier = Modifier.weight(1f),
                        tint = GainGreen,
                    )
                    Metric(
                        label = if (remaining > 0) "left" else "status",
                        value = if (remaining > 0) {
                            "%.1f kg".format(remaining)
                        } else {
                            "Reached"
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Weight check-in", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (checkInStatus) {
                                WeeklyWeighInStatus.UPCOMING ->
                                    "Recommended ${state.profile.weighInDay.displayName()} at " +
                                        "${state.profile.weighInTime} • log anytime"
                                WeeklyWeighInStatus.DUE ->
                                    "Weekly check-in due — you can also log whenever you want"
                                WeeklyWeighInStatus.COMPLETE ->
                                    currentWeekWeight?.let {
                                        "%.1f kg saved on %s".format(
                                            it.weightKg,
                                            it.loggedOn.format(
                                                DateTimeFormatter.ofPattern("EEE, d MMM")
                                            ),
                                        )
                                    } ?: "Completed"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    StatusPill(
                        text = when (checkInStatus) {
                            WeeklyWeighInStatus.UPCOMING -> "Upcoming"
                            WeeklyWeighInStatus.DUE -> "Due"
                            WeeklyWeighInStatus.COMPLETE -> "Done"
                        },
                        color = when (checkInStatus) {
                            WeeklyWeighInStatus.COMPLETE -> GainGreen
                            WeeklyWeighInStatus.DUE -> MaterialTheme.colorScheme.error
                            WeeklyWeighInStatus.UPCOMING -> MaterialTheme.colorScheme.primary
                        },
                    )
                }
                Button(
                    onClick = { loggingWeight = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) {
                    Icon(Icons.Outlined.Scale, contentDescription = null)
                    Text(if (todayWeight == null) " Log today’s weight" else " Update today’s weight")
                }
            }
        }
        item {
            SectionLabel("Weight journey")
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Day 1 to today", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${graphStartDate.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} - " +
                                today.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(
                        "${ChronoUnit.DAYS.between(graphStartDate, today) + 1} days"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    ChartLegend("Actual weight", MaterialTheme.colorScheme.primary)
                    ChartLegend("Target path", GainBlue)
                }
                WeightHistoryChart(
                    values = graphWeightPoints,
                    startDate = graphStartDate,
                    endDate = today,
                    targetAt = state::targetWeightFor,
                )
                if (dailyWeightPoints.isEmpty()) {
                    Text(
                        "The Day 1 starting weight is shown. Log a weight to begin your actual trend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        item {
            SectionLabel("This week")
            SectionCard {
                HabitProgress(
                    label = "Meals",
                    value = completedMeals,
                    total = plannedMeals,
                    supporting = "$completedMeals of $plannedMeals check-ins",
                )
                HabitProgress(
                    label = "Workouts",
                    value = completedWorkouts,
                    total = plannedWorkouts,
                    supporting = "$completedWorkouts of $plannedWorkouts planned sessions",
                )
                HabitProgress(
                    label = "Sleep",
                    value = sleepDays,
                    total = elapsedWeek.size,
                    supporting = "$sleepDays days with 7+ hours",
                )
                HabitProgress(
                    label = "Meditation",
                    value = meditationDays,
                    total = elapsedWeek.size,
                    supporting = "$meditationDays of ${elapsedWeek.size} days completed",
                    last = true,
                )
                MeditationWeek(
                    dates = thisWeek,
                    completedDates = state.meditation.completedDates,
                    today = today,
                )
            }
        }
    }

    if (loggingWeight) {
        WeightEntryDialog(
            initial = todayWeight,
            onDismiss = { loggingWeight = false },
            onSave = { weight ->
                viewModel.repository.setWeight(today, weight)
                loggingWeight = false
            },
        )
    }
}

@Composable
private fun HabitProgress(
    label: String,
    value: Int,
    total: Int,
    supporting: String,
    last: Boolean = false,
) {
    val progress = if (total <= 0) 0f else (value.toFloat() / total).coerceIn(0f, 1f)
    Column(Modifier.padding(bottom = if (last) 8.dp else 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                if (total <= 0) "No plan" else "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = GainGreen,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            color = GainGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
        Text(
            supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun MeditationWeek(
    dates: List<LocalDate>,
    completedDates: Set<String>,
    today: LocalDate,
) {
    Text(
        "Meditation days",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        dates.forEach { date ->
            val done = date.toString() in completedDates
            val future = date.isAfter(today)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    date.format(DateTimeFormatter.ofPattern("EE")).take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    modifier = Modifier.padding(top = 5.dp).size(30.dp),
                    shape = CircleShape,
                    color = when {
                        done -> GainGreen
                        future -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (done) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = "Meditation completed",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp),
                            )
                        } else {
                            Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(9.dp),
            shape = CircleShape,
            color = color,
        ) {}
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun WeightHistoryChart(
    values: List<DaySnapshot>,
    startDate: LocalDate,
    endDate: LocalDate,
    targetAt: (LocalDate) -> Double,
) {
    val actualValues = values
        .mapNotNull { point -> point.weightKg?.let { point.date to it } }
        .sortedBy { it.first }
    val rawMin = minOf(
        targetAt(startDate),
        targetAt(endDate),
        actualValues.minOfOrNull { it.second } ?: targetAt(startDate),
    )
    val rawMax = maxOf(
        targetAt(startDate),
        targetAt(endDate),
        actualValues.maxOfOrNull { it.second } ?: targetAt(endDate),
    )
    val visibleSpan = (rawMax - rawMin).coerceAtLeast(1.0)
    val minWeight = rawMin - visibleSpan * .18
    val maxWeight = rawMax + visibleSpan * .18
    val totalDays = ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(0)
    val actualColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f)
    val pointHalo = MaterialTheme.colorScheme.surface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .18f),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf(
                        maxWeight,
                        (maxWeight + minWeight) / 2.0,
                        minWeight,
                    ).forEach { value ->
                        Text(
                            "%.1f".format(value),
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                        )
                    }
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 42.dp, top = 10.dp, end = 6.dp, bottom = 10.dp),
                ) {
                    fun x(date: LocalDate): Float {
                        if (totalDays == 0L) return size.width / 2f
                        val days = ChronoUnit.DAYS.between(startDate, date).toFloat()
                        return (days / totalDays.toFloat()).coerceIn(0f, 1f) * size.width
                    }

                    fun y(weight: Double): Float =
                        size.height -
                            ((weight - minWeight) / (maxWeight - minWeight) * size.height)
                                .toFloat()

                    repeat(5) { index ->
                        val lineY = size.height * index / 4f
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, lineY),
                            end = Offset(size.width, lineY),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    repeat(4) { index ->
                        val lineX = size.width * index / 3f
                        drawLine(
                            color = gridColor.copy(alpha = .55f),
                            start = Offset(lineX, 0f),
                            end = Offset(lineX, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    if (totalDays == 0L) {
                        drawCircle(
                            color = GainBlue,
                            radius = 4.dp.toPx(),
                            center = Offset(size.width / 2f, y(targetAt(startDate))),
                        )
                    } else {
                        val sampleCount = totalDays.coerceAtMost(40).toInt().coerceAtLeast(1)
                        val targetPath = Path()
                        for (index in 0..sampleCount) {
                            val dayOffset = totalDays * index / sampleCount
                            val date = startDate.plusDays(dayOffset)
                            val pointX = x(date)
                            val pointY = y(targetAt(date))
                            if (index == 0) {
                                targetPath.moveTo(pointX, pointY)
                            } else {
                                targetPath.lineTo(pointX, pointY)
                            }
                        }
                        drawPath(
                            path = targetPath,
                            color = GainBlue.copy(alpha = .88f),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(10.dp.toPx(), 7.dp.toPx())
                                ),
                            ),
                        )
                    }

                    val points = actualValues.map { (date, weight) ->
                        Offset(x(date), y(weight))
                    }
                    if (points.size > 1) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (index in 1 until points.size) {
                                val previous = points[index - 1]
                                val current = points[index]
                                val controlX = (previous.x + current.x) / 2f
                                cubicTo(
                                    controlX,
                                    previous.y,
                                    controlX,
                                    current.y,
                                    current.x,
                                    current.y,
                                )
                            }
                        }
                        val areaPath = Path().apply {
                            moveTo(points.first().x, size.height)
                            lineTo(points.first().x, points.first().y)
                            for (index in 1 until points.size) {
                                val previous = points[index - 1]
                                val current = points[index]
                                val controlX = (previous.x + current.x) / 2f
                                cubicTo(
                                    controlX,
                                    previous.y,
                                    controlX,
                                    current.y,
                                    current.x,
                                    current.y,
                                )
                            }
                            lineTo(points.last().x, size.height)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    actualColor.copy(alpha = .24f),
                                    actualColor.copy(alpha = .02f),
                                ),
                                startY = 0f,
                                endY = size.height,
                            ),
                        )
                        drawPath(
                            path = linePath,
                            color = actualColor.copy(alpha = .14f),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                        )
                        drawPath(
                            path = linePath,
                            color = actualColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    points.forEach { point ->
                        drawCircle(
                            color = pointHalo,
                            radius = 6.dp.toPx(),
                            center = point,
                        )
                        drawCircle(
                            color = actualColor,
                            radius = 4.dp.toPx(),
                            center = point,
                        )
                    }
                }
            }
            if (totalDays == 0L) {
                Text(
                    "Day 1 • Today  ${startDate.format(DateTimeFormatter.ofPattern("d MMM"))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            } else {
                val middleDate = startDate.plusDays(totalDays / 2)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 42.dp, end = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Day 1\n${startDate.format(DateTimeFormatter.ofPattern("d MMM"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                    )
                    if (totalDays >= 2) {
                        Text(
                            "Day ${totalDays / 2 + 1}\n" +
                                middleDate.format(DateTimeFormatter.ofPattern("d MMM")),
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    Text(
                        "Today\n${endDate.format(DateTimeFormatter.ofPattern("d MMM"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightEntryDialog(
    initial: Double?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial?.toString().orEmpty()) }
    val value = text.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(6) },
                label = { Text("Weight") },
                suffix = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = text.isNotBlank() && (value == null || value !in 20.0..350.0),
            )
        },
        confirmButton = {
            TextButton(
                enabled = value != null && value in 20.0..350.0,
                onClick = { value?.let(onSave) },
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

private fun java.time.DayOfWeek.displayName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }
