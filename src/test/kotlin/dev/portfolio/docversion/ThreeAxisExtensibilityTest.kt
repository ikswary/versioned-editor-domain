package dev.portfolio.docversion

import dev.portfolio.docversion.domain.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class ThreeAxisExtensibilityTest : StringSpec({
    val at = Instant.parse("2026-01-01T00:00:00Z")

    "타입 바인딩: COMMENT의 value·position은 SimpleText·Simple로 정적 결정(캐스팅 불요)" {
        val c = EditorUnitType.COMMENT.parse("2|hi")
        c.value.text shouldBe "hi"
        c.position.index shouldBe 2
    }
    "타입 바인딩: IMAGE의 value·position은 FilePath·Xy로 정적 결정" {
        val i = EditorUnitType.IMAGE.parse("1,2|/p.png")
        i.value.path shouldBe "/p.png"
        i.position.x shouldBe 1
        i.position.y shouldBe 2
    }
    "value 공유(N:1): SUBSCRIPTION_TEXT·TEXT_BOX 모두 SimpleTextValue(.text 직접 접근)" {
        EditorUnitType.SUBSCRIPTION_TEXT.parse("0,1|s").value.text shouldBe "s"
        EditorUnitType.TEXT_BOX.parse("3,4|t").value.text shouldBe "t"
    }
    "position 공유: IMAGE·TEXT_BOX 모두 XyPosition(.x/.y 직접 접근)" {
        EditorUnitType.IMAGE.parse("5,6|/p").position.let { it.x shouldBe 5; it.y shouldBe 6 }
        EditorUnitType.TEXT_BOX.parse("7,8|t").position.let { it.x shouldBe 7; it.y shouldBe 8 }
    }
    "value 평문: FilePathValue.textContent=path" {
        UnitValue.FilePathValue("/a/b.png").textContent() shouldBe "/a/b.png"
    }
    "position sortKey: Simple=[index], Subscription=[start,end], Xy=[y,x]" {
        UnitPosition.SimplePosition(3).sortKey() shouldBe listOf(3)
        UnitPosition.SubscriptionPosition(2, 7).sortKey() shouldBe listOf(2, 7)
        UnitPosition.XyPosition(4, 1).sortKey() shouldBe listOf(1, 4)
    }
    "cross-type 정렬: 다른 타입(다른 position 종류)이 sortKey로 한 줄 정렬" {
        val comment = EditorUnit(key = UUID.randomUUID(), type = EditorUnitType.COMMENT, rawContent = "1|c", validFrom = at)
        val image = EditorUnit(key = UUID.randomUUID(), type = EditorUnitType.IMAGE, rawContent = "0,2|/i.png", validFrom = at)
        val sub = EditorUnit(key = UUID.randomUUID(), type = EditorUnitType.SUBSCRIPTION_TEXT, rawContent = "3,9|s", validFrom = at)
        Editor(listOf(sub, image, comment)).asOf(at.plusSeconds(1)).map { it.type } shouldBe
            listOf(EditorUnitType.COMMENT, EditorUnitType.IMAGE, EditorUnitType.SUBSCRIPTION_TEXT)
    }
})
