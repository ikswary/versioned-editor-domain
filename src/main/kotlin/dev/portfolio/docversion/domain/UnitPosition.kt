package dev.portfolio.docversion.domain

sealed interface UnitPosition : Comparable<UnitPosition> {
    fun sortKey(): List<Int>

    override fun compareTo(other: UnitPosition): Int {
        val a = sortKey()
        val b = other.sortKey()
        for (i in 0 until minOf(a.size, b.size)) {
            val c = a[i].compareTo(b[i])
            if (c != 0) return c
        }
        return a.size.compareTo(b.size)
    }

    data class SimplePosition(val index: Int) : UnitPosition {
        override fun sortKey() = listOf(index)
    }

    data class SubscriptionPosition(val start: Int, val end: Int) : UnitPosition {
        override fun sortKey() = listOf(start, end)
    }

    data class XyPosition(val x: Int, val y: Int) : UnitPosition {
        override fun sortKey() = listOf(y, x)
    }
}
