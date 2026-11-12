package com.vocar.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class VoiceState { STARTING, READY, UNAVAILABLE }

/**
 * 영어 표현 음원 재생. 지금은 기기의 TTS 엔진(미국 영어)을 쓴다.
 * 기획안의 "검수 음원 파일"이 준비되면 이 클래스만 바꾸면 된다.
 *
 * - 재생 전에 오디오 포커스를 요청하고, 얻지 못하면 재생하지 않는다(무단 재생·볼륨 변경 없음).
 * - 통화·내비 안내 등으로 포커스를 잃으면 즉시 멈추고 onInterrupted로 알린다.
 * - 엔진이나 영어 음성이 없으면 UNAVAILABLE이 되고, 앱은 글자로 학습을 이어간다.
 */
class Speaker(context: Context, private val onInterrupted: () -> Unit) {
    var state by mutableStateOf(VoiceState.STARTING)
        private set
    var speaking by mutableStateOf(false)
        private set

    private val main = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private var closed = false
    private var hasFocus = false
    private var sequence = 0
    private var current = ""

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener({ change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN && hasFocus) {
                hasFocus = false
                val wasSpeaking = speaking
                halt()
                if (wasSpeaking) onInterrupted()
            }
        }, main)
        .build()

    private var tts: TextToSpeech? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                main.post { onEngineReady(status) }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "TTS 엔진을 시작하지 못했습니다: ${t.message}")
            state = VoiceState.UNAVAILABLE
        }
    }

    private fun onEngineReady(status: Int) {
        val engine = tts
        if (closed || engine == null) return
        if (status != TextToSpeech.SUCCESS) {
            state = VoiceState.UNAVAILABLE
            return
        }
        val language = try {
            engine.setLanguage(Locale.US)
        } catch (t: Throwable) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "이 기기의 TTS는 영어(미국)를 지원하지 않습니다.")
            state = VoiceState.UNAVAILABLE
            return
        }
        engine.setAudioAttributes(attributes)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                main.post { if (utteranceId == current) speaking = true }
            }

            override fun onDone(utteranceId: String?) {
                main.post { finished(utteranceId) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { finished(utteranceId) }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                main.post { finished(utteranceId) }
            }
        })
        state = VoiceState.READY
    }

    private fun finished(utteranceId: String?) {
        if (utteranceId != current) return
        speaking = false
        releaseFocus()
    }

    /** 재생을 시작했으면 true. 엔진이 없거나 포커스를 얻지 못하면 false. */
    fun speak(text: String, slow: Boolean): Boolean {
        val engine = tts
        if (closed || engine == null || state != VoiceState.READY) return false
        if (!hasFocus) {
            hasFocus = audio.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        if (!hasFocus) return false
        current = "utterance-${++sequence}"
        engine.setSpeechRate(if (slow) 0.7f else 0.95f)
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, current)
        if (result != TextToSpeech.SUCCESS) {
            current = ""
            releaseFocus()
            return false
        }
        return true
    }

    fun stop() {
        halt()
        releaseFocus()
    }

    private fun halt() {
        current = ""
        speaking = false
        runCatching { tts?.stop() }
    }

    private fun releaseFocus() {
        if (hasFocus) {
            hasFocus = false
            audio.abandonAudioFocusRequest(focusRequest)
        }
    }

    fun close() {
        closed = true
        stop()
        runCatching { tts?.shutdown() }
        tts = null
    }

    private companion object {
        const val TAG = "Vocar"
    }
}
