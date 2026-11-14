package com.vocar.app.domain

/** 출근 모드(회사 주차 후 · 준비)와 퇴근 모드(집 주차 후 · 회상). 시계가 아니라 사용자가 고른다. */
enum class Mode { MORNING, EVENING }

/** 1분 한마디 = 활동 2개, 3분 기본 = 활동 4개. 시간은 예상치이며 초과해도 오답 처리하지 않는다. */
enum class SessionLength(val activityCount: Int, val minutes: Int) {
    ONE_MIN(2, 1),
    THREE_MIN(4, 3),
}

enum class Goal { DAILY, WORK }

enum class Level { BASIC, SHORT_SENTENCES }

enum class ActivityKind(val graded: Boolean) {
    /** 듣고 뜻 고르기 */
    LISTEN_MEANING(true),

    /** 따라 말하기 (마이크 없이, 연습 완료를 직접 선택) */
    SHADOW(false),

    /** 상황에 맞는 답 고르기 */
    SITUATION(true),

    /** 상황을 바꿔 적용하기 */
    VARIATION_A(true),
    VARIATION_B(true),

    /** 문장 떠올리기 · 자가 확인 */
    RECALL(false),

    /** 예문 없이 상황만 보고 고르는 회상 확인 (복습에도 사용) */
    RECALL_CHOICE(true),

    /** 다시 듣고 한 번 말하기 */
    SHADOW_RECAP(false),
}

enum class Verdict {
    CORRECT,
    WRONG,

    /** 자가 확인: 떠올렸어요 */
    SELF_OK,

    /** 자가 확인: 아직 어려워요 */
    SELF_HARD,

    /** 따라 말하기 연습 참여 */
    PRACTICED,
}

enum class SessionStatus { IN_PROGRESS, COMPLETED, PARTIAL }

data class WrongChoice(val en: String, val meaning: String)

data class Variation(val cue: String, val en: String, val ko: String)

/** 상황 → 핵심 표현 → 의미 확인 → 회상 → 변형 → 이후 복습을 묶은 학습 단위. */
data class Bundle(
    val id: String,
    val week: Int,
    val topic: String,
    /** 홈 추천 카드에 쓰는 한 줄. 영어 표현을 미리 보여주지 않는다. */
    val headline: String,
    val situation: String,
    val en: String,
    /** 직역보다 전달하려는 의도 */
    val ko: String,
    /** 음성 인식 도입 시 사용할 허용 답안 (정규화 전 원문) */
    val accepted: List<String>,
    val wrongMeanings: List<String>,
    val wrongChoices: List<WrongChoice>,
    /** 쓸 때의 주의점 한 가지 */
    val note: String,
    val variationA: Variation,
    val variationB: Variation,
    /** 예문을 보여주지 않는 회상 과제 */
    val recallCue: String,
)

data class SessionItem(val bundleId: String, val kind: ActivityKind, val review: Boolean)

data class Session(
    val id: String,
    val mode: Mode,
    val length: SessionLength,
    val day: Long,
    val startedAtMs: Long,
    val items: List<SessionItem>,
    /** 확정 저장된 활동 수. 이어하기는 이 위치에서 다시 시작한다. */
    val done: Int,
    val status: SessionStatus,
    val endedAtMs: Long,
    /** 변형 문제의 선택지 수 (난이도 규칙으로 2 또는 3) */
    val variationChoices: Int,
)

data class Attempt(
    /** 세션 ID + 활동 순번. 같은 ID는 한 번만 저장한다. */
    val id: String,
    val sessionId: String,
    val bundleId: String,
    val kind: ActivityKind,
    val day: Long,
    val atMs: Long,
    val verdict: Verdict,
    val hintUsed: Boolean,
    /** 무음 모드로 푼 문제는 듣기 평가로 집계하지 않는다. */
    val silent: Boolean,
    val review: Boolean,
    val contentVersion: Int,
)

data class Mastery(
    val bundleId: String,
    val introducedDay: Long,
    val introducedAtMs: Long,
    /** 힌트 없는 회상 성공 횟수에 따른 복습 단계 0..4 */
    val stage: Int,
    val nextReviewDay: Long,
    /** 힌트 없이 회상 확인을 통과한 가장 최근 날. 없으면 -1 */
    val recallConfirmedDay: Long,
    /** 다시 익힐 표현 */
    val needsWork: Boolean,
    val favorite: Boolean,
    val lastDay: Long,
)

