package com.gaintrack.personal.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object SeedPlan {
    val startDate: LocalDate = LocalDate.of(2026, 7, 16)
    val planStart: LocalDate = LocalDate.of(2026, 7, 20)
    val planEnd: LocalDate = LocalDate.of(2027, 1, 17)
    const val startWeight = 54.5
    const val targetWeight = 65.5

    private val weekdayFood = listOf(
        FoodTask("breakfast", "Morning", "Home breakfast", "Normal roti-sabji, oats, or rice-dal. Do not skip.", 450, 15),
        FoodTask("banana_am", "10:30", "Banana + peanuts", "1 banana and 35 g peanuts", 310, 10),
        FoodTask("lunch", "13:30", "Office lunch", "4 roti or 2 cups rice, sabji, and dal/chana or soy", 700, 25),
        FoodTask("snack", "16:30", "Chana + peanuts", "50 g roasted chana and 30 g peanuts", 360, 18),
        FoodTask("eggs", "19:30", "2 boiled eggs", "Add one banana on workout days", 210, 13),
        FoodTask("sattu_pre", "21:05", "Sattu with water", "25 g before training", 100, 5),
        FoodTask("dinner", "After workout", "Home dinner", "4 roti or rice, sabji, and dal/chana/soy", 650, 25),
        FoodTask("sattu_post", "After dinner", "Sattu with water", "Remaining 25 g", 100, 5),
    )

    private val sundayFood = listOf(
        FoodTask("breakfast", "Morning", "Home breakfast", "Normal home breakfast", 450, 15),
        FoodTask("banana_am", "11:00", "Banana + peanuts", "1 banana and 35 g peanuts", 310, 10),
        FoodTask("lunch", "Lunch", "Home lunch", "Rice/roti, sabji, and dal/chana/soy", 700, 25),
        FoodTask("snack", "16:30", "Chana + peanuts", "50 g roasted chana and 30 g peanuts", 360, 18),
        FoodTask("banana_pre", "Pre-workout", "Banana", "45–60 minutes before training", 105, 1),
        FoodTask("eggs_dinner", "After workout", "Eggs + dinner", "2 eggs and normal home dinner", 790, 38),
        FoodTask("sattu", "Evening", "Sattu with water", "50 g, divided if necessary", 200, 10),
    )

    fun foodFor(date: LocalDate): List<FoodTask> {
        if (date.dayOfWeek == DayOfWeek.SUNDAY) return sundayFood
        val workout = workoutFor(date) != null
        return weekdayFood.map {
            if (it.id == "eggs" && workout) it.copy(calories = 315, detail = "2 boiled eggs + 1 banana (workout day)")
            else it
        }
    }

    val workouts = mapOf(
        DayOfWeek.MONDAY to WorkoutPlan("Upper Body A", listOf(
            Exercise("floor_press", "Dumbbell floor press", "10 kg each hand", 4, "8–12", "Lower until upper arms touch the floor; press smoothly."),
            Exercise("one_arm_row", "One-arm dumbbell row", "20 kg", 4, "8–12 / side", "Support one hand and pull toward your waist."),
            Exercise("overhead_press_a", "Seated overhead press", "5 kg each hand", 3, "8–12", "Sit upright; do not arch your lower back."),
            Exercise("lateral_raise", "Lateral raise", "5 kg one arm", 2, "10–15 / side", "Lift only to shoulder height. Use bottles if needed."),
            Exercise("curl", "Dumbbell curl", "5 kg each hand", 2, "10–15", "Keep elbows beside your body; do not swing."),
            Exercise("triceps", "Overhead triceps extension", "10 kg", 2, "10–15", "Hold one dumbbell securely with both hands."),
        )),
        DayOfWeek.WEDNESDAY to WorkoutPlan("Lower Body A", listOf(
            Exercise("goblet_squat", "Goblet squat", "20 kg", 4, "8–15", "Keep heels down and chest upright."),
            Exercise("rdl", "Romanian deadlift", "10 kg each hand", 4, "8–12", "Move hips backward and keep your back neutral."),
            Exercise("split_squat", "Bulgarian split squat", "Bodyweight", 3, "8–12 / leg", "Use a stable chair. Add 5 kg each hand when ready."),
            Exercise("calf_a", "Standing calf raise", "10 kg each hand", 3, "15–25", "Pause at the top and lower slowly."),
            Exercise("plank", "Plank", "Bodyweight", 3, "30–60 sec", "Keep a straight line from shoulders to feet."),
        )),
        DayOfWeek.FRIDAY to WorkoutPlan("Upper Body B", listOf(
            Exercise("pushup", "Push-ups", "Bodyweight", 4, "8–20", "Keep the whole body straight."),
            Exercise("bent_row", "Bent-over dumbbell row", "10 kg each hand", 4, "10–15", "Keep your back neutral and pull with control."),
            Exercise("overhead_press_b", "Seated overhead press", "5 kg each hand", 3, "8–12", "Sit upright; keep ribs down."),
            Exercise("rear_delt", "Rear-delt raise", "Bottles or 5 kg", 3, "12–20", "Use a light, controlled load."),
            Exercise("hammer", "Hammer curl", "5 kg each hand", 2, "10–15", "Keep palms facing each other."),
            Exercise("close_pushup", "Close-grip push-up", "Bodyweight", 2, "8–15", "Keep elbows close to the body."),
        )),
        DayOfWeek.SUNDAY to WorkoutPlan("Lower Body B", listOf(
            Exercise("reverse_lunge", "Reverse lunge", "Bodyweight", 4, "8–12 / leg", "Add 5 kg each hand after clean bodyweight reps."),
            Exercise("single_rdl", "Single-leg Romanian deadlift", "10 kg", 3, "10–15 / leg", "Hold weight opposite the working leg; use a wall."),
            Exercise("bridge", "Weighted glute bridge", "20 kg", 4, "12–20", "Place the dumbbell securely across your hips."),
            Exercise("slow_squat", "Slow goblet squat", "20 kg", 3, "10–15", "Lower for three seconds, pause, then stand."),
            Exercise("calf_b", "Standing calf raise", "20 kg each hand", 3, "15–25", "Use a wall for balance."),
            Exercise("dead_bug", "Dead bug", "Bodyweight", 3, "8–12 / side", "Keep your lower back against the floor."),
        )),
    )

    fun workoutFor(date: LocalDate): WorkoutPlan? = workouts[date.dayOfWeek]

    fun workoutForTrainingIndex(index: Int): WorkoutPlan =
        workouts.values.toList()[index.mod(workouts.size)]

    fun prescribedSets(date: LocalDate, exercise: Exercise): Int {
        val week = ((ChronoUnit.DAYS.between(planStart, date).coerceAtLeast(0) / 7) + 1).toInt()
        return when (week) {
            1 -> minOf(2, exercise.sets)
            2 -> minOf(3, exercise.sets)
            else -> exercise.sets
        }
    }

    fun targetFor(date: LocalDate): Double {
        val progress = (ChronoUnit.DAYS.between(planStart, date).toDouble() /
            ChronoUnit.DAYS.between(planStart, planEnd).toDouble()).coerceIn(0.0, 1.0)
        return startWeight + (targetWeight - startWeight) * progress
    }

    val defaultShoppingTemplates = listOf(
        ShoppingTemplate(
            id = "seed_banana",
            name = "Banana",
            category = "Fruit",
            defaultQuantity = 1.0,
            unit = "piece",
            estimatedPrice = 10.0,
            recurrence = RecurrenceRule(ShoppingFrequency.DAILY),
            startDate = startDate,
        ),
        ShoppingTemplate(
            id = "seed_eggs",
            name = "Eggs",
            category = "Protein",
            defaultQuantity = 2.0,
            unit = "eggs",
            estimatedPrice = 17.0,
            recurrence = RecurrenceRule(ShoppingFrequency.DAILY),
            startDate = startDate,
        ),
        ShoppingTemplate(
            id = "seed_chana",
            name = "Roasted chana",
            category = "Pantry",
            defaultQuantity = 350.0,
            unit = "g",
            estimatedPrice = 70.0,
            recurrence = RecurrenceRule(ShoppingFrequency.WEEKLY, setOf(DayOfWeek.SATURDAY)),
            startDate = startDate,
        ),
        ShoppingTemplate(
            id = "seed_peanuts",
            name = "Peanuts",
            category = "Pantry",
            defaultQuantity = 450.0,
            unit = "g",
            estimatedPrice = 90.0,
            recurrence = RecurrenceRule(ShoppingFrequency.WEEKLY, setOf(DayOfWeek.SATURDAY)),
            startDate = startDate,
        ),
        ShoppingTemplate(
            id = "seed_sattu",
            name = "Sattu",
            category = "Pantry",
            defaultQuantity = 1.5,
            unit = "kg",
            estimatedPrice = 240.0,
            recurrence = RecurrenceRule(ShoppingFrequency.MONTHLY, dayOfMonth = 1),
            startDate = startDate,
        ),
        ShoppingTemplate(
            id = "seed_soy",
            name = "Soy chunks",
            category = "Protein",
            defaultQuantity = 1.0,
            unit = "kg",
            estimatedPrice = 220.0,
            recurrence = RecurrenceRule(ShoppingFrequency.MONTHLY, dayOfMonth = 1),
            startDate = startDate,
        ),
    )

    val fallbackMotivations = listOf(
        Motivation("Small daily improvements create remarkable results.", "GainTrack"),
        Motivation("Consistency is stronger than a perfect day.", "GainTrack"),
        Motivation("Eat enough, train clean, sleep well, repeat.", "GainTrack"),
        Motivation("Your future strength is built by today's choices.", "GainTrack"),
        Motivation("Progress counts even when it feels slow.", "GainTrack"),
    )
}
