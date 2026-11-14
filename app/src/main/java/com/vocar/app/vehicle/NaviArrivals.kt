package com.vocar.app.vehicle

import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.vocar.app.domain.Arrivals
import java.security.MessageDigest

/**
 * 내비게이션(NaviHelper)의 "목적지 도착" 이벤트로 도착을 알아챈다. 기어 도착과 병행한다.
 * 내비 안내를 켠 주행에서만 오므로, 안내 없이 다닌 날은 TripMonitor의 기어·주행거리 방식이 받쳐 준다.
 *
 * - 이벤트 하나로 모든 것을 정한다. 목적지 이름·주소·주행 거리·시간이 도착 이벤트에 함께 온다.
 *   (앱이 백그라운드에서 얼려 있는 동안은 목적지 푸시나 폴링이 멈추므로, 이전 상태에 기대지 않는다.)
 * - 목적지는 이름+주소의 해시 키로만 다룬다. 사용자가 집/회사로 확인한 경우에만 그 키를 기억하고,
 *   이름·주소·좌표 자체는 저장하지 않는다.
 * - 권한 NAVI_ROUTE가 없거나 내비 서비스에 연결하지 못하면 아무것도 하지 않는다.
 */
class NaviArrivals(private val context: Context, private val onArrival: (TripEnd) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private var helper: NaviHelper? = null

    private val listener = object : NaviHelperEventListener {
        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            main.post {
                arrived(
                    destinationArrivedInfo.name,
                    destinationArrivedInfo.address,
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
    fun arrived(name: String?, address: String?, distance: Double, duration: Double) {
        val km = (distance / 1000.0).takeIf { it.isFinite() && it > 0.05 && it < 2000 }?.toFloat()
        val minutes = (duration / 60.0).takeIf { it.isFinite() && it >= 1 && it < 24 * 60 }?.toInt()
        val hint = Arrivals.placeFromName(name)
        val key = placeKey(name, address)
        Log.i(TAG, "navi: 도착 trip=${km}km ${minutes}분 이름규칙=${hint ?: "기타"} 키=${key?.take(6) ?: "없음"}")
        onArrival(TripEnd(System.currentTimeMillis(), null, km, minutes, approximate = false, hint = hint, viaNavi = true, poi = key))
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

        /** 이름+주소의 해시 앞부분. 같은 목적지는 같은 키가 되고, 키에서 이름이나 주소를 되돌릴 수는 없다. */
        fun placeKey(name: String?, address: String?): String? {
            val n = name?.trim().orEmpty()
            if (n.isEmpty()) return null
            val digest = MessageDigest.getInstance("SHA-256").digest("$n|${address?.trim().orEmpty()}".toByteArray())
            return digest.take(10).joinToString("") { "%02x".format(it) }
        }
    }
}
