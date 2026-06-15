package dev.portfolio.docversion

import dev.portfolio.docversion.domain.*
import dev.portfolio.docversion.app.EditorUnitEditService
import dev.portfolio.docversion.fake.InMemoryEditorUnitRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.assertions.throwables.shouldThrow
import java.time.Instant
import java.util.UUID

class ConcurrencyLogicTest : StringSpec({
    val t0 = Instant.parse("2026-01-01T00:00:00Z")
    val t1 = t0.plusSeconds(100)
    val t2 = t0.plusSeconds(200)

    "C1 stale 거부: 종료된 버전 requireLatest → 예외" {
        val old = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0, validTo = t1)
        shouldThrow<StaleEditorUnitException> { old.requireLatest() }
    }
    "C2 최신 통과: modify로 새 버전" {
        val v0 = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0)
        v0.modify("0|b", t1).created.single().rawContent shouldBe "0|b"
    }
    "C3 직렬 2회: validTo 체인 정합(앞.validTo == 뒤.validFrom)" {
        val v0 = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0)
        val c1 = v0.modify("0|b", t1)
        val v1 = c1.created.single()
        val c2 = v1.modify("0|c", t2)
        c1.terminated.single().validTo shouldBe t1
        v1.validFrom shouldBe t1
        c2.terminated.single().validTo shouldBe t2
    }
    "C4 LWW 차단: 종료된(stale) 버전 재수정 거부 — 조용한 덮어쓰기 불가" {
        val v0 = EditorUnit(type = EditorUnitType.COMMENT, rawContent = "0|base", validFrom = t0)
        val stale = v0.modify("0|edit1", t1).terminated.single()
        shouldThrow<StaleEditorUnitException> { stale.modify("0|edit2", t2) }
    }
    "C4b 응용 editMerge: 정렬 순서로 두 key 조율·병합(직렬화는 인프라 계약, 페이크 no-op)" {
        val repo = InMemoryEditorUnitRepository()
        val keyA = UUID.randomUUID()
        val keyB = UUID.randomUUID()
        val a = EditorUnit(key = keyA, type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0)
        val b = EditorUnit(key = keyB, type = EditorUnitType.COMMENT, rawContent = "0|b", validFrom = t0)
        repo.append(VersionChange(emptyList(), listOf(a, b), EditorUnitEvent.Created(a.id, keyA)))
        val event = EditorUnitEditService(repo).editMerge(keyA, keyB, t1)
        event.shouldBeInstanceOf<EditorUnitEvent.Merged>()
        repo.findLatestByKey(keyA)?.rawContent shouldBe "0|a b"
        repo.findLatestByKey(keyB) shouldBe null
    }
    "C4c editMerge 자기병합(keyA==keyB) 거부" {
        val key = UUID.randomUUID()
        shouldThrow<IllegalArgumentException> {
            EditorUnitEditService(InMemoryEditorUnitRepository()).editMerge(key, key, t1)
        }
    }
})
