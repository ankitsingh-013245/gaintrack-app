package com.gaintrack.personal.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class GainRepository(context: Context) {
    private val prefs = context.getSharedPreferences("gaintrack_data", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        val meditationHasMinuteLogs = runCatching {
            JSONObject(prefs.getString("meditation", "{}") ?: "{}").has("minuteLogs")
        }.getOrDefault(false)
        // Persist newly seeded templates, one-time legacy migration, and v2 defaults.
        if (
            !prefs.contains("shopping_templates") ||
            !prefs.contains("purchase_logs") ||
            !prefs.contains("profile") ||
            !prefs.contains("shopping_items") ||
            !prefs.contains("notified_meals") ||
            !prefs.contains("budget_cycle_start_day") ||
            !prefs.contains("extra_food_logs") ||
            !prefs.contains("custom_meal_plans") ||
            !prefs.contains("custom_workout_plans") ||
            !meditationHasMinuteLogs
        ) {
            saveState(_state.value)
        }
    }

    fun selectDate(date: LocalDate) = update { it.copy(selectedDate = date) }

    fun toggleFood(date: LocalDate, taskId: String) = update { state ->
        val day = snapshot(state, date)
        val completed = day.completedFood.toMutableSet().apply {
            if (!add(taskId)) remove(taskId)
        }
        state.withSnapshot(day.copy(completedFood = completed))
    }

    fun addExtraFood(
        date: LocalDate,
        name: String,
        calories: Int,
        protein: Int,
    ) = update { state ->
        state.copy(
            extraFoodLogs = state.extraFoodLogs + ExtraFoodLog(
                id = System.currentTimeMillis(),
                date = date,
                name = name.trim().ifBlank { "Extra food" }.take(80),
                calories = calories.coerceIn(0, 5000),
                protein = protein.coerceIn(0, 500),
            )
        )
    }

    fun removeExtraFood(id: Long) = update { state ->
        state.copy(extraFoodLogs = state.extraFoodLogs.filterNot { it.id == id })
    }

    fun setWeight(date: LocalDate, value: Double?) = update { state ->
        state.withSnapshot(snapshot(state, date).copy(weightKg = value))
    }

    fun setSleep(date: LocalDate, value: Double?) = update { state ->
        state.withSnapshot(snapshot(state, date).copy(sleepHours = value))
    }

    fun setNotes(date: LocalDate, notes: String) = update { state ->
        state.withSnapshot(snapshot(state, date).copy(notes = notes))
    }

    fun setWorkoutStatus(date: LocalDate, done: Boolean, skipped: Boolean = false) = update { state ->
        state.withSnapshot(
            snapshot(state, date).copy(
                workoutDone = done,
                workoutSkipped = skipped,
            )
        )
    }

    fun saveExerciseSet(log: ExerciseLog) = update { state ->
        val filtered = state.exerciseLogs.filterNot {
            it.date == log.date && it.exerciseId == log.exerciseId && it.setNumber == log.setNumber
        }
        state.copy(exerciseLogs = filtered + log)
    }

    fun upsertShoppingTemplate(template: ShoppingTemplate) = update { state ->
        val exists = state.shoppingTemplates.any { it.id == template.id }
        state.copy(
            shoppingTemplates = if (exists) {
                state.shoppingTemplates.map { if (it.id == template.id) template else it }
            } else {
                state.shoppingTemplates + template
            }
        )
    }

    fun newShoppingTemplate(
        name: String = "",
        date: LocalDate = LocalDate.now(),
    ) = ShoppingTemplate(
        id = UUID.randomUUID().toString(),
        name = name,
        category = "Pantry",
        defaultQuantity = 1.0,
        unit = "piece",
        estimatedPrice = 0.0,
        recurrence = RecurrenceRule(ShoppingFrequency.WEEKLY, setOf(date.dayOfWeek)),
        startDate = date,
    )

    fun duplicateShoppingTemplate(id: String) = update { state ->
        val source = state.shoppingTemplates.firstOrNull { it.id == id } ?: return@update state
        state.copy(
            shoppingTemplates = state.shoppingTemplates + source.copy(
                id = UUID.randomUUID().toString(),
                name = "${source.name} copy",
                active = true,
            )
        )
    }

    fun setShoppingTemplateActive(id: String, active: Boolean) = update { state ->
        state.copy(
            shoppingTemplates = state.shoppingTemplates.map {
                if (it.id == id) it.copy(active = active) else it
            }
        )
    }

    fun deleteShoppingTemplate(id: String) = update { state ->
        // Purchase logs deliberately remain: they are immutable snapshots of what happened.
        state.copy(shoppingTemplates = state.shoppingTemplates.filterNot { it.id == id })
    }

    fun logPurchase(
        template: ShoppingTemplate,
        quantity: Double,
        unit: String,
        amount: Double,
        date: LocalDate = LocalDate.now(),
        status: PurchaseStatus = PurchaseStatus.PURCHASED,
    ) = upsertPurchase(
        PurchaseLog(
            id = System.currentTimeMillis(),
            templateId = template.id,
            itemName = template.name,
            category = template.category,
            actualQuantity = quantity,
            unit = unit,
            amount = if (status == PurchaseStatus.SKIPPED) 0.0 else amount,
            purchaseDate = date,
            status = status,
        )
    )

    fun upsertPurchase(log: PurchaseLog) = update { state ->
        val exists = state.purchaseLogs.any { it.id == log.id }
        state.copy(
            purchaseLogs = if (exists) {
                state.purchaseLogs.map { if (it.id == log.id) log else it }
            } else {
                state.purchaseLogs + log
            }
        )
    }

    fun removePurchase(id: Long) = update { state ->
        state.copy(purchaseLogs = state.purchaseLogs.filterNot { it.id == id })
    }

    fun addPurchase(
        itemName: String,
        category: String,
        amount: Double,
        date: LocalDate = LocalDate.now(),
    ) = upsertPurchase(
        PurchaseLog(
            id = System.currentTimeMillis(),
            templateId = null,
            itemName = itemName.trim().take(80),
            category = category.trim().ifBlank { "Other" }.take(40),
            actualQuantity = 1.0,
            unit = "item",
            amount = amount.coerceAtLeast(0.0),
            purchaseDate = date,
        )
    )

    /**
     * Legacy entry point retained for compatibility with older call sites.
     */
    fun addExpense(
        item: String,
        quantity: String,
        amount: Double,
        date: LocalDate = LocalDate.now(),
    ) = upsertPurchase(
        PurchaseLog(
            id = System.currentTimeMillis(),
            templateId = null,
            itemName = item,
            category = "Other",
            actualQuantity = quantity.firstNumberOrNull() ?: 1.0,
            unit = quantity.substringAfter(' ', "item").ifBlank { "item" },
            amount = amount,
            purchaseDate = date,
        )
    )

    fun removeExpense(id: Long) = removePurchase(id)

    fun newShoppingItem() = ShoppingItem(id = UUID.randomUUID().toString(), name = "")

    fun upsertShoppingItem(item: ShoppingItem) = update { state ->
        val clean = item.copy(
            name = item.name.trim().take(80),
            quantity = item.quantity.trim().take(40),
            estimatedPrice = item.estimatedPrice?.coerceAtLeast(0.0),
        )
        val exists = state.shoppingItems.any { it.id == clean.id }
        state.copy(
            shoppingItems = if (exists) {
                state.shoppingItems.map { if (it.id == clean.id) clean else it }
            } else {
                state.shoppingItems + clean
            }
        )
    }

    fun toggleShoppingItem(id: String) = update { state ->
        state.copy(
            shoppingItems = state.shoppingItems.map {
                if (it.id == id) it.copy(bought = !it.bought) else it
            }
        )
    }

    fun deleteShoppingItem(id: String) = update { state ->
        state.copy(shoppingItems = state.shoppingItems.filterNot { it.id == id })
    }

    fun setMotivation(motivation: Motivation) = update { it.copy(motivation = motivation) }

    fun setUnlockReminder(enabled: Boolean) = update { it.copy(unlockReminderEnabled = enabled) }

    fun setDailyNotification(enabled: Boolean) = update { it.copy(dailyNotificationEnabled = enabled) }

    fun setBudget(amount: Double) = update { it.copy(monthlyBudget = amount.coerceAtLeast(0.0)) }

    fun setBudgetCycleStartDay(day: Int) = update {
        it.copy(budgetCycleStartDay = day.coerceIn(1, 31))
    }

    fun saveProfileDraft(profile: UserProfile) = update {
        it.copy(profile = profile.copy(onboardingComplete = false))
    }

    fun completeOnboarding(profile: UserProfile) = update { state ->
        val completed = profile.copy(
            targetWeightKg = maxOf(profile.targetWeightKg, profile.currentWeightKg + 0.1),
            goal = PrimaryGoal.GAIN,
            goalDurationMonths = profile.goalDurationMonths
                .takeIf { it in setOf(1, 2, 3, 6, 9, 12) }
                ?: 6,
            onboardingStep = 7,
            onboardingComplete = true,
            wakeTime = normalizedClock(profile.wakeTime) ?: "07:00",
            weighInTime = normalizedClock(profile.weighInTime) ?: "08:00",
            workoutTime = normalizedClock(profile.workoutTime) ?: "20:30",
            sleepTime = normalizedClock(profile.sleepTime) ?: "23:30",
            loadedTrainingCleared = profile.loadedTrainingCleared &&
                profile.injuries.isBlank(),
        )
        state.copy(
            profile = completed,
            planVersions = state.planVersions + completed.toPlanVersion(),
            monthlyBudget = completed.monthlyFoodBudget.coerceAtLeast(0.0),
        )
    }

    fun restartOnboarding() = update { state ->
        state.copy(profile = state.profile.copy(onboardingStep = 0, onboardingComplete = false))
    }

    fun setLoadedTrainingCleared(cleared: Boolean) = update { state ->
        state.copy(profile = state.profile.copy(loadedTrainingCleared = cleared))
    }

    fun markTourSeen(tour: String) = update { state ->
        state.copy(seenTours = state.seenTours + tour)
    }

    fun resetTours() = update { it.copy(seenTours = emptySet()) }

    fun setMeditationLog(date: LocalDate, minutes: Int?) = update { state ->
        val key = date.toString()
        val cleanMinutes = minutes?.coerceIn(1, 300)
        state.copy(
            meditation = state.meditation.copy(
                completedDates = if (cleanMinutes == null) {
                    state.meditation.completedDates - key
                } else {
                    state.meditation.completedDates + key
                },
                minutesByDate = if (cleanMinutes == null) {
                    state.meditation.minutesByDate - key
                } else {
                    state.meditation.minutesByDate + (key to cleanMinutes)
                },
            )
        )
    }

    fun markMealsNotified(keys: Set<String>) = update { state ->
        val oldest = LocalDate.now().minusDays(2).toString()
        state.copy(
            notifiedMealKeys = (state.notifiedMealKeys + keys).filterTo(mutableSetOf()) {
                it.substringBefore('|') >= oldest
            }
        )
    }

    fun updateFoodTask(task: FoodTask) = update {
        it.copy(foodOverrides = it.foodOverrides + (task.id to task))
    }

    fun updateExercise(exercise: Exercise) = update {
        it.copy(exerciseOverrides = it.exerciseOverrides + (exercise.id to exercise))
    }

    fun newFoodTask() = FoodTask(
        id = "meal-${UUID.randomUUID()}",
        time = "",
        title = "",
        detail = "",
        calories = 0,
        protein = 0,
    )

    fun upsertFoodTask(
        group: MealPlanGroup,
        task: FoodTask,
        position: Int,
    ) = update { state ->
        val current = editableMeals(state, group).filterNot { it.id == task.id }.toMutableList()
        val clean = task.copy(
            time = task.time.trim().take(30),
            title = task.title.trim().ifBlank { "Meal" }.take(80),
            detail = task.detail.trim().take(180),
            calories = task.calories.coerceIn(0, 5000),
            protein = task.protein.coerceIn(0, 500),
        )
        current.add((position - 1).coerceIn(0, current.size), clean)
        state.copy(customMealPlans = state.customMealPlans + (group to current))
    }

    fun deleteFoodTask(group: MealPlanGroup, taskId: String) = update { state ->
        val remaining = editableMeals(state, group).filterNot { it.id == taskId }
        state.copy(customMealPlans = state.customMealPlans + (group to remaining))
    }

    fun newExercise() = Exercise(
        id = "exercise-${UUID.randomUUID()}",
        name = "",
        weight = "Bodyweight",
        sets = 3,
        reps = "8-12",
        technique = "",
    )

    fun upsertExercise(
        workoutName: String,
        exercise: Exercise,
        position: Int,
    ) = update { state ->
        val current = editableExercises(state, workoutName)
            .filterNot { it.id == exercise.id }
            .toMutableList()
        val clean = exercise.copy(
            name = exercise.name.trim().ifBlank { "Exercise" }.take(80),
            weight = exercise.weight.trim().ifBlank { "Bodyweight" }.take(60),
            sets = exercise.sets.coerceIn(1, 20),
            reps = exercise.reps.trim().take(40),
            technique = exercise.technique.trim().take(240),
            commonMistake = exercise.commonMistake.trim().take(200),
            muscles = exercise.muscles.trim().take(120),
            equipment = exercise.equipment.trim().ifBlank { "Bodyweight" }.take(100),
            learnUrl = exercise.learnUrl.trim().take(500),
        )
        current.add((position - 1).coerceIn(0, current.size), clean)
        state.copy(customWorkoutPlans = state.customWorkoutPlans + (workoutName to current))
    }

    fun deleteExercise(workoutName: String, exerciseId: String) = update { state ->
        val remaining = editableExercises(state, workoutName).filterNot { it.id == exerciseId }
        state.copy(customWorkoutPlans = state.customWorkoutPlans + (workoutName to remaining))
    }

    fun importDailyCsv(csv: String): Int {
        var imported = 0
        val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return 0
        val headers = parseCsvLine(lines.first()).map { it.trim() }
        lines.drop(1).forEach { line ->
            runCatching {
                val values = parseCsvLine(line)
                val row = headers.zip(values).toMap()
                val dateText = row["Date"] ?: return@runCatching
                val date = parseDate(dateText)
                val current = snapshot(_state.value, date)
                val legacyTaskMap = mapOf(
                    "Breakfast_done" to "breakfast",
                    "Office_lunch_done" to "lunch",
                    "Banana_done" to "banana_am",
                    "Peanuts_65g_done" to "snack",
                    "Roasted_chana_50g_done" to "snack",
                    "Eggs_2_done" to "eggs",
                    "Sattu_50g_done" to "sattu_post",
                    "Soy_or_dal_done" to "dinner",
                )
                val completed = current.completedFood.toMutableSet()
                legacyTaskMap.forEach { (column, id) ->
                    if (row[column].isTruthy()) completed += id
                }
                val workoutDone =
                    row["Workout_done"].isTruthy() ||
                        row["Workout_done"]?.equals("Done", true) == true
                val importedSnapshot = current.copy(
                    completedFood = completed,
                    workoutDone = workoutDone,
                    weightKg = row["Weigh_AM_kg"]?.toDoubleOrNull()
                        ?: row["Weight_kg"]?.toDoubleOrNull()
                        ?: current.weightKg,
                    sleepHours = row["Sleep_hours"]?.toDoubleOrNull() ?: current.sleepHours,
                    notes = row["Notes"].orEmpty().ifBlank { current.notes },
                )
                _state.value = _state.value.withSnapshot(importedSnapshot)
                imported++
            }
        }
        saveState(_state.value)
        return imported
    }

    fun snapshot(date: LocalDate): DaySnapshot = snapshot(_state.value, date)

    private fun snapshot(state: AppState, date: LocalDate) =
        state.snapshots[date.toString()] ?: DaySnapshot(date)

    private fun AppState.withSnapshot(day: DaySnapshot) =
        copy(snapshots = snapshots + (day.date.toString() to day))

    private fun editableMeals(state: AppState, group: MealPlanGroup): List<FoodTask> =
        state.customMealPlans[group] ?: state.foodFor(mealPlanDate(group))

    private fun mealPlanDate(group: MealPlanGroup): LocalDate {
        var date = LocalDate.now()
        while (
            (group == MealPlanGroup.SUNDAY) !=
            (date.dayOfWeek == DayOfWeek.SUNDAY)
        ) {
            date = date.plusDays(1)
        }
        return date
    }

    private fun editableExercises(state: AppState, workoutName: String): List<Exercise> =
        state.customWorkoutPlans[workoutName]
            ?: SeedPlan.workouts.values
                .firstOrNull { it.name == workoutName }
                ?.exercises
                ?.map { state.exerciseOverrides[it.id] ?: it }
                .orEmpty()

    private fun update(transform: (AppState) -> AppState) {
        _state.value = transform(_state.value)
        saveState(_state.value)
    }

    private fun saveState(state: AppState) {
        val snapshots = JSONArray()
        state.snapshots.values.forEach { day ->
            snapshots.put(JSONObject().apply {
                put("date", day.date.toString())
                put("food", JSONArray(day.completedFood.toList()))
                put("workoutDone", day.workoutDone)
                put("workoutSkipped", day.workoutSkipped)
                put("sleep", day.sleepHours)
                put("weight", day.weightKg)
                put("notes", day.notes)
            })
        }
        val logs = JSONArray()
        state.exerciseLogs.forEach { log ->
            logs.put(JSONObject().apply {
                put("date", log.date)
                put("workout", log.workout)
                put("exercise", log.exerciseId)
                put("set", log.setNumber)
                put("reps", log.reps)
                put("weight", log.weightKg)
            })
        }
        val extraFoodLogs = JSONArray()
        state.extraFoodLogs.forEach { food ->
            extraFoodLogs.put(JSONObject().apply {
                put("id", food.id)
                put("date", food.date.toString())
                put("name", food.name)
                put("calories", food.calories)
                put("protein", food.protein)
            })
        }
        val expenses = JSONArray()
        state.expenses.forEach { expense ->
            expenses.put(JSONObject().apply {
                put("id", expense.id)
                put("date", expense.date)
                put("item", expense.item)
                put("quantity", expense.quantity)
                put("amount", expense.amount)
            })
        }
        val templates = JSONArray()
        state.shoppingTemplates.forEach { template ->
            templates.put(JSONObject().apply {
                put("id", template.id)
                put("name", template.name)
                put("category", template.category)
                put("quantity", template.defaultQuantity)
                put("unit", template.unit)
                put("price", template.estimatedPrice)
                put("frequency", template.recurrence.frequency.name)
                put("weekdays", JSONArray(template.recurrence.weekdays.map { it.name }))
                put("dayOfMonth", template.recurrence.dayOfMonth)
                put("startDate", template.startDate.toString())
                put("active", template.active)
            })
        }
        val shoppingItems = JSONArray()
        state.shoppingItems.forEach { item ->
            shoppingItems.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("quantity", item.quantity)
                put("price", item.estimatedPrice)
                put("bought", item.bought)
            })
        }
        val purchaseLogs = JSONArray()
        state.purchaseLogs.forEach { purchase ->
            purchaseLogs.put(JSONObject().apply {
                put("id", purchase.id)
                put("templateId", purchase.templateId)
                put("itemName", purchase.itemName)
                put("category", purchase.category)
                put("quantity", purchase.actualQuantity)
                put("unit", purchase.unit)
                put("amount", purchase.amount)
                put("date", purchase.purchaseDate.toString())
                put("status", purchase.status.name)
            })
        }
        val foodOverrides = JSONArray()
        state.foodOverrides.values.forEach { task ->
            foodOverrides.put(JSONObject().apply {
                put("id", task.id)
                put("time", task.time)
                put("title", task.title)
                put("detail", task.detail)
                put("calories", task.calories)
                put("protein", task.protein)
            })
        }
        val exerciseOverrides = JSONArray()
        state.exerciseOverrides.values.forEach { exercise ->
            exerciseOverrides.put(JSONObject().apply {
                put("id", exercise.id)
                put("name", exercise.name)
                put("weight", exercise.weight)
                put("sets", exercise.sets)
                put("reps", exercise.reps)
                put("technique", exercise.technique)
                put("commonMistake", exercise.commonMistake)
                put("muscles", exercise.muscles)
                put("equipment", exercise.equipment)
                put("learnUrl", exercise.learnUrl)
            })
        }
        val customMealPlans = JSONArray()
        state.customMealPlans.forEach { (group, meals) ->
            customMealPlans.put(JSONObject().apply {
                put("group", group.name)
                put("meals", JSONArray().apply {
                    meals.forEach { task ->
                        put(JSONObject().apply {
                            put("id", task.id)
                            put("time", task.time)
                            put("title", task.title)
                            put("detail", task.detail)
                            put("calories", task.calories)
                            put("protein", task.protein)
                        })
                    }
                })
            })
        }
        val customWorkoutPlans = JSONArray()
        state.customWorkoutPlans.forEach { (workoutName, exercises) ->
            customWorkoutPlans.put(JSONObject().apply {
                put("workoutName", workoutName)
                put("exercises", JSONArray().apply {
                    exercises.forEach { exercise ->
                        put(JSONObject().apply {
                            put("id", exercise.id)
                            put("name", exercise.name)
                            put("weight", exercise.weight)
                            put("sets", exercise.sets)
                            put("reps", exercise.reps)
                            put("technique", exercise.technique)
                            put("commonMistake", exercise.commonMistake)
                            put("muscles", exercise.muscles)
                            put("equipment", exercise.equipment)
                            put("learnUrl", exercise.learnUrl)
                        })
                    }
                })
            })
        }
        val profile = JSONObject().apply {
            put("name", state.profile.name)
            put("age", state.profile.age)
            put("gender", state.profile.gender.name)
            put("heightCm", state.profile.heightCm)
            put("currentWeightKg", state.profile.currentWeightKg)
            put("targetWeightKg", state.profile.targetWeightKg)
            put("goal", state.profile.goal.name)
            put("goalDurationMonths", state.profile.goalDurationMonths)
            put("weighInDay", state.profile.weighInDay.name)
            put("weighInTime", state.profile.weighInTime)
            put("diet", state.profile.diet.name)
            put("mealsPerDay", state.profile.mealsPerDay)
            put("wakeTime", state.profile.wakeTime)
            put("workoutTime", state.profile.workoutTime)
            put("sleepTime", state.profile.sleepTime)
            put("trainingDays", JSONArray(state.profile.trainingDays.map { it.name }))
            put("equipment", state.profile.equipment)
            put("injuries", state.profile.injuries)
            put("loadedTrainingCleared", state.profile.loadedTrainingCleared)
            put("monthlyFoodBudget", state.profile.monthlyFoodBudget)
            put("meditationMinutes", state.profile.meditationMinutes)
            put("onboardingStep", state.profile.onboardingStep)
            put("onboardingComplete", state.profile.onboardingComplete)
        }
        val planVersions = JSONArray()
        state.planVersions.forEach { plan ->
            planVersions.put(JSONObject().apply {
                put("id", plan.id)
                put("effectiveFrom", plan.effectiveFrom.toString())
                put("targetDate", plan.targetDate.toString())
                put("startWeightKg", plan.startWeightKg)
                put("targetWeightKg", plan.targetWeightKg)
                put("calorieTarget", plan.calorieTarget)
                put("proteinTarget", plan.proteinTarget)
                put("trainingDays", JSONArray(plan.trainingDays.map { it.name }))
                put("diet", plan.diet.name)
                put("meditationMinutes", plan.meditationMinutes)
            })
        }
        val meditation = JSONObject().apply {
            put("completedDates", JSONArray(state.meditation.completedDates.toList()))
            put("minuteLogs", JSONArray().apply {
                state.meditation.minutesByDate
                    .toSortedMap()
                    .forEach { (date, minutes) ->
                        put(JSONObject().apply {
                            put("date", date)
                            put("minutes", minutes)
                        })
                    }
            })
        }
        prefs.edit {
            putString("selected_date", state.selectedDate.toString())
            putString("snapshots", snapshots.toString())
            putString("exercise_logs", logs.toString())
            putString("extra_food_logs", extraFoodLogs.toString())
            putString("expenses", expenses.toString())
            putString("shopping_templates", templates.toString())
            putString("shopping_items", shoppingItems.toString())
            putString("purchase_logs", purchaseLogs.toString())
            putString("motivation", JSONObject().apply {
                put("quote", state.motivation.quote)
                put("author", state.motivation.author)
                put("imageUrl", state.motivation.imageUrl)
                put("imageAuthor", state.motivation.imageAuthor)
            }.toString())
            putBoolean("unlock", state.unlockReminderEnabled)
            putBoolean("notifications", state.dailyNotificationEnabled)
            putFloat("budget", state.monthlyBudget.toFloat())
            putInt("budget_cycle_start_day", state.budgetCycleStartDay)
            putString("food_overrides", foodOverrides.toString())
            putString("exercise_overrides", exerciseOverrides.toString())
            putString("custom_meal_plans", customMealPlans.toString())
            putString("custom_workout_plans", customWorkoutPlans.toString())
            putString("profile", profile.toString())
            putString("plan_versions", planVersions.toString())
            putString("seen_tours", JSONArray(state.seenTours.toList()).toString())
            putString("meditation", meditation.toString())
            putString("notified_meals", JSONArray(state.notifiedMealKeys.toList()).toString())
        }
    }

    private fun loadState(): AppState = runCatching {
        val snapshots = mutableMapOf<String, DaySnapshot>()
        JSONArray(prefs.getString("snapshots", "[]")).forEachObject { json ->
            val date = LocalDate.parse(json.getString("date"))
            snapshots[date.toString()] = DaySnapshot(
                date = date,
                completedFood = json.getJSONArray("food").toStringSet(),
                workoutDone = json.optBoolean("workoutDone"),
                workoutSkipped = json.optBoolean("workoutSkipped"),
                sleepHours = json.nullableDouble("sleep"),
                weightKg = json.nullableDouble("weight"),
                notes = json.optString("notes"),
            )
        }
        val logs = mutableListOf<ExerciseLog>()
        JSONArray(prefs.getString("exercise_logs", "[]")).forEachObject {
            logs += ExerciseLog(
                it.getString("date"),
                it.getString("workout"),
                it.getString("exercise"),
                it.getInt("set"),
                it.getInt("reps"),
                it.getDouble("weight"),
            )
        }
        val extraFoodLogs = buildList {
            JSONArray(prefs.getString("extra_food_logs", "[]")).forEachObject {
                add(
                    ExtraFoodLog(
                        id = it.getLong("id"),
                        date = LocalDate.parse(it.getString("date")),
                        name = it.optString("name", "Extra food"),
                        calories = it.optInt("calories", 0).coerceAtLeast(0),
                        protein = it.optInt("protein", 0).coerceAtLeast(0),
                    )
                )
            }
        }
        val expenses = mutableListOf<Expense>()
        JSONArray(prefs.getString("expenses", "[]")).forEachObject {
            expenses += Expense(
                it.getLong("id"),
                it.getString("date"),
                it.getString("item"),
                it.optString("quantity"),
                it.getDouble("amount"),
            )
        }
        val templates = if (prefs.contains("shopping_templates")) {
            buildList {
                JSONArray(prefs.getString("shopping_templates", "[]")).forEachObject {
                    val weekdays = it.optJSONArray("weekdays")?.toDayOfWeekSet().orEmpty()
                    val frequency = enumValueOrDefault(
                        it.optString("frequency"),
                        ShoppingFrequency.WEEKLY,
                    )
                    add(
                        ShoppingTemplate(
                            id = it.getString("id"),
                            name = it.getString("name"),
                            category = it.optString("category", "Other"),
                            defaultQuantity = it.optDouble("quantity", 1.0),
                            unit = it.optString("unit", "item"),
                            estimatedPrice = it.optDouble("price", 0.0),
                            recurrence = RecurrenceRule(
                                frequency = frequency,
                                weekdays = weekdays,
                                dayOfMonth = it.nullableInt("dayOfMonth"),
                            ),
                            startDate = LocalDate.parse(
                                it.optString("startDate", LocalDate.now().toString())
                            ),
                            active = it.optBoolean("active", true),
                        )
                    )
                }
            }
        } else {
            SeedPlan.defaultShoppingTemplates
        }
        val shoppingItems = if (prefs.contains("shopping_items")) {
            buildList {
                JSONArray(prefs.getString("shopping_items", "[]")).forEachObject {
                    add(
                        ShoppingItem(
                            id = it.getString("id"),
                            name = it.getString("name"),
                            quantity = it.optString("quantity"),
                            estimatedPrice = it.nullableDouble("price"),
                            bought = it.optBoolean("bought"),
                        )
                    )
                }
            }
        } else {
            migrateShoppingTemplates(templates)
        }
        val purchases = if (prefs.contains("purchase_logs")) {
            buildList {
                JSONArray(prefs.getString("purchase_logs", "[]")).forEachObject {
                    add(
                        PurchaseLog(
                            id = it.getLong("id"),
                            templateId = it.nullableString("templateId"),
                            itemName = it.getString("itemName"),
                            category = it.optString("category", "Other"),
                            actualQuantity = it.optDouble("quantity", 1.0),
                            unit = it.optString("unit", "item"),
                            amount = it.optDouble("amount", 0.0),
                            purchaseDate = LocalDate.parse(it.getString("date")),
                            status = enumValueOrDefault(
                                it.optString("status"),
                                PurchaseStatus.PURCHASED,
                            ),
                        )
                    )
                }
            }
        } else {
            migrateLegacyExpenses(expenses)
        }
        val motivationJson = JSONObject(prefs.getString("motivation", "{}") ?: "{}")
        val motivation = if (motivationJson.has("quote")) {
            Motivation(
                motivationJson.getString("quote"),
                motivationJson.optString("author", "GainTrack"),
                motivationJson.optString("imageUrl"),
                motivationJson.optString("imageAuthor"),
            )
        } else {
            SeedPlan.fallbackMotivations[
                LocalDate.now().dayOfYear % SeedPlan.fallbackMotivations.size
            ]
        }
        val foodOverrides = mutableMapOf<String, FoodTask>()
        JSONArray(prefs.getString("food_overrides", "[]")).forEachObject {
            val task = FoodTask(
                it.getString("id"),
                it.getString("time"),
                it.getString("title"),
                it.getString("detail"),
                it.getInt("calories"),
                it.getInt("protein"),
            )
            foodOverrides[task.id] = task
        }
        val exerciseOverrides = mutableMapOf<String, Exercise>()
        JSONArray(prefs.getString("exercise_overrides", "[]")).forEachObject {
            val exercise = Exercise(
                it.getString("id"),
                it.getString("name"),
                it.getString("weight"),
                it.getInt("sets"),
                it.getString("reps"),
                it.getString("technique"),
                it.optString(
                    "commonMistake",
                    "Avoid rushing the movement or losing a neutral spine.",
                ),
                it.optString("muscles"),
                it.optString("equipment", "Bodyweight"),
                it.optString("learnUrl"),
            )
            exerciseOverrides[exercise.id] = exercise
        }
        val customMealPlans = mutableMapOf<MealPlanGroup, List<FoodTask>>()
        JSONArray(prefs.getString("custom_meal_plans", "[]")).forEachObject { planJson ->
            val group = runCatching {
                MealPlanGroup.valueOf(planJson.getString("group"))
            }.getOrNull()
            if (group != null) {
                val meals = buildList {
                    planJson.optJSONArray("meals")?.forEachObject {
                        add(
                            FoodTask(
                                id = it.getString("id"),
                                time = it.optString("time"),
                                title = it.optString("title", "Meal"),
                                detail = it.optString("detail"),
                                calories = it.optInt("calories", 0).coerceIn(0, 5000),
                                protein = it.optInt("protein", 0).coerceIn(0, 500),
                            )
                        )
                    }
                }
                customMealPlans[group] = meals
            }
        }
        val customWorkoutPlans = mutableMapOf<String, List<Exercise>>()
        JSONArray(prefs.getString("custom_workout_plans", "[]")).forEachObject { planJson ->
            val workoutName = planJson.optString("workoutName")
            if (workoutName.isNotBlank()) {
                val exercises = buildList {
                    planJson.optJSONArray("exercises")?.forEachObject {
                        add(
                            Exercise(
                                id = it.getString("id"),
                                name = it.optString("name", "Exercise"),
                                weight = it.optString("weight", "Bodyweight"),
                                sets = it.optInt("sets", 3).coerceIn(1, 20),
                                reps = it.optString("reps", "8-12"),
                                technique = it.optString("technique"),
                                commonMistake = it.optString(
                                    "commonMistake",
                                    "Avoid rushing the movement or losing a neutral spine.",
                                ),
                                muscles = it.optString("muscles"),
                                equipment = it.optString("equipment", "Bodyweight"),
                                learnUrl = it.optString("learnUrl"),
                            )
                        )
                    }
                }
                customWorkoutPlans[workoutName] = exercises
            }
        }
        val profileJson = JSONObject(prefs.getString("profile", "{}") ?: "{}")
        val latestStoredWeight = snapshots.values
            .filter { it.weightKg != null }
            .maxByOrNull { it.date }
            ?.weightKg
        val defaultProfile = UserProfile(
            currentWeightKg = latestStoredWeight ?: SeedPlan.startWeight,
            monthlyFoodBudget = prefs.getFloat("budget", 2000f).toDouble(),
        )
        val profile = if (profileJson.length() == 0) {
            defaultProfile
        } else {
            UserProfile(
                name = profileJson.optString("name"),
                age = profileJson.optInt("age", defaultProfile.age).coerceIn(13, 100),
                gender = enumValueOrDefault(
                    profileJson.optString("gender"),
                    defaultProfile.gender,
                ),
                heightCm = profileJson.optDouble("heightCm", defaultProfile.heightCm),
                currentWeightKg = profileJson.optDouble(
                    "currentWeightKg",
                    defaultProfile.currentWeightKg,
                ),
                targetWeightKg = maxOf(
                    profileJson.optDouble("targetWeightKg", defaultProfile.targetWeightKg),
                    profileJson.optDouble("currentWeightKg", defaultProfile.currentWeightKg) + 0.1,
                ),
                goal = PrimaryGoal.GAIN,
                goalDurationMonths = profileJson.optInt("goalDurationMonths", 6)
                    .takeIf { it in setOf(1, 2, 3, 6, 9, 12) }
                    ?: 6,
                weighInDay = enumValueOrDefault(
                    profileJson.optString("weighInDay"),
                    DayOfWeek.SUNDAY,
                ),
                weighInTime = profileJson.optString("weighInTime", "08:00")
                    .takeIf { parseClock(it) != null }
                    ?: "08:00",
                diet = enumValueOrDefault(
                    profileJson.optString("diet"),
                    defaultProfile.diet,
                ),
                mealsPerDay = profileJson.optInt(
                    "mealsPerDay",
                    defaultProfile.mealsPerDay,
                ).coerceIn(3, 8),
                wakeTime = profileJson.optString("wakeTime", defaultProfile.wakeTime),
                workoutTime = profileJson.optString(
                    "workoutTime",
                    defaultProfile.workoutTime,
                ),
                sleepTime = profileJson.optString("sleepTime", defaultProfile.sleepTime),
                trainingDays = profileJson.optJSONArray("trainingDays")
                    ?.toDayOfWeekSet()
                    .orEmpty()
                    .ifEmpty { defaultProfile.trainingDays },
                equipment = profileJson.optString("equipment", defaultProfile.equipment),
                injuries = profileJson.optString("injuries"),
                loadedTrainingCleared = profileJson.optBoolean(
                    "loadedTrainingCleared",
                    defaultProfile.loadedTrainingCleared,
                ),
                monthlyFoodBudget = profileJson.optDouble(
                    "monthlyFoodBudget",
                    defaultProfile.monthlyFoodBudget,
                ),
                meditationMinutes = profileJson.optInt(
                    "meditationMinutes",
                    defaultProfile.meditationMinutes,
                ).coerceIn(5, 10),
                onboardingStep = profileJson.optInt("onboardingStep", 0).coerceIn(0, 7),
                onboardingComplete = profileJson.optBoolean("onboardingComplete", false),
            )
        }
        val planVersions = buildList {
            JSONArray(prefs.getString("plan_versions", "[]")).forEachObject {
                add(
                    PlanVersion(
                        id = it.getString("id"),
                        effectiveFrom = LocalDate.parse(it.getString("effectiveFrom")),
                        targetDate = LocalDate.parse(it.getString("targetDate")),
                        startWeightKg = it.getDouble("startWeightKg"),
                        targetWeightKg = it.getDouble("targetWeightKg"),
                        calorieTarget = it.getInt("calorieTarget"),
                        proteinTarget = it.getInt("proteinTarget"),
                        trainingDays = it.optJSONArray("trainingDays")
                            ?.toDayOfWeekSet()
                            .orEmpty(),
                        diet = enumValueOrDefault(
                            it.optString("diet"),
                            DietPreference.EVERYTHING,
                        ),
                        meditationMinutes = it.optInt("meditationMinutes", 5),
                    )
                )
            }
        }
        val legacyNonGainProfile =
            profileJson.has("goal") && profileJson.optString("goal") != PrimaryGoal.GAIN.name
        val latestPlan = planVersions.maxByOrNull { it.effectiveFrom }
        val migratedPlanVersions = if (
            profile.onboardingComplete &&
            (
                legacyNonGainProfile ||
                    latestPlan == null ||
                    latestPlan.targetWeightKg <= latestPlan.startWeightKg
                )
        ) {
            planVersions + profile.toPlanVersion(id = "gain-only-${System.currentTimeMillis()}")
        } else {
            planVersions
        }
        val seenTours = JSONArray(prefs.getString("seen_tours", "[]")).toStringSet()
        val meditationJson = JSONObject(prefs.getString("meditation", "{}") ?: "{}")
        val legacyMeditationMinutes = meditationJson.optInt(
            "selectedMinutes",
            profile.meditationMinutes,
        ).coerceIn(5, 10)
        val completedMeditationDates = meditationJson.optJSONArray("completedDates")
            ?.toStringSet()
            .orEmpty()
        val storedMeditationMinutes = buildMap {
            meditationJson.optJSONArray("minuteLogs")?.forEachObject {
                val date = it.optString("date")
                if (date.isNotBlank()) {
                    put(date, it.optInt("minutes", legacyMeditationMinutes).coerceIn(1, 300))
                }
            }
        }
        val migratedMeditationMinutes = storedMeditationMinutes.toMutableMap().apply {
            completedMeditationDates.forEach { date ->
                if (date !in this) put(date, legacyMeditationMinutes)
            }
        }
        val meditation = MeditationState(
            completedDates = completedMeditationDates + migratedMeditationMinutes.keys,
            minutesByDate = migratedMeditationMinutes,
        )
        AppState(
            selectedDate = LocalDate.now(),
            snapshots = snapshots,
            extraFoodLogs = extraFoodLogs,
            exerciseLogs = logs,
            expenses = expenses,
            shoppingTemplates = templates,
            shoppingItems = shoppingItems,
            purchaseLogs = purchases,
            motivation = motivation,
            unlockReminderEnabled = prefs.getBoolean("unlock", false),
            dailyNotificationEnabled = prefs.getBoolean("notifications", true),
            monthlyBudget = prefs.getFloat("budget", 2000f).toDouble(),
            budgetCycleStartDay = prefs.getInt("budget_cycle_start_day", 1).coerceIn(1, 31),
            foodOverrides = foodOverrides,
            exerciseOverrides = exerciseOverrides,
            customMealPlans = customMealPlans,
            customWorkoutPlans = customWorkoutPlans,
            profile = profile,
            planVersions = migratedPlanVersions,
            seenTours = seenTours,
            meditation = meditation,
            notifiedMealKeys = JSONArray(
                prefs.getString("notified_meals", "[]")
            ).toStringSet(),
        )
    }.getOrElse {
        AppState(
            shoppingTemplates = SeedPlan.defaultShoppingTemplates,
            shoppingItems = SeedPlan.defaultShoppingTemplates.map { template ->
                ShoppingItem(
                    id = template.id,
                    name = template.name,
                    quantity = "${template.defaultQuantity} ${template.unit}",
                    estimatedPrice = template.estimatedPrice,
                )
            },
        )
    }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

private fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (index in 0 until length()) block(getJSONObject(index))
}

private fun JSONArray.toStringSet(): Set<String> =
    buildSet { for (index in 0 until length()) add(getString(index)) }

private fun JSONArray.toDayOfWeekSet(): Set<DayOfWeek> =
    buildSet {
        for (index in 0 until length()) {
            runCatching { DayOfWeek.valueOf(getString(index)) }.getOrNull()?.let(::add)
        }
    }

private fun JSONObject.nullableDouble(key: String): Double? =
    if (!has(key) || isNull(key)) null else getDouble(key)

private fun JSONObject.nullableInt(key: String): Int? =
    if (!has(key) || isNull(key)) null else getInt(key)

private fun JSONObject.nullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else getString(key)

private fun String.firstNumberOrNull(): Double? =
    Regex("""\d+(?:\.\d+)?""").find(this)?.value?.toDoubleOrNull()

internal fun migrateLegacyExpenses(expenses: List<Expense>): List<PurchaseLog> =
    expenses.map { expense ->
        PurchaseLog(
            id = expense.id,
            templateId = null,
            itemName = expense.item,
            category = "Legacy",
            actualQuantity = expense.quantity.firstNumberOrNull() ?: 1.0,
            unit = expense.quantity.substringAfter(' ', "item").ifBlank { "item" },
            amount = expense.amount,
            purchaseDate = LocalDate.parse(expense.date),
        )
    }

private fun String?.isTruthy(): Boolean =
    this?.trim()?.lowercase() in setOf("1", "true", "yes", "y", "done", "x", "✓")

private fun parseDate(value: String): LocalDate {
    return runCatching { LocalDate.parse(value) }.getOrElse {
        val parts = value.split("/")
        LocalDate.of(parts[2].toInt(), parts[0].toInt(), parts[1].toInt())
    }
}

private fun parseCsvLine(line: String): List<String> {
    val values = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    var index = 0
    while (index < line.length) {
        val char = line[index]
        when {
            char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                current.append('"')
                index++
            }
            char == '"' -> quoted = !quoted
            char == ',' && !quoted -> {
                values += current.toString()
                current.clear()
            }
            else -> current.append(char)
        }
        index++
    }
    values += current.toString()
    return values
}
