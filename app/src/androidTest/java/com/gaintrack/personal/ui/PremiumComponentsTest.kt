package com.gaintrack.personal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class PremiumComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun goalRingAndGroupedSurfaceExposeReadableLabels() {
        composeRule.setContent {
            GainTrackTheme {
                Column {
                    GoalRing(
                        progress = .5f,
                        primaryText = "58.0",
                        secondaryText = "kg now",
                    )
                    SectionCard {
                        Metric("food adherence", "86%")
                    }
                }
            }
        }

        composeRule.onNodeWithText("58.0").assertIsDisplayed()
        composeRule.onNodeWithText("kg now").assertIsDisplayed()
        composeRule.onNodeWithText("86%").assertIsDisplayed()
        composeRule.onNodeWithText("food adherence").assertIsDisplayed()
    }

    @Test
    fun gainTrackThemeAlwaysUsesTheHighContrastLightPalette() {
        var background = Color.Unspecified
        var onBackground = Color.Unspecified

        composeRule.setContent {
            GainTrackTheme {
                background = MaterialTheme.colorScheme.background
                onBackground = MaterialTheme.colorScheme.onBackground
            }
        }

        composeRule.runOnIdle {
            assertEquals(GainBackground, background)
            assertEquals(GainText, onBackground)
        }
    }
}
