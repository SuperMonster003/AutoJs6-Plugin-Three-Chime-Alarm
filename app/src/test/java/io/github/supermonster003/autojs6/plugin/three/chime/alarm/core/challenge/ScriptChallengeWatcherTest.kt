package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ScriptCondition.RingAnswer
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge.ScriptCondition.StatusAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P9.2 (T284): every fallback condition of the `script` challenge as the watcher follows a round
 * (chime-p9-protocol.md section 4): locked, no script, hosts unavailable, launch failed, scripts ended, the live listener
 * leaving, and the rounds that stop waiting; the poll runs every 5 s on the hosts that started scripts only.
 * zh-CN: 路线图 P9.2 (T284): 跟踪一轮时 `script` 挑战的每个退回条件 (协议第 4 节): 未解锁, 无脚本, 宿主不可用, 启动失败, 脚本已结束, 实时监听者离开,
 * 以及不再等待的轮次; 轮询每 5 秒一次, 只轮询启动了脚本的宿主.
 */
class ScriptChallengeWatcherTest {

    private val port = FakePort()

    private val watcher = ScriptChallengeWatcher(port)

    @Test
    fun `a locked device falls back at once`() {
        port.unlocked = false
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST))
        assertEquals(listOf(OCCURRENCE to ScriptCondition.LOCKED), port.fallbacks)
        assertNull(watcher.waitingFor)
    }

    @Test
    fun `no binding and no listener falls back with noScript`() {
        watcher.ringSent(OCCURRENCE, ALARM, emptyList())
        assertEquals(listOf(OCCURRENCE to ScriptCondition.NO_SCRIPT), port.fallbacks)
    }

    @Test
    fun `a live listener alone keeps the wait until it leaves`() {
        port.listening = true
        watcher.ringSent(OCCURRENCE, ALARM, emptyList())
        assertTrue(port.fallbacks.isEmpty())
        assertEquals(OCCURRENCE, watcher.waitingFor)
        // Another listener change while one still listens changes nothing. zh-CN: 仍有监听者时的变化不影响等待.
        watcher.listenersChanged()
        assertTrue(port.fallbacks.isEmpty())
        port.listening = false
        watcher.listenersChanged()
        assertEquals(listOf(OCCURRENCE to ScriptCondition.NO_SCRIPT), port.fallbacks)
        assertNull(watcher.waitingFor)
        assertTrue(port.tasks.isEmpty())
    }

    @Test
    fun `no host answering the ring falls back with unavailable`() {
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST, OTHER))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to null, OTHER to null))
        assertEquals(listOf(OCCURRENCE to ScriptCondition.UNAVAILABLE), port.fallbacks)
    }

    @Test
    fun `a host skipping the scripts while locked falls back with locked`() {
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(0, 0, 0, "locked")))
        assertEquals(listOf(OCCURRENCE to ScriptCondition.LOCKED), port.fallbacks)
    }

    @Test
    fun `missing scripts fall back with launchFailed, none at all with noScript`() {
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(0, 0, 1)))
        assertEquals(OCCURRENCE to ScriptCondition.LAUNCH_FAILED, port.fallbacks.last())

        watcher.ringSent(NEXT, ALARM, listOf(HOST))
        watcher.ringAnswered(NEXT, mapOf(HOST to RingAnswer(0, 0, 0)))
        assertEquals(NEXT to ScriptCondition.NO_SCRIPT, port.fallbacks.last())
    }

    @Test
    fun `stale hints with a live listener keep the wait`() {
        port.listening = true
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(0, 0, 1)))
        assertTrue(port.fallbacks.isEmpty())
        assertEquals(OCCURRENCE, watcher.waitingFor)
    }

    @Test
    fun `started scripts are polled every 5 s until they end`() {
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST, OTHER, THIRD))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(1, 0, 0), OTHER to RingAnswer(0, 0, 0), THIRD to RingAnswer(0, 2, 0)))
        assertTrue(port.fallbacks.isEmpty())
        assertEquals(listOf(5_000L), port.tasks.map { it.first })

        port.runTask()
        // Only the hosts that started (or already ran) scripts are polled. zh-CN: 只轮询启动了 (或已在运行) 脚本的宿主.
        assertEquals(listOf(HOST, THIRD), port.polls.single().owners)
        port.polls.removeAt(0).done(mapOf(HOST to StatusAnswer(true, 1, 1, 0), THIRD to StatusAnswer(true, 0, 2, 0)))
        assertTrue(port.fallbacks.isEmpty())
        assertEquals(listOf(5_000L), port.tasks.map { it.first })

        port.runTask()
        port.polls.removeAt(0).done(mapOf(HOST to StatusAnswer(true, 0, 1, 0), THIRD to StatusAnswer(true, 0, 2, 0)))
        assertEquals(listOf(OCCURRENCE to ScriptCondition.ENDED), port.fallbacks)
        assertTrue(port.tasks.isEmpty())
        assertNull(watcher.waitingFor)
    }

    @Test
    fun `scripts that never started fall back with launchFailed`() {
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(1, 0, 0)))
        port.runTask()
        port.polls.removeAt(0).done(mapOf(HOST to StatusAnswer(true, 0, 0, 1)))
        assertEquals(listOf(OCCURRENCE to ScriptCondition.LAUNCH_FAILED), port.fallbacks)
    }

    @Test
    fun `a host that restarted counts as ended`() {
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(1, 0, 0)))
        port.runTask()
        port.polls.removeAt(0).done(mapOf(HOST to StatusAnswer(false, 0, 0, 0)))
        assertEquals(listOf(OCCURRENCE to ScriptCondition.ENDED), port.fallbacks)
    }

    @Test
    fun `no poll answer falls back with unavailable, a live listener keeps the wait`() {
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(1, 0, 0)))
        port.runTask()
        port.listening = true
        port.polls.removeAt(0).done(mapOf(HOST to null))
        assertTrue(port.fallbacks.isEmpty())
        assertTrue(port.tasks.isEmpty())
        port.listening = false
        watcher.listenersChanged()
        assertEquals(listOf(OCCURRENCE to ScriptCondition.NO_SCRIPT), port.fallbacks)

        watcher.ringSent(NEXT, ALARM, listOf(HOST))
        watcher.ringAnswered(NEXT, mapOf(HOST to RingAnswer(1, 0, 0)))
        port.runTask()
        port.polls.removeAt(0).done(mapOf(HOST to null))
        assertEquals(NEXT to ScriptCondition.UNAVAILABLE, port.fallbacks.last())
    }

    @Test
    fun `a round that stops waiting cancels the poll and ignores late answers`() {
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(1, 0, 0)))
        // Still waiting: the ring state changed for another reason. zh-CN: 仍在等待: 响铃状态因其他原因变化.
        watcher.roundChanged()
        assertEquals(1, port.tasks.size)
        port.waiting = false
        watcher.roundChanged()
        assertTrue(port.tasks.isEmpty())
        assertNull(watcher.waitingFor)
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(0, 0, 1)))
        assertTrue(port.fallbacks.isEmpty())
    }

    @Test
    fun `a tick after the round ended does not poll`() {
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(1, 0, 0)))
        port.waiting = false
        port.runTask()
        assertTrue(port.polls.isEmpty())
        assertNull(watcher.waitingFor)
        assertTrue(port.fallbacks.isEmpty())
    }

    @Test
    fun `a new ring replaces the wait and answers of the old round are ignored`() {
        watcher.ringSent(OCCURRENCE, ALARM, listOf(HOST))
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to RingAnswer(1, 0, 0)))
        val oldTick = port.tasks.single()
        watcher.ringSent(NEXT, ALARM, listOf(HOST))
        assertTrue(oldTick.third.cancelled)
        assertEquals(NEXT, watcher.waitingFor)
        watcher.ringAnswered(OCCURRENCE, mapOf(HOST to null))
        assertTrue(port.fallbacks.isEmpty())
        oldTick.second()
        assertTrue(port.polls.isEmpty())
    }

    private class Poll(val owners: List<String>, val done: (Map<String, StatusAnswer?>) -> Unit)

    private class Handle {
        var cancelled = false
    }

    private class FakePort : ScriptChallengeWatcher.Port {
        var unlocked = true
        var listening = false
        var waiting = true
        val fallbacks = ArrayList<Pair<String, String>>()
        val polls = ArrayList<Poll>()
        val tasks = ArrayList<Triple<Long, () -> Unit, Handle>>()

        override fun unlocked() = unlocked

        override fun listening() = listening

        override fun waiting(occurrence: String) = waiting

        override fun poll(owners: List<String>, occurrence: String, alarmId: String, done: (Map<String, StatusAnswer?>) -> Unit) {
            polls += Poll(owners, done)
        }

        override fun schedule(delayMillis: Long, task: () -> Unit): () -> Unit {
            val handle = Handle()
            val entry = Triple(delayMillis, task, handle)
            tasks += entry
            return {
                handle.cancelled = true
                tasks.remove(entry)
            }
        }

        override fun fallBack(occurrence: String, reason: String) {
            fallbacks += occurrence to reason
        }

        fun runTask() {
            tasks.removeAt(0).second()
        }
    }

    private companion object {
        const val ALARM = "a1"
        const val OCCURRENCE = "a1@1000"
        const val NEXT = "a1@2000"
        const val HOST = "org.autojs.autojs6"
        const val OTHER = "org.autojs.autojs6.other"
        const val THIRD = "org.autojs.autojs6.third"
    }
}
