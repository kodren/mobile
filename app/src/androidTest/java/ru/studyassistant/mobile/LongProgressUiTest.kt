package ru.studyassistant.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LongProgressUiTest {
    @get:Rule val compose = createAndroidComposeRule<LongProgressDemoActivity>()

    @Test fun waitingSurvivesRecreationAndBackground() {
        compose.onNodeWithText("Запустить демо").performClick()
        compose.waitUntil(10_000) {
            ViewModelProvider(compose.activity)[LongProgressDemoModel::class.java]
                .controller.state.value.progress.totalParts == 6
        }
        val controller = ViewModelProvider(compose.activity)[LongProgressDemoModel::class.java].controller
        compose.activityRule.scenario.recreate()
        assertSame(controller, ViewModelProvider(compose.activity)[LongProgressDemoModel::class.java].controller)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        assertTrue(controller.state.value.running)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.onNodeWithText("Часть 1 из 6").assertExists()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = java.io.File(compose.activity.getExternalFilesDir(null), "long-progress.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithText("Отменить").performScrollTo().performClick()
        assertFalse(controller.state.value.running)
        compose.onNodeWithText("Ожидание отменено. Сервер может продолжать обработку.").assertExists()
    }
}
