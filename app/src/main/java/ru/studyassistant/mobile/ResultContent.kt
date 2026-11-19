package ru.studyassistant.mobile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import ru.studyassistant.mobile.ui.theme.StudyButton
import ru.studyassistant.mobile.ui.theme.StudySpacing

@Composable
fun ResultContent(
    result: String,
    modeTitle: String,
    onModes: () -> Unit,
    onNewMaterial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copied by remember(result) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(StudySpacing.compact)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                modeTitle,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(StudySpacing.compact)
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("result-scroll")
                .verticalScroll(rememberScrollState())
        ) {
            Text(result, modifier = Modifier.testTag("result-text"))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StudySpacing.compact)
        ) {
            StudyButton(
                onClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Результат", result))
                    copied = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (copied) "Скопировано" else "Копировать",
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            OutlinedButton(
                onClick = { shareResult(context, modeTitle, result) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("share-result-button")
            ) {
                Text("Поделиться")
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StudySpacing.compact)
        ) {
            OutlinedButton(onClick = onModes, modifier = Modifier.weight(1f)) {
                Text("Режимы")
            }
            TextButton(onClick = onNewMaterial, modifier = Modifier.weight(1f)) {
                Text("Новый материал")
            }
        }
    }
}