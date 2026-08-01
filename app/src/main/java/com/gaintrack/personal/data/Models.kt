package com.gaintrack.personal.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

data class FoodTask(
    val id: String,
    val time: String,
    val title: String,
    val detail: String,
    val calories: Int,
    val protein: Int,
)

enum class MealPlanGroup {
    WEEKDAY,
    SUNDAY,
}

fun mealPlanGroupFor(date: LocalDate): MealPlanGroup =
    if (date.dayOfWeek == DayOfWeek.SUNDAY) {
        MealPlanGroup.SUNDAY
    } else {
        MealPlanGroup.WEEKDAY
    }

data class ExtraFoodLog(
    val id: Long,
    val date: LocalDate,
    val name: String,
    val calories: Int,
    val protein: Int,
)

data class Exercise(
    val id: String,
    val name: String,
    val weight: String,
    val sets: Int,
    val reps: String,
    val technique: String,
    val commonMistake: String = "Avoid rushing the movement or losing a neutral spine.",
    val muscles: String = "",
    val equipment: String = "Bodyweight",
    val learnUrl: String = "",
)

data class WorkoutPlan(
    val name: String,
    val exercises: List<Exercise>,
)

data class ExerciseLog(
    val date: String,
    val workout: String,
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
)

/**
 * Kept so older GainTrack installations can be migrated without losing purchases.
 * New shopping writes use [PurchaseLog].
 */
data class Expense(
    val id: Long,
    val date: String,
    val item: String,
    val quantity: String,
    val amount: Double,
)

enum class ShoppingFrequency {
    ONE_TIME,
    DAILY,
    SELECTED_WEEKDAYS,
    WEEKLY,
    MONTHLY,
}

data class RecurrenceRule(
    val frequency: ShoppingFrequency,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val dayOfMonth: Int? = null,
)

data class ShoppingTemplate(
    val id: String,
    val name: String,
    val category: String,
    val defaultQuantity: Double,
    val unit: String,
    val estimatedPrice: Double,
    val recurrence: RecurrenceRule,
    val startDate: LocalDate,
    val active: Boolean = true,
)

data class ShoppingItem(
    val id: String,
    val name: String,
    val quantity: String = "",
    val estimatedPrice: Double? = null,
    val bought: Boolean = false,
)

enum class PurchaseStatus {
    PURCHASED,
    SKIPPED,
}

data class PurchaseLog(
    val id: Long,
    val templateId: String?,
    val itemName: String,
    val category: String,
    val actualQuantity: Double,
    val unit: String,
    val amount: Double,
    val purchaseDate: LocalDate,
    val status: PurchaseStatus = PurchaseStatus.PURCHASED,
)

data class BudgetCycle(
    val start: LocalDate,
    val endInclusive: LocalDate,
)

data class Motivation(
    val quote: String,
    val author: String,
    val imageUrl: String = "",
    val imageAuthor: String = "",
)

data class DaySnapshot(
    val date: LocalDate,
    val completedFood: Set<String> = emptySet(),
    val workoutDone: Boolean = false,
    val workoutSkipped: Boolean = false,
    val sleepHours: Double? = null,
    val weightKg: Double? = null,
    val notes: String = "",
)

enum class Gender {
    FEMALE,
    MALE,
    NON_BINARY,
    PREFER_NOT_TO_SAY,
}

enum class PrimaryGoal {
    /**
     * GainTrack is a weight-gain-only product. The other values remain readable
     * solely so older SharedPreferences payloads can be migrated safely.
     */
    GAIN,
    MAINTAIN,
    LOSE,
}

enum class DietPreference {
    EVERYTHING,
    VEGETARIAN,
    VEGAN,
}

