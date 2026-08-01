package com.gaintrack.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PersonalizationTest {
    private val start = LocalDate.of(2026, 7, 20)

    @Test
    fun onboardingProfileBuildsBoundedPersonalizedPlan() {
        val profile = UserProfile(
            age = 30,
            gender = Gender.FEMALE,
            heightCm = 168.0,
            currentWeightKg = 58.0,
            targetWeightKg = 64.0,
            goal = PrimaryGoal.GAIN,
            trainingDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
            meditationMinutes = 7,
        )

        val plan = profile.toPlanVersion(start, id = "test")

        assertTrue(plan.calorieTarget in 1400..4200)
        assertTrue(plan.proteinTarget in 55..240)
        assertEquals(profile.trainingDays, plan.trainingDays)
        assertEquals(7, plan.meditationMinutes)
        assertEquals(start.plusMonths(6), plan.targetDate)
    }

    @Test
    fun planVersionsApplyByEffectiveDateWithoutChangingOlderDates() {
        val older = UserProfile(
            currentWeightKg = 54.0,
            targetWeightKg = 62.0,
            trainingDays = setOf(DayOfWeek.MONDAY),
        ).toPlanVersion(start, id = "older")
        val newer = UserProfile(
            currentWeightKg = 56.0,
            targetWeightKg = 66.0,
            trainingDays = setOf(DayOfWeek.TUESDAY),
        ).toPlanVersion(start.plusMonths(1), id = "newer")
        val state = AppState(planVersions = listOf(older, newer))

        assertEquals("older", state.activePlanFor(start.plusDays(5))?.id)
        assertEquals("newer", state.activePlanFor(start.plusMonths(1))?.id)
        assertTrue(state.workoutFor(start) != null)
        assertTrue(state.workoutFor(start.plusDays(1)) == null)
        assertTrue(state.workoutFor(LocalDate.of(2026, 8, 25)) != null)
    }

    @Test
    fun selectedGoalDurationControlsTargetDate() {
        val plan = UserProfile(
            currentWeightKg = 58.0,
            targetWeightKg = 68.0,
            goalDurationMonths = 12,
        ).toPlanVersion(start, id = "twelve-months")

        assertEquals(start.plusMonths(12), plan.targetDate)
    }

    @Test
    fun twoMonthGoalBuildsTwoMonthTargetPath() {
        val plan = UserProfile(
            currentWeightKg = 40.0,
            targetWeightKg = 44.0,
            goalDurationMonths = 2,
        ).toPlanVersion(start, id = "two-months")

        assertEquals(start.plusMonths(2), plan.targetDate)
        assertEquals(40.0, plan.startWeightKg, 0.0)
        assertEquals(44.0, plan.targetWeightKg, 0.0)
    }

    @Test
    fun dietAndTargetsAreAppliedToGeneratedMeals() {
        val plan = UserProfile(
            diet = DietPreference.VEGAN,
            currentWeightKg = 60.0,
            targetWeightKg = 66.0,
        ).toPlanVersion(start, id = "vegan")
        val state = AppState(planVersions = listOf(plan))
        val meals = state.foodFor(start)

        assertTrue(meals.none { it.title.contains("egg", ignoreCase = true) })
        assertTrue(kotlin.math.abs(meals.sumOf { it.calories } - plan.calorieTarget) < meals.size)
        assertTrue(kotlin.math.abs(meals.sumOf { it.protein } - plan.proteinTarget) < meals.size)
    }

    @Test
    fun everyPersonalizedExerciseHasLearningAndSafetyContext() {
        val plan = UserProfile(
            trainingDays = setOf(DayOfWeek.MONDAY),
        ).toPlanVersion(start, id = "learning")
        val exercises = AppState(planVersions = listOf(plan))
            .workoutFor(start)
            ?.exercises
            .orEmpty()

        assertTrue(exercises.isNotEmpty())
        exercises.forEach {
            assertTrue(it.learnUrl.startsWith("https://www.youtube.com/"))
            assertTrue(it.commonMistake.isNotBlank())
            assertTrue(it.muscles.isNotBlank())
            assertNotEquals("", it.equipment)
        }
    }
}
