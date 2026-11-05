package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingVolume

/**
 * The alarm stream of [RingVolume] (T14). Changing it is refused on some devices while Do Not Disturb is on; the ring
 * then plays at the volume the user set.
 * zh-CN: [RingVolume] 的闹钟流 (T14). 部分设备在勿扰模式下拒绝修改, 此时按用户设定的音量响铃.
 */
internal class AlarmStreamVolume(context: Context) : RingVolume.Stream {

    private val audio: AudioManager = context.getSystemService(AudioManager::class.java)

    override val min: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(AudioManager.STREAM_ALARM) else 0

    override val max: Int get() = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)

    override var index: Int
        get() = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        set(value) {
            try {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, value, 0)
                Log.i(RingService.TAG, "volume alarm=$value of $min - $max")
            } catch (e: SecurityException) {
                Log.w(RingService.TAG, "volume change refused: ${e.message}")
            }
        }
}

/**
 * Keeps the user's alarm volume on device-protected storage until it is restored (T68), written at once so a process
 * that dies while ringing still finds it.
 * zh-CN: 把用户的闹钟音量存在设备加密存储上直到恢复 (T68), 立即写入, 因此响铃中死亡的进程仍能找到它.
 */
internal class StoredVolumeMemory(context: Context) : RingVolume.Memory {

    private val preferences = context.createDeviceProtectedStorageContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override var saved: Int?
        get() = if (preferences.contains(KEY_SAVED)) preferences.getInt(KEY_SAVED, 0) else null
        // Written before the stream changes, so it must be on disk at once. zh-CN: 在改动闹钟流之前写入, 必须立即落盘.
        @SuppressLint("ApplySharedPref")
        set(value) {
            val editor = preferences.edit()
            if (value == null) editor.remove(KEY_SAVED) else editor.putInt(KEY_SAVED, value)
            editor.commit()
        }

    private companion object {
        const val PREFERENCES = "ring"
        const val KEY_SAVED = "volumeRestore"
    }
}