data class UserProfile(
    val name: String = "",
    val age: Int = 24,
    val gender: Gender = Gender.PREFER_NOT_TO_SAY,
    val heightCm: Double = 165.0,
    val currentWeightKg: Double = SeedPlan.startWeight,
    val targetWeightKg: Double = SeedPlan.targetWeight,
    val goal: PrimaryGoal = PrimaryGoal.GAIN,
    val goalDurationMonths: Int = 6,
    val weighInDay: DayOfWeek = DayOfWeek.SUNDAY,
    val weighInTime: String = "08:00",
    val diet: DietPreference = DietPreference.EVERYTHING,
    val mealsPerDay: Int = 5,
    val wakeTime: String = "07:00",
    val workoutTime: String = "20:30",
    val sleepTime: String = "23:30",
    val trainingDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SUNDAY,
    ),
    val equipment: String = "Dumbbells and bodyweight",
    val injuries: String = "",
    val loadedTrainingCleared: Boolean = true,
    val monthlyFoodBudget: Double = 2000.0,
    val meditationMinutes: Int = 5,
    val onboardingStep: Int = 0,
    val onboardingComplete: Boolean = false,
)

data class PlanVersion(
    val id: String,
    val effectiveFrom: LocalDate,
    val targetDate: LocalDate,
    val startWeightKg: Double,
    val targetWeightKg: Double,
    val calorieTarget: Int,
    val proteinTarget: Int,
    val trainingDays: Set<DayOfWeek>,
    val diet: DietPreference,
    val meditationMinutes: Int,
)

data class MeditationState(
    val completedDates: Set<String> = emptySet(),
    val minutesByDate: Map<String, Int> = emptyMap(),
)

fun MeditationState.minutesFor(date: LocalDate): Int? =
    minutesByDate[date.toString()]

data class AppState(
    val selectedDate: LocalDate = LocalDate.now(),
    val snapshots: Map<String, DaySnapshot> = emptyMap(),
    val extraFoodLogs: List<ExtraFoodLog> = emptyList(),
    val exerciseLogs: List<ExerciseLog> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val shoppingTemplates: List<ShoppingTemplate> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val purchaseLogs: List<PurchaseLog> = emptyList(),
    val motivation: Motivation = SeedPlan.fallbackMotivations.first(),
    val unlockReminderEnabled: Boolean = false,
    val dailyNotificationEnabled: Boolean = true,
    val notificationHour: Int = 6,
    val notificationMinute: Int = 30,
    val monthlyBudget: Double = 2000.0,
    val budgetCycleStartDay: Int = 1,
    val foodOverrides: Map<String, FoodTask> = emptyMap(),
    val exerciseOverrides: Map<String, Exercise> = emptyMap(),
    val customMealPlans: Map<MealPlanGroup, List<FoodTask>> = emptyMap(),
    val customWorkoutPlans: Map<String, List<Exercise>> = emptyMap(),
    val profile: UserProfile = UserProfile(),
    val planVersions: List<PlanVersion> = emptyList(),
    val seenTours: Set<String> = emptySet(),
    val meditation: MeditationState = MeditationState(),
    val notifiedMealKeys: Set<String> = emptySet(),
)

fun budgetCycleFor(
    date: LocalDate = LocalDate.now(),
    startDay: Int,
): BudgetCycle {
    val safeDay = startDay.coerceIn(1, 31)
    fun dateIn(month: YearMonth): LocalDate =
        month.atDay(safeDay.coerceAtMost(month.lengthOfMonth()))

    val month = YearMonth.from(date)
    val currentMonthStart = dateIn(month)
    val start = if (date.isBefore(currentMonthStart)) {
        dateIn(month.minusMonths(1))
    } else {
        currentMonthStart
    }
    val nextStart = dateIn(YearMonth.from(start).plusMonths(1))
    return BudgetCycle(start = start, endInclusive = nextStart.minusDays(1))
}

fun AppState.activePlanFor(date: LocalDate = LocalDate.now()): PlanVersion? =
    planVersions
        .filter { !it.effectiveFrom.isAfter(date) }
        .maxByOrNull { it.effectiveFrom }

