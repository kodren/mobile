package ru.studyassistant.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class LongProgressDemoModel : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val controller = LongProcessingController(scope)
    fun start() = controller.start { report ->
        report(LongProgress())
        delay(3_000)
        repeat(6) { index ->
            report(LongProgress(index, 6, (6 - index) * 30, queued = false))
            delay(30_000)
        }
        "Тестовая обработка завершена. Это демонстрационные данные, не ответ сервера."
    }
    override fun onCleared() { scope.cancel() }
}

/** Только debug: запуск через adb, никогда не выдаёт тестовые данные за ответ LLM. */
class LongProgressDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[LongProgressDemoModel::class.java]
        setContent { AIStudyAssistantTheme {
            val state by model.controller.state.collectAsState()
            BackHandler(state.running) { model.controller.cancel() }
            Scaffold { padding -> Column(Modifier.padding(padding).fillMaxSize()) {
                Text("ДЕМО · тестовые данные")
                if (state.result != null) ResultContent(state.result!!, "Краткий конспект",
                    { model.controller.cancel() }, { model.controller.cancel() }, Modifier.weight(1f))
                else LongProgressContent(state, model.controller::cancel, model.controller::retry, Modifier.weight(1f))
                if (!state.running) Button(onClick = model::start) { Text("Запустить демо") }
            } }
        } }
    }
}
