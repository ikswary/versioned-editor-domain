package dev.portfolio.docversion.domain

import java.time.Instant
import java.util.UUID

typealias EditorUnitId = UUID
typealias EditorUnitKey = UUID

data class EditorUnit(
    val id: EditorUnitId = UUID.randomUUID(),
    val key: EditorUnitKey = id,
    val type: EditorUnitType<*, *>,
    val rawContent: String,
    val originEditorUnitIds: Set<EditorUnitId> = setOf(id),
    val validFrom: Instant,
    val validTo: Instant? = null,
    val deleted: Boolean = false,
    val createdBy: UUID? = null,
) : Comparable<EditorUnit> {

    val content: EditorUnitContent<*, *> by lazy { type.parse(rawContent) }

    init {
        require(validTo == null || validFrom < validTo) { "validFrom must be < validTo" }
        content
    }

    fun checkLatest(): Boolean = validTo == null && !deleted

    fun requireLatest() {
        if (!checkLatest()) throw StaleEditorUnitException(id)
    }

    override fun compareTo(other: EditorUnit): Int = content.compareTo(other.content)

    fun endAt(at: Instant): EditorUnit {
        if (at <= validFrom) throw InvalidEditTimeException(at, validFrom)
        return copy(validTo = at)
    }

    fun modify(newRaw: String, at: Instant, by: UUID? = null): VersionChange {
        requireLatest()
        val terminated = endAt(at)
        val next = EditorUnit(
            key = key, type = type, rawContent = newRaw,
            originEditorUnitIds = originEditorUnitIds, validFrom = at, createdBy = by,
        )
        return VersionChange(terminated, listOf(next), EditorUnitEvent.Modified(next.id, key))
    }

    fun merge(other: EditorUnit, newRaw: String, at: Instant, by: UUID? = null): VersionChange {
        if (type != other.type) throw EditorUnitTypeMismatchException(type, other.type)
        requireLatest(); other.requireLatest()
        val terminated = listOf(endAt(at), other.endAt(at))
        val merged = EditorUnit(
            key = key, type = type, rawContent = newRaw,
            originEditorUnitIds = originEditorUnitIds + other.originEditorUnitIds,
            validFrom = at, createdBy = by,
        )
        return VersionChange(terminated, listOf(merged), EditorUnitEvent.Merged(merged.id, setOf(id, other.id)))
    }

    fun split(firstRaw: String, secondRaw: String, at: Instant, by: UUID? = null): VersionChange {
        requireLatest()
        val terminated = endAt(at)
        val first = EditorUnit(
            key = key, type = type, rawContent = firstRaw,
            originEditorUnitIds = originEditorUnitIds, validFrom = at, createdBy = by,
        )
        val second = EditorUnit(
            type = type, rawContent = secondRaw,
            originEditorUnitIds = originEditorUnitIds, validFrom = at, createdBy = by,
        )
        return VersionChange(
            terminated = listOf(terminated),
            created = listOf(first, second),
            event = EditorUnitEvent.Split(id, setOf(first.id, second.id)),
        )
    }

    fun terminate(at: Instant): VersionChange {
        requireLatest()
        val dead = endAt(at).copy(deleted = true)
        return VersionChange(listOf(dead), emptyList(), EditorUnitEvent.Terminated(id, key))
    }
}

data class VersionChange(
    val terminated: List<EditorUnit>,
    val created: List<EditorUnit>,
    val event: EditorUnitEvent,
) {
    constructor(terminated: EditorUnit, created: List<EditorUnit>, event: EditorUnitEvent)
        : this(listOf(terminated), created, event)
}
