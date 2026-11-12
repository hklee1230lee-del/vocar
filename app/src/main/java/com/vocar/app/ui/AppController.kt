package com.vocar.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vocar.app.audio.Speaker
import com.vocar.app.audio.VoiceState
import com.vocar.app.data.Store
import com.vocar.app.domain.ActivityKind
import com.vocar.app.domain.AppData
import com.vocar.app.domain.Choice
import com.vocar.app.domain.Engine
import com.vocar.app.domain.Goal
import com.vocar.app.domain.Level
import com.vocar.app.domain.Mode
import com.vocar.app.domain.Question
import com.vocar.app.domain.Session
import com.vocar.app.domain.SessionLength
import com.vocar.app.domain.Settings
import com.vocar.app.domain.Verdict
import com.vocar.app.domain.Arrival
import com.vocar.app.domain.Arrivals
import com.vocar.app.domain.Place
import com.vocar.app.vehicle.ParkingGate
import com.vocar.app.vehicle.TripEnd
import com.vocar.app.vehicle.TripMonitor
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

enum class Screen { ONBOARDING, HOME, LESSON, DONE }

enum class Tab(val label: String) {
    TODAY("오늘"),
    LIBRARY("표현 보관함"),
    RECORD("내 기록"),
    SETTINGS("설정"),
}

enum class Phase { ASK, FEEDBACK }

enum class PauseReason { GATE, AUDIO, BACKGROUND }

/**
 * 화면 상태와 학습 진행을 잇는다. 학습 규칙은 Engine(순수 함수)에 있고,
 * 여기서는 "언제 저장하고, 언제 멈추고, 어느 화면을 보여줄지"만 정한다.
 */
