package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ScriptCondition.RingAnswer
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ScriptCondition.StatusAnswer
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ScriptCondition.Verdict

/**
 * Follows the `script` dismiss challenge of the sounding round through [ScriptCondition] (roadmap P9.2, T284, the
 * protocol of chime-p9-protocol.md section 4): [ringSent] when the round's `ring` goes out to the hosts whose hint tables
 * bind it, [ringAnswered] with their answers, then a `scriptStatus` poll of the hosts that started scripts every
 * [pollMillis] (counted from the previous answer) while a bound script runs. [listenersChanged] rechecks a round that only
 * waits for a live `ring` listener; [roundChanged] stops the wait once the round no longer waits for a script (dismissed,
 * snoozed, silenced or fallen back). The plugin sounds one round at a time, so one round waits at a time; a new [ringSent]
 * replaces the wait. The [Port] is called outside the lock.
 * zh-CN: 通过 [ScriptCondition] 跟踪发声一轮的关闭挑战 `script` (路线图 P9.2, T284, 协议 chime-p9-protocol.md 第 4 节): 本轮 `ring` 发给提示表绑定了它的宿主时
 * 调用 [ringSent], 收到应答时调用 [ringAnswered], 之后只要有绑定脚本在运行, 就每隔 [pollMillis] (从上次应答算起) 轮询启动了脚本的宿主的 `scriptStatus`.
 * [listenersChanged] 重新判断只靠实时 `ring` 监听者等待的一轮; [roundChanged] 在本轮不再等脚本时 (已关闭, 贪睡, 自动停止或已退回) 停止等待. 插件一次只让一轮发声,
 * 因此一次只有一轮在等; 新的 [ringSent] 取代原来的等待. [Port] 在锁外调用.
 */
class ScriptChallengeWatcher(private val port: Port, private val pollMillis: Long = POLL_MILLIS) {

    /** What the watcher needs from the plugin process. zh-CN: 跟踪所需的插件进程能力. */
    interface Port {
        /** Whether the user unlocked the device since the boot. zh-CN: 开机后用户是否解锁过设备. */
        fun unlocked(): Boolean

        /** Whether a live callback listens to `ring`. zh-CN: 是否有实时回调监听 `ring`. */
        fun listening(): Boolean

        /** Whether [occurrence] is the sounding round and still waits for a script. zh-CN: [occurrence] 是否为发声的一轮且仍在等脚本. */
        fun waiting(occurrence: String): Boolean

        /**
         * Sends `scriptStatus` for the round to [owners]; [done] gets the answer of each (null without one), once, on any thread.
         * zh-CN: 向 [owners] 发送本轮的 `scriptStatus`; [done] 在任意线程收到每个宿主的应答 (未应答为 null), 只调用一次.
         */
        fun poll(owners: List<String>, occurrence: String, alarmId: String, done: (Map<String, StatusAnswer?>) -> Unit)

        /** Runs [task] once after [delayMillis]; the result cancels it. zh-CN: [delayMillis] 之后运行一次 [task]; 返回值用于取消. */
        fun schedule(delayMillis: Long, task: () -> Unit): () -> Unit

        /** Falls the round [occurrence] back to its `script.fallback` for [reason]. zh-CN: 因 [reason] 让 [occurrence] 这一轮退回其 `script.fallback`. */
        fun fallBack(occurrence: String, reason: String)
    }

    private enum class Stage { ANSWERS, POLL, LISTEN }

    private class Wait(val occurrence: String, val alarmId: String, var stage: Stage, var owners: List<String>) {
        var cancel: (() -> Unit)? = null
    }

    private val lock = Any()

    private var wait: Wait? = null

    /** The round that waits for a script, null without one (tests and logs). zh-CN: 正在等脚本的一轮, 没有时为 null (测试与日志). */
    val waitingFor: String? get() = synchronized(lock) { wait?.occurrence }

