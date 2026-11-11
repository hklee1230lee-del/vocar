package com.arrivaltalk.app.domain

import kotlin.random.Random

/**
 * 화면·저장·통신에 의존하지 않는 규칙 기반 학습 엔진.
 * 모든 함수는 AppData를 받아 새 AppData를 돌려주는 순수 함수다.
 */
object Engine {
    /** 힌트 없이 회상에 성공할 때마다 1일 → 3일 → 7일 → 14일 뒤 복습 후보가 된다. */
    val INTERVALS = longArrayOf(1, 3, 7, 14)
    const val MAX_STAGE = 4
    const val MAX_REVIEWS_PER_SESSION = 2
    const val MAX_DIFFICULTY = 2
    private const val MAX_ATTEMPTS = 800
    private const val MAX_SESSIONS = 200
    private const val DIFFICULTY_WINDOW = 5

    // ── 추천·세션 구성 ────────────────────────────────────────

    /** 마지막 선택을 기본값으로 제안하되, 오늘 출근 학습을 이미 했다면 퇴근을 먼저 제안한다. */
    fun defaultMode(data: AppData, today: Long, hourOfDay: Int): Mode {
        val last = data.settings.lastMode ?: return if (hourOfDay < 15) Mode.MORNING else Mode.EVENING
        val morningToday = data.sessions.any { it.day == today && it.mode == Mode.MORNING && it.done > 0 }
        val eveningToday = data.sessions.any { it.day == today && it.mode == Mode.EVENING && it.done > 0 }
        return if (last == Mode.MORNING && morningToday && !eveningToday) Mode.EVENING else last
    }

    fun latestIntroduced(data: AppData): Bundle? =
        data.mastery.values.maxByOrNull { it.introducedAtMs }?.let { Content.find(it.bundleId) }

    fun introducedToday(data: AppData, today: Long): Bundle? =
        data.mastery.values.filter { it.introducedDay == today }
            .maxByOrNull { it.introducedAtMs }?.let { Content.find(it.bundleId) }

    /** 아직 시작하지 않은 다음 묶음. 결석일에도 진도를 자동으로 건너뛰지 않는다. */
    fun nextNew(data: AppData): Bundle? =
        Content.courseOrder(data.settings.goal).firstOrNull { it.id !in data.mastery }

    /** 복습 후보. 다시 익힐 표현을 먼저, 그다음 오래 밀린 순서. 세션당 최대 2개만 꺼낸다. */
    fun dueReviews(data: AppData, today: Long, excludeId: String?): List<Bundle> =
        data.mastery.values
            .filter { it.nextReviewDay <= today && it.introducedDay < today && it.bundleId != excludeId }
            .sortedWith(
                compareByDescending<Mastery> { it.needsWork }
                    .thenBy { it.nextReviewDay }
                    .thenBy { it.introducedAtMs },
            )
            .mapNotNull { Content.find(it.bundleId) }
            .take(MAX_REVIEWS_PER_SESSION)

