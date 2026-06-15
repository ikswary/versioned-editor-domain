package dev.portfolio.docversion.app

import dev.portfolio.docversion.domain.EditorUnitType

class TextBoxUnitTypeService : UnitTypeService {
    override val type = EditorUnitType.TEXT_BOX

    override fun splitContent(rawContent: String, splitAt: Int): Pair<String, String> =
        throw UnsupportedSplitException(type)

    override fun mergeContent(first: String, second: String): String =
        throw UnsupportedMergeException(type)
}
