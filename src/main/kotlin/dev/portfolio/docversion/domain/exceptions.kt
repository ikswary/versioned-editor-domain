package dev.portfolio.docversion.domain

import java.time.Instant
import java.util.UUID

class StaleEditorUnitException(val id: UUID) : RuntimeException("unit $id is not latest")
class EditorUnitTypeMismatchException(val a: EditorUnitType<*, *>, val b: EditorUnitType<*, *>) : RuntimeException("$a != $b")

class InvalidEditTimeException(val at: Instant, val from: Instant) :
    RuntimeException("edit time $at must be after validFrom $from")

class ContentParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