    fun planItems(data: AppData, mode: Mode, length: SessionLength, today: Long): List<SessionItem> {
        val todays = introducedToday(data, today)
        val fresh = nextNew(data)
        val easierVar = if (data.difficulty >= MAX_DIFFICULTY) ActivityKind.VARIATION_B else ActivityKind.VARIATION_A
        val harderVar = if (data.difficulty >= MAX_DIFFICULTY) ActivityKind.VARIATION_A else ActivityKind.VARIATION_B

        fun item(b: Bundle, kind: ActivityKind, review: Boolean = false) = SessionItem(b.id, kind, review)

        // 새 표현: (복습 1개 +) 듣고 이해하기 → 따라 말하기 → 상황 적용
        fun newLesson(b: Bundle): List<SessionItem> {
            if (length == SessionLength.ONE_MIN) {
                return listOf(item(b, ActivityKind.LISTEN_MEANING), item(b, ActivityKind.SHADOW))
            }
            val review = dueReviews(data, today, b.id).firstOrNull()
            return if (review != null) {
                listOf(
                    item(review, ActivityKind.RECALL_CHOICE, review = true),
                    item(b, ActivityKind.LISTEN_MEANING),
                    item(b, ActivityKind.SHADOW),
                    item(b, easierVar),
                )
            } else {
                listOf(
                    item(b, ActivityKind.LISTEN_MEANING),
                    item(b, ActivityKind.SHADOW),
                    item(b, ActivityKind.SITUATION),
                    item(b, easierVar),
                )
            }
        }

        // 오늘 배운 표현 회상: 떠올리기 → 상황 바꾸기 → 이전 표현 복습 → 다시 말하기
        fun recallLesson(b: Bundle): List<SessionItem> {
            if (length == SessionLength.ONE_MIN) {
                return listOf(item(b, ActivityKind.RECALL_CHOICE), item(b, ActivityKind.SHADOW_RECAP))
            }
            val review = dueReviews(data, today, b.id).firstOrNull()
            return listOf(
                item(b, ActivityKind.RECALL),
                item(b, harderVar),
                if (review != null) item(review, ActivityKind.RECALL_CHOICE, review = true) else item(b, easierVar),
                item(b, ActivityKind.SHADOW_RECAP),
            )
        }

        // 새 묶음이 없을 때(코스 완료) 또는 짧은 퇴근 복습
        fun reviewOnly(): List<SessionItem> {
            val due = dueReviews(data, today, null)
            val rest = data.mastery.values
                .filter { m -> due.none { it.id == m.bundleId } }
                .sortedWith(compareBy<Mastery> { it.lastDay }.thenBy { it.introducedAtMs })
                .mapNotNull { Content.find(it.bundleId) }
            val pool = due + rest
            val first = pool.firstOrNull() ?: return emptyList()
            val second = pool.getOrNull(1)
            if (length == SessionLength.ONE_MIN) {
                return listOf(
                    item(first, ActivityKind.RECALL_CHOICE, review = true),
                    item(first, ActivityKind.SHADOW_RECAP, review = true),
                )
            }
            return if (second != null) {
                listOf(
                    item(first, ActivityKind.RECALL_CHOICE, review = true),
                    item(first, ActivityKind.SHADOW_RECAP, review = true),
                    item(second, ActivityKind.RECALL_CHOICE, review = true),
                    item(second, harderVar, review = true),
                )
            } else {
                listOf(
                    item(first, ActivityKind.RECALL_CHOICE, review = true),
                    item(first, easierVar, review = true),
                    item(first, harderVar, review = true),
                    item(first, ActivityKind.SHADOW_RECAP, review = true),
                )
            }
        }

        return when (mode) {
            Mode.MORNING -> {
                val target = todays ?: fresh
                if (target != null) newLesson(target) else reviewOnly()
            }
            Mode.EVENING -> when {
                // 오늘 출근 학습이 있으면 그 표현을 다시 꺼낸다.
                todays != null -> recallLesson(todays)
                // 출근 학습 없이도 완결된다: 1분은 밀린 복습을 먼저, 없으면 새 표현을 독립 학습한다.
                length == SessionLength.ONE_MIN && dueReviews(data, today, null).isNotEmpty() -> reviewOnly()
                fresh != null -> newLesson(fresh)
                else -> reviewOnly()
            }
        }
    }

    fun recommend(data: AppData, mode: Mode, length: SessionLength, today: Long): Recommendation? {
        val items = planItems(data, mode, length, today)
        if (items.isEmpty()) return null
        val primary = (items.firstOrNull { !it.review } ?: items.first()).let { Content.find(it.bundleId) }
            ?: return null
        val isNew = items.any { !it.review && it.kind == ActivityKind.LISTEN_MEANING }
        val recallsToday = items.any {
            !it.review && (it.kind == ActivityKind.RECALL || it.kind == ActivityKind.RECALL_CHOICE)
        }
        val headline = when {
            isNew -> primary.headline
            recallsToday -> "오늘 배운 표현, 한 번 떠올려볼까요?"
            else -> "배운 표현을 다시 꺼내 볼까요?"
        }
        return Recommendation(
            items = items,
            topic = primary.topic,
            headline = headline,
            isNew = isNew,
            reviewCount = items.count { it.review && it.kind == ActivityKind.RECALL_CHOICE },
        )
    }

    // ── 세션 진행 ─────────────────────────────────────────────

