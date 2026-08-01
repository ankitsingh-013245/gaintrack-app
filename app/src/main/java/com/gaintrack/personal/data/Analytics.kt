package com.gaintrack.personal.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class AnalyticsRange(val label: String, val days: Long) {
    WEEK("1 week", 7),
    MONTH("1 month", 31),
    THREE_MONTHS("3 months", 92),
    SIX_MONTHS("6 months", 184),
}

data class AnalyticsSummary(
    val foodAdherence: Int,
    val workoutAdherence: Int,
    val averageSleep: Double,
    val currentWeight: Double?,
    val weightChange: Double,
    val totalVolume: Double,
    val monthSpend: Double,
)

data class HealthAnalytics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val foodAdherence: Int,
    val calorieConsistency: Int,
    val proteinConsistency: Int,
    val workoutAdherence: Int,
    val sleepAdherence: Int,
    val averageSleep: Double,
    val weightLogAdherence: Int,
    val weeklyHealthScore: Int,
    val currentWeight: Double?,
    val currentTarget: Double,
    val totalWeightChange: Double,
    val weeklyGain: Double?,
    val projectedGoalDate: LocalDate?,
    val totalVolume: Double,
    val volumeChangePercent: Int?,
    val monthSpend: Double,
    val monthPlanned: Double,
    val remainingBudget: Double,
    val spendingByCategory: Map<String, Double>,
    val spendingByItem: Map<String, Double>,
)

object Analytics {
    fun summarize(state: AppState, today: LocalDate = LocalDate.now()): AnalyticsSummary {
        val detail = analyze(state, AnalyticsRange.MONTH, today)
        return AnalyticsSummary(
            foodAdherence = detail.foodAdherence,
            workoutAdherence = detail.workoutAdherence,
            averageSleep = detail.averageSleep,
            currentWeight = detail.currentWeight,
            weightChange = detail.totalWeightChange,
            totalVolume = detail.totalVolume,
            monthSpend = detail.monthSpend,
        )
    }

