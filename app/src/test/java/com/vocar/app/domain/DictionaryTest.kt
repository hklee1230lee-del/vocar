package com.vocar.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryTest {
    private val terms = Dictionary.terms
    private val banned = listOf("추천", "매수", "매도", "유망", "사세요", "파세요", "수익률 보장", "반드시 오른")

    @Test
    fun `id는 서로 달라야 한다`() {
        assertEquals(terms.size, terms.map { it.id }.toSet().size)
    }

    @Test
    fun `모든 칸이 채워져 있고 영어 문장이 없다`() {
        terms.forEach { t ->
            listOf(t.id, t.name, t.topic, t.plain, t.example).forEach {
                assertTrue("${t.id}에 빈 칸이 있어요", it.isNotBlank())
            }
            listOf(t.plain, t.example).forEach { text ->
                assertFalse("${t.id}에 영어 문장이 있어요: $text", Regex("[A-Za-z]{3,} [A-Za-z]{3,}").containsMatchIn(text))
            }
        }
    }

    @Test
    fun `관련 개념은 실제로 존재하고 자기 자신이 아니다`() {
        terms.forEach { t ->
            assertTrue("${t.id}의 관련 개념이 비어 있어요", t.related.isNotEmpty())
            t.related.forEach { rid ->
                assertNotNull("${t.id}의 관련 개념 $rid 가 없어요", Dictionary.find(rid))
                assertNotEquals("${t.id}가 자기 자신을 가리켜요", t.id, rid)
            }
            assertEquals("${t.id}의 관련 개념이 겹쳐요", t.related.size, t.related.toSet().size)
        }
    }

    @Test
    fun `주제마다 개념이 있다`() {
        assertEquals(Dictionary.topics.size, Dictionary.topics.toSet().size)
        Dictionary.topics.forEach { assertTrue(Dictionary.byTopic(it).isNotEmpty()) }
        assertEquals(terms.size, Dictionary.topics.sumOf { Dictionary.byTopic(it).size })
    }

    @Test
    fun `수치와 투자 권유 표현은 넣지 않는다`() {
        terms.forEach { t ->
            listOf(t.name, t.plain, t.example).forEach { text ->
                assertFalse("${t.id}에 숫자가 있어요: $text", text.any { it.isDigit() })
                banned.forEach { w -> assertFalse("${t.id}에 '$w' 표현이 있어요", text.contains(w)) }
            }
        }
    }

    @Test
    fun `흐름 그림은 단계가 두 개 이상이다`() {
        terms.mapNotNull { it.diagram }.forEach { d ->
            assertTrue(d.caption.isNotBlank())
            if (d.kind == DiagramKind.FLOW) assertTrue(d.steps.size >= 2)
        }
    }
}
