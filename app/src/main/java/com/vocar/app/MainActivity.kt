package com.vocar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vocar.app.audio.Speaker
import com.vocar.app.data.Store
import com.vocar.app.ui.AppController
import com.vocar.app.ui.AppRoot
import com.vocar.app.vehicle.ParkingGate
import com.vocar.app.vehicle.TripMonitor
import java.io.File

/**
 * VOCAR — 주차 후, 내리기 전 1~3분 영어 한마디.
 *
 * 화면이 보일 때(onStart)마다 주차 상태를 처음부터 다시 확인하고,
 * 화면에서 벗어나면(onStop) 재생·확인을 모두 멈춘다. 돌아와도 자동으로 재생하거나 이어가지 않는다.
 */
class MainActivity : ComponentActivity() {
    private lateinit var gate: ParkingGate
    private lateinit var speaker: Speaker
    private lateinit var controller: AppController
    private lateinit var trips: TripMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gate = ParkingGate(this, onParked = { controller.onParked() }, onLeftParked = { controller.onGateLost() })
        speaker = Speaker(this) { controller.onAudioInterrupted() }
        trips = TripMonitor.get(this)
        controller = AppController(Store(File(filesDir, "vocar.json")), gate, speaker, trips)
        trips.attach { controller.onTripEnd(it) }
        setContent { AppRoot(controller) }
    }

    override fun onStart() {
        super.onStart()
        controller.onForeground()
        gate.start()
    }

    override fun onStop() {
        controller.onBackground()
        gate.stop()
        super.onStop()
    }

    override fun onDestroy() {
        // 도착 감지(TripMonitor)는 프로세스가 사는 동안 계속 듣는다. 화면과의 연결만 끊는다.
        trips.detach()
        speaker.close()
        gate.close()
        super.onDestroy()
    }
}
