package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import org.autojs.plugin.alarm.api.AlarmSpeak
import java.util.Locale

/**
 * The voice of a ring (roadmap P2.4, T62, T63): a [TextToSpeech] of the `engine` of [speak] (the system default when
 * omitted) speaking on the alarm stream. An engine that is not installed, does not start within [INIT_TIMEOUT_MILLIS],
 * lacks the `language` or fails to speak makes the speech unavailable: [Listener.onUnavailable] is called once and the
 * ring goes on with its sound (T63). An engine named explicitly is never replaced by another one. Used on the main
 * thread only; the callbacks come on it too.
 *
 * zh-CN: 响铃的朗读 (路线图 P2.4, T62, T63): 用 [speak] 的 `engine` (省略时为系统默认引擎) 的 [TextToSpeech] 在闹钟流上朗读. 引擎未安装, 未在
 * [INIT_TIMEOUT_MILLIS] 内启动, 缺少 `language` 或朗读失败时朗读不可用: 调用一次 [Listener.onUnavailable], 响铃照常发声 (T63). 明确指定的引擎不会被
 * 其他引擎替代. 只在主线程使用, 回调也在主线程.
 */
internal class RingSpeaker(context: Context, private val speak: AlarmSpeak, private val listener: Listener) {

    interface Listener {
        fun onSpoken()
        fun onUnavailable()
    }

    private val main = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null

    private var ready = false

    private var failed = false

    private var pending = false

    private var utterance: String? = null

    private var count = 0

    private val initTimeout = Runnable { fail("engine did not start within ${INIT_TIMEOUT_MILLIS / 1000} s") }

    private val watchdog = Runnable { finish(utterance ?: return@Runnable, "watchdog") }

    init {
        val engine = speak.engine
        if (engine != null && !installed(context, engine)) {
            fail("engine not installed")
        } else {
            main.postDelayed(initTimeout, INIT_TIMEOUT_MILLIS)
            tts = TextToSpeech(context.applicationContext, { status -> main.post { onInit(status) } }, engine)
        }
    }

    /** Speaks the text once; before the engine is ready it waits for it. zh-CN: 朗读一次文本; 引擎就绪前先等待. */
    fun speak() {
        if (failed) return
        if (!ready) {
            pending = true
            return
        }
        val tts = tts ?: return
        val id = "chime-${++count}"
        utterance = id
        if (tts.speak(speak.text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR) {
            fail("speak refused")
            return
        }
        main.removeCallbacks(watchdog)
        main.postDelayed(watchdog, UTTERANCE_TIMEOUT_MILLIS)
    }

    fun shutdown() {
        failed = true
        main.removeCallbacks(initTimeout)
        main.removeCallbacks(watchdog)
        tts?.let {
            it.stop()
            it.shutdown()
        }
        tts = null
    }

    private fun onInit(status: Int) {
        val tts = tts ?: return
        if (failed) return
        main.removeCallbacks(initTimeout)
        if (status != TextToSpeech.SUCCESS) {
            fail("engine init status $status")
            return
        }
        speak.language?.let { tag ->
            val result = tts.setLanguage(Locale.forLanguageTag(tag))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                fail("language not available ($result)")
                return
            }
        }
        speak.rate?.let { tts.setSpeechRate(it.toFloat()) }
        speak.pitch?.let { tts.setPitch(it.toFloat()) }
        tts.setAudioAttributes(ATTRIBUTES)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                main.post { finish(utteranceId, "done") }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { finish(utteranceId, "error") }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                main.post { finish(utteranceId, "error $errorCode") }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                main.post { finish(utteranceId, "stopped") }
            }
        })
        ready = true
        Log.i(RingService.TAG, "speech ready" + (speak.engine?.let { " (engine $it)" } ?: " (system engine)"))
        if (pending) {
            pending = false
            speak()
        }
    }

    private fun finish(id: String?, how: String) {
        if (failed || id == null || id != utterance) return
        utterance = null
        main.removeCallbacks(watchdog)
        Log.i(RingService.TAG, "speech $how")
        listener.onSpoken()
    }

    private fun fail(reason: String) {
        if (failed) return
        failed = true
        main.removeCallbacks(initTimeout)
        main.removeCallbacks(watchdog)
        Log.w(RingService.TAG, "speech unavailable: $reason")
        main.post { listener.onUnavailable() }
    }

    private companion object {

        const val INIT_TIMEOUT_MILLIS = 10_000L

        /** Gives up on an utterance that never reports its end. zh-CN: 放弃一直不报告结束的朗读. */
        const val UTTERANCE_TIMEOUT_MILLIS = 180_000L

        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        fun installed(context: Context, engine: String): Boolean =
            context.packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0).any { it.serviceInfo.packageName == engine }
    }
}
