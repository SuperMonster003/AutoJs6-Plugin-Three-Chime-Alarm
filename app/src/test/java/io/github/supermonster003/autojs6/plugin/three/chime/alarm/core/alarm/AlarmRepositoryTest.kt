package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm

import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.IO_FAILED
import org.autojs.plugin.alarm.api.AlarmErrorCodes.LIMIT_EXCEEDED
import org.autojs.plugin.alarm.api.AlarmErrorCodes.NOT_FOUND
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * Roadmap P2.1 (T18, T48, D24): ids, sources, limits, change notifications, patch semantics and the implicit date.
 * zh-CN: 路线图 P2.1 (T18, T48, D24): id, 来源, 上限, 变更通知, patch 语义与隐式日期.
 */
class AlarmRepositoryTest {

    @get:Rule
    val temp = TemporaryFolder()

    /** 2026-11-05 08:00 in Asia/Shanghai. zh-CN: 上海时间 2026-11-05 08:00. */
    private var now = Instant.parse("2026-11-05T00:00:00Z").toEpochMilli()

    private val directory by lazy { File(temp.root, "alarms") }

    private val changes = ArrayList<AlarmChange>()

    private fun repository(planner: (AlarmRecord) -> AlarmRuntimeState = AlarmRecord::state) = AlarmRepository(
        store = AlarmStore(directory, clock = { now }),
        clock = { now },
        deviceZone = { SHANGHAI },
        planner = planner,
    ).also { it.addListener(changes::add) }

    @Test
    fun `ids, sources and timestamps`() {
        val repository = repository()
        val alarm = repository.add(optionsOf(7, 30, "label" to "Run"), "script")
        assertTrue(alarm.id, Regex("a_[0-9a-z]{12}").matches(alarm.id))
        assertEquals("script", alarm.source)
        assertEquals("script", alarm.updatedBy)
        assertEquals(now, alarm.createdAt)
        assertEquals(now, alarm.updatedAt)
        assertTrue(alarm.enabled)

        now += 60_000L
        val updated = repository.update(alarm.id, mapOf("label" to "Walk"), "tool")
        assertEquals("script", updated.source)
        assertEquals("tool", updated.updatedBy)
        assertEquals(alarm.createdAt, updated.createdAt)
        assertEquals(now, updated.updatedAt)
        assertEquals("Walk", updated.spec.label)

        now += 60_000L
        val disabled = repository.setEnabled(alarm.id, false, "user")
        assertFalse(disabled.enabled)
        assertEquals("user", disabled.updatedBy)
        assertTrue(repository.remove(alarm.id, "intent"))
        assertFalse(repository.remove(alarm.id, "intent"))

        assertEquals(listOf("add" to "script", "update" to "tool", "update" to "user", "remove" to "intent"), changes.map { it.action to it.source })
        assertEquals(alarm, changes[0].after)
        assertEquals(alarm, changes[1].before)
        assertNull(changes[3].after)
        assertTrue(changes.all { it.alarmId == alarm.id })

        val ids = (1..50).map { repository.add(optionsOf(it % 24, 0, "enabled" to false), "user").id }.toSet()
        assertEquals(50, ids.size)
    }

    @Test
    fun `unknown ids, unknown sources and unchanged states`() {
        val repository = repository()
        assertAlarmError(NOT_FOUND, "a_missing") { repository.update("a_missing", mapOf("label" to "x"), "script") }
        assertAlarmError(NOT_FOUND, "a_missing") { repository.setEnabled("a_missing", true, "script") }
        assertNull(repository.get("a_missing"))
        assertNull(repository.updateState("a_missing") { it.state })
        try {
            repository.add(optionsOf(), "robot")
            fail("an unknown source must be refused")
        } catch (_: IllegalArgumentException) {
        }
        val alarm = repository.add(optionsOf(), "script")
        changes.clear()
        assertEquals(alarm, repository.setEnabled(alarm.id, true, "tool"))
        assertTrue("enabling an enabled alarm is no change", changes.isEmpty())
        assertAlarmError(INVALID_OPTIONS, "lable") { repository.update(alarm.id, mapOf("lable" to "x"), "script") }
        assertAlarmError(INVALID_OPTIONS, "lable") { repository.add(optionsOf(7, 0, "lable" to "x"), "script") }
        assertEquals(1, repository.list().size)
    }

