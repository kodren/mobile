package ru.studyassistant.mobile

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Внутренняя модель UI, не формат HTTP. Адаптер контракта добавляется в #49. */
data class LongProgress(
    val completedParts: Int = 0,
    val totalParts: Int? = null,
    val remainingSeconds: Int? = null,
    val queued: Boolean = true
) {
    init {
        require(completedParts >= 0)
        require(totalParts == null || totalParts > 0 && completedParts <= totalParts)
        require(remainingSeconds == null || remainingSeconds >= 0)
    }
}

data class LongProcessingState(
    val running: Boolean = false,
    val progress: LongProgress = LongProgress(),
    val result: String? = null,
    val error: String? = null,
    val canRetry: Boolean = false,
    val cancelled: Boolean = false
)

/** Держать в ViewModel: поворот и уход в фон не отменяют ожидание. */
class LongProcessingController(private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(LongProcessingState())
    val state = mutableState.asStateFlow()
    private var generation = 0
    private var job: Job? = null
    private var lastOperation: (suspend ((LongProgress) -> Unit) -> String)? = null

    // Вызывать из UI-потока. Отчёты адаптера также должны приходить в этот поток.
    fun start(operation: suspend ((LongProgress) -> Unit) -> String) {
        if (state.value.running) return
        lastOperation = operation
        val token = ++generation
        mutableState.value = LongProcessingState(running = true)
        job = scope.launch {
            try {
                val result = operation { progress ->
                    if (generation == token) mutableState.value = state.value.copy(progress = progress)
                }
                check(result.isNotBlank())
                if (generation == token) mutableState.value = state.value.copy(running = false, result = result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: LongMaterialException) {
                val presentation = e.reason.presentation()
                if (generation == token) mutableState.value = state.value.copy(
                    running = false, error = presentation.message, canRetry = presentation.canRetry)
            } catch (e: Exception) {
                if (generation == token) mutableState.value = state.value.copy(
                    running = false, error = "Не удалось получить результат. Попробуйте снова.", canRetry = true)
            }
        }
    }

    fun retry() { if (state.value.canRetry) lastOperation?.let { start(it) } }

    fun cancel() {
        generation++
        job?.cancel()
        lastOperation = null
        mutableState.value = LongProcessingState(cancelled = true)
    }
}
