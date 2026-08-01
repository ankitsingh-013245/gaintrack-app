package com.gaintrack.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class TrackingTest {
    private val monday = LocalDate.of(2026, 7, 20)

    @Test
    fun overdueMealsWaitForGraceAndSumOnlyUncheckedMeals() {
        val breakfast = FoodTask("breakfast", "08:00", "Breakfast", "", 450, 15)
        val lunch = FoodTask("lunch", "13:30", "Lunch", "", 700, 25)
        val state = AppState(
            foodOverrides = mapOf(
                breakfast.id to breakfast,
                lunch.id to lunch,
            ),
            snapshots = mapOf(
                monday.toString() to DaySnapshot(
                    monday,
                    completedFood = SeedPlan.foodFor(monday)
                        .mapTo(mutableSetOf()) { it.id }
                        .apply { remove("lunch") },
                )
            ),
        )

        assertTrue(
            overdueMeals(state, LocalDateTime.of(2026, 7, 20, 13, 59)).meals.isEmpty()
        )
        val summary = overdueMeals(state, LocalDateTime.of(2026, 7, 20, 14, 0))

        assertEquals(listOf("lunch"), summary.meals.map { it.id })
        assertEquals(700, summary.calories)
        assertEquals(25, summary.protein)
    }

    @Test
    fun nextFutureMealSkipsPastAndCompletedMeals() {
        val state = AppState(
            foodOverrides = mapOf(
                "breakfast" to FoodTask("breakfast", "08:00", "Breakfast", "", 450, 15),
                "banana_am" to FoodTask("banana_am", "10:30", "Snack", "", 300, 10),
                "lunch" to FoodTask("lunch", "13:30", "Lunch", "", 700, 25),
            ),
            snapshots = mapOf(
                monday.toString() to DaySnapshot(
                    monday,
                    completedFood = setOf("banana_am"),
                )
            ),
        )

        val result = state.nextFutureMeal(monday, java.time.LocalTime.of(11, 0))

        assertEquals("lunch", result?.id)
    }

    @Test
    fun flexibleMealLabelsResolveAgainstTheUsersRoutine() {
        val profile = UserProfile(wakeTime = "06:30", workoutTime = "20:00")

        assertEquals(
            java.time.LocalTime.of(7, 0),
            scheduledMealTime(
                profile,
                FoodTask("breakfast", "Morning", "Breakfast", "", 400, 15),
            ),
        )
        assertEquals(
            java.time.LocalTime.of(21, 0),
            scheduledMealTime(
                profile,
                FoodTask("dinner", "After workout", "Dinner", "", 700, 25),
            ),
        )
    }

    @Test
    fun weeklyCheckInMovesFromUpcomingToDueAndComplete() {
        val profile = UserProfile(
            weighInDay = DayOfWeek.WEDNESDAY,
            weighInTime = "08:00",
        )
        assertEquals(
            WeeklyWeighInStatus.UPCOMING,
            weeklyWeighInStatus(
                profile,
                emptyList(),
                LocalDateTime.of(2026, 7, 20, 9, 0),
            ),
        )
        assertEquals(
            WeeklyWeighInStatus.DUE,
            weeklyWeighInStatus(
                profile,
                emptyList(),
                LocalDateTime.of(2026, 7, 22, 8, 0),
            ),
        )
        assertEquals(
            WeeklyWeighInStatus.COMPLETE,
            weeklyWeighInStatus(
                profile,
                listOf(DaySnapshot(LocalDate.of(2026, 7, 23), weightKg = 60.0)),
                LocalDateTime.of(2026, 7, 23, 9, 0),
            ),
        )
    }

    @Test
    fun weeklyWeightsKeepLatestEntryPerIsoWeek() {
        val result = weeklyWeights(
            listOf(
                DaySnapshot(monday, weightKg = 60.0),
                DaySnapshot(monday.plusDays(2), weightKg = 60.4),
                DaySnapshot(monday.plusDays(7), weightKg = 60.8),
            ),
            monday.plusDays(10),
        )

        assertEquals(2, result.size)
        assertEquals(60.4, result.first().weightKg, 0.0)
        assertEquals(monday.plusDays(2), result.first().loggedOn)
    }

    @Test
    fun shoppingMigrationKeepsOnlyActiveTemplatesAndOptionalPrice() {
        val templates = listOf(
            ShoppingTemplate(
                "rice",
                "Rice",
                "Pantry",
                2.0,
                "kg",
                180.0,
                RecurrenceRule(ShoppingFrequency.WEEKLY),
                monday,
            ),
            ShoppingTemplate(
                "old",
                "Old item",
                "Other",
                1.0,
                "piece",
                0.0,
                RecurrenceRule(ShoppingFrequency.ONE_TIME),
                monday,
                active = false,
            ),
        )

        val result = migrateShoppingTemplates(templates)

        assertEquals(1, result.size)
        assertEquals("2 kg", result.single().quantity)
        assertEquals(180.0, result.single().estimatedPrice ?: 0.0, 0.0)
        assertNull(result.firstOrNull { it.id == "old" })
    }
}
