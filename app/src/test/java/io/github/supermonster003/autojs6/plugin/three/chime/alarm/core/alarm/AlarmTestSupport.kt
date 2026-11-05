package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

/** Asserts that [block] throws [AlarmException] with [code], optionally naming [mention]. zh-CN: 断言 [block] 抛出带 [code] 的 [AlarmException]. */
internal fun assertAlarmError(code: String, mention: String? = null, block: () -> Unit): AlarmException {
    try {
        block()
    } catch (e: AlarmException) {
        assertEquals("error code of '${e.message}'", code, e.code)
        if (mention != null) assertTrue("'${e.message}' should mention '$mention'", e.detail.contains(mention))
        return e
    }
    fail("expected $code")
    throw AssertionError()
}

/** The options of an alarm at [hour]:[minute] plus [extra]. zh-CN: [hour]:[minute] 闹钟的选项加上 [extra]. */
internal fun optionsOf(hour: Int = 7, minute: Int = 30, vararg extra: Pair<String, Any?>): Map<String, Any?> =
    linkedMapOf<String, Any?>("hour" to hour, "minute" to minute).apply { extra.forEach { (k, v) -> put(k, v) } }

/** A string of [count] code points outside the BMP (two chars each). zh-CN: [count] 个 BMP 之外的码点 (每个两个 char). */
internal fun astral(count: Int): String = buildString { repeat(count) { appendCodePoint(0x1F514) } }
