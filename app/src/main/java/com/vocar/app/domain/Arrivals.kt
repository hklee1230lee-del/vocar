package com.vocar.app.domain

import kotlin.math.abs

/**
 * 출근·퇴근이 끝난 시점을 알아내는 규칙. 위치(GPS)나 내비 목적지를 쓰지 않고
 * Vehicle SDK로 받을 수 있는 값만 쓴다: 기어가 P가 된 시각, 이번 주행 거리, 누적 주행거리.
 *
 * 1) 평소 출퇴근 거리와 비슷한가  2) 오늘 출근 뒤의 도착인가(순서)  3) 평소·일반적인 도착 시간대인가
 * 사용자가 그 도착 뒤에 출근/퇴근 학습을 시작하면 그 값을 정답으로 삼아 다음 추정에 쓴다.
 */
object Arrivals {
    const val MAX = 120

    /** 이만큼도 움직이지 않았으면 같은 자리에서의 주차 조작으로 본다. */
    const val SAME_STOP_KM = 0.3f
    const val SAME_STOP_MS = 5 * 60_000L

    /** 내비 도착과 기어 도착은 같은 주행의 두 신호다. 이 시간 안에 서로 다른 신호가 오면 하나로 합친다. */
    const val MERGE_MS = 20 * 60_000L

    /** 도착 뒤 이 시간 안에만 "방금 도착"으로 안내한다. */
    const val PENDING_MS = 45 * 60_000L
    private const val SHORT_TRIP_KM = 1.0f
    private const val MORNING_END = 11 * 60
    private const val EVENING_START = 16 * 60

    data class Guess(val place: Place, val sure: Boolean, val reason: String)

    fun placeOf(a: Arrival): Place = a.confirmed ?: a.guess

    fun modeFor(place: Place): Mode? = when (place) {
        Place.WORK -> Mode.MORNING
        Place.HOME -> Mode.EVENING
        Place.OTHER -> null
    }

    /**
     * 내비 목적지 이름이 집·회사로 저장된 이름과 정확히 같을 때만 알아본다.
     * "집밥", "돼지집"처럼 이름에 글자가 들어 있을 뿐인 장소는 건드리지 않는다. 주소·좌표는 쓰지도 저장하지도 않는다.
     */
    fun placeFromName(name: String?): Place? = when (name?.filterNot { it.isWhitespace() }?.lowercase()) {
        "집", "우리집", "내집", "home" -> Place.HOME
        "회사", "우리회사", "직장", "사무실", "office", "work" -> Place.WORK
        else -> null
    }

