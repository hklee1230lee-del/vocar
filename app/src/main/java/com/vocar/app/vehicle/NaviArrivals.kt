package com.vocar.app.vehicle

import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.vocar.app.BuildConfig
import com.vocar.app.domain.Arrivals

/**
 * 내비게이션(NaviHelper)의 "목적지 도착" 이벤트로 도착을 알아챈다. 기어 도착과 병행한다.
 * 내비 안내를 켠 주행에서만 오므로, 안내 없이 다닌 날은 TripMonitor의 기어·주행거리 방식이 받쳐 준다.
 *
 * - 목적지 이름은 메모리에서 집/회사로 알아보는 데만 쓰고, 이름·주소·좌표는 저장하지 않는다.
 * - 도착 이벤트의 주행 거리·시간을 출근/퇴근 추정에 쓴다.
 * - 권한 NAVI_ROUTE가 없거나 내비 서비스에 연결하지 못하면 아무것도 하지 않는다.
 */
class NaviArrivals(private val context: Context, private val onArrival: (TripEnd) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private var helper: NaviHelper? = null
    private var destination: String? = null

    private val listener = object : NaviHelperEventListener {
        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            main.post {
                destination = destinationInfo.name.takeIf { it.isNotBlank() }
                // 이름이 집/회사로 오는지 확인하는 용도. 이름은 debug 빌드 로그에만 남긴다.
                if (BuildConfig.DEBUG) Log.i(TAG, "navi: 목적지 이름='${destinationInfo.name}' → ${Arrivals.placeFromName(destinationInfo.name) ?: "기타"}")
            }
        }

        override fun onDestinationChanged() {
            main.post {
                destination = null
                runCatching { helper?.getDestinationInfo() }
            }
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            main.post {
                arrived(
                    destinationArrivedInfo.name.takeIf { it.isNotBlank() } ?: destination,
                    destinationArrivedInfo.distance.toDouble(),
                    destinationArrivedInfo.duration.toDouble(),
                )
            }
        }
    }

    fun start() {
        if (helper != null) return
        if (context.checkSelfPermission(NAVI_ROUTE) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "navi: NAVI_ROUTE 권한이 없어 내비 도착은 쓰지 않습니다")
            return
        }
        try {
            val navi = NaviHelper(context)
            helper = navi
            navi.initialize(
                onConnected = {
                    main.post {
                        if (helper !== navi) return@post
                        navi.addListener(listener)
                        runCatching { navi.getDestinationInfo() }
                        Log.i(TAG, "navi: 내비 연결")
                    }
                },
                onFailed = { error ->
                    main.post {
                        Log.w(TAG, "navi: 연결 실패 ${error.javaClass.simpleName}")
                        if (helper === navi) helper = null
                    }
                },
            )
        } catch (t: Throwable) {
            helper = null
            Log.w(TAG, "navi: 시작하지 못했습니다: ${t.message}")
        }
    }

    /**
     * 도착 하나를 TripEnd로 바꿔 넘긴다. SDK 이벤트와 debug 주입이 같은 곳으로 들어온다.
     * 거리는 m, 시간은 초로 가정한다(SDK 문서 확인 전까지): 말이 안 되는 값은 버리고 나머지로 진행한다.
     */
    fun arrived(name: String?, distance: Double, duration: Double) {
        val km = (distance / 1000.0).takeIf { it.isFinite() && it > 0.05 && it < 2000 }?.toFloat()
        val minutes = (duration / 60.0).takeIf { it.isFinite() && it >= 1 && it < 24 * 60 }?.toInt()
        val hint = Arrivals.placeFromName(name)
        Log.i(TAG, "navi: 도착 trip=${km}km ${minutes}분 목적지=${hint ?: "기타"}")
        onArrival(TripEnd(System.currentTimeMillis(), null, km, minutes, approximate = false, hint = hint, viaNavi = true))
    }

    fun stop() {
        helper?.let { navi ->
            runCatching { navi.removeListener(listener) }
            runCatching { navi.release() }
        }
        helper = null
    }

    private companion object {
        const val TAG = "Vocar"
        const val NAVI_ROUTE = "pleos.car.permission.NAVI_ROUTE"
    }
}
