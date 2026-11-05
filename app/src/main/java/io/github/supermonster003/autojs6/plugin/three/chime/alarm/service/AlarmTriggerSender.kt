package io.github.supermonster003.autojs6.plugin.three.chime.alarm.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.BindingHints
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.AlarmEventBus
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.AlarmEventKeys
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmPluginActions
import java.util.concurrent.ConcurrentHashMap

/**
 * The `ALARM_TRIGGER` broadcasts of bound scripts (roadmap P2.5, T11, T12, appendix A section 5.3): for every event of
 * [AlarmEventBus], each host package whose hint table binds the alarm to the event type gets an explicit ordered
 * broadcast protected by the plugin signature permission, with the event JSON and its `occurrence`. A `change` with
 * action `remove` goes to every host with a row for the alarm, then the rows are dropped here too. Live callbacks do
 * not wait for the answer (T76): the host's counts, or `scriptSkipped` (`locked` before the first unlock, `unavailable`
 * when no host answered), only reach the log (`adb logcat -s ChimeTrigger`) and [lastOutcome]; nothing is retried.
 * zh-CN: 绑定脚本的 `ALARM_TRIGGER` 广播 (路线图 P2.5, T11, T12, 附录 A 第 5.3 节): 对 [AlarmEventBus] 的每个事件, 凡提示表把该闹钟绑定到该事件类型的
 * 宿主包都会收到一条显式的有序广播, 受插件签名权限保护, 带事件 JSON 与 `occurrence`. action 为 `remove` 的 `change` 发给所有登记了该闹钟的宿主, 随后
 * 这里也删除这些行. 实时回调不等待应答 (T76): 宿主的计数, 或 `scriptSkipped` (首次解锁前为 `locked`, 没有宿主应答时为 `unavailable`), 只写入日志
 * (`adb logcat -s ChimeTrigger`) 与 [lastOutcome]; 不重试.
 */
class AlarmTriggerSender(context: Context, private val hints: BindingHints, bus: AlarmEventBus) {

    /** The answer of one host to one trigger. zh-CN: 一个宿主对一次触发的应答. */
    data class Outcome(
        val owner: String,
        val type: String,
        val occurrence: String,
        val launched: Int = 0,
        val duplicates: Int = 0,
        val missing: Int = 0,
        val removed: Int = 0,
        val scriptSkipped: String? = null,
    )

    private val context: Context = context.applicationContext

    private val outcomes = ConcurrentHashMap<String, Outcome>()

    init {
        bus.addListener(::send)
    }

    /** The last answer for [alarmId] (debug checks). zh-CN: [alarmId] 最近一次的应答 (调试检查). */
    fun lastOutcome(alarmId: String): Outcome? = outcomes[alarmId]

    private fun send(event: AlarmEvent) {
        val owners = hints.targets(event)
        val removal = event.type == AlarmContract.EVENT_CHANGE && event.action == AlarmContract.CHANGE_REMOVE
        if (owners.isNotEmpty()) {
            val json = event.toJson()
            if (json.toByteArray(Charsets.UTF_8).size > AlarmLimits.MAX_EVENT_JSON_BYTES) {
                Log.w(TAG, "${event.type} ${event.alarmId}: event JSON above ${AlarmLimits.MAX_EVENT_JSON_BYTES} bytes, not sent")
            } else {
                for (owner in owners) broadcast(owner, event, json)
            }
        }
        if (removal) hints.forget(event.alarmId)
    }

    private fun broadcast(owner: String, event: AlarmEvent, json: String) {
        val intent = Intent(AlarmPluginActions.HOST_TRIGGER)
            .setPackage(owner)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            .putExtra(AlarmEventKeys.EXTRA_CONTRACT_VERSION, AlarmContract.CONTRACT_VERSION)
            .putExtra(AlarmEventKeys.EXTRA_REQUEST, AlarmContract.REQUEST_EVENT)
            .putExtra(AlarmEventKeys.EXTRA_EVENT_JSON, json)
            .putExtra(AlarmEventKeys.EXTRA_OCCURRENCE, event.occurrence)
        val answer = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                record(answerOf(owner, event, resultCode, getResultExtras(false)), event.alarmId)
            }
        }
        try {
            context.sendOrderedBroadcast(intent, AlarmPluginActions.PLUGIN_PERMISSION, answer, null, Activity.RESULT_CANCELED, null, null)
        } catch (e: RuntimeException) {
            Log.w(TAG, "${event.type} ${event.alarmId} -> $owner: not sent (${e.javaClass.simpleName})")
            record(Outcome(owner, event.type, event.occurrence, scriptSkipped = skippedReason()), event.alarmId)
        }
    }

    private fun answerOf(owner: String, event: AlarmEvent, code: Int, extras: android.os.Bundle?): Outcome {
        if (code != Activity.RESULT_OK || extras == null) return Outcome(owner, event.type, event.occurrence, scriptSkipped = skippedReason())
        return Outcome(
            owner = owner,
            type = event.type,
            occurrence = event.occurrence,
            launched = extras.getInt(AlarmEventKeys.RESULT_LAUNCHED),
            duplicates = extras.getInt(AlarmEventKeys.RESULT_DUPLICATES),
            missing = extras.getInt(AlarmEventKeys.RESULT_MISSING),
            removed = extras.getInt(AlarmEventKeys.RESULT_REMOVED),
            scriptSkipped = extras.getString(AlarmEventKeys.RESULT_SCRIPT_SKIPPED),
        )
    }

    private fun record(outcome: Outcome, alarmId: String) {
        outcomes[alarmId] = outcome
        val detail = outcome.scriptSkipped?.let { "scriptSkipped=$it" }
            ?: "launched=${outcome.launched} duplicates=${outcome.duplicates} missing=${outcome.missing} removed=${outcome.removed}"
        Log.i(TAG, "${outcome.type} $alarmId -> ${outcome.owner}: $detail")
    }

    /** `locked` while the user has not unlocked since boot, `unavailable` otherwise. zh-CN: 首次解锁前为 `locked`, 否则为 `unavailable`. */
    private fun skippedReason(): String {
        val unlocked = runCatching { context.getSystemService(UserManager::class.java).isUserUnlocked }.getOrDefault(true)
        return if (unlocked) AlarmContract.SCRIPT_SKIPPED_UNAVAILABLE else AlarmContract.SCRIPT_SKIPPED_LOCKED
    }

    companion object {
        const val TAG = "ChimeTrigger"
    }
}