    private fun median(values: List<Float>): Float? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        return sorted[sorted.size / 2]
    }

    private fun near(tripKm: Float?, legKm: Float?): Boolean =
        tripKm != null && legKm != null && abs(tripKm - legKm) <= maxOf(1.0f, legKm * 0.25f)

    fun classify(history: List<Arrival>, day: Long, minuteOfDay: Int, tripKm: Float?): Guess {
        val known = history.filter { it.confirmed == Place.WORK || it.confirmed == Place.HOME }
        fun leg(place: Place) = median(known.filter { it.confirmed == place }.mapNotNull { it.tripKm })
        fun usual(place: Place) = median(known.filter { it.confirmed == place }.map { it.minuteOfDay.toFloat() })

        val legWork = leg(Place.WORK)
        val legHome = leg(Place.HOME)
        val likeWork = near(tripKm, legWork)
        val likeHome = near(tripKm, legHome)
        val commuteLike = likeWork || likeHome
        val learnedDistance = legWork != null || legHome != null

        // 평소 거리를 아는데 전혀 다르거나, 아주 짧은 이동이면 출퇴근으로 보지 않는다.
        if (tripKm != null && !commuteLike && (learnedDistance || tripKm < SHORT_TRIP_KM)) {
            val why = if (tripKm < SHORT_TRIP_KM) "아주 짧은 이동이에요" else "평소 출퇴근 거리와 달라요"
            return Guess(Place.OTHER, false, why)
        }

        // 순서: 오늘 회사에 도착한 뒤의 도착은 집, 집 다음 도착은 회사.
        val last = history.lastOrNull { placeOf(it) != Place.OTHER }
        val expected = when {
            last == null -> null
            placeOf(last) == Place.WORK && last.day == day -> Place.HOME
            placeOf(last) == Place.HOME -> Place.WORK
            else -> null
        }

        // 시간: 배운 도착 시각이 있으면 그것과, 없으면 일반적인 아침·저녁으로.
        val usualWork = usual(Place.WORK)
        val usualHome = usual(Place.HOME)
        fun gap(usual: Float?) = if (usual == null) null else abs(minuteOfDay - usual)
        val gapWork = gap(usualWork)
        val gapHome = gap(usualHome)
        val learnedTime = when {
            gapWork != null && gapWork <= 120 && (gapHome == null || gapWork < gapHome) -> Place.WORK
            gapHome != null && gapHome <= 120 && (gapWork == null || gapHome < gapWork) -> Place.HOME
            else -> null
        }
        val clockTime = when {
            minuteOfDay < MORNING_END -> Place.WORK
            minuteOfDay >= EVENING_START -> Place.HOME
            else -> null
        }

        fun score(place: Place): Int {
            var s = 0
            if (expected == place) s += 2
            if (learnedTime == place) s += 2 else if (learnedTime == null && clockTime == place) s += 1
            val other = if (place == Place.WORK) likeHome else likeWork
            val mine = if (place == Place.WORK) likeWork else likeHome
            if (mine && !other) s += 2
            return s
        }

        val work = score(Place.WORK)
        val home = score(Place.HOME)
        if (work == 0 && home == 0) return Guess(Place.OTHER, false, "출퇴근으로 보기 어려운 시간이에요")
        val place = when {
            work > home -> Place.WORK
            home > work -> Place.HOME
            else -> expected ?: clockTime ?: Place.OTHER
        }
        if (place == Place.OTHER) return Guess(Place.OTHER, false, "출퇴근으로 보기 어려워요")

        val reasons = ArrayList<String>()
        if (commuteLike) reasons += "평소 출퇴근 거리"
        if (expected == place) reasons += if (place == Place.HOME) "오늘 출근 뒤 도착" else "집에서 출발한 뒤 도착"
        if (learnedTime == place) reasons += "평소 도착 시간"
        else if (clockTime == place) reasons += if (place == Place.WORK) "아침 시간" else "저녁 시간"
        val sure = commuteLike && known.size >= 2 && maxOf(work, home) >= 4
        return Guess(place, sure, reasons.joinToString(" · "))
    }

    /** 주행이 끝난 시점을 기록한다. 같은 자리에서의 반복(주차 조작, 앱을 다시 연 경우)은 무시한다. */
    fun record(
        data: AppData,
        id: String,
        atMs: Long,
        day: Long,
        minuteOfDay: Int,
        odometerKm: Float?,
        tripKm: Float?,
        tripMinutes: Int?,
        approximate: Boolean,
        hint: Place? = null,
        viaNavi: Boolean = false,
    ): AppData {
        val last = data.arrivals.lastOrNull()
        val hinted = hint?.takeIf { it != Place.OTHER }?.let {
            Guess(it, true, "내비 목적지가 ${if (it == Place.HOME) "집" else "회사"}로 저장돼 있어요")
        }
        if (last != null && !approximate && last.viaNavi != viaNavi && atMs - last.atMs in 0..MERGE_MS) {
            // 내비가 직접 준 주행 거리·시간이 주행거리 차이로 추정한 값보다 믿을 만하다.
            val trip = tripKm?.takeIf { it > 0.05f }
            val merged = last.copy(
                odometerKm = last.odometerKm ?: odometerKm,
                tripKm = if (viaNavi) trip ?: last.tripKm else last.tripKm ?: trip,
                tripMinutes = if (viaNavi) tripMinutes ?: last.tripMinutes else last.tripMinutes ?: tripMinutes,
                approximate = false,
                viaNavi = true,
            )
            val better = if (hinted != null && last.confirmed == null)
                merged.copy(guess = hinted.place, sure = true, reason = hinted.reason) else merged
            return data.copy(arrivals = data.arrivals.dropLast(1) + better)
        }
        val moved = if (last?.odometerKm != null && odometerKm != null) abs(odometerKm - last.odometerKm) else null
        if (last != null) {
            if (approximate) {
                // 기어 변화를 못 본 경우에는 주행거리가 늘었을 때만 새 도착으로 본다.
                if (moved == null || moved < SAME_STOP_KM) return data
            } else if (atMs - last.atMs < SAME_STOP_MS && (moved == null || moved < SAME_STOP_KM)) {
                return data
            }
        } else if (approximate && odometerKm == null) {
            return data
        }
        // 이번 주행 거리: 트립 값이 없으면 지난 도착 이후 늘어난 누적 거리로 대신한다. 0은 "모름"으로 둔다.
        val distance = (tripKm?.takeIf { it > 0.05f } ?: moved)?.takeIf { it > 0.05f }
        val guess = hinted ?: classify(data.arrivals, day, minuteOfDay, distance)
        val arrival = Arrival(
            id, atMs, day, minuteOfDay, odometerKm, distance, tripMinutes,
            guess.place, guess.sure, guess.reason, null, approximate, viaNavi,
        )
        return data.copy(arrivals = (data.arrivals + arrival).takeLast(MAX))
    }

    /** 방금 도착해서 아직 확정하지 않은 출퇴근 도착. 홈 화면의 안내와 모드 추천에 쓴다. */
    fun pending(data: AppData, nowMs: Long): Arrival? {
        val last = data.arrivals.lastOrNull() ?: return null
        val fresh = nowMs - last.atMs in 0..PENDING_MS
        return if (fresh && last.confirmed == null && last.guess != Place.OTHER) last else null
    }

    fun confirm(data: AppData, id: String, place: Place): AppData =
        data.copy(arrivals = data.arrivals.map { if (it.id == id) it.copy(confirmed = place) else it })

    /** 도착 직후 사용자가 출근/퇴근 학습을 시작하면 그 선택을 도착지의 정답으로 삼는다. */
    fun confirmFromSession(data: AppData, mode: Mode, nowMs: Long): AppData {
        val last = data.arrivals.lastOrNull() ?: return data
        if (last.confirmed != null || nowMs - last.atMs !in 0..PENDING_MS) return data
        return confirm(data, last.id, if (mode == Mode.MORNING) Place.WORK else Place.HOME)
    }

    /** 오늘 출근이 끝난 시점, 퇴근이 끝난 시점. */
    fun today(data: AppData, day: Long): Pair<Arrival?, Arrival?> {
        val todays = data.arrivals.filter { it.day == day }
        return todays.firstOrNull { placeOf(it) == Place.WORK } to todays.lastOrNull { placeOf(it) == Place.HOME }
    }
}
