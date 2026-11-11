package com.arrivaltalk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arrivaltalk.app.audio.Speaker
import com.arrivaltalk.app.data.Store
import com.arrivaltalk.app.ui.AppController
import com.arrivaltalk.app.ui.AppRoot
import com.arrivaltalk.app.vehicle.ParkingGate
import java.io.File

/**
 * 도착한마디 — 주차 후, 내리기 전 1~3분 영어 한마디.
 *
 * 화면이 보일 때(onStart)마다 주차 상태를 처음부터 다시 확인하고,
 * 화면에서 벗어나면(onStop) 재생·확인을 모두 멈춘다. 돌아와도 자동으로 재생하거나 이어가지 않는다.
 */
class MainActivity : ComponentActivity() {
    private lateinit var gate: ParkingGate
    private lateinit var speaker: Speaker
    private lateinit var controller: AppController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gate = ParkingGate(this) { controller.onGateLost() }
        speaker = Speaker(this) { controller.onAudioInterrupted() }
        controller = AppController(Store(File(filesDir, "arrival-talk.json")), gate, speaker)
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
        speaker.close()
        gate.close()
        super.onDestroy()
    }
}
