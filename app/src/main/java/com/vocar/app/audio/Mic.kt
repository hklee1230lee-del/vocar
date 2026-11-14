package com.vocar.app.audio

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class MicState { IDLE, LISTENING, FAILED }

/**
 * 말하기 단계의 Pleos STT. 마이크 버튼을 누를 때만 한 번 듣고, 실패하면 FAILED로 두어
 * 화면이 기존 "말해 봤어요" 버튼으로 이어가게 한다.
 */
class Mic(private val context: Context, private val locale: Locale) {
    var state by mutableStateOf(MicState.IDLE)
        private set
    var heard by mutableStateOf("")
        private set

    private val main = Handler(Looper.getMainLooper())
    private var stt: SpeechToText? = null
    private var ready = false
    private var closed = false
    private var onFinal: ((String) -> Unit)? = null

    private val listener = object : ResultListener {
        override fun onUpdated(text: String, isFinal: Boolean) {
            main.post {
                if (state != MicState.LISTENING) return@post
                heard = text
                if (isFinal) {
                    state = MicState.IDLE
                    onFinal?.invoke(text)
                }
            }
        }
        override fun onUpdatedEpdData(on: Long, off: Long) {}
        override fun onStartedRecognition() {}
        override fun onEndedRecognition() { main.post { if (state == MicState.LISTENING) state = MicState.IDLE } }
        override fun onError(error: ErrorType) { main.post { fail("STT 오류 $error") } }
        override fun onReady() {}
        override fun onUpdatedRms(rms: Double) {}
    }

    /** 버튼을 눌렀을 때만 호출한다. 결과 문장은 onFinal로 전달된다. */
    fun listen(onFinal: (String) -> Unit) {
        if (closed || state == MicState.LISTENING) return
        this.onFinal = onFinal
        heard = ""
        state = MicState.LISTENING
        main.removeCallbacks(timeout)
        main.postDelayed(timeout, LISTEN_MS)
        try {
            val engine = stt ?: SpeechToText(context.applicationContext, Mode.HYBRID).also { stt = it }
            if (ready) {
                engine.request()
            } else {
                engine.initialize(object : OnInitializeListener {
                    override fun onSuccess() {
                        main.post {
                            if (closed) return@post
                            engine.setLocale(locale)
                            engine.addListener(listener)
                            ready = true
                            engine.request()
                        }
                    }
                    override fun onFailure(error: ErrorType) { main.post { fail("STT 초기화 실패 $error") } }
                })
            }
        } catch (t: Throwable) {
            fail("STT 시작 실패 ${t.message}")
        }
    }

    private val timeout = Runnable { cancel() }

    fun cancel() {
        main.removeCallbacks(timeout)
        if (state == MicState.LISTENING) runCatching { stt?.stop() }
        state = MicState.IDLE
    }

    private fun fail(why: String) {
        Log.w("Vocar", why)
        runCatching { stt?.stop() }
        state = MicState.FAILED
    }

    fun close() {
        closed = true
        main.removeCallbacks(timeout)
        runCatching { stt?.removeListener(listener) }
        runCatching { stt?.release() }
        stt = null
    }
}

private const val LISTEN_MS = 10_000L

/** 소문자·구두점 제거 후 단어 겹침 70% 이상이면 같은 문장으로 본다. */
fun sameSentence(heard: String, expected: String): Boolean {
    fun words(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), " ").split(' ').filter { it.isNotBlank() }
    val e = words(expected)
    if (e.isEmpty()) return false
    val h = words(heard).toSet()
    return e.count { it in h } >= (e.size * 0.7).coerceAtLeast(1.0)
}
