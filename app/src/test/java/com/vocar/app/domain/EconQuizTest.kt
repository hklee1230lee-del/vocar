package com.vocar.app.domain

import com.vocar.app.data.EconStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EconQuizTest {
    private val bank = EconQuiz.bank

    @Test
    fun `문제는 보기 3개이고 정답 번호가 범위 안이며 개념이 사전에 있다`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
        bank.forEach { q ->
            assertEquals("${q.id} 보기는 3개", 3, q.choices.size)
            assertEquals("${q.id} 보기가 겹쳐요", 3, q.choices.toSet().size)
            assertTrue("${q.id} 정답 번호", q.answer in q.choices.indices)
            assertNotNull("${q.id} 개념 ${q.termId} 없음", Dictionary.find(q.termId))
            assertTrue(q.prompt.isNotBlank() && q.explanation.isNotBlank())
        }
    }

    @Test
    fun `정답이 한 자리에 몰리지 않는다`() {
        val counts = bank.groupingBy { it.answer }.eachCount()
        assertTrue("정답 위치가 한쪽에 몰렸어요: $counts", counts.size >= 2)
    }

    @Test
    fun `하루 문제는 같은 날 같고 서로 다르다`() {
        val day = 20_400L
        assertEquals(EconQuiz.questsFor(day), EconQuiz.questsFor(day))
        (day until day + 30).forEach { d ->
            val qs = EconQuiz.questsFor(d)
            assertEquals(EconQuiz.PER_DAY, qs.size)
            assertEquals(EconQuiz.PER_DAY, qs.map { it.id }.toSet().size)
        }
        assertFalse(EconQuiz.questsFor(day) == EconQuiz.questsFor(day + 1))
    }

    @Test
    fun `며칠이 지나면 모든 문제가 한 번씩 나온다`() {
        val seen = (0L until bank.size.toLong()).flatMap { EconQuiz.questsFor(it) }.map { it.id }.toSet()
        assertEquals(bank.size, seen.size)
    }

    @Test
    fun `틀리면 다시 볼 개념에 들어가고 맞히면 들어가지 않는다`() {
        var p = EconProgress.empty(10)
        val qs = p.questions()
        p = p.answer(qs[0].answer)
        p = p.answer((qs[1].answer + 1) % 3)
        assertEquals(listOf(qs[1].termId), p.review)
        p = p.answer(qs[2].answer)
        assertTrue(p.done)
        assertEquals(2, p.correct)
        // 끝난 뒤에는 더 답할 수 없다
        assertEquals(p, p.answer(0))
    }

    @Test
    fun `날이 바뀌면 답은 비우고 다시 볼 개념은 남긴다`() {
        var p = EconProgress.empty(10)
        val q = p.questions()[0]
        p = p.answer((q.answer + 1) % 3)
        val next = p.forDay(11)
        assertTrue(next.picks.isEmpty())
        assertEquals(p.review, next.review)
        assertEquals(p, p.forDay(10))
    }

    @Test
    fun `사전에서 확인하면 다시 볼 개념에서 빠진다`() {
        val p = EconProgress(1, emptyList(), listOf("inflation", "gdp"))
        assertEquals(listOf("gdp"), p.reviewed("inflation").review)
    }

    @Test
    fun `잘못된 보기 번호는 무시한다`() {
        val p = EconProgress.empty(3)
        assertEquals(p, p.answer(7))
    }

    /** 그날 문제를 모두 맞히거나(정답) 모두 틀리게(오답) 끝낸다. */
    private fun finish(p: EconProgress, right: Boolean = true): EconProgress {
        var cur = p
        while (!cur.done) {
            val q = cur.questions()[cur.picks.size]
            cur = cur.answer(if (right) q.answer else (q.answer + 1) % 3)
        }
        return cur
    }

    @Test
    fun `매일 끝내면 연속 기록이 늘고 누적 일차가 쌓인다`() {
        var p = finish(EconProgress.empty(100))
        assertEquals(1, p.streak)
        p = finish(p.forDay(101))
        p = finish(p.forDay(102))
        assertEquals(3, p.streak)
        assertEquals(3, p.totalDays)
        assertEquals(3, p.currentStreak(102))
        assertEquals(3, p.currentStreak(103)) // 아직 오늘 안 했어도 어제까지 이어졌으면 유지
    }

    @Test
    fun `하루를 놓치면 연속이 끊기고 다시 1부터 시작한다`() {
        var p = finish(EconProgress.empty(100))
        p = finish(p.forDay(101))
        assertEquals(0, p.currentStreak(104))
        p = finish(p.forDay(104))
        assertEquals(1, p.streak)
        assertEquals(2, p.best)
    }

    @Test
    fun `7일 연속이면 보호권을 받고 하루를 놓쳐도 이어진다`() {
        var p = EconProgress.empty(0)
        for (d in 0L until 7L) p = finish(p.forDay(d))
        assertEquals(7, p.streak)
        assertEquals(1, p.freezes)
        // 7일째를 건너뛰고 8일째에 하면 보호권을 쓰고 이어진다
        assertEquals(7, p.currentStreak(8))
        p = finish(p.forDay(8))
        assertEquals(8, p.streak)
        assertEquals(0, p.freezes)
    }

    @Test
    fun `배지는 처음 받을 때만 새 배지로 표시된다`() {
        var p = finish(EconProgress.empty(0))
        assertTrue("first" in p.newBadges)
        assertTrue("perfect1" in p.newBadges)
        p = finish(p.forDay(1))
        assertFalse("first" in p.newBadges)
        p = finish(p.forDay(2))
        assertTrue("streak3" in p.newBadges)
        assertTrue(p.forDay(3).newBadges.isEmpty())
    }

    @Test
    fun `모두 틀리면 만점 배지는 없다`() {
        val p = finish(EconProgress.empty(0), right = false)
        assertFalse("perfect1" in p.newBadges)
        assertEquals(0, p.perfectDays)
        assertEquals(1, p.dayNumber())
    }

    @Test
    fun `심화 퀴즈 팩을 풀면 정복 목록과 전용 배지가 반영된다`() {
        val starter = EconQuiz.findPack("pack-starter")
        assertNotNull(starter)
        assertEquals(3, EconQuiz.packs.size)
        var p = EconProgress.empty(0)
        starter!!.questions.forEach { q ->
            p = p.answerPackQuestion(q, q.answer)
        }
        assertTrue(p.isPackCompleted(starter))
        assertEquals(starter.questions.size, p.packSolvedCount(starter))
        val earnedIds = EconBadges.earned(p).map { it.id }.toSet()
        assertTrue("pack_first" in earnedIds)
        assertTrue("pack_starter" in earnedIds)
    }

    @Test
    fun `가민 스타일 배지 도감 30종과 유저 레벨이 정상 계산된다`() {
        assertEquals(30, EconBadges.all.size)
        assertEquals(30, EconBadges.all.map { it.id }.toSet().size)
        val lv1 = EconBadges.levelInfo(EconProgress.empty(0))
        assertEquals(1, lv1.level)
        assertEquals(0, lv1.totalPoints)

        var p = finish(EconProgress.empty(0))
        EconQuiz.packs.flatMap { it.questions }.forEach { q ->
            p = p.answerPackQuestion(q, q.answer)
        }
        val lvAfter = EconBadges.levelInfo(p)
        assertTrue(lvAfter.totalPoints > 15)
        assertTrue(lvAfter.level >= 3)
    }

    @Test
    fun `연속 보호권 추가 구매는 최대 보유 한도까지 동작하고 저장소에서 복원된다`() {
        var p = EconProgress.empty(5)
        p = p.buyFreeze()
        assertEquals(1, p.freezes)
        assertEquals(1, p.purchasedFreezes)
        assertTrue("freeze_keeper" in p.newBadges)

        repeat(10) { p = p.buyFreeze() }
        assertEquals(EconProgress.MAX_PURCHASED_FREEZES, p.freezes)

        p = p.reviewed("inflation").answerPackQuestion(EconQuiz.packs.first().questions.first(), EconQuiz.packs.first().questions.first().answer)
        val decoded = EconStore.decode(EconStore.encode(p))
        assertEquals(p, decoded)
    }
}
