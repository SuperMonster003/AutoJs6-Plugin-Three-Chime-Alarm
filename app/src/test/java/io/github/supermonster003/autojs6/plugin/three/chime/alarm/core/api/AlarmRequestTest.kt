package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmChange
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event.ChangeEvents
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes.INVALID_OPTIONS
import org.autojs.plugin.alarm.api.AlarmErrorCodes.LIMIT_EXCEEDED
import org.autojs.plugin.alarm.api.AlarmErrorCodes.UNSUPPORTED
import org.autojs.plugin.alarm.api.AlarmLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P2.5 (appendix A section 11, T73): the request reader of the Binder methods and the `occurrence` of `change`
 * events.
 * zh-CN: 路线图 P2.5 (附录 A 第 11 节, T73): Binder 方法的请求读取与 `change` 事件的 `occurrence`.
 */
class AlarmRequestTest {

    @Test
    fun `the contract stamp is checked and removed`() {
        assertEquals(mapOf("id" to "a_x"), AlarmRequest.of(mapOf("contractVersion" to 1, "id" to "a_x"), setOf("id")).values)
        assertEquals(mapOf("id" to "a_x"), AlarmRequest.of(mapOf("contractVersion" to 1.0, "id" to "a_x"), setOf("id")).values)
        assertAlarmError(UNSUPPORTED) { AlarmRequest.of(mapOf("contractVersion" to 2), emptySet()) }
        assertAlarmError(UNSUPPORTED) { AlarmRequest.of(mapOf("contractVersion" to "1"), emptySet()) }
        assertAlarmError(INVALID_OPTIONS, "unknown option 'id'") { AlarmRequest.of(mapOf("id" to "a_x"), emptySet()) }
        val long = "k".repeat(100)
        val error = assertAlarmError(INVALID_OPTIONS) { AlarmRequest.of(mapOf(long to 1), emptySet()) }
        assertTrue("long keys are clipped", error.detail.length < 100)
        assertEquals(mapOf("anything" to 1), AlarmRequest.of(mapOf("anything" to 1), null).values)
    }

    @Test
    fun `typed readers treat null as absent and refuse other types`() {
        val request = AlarmRequest.of(
            mapOf("s" to "x", "b" to true, "i" to 3L, "d" to 2.0, "f" to 2.5, "m" to mapOf("k" to 1), "l" to listOf("a"), "n" to null),
            null,
        )
        assertEquals("x", request.string("s"))
        assertNull(request.string("n"))
        assertNull(request.string("absent"))
        assertEquals(true, request.requireBoolean("b"))
        assertEquals(3, request.int("i"))
        assertEquals(2, request.int("d"))
        assertEquals(mapOf("k" to 1), request.requireMap("m"))
        assertEquals(listOf("a"), request.stringList("l"))
        assertAlarmError(INVALID_OPTIONS, "'b' must be a string") { request.string("b") }
        assertAlarmError(INVALID_OPTIONS, "'s' must be a boolean") { request.boolean("s") }
        assertAlarmError(INVALID_OPTIONS, "'f' must be an integer") { request.int("f") }
        assertAlarmError(INVALID_OPTIONS, "'s' must be an object") { request.map("s") }
        assertAlarmError(INVALID_OPTIONS, "'m' must be an array of strings") { request.stringList("m") }
        assertAlarmError(INVALID_OPTIONS, "missing option 'n'") { request.requireString("n") }
    }

    @Test
    fun `counts below the minimum are invalid and above the maximum exceed the limit`() {
        fun count(value: Any?) = AlarmRequest.of(mapOf("count" to value), null).count("count", 1, 100, 7)
        assertEquals(7, count(null))
        assertEquals(1, count(1))
        assertEquals(100, count(100))
        assertAlarmError(INVALID_OPTIONS) { count(0) }
        assertAlarmError(INVALID_OPTIONS) { count(-5) }
        assertAlarmError(LIMIT_EXCEEDED) { count(101) }
        assertAlarmError(INVALID_OPTIONS) { count("3") }
    }

    @Test
    fun `a change has an occurrence of its own`() {
        val change = AlarmChange(AlarmContract.CHANGE_REMOVE, "a_abc", AlarmContract.SOURCE_TOOL, null, null)
        val first = ChangeEvents.of(change, 1_000L)
        val second = ChangeEvents.of(change, 1_000L)
        assertEquals(AlarmContract.EVENT_CHANGE, first.type)
        assertEquals(AlarmContract.CHANGE_REMOVE, first.action)
        assertEquals(AlarmContract.SOURCE_TOOL, first.source)
        assertNull(first.scheduledAt)
        assertTrue(first.occurrence, first.occurrence.matches(Regex("a_abc@change~1000~[0-9a-z]{6}")))
        assertNotEquals(first.occurrence, second.occurrence)
        val longest = ChangeEvents.occurrenceOf("a_" + "x".repeat(62), Long.MAX_VALUE, "zzzzzz")
        assertTrue(longest.length <= AlarmLimits.MAX_OCCURRENCE_CHARS)
    }
}
