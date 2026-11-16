package ru.studyassistant.mobile

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LongProcessingTest {
    @Test fun reportsProgressThenResult() = runBlocking {
        val gate = CompletableDeferred<String>()
        val controller = LongProcessingController(this)
        controller.start { report -> report(LongProgress(1, 6, 150, false)); gate.await() }
        yield()
        assertTrue(controller.state.value.running)
        assertEquals(1, controller.state.value.progress.completedParts)
        controller.start { error("Повторный запуск недопустим") }
        gate.complete("Ответ")
        yield()
        assertEquals("Ответ", controller.state.value.result)
        assertFalse(controller.state.value.running)
    }

    @Test fun cancelIgnoresLateProgressAndResponse() = runBlocking {
        val gate = CompletableDeferred<String>()
        val controller = LongProcessingController(this)
        controller.start { report -> withContext(NonCancellable) {
            val result = gate.await()
            report(LongProgress(5, 6, 30, false))
            result
        } }
        yield()
        controller.cancel()
        gate.complete("Поздний ответ")
        yield()
        assertTrue(controller.state.value.cancelled)
        assertNull(controller.state.value.result)
        assertEquals(LongProgress(), controller.state.value.progress)
    }

    @Test fun retriesOnlyAfterUserAction() = runBlocking {
        val controller = LongProcessingController(this)
        var calls = 0
        controller.start { if (++calls == 1) error("Ошибка сети") else "Ответ" }
        yield()
        assertEquals(1, calls)
        assertTrue(controller.state.value.canRetry)
        controller.retry()
        yield()
        assertEquals(2, calls)
        assertEquals("Ответ", controller.state.value.result)
    }
}