data class Settings(
    val onboarded: Boolean,
    val goal: Goal,
    val level: Level,
    val length: SessionLength,
    /** 마지막으로 고른 모드. 아직 없으면 null */
    val lastMode: Mode?,
    val silent: Boolean,
    val slowAudio: Boolean,
    val weeklyGoalDays: Int,
) {
    companion object {
        val DEFAULT = Settings(
            onboarded = false,
            goal = Goal.DAILY,
            level = Level.BASIC,
            length = SessionLength.THREE_MIN,
            lastMode = null,
            silent = false,
            slowAudio = false,
            weeklyGoalDays = 3,
        )
    }
}

data class AppData(
    val settings: Settings,
    val mastery: Map<String, Mastery>,
    val attempts: List<Attempt>,
    val sessions: List<Session>,
    /** 변형 난이도 0..2 */
    val difficulty: Int,
    /** 마지막 난이도 조정 이후 채점된 문항 수 */
    val gradedSinceAdjust: Int,
    /** 주행이 끝난 시점(도착) 기록. 차량 신호로 자동으로 쌓인다. */
    val arrivals: List<Arrival> = emptyList(),
    /** 사용자가 집/회사로 확인한 내비 목적지의 키(이름+주소의 해시)와 그 값. 이름·주소·좌표는 저장하지 않는다. */
    val knownPlaces: Map<String, Place> = emptyMap(),
) {
    companion object {
        val EMPTY = AppData(Settings.DEFAULT, emptyMap(), emptyList(), emptyList(), 0, 0)
    }
}

data class Choice(val text: String, val correct: Boolean, val explain: String)

/** 화면이 그대로 그릴 수 있는 한 문제. */
data class Question(
    val item: SessionItem,
    val bundle: Bundle,
    val instruction: String,
    val prompt: String,
    /** 재생할 영어 음원 문장. 듣기가 없는 문제는 null */
    val audioText: String?,
    val choices: List<Choice>,
    val hint: String,
    val answerEn: String,
    val answerKo: String,
    val note: String,
)

data class Recommendation(
    val items: List<SessionItem>,
    val topic: String,
    val headline: String,
    val isNew: Boolean,
    val reviewCount: Int,
)

data class WeekDay(val day: Long, val label: String, val studied: Boolean, val isToday: Boolean)

data class Stats(
    val week: List<WeekDay>,
    val studiedDaysThisWeek: Int,
    val weeklyGoalDays: Int,
    val studiedToday: Boolean,
    val practicedExpressions: Int,
    val recallConfirmed: Int,
    val needsWork: Int,
    val sessionsCompletedThisWeek: Int,
    val sessionsPartialThisWeek: Int,
)

/** 도착한 곳. 위치를 쓰지 않고 주행 거리·시각·순서로 추정한다. */
enum class Place { WORK, HOME, OTHER }

/**
 * 주행이 끝난 시점 하나. 출근이 끝난 시점 = place가 WORK인 도착, 퇴근이 끝난 시점 = HOME인 도착.
 * 좌표·주소는 저장하지 않는다.
 */
data class Arrival(
    val id: String,
    val atMs: Long,
    val day: Long,
    /** 도착 시각(그날 0시부터의 분) */
    val minuteOfDay: Int,
    /** 누적 주행거리(km). 차량이 주지 않으면 null */
    val odometerKm: Float?,
    /** 이번 주행 거리(km). 알 수 없으면 null */
    val tripKm: Float?,
    val tripMinutes: Int?,
    val guess: Place,
    /** 학습된 출퇴근 패턴과 맞아떨어져 확실하다고 볼 때 true */
    val sure: Boolean,
    val reason: String,
    /** 사용자가 고르거나 학습을 시작해 확정한 값. 아직이면 null */
    val confirmed: Place?,
    /** 기어 변화를 직접 보지 못하고, 앱을 열었을 때 주행거리 변화로 알아챈 도착 */
    val approximate: Boolean,
    /** 내비 목적지 도착 이벤트로 알아챈 도착(기어 도착과 합쳐졌을 수 있다) */
    val viaNavi: Boolean = false,
)