    @Test
    fun `at most 300 alarms, of which 200 enabled`() {
        val store = AlarmStore(directory)
        store.save(
            (1..200).map { stored("a_e$it", enabled = true) } + (1..99).map { stored("a_d$it", enabled = false) },
        )
        val repository = repository()
        assertAlarmError(LIMIT_EXCEEDED, "200 enabled") { repository.add(optionsOf(), "script") }
        assertAlarmError(LIMIT_EXCEEDED, "200 enabled") { repository.setEnabled("a_d1", true, "script") }
        assertAlarmError(LIMIT_EXCEEDED, "200 enabled") { repository.update("a_d1", mapOf("enabled" to true), "script") }
        val last = repository.add(optionsOf(7, 0, "enabled" to false), "script")
        assertEquals(300, repository.list().size)
        assertAlarmError(LIMIT_EXCEEDED, "300 alarms") { repository.add(optionsOf(7, 0, "enabled" to false), "script") }
        repository.setEnabled("a_e1", false, "user")
        assertTrue(repository.setEnabled(last.id, true, "user").enabled)
        assertEquals(200, repository.list(AlarmFilter(enabled = true)).size)
    }

    @Test
    fun `changes persist across instances and failed saves change nothing`() {
        val first = repository()
        val a = first.add(optionsOf(6, 0, "tag" to "work", "label" to "A"), "script")
        val b = first.add(optionsOf(7, 0, "tag" to "home", "label" to "B", "enabled" to false), "user")
        val c = first.add(mapOf("at" to now + 3_600_000L, "label" to "C"), "tool")
        val second = repository()
        assertEquals(listOf(a, b, c), second.list())
        assertTrue(second.loadResult!!.clean)

        assertEquals(listOf(a), second.list(AlarmFilter(tag = "work")))
        assertEquals(listOf(b), second.list(AlarmFilter(enabled = false)))
        assertEquals(listOf(a, c), second.list(AlarmFilter(sources = setOf("script", "tool"))))
        assertEquals(listOf(c), second.list(AlarmFilter(label = "C")))
        assertEquals(listOf(c), second.list(AlarmFilter.fromValues(mapOf("sources" to listOf("tool")))))
        assertAlarmError(INVALID_OPTIONS, "filter.source") { AlarmFilter.fromValues(mapOf("source" to "tool")) }
        assertAlarmError(INVALID_OPTIONS, "filter.sources") { AlarmFilter.fromValues(mapOf("sources" to listOf("robot"))) }

        // The directory turns into a file: the save fails and the list stays as it was.
        // zh-CN: 目录变成文件: 保存失败, 列表保持原样.
        directory.deleteRecursively()
        directory.writeText("not a directory")
        changes.clear()
        assertAlarmError(IO_FAILED) { second.update(a.id, mapOf("label" to "A2"), "script") }
        assertAlarmError(IO_FAILED) { second.remove(b.id, "script") }
        assertEquals(listOf(a, b, c), second.list())
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `the plugin state changes without a change notification`() {
        val repository = repository(planner = { record -> record.state.copy(nextTrigger = 42L) })
        val alarm = repository.add(optionsOf(), "script")
        assertEquals(42L, alarm.state.nextTrigger)
        assertEquals(42L, alarm.toSnapshot().nextTrigger)
        changes.clear()
        now += 1_000L
        val skipped = repository.updateState(alarm.id) { it.state.copy(skipAt = 42L) }!!
        assertTrue(skipped.toSnapshot().skipping)
        assertEquals(alarm.updatedAt, skipped.updatedAt)
        assertEquals("script", skipped.updatedBy)
        assertTrue(changes.isEmpty())
        val all = repository.updateStates { it.state.copy(handledThrough = 7L) }
        assertEquals(7L, all.single().state.handledThrough)
        assertEquals(all, repository().list())

        // Disabling drops the skip and the snooze; the snapshot shows no next trigger.
        // zh-CN: 禁用时清除跳过与贪睡, 快照不显示下一次.
        repository.updateState(alarm.id) { it.state.copy(snoozeUntil = 99L, snoozeCount = 1) }
        val off = repository(planner = { it.state }).setEnabled(alarm.id, false, "user")
        assertNull(off.state.skipAt)
        assertNull(off.state.snoozeUntil)
        assertEquals(0, off.state.snoozeCount)
        assertNull(off.toSnapshot().nextTrigger)
        assertFalse(off.toSnapshot().skipping)
    }

    @Test
    fun `a failing listener neither undoes the change nor starves the others`() {
        val repository = repository()
        val seen = ArrayList<String>()
        repository.addListener { throw IllegalStateException("boom") }
        repository.addListener { seen += it.action }
        repository.add(optionsOf(), "script")
        assertEquals(listOf("add"), seen)
        assertEquals(1, repository().list().size)
    }

    @Test
    fun `the implicit date follows the time and moves on when enabled again (T48)`() {
        val repository = repository()
        val later = repository.add(optionsOf(9, 0), "script")
        assertEquals("2026-11-05", later.spec.date)
        assertTrue(later.state.dateImplicit)
        val passed = repository.add(optionsOf(7, 30), "script")
        assertEquals("2026-11-06", passed.spec.date)
        val exact = repository.add(optionsOf(8, 0), "script")
        assertEquals("the current minute is reached already", "2026-11-06", exact.spec.date)
        val daily = repository.add(optionsOf(7, 30, "repeat" to "FREQ=DAILY"), "script")
        assertEquals("2026-11-05", daily.spec.date)
        val newYork = repository.add(optionsOf(21, 0, "timeZone" to "America/New_York"), "script")
        assertEquals("19:00 on 11-04 in New York, 21:00 is still ahead", "2026-11-04", newYork.spec.date)
        val explicit = repository.add(optionsOf(7, 30, "date" to "2026-12-01"), "script")
        assertEquals("2026-12-01", explicit.spec.date)
        assertFalse(explicit.state.dateImplicit)
        assertNull(repository.add(mapOf("at" to now + 1L), "script").spec.date)

        // A new time moves an implicit date along; other changes keep it.
        // zh-CN: 修改时刻时隐式日期随之更新; 其他修改保留日期.
        assertEquals("2026-11-06", repository.update(later.id, mapOf("hour" to 7, "minute" to 0), "script").spec.date)
        now += 86_400_000L
        assertEquals("2026-11-06", repository.update(later.id, mapOf("label" to "x"), "script").spec.date)
        val pinned = repository.update(later.id, mapOf("date" to "2026-11-20"), "script")
        assertFalse(pinned.state.dateImplicit)
        assertEquals("2026-11-20", repository.update(later.id, mapOf("hour" to 6, "minute" to 0), "script").spec.date)

        // Enabled again after its moment passed: the next day the time is reached (now 2026-12-02 08:00).
        // zh-CN: 时刻已过后再次启用: 改到下一个到达该时刻的日子 (此时为 2026-12-02 08:00).
        repository.setEnabled(explicit.id, false, "user")
        now = Instant.parse("2026-12-02T00:00:00Z").toEpochMilli()
        val again = repository.setEnabled(explicit.id, true, "user")
        assertEquals("2026-12-03", again.spec.date)
        assertTrue(again.state.dateImplicit)
        repository.setEnabled(explicit.id, false, "user")
        val pinnedAgain = repository.update(explicit.id, mapOf("enabled" to true, "date" to "2026-12-02"), "user")
        assertEquals("an explicit date in the same patch wins", "2026-12-02", pinnedAgain.spec.date)
    }

    private fun stored(id: String, enabled: Boolean) = AlarmRecord(
        id = id,
        source = "script",
        updatedBy = "script",
        createdAt = 1L,
        updatedAt = 1L,
        spec = AlarmSpec(time = AlarmTime(7, 0), date = "2026-11-06", enabled = enabled),
    )

    companion object {
        private val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
    }
}