    fun startSession(
        data: AppData,
        mode: Mode,
        length: SessionLength,
        today: Long,
        nowMs: Long,
        sessionId: String,
    ): Pair<AppData, Session?> {
        val items = planItems(data, mode, length, today)
        if (items.isEmpty()) return data to null
        val session = Session(
            id = sessionId,
            mode = mode,
            length = length,
            day = today,
            startedAtMs = nowMs,
            items = items,
            done = 0,
            status = SessionStatus.IN_PROGRESS,
            endedAtMs = 0,
            variationChoices = if (data.difficulty == 0) 2 else 3,
        )
        // 끝내지 않은 이전 세션은 부분 종료로 정리한다. 이미 저장된 활동은 그대로 남는다.
        val closed = data.sessions.map {
            if (it.status == SessionStatus.IN_PROGRESS) it.copy(status = SessionStatus.PARTIAL, endedAtMs = nowMs) else it
        }
        val next = data.copy(
            // 기본 길이는 설정에서만 바꾼다. '1분만 하기'는 이번 한 번에만 적용된다.
            settings = data.settings.copy(lastMode = mode),
            sessions = (closed + session).takeLast(MAX_SESSIONS),
        )
        return next to session
    }

    /** 이어할 수 있는 세션: 가장 최근 세션이 끝나지 않았고 저장된 활동이 1개 이상 있을 때. */
    fun resumable(data: AppData): Session? {
        val last = data.sessions.lastOrNull() ?: return null
        return if (last.status != SessionStatus.COMPLETED && last.done > 0 && last.done < last.items.size) last else null
    }

    fun resume(data: AppData, sessionId: String): AppData =
        data.copy(
            sessions = data.sessions.map {
                if (it.id == sessionId && it.status == SessionStatus.PARTIAL) {
                    it.copy(status = SessionStatus.IN_PROGRESS, endedAtMs = 0)
                } else it
            },
        )

    fun session(data: AppData, sessionId: String): Session? = data.sessions.firstOrNull { it.id == sessionId }

    /**
     * 현재 활동의 결과를 확정한다. 같은 활동을 두 번 제출해도 한 번만 반영된다.
     * 기록·복습 일정·난이도를 한 번에 갱신한 새 상태를 돌려준다.
     */
    fun answer(
        data: AppData,
        sessionId: String,
        verdict: Verdict,
        hintUsed: Boolean,
        silent: Boolean,
        today: Long,
        nowMs: Long,
    ): AppData {
        val session = session(data, sessionId) ?: return data
        if (session.done >= session.items.size) return data
        val attemptId = "$sessionId#${session.done}"
        if (data.attempts.any { it.id == attemptId }) return data
        val item = session.items[session.done]
        val attempt = Attempt(
            id = attemptId,
            sessionId = sessionId,
            bundleId = item.bundleId,
            kind = item.kind,
            day = today,
            atMs = nowMs,
            verdict = verdict,
            hintUsed = hintUsed,
            silent = silent,
            review = item.review,
            contentVersion = Content.CONTENT_VERSION,
        )
        val done = session.done + 1
        val finished = done >= session.items.size
        val updatedSession = session.copy(
            done = done,
            status = if (finished) SessionStatus.COMPLETED else SessionStatus.IN_PROGRESS,
            endedAtMs = if (finished) nowMs else 0,
        )
        val attempts = (data.attempts + attempt).takeLast(MAX_ATTEMPTS)
        val mastery = data.mastery + (item.bundleId to updateMastery(data.mastery[item.bundleId], attempt))

        var difficulty = data.difficulty
        var gradedSince = data.gradedSinceAdjust
        if (item.kind.graded) {
            gradedSince += 1
            if (gradedSince >= DIFFICULTY_WINDOW) {
                val window = attempts.filter { it.kind.graded }.takeLast(DIFFICULTY_WINDOW)
                val solved = window.count { it.verdict == Verdict.CORRECT && !it.hintUsed }
                if (solved >= 4) difficulty = minOf(difficulty + 1, MAX_DIFFICULTY)
                if (solved <= 2) difficulty = maxOf(difficulty - 1, 0)
                gradedSince = 0
            }
        }
        return data.copy(
            mastery = mastery,
            attempts = attempts,
            sessions = data.sessions.map { if (it.id == sessionId) updatedSession else it },
            difficulty = difficulty,
            gradedSinceAdjust = gradedSince,
        )
    }

