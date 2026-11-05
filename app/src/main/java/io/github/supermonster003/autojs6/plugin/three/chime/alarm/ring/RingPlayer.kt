package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.ImportedRingtones
import org.autojs.plugin.alarm.api.AlarmContract
import java.io.IOException

/**
 * Plays the sound of a ring (roadmap P2.4, T14, T35, T70) with `USAGE_ALARM` attributes, looping. A source that cannot
 * be played (a deleted or corrupt file, a uri without permission, the media store before the first unlock) falls back
 * to the default alarm sound, then the default ringtone and notification sound, and finally a tone generated on the
 * alarm stream; every fallback is logged. Audio focus is asked for (`AUDIOFOCUS_GAIN_TRANSIENT`) but the ring does not
 * depend on it (T35). Used on the main thread only.
 *
 * zh-CN: 以 `USAGE_ALARM` 音频属性循环播放响铃的铃声 (路线图 P2.4, T14, T35, T70). 无法播放的铃声 (已删除或损坏的文件, 没有权限的 uri, 首次解锁前的
 * 媒体库) 依次回退为默认闹钟铃声, 默认来电铃声与通知音, 最后是在闹钟流上生成的提示音; 每次回退都记日志. 会申请音频焦点 (`AUDIOFOCUS_GAIN_TRANSIENT`),
 * 但响铃不依赖它 (T35). 只在主线程使用.
 */
internal class RingPlayer(private val context: Context) {

    private val audio: AudioManager = context.getSystemService(AudioManager::class.java)

    private val main = Handler(Looper.getMainLooper())

    private var source: String? = null

    private var candidates: List<Uri> = emptyList()

    private var index = 0

    private var player: MediaPlayer? = null

    private var tone: ToneGenerator? = null

    private var toneOn = false

    private var playing = false

    private var gain = 1f

    private var focusRequest: AudioFocusRequest? = null

    private var focusAsked = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        // Losing focus does not stop an alarm (T35). zh-CN: 失去焦点不会停止闹钟 (T35).
        Log.i(RingService.TAG, "audio focus change $change, ringing on")
    }

    /** What is playing, for the logs and device checks. zh-CN: 正在播放的内容, 供日志与设备检查使用. */
    val playingWhat: String
        get() = when {
            player != null -> "uri#$index"
            tone != null -> "tone"
            else -> "none"
        }

    /**
     * Plays [source] at [gain] (0 - 1, applied to the player, not the stream); a null [source] pauses.
     * zh-CN: 以 [gain] (0 - 1, 作用于播放器而非闹钟流) 播放 [source]; [source] 为 null 时暂停.
     */
    fun set(source: String?, gain: Float) {
        if (source == null) {
            pause()
            return
        }
        if (source != this.source) open(source)
        this.gain = gain
        player?.setVolume(gain, gain)
        playing = true
        resume()
    }

    /** Asks for audio focus (again, when the ringing screen comes to the front, T35). zh-CN: 申请音频焦点 (响铃界面到前台时再申请一次, T35). */
    fun requestFocus() {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(ATTRIBUTES)
                .setOnAudioFocusChangeListener(focusListener, main)
                .build()
                .also { focusRequest = it }
            audio.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audio.requestAudioFocus(focusListener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
        focusAsked = true
        Log.i(RingService.TAG, "audio focus " + if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "granted" else "denied ($result), ringing on")
    }

    fun release() {
        playing = false
        player?.release()
        player = null
        tone?.release()
        tone = null
        toneOn = false
        source = null
        if (focusAsked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let(audio::abandonAudioFocusRequest)
            } else {
                @Suppress("DEPRECATION")
                audio.abandonAudioFocus(focusListener)
            }
            focusAsked = false
        }
    }

    private fun pause() {
        playing = false
        player?.takeIf { it.isPlaying }?.pause()
        stopTone()
    }

    private fun resume() {
        if (!focusAsked) requestFocus()
        player?.let { if (!it.isPlaying) it.start() }
        val tone = tone ?: return
        if (gain < MIN_TONE_GAIN) {
            stopTone()
        } else if (!toneOn) {
            toneOn = tone.startTone(ToneGenerator.TONE_SUP_RINGTONE)
        }
    }

    private fun stopTone() {
        if (toneOn) tone?.stopTone()
        toneOn = false
    }

    private fun open(source: String) {
        player?.release()
        player = null
        tone?.release()
        tone = null
        toneOn = false
        this.source = source
        candidates = (listOfNotNull(primary(source)) + defaults()).distinct()
        index = 0
        load()
    }

    /** Prepares the first candidate from [index] on that plays, the tone when none does. zh-CN: 从 [index] 起准备第一个能播放的候选, 都不行时用提示音. */
    private fun load() {
        while (index < candidates.size) {
            val uri = candidates[index]
            val prepared = MediaPlayer()
            try {
                prepared.setAudioAttributes(ATTRIBUTES)
                prepared.setDataSource(context, uri)
                prepared.isLooping = true
                prepared.setOnErrorListener { failed, what, extra ->
                    main.post { onError(failed, "error $what / $extra") }
                    true
                }
                prepared.prepare()
                prepared.setVolume(gain, gain)
                player = prepared
                Log.i(RingService.TAG, "player source #$index of ${candidates.size} ready (${kindOf(uri)})")
                return
            } catch (e: IOException) {
                fallback(prepared, uri, e)
            } catch (e: RuntimeException) {
                // IllegalArgumentException, IllegalStateException and SecurityException of unreadable sources. zh-CN: 不可读来源的各类异常.
                fallback(prepared, uri, e)
            }
        }
        tone = try {
            ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
        } catch (e: RuntimeException) {
            Log.e(RingService.TAG, "player tone unavailable, the ring is silent", e)
            null
        }
        if (tone != null) Log.w(RingService.TAG, "player falls back to the tone")
    }

    private fun fallback(prepared: MediaPlayer, uri: Uri, e: Exception) {
        prepared.release()
        Log.w(RingService.TAG, "player source #$index (${kindOf(uri)}) failed, falling back: ${e.javaClass.simpleName} ${e.message}")
        index++
    }

    private fun onError(failed: MediaPlayer, reason: String) {
        if (failed !== player) return
        Log.w(RingService.TAG, "player source #$index failed while playing, falling back: $reason")
        failed.release()
        player = null
        index++
        load()
        if (playing) resume()
    }

    private fun primary(source: String): Uri? = when {
        source == AlarmContract.SOUND_DEFAULT -> null
        ImportedRingtones.isImported(source) -> {
            val file = ImportedRingtones.fileOf(context.createDeviceProtectedStorageContext().filesDir, source)
            if (file == null) Log.w(RingService.TAG, "player source is not a valid imported ringtone uri")
            file?.let(Uri::fromFile)
        }
        else -> Uri.parse(source)
    }

    /**
     * The defaults in order. The settings uri of the alarm sound comes first: the media player reads the system's copy
     * of it, which is available before the first unlock (T7).
     * zh-CN: 按顺序排列的默认铃声. 闹钟铃声的设置 uri 在最前: 媒体播放器读取系统为它保存的副本, 首次解锁前也可用 (T7).
     */
    private fun defaults(): List<Uri> = listOfNotNull(
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
        runCatching { RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM) }.getOrNull(),
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
    )

    private fun kindOf(uri: Uri): String = when {
        uri.scheme == "file" -> "imported"
        uri.authority == "settings" -> "default"
        else -> uri.scheme ?: "uri"
    }

    companion object {

        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        private const val MIN_TONE_GAIN = 0.01f
    }
}
