package ru.studyassistant.mobile

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class BackgroundFlowUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun normalFlowShowsProgressAndCanCancel() {
        lateinit var flow: StudyFlow
        compose.setContent {
            val scope = rememberCoroutineScope()
            flow = remember { StudyFlow(scope, { _, _ -> "Короткий ответ" }, { "ok" },
                processMaterial = { _, _, _, _, report ->
                    report(LongProgress(2, 5, queued = false)); delay(60_000); "Готово"
                }) }
            AIStudyAssistantTheme { StudyApp(flow) }
        }
        compose.runOnIdle { flow.editMaterial("а".repeat(12000)); flow.showModes(); flow.submit() }
        compose.onNodeWithText("Часть 3 из 5").assertIsDisplayed()
        compose.onNodeWithText("Отменить").performScrollTo().assertIsDisplayed()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "background-flow.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Отменить").performClick()
        compose.onNodeWithText("Создать").performScrollTo().assertIsEnabled()
    }
}
