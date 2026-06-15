package dev.portfolio.docversion.domain

data class EditorUnitContent<out V : UnitValue, out P : UnitPosition>(
    val value: V,
    val position: P,
) : Comparable<EditorUnitContent<*, *>> {
    override fun compareTo(other: EditorUnitContent<*, *>): Int = position.compareTo(other.position)
}
