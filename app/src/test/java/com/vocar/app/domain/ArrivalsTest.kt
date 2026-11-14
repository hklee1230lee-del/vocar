package com.vocar.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalsTest {
    private val day = 200L
    private fun ms(d: Long, minute: Int) = d * 86_400_000L + minute * 60_000L

    /** 도착 하나를 기록한다. odo는 누적 주행거리(km). */
    private fun arrive(
        data: AppData,
        d: Long,
        minute: Int,
        odo: Float?,
        trip: Float? = null,
        approximate: Boolean = false,
    ): AppData = Arrivals.record(data, "a$d-$minute", ms(d, minute), d, minute, odo, trip, null, approximate)

    /** 며칠 동안 출근(08:40, 12km)·퇴근(18:50, 12km)을 하고, 매번 학습으로 확정한 상태 */
    private fun commuter(days: Int): AppData {
        var data = AppData.EMPTY
        var odo = 1000f
        for (i in 0 until days) {
            odo += 12f
            data = arrive(data, day + i, 8 * 60 + 40, odo)
            data = Arrivals.confirmFromSession(data, Mode.MORNING, ms(day + i, 8 * 60 + 42))
            odo += 12f
            data = arrive(data, day + i, 18 * 60 + 50, odo)
            data = Arrivals.confirmFromSession(data, Mode.EVENING, ms(day + i, 18 * 60 + 55))
        }
        return data
    }

    @Test
    fun first_arrivals_are_guessed_from_the_clock_and_marked_unsure() {
        val morning = arrive(AppData.EMPTY, day, 8 * 60 + 30, 1000f).arrivals.single()
        assertEquals(Place.WORK, morning.guess)
        assertFalse(morning.sure)
        val evening = arrive(AppData.EMPTY, day, 19 * 60, 1000f).arrivals.single()
        assertEquals(Place.HOME, evening.guess)
        val midday = arrive(AppData.EMPTY, day, 13 * 60, 1000f).arrivals.single()
        assertEquals(Place.OTHER, midday.guess)
        assertNull(Arrivals.pending(arrive(AppData.EMPTY, day, 13 * 60, 1000f), ms(day, 13 * 60 + 1)))
    }

    @Test
    fun starting_a_lesson_right_after_arriving_confirms_the_place() {
        var data = arrive(AppData.EMPTY, day, 8 * 60 + 30, 1000f)
        val pending = Arrivals.pending(data, ms(day, 8 * 60 + 35))
        assertNotNull(pending)
        assertEquals(Mode.MORNING, Arrivals.modeFor(Arrivals.placeOf(pending!!)))
        // 사용자가 추정과 다르게 퇴근 학습을 골랐다면 그쪽이 정답이다.
        data = Arrivals.confirmFromSession(data, Mode.EVENING, ms(day, 8 * 60 + 36))
        assertEquals(Place.HOME, data.arrivals.single().confirmed)
        assertNull(Arrivals.pending(data, ms(day, 8 * 60 + 37)))
        // 오래 지난 도착은 확정하지 않는다.
        val late = Arrivals.confirmFromSession(arrive(AppData.EMPTY, day, 8 * 60, 1000f), Mode.MORNING, ms(day, 11 * 60))
        assertNull(late.arrivals.single().confirmed)
    }

    @Test
    fun learned_commute_is_recognised_with_confidence() {
        val data = commuter(3)
        val odo = data.arrivals.last().odometerKm!!
        val atWork = arrive(data, day + 3, 8 * 60 + 50, odo + 12.5f).arrivals.last()
        assertEquals(Place.WORK, atWork.guess)
        assertTrue(atWork.sure)
        assertEquals(12.5f, atWork.tripKm!!, 0.01f)

        var next = arrive(data, day + 3, 8 * 60 + 50, odo + 12.5f)
        next = Arrivals.confirmFromSession(next, Mode.MORNING, ms(day + 3, 8 * 60 + 52))
        val atHome = arrive(next, day + 3, 18 * 60 + 20, odo + 24.6f).arrivals.last()
        assertEquals(Place.HOME, atHome.guess)
        assertTrue(atHome.sure)

        val (work, home) = Arrivals.today(arrive(next, day + 3, 18 * 60 + 20, odo + 24.6f), day + 3)
        assertEquals(8 * 60 + 50, work?.minuteOfDay)
        assertEquals(18 * 60 + 20, home?.minuteOfDay)
    }

    @Test
    fun order_beats_the_clock_when_leaving_work_early() {
        var data = commuter(3)
        val odo = data.arrivals.last().odometerKm!!
        data = arrive(data, day + 3, 8 * 60 + 40, odo + 12f)
        data = Arrivals.confirmFromSession(data, Mode.MORNING, ms(day + 3, 8 * 60 + 41))
        // 오후 2시 반차 퇴근: 시간대는 애매하지만 오늘 출근 뒤의 평소 거리 도착이다.
        val early = arrive(data, day + 3, 14 * 60, odo + 24f).arrivals.last()
        assertEquals(Place.HOME, early.guess)
    }

    @Test
    fun trips_unlike_the_commute_are_not_treated_as_commute() {
        val data = commuter(3)
        val odo = data.arrivals.last().odometerKm!!
        val far = arrive(data, day + 4, 9 * 60, odo + 48f).arrivals.last()
        assertEquals(Place.OTHER, far.guess)
        val near = arrive(AppData.EMPTY.copy(arrivals = data.arrivals), day + 4, 9 * 60, odo + 0.6f).arrivals.last()
        assertEquals(Place.OTHER, near.guess)
        assertNull(Arrivals.pending(arrive(data, day + 4, 9 * 60, odo + 48f), ms(day + 4, 9 * 60 + 1)))
    }

    @Test
    fun parking_manoeuvres_and_reopening_the_app_do_not_add_arrivals() {
        var data = arrive(AppData.EMPTY, day, 8 * 60 + 30, 1000f)
        // 같은 자리에서 D ↔ P를 반복
        data = arrive(data, day, 8 * 60 + 31, 1000.05f)
        // 앱을 다시 열어 주행거리만 읽은 경우
        data = arrive(data, day, 9 * 60, 1000.05f, approximate = true)
        assertEquals(1, data.arrivals.size)
        // 프로세스가 꺼져 있던 사이의 주행은 주행거리 변화로 알아챈다.
        data = arrive(data, day, 19 * 60, 1012f, approximate = true)
        assertEquals(2, data.arrivals.size)
        assertTrue(data.arrivals.last().approximate)
        assertEquals(12f, data.arrivals.last().tripKm!!, 0.01f)
        // 주행거리를 못 받는 차량에서 값만 읽은 경우는 기록하지 않는다.
        assertTrue(arrive(AppData.EMPTY, day, 9 * 60, null, approximate = true).arrivals.isEmpty())
        // 주행거리가 없어도 기어 변화로 본 도착은 기록한다.
        assertEquals(1, arrive(AppData.EMPTY, day, 9 * 60, null).arrivals.size)
    }

    @Test
    fun only_exact_saved_names_count_as_home_or_work() {
        assertEquals(Place.HOME, Arrivals.placeFromName(" 우리 집 "))
        assertEquals(Place.HOME, Arrivals.placeFromName("Home"))
        assertEquals(Place.WORK, Arrivals.placeFromName("회사"))
        assertEquals(Place.WORK, Arrivals.placeFromName("사무실"))
        assertNull(Arrivals.placeFromName("집밥 식당"))
        assertNull(Arrivals.placeFromName("돼지집"))
        assertNull(Arrivals.placeFromName(null))
    }

    @Test
    fun navi_destination_hint_makes_the_guess_sure_even_without_history() {
        val d = Arrivals.record(AppData.EMPTY, "n1", ms(day, 13 * 60), day, 13 * 60, null, 12.4f, 25, false,
            hint = Place.WORK, viaNavi = true)
        val a = d.arrivals.single()
        assertEquals(Place.WORK, a.guess)
        assertTrue(a.sure)
        assertTrue(a.viaNavi)
        assertEquals(12.4f, a.tripKm!!, 0.01f)
    }

    @Test
    fun navi_and_gear_arrivals_of_one_trip_merge_in_either_order() {
        val gearSecond = Arrivals.record(AppData.EMPTY, "n1", ms(day, 520), day, 520, null, 12.4f, 25, false,
            hint = Place.WORK, viaNavi = true)
            .let { Arrivals.record(it, "g1", ms(day, 527), day, 527, 1012.4f, 12.3f, 24, false) }
        assertEquals(1, gearSecond.arrivals.size)
        assertEquals(1012.4f, gearSecond.arrivals[0].odometerKm!!, 0.01f)
        assertEquals(12.4f, gearSecond.arrivals[0].tripKm!!, 0.01f)
        assertEquals(Place.WORK, gearSecond.arrivals[0].guess)

        val naviSecond = Arrivals.record(AppData.EMPTY, "g1", ms(day, 520), day, 520, 1012.4f, 12.3f, 24, false)
            .let { Arrivals.record(it, "n1", ms(day, 524), day, 524, null, 12.4f, 25, false, hint = Place.HOME, viaNavi = true) }
        assertEquals(1, naviSecond.arrivals.size)
        assertEquals(Place.HOME, naviSecond.arrivals[0].guess)
        assertTrue(naviSecond.arrivals[0].sure)
        assertEquals(1012.4f, naviSecond.arrivals[0].odometerKm!!, 0.01f)
    }

    @Test
    fun a_confirmed_arrival_is_not_overwritten_by_a_late_navi_hint() {
        var d = Arrivals.record(AppData.EMPTY, "g1", ms(day, 520), day, 520, 1012.4f, 12.3f, 24, false)
        d = Arrivals.confirm(d, "g1", Place.HOME)
        d = Arrivals.record(d, "n1", ms(day, 524), day, 524, null, 12.4f, 25, false, hint = Place.WORK, viaNavi = true)
        assertEquals(Place.HOME, Arrivals.placeOf(d.arrivals.single()))
    }

    @Test
    fun arrivals_far_apart_in_time_stay_separate() {
        val d = Arrivals.record(AppData.EMPTY, "n1", ms(day, 520), day, 520, null, 12.4f, 25, false, hint = Place.WORK, viaNavi = true)
            .let { Arrivals.record(it, "g1", ms(day, 520 + 40), day, 560, 1030f, 12.0f, 24, false) }
        assertEquals(2, d.arrivals.size)
    }

    @Test
    fun navi_values_replace_a_guessed_distance_from_an_approximate_arrival() {
        val d = Arrivals.record(AppData.EMPTY, "g0", ms(day, 520), day, 520, 15000f, null, null, true)
            .let { Arrivals.record(it, "g1", ms(day + 1, 520), day + 1, 520, 0f, null, null, true) }
        val last = d.arrivals.last()
        assertEquals(15000f, last.tripKm!!, 0.01f)
        val merged = Arrivals.record(d, "n1", last.atMs + 60_000, day + 1, 521, null, 12.4f, 25, false,
            hint = Place.WORK, viaNavi = true).arrivals.last()
        assertEquals(12.4f, merged.tripKm!!, 0.01f)
        assertEquals(25, merged.tripMinutes)
        assertFalse(merged.approximate)
    }

    @Test
    fun a_confirmed_destination_is_remembered_and_recognised_next_time() {
        var d = Arrivals.learn(AppData.EMPTY, "poi/1", Place.WORK)
        assertEquals(Place.WORK, d.knownPlaces["poi/1"])
        val hinted = Arrivals.record(d, "n1", ms(day, 13 * 60), day, 13 * 60, null, 12.4f, 25, false,
            hint = d.knownPlaces["poi/1"], viaNavi = true, hintLearned = true).arrivals.single()
        assertEquals(Place.WORK, hinted.guess)
        assertTrue(hinted.sure)
        assertTrue(hinted.reason.startsWith("전에"))
        // 다시 확인하면 덮어쓰고, 출퇴근이 아니라고 하면 잊는다.
        d = Arrivals.learn(d, "poi/1", Place.HOME)
        assertEquals(Place.HOME, d.knownPlaces["poi/1"])
        d = Arrivals.learn(d, "poi/1", Place.OTHER)
        assertTrue(d.knownPlaces.isEmpty())
    }

    @Test
    fun only_the_most_recent_destinations_are_kept() {
        var d = AppData.EMPTY
        for (i in 0 until Arrivals.MAX_PLACES + 5) d = Arrivals.learn(d, "poi/$i", Place.HOME)
        assertEquals(Arrivals.MAX_PLACES, d.knownPlaces.size)
        assertNull(d.knownPlaces["poi/0"])
        assertNotNull(d.knownPlaces["poi/${Arrivals.MAX_PLACES + 4}"])
    }
}