    private fun updateMastery(old: Mastery?, a: Attempt): Mastery {
        val base = old ?: Mastery(
            bundleId = a.bundleId,
            introducedDay = a.day,
            introducedAtMs = a.atMs,
            stage = 0,
            // 처음 배운 표현은 다음 학습일에 다시 제시한다.
            nextReviewDay = a.day + 1,
            recallConfirmedDay = -1,
            needsWork = false,
            favorite = false,
            lastDay = a.day,
        )
        val touched = base.copy(lastDay = a.day)
        return when (a.verdict) {
            Verdict.CORRECT ->
                if (a.kind == ActivityKind.RECALL_CHOICE) {
                    if (a.hintUsed) {
                        // 힌트를 쓴 성공은 실패가 아니지만 간격을 늘리지는 않는다.
                        touched.copy(nextReviewDay = a.day + 1)
                    } else {
                        val stage = minOf(touched.stage + 1, MAX_STAGE)
                        touched.copy(
                            stage = stage,
                            nextReviewDay = a.day + INTERVALS[stage - 1],
                            recallConfirmedDay = a.day,
                            needsWork = false,
                        )
                    }
                } else touched
            // 오답은 정답을 확인한 뒤 다음 학습일에 다시 낸다. 세션을 늘리지 않는다.
            Verdict.WRONG -> touched.copy(
                stage = if (a.kind == ActivityKind.RECALL_CHOICE) maxOf(touched.stage - 1, 0) else touched.stage,
                nextReviewDay = a.day + 1,
                needsWork = true,
            )
            // 자가 확인은 연습 참여로만 남긴다. 자동 채점 결과와 섞지 않는다.
            Verdict.SELF_OK -> touched
            Verdict.SELF_HARD -> touched.copy(nextReviewDay = a.day + 1, needsWork = true)
            Verdict.PRACTICED -> touched
        }
    }

    /** 끝내기: 저장된 활동은 유지하고 세션만 부분 종료로 표시한다. */
    fun finishEarly(data: AppData, sessionId: String, nowMs: Long): AppData =
        data.copy(
            sessions = data.sessions.map {
                if (it.id == sessionId && it.status == SessionStatus.IN_PROGRESS) {
                    it.copy(
                        status = if (it.done >= it.items.size) SessionStatus.COMPLETED else SessionStatus.PARTIAL,
                        endedAtMs = nowMs,
                    )
                } else it
            },
        )

    fun toggleFavorite(data: AppData, bundleId: String): AppData {
        val m = data.mastery[bundleId] ?: return data
        return data.copy(mastery = data.mastery + (bundleId to m.copy(favorite = !m.favorite)))
    }

    // ── 문제 만들기 ───────────────────────────────────────────

