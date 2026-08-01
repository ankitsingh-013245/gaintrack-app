@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gaintrack.personal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gaintrack.personal.MainViewModel
import com.gaintrack.personal.data.AppState
import com.gaintrack.personal.data.CsvTools
import com.gaintrack.personal.data.DaySnapshot
import com.gaintrack.personal.data.PurchaseStatus
import com.gaintrack.personal.data.foodFor
import com.gaintrack.personal.data.minutesFor
import com.gaintrack.personal.data.workoutFor
import com.gaintrack.personal.ui.AppTopBar
import com.gaintrack.personal.ui.GainAmber
import com.gaintrack.personal.ui.GainBlue
import com.gaintrack.personal.ui.GainGreen
import com.gaintrack.personal.ui.GainRose
import com.gaintrack.personal.ui.SectionCard
import com.gaintrack.personal.ui.SectionLabel
import com.gaintrack.personal.ui.StatusPill
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun LogbookScreen(state: AppState, viewModel: MainViewModel) {
    var monthOffset by remember {
        mutableIntStateOf(
            (YearMonth.from(state.selectedDate).year - YearMonth.now().year) * 12 +
                YearMonth.from(state.selectedDate).monthValue - YearMonth.now().monthValue
        )
    }
    val month = YearMonth.now().plusMonths(monthOffset.toLong())
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var pendingCsv by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                writer.write(pendingCsv)
            }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val csv = context.contentResolver.openInputStream(it)
                ?.bufferedReader()
                ?.use { reader -> reader.readText() }
                .orEmpty()
            val count = viewModel.repository.importDailyCsv(csv)
            importMessage = if (count > 0) {
                "Imported $count daily rows."
            } else {
                "No compatible rows found."
            }
        }
    }
    val dates = (1..month.lengthOfMonth()).map(month::atDay)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppTopBar(
                title = "Logbook",
                subtitle = "Tap a day to review what happened.",
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(Icons.Outlined.ArrowBackIosNew, "Previous month")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${dates.count {
                            state.snapshots.containsKey(it.toString()) ||
                                state.meditation.minutesFor(it) != null
                        }} days with data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { monthOffset++ }) {
                    Icon(Icons.Outlined.ArrowForwardIos, "Next month")
                }
            }
        }
        item {
            SectionCard {
                CalendarMonth(
                    month = month,
                    state = state,
                    onSelect = { date ->
                        viewModel.repository.selectDate(date)
                        selectedDay = date
                    },
                )
            }
        }
        item {
            CalendarLegend()
        }

        item {
            SectionLabel("Data tools")
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            pendingCsv = CsvTools.dailyLog(state)
                            exporter.launch("gaintrack-daily-log.csv")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Download, null)
                        Text(" Daily")
                    }
                    Button(
                        onClick = {
                            pendingCsv = CsvTools.exerciseLog(state)
                            exporter.launch("gaintrack-exercise-log.csv")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Exercise")
                    }
                }
                TextButton(
                    onClick = {
                        importer.launch(
                            arrayOf("text/csv", "text/comma-separated-values", "text/plain")
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Upload, null)
                    Text(" Import daily tracker CSV")
                }
                if (importMessage.isNotBlank()) {
                    Text(
                        importMessage,
                        color = GainGreen,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    selectedDay?.let { date ->
        DayDetailSheet(
            state = state,
            date = date,
            onDismiss = { selectedDay = null },
        )
    }
}

@Composable
private fun CalendarMonth(
    month: YearMonth,
    state: AppState,
    onSelect: (LocalDate) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val weekdays = DayOfWeek.entries
    Row(Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
            Text(
                day.getDisplayName(TextStyle.NARROW, locale),
                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = buildList<LocalDate?> {
        repeat(leading) { add(null) }
        (1..month.lengthOfMonth()).forEach { add(month.atDay(it)) }
        while (size % 7 != 0) add(null)
    }
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                if (date == null) {
                    Box(Modifier.weight(1f).aspectRatio(.92f))
                } else {
                    CalendarDayCell(
                        date = date,
                        state = state,
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    state: AppState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val future = date.isAfter(today)
    val snapshot = state.snapshots[date.toString()]
    val meditationMinutes = state.meditation.minutesFor(date)
    val hasData = snapshot != null || meditationMinutes != null
    val adherence = dayAdherence(state, date)
    val container = when {
        date == today -> MaterialTheme.colorScheme.primaryContainer
        future -> MaterialTheme.colorScheme.surface
        !hasData -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .32f)
        adherence >= .85f -> GainGreen.copy(alpha = .14f)
        adherence >= .45f -> GainAmber.copy(alpha = .14f)
        else -> GainRose.copy(alpha = .12f)
    }
    val status = when {
        future || !hasData -> MaterialTheme.colorScheme.outlineVariant
        adherence >= .85f -> GainGreen
        adherence >= .45f -> GainAmber
        else -> GainRose
    }
    Surface(
        modifier = modifier.aspectRatio(.92f).padding(2.dp).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = container,
    ) {
        Column(
            Modifier.fillMaxSize().padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (future) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(4) { index ->
                    val filled = when (index) {
                        0 -> snapshot?.completedFood?.isNotEmpty() == true
                        1 -> snapshot?.workoutDone == true ||
                            (snapshot != null && state.workoutFor(date) == null)
                        2 -> snapshot?.weightKg != null || snapshot?.sleepHours != null
                        else -> meditationMinutes != null
                    }
                    Box(
                        Modifier
                            .padding(bottom = 2.dp)
                            .width(5.dp)
                            .aspectRatio(1f)
                            .background(
                                when {
                                    !filled -> MaterialTheme.colorScheme.outlineVariant
                                    index == 3 -> GainBlue
                                    else -> status
                                },
                                shape = androidx.compose.foundation.shape.CircleShape,
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LegendDot("On track", GainGreen)
        LegendDot("Partial", GainAmber)
        LegendDot("Missed", GainRose)
        LegendDot("Meditation", GainBlue)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(8.dp)
                .aspectRatio(1f)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Text(
            label,
            modifier = Modifier.padding(start = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayDetailSheet(state: AppState, date: LocalDate, onDismiss: () -> Unit) {
    val snapshot = state.snapshots[date.toString()] ?: DaySnapshot(date)
    val foods = state.foodFor(date)
    val completed = foods.filter { it.id in snapshot.completedFood }
    val extraFoods = state.extraFoodLogs.filter { it.date == date }
    val consumedCalories =
        completed.sumOf { it.calories } + extraFoods.sumOf { it.calories }
    val consumedProtein =
        completed.sumOf { it.protein } + extraFoods.sumOf { it.protein }
    val plannedCalories = foods.sumOf { it.calories }
    val plannedProtein = foods.sumOf { it.protein }
    val meditationMinutes = state.meditation.minutesFor(date)
    val workout = state.workoutFor(date)
    val purchases = state.purchaseLogs.filter {
        it.purchaseDate == date && it.status == PurchaseStatus.PURCHASED
    }
    val adherence = dayAdherence(state, date)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EEEE")),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            date.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(
                        when {
                            date.isAfter(LocalDate.now()) -> "Future"
                            adherence >= .85f -> "On track"
                            adherence >= .45f -> "Partial"
                            else -> "Needs attention"
                        },
                        when {
                            date.isAfter(LocalDate.now()) -> MaterialTheme.colorScheme.outline
                            adherence >= .85f -> GainGreen
                            adherence >= .45f -> GainAmber
                            else -> GainRose
                        },
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DayMetric(
                        icon = Icons.Outlined.Restaurant,
                        value = "${completed.size}/${foods.size}",
                        label = "meals",
                        modifier = Modifier.weight(1f),
                    )
                    DayMetric(
                        icon = Icons.Outlined.Scale,
                        value = snapshot.weightKg?.let { "%.1f kg".format(it) } ?: "—",
                        label = "weight",
                        modifier = Modifier.weight(1f),
                    )
                    DayMetric(
                        icon = Icons.Outlined.Check,
                        value = snapshot.sleepHours?.let { "%.1f h".format(it) } ?: "—",
                        label = "sleep",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DayMetric(
                        icon = Icons.Outlined.Restaurant,
                        value = "$consumedCalories kcal",
                        label = "of $plannedCalories",
                        modifier = Modifier.weight(1f),
                    )
                    DayMetric(
                        icon = Icons.Outlined.FitnessCenter,
                        value = "$consumedProtein g",
                        label = "of $plannedProtein g",
                        modifier = Modifier.weight(1f),
                    )
                    DayMetric(
                        icon = Icons.Outlined.Check,
                        value = meditationMinutes?.let { "$it min" } ?: "—",
                        label = "meditation",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                SectionCard {
                    Text("Food", style = MaterialTheme.typography.titleMedium)
                    foods.forEach { food ->
                        Row(
                            Modifier.fillMaxWidth().padding(top = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                null,
                                tint = if (food.id in snapshot.completedFood) {
                                    GainGreen
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            )
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(food.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${food.time} • ${food.calories} kcal • " +
                                        "${food.protein} g protein",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    extraFoods.forEach { food ->
                        Row(
                            Modifier.fillMaxWidth().padding(top = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Restaurant,
                                null,
                                tint = GainGreen,
                            )
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(food.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Extra • ${food.calories} kcal • ${food.protein} g protein",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FitnessCenter, null, tint = GainGreen)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(workout?.name ?: "Recovery day", style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    workout == null -> "No workout planned"
                                    snapshot.workoutDone -> "Completed"
                                    snapshot.workoutSkipped -> "Skipped"
                                    else -> "Not completed"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                SectionCard(
                    containerColor = if (meditationMinutes != null) {
                        GainBlue.copy(alpha = .10f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = if (meditationMinutes != null) {
                                GainBlue
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Meditation", style = MaterialTheme.typography.titleMedium)
                            Text(
                                meditationMinutes?.let { "$it minutes completed" }
                                    ?: "Not logged",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (purchases.isNotEmpty()) {
                item {
                    SectionCard {
                        Text("Spending", style = MaterialTheme.typography.titleMedium)
                        purchases.forEach { purchase ->
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(purchase.itemName)
                                Text("₹${purchase.amount.toInt()}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (snapshot.notes.isNotBlank()) {
                item {
                    SectionCard {
                        Text("Notes", style = MaterialTheme.typography.titleMedium)
                        Text(
                            snapshot.notes,
                            modifier = Modifier.padding(top = 7.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun dayAdherence(state: AppState, date: LocalDate): Float {
    if (date.isAfter(LocalDate.now())) return 0f
    val snapshot = state.snapshots[date.toString()] ?: return 0f
    val foods = state.foodFor(date)
    val foodScore = if (foods.isEmpty()) {
        1f
    } else {
        snapshot.completedFood.count { id -> foods.any { it.id == id } }.toFloat() / foods.size
    }
    val workoutScore = when {
        state.workoutFor(date) == null -> 1f
        snapshot.workoutDone -> 1f
        else -> 0f
    }
    val sleepScore = if ((snapshot.sleepHours ?: 0.0) >= 7.0) 1f else 0f
    val weightScore = if (snapshot.weightKg != null) 1f else 0f
    return foodScore * .4f + workoutScore * .3f + sleepScore * .15f + weightScore * .15f
}
