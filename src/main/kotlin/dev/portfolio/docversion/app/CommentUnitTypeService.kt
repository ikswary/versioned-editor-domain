package dev.portfolio.docversion.app

import dev.portfolio.docversion.domain.EditorUnitType

class CommentUnitTypeService : UnitTypeService {
    override val type = EditorUnitType.COMMENT
}
