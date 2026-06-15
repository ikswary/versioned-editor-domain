package dev.portfolio.docversion.port

import dev.portfolio.docversion.domain.*
import java.util.SortedSet

interface EditorUnitRepository {
    fun findLatestByKey(key: EditorUnitKey): EditorUnit?

    fun lockByKey(key: EditorUnitKey): EditorUnit?

    fun lockByKeys(keys: SortedSet<EditorUnitKey>): Map<EditorUnitKey, EditorUnit>

    fun append(change: VersionChange)

    fun history(originEditorUnitIds: Set<EditorUnitId>): List<EditorUnit>
}
