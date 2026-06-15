package dev.portfolio.docversion.fake

import dev.portfolio.docversion.domain.*
import dev.portfolio.docversion.port.EditorUnitRepository
import java.util.SortedSet

class InMemoryEditorUnitRepository : EditorUnitRepository {
    private val store = LinkedHashMap<EditorUnitId, EditorUnit>()

    override fun findLatestByKey(key: EditorUnitKey): EditorUnit? =
        store.values.filter { it.key == key }.firstOrNull { it.checkLatest() }

    override fun lockByKey(key: EditorUnitKey): EditorUnit? = findLatestByKey(key)

    override fun lockByKeys(keys: SortedSet<EditorUnitKey>): Map<EditorUnitKey, EditorUnit> =
        keys.mapNotNull { k -> findLatestByKey(k)?.let { k to it } }.toMap()

    override fun append(change: VersionChange) {
        change.terminated.forEach { store[it.id] = it }
        change.created.forEach { store[it.id] = it }
    }

    override fun history(originEditorUnitIds: Set<EditorUnitId>): List<EditorUnit> =
        store.values.filter { it.originEditorUnitIds.any { o -> o in originEditorUnitIds } }
}
