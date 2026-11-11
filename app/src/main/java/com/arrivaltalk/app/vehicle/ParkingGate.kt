package com.arrivaltalk.app.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class GateState {
    /** 화면에 들어온 직후: 아직 차량 상태를 받지 못했다. */
    CHECKING,

    /** 기어 P + 주행 제한 없음 + 방금 확인한 값. 이때만 학습·음원을 허용한다. */
    PARKED,

    /** 주행 제한이 걸렸거나 기어가 P가 아니다. */
    RESTRICTED,

    /** 상태를 읽지 못했다. 주차로 추정하지 않는다. */
    UNKNOWN,
}

/**
 * 주차 전용 게이트. 학습 시작·이어하기·음원 재생은 모두 이 상태가 PARKED일 때만 가능하다.
 *
 * - 주행 제한: CarUxRestrictionsManager (조회 + 변경 리스너)
 * - 주차 여부: Pleos Vehicle SDK의 기어 조회. 속도 0이나 "제한 없음"만으로 주차라고 보지 않는다.
 * - 화면에 보이는 동안에만 확인하고(start/stop), 2초 넘게 갱신되지 않은 값은 버린다.
 */
class ParkingGate(context: Context, private val onLeftParked: () -> Unit) {
    var state by mutableStateOf(GateState.CHECKING)
        private set

    val parked: Boolean get() = state == GateState.PARKED

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var vehicle: Vehicle? = null
    private var car: Car? = null
    private var ux: CarUxRestrictionsManager? = null
    private var closed = false
    private var running = false
    private var startedAt = 0L

    /** null = 모름, true = P, false = P 아님 */
    private var gearPark: Boolean? = null
    private var gearAt = 0L
    private var gearRequest = 0L
    private var gearApplied = 0L

    /** null = 모름, true = 주행 제한 없음, false = 제한 중 */
    private var uxClear: Boolean? = null

    private fun main(block: () -> Unit) {
        handler.post { if (!closed) block() }
    }

    init {
        try {
            vehicle = Vehicle(context).also { it.initialize() }
        } catch (t: Throwable) {
            Log.w(TAG, "Vehicle SDK를 시작하지 못했습니다: ${t.message}")
        }
        try {
            car = Car.createCar(appContext, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                main {
                    uxClear = null
                    ux = null
                    if (ready) {
                        try {
                            val manager = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                            manager.registerListener { restrictions ->
                                main {
                                    uxClear = !restrictions.isRequiresDistractionOptimization
                                    update()
                                }
                            }
                            uxClear = manager.currentCarUxRestrictions?.isRequiresDistractionOptimization == false
                            ux = manager
                        } catch (t: Throwable) {
                            Log.w(TAG, "주행 제한을 확인하지 못했습니다: ${t.message}")
                        }
                    }
                    update()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "차량 서비스에 연결하지 못했습니다: ${t.message}")
        }
    }

    private val poll = object : Runnable {
        override fun run() {
            if (closed || !running) return
            requestGear()
            try {
                ux?.let { uxClear = it.currentCarUxRestrictions?.isRequiresDistractionOptimization == false }
            } catch (t: Throwable) {
                uxClear = null
            }
            update()
            handler.postDelayed(this, POLL_MS)
        }
    }

    private fun requestGear() {
        val v = vehicle
        if (v == null) {
            gearPark = null
            return
        }
        val request = ++gearRequest
        val requestedAt = SystemClock.elapsedRealtime()
        try {
            v.getGear().getCurrentGearSelectionState({ gear ->
                main {
                    if (request >= gearApplied) {
                        gearApplied = request
                        gearAt = requestedAt
                        gearPark = gear == VehicleGear.GEAR_PARK
                        update()
                    }
                }
            }, {
                main {
                    if (request >= gearApplied) {
                        gearApplied = request
                        gearPark = null
                        update()
                    }
                }
            })
        } catch (t: Throwable) {
            gearPark = null
        }
    }

    private fun update() {
        val now = SystemClock.elapsedRealtime()
        val gear = gearPark?.takeIf { now - gearAt < STALE_MS }
        val next = when {
            !running -> GateState.CHECKING
            uxClear == false || gear == false -> GateState.RESTRICTED
            uxClear == true && gear == true -> GateState.PARKED
            now - startedAt < SETTLE_MS -> GateState.CHECKING
            else -> GateState.UNKNOWN
        }
        if (next == state) return
        val wasParked = state == GateState.PARKED
        Log.i(TAG, "gate ${state.name} -> ${next.name} (gear=$gearPark ux=$uxClear)")
        state = next
        if (wasParked) onLeftParked()
    }

    /** 화면이 보일 때 호출한다. 이전 값은 믿지 않고 처음부터 다시 확인한다. */
    fun start() {
        if (closed || running) return
        running = true
        startedAt = SystemClock.elapsedRealtime()
        gearPark = null
        handler.removeCallbacks(poll)
        handler.post(poll)
    }

    /** 화면에서 벗어나면 확인을 멈춘다. 백그라운드에서 반복 조회하지 않는다. */
    fun stop() {
        if (!running) return
        running = false
        handler.removeCallbacks(poll)
        gearPark = null
        update()
    }

    fun close() {
        closed = true
        running = false
        handler.removeCallbacksAndMessages(null)
        runCatching { ux?.unregisterListener() }
        runCatching { car?.disconnect() }
        runCatching { vehicle?.release() }
    }

    private companion object {
        const val TAG = "ArrivalTalk"
        const val POLL_MS = 500L
        const val STALE_MS = 2000L
        const val SETTLE_MS = 3000L
    }
}
