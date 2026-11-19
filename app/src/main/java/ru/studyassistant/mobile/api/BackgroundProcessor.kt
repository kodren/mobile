package ru.studyassistant.mobile.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.studyassistant.mobile.*
import java.util.UUID

/** POST is never automatically retried. Polling remains cancellable between bounded HTTP calls. */
class BackgroundProcessor(private val api: StudyApi,
    private val wait: suspend (Long) -> Unit = { delay(it) }) {
    suspend fun process(text: String, name: String?, bytes: ByteArray?, mode: ProcessingMode,
                        report: (LongProgress) -> Unit): String {
        val file = name != null && bytes != null
        if (!file) validateMaterial(text)?.let { throw ApiException(it) }
        // A file's decoded length is known only to the server. A length rejection happens before LLM.
        if (file || text.codePointCount(0, text.length) <= 10_000) {
            try {
                return withContext(Dispatchers.IO) {
                    if (file) api.uploadTxt(name!!, bytes!!, mode.name) else api.process(text, mode.name)
                }
            } catch (e: ApiException) {
                val detail = try { JSONObject(e.problem ?: "{}").optString("detail") } catch (_: Exception) { "" }
                if (e.status != 413 || detail !in setOf(
                        "Текст в файле превышает максимальную длину в 10000 символов.",
                        "Максимальная длина текста — 10000 символов.")) throw e
            }
        }
        report(LongProgress())
        var job = try {
            withContext(Dispatchers.IO) {
                if (file) api.requestMultipart("/api/v1/jobs/materials", name!!, bytes!!, mode.name, 202)
                else api.requestObject("POST", "/api/v1/jobs/text",
                    JSONObject().put("text", text).put("mode", mode.name), 202)
            }
        } catch (e: ApiException) {
            if (e.status == 413) throw LongMaterialException(LongMaterialFailure.TOO_LONG)
            throw e
        }
        val id = try { UUID.fromString(job.getString("id")).toString() }
            catch (_: Exception) { throw ApiException(ApiMessages.INVALID_RESPONSE) }
        while (true) {
            val status = job.optString("status")
            val total = job.optInt("totalParts", -1)
            val completed = job.optInt("completedParts", -1)
            if (job.optString("id") != id || job.optString("mode") != mode.name ||
                total !in 2..6 || completed !in 0..total) throw ApiException(ApiMessages.INVALID_RESPONSE)
            when (status) {
                "SUCCEEDED" -> return job.optString("result").takeIf { it.isNotBlank() && it != "null" }
                    ?: throw ApiException(ApiMessages.INVALID_RESPONSE)
                "FAILED" -> {
                    val error = job.optJSONObject("error") ?: throw ApiException(ApiMessages.INVALID_RESPONSE)
                    if (error.optString("code") == "LLM_RATE_LIMIT") throw LongMaterialException(LongMaterialFailure.QUOTA_EXHAUSTED)
                    if (!error.isNull("part")) throw LongMaterialException(LongMaterialFailure.PART_FAILED)
                    throw ApiException("Не удалось завершить обработку. Попробуйте ещё раз позже.")
                }
                "QUEUED", "RUNNING" -> report(LongProgress(completed, total, queued = status == "QUEUED"))
                else -> throw ApiException(ApiMessages.INVALID_RESPONSE)
            }
            wait(job.optInt("_pollSeconds", 3).coerceIn(1, 60) * 1000L)
            job = try { withContext(Dispatchers.IO) { api.requestObject("GET", "/api/v1/jobs/$id") } }
                catch (e: ApiException) {
                    if (e.status == 404) throw LongMaterialException(LongMaterialFailure.EXPIRED)
                    throw e
                }
        }
    }
}
