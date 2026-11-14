package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P9.2 (T284): the answers of one round of broadcasts reach the script condition once, complete or after the
 * timeout with null for the hosts that did not answer.
 * zh-CN: 路线图 P9.2 (T284): 一轮广播的应答只交给脚本条件一次, 齐全时或超时后交出, 未应答的宿主为 null.
 */
class AnswerCollectorTest {

    private val results = ArrayList<Map<String, Int?>>()

    private val timeouts = ArrayList<() -> Unit>()

    private var cancelled = 0

    private fun collector(vararg owners: String) = AnswerCollector<Int>(owners.toList(), 15_000L, { _, task ->
        timeouts += task
        ({ cancelled++ })
    }) { results += it }

    @Test
    fun `the answers go out once every host answered, in the order of the hosts`() {
        val answers = collector("a", "b")
        answers.answer("b", 2)
        assertTrue(results.isEmpty())
        answers.answer("a", null)
        assertEquals(listOf(mapOf("a" to null, "b" to 2)), results)
        assertEquals(listOf("a", "b"), results.single().keys.toList())
        assertEquals(1, cancelled)
        // Late, repeated or unknown answers change nothing. zh-CN: 迟到, 重复或未知的应答不起作用.
        answers.answer("a", 1)
        answers.answer("c", 3)
        timeouts.single()()
        assertEquals(1, results.size)
    }

    @Test
    fun `the timeout hands over what came with null for the rest`() {
        val answers = collector("a", "b")
        answers.answer("a", 1)
        timeouts.single()()
        assertEquals(listOf(mapOf("a" to 1, "b" to null)), results)
        answers.answer("b", 2)
        assertEquals(1, results.size)
    }

    @Test
    fun `no hosts hand over at once without a timeout`() {
        collector()
        assertEquals(listOf(emptyMap<String, Int?>()), results)
        assertTrue(timeouts.isEmpty())
    }
}
