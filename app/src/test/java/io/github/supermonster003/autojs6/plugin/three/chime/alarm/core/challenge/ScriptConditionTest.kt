package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ScriptCondition.RingAnswer
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ScriptCondition.StatusAnswer
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ScriptCondition.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Roadmap P9.2 (T284): every fallback condition of the `script` dismiss challenge (chime-p9-protocol.md section 4):
 * `locked`, `noScript`, `unavailable`, `launchFailed`, `ended`, and when the challenge keeps waiting.
 * zh-CN: 路线图 P9.2 (T284): 关闭挑战 `script` 的全部退回条件 (chime-p9-protocol.md 第 4 节): `locked`, `noScript`, `unavailable`, `launchFailed`,
 * `ended`, 以及何时继续等待.
 */
class ScriptConditionTest {

    private fun fallBack(reason: String) = Verdict.FallBack(reason)

    @Test
    fun `at the start a locked device or no script at all falls back`() {
        assertEquals(fallBack(ScriptCondition.LOCKED), ScriptCondition.atStart(unlocked = false, targets = 1, listening = true))
        assertEquals(fallBack(ScriptCondition.NO_SCRIPT), ScriptCondition.atStart(unlocked = true, targets = 0, listening = false))
        assertEquals(Verdict.Listen, ScriptCondition.atStart(unlocked = true, targets = 0, listening = true))
        assertEquals(Verdict.AwaitAnswers, ScriptCondition.atStart(unlocked = true, targets = 2, listening = false))
    }

    @Test
    fun `the ring answers decide unavailable, locked, launchFailed, noScript or polling`() {
        // No host answered. zh-CN: 没有宿主应答.
        assertEquals(fallBack(ScriptCondition.UNAVAILABLE), ScriptCondition.afterRing(listOf(null, null), listening = false))
        // A host skipped the scripts before the first unlock. zh-CN: 宿主在首次解锁前跳过了脚本.
        assertEquals(fallBack(ScriptCondition.LOCKED), ScriptCondition.afterRing(listOf(RingAnswer(0, 0, 0, "locked")), listening = true))
        assertEquals(fallBack(ScriptCondition.LAUNCH_FAILED), ScriptCondition.afterRing(listOf(RingAnswer(0, 0, 2), null), listening = false))
        // Stale hints: the host has no binding for the round. zh-CN: 提示表过期: 宿主没有本轮的绑定.
        assertEquals(fallBack(ScriptCondition.NO_SCRIPT), ScriptCondition.afterRing(listOf(RingAnswer(0, 0, 0)), listening = false))
        assertEquals(Verdict.Poll, ScriptCondition.afterRing(listOf(RingAnswer(1, 0, 1), null), listening = false))
        // A round picked up after a plugin restart: the host started the script before. zh-CN: 插件重启后接续的一轮: 宿主之前已启动脚本.
        assertEquals(Verdict.Poll, ScriptCondition.afterRing(listOf(RingAnswer(0, 1, 0)), listening = false))
    }

    @Test
    fun `a live ring listener keeps the wait when no bound script runs`() {
        assertEquals(Verdict.Listen, ScriptCondition.afterRing(listOf(null), listening = true))
        assertEquals(Verdict.Listen, ScriptCondition.afterRing(listOf(RingAnswer(0, 0, 1)), listening = true))
        assertEquals(Verdict.Listen, ScriptCondition.afterRing(listOf(RingAnswer(0, 0, 0)), listening = true))
        assertEquals(Verdict.Listen, ScriptCondition.afterPoll(listOf(StatusAnswer(true, 0, 1, 0)), listening = true))
        assertEquals(fallBack(ScriptCondition.NO_SCRIPT), ScriptCondition.listenersGone())
    }

    @Test
    fun `the polls decide unavailable, launchFailed, ended or more polling`() {
        assertEquals(fallBack(ScriptCondition.UNAVAILABLE), ScriptCondition.afterPoll(listOf(null), listening = false))
        assertEquals(Verdict.Poll, ScriptCondition.afterPoll(listOf(StatusAnswer(true, 1, 1, 0), null), listening = false))
        // Delivered but not started yet counts as running. zh-CN: 已投递未启动也算运行中.
        assertEquals(Verdict.Poll, ScriptCondition.afterPoll(listOf(StatusAnswer(true, 1, 0, 0)), listening = false))
        assertEquals(fallBack(ScriptCondition.ENDED), ScriptCondition.afterPoll(listOf(StatusAnswer(true, 0, 2, 0)), listening = false))
        assertEquals(fallBack(ScriptCondition.LAUNCH_FAILED), ScriptCondition.afterPoll(listOf(StatusAnswer(true, 0, 0, 1)), listening = false))
        // A restarted host lost the record: its scripts ended with the process. zh-CN: 重启过的宿主没有记录: 脚本已随进程结束.
        assertEquals(fallBack(ScriptCondition.ENDED), ScriptCondition.afterPoll(listOf(StatusAnswer(false, 0, 0, 0)), listening = false))
        assertEquals(fallBack(ScriptCondition.ENDED), ScriptCondition.afterPoll(listOf(StatusAnswer(false, 0, 0, 0), StatusAnswer(true, 0, 0, 1)), listening = false))
        // An untracked host's counts never keep the wait. zh-CN: 无记录宿主的计数不会让等待继续.
        assertEquals(fallBack(ScriptCondition.ENDED), ScriptCondition.afterPoll(listOf(StatusAnswer(false, 3, 3, 0)), listening = false))
    }
}