fun AppState.foodFor(date: LocalDate): List<FoodTask> {
    customMealPlans[mealPlanGroupFor(date)]?.let { return it }
    val base = SeedPlan.foodFor(date).map { foodOverrides[it.id] ?: it }
    val plan = activePlanFor(date) ?: return base
    val baseCalories = base.sumOf { it.calories }.coerceAtLeast(1)
    val baseProtein = base.sumOf { it.protein }.coerceAtLeast(1)
    val calorieScale = plan.calorieTarget.toDouble() / baseCalories
    val proteinScale = plan.proteinTarget.toDouble() / baseProtein
    return base.map { task ->
        val dietTask = when {
            plan.diet == DietPreference.VEGETARIAN &&
                task.title.contains("egg", ignoreCase = true) -> task.copy(
                    title = "Curd + soy bowl",
                    detail = "Curd, soy chunks and fruit; match the planned protein.",
                )
            plan.diet == DietPreference.VEGAN &&
                task.title.contains("egg", ignoreCase = true) -> task.copy(
                    title = "Tofu + soy bowl",
                    detail = "Tofu, soy chunks and fruit; match the planned protein.",
                )
            else -> task
        }
        dietTask.copy(
            calories = (dietTask.calories * calorieScale).roundToInt().coerceAtLeast(1),
            protein = (dietTask.protein * proteinScale).roundToInt().coerceAtLeast(1),
        )
    }
}

fun AppState.workoutFor(date: LocalDate): WorkoutPlan? =
    personalizedWorkoutFor(date)?.let { plan ->
        plan.copy(
            exercises = (customWorkoutPlans[plan.name] ?: plan.exercises).map {
                (exerciseOverrides[it.id] ?: it).withLearningGuidance()
            }
        )
    }

private fun AppState.personalizedWorkoutFor(date: LocalDate): WorkoutPlan? {
    val plan = activePlanFor(date) ?: return SeedPlan.workoutFor(date)
    if (date.dayOfWeek !in plan.trainingDays) return null
    val orderedDays = plan.trainingDays.sortedBy { it.value }
    val trainingIndex = orderedDays.indexOf(date.dayOfWeek).coerceAtLeast(0)
    return SeedPlan.workoutForTrainingIndex(trainingIndex)
}

fun AppState.calorieTargetFor(date: LocalDate = LocalDate.now()): Int =
    activePlanFor(date)?.calorieTarget ?: foodFor(date).sumOf { it.calories }

fun AppState.proteinTargetFor(date: LocalDate = LocalDate.now()): Int =
    activePlanFor(date)?.proteinTarget ?: foodFor(date).sumOf { it.protein }

fun AppState.targetWeightFor(date: LocalDate = LocalDate.now()): Double {
    val plan = activePlanFor(date) ?: return SeedPlan.targetFor(date)
    val totalDays = ChronoUnit.DAYS.between(plan.effectiveFrom, plan.targetDate).coerceAtLeast(1)
    val elapsed = ChronoUnit.DAYS.between(plan.effectiveFrom, date)
    val progress = (elapsed.toDouble() / totalDays).coerceIn(0.0, 1.0)
    return plan.startWeightKg + (plan.targetWeightKg - plan.startWeightKg) * progress
}

fun UserProfile.toPlanVersion(
    effectiveFrom: LocalDate = LocalDate.now(),
    id: String = "plan-${System.currentTimeMillis()}",
): PlanVersion {
    val genderOffset = when (gender) {
        Gender.MALE -> 5.0
        Gender.FEMALE -> -161.0
        else -> -78.0
    }
    val bmr = 10 * currentWeightKg + 6.25 * heightCm - 5 * age + genderOffset
    val goalAdjustment = 300
    val calories = (bmr * 1.45 + goalAdjustment).roundToInt().coerceIn(1400, 4200)
    val protein = (currentWeightKg * 1.8)
        .roundToInt()
        .coerceIn(55, 240)
    val months = goalDurationMonths.takeIf { it in setOf(1, 2, 3, 6, 9, 12) } ?: 6
    return PlanVersion(
        id = id,
        effectiveFrom = effectiveFrom,
        targetDate = effectiveFrom.plusMonths(months.toLong()),
        startWeightKg = currentWeightKg,
        targetWeightKg = targetWeightKg,
        calorieTarget = calories,
        proteinTarget = protein,
        trainingDays = trainingDays.ifEmpty { setOf(DayOfWeek.MONDAY) },
        diet = diet,
        meditationMinutes = meditationMinutes.coerceIn(5, 10),
    )
}

