package dev.portfolio.docversion

import dev.portfolio.docversion.domain.*
import dev.portfolio.docversion.app.EditorUnitEditService
import dev.portfolio.docversion.fake.InMemoryEditorUnitRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.assertions.throwables.shouldThrow
import java.time.Instant
import java.util.UUID

class EditorUnitEditServiceTest : StringSpec({
    val t0 = Instant.parse("2026-01-01T00:00:00Z")
    val t1 = t0.plusSeconds(100)
    val t2 = t0.plusSeconds(200)

    fun repoWith(vararg units: EditorUnit): InMemoryEditorUnitRepository {
        val repo = InMemoryEditorUnitRepository()
        repo.append(VersionChange(emptyList(), units.toList(), EditorUnitEvent.Created(units.first().id, units.first().key)))
        return repo
    }

    "edit(): lockByKey→modify→append, 최신이 새 raw로 전환" {
        val key = UUID.randomUUID()
        val repo = repoWith(EditorUnit(key = key, type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0))
        val event = EditorUnitEditService(repo).edit(key, "0|b", t1)
        event.shouldBeInstanceOf<EditorUnitEvent.Modified>()
        repo.findLatestByKey(key)?.rawContent shouldBe "0|b"
    }

    "edit(): 없는 key → StaleEditorUnitException" {
        shouldThrow<StaleEditorUnitException> {
            EditorUnitEditService(InMemoryEditorUnitRepository()).edit(UUID.randomUUID(), "0|x", t1)
        }
    }

    "editSplit(): 응용 흐름 — 첫째 key 유지·두 자식 생성(COMMENT 분할 지원)" {
        val key = UUID.randomUUID()
        val repo = repoWith(EditorUnit(key = key, type = EditorUnitType.COMMENT, rawContent = "0|hello", validFrom = t0))
        val event = EditorUnitEditService(repo).editSplit(key, 2, t1)
        event.shouldBeInstanceOf<EditorUnitEvent.Split>()
        repo.findLatestByKey(key)?.rawContent shouldBe "0|he"
        (event as EditorUnitEvent.Split).into.size shouldBe 2
    }

    "무손실 시점 복원(end-to-end): edit 2회 후 과거 t는 옛 버전, 최신은 마지막" {
        val key = UUID.randomUUID()
        val v0 = EditorUnit(key = key, type = EditorUnitType.COMMENT, rawContent = "0|v1", validFrom = t0)
        val repo = repoWith(v0)
        val svc = EditorUnitEditService(repo)
        svc.edit(key, "0|v2", t1)
        svc.edit(key, "0|v3", t2)
        val doc = Editor(repo.history(setOf(v0.id)))
        doc.asOf(t0.plusSeconds(1)).map { it.rawContent } shouldBe listOf("0|v1")
        doc.asOf(t1.plusSeconds(1)).map { it.rawContent } shouldBe listOf("0|v2")
        doc.latest().map { it.rawContent } shouldBe listOf("0|v3")
    }

    "L6 계보 역추적: merge 후 history(origin)가 조상+병합본 포함" {
        val keyA = UUID.randomUUID(); val keyB = UUID.randomUUID()
        val a = EditorUnit(key = keyA, type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0)
        val b = EditorUnit(key = keyB, type = EditorUnitType.COMMENT, rawContent = "0|b", validFrom = t0)
        val repo = InMemoryEditorUnitRepository()
        repo.append(VersionChange(emptyList(), listOf(a, b), EditorUnitEvent.Created(a.id, keyA)))
        EditorUnitEditService(repo).editMerge(keyA, keyB, t1)
        repo.history(setOf(a.id)).map { it.rawContent } shouldContainAll listOf("0|a", "0|a b")
    }

    "InvalidEditTime: at <= validFrom → 도메인 예외(IllegalArgument 누출 아님)" {
        val key = UUID.randomUUID()
        val repo = repoWith(EditorUnit(key = key, type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t1))
        shouldThrow<InvalidEditTimeException> { EditorUnitEditService(repo).edit(key, "0|b", t0) }
    }

    "terminate(): 응용 노출 — soft delete 후 최신 부재 + Terminated 이벤트" {
        val key = UUID.randomUUID()
        val repo = repoWith(EditorUnit(key = key, type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0))
        val event = EditorUnitEditService(repo).terminate(key, t1)
        event.shouldBeInstanceOf<EditorUnitEvent.Terminated>()
        repo.findLatestByKey(key) shouldBe null
    }

    "editMerge(): 타입 불일치 → EditorUnitTypeMismatchException(정책 호출 전)" {
        val keyA = UUID.randomUUID(); val keyB = UUID.randomUUID()
        val a = EditorUnit(key = keyA, type = EditorUnitType.COMMENT, rawContent = "0|a", validFrom = t0)
        val b = EditorUnit(key = keyB, type = EditorUnitType.IMAGE, rawContent = "0,0|/p.png", validFrom = t0)
        val repo = InMemoryEditorUnitRepository()
        repo.append(VersionChange(emptyList(), listOf(a, b), EditorUnitEvent.Created(a.id, keyA)))
        shouldThrow<EditorUnitTypeMismatchException> { EditorUnitEditService(repo).editMerge(keyA, keyB, t1) }
    }
})
