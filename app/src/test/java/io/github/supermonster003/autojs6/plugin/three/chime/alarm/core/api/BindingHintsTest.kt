package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.LIMIT_EXCEEDED
import org.autojs.plugin.alarm.api.AlarmEvent
import org.autojs.plugin.alarm.api.BindingHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Roadmap P2.5 (T12, T32, T71): the hint tables of the hosts decide which trigger broadcasts go out, one table per
 * host package, kept across processes; malformed rows refuse the whole call.
 * zh-CN: 路线图 P2.5 (T12, T32, T71): 各宿主的提示表决定发出哪些触发广播, 每个宿主包一张表, 跨进程保留; 格式错误的行使整个调用失败.
 */
class BindingHintsTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val file by lazy { File(temp.root, BindingHints.FILE_NAME) }

    private val known = setOf(A, B)

    private fun row(id: String, vararg events: String, lead: Int? = null): Map<String, Any?> =
        linkedMapOf<String, Any?>("alarmId" to id, "events" to events.toList()).apply { lead?.let { put("leadSeconds", it) } }

    private fun table(vararg rows: Map<String, Any?>) = mapOf("hints" to rows.toList())

    private fun event(type: String, id: String = A, action: String? = null, kind: String = AlarmContract.KIND_ALARM) =
        AlarmEvent(type = type, kind = kind, alarmId = id, occurrence = "$id@1", firedAt = 1L, action = action)

    @Test
    fun `each host gets the triggers of its own rows`() {
        val hints = BindingHints(file)
        assertEquals(listOf("a_unknown", "t_timer"), hints.replace(HOST, table(row(A, "ring", "dismiss"), row("a_unknown", "ring"), row("t_timer", "ring")), known::contains))
        assertEquals(emptyList<String>(), hints.replace(OTHER, table(row(A, "dismiss"), row(B, "change")), known::contains))

        assertEquals(listOf(HOST), hints.targets(event("ring")))
        assertEquals(listOf(HOST, OTHER), hints.targets(event("dismiss")))
        assertEquals(emptyList<String>(), hints.targets(event("snooze")))
        assertEquals(listOf(OTHER), hints.targets(event("change", B, AlarmContract.CHANGE_UPDATE)))
        assertEquals(emptyList<String>(), hints.targets(event("ring", kind = AlarmContract.KIND_TIMER)))
        assertEquals("a removal reaches every host with a row", listOf(HOST, OTHER), hints.targets(event("change", A, AlarmContract.CHANGE_REMOVE)))
        assertEquals(emptyList<String>(), hints.targets(event("change", A, AlarmContract.CHANGE_UPDATE)))
        assertTrue(hints.hasBindings(A))
        assertFalse(hints.hasBindings("a_unknown"))
        assertEquals(listOf(BindingHint(A, linkedSetOf("ring", "dismiss"), null)), hints.table(HOST))

        assertEquals(emptyList<String>(), hints.replace(HOST, table(), known::contains))
        assertEquals(listOf(OTHER), hints.owners())
        assertEquals(listOf(OTHER), hints.targets(event("dismiss")))
    }

    @Test
    fun `the tables survive a new process and a removed alarm is forgotten`() {
        BindingHints(file).replace(HOST, table(row(A, "beforeRing", "ring", lead = 90), row(B, "ring")), known::contains)
        val restarted = BindingHints(file)
        assertEquals(listOf(BindingHint(A, linkedSetOf("beforeRing", "ring"), 90), BindingHint(B, linkedSetOf("ring"), null)), restarted.table(HOST))
        assertTrue(restarted.forget(A))
        assertEquals(listOf(B), BindingHints(file).table(HOST).map { it.alarmId })
        assertTrue(restarted.forget(B))
        assertEquals(emptyList<String>(), BindingHints(file).owners())
        assertTrue("forgetting an alarm without rows changes nothing", restarted.forget("a_none"))
    }

    @Test
    fun `a corrupt file starts empty`() {
        file.writeText("{not json", Charsets.UTF_8)
        val hints = BindingHints(file)
        assertEquals(emptyList<String>(), hints.owners())
        hints.replace(HOST, table(row(A, "ring")), known::contains)
        assertEquals(listOf(HOST), BindingHints(file).owners())
    }

    @Test
    fun `malformed tables are refused as a whole`() {
        val hints = BindingHints(file)
        hints.replace(HOST, table(row(A, "ring")), known::contains)
        assertAlarmError(INVALID_OPTIONS, "missing option 'hints'") { hints.replace(HOST, emptyMap(), known::contains) }
        assertAlarmError(INVALID_OPTIONS, "array of objects") { hints.replace(HOST, mapOf("hints" to "x"), known::contains) }
        assertAlarmError(INVALID_OPTIONS, "unknown option 'hints[0].script'") {
            hints.replace(HOST, table(row(A, "ring") + ("script" to "/sdcard/a.js")), known::contains)
        }
        assertAlarmError(INVALID_OPTIONS, "alarmId must be a string") { hints.replace(HOST, table(mapOf("alarmId" to 1, "events" to listOf("ring"))), known::contains) }
        assertAlarmError(INVALID_OPTIONS, "missing option 'hints[0].events'") { hints.replace(HOST, table(mapOf("alarmId" to A)), known::contains) }
        assertAlarmError(INVALID_OPTIONS, "unknown event 'wake'") { hints.replace(HOST, table(row(A, "wake")), known::contains) }
        assertAlarmError(INVALID_OPTIONS, "needs the beforeRing event") { hints.replace(HOST, table(row(A, "ring", lead = 60)), known::contains) }
        assertAlarmError(INVALID_OPTIONS, "15 - 300") { hints.replace(HOST, table(row(A, "beforeRing", lead = 14)), known::contains) }
        assertAlarmError(INVALID_OPTIONS, "15 - 300") { hints.replace(HOST, table(row(A, "beforeRing", lead = 301)), known::contains) }
        assertAlarmError(INVALID_OPTIONS, "appears twice") { hints.replace(HOST, table(row(A, "ring"), row(A, "dismiss")), known::contains) }
        assertAlarmError(LIMIT_EXCEEDED, "1000") {
            hints.replace(HOST, mapOf("hints" to List(BindingHints.MAX_ROWS + 1) { row("a_$it", "ring") }), known::contains)
        }
        assertEquals("a refused call keeps the table", listOf(BindingHint(A, linkedSetOf("ring"), null)), BindingHints(file).table(HOST))
        hints.replace(HOST, table(row(A, "beforeRing", lead = 15), row(B, "beforeRing", lead = 300)), known::contains)
        assertEquals(listOf(15, 300), hints.table(HOST).map { it.leadSeconds })
    }

    private companion object {
        const val HOST = "org.autojs.autojs6"
        const val OTHER = "org.autojs.autojs6.inrt"
        const val A = "a_morning"
        const val B = "a_evening"
    }
}
