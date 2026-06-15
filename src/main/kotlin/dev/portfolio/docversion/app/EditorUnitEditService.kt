package dev.portfolio.docversion.app

import dev.portfolio.docversion.domain.*
import dev.portfolio.docversion.port.EditorUnitRepository
import java.time.Instant
import java.util.UUID

class EditorUnitEditService(
    private val repo: EditorUnitRepository,
    private val dispatcher: UnitTypeDispatcher = UnitTypeDispatcher.default(),
) {

    fun edit(key: EditorUnitKey, newRaw: String, at: Instant, by: UUID? = null): EditorUnitEvent {
        val locked = repo.lockByKey(key) ?: throw StaleEditorUnitException(key)
        val processed = dispatcher.modifyContent(locked.type, newRaw)
        val change = locked.modify(processed, at, by)
        repo.append(change)
        return change.event
    }

    fun editMerge(keyA: EditorUnitKey, keyB: EditorUnitKey, at: Instant, by: UUID? = null): EditorUnitEvent {
        require(keyA != keyB) { "merge requires two distinct keys" }
        val locked = repo.lockByKeys(sortedSetOf(keyA, keyB))
        val a = locked[keyA] ?: throw StaleEditorUnitException(keyA)
        val b = locked[keyB] ?: throw StaleEditorUnitException(keyB)
        if (a.type != b.type) throw EditorUnitTypeMismatchException(a.type, b.type)
        val mergedRaw = dispatcher.mergeContent(a.type, a.rawContent, b.rawContent)
        val change = a.merge(b, mergedRaw, at, by)
        repo.append(change)
        return change.event
    }

    fun editSplit(key: EditorUnitKey, splitAt: Int, at: Instant, by: UUID? = null): EditorUnitEvent {
        val locked = repo.lockByKey(key) ?: throw StaleEditorUnitException(key)
        val (firstRaw, secondRaw) = dispatcher.splitContent(locked.type, locked.rawContent, splitAt)
        val change = locked.split(firstRaw, secondRaw, at, by)
        repo.append(change)
        return change.event
    }

    fun terminate(key: EditorUnitKey, at: Instant): EditorUnitEvent {
        val locked = repo.lockByKey(key) ?: throw StaleEditorUnitException(key)
        val change = locked.terminate(at)
        repo.append(change)
        return change.event
    }
}
