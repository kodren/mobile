package ru.studyassistant.mobile

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LongMaterialFailureTest {
    @Test fun everyReasonHasActionableMessageAndCorrectRetry() = runBlocking {
        val expected = mapOf(
            LongMaterialFailure.TOO_LONG to ("разделить" to false),
            LongMaterialFailure.PART_FAILED to ("Попробуйте ещё раз" to true),
            LongMaterialFailure.QUOTA_EXHAUSTED to ("Попробуйте позже" to false),
            LongMaterialFailure.EXPIRED to ("Отправьте материал" to true))
        for ((reason, pair) in expected) {
            val controller = LongProcessingController(this)
            controller.start { throw LongMaterialException(reason) }
            yield()
            assertFalse(controller.state.value.running)
            assertTrue(controller.state.value.error!!.contains(pair.first))
            assertEquals(pair.second, controller.state.value.canRetry)
        }
    }

    @Test fun forbiddenRetryDoesNotSendAndExpiredResubmits() = runBlocking {
        for (reason in LongMaterialFailure.entries) {
            val controller = LongProcessingController(this)
            var calls = 0
            controller.start { if (++calls == 1) throw LongMaterialException(reason) else "Новый результат" }
            yield()
            controller.retry()
            yield()
            assertEquals(if (reason.presentation().canRetry) 2 else 1, calls)
        }
    }
}