private fun Exercise.withLearningGuidance(): Exercise {
    val lowerBody = id in setOf(
        "goblet_squat",
        "rdl",
        "split_squat",
        "calf_a",
        "reverse_lunge",
        "single_rdl",
        "bridge",
        "slow_squat",
        "calf_b",
    )
    val core = id in setOf("plank", "dead_bug")
    val back = id in setOf("one_arm_row", "bent_row", "rear_delt")
    val chest = id in setOf("floor_press", "pushup", "close_pushup")
    val resolvedMuscles = when {
        lowerBody -> "Glutes • quads • hamstrings"
        core -> "Core • trunk stability"
        back -> "Upper back • lats • rear shoulders"
        chest -> "Chest • shoulders • triceps"
        id.contains("press") || id.contains("raise") -> "Shoulders • upper body"
        id.contains("curl") || id == "hammer" -> "Biceps • forearms"
        id == "triceps" -> "Triceps"
        else -> "Full body"
    }
    val resolvedMistake = when {
        lowerBody -> "Do not let the knees collapse inward or trade depth for a rounded back."
        core -> "Do not hold your breath or let the lower back lose position."
        back -> "Avoid shrugging and twisting; move the shoulder blade with control."
        chest -> "Avoid flared elbows and losing a straight, braced body position."
        id.contains("press") -> "Do not overarch the lower back to finish the rep."
        id.contains("raise") -> "Do not swing the weight or lift above a pain-free range."
        else -> commonMistake
    }
    val resolvedEquipment = when {
        weight.contains("Bodyweight", ignoreCase = true) -> "Bodyweight"
        weight.contains("Bottle", ignoreCase = true) -> "Light dumbbell or bottles"
        else -> "Dumbbell"
    }
    val query = URLEncoder.encode("$name proper form tutorial", StandardCharsets.UTF_8.toString())
    return copy(
        commonMistake = if (
            commonMistake == "Avoid rushing the movement or losing a neutral spine."
        ) {
            resolvedMistake
        } else {
            commonMistake
        },
        muscles = muscles.ifBlank { resolvedMuscles },
        equipment = if (equipment == "Bodyweight") resolvedEquipment else equipment,
        learnUrl = learnUrl.ifBlank {
            "https://www.youtube.com/results?search_query=$query"
        },
    )
}

fun ShoppingTemplate.isScheduledOn(date: LocalDate): Boolean {
    if (!active || date.isBefore(startDate)) return false
    return when (recurrence.frequency) {
        ShoppingFrequency.ONE_TIME -> date == startDate
        ShoppingFrequency.DAILY -> true
        ShoppingFrequency.SELECTED_WEEKDAYS -> date.dayOfWeek in recurrence.weekdays
        ShoppingFrequency.WEEKLY ->
            date.dayOfWeek == recurrence.weekdays.firstOrNull() ?: startDate.dayOfWeek
        ShoppingFrequency.MONTHLY -> {
            val scheduledDay = (recurrence.dayOfMonth ?: startDate.dayOfMonth)
                .coerceAtMost(date.lengthOfMonth())
            date.dayOfMonth == scheduledDay
        }
    }
}

fun AppState.dueShopping(date: LocalDate = LocalDate.now()): List<ShoppingTemplate> =
    shoppingTemplates
        .filter { it.isScheduledOn(date) }
        .filter { template ->
            purchaseLogs.none { log ->
                log.templateId == template.id && log.purchaseDate == date
            }
        }
        .sortedWith(compareBy<ShoppingTemplate> { it.category }.thenBy { it.name })