    fun question(session: Session, index: Int): Question? {
        val item = session.items.getOrNull(index) ?: return null
        val b = Content.find(item.bundleId) ?: return null
        val random = Random(session.id.hashCode() * 31 + index)

        fun englishChoices(): List<Choice> = (
            listOf(Choice(b.en, true, "")) +
                b.wrongChoices.map { Choice(it.en, false, "‘${it.en}’는 ${it.meaning}이에요.") }
            ).shuffled(random)

        fun variation(v: Variation, other: Variation): Question {
            val wrong = mutableListOf(Choice(b.en, false, "‘${b.en}’는 ‘${b.ko}’라는 뜻이에요."))
            if (session.variationChoices >= 3) {
                wrong += Choice(other.en, false, "‘${other.en}’는 ‘${other.ko}’라는 뜻이에요.")
            }
            return Question(
                item = item, bundle = b,
                instruction = "상황이 바뀌었어요. 뭐라고 말할까요?",
                prompt = v.cue,
                audioText = null,
                choices = (listOf(Choice(v.en, true, "")) + wrong).shuffled(random),
                hint = "찾는 말의 뜻: ${v.ko}",
                answerEn = v.en, answerKo = v.ko,
                note = "배운 표현 ‘${b.en}’에서 한 부분만 바꿨어요.",
            )
        }

        return when (item.kind) {
            ActivityKind.LISTEN_MEANING -> Question(
                item = item, bundle = b,
                instruction = "듣고 뜻을 골라요",
                prompt = b.situation,
                audioText = b.en,
                choices = (
                    listOf(Choice(b.ko, true, "")) +
                        b.wrongMeanings.map { Choice(it, false, "이 표현은 ‘${b.ko}’라는 뜻이에요.") }
                    ).shuffled(random),
                hint = b.en,
                answerEn = b.en, answerKo = b.ko, note = b.note,
            )
            ActivityKind.SITUATION -> Question(
                item = item, bundle = b,
                instruction = "이럴 때 뭐라고 말할까요?",
                prompt = b.situation,
                audioText = null,
                choices = englishChoices(),
                hint = "찾는 말의 뜻: ${b.ko}",
                answerEn = b.en, answerKo = b.ko, note = b.note,
            )
            ActivityKind.RECALL_CHOICE -> Question(
                item = item, bundle = b,
                instruction = if (item.review) "전에 배운 표현이에요. 기억나나요?" else "오늘 배운 표현, 기억나나요?",
                prompt = b.recallCue,
                audioText = null,
                choices = englishChoices(),
                hint = "찾는 말의 뜻: ${b.ko}",
                answerEn = b.en, answerKo = b.ko, note = b.note,
            )
            ActivityKind.VARIATION_A -> variation(b.variationA, b.variationB)
            ActivityKind.VARIATION_B -> variation(b.variationB, b.variationA)
            ActivityKind.RECALL -> Question(
                item = item, bundle = b,
                instruction = "보기 없이 먼저 떠올려 봐요",
                prompt = b.recallCue,
                audioText = b.en,
                choices = emptyList(),
                hint = b.en.split(" ").take(2).joinToString(" ") + " …",
                answerEn = b.en, answerKo = b.ko, note = b.note,
            )
            ActivityKind.SHADOW -> Question(
                item = item, bundle = b,
                instruction = "듣고 따라 말해요",
                prompt = b.situation,
                audioText = b.en,
                choices = emptyList(),
                hint = "",
                answerEn = b.en, answerKo = b.ko, note = b.note,
            )
            ActivityKind.SHADOW_RECAP -> Question(
                item = item, bundle = b,
                instruction = "한 번 더 듣고 말해요",
                prompt = b.situation,
                audioText = b.en,
                choices = emptyList(),
                hint = "",
                answerEn = b.en, answerKo = b.ko, note = b.note,
            )
        }
    }

    // ── 기록 ─────────────────────────────────────────────────

    /** 월요일을 0으로 하는 요일 번호. epochDay 0(1970-01-01)은 목요일이다. */
    fun weekdayIndex(day: Long): Int = (((day + 3) % 7 + 7) % 7).toInt()

    fun stats(data: AppData, today: Long): Stats {
        val weekStart = today - weekdayIndex(today)
        val labels = listOf("월", "화", "수", "목", "금", "토", "일")
        // 유효 학습일: 응답이나 자가 회상 활동을 1개 이상 저장한 날. 세션 완료와 따로 센다.
        val studiedDays = data.attempts.map { it.day }.toSet()
        val week = (0 until 7).map { i ->
            val d = weekStart + i
            WeekDay(d, labels[i], d in studiedDays, d == today)
        }
        val weekSessions = data.sessions.filter { it.day >= weekStart && it.day < weekStart + 7 }
        return Stats(
            week = week,
            studiedDaysThisWeek = week.count { it.studied },
            weeklyGoalDays = data.settings.weeklyGoalDays,
            studiedToday = today in studiedDays,
            practicedExpressions = data.mastery.size,
            recallConfirmed = data.mastery.values.count { it.recallConfirmedDay >= 0 },
            needsWork = data.mastery.values.count { it.needsWork },
            sessionsCompletedThisWeek = weekSessions.count { it.status == SessionStatus.COMPLETED },
            sessionsPartialThisWeek = weekSessions.count { it.status != SessionStatus.COMPLETED && it.done > 0 },
        )
    }
}
