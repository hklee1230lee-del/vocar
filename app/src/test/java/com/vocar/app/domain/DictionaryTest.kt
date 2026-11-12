package com.vocar.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryTest {
    private val terms = Dictionary.terms

    @Test
    fun `id는 서로 달라야 한다`() {
        assertEquals(terms.size, terms.map { it.id }.toSet().size)
    }

    @Test
    fun `모든 칸이 채워져 있다`() {
        terms.forEach { t ->
            listOf(t.id, t.ko, t.en, t.topic, t.plain, t.example, t.exampleKo).forEach {
                assertTrue("${t.id}에 빈 칸이 있어요", it.isNotBlank())
            }
        }
    }

    @Test
    fun `관련 용어는 실제로 존재하고 자기 자신이 아니다`() {
        terms.forEach { t ->
            assertTrue("${t.id}의 관련 용어가 비어 있어요", t.related.isNotEmpty())
            t.related.forEach { rid ->
                assertNotNull("${t.id}의 관련 용어 $rid 가 없어요", Dictionary.find(rid))
                assertNotEquals("${t.id}가 자기 자신을 가리켜요", t.id, rid)
            }
            assertEquals("${t.id}의 관련 용어가 겹쳐요", t.related.size, t.related.toSet().size)
        }
    }

    @Test
    fun `주제마다 용어가 있고 주제 목록이 중복되지 않는다`() {
        assertEquals(Dictionary.topics.size, Dictionary.topics.toSet().size)
        Dictionary.topics.forEach { assertTrue(Dictionary.byTopic(it).isNotEmpty()) }
        assertEquals(terms.size, Dictionary.topics.sumOf { Dictionary.byTopic(it).size })
    }

    @Test
    fun `수치와 투자 권유 표현은 넣지 않는다`() {
        val banned = listOf("추천", "매수", "매도", "유망", "사세요", "파세요", "수익률 보장", "반드시 오른")
        terms.forEach { t ->
            listOf(t.ko, t.en, t.plain, t.example, t.exampleKo).forEach { text ->
                assertFalse("${t.id}에 숫자가 있어요: $text", text.any { it.isDigit() })
                banned.forEach { w -> assertFalse("${t.id}에 '$w' 표현이 있어요", text.contains(w)) }
            }
        }
    }

    @Test
    fun `오늘의 용어는 같은 날 같고 하루 지나면 바뀐다`() {
        assertEquals(Dictionary.termOfDay(20_000), Dictionary.termOfDay(20_000))
        (20_000L until 20_000L + terms.size).forEach { d ->
            assertNotEquals(Dictionary.termOfDay(d), Dictionary.termOfDay(d + 1))
        }
    }

    @Test
    fun `오늘의 용어는 한 바퀴를 돌면 모든 용어를 한 번씩 거친다`() {
        val seen = (0L until terms.size.toLong()).map { Dictionary.termOfDay(it).id }.toSet()
        assertEquals(terms.size, seen.size)
    }

    @Test
    fun `날짜가 음수여도 범위를 벗어나지 않는다`() {
        assertNotNull(Dictionary.find(Dictionary.termOfDay(-5).id))
    }
}
