package com.gaintrack.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SeedPlanTest {
    @Test
    fun workoutScheduleMatchesFourDayPlan() {
        val monday = LocalDate.of(2026, 7, 20)
        assertEquals("Upper Body A", SeedPlan.workoutFor(monday)?.name)
        assertNull(SeedPlan.workoutFor(monday.plusDays(1)))
        assertEquals("Lower Body A", SeedPlan.workoutFor(monday.plusDays(2))?.name)
        assertEquals("Upper Body B", SeedPlan.workoutFor(monday.plusDays(4))?.name)
        assertEquals("Lower Body B", SeedPlan.workoutFor(monday.plusDays(6))?.name)
    }

    @Test
    fun introductorySetsProgressCorrectly() {
        val exercise = SeedPlan.workoutFor(SeedPlan.planStart)!!.exercises.first()
        assertEquals(2, SeedPlan.prescribedSets(SeedPlan.planStart, exercise))
        assertEquals(3, SeedPlan.prescribedSets(SeedPlan.planStart.plusWeeks(1), exercise))
        assertEquals(4, SeedPlan.prescribedSets(SeedPlan.planStart.plusWeeks(2), exercise))
    }

    @Test
    fun targetMovesFromStartToFinalWeight() {
        assertEquals(SeedPlan.startWeight, SeedPlan.targetFor(SeedPlan.planStart), 0.01)
        assertEquals(SeedPlan.targetWeight, SeedPlan.targetFor(SeedPlan.planEnd), 0.01)
        assertTrue(SeedPlan.targetFor(SeedPlan.planStart.plusMonths(3)) > SeedPlan.startWeight)
    }

    @Test
    fun mealPlanMeetsEstimatedRange() {
        val monday = LocalDate.of(2026, 7, 20)
        val food = SeedPlan.foodFor(monday)
        assertTrue(food.sumOf { it.calories } in 2400..3000)
        assertTrue(food.sumOf { it.protein } >= 90)
        assertNotNull(food.firstOrNull { it.title.contains("eggs", ignoreCase = true) })
    }
}
