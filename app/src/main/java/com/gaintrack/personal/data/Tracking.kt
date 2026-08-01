package com.gaintrack.personal.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class OverdueMealSummary(
    val meals: List<FoodTask>,
    val calories: Int,
    val protein: Int,
)

enum class WeeklyWeighInStatus {
    UPCOMING,
    DUE,
    COMPLETE,
}

data class WeeklyWeight(
    val weekStart: LocalDate,
    val loggedOn: LocalDate,
    val weightKg: Double,
)

fun parseClock(value: String): LocalTime? =
    if (value.trim() == "24:00") {
        LocalTime.MIDNIGHT
    } else {
        runCatching { LocalTime.parse(value.trim()) }.getOrNull()
    }

fun normalizedClock(value: String): String? =
    parseClock(value)?.let {
        String.format(Locale.ROOT, "%02d:%02d", it.hour, it.minute)
    }

fun scheduledMealTime(profile: UserProfile, task: FoodTask): LocalTime? {
    parseClock(task.time)?.let { return it }
    val workout = parseClock(profile.workoutTime) ?: LocalTime.of(20, 30)
    return when (task.time.trim().lowercase()) {
        "morning" -> (parseClock(profile.wakeTime) ?: LocalTime.of(7, 0)).plusMinutes(30)
        "lunch" -> LocalTime.of(13, 30)
        "pre-workout" -> workout.minusHours(1)
        "after workout" -> workout.plusHours(1)
        "after dinner" -> workout.plusHours(2)
        "evening" -> LocalTime.of(19, 0)
        else -> null
    }
}

fun mealReminderKey(date: LocalDate, mealId: String): String = "$date|$mealId"

internal fun migrateShoppingTemplates(
    templates: List<ShoppingTemplate>,
): List<ShoppingItem> = templates.filter { it.active }.map { template ->
    ShoppingItem(
        id = template.id,
        name = template.name,
        quantity = buildString {
            append(
                if (template.defaultQuantity % 1.0 == 0.0) {
                    template.defaultQuantity.toInt()
                } else {
                    template.defaultQuantity
                }
            )
            if (template.unit.isNotBlank()) append(" ${template.unit}")
        },
        estimatedPrice = template.estimatedPrice.takeIf { it > 0.0 },
    )
}

fun overdueMeals(
    state: AppState,
    now: LocalDateTime = LocalDateTime.now(),
    graceMinutes: Long = 30,
): OverdueMealSummary {
    val date = now.toLocalDate()
    val completed = state.snapshots[date.toString()]?.completedFood.orEmpty()
    val meals = state.foodFor(date)
        .filter { task ->
            task.id !in completed &&
                scheduledMealTime(state.profile, task)?.let { time ->
                    !date.atTime(time).plusMinutes(graceMinutes).isAfter(now)
                } == true
        }
        .sortedBy { scheduledMealTime(state.profile, it) }
    return OverdueMealSummary(
        meals = meals,
        calories = meals.sumOf { it.calories },
        protein = meals.sumOf { it.protein },
    )
}

fun AppState.nextFutureMeal(
    date: LocalDate,
    now: LocalTime = LocalTime.now(),
): FoodTask? {
    val completed = snapshots[date.toString()]?.completedFood.orEmpty()
    return foodFor(date)
        .asSequence()
        .filter { it.id !in completed }
        .mapNotNull { task -> scheduledMealTime(profile, task)?.let { it to task } }
        .filter { (time, _) -> !time.isBefore(now) }
        .minByOrNull { it.first }
        ?.second
}

fun weekStart(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun weeklyWeights(
    snapshots: Collection<DaySnapshot>,
    through: LocalDate = LocalDate.now(),
): List<WeeklyWeight> = snapshots
    .asSequence()
    .filter { it.weightKg != null && !it.date.isAfter(through) }
    .groupBy { weekStart(it.date) }
    .map { (start, entries) ->
        val latest = entries.maxBy { it.date }
        WeeklyWeight(start, latest.date, latest.weightKg!!)
    }
    .sortedBy { it.weekStart }

fun weeklyWeighInStatus(
    profile: UserProfile,
    snapshots: Collection<DaySnapshot>,
    now: LocalDateTime = LocalDateTime.now(),
): WeeklyWeighInStatus {
    val start = weekStart(now.toLocalDate())
    val end = start.plusDays(6)
    val logged = snapshots.any {
        it.weightKg != null && !it.date.isBefore(start) && !it.date.isAfter(end)
    }
    if (logged) return WeeklyWeighInStatus.COMPLETE
    val dueDate = start.plusDays(profile.weighInDay.value.toLong() - 1)
    val dueTime = parseClock(profile.weighInTime) ?: LocalTime.of(8, 0)
    return if (now.isBefore(dueDate.atTime(dueTime))) {
        WeeklyWeighInStatus.UPCOMING
    } else {
        WeeklyWeighInStatus.DUE
    }
}