    fun analyze(
        state: AppState,
        range: AnalyticsRange = AnalyticsRange.MONTH,
        today: LocalDate = LocalDate.now(),
    ): HealthAnalytics {
        val start = today.minusDays(range.days - 1)
        val days = datesBetween(start, today)
        val relevant = days.map { date -> date to state.snapshots[date.toString()] }

        val plannedFoods = relevant.sumOf { (date, _) -> state.foodFor(date).size }
        val completedFoods = relevant.sumOf { (date, snapshot) ->
            val validIds = state.foodFor(date).mapTo(mutableSetOf()) { it.id }
            snapshot?.completedFood?.count { it in validIds } ?: 0
        }
        val completedCalories = relevant.sumOf { (date, snapshot) ->
            state.foodFor(date)
                .filter { it.id in snapshot?.completedFood.orEmpty() }
                .sumOf { it.calories } +
                state.extraFoodLogs.filter { it.date == date }.sumOf { it.calories }
        }
        val plannedCalories = relevant.sumOf { (date, _) -> state.foodFor(date).sumOf { it.calories } }
        val completedProtein = relevant.sumOf { (date, snapshot) ->
            state.foodFor(date)
                .filter { it.id in snapshot?.completedFood.orEmpty() }
                .sumOf { it.protein } +
                state.extraFoodLogs.filter { it.date == date }.sumOf { it.protein }
        }
        val plannedProtein = relevant.sumOf { (date, _) -> state.foodFor(date).sumOf { it.protein } }

        val workoutDates = days.filter { state.workoutFor(it) != null }
        val workoutsDone = workoutDates.count { state.snapshots[it.toString()]?.workoutDone == true }
        val sleepValues = relevant.mapNotNull { it.second?.sleepHours }
        val sleepMet = sleepValues.count { it >= 7.0 }
        val foodAdherence = percentage(completedFoods, plannedFoods)
        val workoutAdherence = percentage(workoutsDone, workoutDates.size)
        val sleepAdherence = percentage(sleepMet, days.size)

        val lastWeek = datesBetween(today.minusDays(6), today)
        val lastWeekFoodTotal = lastWeek.sumOf { state.foodFor(it).size }
        val lastWeekFoodDone = lastWeek.sumOf { date ->
            val ids = state.foodFor(date).mapTo(mutableSetOf()) { it.id }
            state.snapshots[date.toString()]?.completedFood?.count { it in ids } ?: 0
        }
        val lastWeekWorkouts = lastWeek.filter { state.workoutFor(it) != null }
        val lastWeekWorkoutDone =
            lastWeekWorkouts.count { state.snapshots[it.toString()]?.workoutDone == true }
        val lastWeekSleepMet =
            lastWeek.count { (state.snapshots[it.toString()]?.sleepHours ?: 0.0) >= 7.0 }
        val requiredWeightLogs = 1
        val actualWeightLogs = lastWeek.count { state.snapshots[it.toString()]?.weightKg != null }
        val weightLogAdherence = percentage(
            actualWeightLogs.coerceAtMost(requiredWeightLogs),
            requiredWeightLogs,
        )
        val weeklyHealthScore = (
            percentage(lastWeekFoodDone, lastWeekFoodTotal) * .40 +
                percentage(lastWeekWorkoutDone, lastWeekWorkouts.size) * .30 +
                percentage(lastWeekSleepMet, lastWeek.size) * .15 +
                weightLogAdherence * .15
            ).roundToInt()

        val allWeights = weeklyWeights(state.snapshots.values, today).map {
            DaySnapshot(date = it.loggedOn, weightKg = it.weightKg)
        }
        val rangeWeights = allWeights.filter { !it.date.isBefore(start) }
        val currentWeight = allWeights.lastOrNull()?.weightKg
        val firstWeight = allWeights.firstOrNull()?.weightKg
        val weeklyGain = gainPerWeek(rangeWeights)
        val goalWeight = state.activePlanFor(today)?.targetWeightKg ?: state.profile.targetWeightKg
        val projectedDate = if (
            currentWeight != null &&
            weeklyGain != null &&
            weeklyGain > 0.01 &&
            currentWeight < goalWeight
        ) {
            today.plusDays(
                (((goalWeight - currentWeight) / weeklyGain) * 7)
                    .roundToInt()
                    .coerceAtMost(3650)
                    .toLong()
            )
        } else {
            null
        }

        val currentLogs = state.exerciseLogs.filter {
            runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { date ->
                !date.isBefore(start) && !date.isAfter(today)
            } == true
        }
        val previousStart = start.minusDays(range.days)
        val previousLogs = state.exerciseLogs.filter {
            runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { date ->
                !date.isBefore(previousStart) && date.isBefore(start)
            } == true
        }
        val currentVolume = currentLogs.sumOf { it.weightKg * it.reps }
        val previousVolume = previousLogs.sumOf { it.weightKg * it.reps }
        val volumeChange = if (previousVolume > 0) {
            (((currentVolume - previousVolume) / previousVolume) * 100).roundToInt()
        } else {
            null
        }

        val budgetCycle = budgetCycleFor(today, state.budgetCycleStartDay)
        val purchasedThisMonth = state.purchaseLogs.filter {
            it.status == PurchaseStatus.PURCHASED &&
                !it.purchaseDate.isBefore(budgetCycle.start) &&
                !it.purchaseDate.isAfter(budgetCycle.endInclusive)
        }
        val monthSpend = purchasedThisMonth.sumOf { it.amount }
        val monthDays = datesBetween(budgetCycle.start, budgetCycle.endInclusive)
        val planned = state.shoppingTemplates.sumOf { template ->
            monthDays.count { template.isScheduledOn(it) } * template.estimatedPrice
        }

        return HealthAnalytics(
            startDate = start,
            endDate = today,
            foodAdherence = foodAdherence,
            calorieConsistency = percentage(completedCalories, plannedCalories),
            proteinConsistency = percentage(completedProtein, plannedProtein),
            workoutAdherence = workoutAdherence,
            sleepAdherence = sleepAdherence,
            averageSleep = sleepValues.takeIf { it.isNotEmpty() }?.average() ?: 0.0,
            weightLogAdherence = weightLogAdherence,
            weeklyHealthScore = weeklyHealthScore,
            currentWeight = currentWeight,
            currentTarget = state.targetWeightFor(today),
            totalWeightChange = if (currentWeight != null && firstWeight != null) {
                currentWeight - firstWeight
            } else {
                0.0
            },
            weeklyGain = weeklyGain,
            projectedGoalDate = projectedDate,
            totalVolume = currentVolume,
            volumeChangePercent = volumeChange,
            monthSpend = monthSpend,
            monthPlanned = planned,
            remainingBudget = (state.monthlyBudget - monthSpend).coerceAtLeast(0.0),
            spendingByCategory = purchasedThisMonth
                .groupBy { it.category }
                .mapValues { (_, logs) -> logs.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .toMap(),
            spendingByItem = purchasedThisMonth
                .groupBy { it.itemName }
                .mapValues { (_, logs) -> logs.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .toMap(),
        )
    }

    private fun percentage(value: Int, total: Int): Int =
        if (total <= 0) 0 else (value * 100.0 / total).roundToInt().coerceIn(0, 100)

    private fun gainPerWeek(weights: List<DaySnapshot>): Double? {
        if (weights.size < 2) return null
        val first = weights.first()
        val last = weights.last()
        val days = ChronoUnit.DAYS.between(first.date, last.date)
        if (days < 3) return null
        return ((last.weightKg ?: return null) - (first.weightKg ?: return null)) / days * 7
    }

    private fun datesBetween(start: LocalDate, endInclusive: LocalDate): List<LocalDate> =
        generateSequence(start) { date ->
            if (date < endInclusive) date.plusDays(1) else null
        }.toList()
}
