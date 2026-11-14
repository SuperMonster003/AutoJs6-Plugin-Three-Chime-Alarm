package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

/**
 * When the `script` dismiss challenge falls back to its `script.fallback` (roadmap P9.2, T284, the protocol of
 * chime-p9-protocol.md section 4): the challenge waits for a script to call `alarms.dismiss(id)` only while such a
 * script can still do so. Each check answers a [Verdict]; a [Verdict.FallBack] carries the reason that goes to the log.
 *
 * - [atStart], when the round starts sounding: the device is locked (`locked`); no host binds the round's `ring` and no
 *   live callback listens to `ring` (`noScript`); only live listeners ([Verdict.Listen]); else wait for the answers of
 *   the `ring` broadcast ([Verdict.AwaitAnswers]).
 * - [afterRing], with the answer of every target host (null when it did not answer): a host skipped the scripts while
 *   locked (`locked`); a script started or was already running ([Verdict.Poll]); otherwise a live `ring` listener keeps
 *   the wait ([Verdict.Listen]), else none answered (`unavailable`), scripts were missing (`launchFailed`) or nothing at
 *   all (stale hints, `noScript`).
 * - [afterPoll], with the `scriptStatus` answers: a bound script still running keeps polling; otherwise a live listener
 *   keeps the wait, else none answered (`unavailable`), none ever started (`launchFailed`) or they ended (`ended`; a host
 *   without the record restarted, so its scripts ended).
 * - [listenersGone], when the last live `ring` listener leaves while the round only listens: `noScript`.
 *
 * zh-CN: 关闭挑战 `script` 何时退回 `script.fallback` (路线图 P9.2, T284, 协议 chime-p9-protocol.md 第 4 节): 只在仍有脚本能调用 `alarms.dismiss(id)`
 * 时等待. 每次判断返回一个 [Verdict]; [Verdict.FallBack] 带写入日志的原因. [atStart] 在本轮开始发声时: 设备未解锁 (`locked`); 没有宿主绑定本轮 `ring`
 * 且没有实时回调监听 `ring` (`noScript`); 只有实时监听者 ([Verdict.Listen]); 否则等 `ring` 广播的应答 ([Verdict.AwaitAnswers]). [afterRing] 收到每个目标
 * 宿主的应答 (未应答为 null): 有宿主因未解锁跳过脚本 (`locked`); 有脚本启动或已在运行 ([Verdict.Poll]); 否则有实时 `ring` 监听者就继续等 ([Verdict.Listen]),
 * 没有时: 都没应答 (`unavailable`), 有缺失 (`launchFailed`), 全为 0 (提示表过期, `noScript`). [afterPoll] 收到 `scriptStatus` 应答: 仍有绑定脚本在运行就继续轮询;
 * 否则有实时监听者就继续等, 没有时: 都没应答 (`unavailable`), 从未启动 (`launchFailed`), 已结束 (`ended`; 没有记录的宿主重启过, 脚本已随之结束).
 * [listenersGone] 在只靠实时监听等待时最后一个 `ring` 监听者离开: `noScript`.
 */
object ScriptCondition {

    const val LOCKED = "locked"
    const val NO_SCRIPT = "noScript"
    const val UNAVAILABLE = "unavailable"
    const val LAUNCH_FAILED = "launchFailed"
    const val ENDED = "ended"

    /** The value of `scriptSkipped` a host answers while the device is locked. zh-CN: 设备未解锁时宿主应答的 `scriptSkipped` 值. */
    private const val SKIPPED_LOCKED = "locked"

    sealed interface Verdict {
        /** Fall back now for [reason]. zh-CN: 因 [reason] 立即退回. */
        data class FallBack(val reason: String) : Verdict

        /** Wait for the answers of the `ring` broadcast. zh-CN: 等待 `ring` 广播的应答. */
        data object AwaitAnswers : Verdict

        /** Poll `scriptStatus` of the hosts that started scripts. zh-CN: 轮询启动了脚本的宿主的 `scriptStatus`. */
        data object Poll : Verdict

        /** Wait for a live `ring` listener; recheck when the listeners change. zh-CN: 等实时 `ring` 监听者; 监听者变化时再判. */
        data object Listen : Verdict
    }

    /** The answer of one host to the `ring` broadcast. zh-CN: 一个宿主对 `ring` 广播的应答. */
    data class RingAnswer(val launched: Int, val duplicates: Int, val missing: Int, val scriptSkipped: String? = null) {
        val started: Boolean get() = launched + duplicates > 0
    }

    /** The answer of one host to `scriptStatus`. zh-CN: 一个宿主对 `scriptStatus` 的应答. */
    data class StatusAnswer(val tracked: Boolean, val running: Int, val started: Int, val failed: Int)

    fun atStart(unlocked: Boolean, targets: Int, listening: Boolean): Verdict = when {
        !unlocked -> Verdict.FallBack(LOCKED)
        targets > 0 -> Verdict.AwaitAnswers
        listening -> Verdict.Listen
        else -> Verdict.FallBack(NO_SCRIPT)
    }

    fun afterRing(answers: List<RingAnswer?>, listening: Boolean): Verdict {
        val answered = answers.filterNotNull()
        if (answered.isEmpty()) return if (listening) Verdict.Listen else Verdict.FallBack(UNAVAILABLE)
        if (answered.any { it.scriptSkipped == SKIPPED_LOCKED }) return Verdict.FallBack(LOCKED)
        if (answered.any { it.started }) return Verdict.Poll
        return when {
            listening -> Verdict.Listen
            answered.sumOf { it.missing } > 0 -> Verdict.FallBack(LAUNCH_FAILED)
            else -> Verdict.FallBack(NO_SCRIPT)
        }
    }

    fun afterPoll(answers: List<StatusAnswer?>, listening: Boolean): Verdict {
        val answered = answers.filterNotNull()
        if (answered.isEmpty()) return if (listening) Verdict.Listen else Verdict.FallBack(UNAVAILABLE)
        if (answered.sumOf { if (it.tracked) it.running.coerceAtLeast(0) else 0 } > 0) return Verdict.Poll
        return when {
            listening -> Verdict.Listen
            // A host without the record restarted, so its scripts ended with it. zh-CN: 没有记录的宿主重启过, 其脚本已随之结束.
            answered.all { it.tracked } && answered.sumOf { it.started.coerceAtLeast(0) } == 0 -> Verdict.FallBack(LAUNCH_FAILED)
            else -> Verdict.FallBack(ENDED)
        }
    }

    fun listenersGone(): Verdict = Verdict.FallBack(NO_SCRIPT)
}
