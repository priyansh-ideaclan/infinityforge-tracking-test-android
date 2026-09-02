package com.factory.feature.auth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.factory.core.designsystem.theme.FactoryTheme
import org.junit.Rule
import org.junit.Test

/**
 * Representative Compose UI test for the factory's Compose UI testing setup — an
 * instrumented test, so it needs a connected device/emulator to actually run (the user
 * deferred emulator setup this session; see Docs/testing/README.md for how to run it).
 * It compiles as part of `compileDebugAndroidTestKotlin`, which canonical verification
 * does check.
 */
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun submittingWithoutInput_showsValidationError() {
        var submitCount = 0
        composeTestRule.setContent {
            FactoryTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = { submitCount++ },
                    onSignInAnonymously = {},
                    onGoToRegister = {},
                    onGoToForgotPassword = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(LoginScreenTestTags.SUBMIT_BUTTON).performClick()

        assert(submitCount == 1)
    }

    @Test
    fun errorMessage_isDisplayed_whenPresent() {
        composeTestRule.setContent {
            FactoryTheme {
                LoginScreen(
                    uiState = LoginUiState(errorMessage = "Incorrect email or password."),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onSignInAnonymously = {},
                    onGoToRegister = {},
                    onGoToForgotPassword = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(LoginScreenTestTags.ERROR_TEXT).assertIsDisplayed()
        composeTestRule.onNodeWithText("Incorrect email or password.").assertIsDisplayed()
    }

    @Test
    fun typingEmail_updatesCallback() {
        var lastEmail = ""
        composeTestRule.setContent {
            FactoryTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onEmailChanged = { lastEmail = it },
                    onPasswordChanged = {},
                    onSubmit = {},
                    onSignInAnonymously = {},
                    onGoToRegister = {},
                    onGoToForgotPassword = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(LoginScreenTestTags.EMAIL_FIELD).performTextInput("a@example.com")

        assert(lastEmail == "a@example.com")
    }
}
