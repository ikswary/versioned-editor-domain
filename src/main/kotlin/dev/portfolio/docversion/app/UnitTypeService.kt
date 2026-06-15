package dev.portfolio.docversion.app

import dev.portfolio.docversion.domain.EditorUnit
import dev.portfolio.docversion.domain.EditorUnitType
import java.time.Instant
import java.util.UUID

internal fun splitRaw(raw: String): Pair<String, String> {
    val i = raw.indexOf('|')
    return if (i < 0) "" to raw else raw.substring(0, i) to raw.substring(i + 1)
}

interface UnitTypeService {
    val type: EditorUnitType<*, *>

    fun create(rawContent: String, at: Instant, by: UUID? = null): EditorUnit =
        EditorUnit(type = type, rawContent = rawContent, validFrom = at, createdBy = by)

    fun splitContent(rawContent: String, splitAt: Int): Pair<String, String> {
        val (prefix, payload) = splitRaw(rawContent)
        val i = splitAt.coerceIn(0, payload.length)
        return "$prefix|${payload.substring(0, i)}" to "$prefix|${payload.substring(i)}"
    }

    fun mergeContent(first: String, second: String): String {
        val (prefix, payloadA) = splitRaw(first)
        val payloadB = splitRaw(second).second
        return "$prefix|$payloadA $payloadB"
    }

    fun modifyContent(newRaw: String): String = newRaw
}