class AppController(
    private val store: Store,
    val gate: ParkingGate,
    val speaker: Speaker,
    private val trips: TripMonitor,
) {
    var data by mutableStateOf(AppData.EMPTY)
        private set
    var screen by mutableStateOf(Screen.HOME)
        private set
    var tab by mutableStateOf(Tab.TODAY)
    var mode by mutableStateOf(Mode.MORNING)
    var today by mutableStateOf(LocalDate.now().toEpochDay())
        private set

    /** 저장 실패는 실제 데이터 손실이 있을 때만 짧게 알린다. */
    var saveFailed by mutableStateOf(false)
        private set

    // ── 진행 중인 학습 ──
    var sessionId by mutableStateOf<String?>(null)
        private set

    /** 화면에 보이는 활동의 순번. 답을 저장한 뒤에도 피드백을 보여주는 동안은 그대로다. */
    var index by mutableStateOf(0)
        private set
    var phase by mutableStateOf(Phase.ASK)
        private set
    var picked by mutableStateOf<Choice?>(null)
        private set
    var hintShown by mutableStateOf(false)
        private set
    var revealed by mutableStateOf(false)
        private set
    var pause by mutableStateOf<PauseReason?>(null)
        private set
    var audioNote by mutableStateOf<String?>(null)
        private set

    /** 완료 화면이 보여줄 세션 */
    var finishedSessionId by mutableStateOf<String?>(null)
        private set

    val session: Session? get() = sessionId?.let { Engine.session(data, it) }
    val question: Question? get() = session?.let { Engine.question(it, index) }

    /** 무음으로 고른 경우뿐 아니라, 이 기기에서 음원을 낼 수 없는 경우도 글자로 진행한다. */
    val silent: Boolean get() = data.settings.silent || speaker.state == VoiceState.UNAVAILABLE

    /** 방금 도착해 아직 확정하지 않은 출퇴근 도착(있으면 홈에서 안내한다). */
    val pendingArrival: Arrival? get() = Arrivals.pending(data, clock)

    /** 화면이 "방금"을 다시 계산하도록 앞으로 나올 때마다 갱신하는 현재 시각 */
    private var clock by mutableStateOf(System.currentTimeMillis())

    private fun suggestedMode(): Mode =
        pendingArrival?.let { Arrivals.modeFor(Arrivals.placeOf(it)) }
            ?: Engine.defaultMode(data, today, LocalTime.now().hour)

    init {
        data = store.load()
        mode = suggestedMode()
        screen = if (data.settings.onboarded) Screen.HOME else Screen.ONBOARDING
    }

    private fun persist(next: AppData) {
        data = next
        saveFailed = !store.save(next)
    }

    private fun refreshDay() {
        today = LocalDate.now().toEpochDay()
        clock = System.currentTimeMillis()
    }

    // ── 앱 생명주기 ─────────────────────────────────────────

    fun onForeground() {
        refreshDay()
        if (screen == Screen.HOME && sessionId == null) mode = suggestedMode()
    }

    // ── 도착 감지 ──────────────────────────────────────────

    /** 주차가 확인되면, 앱이 꺼져 있던 사이에 주행이 있었는지 확인한다. */
    fun onParked() = trips.checkNow()

    /** 주행이 끝난 시점을 기록하고, 출근/퇴근 추정에 맞춰 오늘 할 학습을 바꿔 제안한다. */
    fun onTripEnd(trip: TripEnd) {
        val local = Instant.ofEpochMilli(trip.atMs).atZone(ZoneId.systemDefault())
        val before = data.arrivals.size
        val next = Arrivals.record(
            data,
            id = "a" + trip.atMs.toString(36),
            atMs = trip.atMs,
            day = local.toLocalDate().toEpochDay(),
            minuteOfDay = local.hour * 60 + local.minute,
            odometerKm = trip.odometerKm,
            tripKm = trip.tripKm,
            tripMinutes = trip.tripMinutes,
            approximate = trip.approximate,
        )
        if (next.arrivals.size == before) return
        persist(next)
        refreshDay()
        if (screen == Screen.HOME && sessionId == null) mode = suggestedMode()
    }

    /** 안내가 틀렸을 때 사용자가 바로잡는다. 이 값은 다음 추정의 기준이 된다. */
    fun confirmArrival(place: Place) {
        val arrival = pendingArrival ?: return
        persist(Arrivals.confirm(data, arrival.id, place))
        Arrivals.modeFor(place)?.let { mode = it }
    }

    /** 다른 앱으로 전환·화면 꺼짐: 재생을 멈추고, 돌아와도 자동으로 이어가지 않는다. */
    fun onBackground() {
        speaker.stop()
        if (screen == Screen.LESSON && pause == null) pause = PauseReason.BACKGROUND
    }

    fun onGateLost() {
        speaker.stop()
        if (screen == Screen.LESSON && pause == null) pause = PauseReason.GATE
    }

    fun onAudioInterrupted() {
        if (screen == Screen.LESSON && pause == null) pause = PauseReason.AUDIO
    }

    // ── 설정 ───────────────────────────────────────────────

    fun updateSettings(change: (Settings) -> Settings) {
        val next = change(data.settings)
        if (next.silent) speaker.stop()
        persist(data.copy(settings = next))
    }

    fun finishOnboarding(goal: Goal, level: Level, length: SessionLength, startNow: Boolean) {
        persist(
            data.copy(
                settings = data.settings.copy(onboarded = true, goal = goal, level = level, length = length),
                // 체감 수준은 변형 문제의 시작 난이도만 정한다. 이후에는 학습 결과로 보정된다.
                difficulty = if (level == Level.SHORT_SENTENCES) 1 else 0,
            ),
        )
        tab = Tab.TODAY
        screen = Screen.HOME
        if (startNow) start(length)
    }

    fun toggleFavorite(bundleId: String) = persist(Engine.toggleFavorite(data, bundleId))

    fun eraseEverything() {
        speaker.stop()
        store.clear()
        data = AppData.EMPTY
        saveFailed = false
        sessionId = null
        finishedSessionId = null
        pause = null
        tab = Tab.TODAY
        mode = suggestedMode()
        screen = Screen.ONBOARDING
    }

    // ── 학습 시작·재개 ──────────────────────────────────────

    fun start(length: SessionLength) {
        if (!gate.parked) return
        refreshDay()
        val now = System.currentTimeMillis()
        val id = "s" + now.toString(36) + Random.nextInt(100, 999)
        val (next, created) = Engine.startSession(data, mode, length, today, now, id)
        if (created == null) return
        // 도착 직후 고른 모드는 그 도착이 출근이었는지 퇴근이었는지의 정답이 된다.
        persist(Arrivals.confirmFromSession(next, mode, now))
        enter(created.id, 0)
    }

    fun resumeSaved() {
        if (!gate.parked) return
        val saved = Engine.resumable(data) ?: return
        persist(Engine.resume(data, saved.id))
        enter(saved.id, saved.done)
    }

    private fun enter(id: String, at: Int) {
        sessionId = id
        index = at
        pause = null
        finishedSessionId = null
        screen = Screen.LESSON
        resetQuestion()
        autoPlay()
    }

    private fun resetQuestion() {
        phase = Phase.ASK
        picked = null
        hintShown = false
        revealed = false
        audioNote = null
    }

    /** 듣기가 중심인 활동은 들어오자마자 한 번 들려준다. 사용자가 시작/다음을 누른 직후에만 호출된다. */
    private fun autoPlay() {
        val q = question ?: return
        val text = q.audioText ?: return
        val listenFirst = q.item.kind == ActivityKind.LISTEN_MEANING ||
            q.item.kind == ActivityKind.SHADOW ||
            q.item.kind == ActivityKind.SHADOW_RECAP
        if (listenFirst && !silent) listen(text, quiet = true)
    }

    // ── 문제 화면의 행동 ────────────────────────────────────

    private fun canAnswer(): Boolean {
        val s = session ?: return false
        return screen == Screen.LESSON && pause == null && phase == Phase.ASK && index == s.done
    }

    private fun commit(verdict: Verdict) {
        val id = sessionId ?: return
        refreshDay()
        persist(Engine.answer(data, id, verdict, hintShown, silent, today, System.currentTimeMillis()))
    }

    /** 선택 즉시 제출한다. 두 번 눌려도 한 번만 반영된다. */
    fun choose(choice: Choice) {
        if (!canAnswer()) return
        speaker.stop()
        picked = choice
        commit(if (choice.correct) Verdict.CORRECT else Verdict.WRONG)
        phase = Phase.FEEDBACK
    }

    fun showHint() {
        if (canAnswer()) hintShown = true
    }

    /** 문장 떠올리기: 생각해 본 뒤 정답을 본다. */
    fun reveal() {
        if (!canAnswer()) return
        revealed = true
        val text = question?.audioText
        if (text != null && !silent) listen(text, quiet = true)
    }

    /** 자가 확인은 연습 참여로 저장한다. 자동 채점과 섞지 않는다. */
    fun selfCheck(remembered: Boolean) {
        if (!canAnswer() || !revealed) return
        commit(if (remembered) Verdict.SELF_OK else Verdict.SELF_HARD)
        advance()
    }

    /** 따라 말하기: 마이크 없이, 연습했다고 직접 알려준다. */
    fun practiced() {
        if (!canAnswer()) return
        commit(Verdict.PRACTICED)
        advance()
    }

    fun next() {
        if (screen != Screen.LESSON || pause != null || phase != Phase.FEEDBACK) return
        advance()
    }

    private fun advance() {
        speaker.stop()
        val s = session
        if (s == null) {
            leaveLesson()
            return
        }
        if (s.done >= s.items.size) {
            finishedSessionId = s.id
            sessionId = null
            screen = Screen.DONE
        } else {
            index = s.done
            resetQuestion()
            autoPlay()
        }
    }

    /** 끝내기: 어느 단계에서든 한 번에. 확정된 활동은 이미 저장되어 있다. */
    fun finishNow() {
        speaker.stop()
        val s = session
        if (s == null) {
            leaveLesson()
            return
        }
        persist(Engine.finishEarly(data, s.id, System.currentTimeMillis()))
        sessionId = null
        pause = null
        if (s.done > 0) {
            finishedSessionId = s.id
            screen = Screen.DONE
        } else {
            leaveLesson()
        }
    }

    /** 멈춘 뒤에는 사용자가 직접 눌러야 이어간다. 주차가 다시 확인되어야 한다. */
    fun continueLesson() {
        if (!gate.parked || pause == null) return
        pause = null
        val s = session ?: return
        if (index == s.done) {
            // 답하지 않은 문제는 처음부터 다시 낸다.
            resetQuestion()
            autoPlay()
        }
    }

    fun listen(text: String, slow: Boolean = false, quiet: Boolean = false) {
        if (!gate.parked || pause != null) return
        val started = speaker.speak(text, slow || data.settings.slowAudio)
        audioNote = when {
            started -> null
            quiet && speaker.state == VoiceState.STARTING -> null
            else -> "지금은 소리를 재생할 수 없어요. 글자로 계속할 수 있어요."
        }
    }

    fun stopAudio() = speaker.stop()

    private fun leaveLesson() {
        sessionId = null
        pause = null
        tab = Tab.TODAY
        screen = Screen.HOME
    }

    fun closeDone(goTo: Tab = Tab.TODAY) {
        finishedSessionId = null
        tab = goTo
        refreshDay()
        mode = suggestedMode()
        screen = Screen.HOME
    }

    /** 시스템 뒤로 가기 */
    fun back(): Boolean = when (screen) {
        Screen.LESSON -> {
            finishNow()
            true
        }
        Screen.DONE -> {
            closeDone()
            true
        }
        Screen.HOME -> if (tab != Tab.TODAY) {
            tab = Tab.TODAY
            true
        } else false
        Screen.ONBOARDING -> false
    }
}
