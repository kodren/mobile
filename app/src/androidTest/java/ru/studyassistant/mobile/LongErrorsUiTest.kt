package ru.studyassistant.mobile

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class LongErrorsUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun displaysAllErrorsAndRetryPolicy() {
        var reason by mutableStateOf(LongMaterialFailure.TOO_LONG)
        var retries = 0
        compose.setContent { AIStudyAssistantTheme {
            val presentation = reason.presentation()
            LongProgressContent(LongProcessingState(error = presentation.message, canRetry = presentation.canRetry),
                {}, { retries++ }, materialName = "Лекция_ИИ.txt")
        } }
        for (item in LongMaterialFailure.entries) {
            compose.runOnIdle { reason = item }
            compose.onNodeWithText(item.presentation().message).assertExists()
            if (item.presentation().canRetry) compose.onNodeWithText("Повторить").performScrollTo().performClick()
            else compose.onNodeWithText("Повторить").assertDoesNotExist()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.getExternalFilesDir(null), "long-error-${item.name}.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        compose.runOnIdle { check(retries == 2) }
    }
}
