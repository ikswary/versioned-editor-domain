package dev.portfolio.docversion

import dev.portfolio.docversion.domain.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class EditorUnitTemporalTest : StringSpec({
    val key = UUID.randomUUID()
    val t0 = Instant.parse("2026-01-01T00:00:00Z")
    val t1 = Instant.parse("2026-01-02T00:00:00Z")
    val t2 = Instant.parse("2026-01-03T00:00:00Z")

    fun v(from: Instant, to: Instant?, raw: String) =
        EditorUnit(key = key, type = EditorUnitType.COMMENT, rawContent = raw, validFrom = from, validTo = to)

    "T1 경계 하단: asOf(t=validFrom) 포함 (<=)" {
        Editor(listOf(v(t0, t1, "0|a"))).asOf(t0).map { it.rawContent } shouldBe listOf("0|a")
    }
    "T2 경계 상단: asOf(t=validTo) 제외, 다음 버전 (<)" {
        val doc = Editor(listOf(v(t0, t1, "0|a"), v(t1, null, "0|b")))
        doc.asOf(t1).map { it.rawContent } shouldBe listOf("0|b")
    }
    "T3 시점 유일성: 임의 t에 key당 정확히 1버전" {
        val doc = Editor(listOf(v(t0, t1, "0|a"), v(t1, t2, "0|b"), v(t2, null, "0|c")))
        doc.asOf(t1.plusSeconds(10)).size shouldBe 1
    }
    "T4 최신 = validTo null" {
        Editor(listOf(v(t0, t1, "0|a"), v(t1, null, "0|b"))).latest().map { it.rawContent } shouldBe listOf("0|b")
    }
    "T5 과거 복원: v3 최신에서 과거 t는 v2" {
        val doc = Editor(listOf(v(t0, t1, "0|a"), v(t1, t2, "0|b"), v(t2, null, "0|c")))
        doc.asOf(t1.plusSeconds(1)).map { it.rawContent } shouldBe listOf("0|b")
    }
})
