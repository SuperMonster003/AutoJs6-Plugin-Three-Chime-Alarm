package io.github.supermonster003.autojs6.plugin.three.chime.alarm.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.BindingHints
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.AnswerCollector
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ScriptChallengeWatcher
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ScriptCondition
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
 * action `remove`, or the `dismiss` / `silence` that ends a timer (roadmap P7.3, T125), goes to every host with a row for
 * it, then the rows are dropped here too. Live callbacks do
 * not wait for the answer (T76): the host's counts, or `scriptSkipped` (`locked` before the first unlock, `unavailable`
 * when no host answered), only reach the log (`adb logcat -s ChimeTrigger`) and [lastOutcome]; nothing is retried.
 * zh-CN: 绑定脚本的 `ALARM_TRIGGER` 广播 (路线图 P2.5, T11, T12, 附录 A 第 5.3 节): 对 [AlarmEventBus] 的每个事件, 凡提示表把该闹钟绑定到该事件类型的
 * 宿主包都会收到一条显式的有序广播, 受插件签名权限保护, 带事件 JSON 与 `occurrence`. action 为 `remove` 的 `change`, 或结束计时器的 `dismiss` / `silence` (路线图 P7.3,
 * T125), 发给所有登记了它的宿主, 随后这里也删除这些行. 实时回调不等待应答 (T76): 宿主的计数, 或 `scriptSkipped` (首次解锁前为 `locked`, 没有宿主应答时为 `unavailable`), 只写入日志
 * (`adb logcat -s ChimeTrigger`) 与 [lastOutcome]; 不重试.
 *
 * The `ring` of a round with the `script` dismiss challenge (1.3.0, roadmap P9.2, T284) also goes to the [scriptWatcher]:
 * the hosts it goes to first, then their answers gathered (null for a host without one, after [ANSWER_TIMEOUT_MILLIS] at
 * the latest); [scriptStatus] polls the hosts that started scripts (chime-p9-protocol.md section 4).
 * zh-CN: 带关闭挑战 `script` 的一轮的 `ring` (1.3.0, 路线图 P9.2, T284) 也交给 [scriptWatcher]: 先告知发往哪些宿主, 再交出收集到的应答 (未应答的宿主为 null,
 * 最迟 [ANSWER_TIMEOUT_MILLIS] 之后); [scriptStatus] 轮询启动了脚本的宿主 (协议 chime-p9-protocol.md 第 4 节).
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

    /** The last `scriptStatus` answer logged per host (one line per change). zh-CN: 每个宿主最近记录的 `scriptStatus` 应答 (变化时才记一行). */
    private val statusLogged = ConcurrentHashMap<String, String>()

    private val main = Handler(Looper.getMainLooper())

    /** Follows the `script` challenge rounds (roadmap P9.2); set by the runtime. zh-CN: 跟踪 `script` 挑战的轮次 (路线图 P9.2); 由运行时设置. */
    @Volatile
    var scriptWatcher: ScriptChallengeWatcher? = null

    init {
        bus.addListener(::send)
    }

    /** The last answer for [alarmId] (debug checks). zh-CN: [alarmId] 最近一次的应答 (调试检查). */
    fun lastOutcome(alarmId: String): Outcome? = outcomes[alarmId]

    private fun send(event: AlarmEvent) {
        val owners = hints.targets(event)
        val removal = hints.ends(event)
        val answers = scriptAnswers(event, owners)
        if (owners.isNotEmpty()) {
            val json = event.toJson()
            if (json.toByteArray(Charsets.UTF_8).size > AlarmLimits.MAX_EVENT_JSON_BYTES) {
                Log.w(TAG, "${event.type} ${event.alarmId}: event JSON above ${AlarmLimits.MAX_EVENT_JSON_BYTES} bytes, not sent")
                for (owner in owners) answers?.answer(owner, null)
            } else {
                for (owner in owners) broadcast(owner, event, json, answers)
            }
        }
        if (removal) hints.forget(event.alarmId)
    }

    /**
     * Tells the [scriptWatcher] that the `ring` of a `script` round goes to [owners] and gathers their answers for it;
     * null for other events, or when no host gets the `ring`.
     * zh-CN: 告知 [scriptWatcher] `script` 一轮的 `ring` 发往 [owners], 并为它收集应答; 其他事件或没有宿主收到 `ring` 时为 null.
     */
    private fun scriptAnswers(event: AlarmEvent, owners: List<String>): AnswerCollector<ScriptCondition.RingAnswer>? {
        val watcher = scriptWatcher ?: return null
        if (event.type != AlarmContract.EVENT_RING || event.challenge != AlarmContract.CHALLENGE_SCRIPT) return null
        Log.i(TAG, "script challenge ${event.occurrence}: ring to ${owners.size} hosts")
        watcher.ringSent(event.occurrence, event.alarmId, owners)
        if (owners.isEmpty()) return null
        return AnswerCollector(owners, ANSWER_TIMEOUT_MILLIS, ::schedule) { watcher.ringAnswered(event.occurrence, it) }
    }

    private fun broadcast(owner: String, event: AlarmEvent, json: String, answers: AnswerCollector<ScriptCondition.RingAnswer>?) {
        val intent = Intent(AlarmPluginActions.HOST_TRIGGER)
            .setPackage(owner)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            .putExtra(AlarmEventKeys.EXTRA_CONTRACT_VERSION, AlarmContract.CONTRACT_VERSION)
            .putExtra(AlarmEventKeys.EXTRA_REQUEST, AlarmContract.REQUEST_EVENT)
            .putExtra(AlarmEventKeys.EXTRA_EVENT_JSON, json)
            .putExtra(AlarmEventKeys.EXTRA_OCCURRENCE, event.occurrence)
        val answer = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val extras = getResultExtras(false)
                record(answerOf(owner, event, resultCode, extras), event.alarmId)
                answers?.answer(owner, ringAnswerOf(resultCode, extras))
            }
        }
        try {
            context.sendOrderedBroadcast(intent, AlarmPluginActions.PLUGIN_PERMISSION, answer, null, Activity.RESULT_CANCELED, null, null)
        } catch (e: RuntimeException) {
            Log.w(TAG, "${event.type} ${event.alarmId} -> $owner: not sent (${e.javaClass.simpleName})")
            record(Outcome(owner, event.type, event.occurrence, scriptSkipped = skippedReason()), event.alarmId)
            answers?.answer(owner, null)
        }
    }

    /** The answer to a `ring` for the script condition: null when the host did not answer. zh-CN: 供脚本条件使用的 `ring` 应答: 宿主未应答时为 null. */
    private fun ringAnswerOf(code: Int, extras: android.os.Bundle?): ScriptCondition.RingAnswer? {
        if (code != Activity.RESULT_OK || extras == null) return null
        return ScriptCondition.RingAnswer(
            launched = extras.getInt(AlarmEventKeys.RESULT_LAUNCHED),
            duplicates = extras.getInt(AlarmEventKeys.RESULT_DUPLICATES),
            missing = extras.getInt(AlarmEventKeys.RESULT_MISSING),
            scriptSkipped = extras.getString(AlarmEventKeys.RESULT_SCRIPT_SKIPPED),
        )
    }

    /**
     * Polls the bound scripts of the `script` round [occurrence] on [owners] (chime-p9-protocol.md section 4.2): an ordered
     * `request=scriptStatus` broadcast to each, answered at once by the host with `tracked` / `running` / `started` /
     * `failed`; [done] gets the answers by host (null for a host that did not answer, an old host that does not know the
     * request, or after [ANSWER_TIMEOUT_MILLIS]). Only changes reach the log.
     * zh-CN: 轮询 `script` 一轮 [occurrence] 在 [owners] 上的绑定脚本 (协议第 4.2 节): 向每个宿主发送 `request=scriptStatus` 有序广播, 宿主立即以 `tracked` /
     * `running` / `started` / `failed` 应答; [done] 按宿主收到应答 (未应答, 不认识该请求的旧宿主, 或超过 [ANSWER_TIMEOUT_MILLIS] 时为 null). 只有变化写入日志.
     */
    fun scriptStatus(owners: List<String>, occurrence: String, alarmId: String, done: (Map<String, ScriptCondition.StatusAnswer?>) -> Unit) {
        val answers = AnswerCollector(owners, ANSWER_TIMEOUT_MILLIS, ::schedule, done)
        for (owner in owners) {
            val intent = Intent(AlarmPluginActions.HOST_TRIGGER)
                .setPackage(owner)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(AlarmEventKeys.EXTRA_CONTRACT_VERSION, AlarmContract.CONTRACT_VERSION)
                .putExtra(AlarmEventKeys.EXTRA_REQUEST, ScriptCondition.REQUEST_SCRIPT_STATUS)
                .putExtra(AlarmEventKeys.EXTRA_OCCURRENCE, occurrence)
                .putExtra(AlarmEventKeys.EXTRA_ALARM_ID, alarmId)
            val answer = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val extras = getResultExtras(false)
                    val status = if (resultCode == Activity.RESULT_OK && extras != null) {
                        ScriptCondition.StatusAnswer(
                            tracked = extras.getBoolean(ScriptCondition.KEY_TRACKED),
                            running = extras.getInt(ScriptCondition.KEY_RUNNING),
                            started = extras.getInt(ScriptCondition.KEY_STARTED),
                            failed = extras.getInt(ScriptCondition.KEY_FAILED),
                        )
                    } else {
                        null
                    }
                    val line = status?.let { "tracked=${it.tracked} running=${it.running} started=${it.started} failed=${it.failed}" } ?: "no answer"
                    if (statusLogged.put(owner, "$occurrence $line") != "$occurrence $line") Log.i(TAG, "scriptStatus $occurrence -> $owner: $line")
                    answers.answer(owner, status)
                }
            }
            try {
                context.sendOrderedBroadcast(intent, AlarmPluginActions.PLUGIN_PERMISSION, answer, null, Activity.RESULT_CANCELED, null, null)
            } catch (e: RuntimeException) {
                Log.w(TAG, "scriptStatus $occurrence -> $owner: not sent (${e.javaClass.simpleName})")
                answers.answer(owner, null)
            }
        }
    }

    private fun schedule(delayMillis: Long, task: () -> Unit): () -> Unit {
        val runnable = Runnable(task)
        main.postDelayed(runnable, delayMillis)
        return { main.removeCallbacks(runnable) }
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

    /** The answer of [unbind]: how many bindings the hosts removed and which hosts did not answer. zh-CN: [unbind] 的结果. */
    data class UnbindResult(val removed: Int, val failed: List<String>)

    /**
     * Removes the script bindings of [alarmId] when the user does so on the alarm list (roadmap D20, T27): every host
     * with a hint row for the alarm gets a `request=unbind` ordered broadcast; a host that answers `RESULT_OK` drops the
     * bindings, resends its table later, and its row is dropped here at once. [done] runs on the main thread after every
     * host answered; a host that did not answer keeps its row and is listed in [UnbindResult.failed].
     * zh-CN: 用户在闹钟列表上解除 [alarmId] 的脚本绑定 (路线图 D20, T27): 向每个为该闹钟登记了提示行的宿主发送 `request=unbind` 有序广播; 应答
     * `RESULT_OK` 的宿主删除绑定并稍后重发提示表, 这里立即删除其行. 所有宿主应答后在主线程调用 [done]; 未应答的宿主保留其行, 列入 [UnbindResult.failed].
     */
    fun unbind(alarmId: String, done: (UnbindResult) -> Unit) {
        val owners = hints.owners().filter { owner -> hints.table(owner).any { it.alarmId == alarmId } }
        if (owners.isEmpty()) {
            done(UnbindResult(0, emptyList()))
            return
        }
        val pending = owners.toMutableSet()
        val failed = ArrayList<String>()
        var removed = 0
        fun answered(owner: String, ok: Boolean, count: Int) {
            if (!pending.remove(owner)) return
            if (ok) {
                removed += count
                hints.forget(alarmId, owner)
            } else {
                failed += owner
            }
            Log.i(TAG, "unbind $alarmId -> $owner: " + if (ok) "removed=$count" else "no answer")
            if (pending.isEmpty()) done(UnbindResult(removed, failed.toList()))
        }
        for (owner in owners) {
            val intent = Intent(AlarmPluginActions.HOST_TRIGGER)
                .setPackage(owner)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(AlarmEventKeys.EXTRA_CONTRACT_VERSION, AlarmContract.CONTRACT_VERSION)
                .putExtra(AlarmEventKeys.EXTRA_REQUEST, AlarmContract.REQUEST_UNBIND)
                .putExtra(AlarmEventKeys.EXTRA_ALARM_ID, alarmId)
            val answer = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    answered(owner, resultCode == Activity.RESULT_OK, getResultExtras(false)?.getInt(AlarmEventKeys.RESULT_REMOVED) ?: 0)
                }
            }
            try {
                // The result receiver runs on the main thread (no scheduler given). zh-CN: 结果接收器在主线程运行 (未指定 Handler).
                context.sendOrderedBroadcast(intent, AlarmPluginActions.PLUGIN_PERMISSION, answer, null, Activity.RESULT_CANCELED, null, null)
            } catch (e: RuntimeException) {
                Log.w(TAG, "unbind $alarmId -> $owner: not sent (${e.javaClass.simpleName})")
                answered(owner, false, 0)
            }
        }
    }

    /** `locked` while the user has not unlocked since boot, `unavailable` otherwise. zh-CN: 首次解锁前为 `locked`, 否则为 `unavailable`. */
    private fun skippedReason(): String {
        val unlocked = runCatching { context.getSystemService(UserManager::class.java).isUserUnlocked }.getOrDefault(true)
        return if (unlocked) AlarmContract.SCRIPT_SKIPPED_UNAVAILABLE else AlarmContract.SCRIPT_SKIPPED_LOCKED
    }

    companion object {
        const val TAG = "ChimeTrigger"

        /**
         * How long to wait for the answers of one round of broadcasts to the hosts of a `script` round: a foreground
         * receiver gets 10 s before the system gives up on it.
         * zh-CN: 等待 `script` 一轮的一批广播应答的时长: 前台接收器在被系统放弃前有 10 秒.
         */
        const val ANSWER_TIMEOUT_MILLIS = 15_000L
    }
}
