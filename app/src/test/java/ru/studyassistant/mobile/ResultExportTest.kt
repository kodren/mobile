package ru.studyassistant.mobile

import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultExportTest {

    private val fixedDate = requireNotNull(
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-11-18")
    )

    @Test
    fun buildShareTextCombinesModeTitleAndResult() {
        val shareText = buildShareText(
            modeTitle = " Краткий конспект ",
            result = " Фотосинтез — процесс преобразования света. "
        )

        assertEquals(
            "Краткий конспект\n\nФотосинтез — процесс преобразования света.",
            shareText
        )
    }

    @Test
    fun buildShareTextHandlesBlankModeTitle() {
        val shareText = buildShareText(
            modeTitle = "   ",
            result = "  Только текст результата  "
        )

        assertEquals("Только текст результата", shareText)
    }

    @Test
    fun buildExportFileNameUsesModeDateAndTxtExtension() {
        val fileName = buildExportFileName(
            modeTitle = "Краткий конспект",
            extension = ExportFormat.TXT.extension,
            date = fixedDate
        )

        assertEquals("Краткий_конспект_2026-11-18.txt", fileName)
    }

    @Test
    fun buildExportFileNameUsesMarkdownExtensionAndRemovesForbiddenChars() {
        val fileName = buildExportFileName(
            modeTitle = "План: экзамен/тест?",
            extension = ExportFormat.MARKDOWN.extension,
            date = fixedDate
        )

        assertEquals("План_экзаментест_2026-11-18.md", fileName)
    }

    @Test
    fun buildExportFileNameFallsBackToDefaultWhenModeIsBlank() {
        val fileName = buildExportFileName(
            modeTitle = "   ",
            extension = ExportFormat.TXT.extension,
            date = fixedDate
        )

        assertEquals("result_2026-11-18.txt", fileName)
    }

    @Test
    fun exportFormatsProvideExpectedExtensionsAndMimeTypes() {
        assertEquals("txt", ExportFormat.TXT.extension)
        assertEquals("text/plain", ExportFormat.TXT.mimeType)
        assertEquals("md", ExportFormat.MARKDOWN.extension)
        assertEquals("text/markdown", ExportFormat.MARKDOWN.mimeType)
    }

    @Test
    fun formatExportContentBuildsPlainTextForTxt() {
        val content = formatExportContent(
            modeTitle = "Краткий конспект",
            result = "Основная идея текста.",
            format = ExportFormat.TXT
        )

        assertFalse(content.startsWith("# "))
        assertEquals("Краткий конспект\n\nОсновная идея текста.", content)
    }

    @Test
    fun formatExportContentBuildsMarkdownHeadingForMd() {
        val content = formatExportContent(
            modeTitle = "Краткий конспект",
            result = "Основная идея текста.",
            format = ExportFormat.MARKDOWN
        )

        assertTrue(content.startsWith("# Краткий конспект"))
        assertEquals("# Краткий конспект\n\nОсновная идея текста.", content)
    }

    @Test
    fun encodeExportUtf8PreservesRussianTextAndSpecialCharacters() {
        val original = "# Краткий конспект\n\nФотосинтез — энергия для растений (ё, Й, №1)."
        val bytes = encodeExportUtf8(original)

        assertArrayEquals(original.toByteArray(Charsets.UTF_8), bytes)
        assertEquals(original, String(bytes, Charsets.UTF_8))
        assertTrue(bytes.size > original.length)
    }
}