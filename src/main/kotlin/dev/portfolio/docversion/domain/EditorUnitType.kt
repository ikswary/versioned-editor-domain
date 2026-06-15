package dev.portfolio.docversion.domain

sealed class EditorUnitType<out V : UnitValue, out P : UnitPosition> {
    abstract fun parseValue(raw: String): V
    abstract fun parsePosition(raw: String): P

    fun parse(raw: String): EditorUnitContent<V, P> = EditorUnitContent(parseValue(raw), parsePosition(raw))

    data object SUBSCRIPTION_TEXT : EditorUnitType<UnitValue.SimpleTextValue, UnitPosition.SubscriptionPosition>() {
        override fun parseValue(raw: String) = UnitValue.SimpleTextValue(payloadOf(raw))
        override fun parsePosition(raw: String): UnitPosition.SubscriptionPosition {
            val (start, end) = twoInts(prefixOf(raw))
            if (start >= end) throw ContentParseException("subscription start($start) must be < end($end)")
            return UnitPosition.SubscriptionPosition(start, end)
        }
    }

    data object IMAGE : EditorUnitType<UnitValue.FilePathValue, UnitPosition.XyPosition>() {
        override fun parseValue(raw: String) = UnitValue.FilePathValue(payloadOf(raw))
        override fun parsePosition(raw: String): UnitPosition.XyPosition {
            val (x, y) = twoInts(prefixOf(raw))
            return UnitPosition.XyPosition(x, y)
        }
    }

    data object TEXT_BOX : EditorUnitType<UnitValue.SimpleTextValue, UnitPosition.XyPosition>() {
        override fun parseValue(raw: String) = UnitValue.SimpleTextValue(payloadOf(raw))
        override fun parsePosition(raw: String): UnitPosition.XyPosition {
            val (x, y) = twoInts(prefixOf(raw))
            return UnitPosition.XyPosition(x, y)
        }
    }

    data object COMMENT : EditorUnitType<UnitValue.SimpleTextValue, UnitPosition.SimplePosition>() {
        override fun parseValue(raw: String) = UnitValue.SimpleTextValue(payloadOf(raw))
        override fun parsePosition(raw: String) = UnitPosition.SimplePosition(nonNegInt(prefixOf(raw)))
    }
}

private fun prefixOf(raw: String): String {
    val i = raw.indexOf('|')
    if (i < 0) throw ContentParseException("missing '|' position separator in '$raw'")
    return raw.substring(0, i)
}

private fun payloadOf(raw: String): String {
    val i = raw.indexOf('|')
    if (i < 0) throw ContentParseException("missing '|' position separator in '$raw'")
    return raw.substring(i + 1)
}

private fun nonNegInt(s: String): Int {
    val n = s.trim().toIntOrNull() ?: throw ContentParseException("position must be an integer but was '$s'")
    if (n < 0) throw ContentParseException("position must be >= 0 but was $n")
    return n
}

private fun twoInts(s: String): Pair<Int, Int> {
    val parts = s.split(',')
    if (parts.size != 2) throw ContentParseException("expected 'a,b' but was '$s'")
    return nonNegInt(parts[0]) to nonNegInt(parts[1])
}