    /**
     * The `ring` of a `script` round goes out to [owners], the hosts whose hint tables bind it (none when no host does).
     * zh-CN: `script` 一轮的 `ring` 发给 [owners], 即提示表绑定了它的宿主 (没有宿主绑定时为空).
     */
    fun ringSent(occurrence: String, alarmId: String, owners: List<String>) {
        val verdict = ScriptCondition.atStart(port.unlocked(), owners.size, port.listening())
        val reason = synchronized(lock) {
            stopLocked()
            when (verdict) {
                is Verdict.FallBack -> verdict.reason
                Verdict.AwaitAnswers -> {
                    wait = Wait(occurrence, alarmId, Stage.ANSWERS, owners)
                    null
                }
                Verdict.Listen -> {
                    wait = Wait(occurrence, alarmId, Stage.LISTEN, emptyList())
                    null
                }
                Verdict.Poll -> null
            }
        }
        reason?.let { port.fallBack(occurrence, it) }
    }

    /**
     * The answers of the hosts to the `ring` of [occurrence], by host (null when one did not answer).
     * zh-CN: 各宿主对 [occurrence] 的 `ring` 的应答, 按宿主 (未应答为 null).
     */
    fun ringAnswered(occurrence: String, answers: Map<String, RingAnswer?>) {
        val verdict = ScriptCondition.afterRing(answers.values.toList(), port.listening())
        val reason = synchronized(lock) {
            val current = wait?.takeIf { it.occurrence == occurrence && it.stage == Stage.ANSWERS } ?: return
            when (verdict) {
                is Verdict.FallBack -> {
                    wait = null
                    verdict.reason
                }
                Verdict.Poll -> {
                    current.stage = Stage.POLL
                    current.owners = answers.filterValues { it?.started == true }.keys.toList()
                    current.cancel = port.schedule(pollMillis) { tick(current) }
                    null
                }
                Verdict.Listen -> {
                    current.stage = Stage.LISTEN
                    null
                }
                Verdict.AwaitAnswers -> null
            }
        }
        reason?.let { port.fallBack(occurrence, it) }
    }

    /** The live `ring` listeners changed. zh-CN: 实时 `ring` 监听者发生了变化. */
    fun listenersChanged() {
        if (port.listening()) return
        val verdict = ScriptCondition.listenersGone() as Verdict.FallBack
        val occurrence = synchronized(lock) {
            val current = wait?.takeIf { it.stage == Stage.LISTEN } ?: return
            wait = null
            current.occurrence
        }
        port.fallBack(occurrence, verdict.reason)
    }

    /** The ring state changed: stop when the round no longer waits for a script. zh-CN: 响铃状态变化: 本轮不再等脚本时停止. */
    fun roundChanged() {
        val current = synchronized(lock) { wait } ?: return
        if (port.waiting(current.occurrence)) return
        synchronized(lock) {
            if (wait === current) stopLocked()
        }
    }

    private fun tick(current: Wait) {
        synchronized(lock) {
            if (wait !== current || current.stage != Stage.POLL) return
            current.cancel = null
        }
        if (!port.waiting(current.occurrence)) {
            synchronized(lock) {
                if (wait === current) wait = null
            }
            return
        }
        port.poll(current.owners, current.occurrence, current.alarmId) { polled(current, it) }
    }

    private fun polled(current: Wait, answers: Map<String, StatusAnswer?>) {
        val verdict = ScriptCondition.afterPoll(answers.values.toList(), port.listening())
        val reason = synchronized(lock) {
            if (wait !== current || current.stage != Stage.POLL) return
            when (verdict) {
                is Verdict.FallBack -> {
                    wait = null
                    verdict.reason
                }
                Verdict.Poll -> {
                    current.cancel = port.schedule(pollMillis) { tick(current) }
                    null
                }
                Verdict.Listen -> {
                    current.stage = Stage.LISTEN
                    null
                }
                Verdict.AwaitAnswers -> null
            }
        }
        reason?.let { port.fallBack(current.occurrence, it) }
    }

    private fun stopLocked() {
        wait?.cancel?.invoke()
        wait = null
    }

    companion object {
        /** The `scriptStatus` poll interval (chime-p9-protocol.md section 4.2). zh-CN: `scriptStatus` 的轮询间隔 (协议第 4.2 节). */
        const val POLL_MILLIS = 5_000L
    }
}
