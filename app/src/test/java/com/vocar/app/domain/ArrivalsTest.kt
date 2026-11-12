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
}
