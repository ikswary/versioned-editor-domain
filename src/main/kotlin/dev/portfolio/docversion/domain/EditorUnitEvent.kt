package dev.portfolio.docversion.domain

sealed interface EditorUnitEvent {
    data class Created(val unitId: EditorUnitId, val key: EditorUnitKey) : EditorUnitEvent
    data class Modified(val unitId: EditorUnitId, val key: EditorUnitKey) : EditorUnitEvent
    data class Terminated(val unitId: EditorUnitId, val key: EditorUnitKey) : EditorUnitEvent
    data class Merged(val unitId: EditorUnitId, val sources: Set<EditorUnitId>) : EditorUnitEvent
    data class Split(val origin: EditorUnitId, val into: Set<EditorUnitId>) : EditorUnitEvent
}
