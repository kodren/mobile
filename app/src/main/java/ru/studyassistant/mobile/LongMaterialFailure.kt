package ru.studyassistant.mobile

/** Доменные причины; HTTP-коды/поля необходимо сопоставить после backend#43. */
enum class LongMaterialFailure {
    TOO_LONG, PART_FAILED, QUOTA_EXHAUSTED, EXPIRED
}

data class LongFailurePresentation(val message: String, val canRetry: Boolean)

fun LongMaterialFailure.presentation(): LongFailurePresentation = when (this) {
    LongMaterialFailure.TOO_LONG -> LongFailurePresentation(
        "Материал слишком длинный. Попробуйте выбрать более короткий файл или разделить его на части.", false)
    LongMaterialFailure.PART_FAILED -> LongFailurePresentation(
        "Ошибка обработки части. Что-то пошло не так. Попробуйте ещё раз.", true)
    LongMaterialFailure.QUOTA_EXHAUSTED -> LongFailurePresentation(
        "Лимит исчерпан. Вы достигли лимита по количеству запросов. Попробуйте позже.", false)
    LongMaterialFailure.EXPIRED -> LongFailurePresentation(
        "Задание устарело. Отправьте материал на обработку ещё раз.", true)
}

class LongMaterialException(val reason: LongMaterialFailure) : Exception()
