package com.vocar.app.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/** 주행이 끝난 시점 하나와 그때 차량이 알려준 값. */
data class TripEnd(
    val atMs: Long,
    /** 누적 주행거리(km) */
    val odometerKm: Float?,
    /** 이번 주행 거리(km) */
    val tripKm: Float?,
    /** 이번 주행 시간(분) */
    val tripMinutes: Int?,
    /** 기어 변화를 직접 본 것이 아니라, 주차 상태에서 값을 읽기만 한 경우 */
    val approximate: Boolean,
)

/**
 * "주행이 끝났다"를 알아챈다. Vehicle SDK의 읽기 API만 쓴다(권한: CAR_DRIVE).
 *
 * - 기어 콜백(registerGearSelection): P가 아닌 기어를 본 뒤 P가 되면 그 순간이 도착 시각이다.
 *   폴링이 아니라 콜백이므로 앱이 화면에 없어도 프로세스가 살아 있는 동안 받는다.
 * - 프로세스가 꺼져 있던 사이의 주행은 콜백으로 알 수 없다. 그래서 앱이 주차 상태로 열릴 때
 *   누적 주행거리를 읽어(checkNow) 지난 기록보다 늘었으면 "그 사이에 도착했다"고 본다.
 * - 도착 시 누적 주행거리·이번 주행 거리·시간을 함께 읽어 출근/퇴근 추정에 쓴다.
 */
class TripMonitor private constructor(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var vehicle: Vehicle? = null
    private var drove = false
    private var retries = 0
    private var sink: ((TripEnd) -> Unit)? = null
    private val waiting = ArrayList<TripEnd>()

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            handler.post { onGear(gear) }
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "기어 구독 실패: ${e.message}")
            handler.post { subscribeLater() }
        }
    }

    init {
        try {
            vehicle = Vehicle(context.applicationContext).also { it.initialize() }
            subscribeLater()
        } catch (t: Throwable) {
            Log.w(TAG, "도착 감지를 시작하지 못했습니다: ${t.message}")
        }
    }

    private fun subscribeLater() {
        if (retries >= 5) return
        retries++
        handler.postDelayed({
            try {
                vehicle?.getGear()?.registerGearSelection(gearListener)
                Log.i(TAG, "trip: 기어 구독 시작")
            } catch (t: Throwable) {
                Log.w(TAG, "기어 구독 실패: ${t.message}")
            }
        }, 1500L * retries)
    }

    private fun onGear(gear: VehicleGear?) {
        if (gear == null || gear == VehicleGear.GEAR_UNKNOWN) return
        if (gear != VehicleGear.GEAR_PARK) {
            if (!drove) Log.i(TAG, "trip: 주행 시작 ($gear)")
            drove = true
        } else if (drove) {
            drove = false
            read(approximate = false)
        }
    }

    /** 주차가 확인됐을 때 호출한다. 그 사이 놓친 주행이 있었는지 누적 주행거리로 확인한다. */
    fun checkNow() = read(approximate = true)

    private fun read(approximate: Boolean) {
        val at = System.currentTimeMillis()
        // SDK가 성공·실패 콜백을 겹쳐 부르더라도 한 번만 넘긴다.
        var sent = false
        fun finish(trip: TripEnd) {
            if (!sent) {
                sent = true
                deliver(trip)
            }
        }
        val odometer = try {
            vehicle?.getOdometer()
        } catch (t: Throwable) {
            null
        }
        if (odometer == null) {
            finish(TripEnd(at, null, null, null, approximate))
            return
        }
        // 값 하나를 못 읽어도 나머지로 진행한다.
        fun step3(km: Float?, tripKm: Float?) {
            try {
                odometer.getTripCurrentDrivingTime(
                    { minutes -> handler.post { finish(TripEnd(at, km, tripKm, minutes, approximate)) } },
                    { handler.post { finish(TripEnd(at, km, tripKm, null, approximate)) } },
                )
            } catch (t: Throwable) {
                finish(TripEnd(at, km, tripKm, null, approximate))
            }
        }

        fun step2(km: Float?) {
            try {
                odometer.getTripCurrentDrivingDistance(
                    { meters -> handler.post { step3(km, meters?.let { it / 1000f }) } },
                    { handler.post { step3(km, null) } },
                )
            } catch (t: Throwable) {
                step3(km, null)
            }
        }
        try {
            odometer.getCurrentDistanceDriven(
                { km -> handler.post { step2(km) } },
                { handler.post { step2(null) } },
            )
        } catch (t: Throwable) {
            step2(null)
        }
    }

    private fun deliver(trip: TripEnd) {
        Log.i(TAG, "trip: 도착 odo=${trip.odometerKm}km trip=${trip.tripKm}km ${trip.tripMinutes}분 approx=${trip.approximate}")
        val target = sink
        if (target != null) target(trip) else waiting += trip
    }

    /** 화면이 다시 만들어져도 이어받을 수 있게, 그 사이 쌓인 도착을 먼저 넘겨준다. */
    fun attach(onTripEnd: (TripEnd) -> Unit) {
        sink = onTripEnd
        val queued = ArrayList(waiting)
        waiting.clear()
        queued.forEach(onTripEnd)
    }

    fun detach() {
        sink = null
    }

    companion object {
        private const val TAG = "Vocar"
        private var instance: TripMonitor? = null

        /** 프로세스에 하나만 둔다. 화면(Activity)보다 오래 산다. */
        fun get(context: Context): TripMonitor =
            instance ?: TripMonitor(context).also { instance = it }
    }
}
