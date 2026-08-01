package com.gaintrack.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ShoppingRecurrenceTest {
    private val monday = LocalDate.of(2026, 7, 20)

    @Test
    fun dailySelectedWeekdayWeeklyMonthlyAndOneTimeRulesAreCalculated() {
        assertTrue(template(ShoppingFrequency.DAILY).isScheduledOn(monday.plusDays(3)))

        val selected = template(
            ShoppingFrequency.SELECTED_WEEKDAYS,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
        )
        assertTrue(selected.isScheduledOn(monday))
        assertTrue(selected.isScheduledOn(monday.plusDays(4)))
        assertFalse(selected.isScheduledOn(monday.plusDays(1)))

        val weekly = template(
            ShoppingFrequency.WEEKLY,
            weekdays = setOf(DayOfWeek.SATURDAY),
        )
        assertTrue(weekly.isScheduledOn(monday.plusDays(5)))
        assertFalse(weekly.isScheduledOn(monday.plusDays(6)))

        val monthly = template(ShoppingFrequency.MONTHLY, dayOfMonth = 31)
        assertTrue(monthly.isScheduledOn(LocalDate.of(2026, 9, 30)))
        assertFalse(monthly.isScheduledOn(LocalDate.of(2026, 9, 29)))

        val oneTime = template(ShoppingFrequency.ONE_TIME)
        assertTrue(oneTime.isScheduledOn(monday))
        assertFalse(oneTime.isScheduledOn(monday.plusDays(1)))
    }

    @Test
    fun pausedAndNotYetStartedItemsAreNotScheduled() {
        assertFalse(template(ShoppingFrequency.DAILY, active = false).isScheduledOn(monday))
        assertFalse(
            template(
                ShoppingFrequency.DAILY,
                startDate = monday.plusDays(1),
            ).isScheduledOn(monday)
        )
    }

    @Test
    fun purchasedAndSkippedItemsAreRemovedFromDueForThatDate() {
        val daily = template(ShoppingFrequency.DAILY)
        val purchased = PurchaseLog(
            id = 1,
            templateId = daily.id,
            itemName = daily.name,
            category = daily.category,
            actualQuantity = 1.0,
            unit = daily.unit,
            amount = 10.0,
            purchaseDate = monday,
        )
        val skipped = purchased.copy(id = 2, status = PurchaseStatus.SKIPPED)

        assertTrue(AppState(shoppingTemplates = listOf(daily)).dueShopping(monday).isNotEmpty())
        assertTrue(
            AppState(
                shoppingTemplates = listOf(daily),
                purchaseLogs = listOf(purchased),
            ).dueShopping(monday).isEmpty()
        )
        assertTrue(
            AppState(
                shoppingTemplates = listOf(daily),
                purchaseLogs = listOf(skipped),
            ).dueShopping(monday).isEmpty()
        )
        assertTrue(
            AppState(
                shoppingTemplates = listOf(daily),
                purchaseLogs = listOf(purchased),
            ).dueShopping(monday.plusDays(1)).isNotEmpty()
        )
    }

    @Test
    fun templateEditsDoNotChangeHistoricalSnapshot() {
        val original = template(ShoppingFrequency.DAILY)
        val history = PurchaseLog(
            id = 8,
            templateId = original.id,
            itemName = original.name,
            category = original.category,
            actualQuantity = original.defaultQuantity,
            unit = original.unit,
            amount = original.estimatedPrice,
            purchaseDate = monday,
        )
        val edited = original.copy(name = "Plantain", defaultQuantity = 4.0, estimatedPrice = 50.0)

        assertEquals("Banana", history.itemName)
        assertEquals(1.0, history.actualQuantity, 0.0)
        assertEquals(10.0, history.amount, 0.0)
        assertEquals("Plantain", edited.name)
    }

    @Test
    fun legacyExpensesMigrateWithoutLosingIdentityDateQuantityOrAmount() {
        val legacy = Expense(42, "2026-07-18", "Peanuts", "2 kg", 340.0)
        val migrated = migrateLegacyExpenses(listOf(legacy)).single()

        assertEquals(42, migrated.id)
        assertEquals("Peanuts", migrated.itemName)
        assertEquals(LocalDate.of(2026, 7, 18), migrated.purchaseDate)
        assertEquals(2.0, migrated.actualQuantity, 0.0)
        assertEquals("kg", migrated.unit)
        assertEquals(340.0, migrated.amount, 0.0)
    }

    private fun template(
        frequency: ShoppingFrequency,
        weekdays: Set<DayOfWeek> = emptySet(),
        dayOfMonth: Int? = null,
        active: Boolean = true,
        startDate: LocalDate = monday,
    ) = ShoppingTemplate(
        id = "banana",
        name = "Banana",
        category = "Fruit",
        defaultQuantity = 1.0,
        unit = "piece",
        estimatedPrice = 10.0,
        recurrence = RecurrenceRule(frequency, weekdays, dayOfMonth),
        startDate = startDate,
        active = active,
    )
}
