package com.example

import android.app.Application
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ExamViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pembelajaran dengan AI", appName)
    }

    @Test
    fun `dark mode toggle works on LoginScreen and SMK shows numeric class and kejurusan`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ExamViewModel(app)
        assertFalse(viewModel.isDarkTheme.value)

        composeTestRule.setContent {
            val isDark by viewModel.isDarkTheme.collectAsState()
            MyApplicationTheme(darkTheme = isDark) {
                LoginScreen(viewModel = viewModel, onLoginSuccess = {})
            }
        }

        // Verify dark mode toggle actually responds to clicks
        composeTestRule.onNodeWithTag("theme_toggle_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(viewModel.isDarkTheme.value)

        composeTestRule.onNodeWithTag("theme_toggle_button").performClick()
        composeTestRule.waitForIdle()
        assertFalse(viewModel.isDarkTheme.value)

        // Verify horizontal SD, SMP, SMA, SMK options and numeric class labels for SMK
        composeTestRule.onNodeWithTag("school_option_sd").assertIsDisplayed()
        composeTestRule.onNodeWithTag("school_option_smp").assertIsDisplayed()
        composeTestRule.onNodeWithTag("school_option_sma").assertIsDisplayed()
        composeTestRule.onNodeWithTag("school_option_smk").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("class_option_10").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        composeTestRule.onNodeWithTag("class_option_11").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("class_option_12").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("subject_option_Rekayasa Perangkat Lunak (RPL)")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
