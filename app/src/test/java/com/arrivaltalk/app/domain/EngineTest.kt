package com.arrivaltalk.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    private val day = 100L

    private fun good(item: SessionItem): Verdict = when {
        item.kind.graded -> Verdict.CORRECT
        item.kind == ActivityKind.RECALL -> Verdict.SELF_OK
        else -> Verdict.PRACTICED
    }

    /** 세션을 시작해 limit개까지 답한다. */
    private fun play(
        data: AppData,
        mode: Mode,
        length: SessionLength,
        onDay: Long,
        id: String,
        limit: Int = Int.MAX_VALUE,
        hint: Boolean = false,
        verdict: (SessionItem) -> Verdict = ::good,
    ): AppData {
        val (started, session) = Engine.startSession(data, mode, length, onDay, onDay * 86_400_000L, id)
        requireNotNull(session)
        var current = started
        session.items.take(limit).forEachIndexed { i, item ->
            current = Engine.answer(current, id, verdict(item), hint, false, onDay, onDay * 86_400_000L + i + 1)
        }
        return current
    }

    @Test
    fun content_has_twenty_checked_bundles() {
        assertEquals(20, Content.bundles.size)
        assertEquals(20, Content.bundles.map { it.id }.toSet().size)
        assertEquals(20, Content.bundles.map { it.en }.toSet().size)
        for (b in Content.bundles) {
            assertEquals("${b.id} 뜻 오답 2개", 2, b.wrongMeanings.size)
            assertEquals("${b.id} 표현 오답 2개", 2, b.wrongChoices.size)
            assertFalse("${b.id} 정답이 오답에 섞임", b.wrongChoices.any { it.en == b.en })
            assertFalse("${b.id} 뜻 정답이 오답에 섞임", b.ko in b.wrongMeanings)
            assertTrue("${b.id} 변형은 원문과 달라야 함", b.variationA.en != b.en && b.variationB.en != b.en)
            assertTrue("${b.id} 변형 두 개는 서로 달라야 함", b.variationA.en != b.variationB.en)
            assertTrue("${b.id} 허용 답안에 원문 포함", b.en in b.accepted)
        }
        assertEquals(20, Content.courseOrder(Goal.DAILY).size)
        assertEquals(20, Content.courseOrder(Goal.WORK).size)
    }

    @Test
    fun every_question_has_exactly_one_correct_choice() {
        for (b in Content.bundles) {
            for (kind in ActivityKind.entries) {
                for (choices in listOf(2, 3)) {
                    val session = Session(
                        "t", Mode.MORNING, SessionLength.THREE_MIN, day, 0,
                        listOf(SessionItem(b.id, kind, false)), 0, SessionStatus.IN_PROGRESS, 0, choices,
                    )
                    val q = Engine.question(session, 0)
                    assertNotNull(q)
                    q!!
                    if (kind.graded) {
                        assertEquals("${b.id} $kind 정답 1개", 1, q.choices.count { it.correct })
                        assertTrue("${b.id} $kind 보기 2~3개", q.choices.size in 2..3)
                        assertEquals("${b.id} $kind 보기 중복 없음", q.choices.size, q.choices.map { it.text }.toSet().size)
                        assertTrue(q.choices.filter { !it.correct }.all { it.explain.isNotBlank() })
                    } else {
                        assertTrue(q.choices.isEmpty())
                    }
                }
            }
        }
    }

    @Test
    fun first_morning_session_teaches_one_new_expression() {
        val (data, session) = Engine.startSession(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, 1, "a")
        assertNotNull(session)
        assertEquals(
            listOf(ActivityKind.LISTEN_MEANING, ActivityKind.SHADOW, ActivityKind.SITUATION, ActivityKind.VARIATION_A),
            session!!.items.map { it.kind },
        )
        assertEquals(setOf("w1d1"), session.items.map { it.bundleId }.toSet())
        assertEquals(Mode.MORNING, data.settings.lastMode)
    }

    @Test
    fun choosing_one_minute_once_does_not_change_the_default_length() {
        val (data, _) = Engine.startSession(AppData.EMPTY, Mode.MORNING, SessionLength.ONE_MIN, day, 1, "a")
        assertEquals(SessionLength.THREE_MIN, data.settings.length)
    }

    @Test
    fun one_minute_session_has_two_activities() {
        val (_, morning) = Engine.startSession(AppData.EMPTY, Mode.MORNING, SessionLength.ONE_MIN, day, 1, "a")
        assertEquals(listOf(ActivityKind.LISTEN_MEANING, ActivityKind.SHADOW), morning!!.items.map { it.kind })
    }

    @Test
    fun finishing_all_activities_completes_the_session() {
        val data = play(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, "a")
        val session = Engine.session(data, "a")!!
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(4, session.done)
        assertEquals(4, data.attempts.size)
        assertNull(Engine.resumable(data))
        assertTrue(Engine.stats(data, day).studiedToday)
    }

    @Test
    fun ending_early_keeps_saved_activities_and_can_resume_without_duplicates() {
        var data = play(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, "a", limit = 2)
        data = Engine.finishEarly(data, "a", 5)
        assertEquals(SessionStatus.PARTIAL, Engine.session(data, "a")!!.status)
        assertEquals(2, data.attempts.size)
        // 일부만 해도 유효 학습일이다.
        assertTrue(Engine.stats(data, day).studiedToday)

        val saved = Engine.resumable(data)
        assertEquals("a", saved?.id)
        assertEquals(2, saved?.done)

        data = Engine.resume(data, "a")
        val items = Engine.session(data, "a")!!.items
        data = Engine.answer(data, "a", good(items[2]), false, false, day, 6)
        data = Engine.answer(data, "a", good(items[3]), false, false, day, 7)
        // 끝난 세션에 한 번 더 제출해도 늘어나지 않는다.
        data = Engine.answer(data, "a", Verdict.CORRECT, false, false, day, 8)
        assertEquals(SessionStatus.COMPLETED, Engine.session(data, "a")!!.status)
        assertEquals(4, data.attempts.size)
        assertEquals(4, data.attempts.map { it.id }.toSet().size)
    }

    @Test
    fun session_with_nothing_saved_is_not_offered_for_resume() {
        val (data, _) = Engine.startSession(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, 1, "a")
        assertNull(Engine.resumable(Engine.finishEarly(data, "a", 2)))
    }

    @Test
    fun starting_again_closes_the_unfinished_session() {
        var data = play(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, "a", limit = 1)
        data = Engine.startSession(data, Mode.MORNING, SessionLength.ONE_MIN, day, 9, "b").first
        assertEquals(SessionStatus.PARTIAL, Engine.session(data, "a")!!.status)
        assertEquals(SessionStatus.IN_PROGRESS, Engine.session(data, "b")!!.status)
    }

    @Test
    fun second_session_on_the_same_day_does_not_advance_the_course() {
        var data = play(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, "a")
        data = play(data, Mode.MORNING, SessionLength.THREE_MIN, day, "b")
        assertEquals(setOf("w1d1"), data.mastery.keys)
    }

    @Test
    fun missed_days_do_not_skip_content() {
        val data = play(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, "a")
        assertEquals("w1d2", Engine.nextNew(data)?.id)
        val items = Engine.planItems(data, Mode.MORNING, SessionLength.THREE_MIN, day + 10)
        assertEquals("w1d2", items.first { !it.review }.bundleId)
    }

    @Test
    fun goal_changes_only_the_topic_order() {
        val work = AppData.EMPTY.copy(settings = Settings.DEFAULT.copy(goal = Goal.WORK))
        var data = play(work, Mode.MORNING, SessionLength.ONE_MIN, day, "a")
        for (i in 1..4) data = play(data, Mode.MORNING, SessionLength.ONE_MIN, day + i, "a$i")
        assertEquals("w2d1", Engine.nextNew(data)?.id)

        var daily = play(AppData.EMPTY, Mode.MORNING, SessionLength.ONE_MIN, day, "a")
        for (i in 1..4) daily = play(daily, Mode.MORNING, SessionLength.ONE_MIN, day + i, "a$i")
        assertEquals("w4d1", Engine.nextNew(daily)?.id)
    }

    @Test
    fun evening_recalls_what_was_learned_in_the_morning() {
        val data = play(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, "a")
        val three = Engine.planItems(data, Mode.EVENING, SessionLength.THREE_MIN, day)
        assertEquals(ActivityKind.RECALL, three.first().kind)
        assertEquals(4, three.size)
        assertEquals(setOf("w1d1"), three.map { it.bundleId }.toSet())
        val one = Engine.planItems(data, Mode.EVENING, SessionLength.ONE_MIN, day)
        assertEquals(listOf(ActivityKind.RECALL_CHOICE, ActivityKind.SHADOW_RECAP), one.map { it.kind })
        assertEquals(Mode.EVENING, Engine.defaultMode(data, day, 9))
    }

    @Test
    fun evening_works_without_a_morning_session() {
        val items = Engine.planItems(AppData.EMPTY, Mode.EVENING, SessionLength.THREE_MIN, day)
        assertEquals(ActivityKind.LISTEN_MEANING, items.first().kind)
        assertEquals(4, items.size)
        assertEquals(2, Engine.planItems(AppData.EMPTY, Mode.EVENING, SessionLength.ONE_MIN, day).size)
    }

    @Test
    fun learned_expression_comes_back_on_the_next_study_day() {
        val learned = play(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, "a")
        assertEquals(day + 1, learned.mastery.getValue("w1d1").nextReviewDay)
        // 같은 날에는 복습으로 다시 나오지 않는다.
        assertTrue(Engine.dueReviews(learned, day, null).isEmpty())

        val (_, next) = Engine.startSession(learned, Mode.MORNING, SessionLength.THREE_MIN, day + 1, 1, "b")
        val first = next!!.items.first()
        assertEquals(SessionItem("w1d1", ActivityKind.RECALL_CHOICE, true), first)
        assertEquals("w1d2", next.items[1].bundleId)
    }

    @Test
    fun recall_without_hint_stretches_the_interval_1_3_7_14() {
        var data = play(AppData.EMPTY, Mode.MORNING, SessionLength.ONE_MIN, day, "a")
        var at = day
        val expected = listOf(1L, 3L, 7L, 14L)
        expected.forEachIndexed { stage, gap ->
            at = data.mastery.getValue("w1d1").nextReviewDay
            val session = Session(
                "r$stage", Mode.EVENING, SessionLength.ONE_MIN, at, 0,
                listOf(SessionItem("w1d1", ActivityKind.RECALL_CHOICE, true)), 0, SessionStatus.IN_PROGRESS, 0, 2,
            )
            data = data.copy(sessions = data.sessions + session)
            data = Engine.answer(data, session.id, Verdict.CORRECT, false, false, at, at * 86_400_000L)
            val m = data.mastery.getValue("w1d1")
            assertEquals(stage + 1, m.stage)
            assertEquals(at + gap, m.nextReviewDay)
            assertEquals(at, m.recallConfirmedDay)
        }
        assertEquals(1, Engine.stats(data, at).recallConfirmed)
    }

    @Test
    fun hint_or_wrong_answer_does_not_count_as_recall() {
        val learned = play(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, "a")

        val hinted = play(learned, Mode.MORNING, SessionLength.THREE_MIN, day + 1, "b", limit = 1, hint = true)
        assertEquals(0, hinted.mastery.getValue("w1d1").stage)
        assertEquals(-1, hinted.mastery.getValue("w1d1").recallConfirmedDay)
        assertEquals(day + 2, hinted.mastery.getValue("w1d1").nextReviewDay)
        assertFalse(hinted.mastery.getValue("w1d1").needsWork)

        val wrong = play(learned, Mode.MORNING, SessionLength.THREE_MIN, day + 1, "b", limit = 1) { Verdict.WRONG }
        val m = wrong.mastery.getValue("w1d1")
        assertTrue(m.needsWork)
        assertEquals(day + 2, m.nextReviewDay)
        assertEquals(1, Engine.stats(wrong, day + 1).needsWork)
        // 오답 때문에 세션이 길어지지 않는다.
        assertEquals(4, Engine.session(wrong, "b")!!.items.size)
    }

    @Test
    fun self_check_is_kept_apart_from_graded_recall() {
        val learned = play(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, "a")
        val ok = play(learned, Mode.EVENING, SessionLength.THREE_MIN, day, "b", limit = 1)
        assertEquals(Verdict.SELF_OK, ok.attempts.last().verdict)
        assertEquals(-1, ok.mastery.getValue("w1d1").recallConfirmedDay)
        assertEquals(0, Engine.stats(ok, day).recallConfirmed)

        val hard = play(learned, Mode.EVENING, SessionLength.THREE_MIN, day, "b", limit = 1) { Verdict.SELF_HARD }
        assertTrue(hard.mastery.getValue("w1d1").needsWork)
    }

    @Test
    fun at_most_two_reviews_are_pulled_into_a_session() {
        var data = AppData.EMPTY
        for (i in 0 until 5) data = play(data, Mode.MORNING, SessionLength.ONE_MIN, day + i, "s$i")
        assertEquals(5, data.mastery.size)
        assertEquals(Engine.MAX_REVIEWS_PER_SESSION, Engine.dueReviews(data, day + 30, null).size)
        val items = Engine.planItems(data, Mode.MORNING, SessionLength.THREE_MIN, day + 30)
        assertTrue(items.count { it.review } <= Engine.MAX_REVIEWS_PER_SESSION)
    }

    @Test
    fun difficulty_follows_the_last_five_graded_answers() {
        var data = play(AppData.EMPTY, Mode.MORNING, SessionLength.THREE_MIN, day, "a")
        assertEquals(0, data.difficulty)
        data = play(data, Mode.MORNING, SessionLength.THREE_MIN, day + 1, "b")
        assertEquals(1, data.difficulty)
        val (_, harder) = Engine.startSession(data, Mode.MORNING, SessionLength.THREE_MIN, day + 2, 1, "c")
        assertEquals(3, harder!!.variationChoices)

        // 채점 문항 5개 중 2개 이하만 맞히면 한 단계 낮춘다.
        var falling = data
        var n = 0
        while (falling.gradedSinceAdjust != 0 || n == 0) {
            n++
            falling = play(falling, Mode.MORNING, SessionLength.THREE_MIN, day + 1 + n, "f$n") {
                if (it.kind.graded) Verdict.WRONG else good(it)
            }
            if (n > 5) break
        }
        assertEquals(0, falling.difficulty)
    }

    @Test
    fun week_counts_distinct_study_days_and_separates_partial_sessions() {
        assertEquals(3, Engine.weekdayIndex(0)) // 1970-01-01은 목요일
        val monday = day - Engine.weekdayIndex(day)
        var data = play(AppData.EMPTY, Mode.MORNING, SessionLength.ONE_MIN, monday, "a")
        data = play(data, Mode.EVENING, SessionLength.ONE_MIN, monday, "b")
        data = play(data, Mode.MORNING, SessionLength.THREE_MIN, monday + 2, "c", limit = 1)
        data = Engine.finishEarly(data, "c", 1)
        val stats = Engine.stats(data, monday + 2)
        assertEquals(2, stats.studiedDaysThisWeek)
        assertEquals(listOf(true, false, true, false, false, false, false), stats.week.map { it.studied })
        assertEquals(2, stats.sessionsCompletedThisWeek)
        assertEquals(1, stats.sessionsPartialThisWeek)
        assertEquals(0, Engine.stats(data, monday + 7).studiedDaysThisWeek)
    }
}
