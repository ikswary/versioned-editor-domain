package dev.portfolio.docversion.app

import dev.portfolio.docversion.domain.EditorUnit
import dev.portfolio.docversion.domain.EditorUnitType
import java.time.Instant
import java.util.UUID

class ImageUnitTypeService : UnitTypeService {
    override val type = EditorUnitType.IMAGE

    override fun create(rawContent: String, at: Instant, by: UUID?): EditorUnit {
        require(type.parseValue(rawContent).path.isNotBlank()) { "image path must not be blank" }
        return EditorUnit(type = type, rawContent = rawContent, validFrom = at, createdBy = by)
    }

    override fun splitContent(rawContent: String, splitAt: Int): Pair<String, String> =
        throw UnsupportedSplitException(type)

    override fun mergeContent(first: String, second: String): String =
        throw UnsupportedMergeException(type)
}
