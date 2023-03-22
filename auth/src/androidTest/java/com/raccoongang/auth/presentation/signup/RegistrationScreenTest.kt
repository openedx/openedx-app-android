package com.raccoongang.auth.presentation.signup

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.raccoongang.auth.R
import com.raccoongang.core.domain.model.RegistrationField
import com.raccoongang.core.domain.model.RegistrationFieldType
import com.raccoongang.core.ui.WindowSize
import com.raccoongang.core.ui.WindowType
import org.junit.Rule
import org.junit.Test

class RegistrationScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    //region mockField
    private val option = RegistrationField.Option("def", "Bachelor", "Android")

    private val mockField = RegistrationField(
        "Fullname",
        "Fullname",
        RegistrationFieldType.TEXT,
        "Fullname",
        instructions = "Enter your fullname",
        exposed = false,
        required = true,
        restrictions = RegistrationField.Restrictions(),
        options = listOf(option, option),
        errorInstructions = ""
    )
    //endregion


    @Test
    fun signUpLoadingFields() {
        composeTestRule.setContent {
            RegistrationScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                uiState = SignUpUIState.Loading,
                uiMessage = null,
                isButtonClicked = false,
                validationError = false,
                onBackClick = {},
                onRegisterClick = {}
            )
        }
        with(composeTestRule) {
            onRoot().printToLog("ROOT_TAG")

            onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0f, 0f..0f))).assertExists()
            onNode(hasScrollAction().and(hasAnyChild(hasText(activity.getString(R.string.auth_sign_up))))).assertDoesNotExist()
        }
    }

    @Test
    fun signUpNoOptionalFields() {
        composeTestRule.setContent {
            RegistrationScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                uiState = SignUpUIState.Fields(
                    fields = listOf(
                        mockField,
                        mockField.copy(name = "Age", label = "Age", errorInstructions = "error")
                    ),
                    optionalFields = emptyList()
                ),
                uiMessage = null,
                isButtonClicked = false,
                validationError = false,
                onBackClick = {},
                onRegisterClick = {}
            )
        }
        with(composeTestRule) {
            onNode(hasText("Fullname").and(hasSetTextAction())).assertExists()

            onNode(hasText("Age").and(hasSetTextAction())).assertExists()

            onNodeWithText(activity.getString(R.string.auth_show_optional_fields)).assertDoesNotExist()

            onNode(hasText(activity.getString(R.string.auth_create_account)).and(hasClickAction())).assertExists()
        }
    }

    @Test
    fun signUpHasOptionalFields() {
        composeTestRule.setContent {
            RegistrationScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                uiState = SignUpUIState.Fields(
                    fields = listOf(
                        mockField,
                        mockField.copy(name = "Age", label = "Age", errorInstructions = "error")
                    ),
                    optionalFields = listOf(mockField)
                ),
                uiMessage = null,
                isButtonClicked = false,
                validationError = false,
                onBackClick = {},
                onRegisterClick = {}
            )
        }
        with(composeTestRule) {
            onNode(hasText("Age").and(hasSetTextAction())).assertExists()

            onNodeWithText(activity.getString(R.string.auth_show_optional_fields)).assertExists()

            onNode(hasText(activity.getString(R.string.auth_create_account)).and(hasClickAction())).assertExists()
        }
    }

    @Test
    fun signUpFieldsWithError() {
        composeTestRule.setContent {
            RegistrationScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                uiState = SignUpUIState.Fields(
                    fields = listOf(
                        mockField,
                        mockField.copy(name = "Age", label = "Age", errorInstructions = "error")
                    ),
                    optionalFields = emptyList()
                ),
                uiMessage = null,
                isButtonClicked = false,
                validationError = false,
                onBackClick = {},
                onRegisterClick = {}
            )
        }
        with(composeTestRule) {
            onNode(hasText("error")).assertExists()
        }
    }

    @Test
    fun signUpCreateAccountClicked() {
        composeTestRule.setContent {
            RegistrationScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                uiState = SignUpUIState.Fields(
                    fields = listOf(
                        mockField,
                        mockField.copy(name = "Age", label = "Age", errorInstructions = "error")
                    ),
                    optionalFields = listOf(mockField)
                ),
                uiMessage = null,
                isButtonClicked = true,
                validationError = false,
                onBackClick = {},
                onRegisterClick = {}
            )
        }
        with(composeTestRule) {
            onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0f, 0f..0f))).assertExists()
            onNode(hasText(activity.getString(R.string.auth_create_account)).and(hasClickAction())).assertDoesNotExist()
        }
    }
}