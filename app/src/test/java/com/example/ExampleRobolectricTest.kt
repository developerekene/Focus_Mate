package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FocusMate", appName)
    }

    @Test
    fun `timer mode switch updates countdown duration`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("timer_countdown_text", useUnmergedTree = true)
            .assertTextEquals("25:00")

        composeRule.onNodeWithTag("mode_short_break").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("timer_countdown_text", useUnmergedTree = true)
            .assertTextEquals("05:00")

        composeRule.onNodeWithTag("mode_long_break").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("timer_countdown_text", useUnmergedTree = true)
            .assertTextEquals("15:00")

        composeRule.onNodeWithTag("mode_focus").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("timer_countdown_text", useUnmergedTree = true)
            .assertTextEquals("25:00")
    }

    @Test
    fun `bottom navigation switches between timer stats and settings`() {
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_stats").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("stats_screen_title").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_screen_title").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_timer").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start_pause_button").assertExists()
    }
}
