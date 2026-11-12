package com.vocar.app.domain

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
}
