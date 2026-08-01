package com.gaintrack.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AnalyticsTest {
    private val today = LocalDate.of(2026, 7, 26)

    @Test
    fun noDataProducesSafeZeroMetrics() {
        val result = Analytics.analyze(AppState(), AnalyticsRange.MONTH, today)

        assertEquals(0, result.weeklyHealthScore)
        assertEquals(0, result.foodAdherence)
        assertEquals(0.0, result.monthSpend, 0.0)
        assertNull(result.currentWeight)
        assertNull(result.projectedGoalDate)
    }

    @Test
    fun completeWeekProducesFullWeightedHealthScore() {
        val start = today.minusDays(6)
        val snapshots = buildMap {
            repeat(7) { offset ->
                val date = start.plusDays(offset.toLong())
                val food = SeedPlan.foodFor(date)
                put(
                    date.toString(),
                    DaySnapshot(
                        date = date,
                        completedFood = food.mapTo(mutableSetOf()) { it.id },
                        workoutDone = SeedPlan.workoutFor(date) != null,
                        sleepHours = 7.5,
                        weightKg = if (offset == 5) 55.5 else null,
                    )
                )
            }
        }

        val result = Analytics.analyze(
            AppState(snapshots = snapshots),
            AnalyticsRange.MONTH,
            today,
        )

        assertEquals(100, result.weeklyHealthScore)
        assertEquals(100, result.weightLogAdherence)
        assertTrue(result.averageSleep >= 7.0)
    }

    @Test
    fun purchaseAnalyticsUseSnapshotsAndRespectMonthlyBudget() {
        val purchases = listOf(
            PurchaseLog(1, "egg", "Eggs", "Protein", 12.0, "eggs", 100.0, today),
            PurchaseLog(2, "banana", "Banana", "Fruit", 6.0, "piece", 60.0, today),
            PurchaseLog(
                3,
                "chana",
                "Chana",
                "Pantry",
                1.0,
                "kg",
                0.0,
                today,
                PurchaseStatus.SKIPPED,
            ),
        )
        val result = Analytics.analyze(
            AppState(purchaseLogs = purchases, monthlyBudget = 200.0),
            AnalyticsRange.MONTH,
            today,
        )

        assertEquals(160.0, result.monthSpend, 0.0)
        assertEquals(40.0, result.remainingBudget, 0.0)
        assertEquals(100.0, result.spendingByCategory["Protein"] ?: 0.0, 0.0)
        assertEquals(60.0, result.spendingByItem["Banana"] ?: 0.0, 0.0)
    }
}
