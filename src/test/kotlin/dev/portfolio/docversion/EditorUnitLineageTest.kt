package dev.portfolio.docversion

import dev.portfolio.docversion.domain.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import java.time.Instant

class EditorUnitLineageTest : StringSpec({
    val at = Instant.parse("2026-01-01T00:00:00Z")
    val at2 = at.plusSeconds(100)

    "L1 merge: origin 합집합·key 유지·양쪽 종료" {
        val a = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = at)
        val b = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|b", validFrom = at)
        val change = a.merge(b, "0|ab", at2)
        val merged = change.created.single()
        merged.originEditorUnitIds shouldBe (a.originEditorUnitIds + b.originEditorUnitIds)
        merged.key shouldBe a.key
        change.terminated.map { it.id }.toSet() shouldBe setOf(a.id, b.id)
    }
    "L2 merge origin 멱등: 중복 origin 합집합 불변(Set 의미)" {
        val a = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = at)
        val b = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|b", validFrom = at, originEditorUnitIds = setOf(a.id))
        val merged = a.merge(b, "0|ab", at2).created.single()
        merged.originEditorUnitIds shouldBe setOf(a.id)
    }
    "L3 split: 첫째 key 유지·둘째 새 key" {
        val o = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|o", validFrom = at)
        val (first, second) = o.split("0|x", "0|y", at2).created
        first.key shouldBe o.key
        second.key shouldNotBe first.key
    }
    "L4 split: origin 양쪽 승계" {
        val o = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|o", validFrom = at)
        o.split("0|x", "0|y", at2).created.forEach { it.originEditorUnitIds shouldBe o.originEditorUnitIds }
    }
    "L5 merge 타입 불일치 예외" {
        val a = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = at)
        val b = EditorUnit(type = EditorUnitType.IMAGE, rawContent = "0,0|/p.png", validFrom = at)
        shouldThrow<EditorUnitTypeMismatchException> { a.merge(b, "0|x", at2) }
    }
})
