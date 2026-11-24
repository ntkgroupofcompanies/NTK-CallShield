package com.ntk.callshield

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CallShieldUiSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun finishOnboardingViaDemoOrSteps() {
        composeRule.waitForIdle()
        val hasWelcome = runCatching {
            composeRule.onNodeWithTag("onboarding_start_setup_btn").assertExists()
            true
        }.getOrDefault(false)
        if (hasWelcome) {
            composeRule.onNodeWithTag("onboarding_start_setup_btn").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("onboarding_skip_permissions_btn").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("onboarding_finish_btn").performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 8_000) {
                runCatching {
                    composeRule.onNodeWithTag("nav_home").assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun `3-step onboarding completes and 5-tab navigation traverses all tabs`() {
        finishOnboardingViaDemoOrSteps()

        composeRule.onNodeWithText("NTK CallShield").assertIsDisplayed()
        composeRule.onNodeWithText("NTK CallShield is Active").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_calls").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Recent Calls").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_check").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Scam & Caller Check").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_contacts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Stored only on your phone · Never uploaded").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Quick Preferences").assertIsDisplayed()
    }

    @Test
    fun `reverse phone lookup displays report and bank safety tip for Chase`() {
        finishOnboardingViaDemoOrSteps()

        composeRule.onNodeWithTag("nav_check").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("reverse_lookup_input").performTextInput("+18009359935")
        composeRule.onNodeWithTag("reverse_lookup_button").performClick()

        composeRule.waitUntil(timeoutMillis = 8_000) {
            runCatching {
                composeRule.onNodeWithTag("reverse_lookup_list")
                    .performScrollToNode(hasTestTag("intelligence_report_card"))
                composeRule.onNodeWithTag("intelligence_report_card").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Chase Bank Fraud Desk").assertIsDisplayed()
        composeRule.onNodeWithTag("bank_safety_tip_banner").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `test lab runs scam robocall simulation and shows technical telemetry accordion`() {
        finishOnboardingViaDemoOrSteps()

        composeRule.waitUntil(timeoutMillis = 8_000) {
            runCatching {
                composeRule.onNodeWithTag("home_list")
                    .performScrollToNode(hasTestTag("quick_test_shield"))
                composeRule.onNodeWithTag("quick_test_shield").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("quick_test_shield").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Test My Shield").assertIsDisplayed()

        composeRule.onNodeWithTag("test_lab_list")
            .performScrollToNode(hasTestTag("run_sim_scam_robocall"))
        composeRule.onNodeWithTag("run_sim_scam_robocall").performClick()

        composeRule.waitUntil(timeoutMillis = 8_000) {
            runCatching {
                composeRule.onNodeWithTag("test_lab_list")
                    .performScrollToNode(hasTestTag("tech_telemetry_accordion"))
                composeRule.onNodeWithTag("tech_telemetry_accordion").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("tech_telemetry_accordion").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Technical Telemetry Details").assertIsDisplayed()
    }
}
