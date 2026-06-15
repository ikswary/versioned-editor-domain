package dev.portfolio.docversion.domain

sealed interface UnitValue {
    fun textContent(): String

    data class SimpleTextValue(val text: String) : UnitValue {
        override fun textContent() = text
    }
    data class FilePathValue(val path: String) : UnitValue {
        override fun textContent() = path
    }
}
