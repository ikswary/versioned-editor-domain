package dev.portfolio.docversion.app

import dev.portfolio.docversion.domain.EditorUnit
import dev.portfolio.docversion.domain.EditorUnitType
import java.time.Instant
import java.util.UUID

class UnsupportedUnitTypeException(val type: EditorUnitType<*, *>) :
    RuntimeException("no UnitTypeService for type $type")

class UnsupportedSplitException(val type: EditorUnitType<*, *>) :
    RuntimeException("type $type does not support split")

class UnsupportedMergeException(val type: EditorUnitType<*, *>, detail: String = "") :
    RuntimeException("type $type does not support merge" + if (detail.isEmpty()) "" else ": $detail")

class UnitTypeDispatcher(services: List<UnitTypeService>) {
    private val byType: Map<EditorUnitType<*, *>, UnitTypeService> = services.associateBy { it.type }

    fun create(type: EditorUnitType<*, *>, rawContent: String, at: Instant, by: UUID? = null): EditorUnit =
        service(type).create(rawContent, at, by)

    fun splitContent(type: EditorUnitType<*, *>, rawContent: String, splitAt: Int): Pair<String, String> =
        service(type).splitContent(rawContent, splitAt)

    fun mergeContent(type: EditorUnitType<*, *>, first: String, second: String): String =
        service(type).mergeContent(first, second)

    fun modifyContent(type: EditorUnitType<*, *>, newRaw: String): String =
        service(type).modifyContent(newRaw)

    private fun service(type: EditorUnitType<*, *>): UnitTypeService =
        byType[type] ?: throw UnsupportedUnitTypeException(type)

    companion object {
        fun default(): UnitTypeDispatcher = UnitTypeDispatcher(
            listOf(
                SubscriptionTextUnitTypeService(),
                ImageUnitTypeService(),
                TextBoxUnitTypeService(),
                CommentUnitTypeService(),
            ),
        )
    }
}
