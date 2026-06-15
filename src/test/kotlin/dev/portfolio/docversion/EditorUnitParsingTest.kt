package dev.portfolio.docversion

import dev.portfolio.docversion.domain.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import java.time.Instant
import java.util.UUID

class EditorUnitParsingTest : StringSpec({
    val at = Instant.parse("2026-01-01T00:00:00Z")

    "COMMENT 파싱: 'index|text' → SimplePosition + SimpleTextValue" {
        val c = EditorUnitType.COMMENT.parse("2|hello")
        c.position shouldBe UnitPosition.SimplePosition(2)
        c.value.textContent() shouldBe "hello"
    }
    "SUBSCRIPTION_TEXT 파싱: 'start,end|text' → SubscriptionPosition" {
        val s = EditorUnitType.SUBSCRIPTION_TEXT.parse("3,8|자막")
        s.position shouldBe UnitPosition.SubscriptionPosition(3, 8)
        s.value.text shouldBe "자막"
    }
    "IMAGE 파싱: 'x,y|path' → XyPosition + FilePathValue(textContent=path)" {
        val i = EditorUnitType.IMAGE.parse("1,2|/img.png")
        i.position shouldBe UnitPosition.XyPosition(1, 2)
        i.value.textContent() shouldBe "/img.png"
    }
    "TEXT_BOX 파싱: 'x,y|text' → XyPosition + SimpleTextValue" {
        val t = EditorUnitType.TEXT_BOX.parse("4,5|boxed")
        t.position shouldBe UnitPosition.XyPosition(4, 5)
        t.value.text shouldBe "boxed"
    }
    "음수 index → ContentParseException" {
        shouldThrow<ContentParseException> { EditorUnitType.COMMENT.parse("-1|x") }
    }
    "구분자 '|' 없으면 ContentParseException" {
        shouldThrow<ContentParseException> { EditorUnitType.COMMENT.parse("nopipe") }
    }
    "SUBSCRIPTION 역범위(start>=end) → ContentParseException" {
        shouldThrow<ContentParseException> { EditorUnitType.SUBSCRIPTION_TEXT.parse("5,5|x") }
        shouldThrow<ContentParseException> { EditorUnitType.SUBSCRIPTION_TEXT.parse("8,3|x") }
    }
    "XY 음수 좌표 → ContentParseException" {
        shouldThrow<ContentParseException> { EditorUnitType.IMAGE.parse("-1,2|/p") }
    }
    "two-int 포맷 오류(요소 개수) → ContentParseException" {
        shouldThrow<ContentParseException> { EditorUnitType.IMAGE.parse("1|/p") }
    }
    "EditorUnit init이 잘못된 본문을 생성 시점에 거부" {
        shouldThrow<ContentParseException> {
            EditorUnit(type = EditorUnitType.COMMENT, rawContent = "-1|x", validFrom = at)
        }
    }
    "asOf 다중 key: position(index) 오름차순 정렬" {
        val k1 = UUID.randomUUID(); val k2 = UUID.randomUUID()
        val high = EditorUnit(key = k1, type = EditorUnitType.COMMENT, rawContent = "5|late", validFrom = at)
        val low = EditorUnit(key = k2, type = EditorUnitType.COMMENT, rawContent = "1|early", validFrom = at)
        Editor(listOf(high, low)).asOf(at.plusSeconds(1)).map { it.content.value.textContent() } shouldBe listOf("early", "late")
    }
})
