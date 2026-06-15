package dev.portfolio.docversion

import dev.portfolio.docversion.domain.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.util.UUID

class EditorUnitTerminateTest : StringSpec({
    val t0 = Instant.parse("2026-01-01T00:00:00Z")
    val t1 = t0.plusSeconds(100)

    "D1 soft delete: 과거 시점엔 살아있고 종료 후엔 미노출(이력 잔존)" {
        val key = UUID.randomUUID()
        val v1 = EditorUnit(key = key, type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0)
        val dead = v1.terminate(t1).terminated.single()
        val doc = Editor(listOf(dead))
        doc.asOf(t0.plusSeconds(10)).map { it.rawContent } shouldBe listOf("0|a")
        doc.asOf(t1.plusSeconds(10)) shouldBe emptyList()
    }
    "D2 종료 후 latest 부재" {
        val v1 = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0)
        val dead = v1.terminate(t1).terminated.single()
        Editor(listOf(dead)).latest() shouldBe emptyList()
    }
    "D3 terminate: deleted 플래그·validTo·Terminated 이벤트" {
        val v1 = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0)
        val change = v1.terminate(t1)
        change.event.shouldBeInstanceOf<EditorUnitEvent.Terminated>()
        val dead = change.terminated.single()
        dead.deleted shouldBe true
        dead.validTo shouldBe t1
    }
})
