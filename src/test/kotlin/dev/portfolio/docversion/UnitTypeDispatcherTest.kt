package dev.portfolio.docversion

import dev.portfolio.docversion.app.CommentUnitTypeService
import dev.portfolio.docversion.app.UnitTypeDispatcher
import dev.portfolio.docversion.app.UnitTypeService
import dev.portfolio.docversion.app.UnsupportedMergeException
import dev.portfolio.docversion.app.UnsupportedSplitException
import dev.portfolio.docversion.app.UnsupportedUnitTypeException
import dev.portfolio.docversion.domain.EditorUnitType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.time.Instant

class UnitTypeDispatcherTest : StringSpec({
    val at = Instant.parse("2026-01-01T00:00:00Z")
    val dispatcher = UnitTypeDispatcher.default()

    "create: COMMENT → COMMENT 유닛 생성" {
        val unit = dispatcher.create(EditorUnitType.COMMENT, "0|hello", at)
        unit.type shouldBe EditorUnitType.COMMENT
        unit.rawContent shouldBe "0|hello"
    }
    "create: IMAGE 고유 검증 — path 비면 거부" {
        shouldThrow<IllegalArgumentException> { dispatcher.create(EditorUnitType.IMAGE, "0,0|", at) }
    }
    "create: SUBSCRIPTION_TEXT 고유 검증 — text 비면 거부" {
        shouldThrow<IllegalArgumentException> { dispatcher.create(EditorUnitType.SUBSCRIPTION_TEXT, "0,1|", at) }
    }
    "create: 미지원 타입(담당 서비스 없음) → UnsupportedUnitTypeException" {
        val onlyComment = UnitTypeDispatcher(listOf(CommentUnitTypeService()))
        shouldThrow<UnsupportedUnitTypeException> { onlyComment.create(EditorUnitType.IMAGE, "0,0|/p", at) }
    }

    "split: COMMENT 지원 — 위치 prefix 보존하며 payload 분할" {
        dispatcher.splitContent(EditorUnitType.COMMENT, "0|hello", 2) shouldBe ("0|he" to "0|llo")
    }
    "split: IMAGE = 분할 거부(파일은 쪼갤 수 없음)" {
        shouldThrow<UnsupportedSplitException> { dispatcher.splitContent(EditorUnitType.IMAGE, "0,0|/p", 1) }
    }
    "split: SUBSCRIPTION_TEXT = 분할 거부(자막은 원자 단위)" {
        shouldThrow<UnsupportedSplitException> { dispatcher.splitContent(EditorUnitType.SUBSCRIPTION_TEXT, "0,1|s", 1) }
    }
    "split: TEXT_BOX = 분할 거부(위치 박스는 원자 단위)" {
        shouldThrow<UnsupportedSplitException> { dispatcher.splitContent(EditorUnitType.TEXT_BOX, "0,0|t", 1) }
    }

    "merge: COMMENT = payload 공백 조인(위치 prefix 유지)" {
        dispatcher.mergeContent(EditorUnitType.COMMENT, "0|a", "0|b") shouldBe "0|a b"
    }
    "merge: IMAGE = 병합 거부" {
        shouldThrow<UnsupportedMergeException> { dispatcher.mergeContent(EditorUnitType.IMAGE, "0,0|/a", "1,1|/b") }
    }
    "merge: SUBSCRIPTION_TEXT = 병합 거부" {
        shouldThrow<UnsupportedMergeException> { dispatcher.mergeContent(EditorUnitType.SUBSCRIPTION_TEXT, "0,1|a", "1,2|b") }
    }

    "modify: default 그대로 통과(변환단계 실재, 미래 확장점)" {
        dispatcher.modifyContent(EditorUnitType.COMMENT, "0|x") shouldBe "0|x"
    }

    "OCP: 새 타입 담당 서비스를 List에 더하면 자동 등록(디스패처 무수정)" {
        val textBoxPlain = object : UnitTypeService { override val type = EditorUnitType.TEXT_BOX }
        val extended = UnitTypeDispatcher(listOf(CommentUnitTypeService(), textBoxPlain))
        extended.create(EditorUnitType.TEXT_BOX, "0,0|t", at).type shouldBe EditorUnitType.TEXT_BOX
    }
})
