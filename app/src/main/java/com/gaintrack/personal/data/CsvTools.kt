package com.gaintrack.personal.data

import java.time.LocalDate

object CsvTools {
    fun dailyLog(state: AppState): String = buildString {
        appendLine("Date,Day,Weight_kg,Food_completed,Food_total,Estimated_calories,Estimated_protein_g,Meditation_minutes,Workout_plan,Workout_done,Sleep_hours,Notes")
        val meditationDates = state.meditation.completedDates.mapNotNull {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
        val start = minOf(
            SeedPlan.startDate,
            state.snapshots.values.minOfOrNull { it.date } ?: SeedPlan.startDate,
            meditationDates.minOrNull() ?: SeedPlan.startDate,
        )
        val end = maxOf(
            LocalDate.now(),
            state.snapshots.values.maxOfOrNull { it.date } ?: LocalDate.now(),
            meditationDates.maxOrNull() ?: LocalDate.now(),
        )
        var date = start
        while (!date.isAfter(end)) {
            val snapshot = state.snapshots[date.toString()] ?: DaySnapshot(date)
            val foods = state.foodFor(date)
            val doneFoods = foods.filter { it.id in snapshot.completedFood }
            val extraFoods = state.extraFoodLogs.filter { it.date == date }
            appendCsv(
                date, date.dayOfWeek, snapshot.weightKg ?: "", doneFoods.size, foods.size,
                doneFoods.sumOf { it.calories } + extraFoods.sumOf { it.calories },
                doneFoods.sumOf { it.protein } + extraFoods.sumOf { it.protein },
                state.meditation.minutesFor(date) ?: "",
                state.workoutFor(date)?.name ?: "Rest", snapshot.workoutDone,
                snapshot.sleepHours ?: "", snapshot.notes
            )
            appendLine()
            date = date.plusDays(1)
        }
    }

    fun exerciseLog(state: AppState): String = buildString {
        appendLine("Date,Workout,Exercise,Set,Reps,Weight_kg,Volume_kg")
        state.exerciseLogs.sortedWith(compareBy({ it.date }, { it.workout }, { it.exerciseId }, { it.setNumber })).forEach {
            appendCsv(it.date, it.workout, it.exerciseId, it.setNumber, it.reps, it.weightKg, it.reps * it.weightKg)
            appendLine()
        }
    }

    fun expenses(state: AppState): String = buildString {
        appendLine("Date,Item,Category,Quantity,Unit,Amount_INR,Status")
        state.purchaseLogs.sortedBy { it.purchaseDate }.forEach {
            appendCsv(
                it.purchaseDate,
                it.itemName,
                it.category,
                it.actualQuantity,
                it.unit,
                it.amount,
                it.status,
            )
            appendLine()
        }
    }

    private fun StringBuilder.appendCsv(vararg values: Any) {
        append(values.joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" })
    }
}
