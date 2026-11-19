package ru.studyassistant.mobile.api

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import ru.studyassistant.mobile.*

class BackgroundProcessorTest {
    private val id = "12345678-1234-1234-1234-123456789abc"
    private fun job(status: String, extra: String = "") = """{"id":"$id","mode":"SHORT_SUMMARY","status":"$status","totalParts":3,"completedParts":1$extra}"""
    private fun server(block: (String) -> Pair<Int, String>, test: (StudyApi) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.requestBody.use { it.readBytes() }
            val (status, body) = block(exchange.requestURI.path)
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Retry-After", "3")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try { test(StudyApi("http://127.0.0.1:${server.address.port}")) } finally { server.stop(0) }
    }
    @Test fun shortTextUsesOnlySync() {
        val paths = mutableListOf<String>()
        server({ paths += it; 200 to """{"mode":"SHORT_SUMMARY","result":"Ответ"}""" }) { api -> runBlocking {
            assertEquals("Ответ", BackgroundProcessor(api).process("Текст", null, null, ProcessingMode.SHORT_SUMMARY) { error("No progress") })
        } }
        assertEquals(listOf("/api/v1/text/process"), paths)
    }
    @Test fun longTextPollsAndUsesRetryAfter() {
        var polls = 0; val waits = mutableListOf<Long>(); val progress = mutableListOf<LongProgress>()
        server({ if (it.endsWith("/text")) 202 to job("QUEUED") else {
            polls++; 200 to if (polls == 1) job("RUNNING") else job("SUCCEEDED", """, "result":"Готово"""")
        } }) { api -> runBlocking {
            assertEquals("Готово", BackgroundProcessor(api) { waits += it }.process("а".repeat(12000), null, null, ProcessingMode.SHORT_SUMMARY, progress::add))
        } }
        assertEquals(listOf(3000L, 3000L), waits)
        assertTrue(progress.any { !it.queued && it.completedParts == 1 })
    }
    @Test fun longFileFallsBackOnlyForDecodedLength() {
        val paths = mutableListOf<String>()
        server({ paths += it; when (it) {
            "/api/v1/materials/process" -> 413 to """{"detail":"Текст в файле превышает максимальную длину в 10000 символов."}"""
            "/api/v1/jobs/materials" -> 202 to job("QUEUED")
            else -> 200 to job("SUCCEEDED", """, "result":"Файл обработан"""")
        } }) { api -> runBlocking {
            assertEquals("Файл обработан", BackgroundProcessor(api) {}.process("", "Лекция.pdf", byteArrayOf(1), ProcessingMode.SHORT_SUMMARY) {})
        } }
        assertEquals(3, paths.size)
    }
    @Test fun fileSizeRejectionNeverCreatesJob() {
        var calls = 0
        server({ calls++; 413 to """{"detail":"Размер файла превышает максимальный лимит в 10 МБ."}""" }) { api ->
            assertThrows(ApiException::class.java) { runBlocking { BackgroundProcessor(api).process("", "file.txt", byteArrayOf(1), ProcessingMode.SHORT_SUMMARY) {} } }
        }
        assertEquals(1, calls)
    }
    @Test fun expiredAndQuotaAreMapped() {
        for (expired in listOf(true, false)) server({
            if (it.endsWith("/text")) 202 to job("QUEUED")
            else if (expired) 404 to "{}"
            else 200 to job("FAILED", """, "error":{"code":"LLM_RATE_LIMIT","status":429,"detail":"private"}""")
        }) { api ->
            val e = assertThrows(LongMaterialException::class.java) { runBlocking {
                BackgroundProcessor(api) {}.process("a".repeat(12000), null, null, ProcessingMode.SHORT_SUMMARY) {}
            } }
            assertEquals(if (expired) LongMaterialFailure.EXPIRED else LongMaterialFailure.QUOTA_EXHAUSTED, e.reason)
        }
    }
    @Test fun cancellationDuringWaitDoesNotPoll() {
        var calls = 0
        server({ calls++; 202 to job("QUEUED") }) { api ->
            assertThrows(CancellationException::class.java) { runBlocking {
                BackgroundProcessor(api) { throw CancellationException() }.process("a".repeat(12000), null, null, ProcessingMode.SHORT_SUMMARY) {}
            } }
        }
        assertEquals(1, calls)
    }
}
