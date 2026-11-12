package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerRecord
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmResultKeys
import org.json.JSONArray
import org.json.JSONObject

/**
 * Debug builds only: the hook of `.python/device_ring.py` (roadmap P2.4). `am instrument` force-stops the plugin, which
 * would end the ring under test, so the device script sends this receiver shell broadcasts instead and reads the answer
 * from the log (tag [TAG], each line starts with the `token` of its request). Operations (`op`):
 *
 * - `add`: adds the one-shot probe alarm `label` (starting with [LABEL_PREFIX]) `inSeconds` from now, with the extra
 *   options of `options64` (Base64 of a JSON object);
 * - `snooze` / `dismiss`: sends the ring service the intent of the notification button for the probe `id` (all when
 *   omitted), the path the user takes;
 * - `state`: reports the ring state, the alarm stream volume and the saved user volume, the lock screen, the screen,
 *   the full screen and notification permissions and the active players with alarm usage;
 * - `clear`: dismisses and removes every probe alarm;
 * - `keepalive`: sets "Keep running in background" (roadmap Q23 (b), T100) to `value` `on` / `off` and syncs the
 *   keep-alive service, or only reports it without a `value`;
 * - `timer_start`: the hook of `.python/device_timers.py` (roadmap P7.1): starts the probe timer `label` (starting with
 *   [TIMER_PREFIX]) of `millis`, silent and without vibration, and pauses it at once when `paused` is true;
 * - `timers`: reports every probe timer (state, time left, end, boot) and the boot count;
 * - `timer_clear`: cancels every probe timer.
 *
 * The receiver is direct boot aware (debug manifest), so the timer operations also answer before the first unlock.
 *
 * zh-CN: 仅 debug 构建: `.python/device_ring.py` 的探针 (路线图 P2.4). `am instrument` 会强行停止插件, 从而结束被测的响铃, 因此设备脚本改为向本接收器
 * 发送 shell 广播, 并从日志读取应答 (标签 [TAG], 每行以请求的 `token` 开头). 操作 (`op`): `add` 在 `inSeconds` 秒后添加一次性探针闹钟 `label`
 * (以 [LABEL_PREFIX] 开头), 附加 `options64` (JSON 对象的 Base64) 中的选项; `snooze` / `dismiss` 向响铃服务发送通知按钮的 Intent (针对探针 `id`,
 * 省略时为全部), 与用户的操作路径相同; `state` 报告响铃状态, 闹钟流音量与保存的用户音量, 锁屏, 屏幕, 全屏与通知权限, 以及闹钟用途的活动播放器;
 * `clear` 关闭并删除全部探针闹钟; `keepalive` 把 "保持后台运行" (路线图 Q23 (b), T100) 设为 `value` `on` / `off` 并同步保活服务, 不带 `value` 时只报告;
 * `timer_start` 是 `.python/device_timers.py` 的探针 (路线图 P7.1): 启动时长为 `millis` 的探针计时器 `label` (以 [TIMER_PREFIX] 开头), 无声且不振动,
 * `paused` 为 true 时随即暂停; `timers` 报告全部探针计时器 (状态, 剩余时间, 到点时刻, 启动序号) 与开机次数; `timer_clear` 取消全部探针计时器.
 * 本接收器可直接启动感知 (debug 清单), 所以计时器操作在首次解锁前也能应答.
 */
class RingDebugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val runtime = AlarmRuntime.get(app)
        val token = intent.getStringExtra("token") ?: "-"
        val pending = goAsync()
        runtime.executor.execute {
            try {
                Log.i(TAG, "token=$token " + handle(app, runtime, intent))
            } catch (e: Exception) {
                Log.i(TAG, "token=$token error=${e.javaClass.simpleName}: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }

    private fun handle(context: Context, runtime: AlarmRuntime, intent: Intent): String = when (val op = intent.getStringExtra("op")) {        "add" -> add(runtime, intent)
        "snooze" -> send(context, RingService.ACTION_SNOOZE, intent.getStringExtra("id"))
        "dismiss" -> send(context, RingService.ACTION_DISMISS, intent.getStringExtra("id"))
        "state" -> state(context, runtime)
        "clear" -> {
            val probes = runtime.repository.list().filter(::isProbe)
            for (record in probes) {
                runtime.rings.dismiss(record.id, AlarmContract.BY_USER)
                runtime.repository.remove(record.id, AlarmContract.SOURCE_TOOL)
            }
            "cleared=${probes.size}"
        }
        "keepalive" -> keepAlive(context, intent.getStringExtra("value"))
        "timer_start" -> startTimer(runtime, intent)
        "timers" -> {
            val now = System.currentTimeMillis()
            val probes = runtime.timers.list().filter(::isProbe).joinToString(",") { record ->
                "${record.spec.label}:${record.id}:${record.state}:remaining=${record.toSnapshot(now).remainingMillis}:endsAt=${record.endsAt}:boot=${record.boot}"
            }
            val boot = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrNull()
            "now=$now bootCount=$boot timers=${probes.ifEmpty { "none" }}"
        }
        "timer_clear" -> {
            val probes = runtime.timers.list().filter(::isProbe)
            "cleared=${probes.count { runtime.toolTimerApi.cancel(it.id) }}"
        }
        else -> "error=unknown op '$op'"
    }

    private fun startTimer(runtime: AlarmRuntime, intent: Intent): String {
        val label = intent.getStringExtra("label").orEmpty()
        require(label.startsWith(TIMER_PREFIX)) { "probe timer labels start with $TIMER_PREFIX" }
        val options = linkedMapOf<String, Any?>(
            AlarmOptionKeys.DURATION_MILLIS to intent.getLongExtra("millis", 0L),
            AlarmOptionKeys.LABEL to label,
            AlarmOptionKeys.SOUND to mapOf("source" to "silent"),
            AlarmOptionKeys.VIBRATE to false,
        )
        return try {
            val started = runtime.toolTimerApi.startTimer(options)[AlarmResultKeys.TIMER] as Map<*, *>
            var record = runtime.timers.get(started["id"] as String)!!
            if (intent.getBooleanExtra("paused", false)) record = runtime.timers.pause(record.id)
            "started=${record.id} label=$label state=${record.state} remaining=${record.remainingMillis} endsAt=${record.endsAt}"
        } catch (e: AlarmException) {
            "error=${e.code}: ${e.message}"
        }
    }

    private fun add(runtime: AlarmRuntime, intent: Intent): String {
        val label = intent.getStringExtra("label").orEmpty()
        require(label.startsWith(LABEL_PREFIX)) { "probe labels start with $LABEL_PREFIX" }
        val seconds = intent.getIntExtra("inSeconds", 70)
        val at = (System.currentTimeMillis() / 1000 + seconds) * 1000
        val options = linkedMapOf<String, Any?>(AlarmOptionKeys.AT to at, AlarmOptionKeys.LABEL to label)
        intent.getStringExtra("options64")?.let { encoded ->
            val json = JSONObject(String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8))
            @Suppress("UNCHECKED_CAST")
            options.putAll(plain(json) as Map<String, Any?>)
        }
        return try {
            "added=${runtime.repository.add(options, AlarmContract.SOURCE_TOOL).id} label=$label at=$at"
        } catch (e: AlarmException) {
            "error=${e.code}: ${e.message}"
        }
    }

    private fun keepAlive(context: Context, value: String?): String {
        val settings = KeepAliveService.settings(context)
        val saved = when (value) {
            "on" -> settings.setEnabled(true)
            "off" -> settings.setEnabled(false)
            else -> null
        }
        if (saved == true) KeepAliveService.sync(context)
        return "keepAlive=${settings.enabled} running=${KeepAliveService.running}" + (saved?.let { " saved=$it" } ?: "")
    }

    private fun send(context: Context, action: String, id: String?): String {
        context.startService(Intent(context, RingService::class.java).setAction(action).putExtra(RingService.EXTRA_ALARM_ID, id))
        return "sent=$action id=${id ?: "all"}"
    }

    private fun state(context: Context, runtime: AlarmRuntime): String {
        val rings = runtime.rings
        val audio = context.getSystemService(AudioManager::class.java)
        // Playback configurations exist from API 26; older devices report `unknown`. zh-CN: 播放配置从 API 26 起才有, 更旧的设备报 `unknown`.
        val alarmPlayers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audio.activePlaybackConfigurations.count { it.audioAttributes.usage == AudioAttributes.USAGE_ALARM }.toString()
        } else {
            "unknown"
        }
        val saved = context.createDeviceProtectedStorageContext().getSharedPreferences("ring", Context.MODE_PRIVATE).let { preferences ->
            if (preferences.contains("volumeRestore")) preferences.getInt("volumeRestore", -1).toString() else "none"
        }
        val sounding = rings.sounding
        val probes = runtime.repository.list().filter(::isProbe).joinToString(",") { record ->
            "${record.spec.label}:${record.id}:enabled=${record.enabled}:snoozeCount=${record.state.snoozeCount}:snoozeUntil=${record.state.snoozeUntil}"
        }
        return listOf(
            "sounding=${sounding?.alarmId ?: "none"}",
            "label=${sounding?.label ?: "none"}",
            "occurrence=${sounding?.occurrence ?: "none"}",
            "queued=${rings.queued.joinToString(",") { it.alarmId }.ifEmpty { "none" }}",
            "ringing=${rings.ringing().joinToString(",") { "${it.label}:${if (it.snoozed) "snoozed" else "ringing"}:${it.snoozeCount}" }.ifEmpty { "none" }}",
            "volume=${audio.getStreamVolume(AudioManager.STREAM_ALARM)}",
            "volumeMax=${audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)}",
            "volumeMin=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(AudioManager.STREAM_ALARM) else 0}",
            "saved=$saved",
            "keyguard=${context.getSystemService(KeyguardManager::class.java).isKeyguardLocked}",
            "interactive=${context.getSystemService(PowerManager::class.java).isInteractive}",
            "fullScreen=${RingService.fullScreenAllowed(context)}",
            "notifications=${NotificationManagerCompat.from(context).areNotificationsEnabled()}",
            "alarmPlayers=$alarmPlayers",
            "probes=${probes.ifEmpty { "none" }}",
            "keepAlive=${KeepAliveService.settings(context).enabled}",
            "keepAliveRunning=${KeepAliveService.running}",
            "enabledAlarms=${runtime.repository.list().count { it.enabled }}",
        ).joinToString(" ")
    }

    private fun isProbe(record: AlarmRecord) = record.spec.label?.startsWith(LABEL_PREFIX) == true

    private fun isProbe(record: TimerRecord) = record.spec.label?.startsWith(TIMER_PREFIX) == true

    private fun plain(value: Any?): Any? = when (value) {
        is JSONObject -> value.keys().asSequence().associateWith { plain(value.get(it)) }
        is JSONArray -> (0 until value.length()).map { plain(value.get(it)) }
        JSONObject.NULL -> null
        else -> value
    }

    private companion object {
        const val TAG = "ChimeRingDebug"
        const val LABEL_PREFIX = "p24-probe"
        const val TIMER_PREFIX = "p71-probe"
    }
}
