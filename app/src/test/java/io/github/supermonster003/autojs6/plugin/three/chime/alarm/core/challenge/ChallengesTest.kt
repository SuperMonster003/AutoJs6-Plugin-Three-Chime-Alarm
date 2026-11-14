package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Roadmap P9.1 / P9.3 (T280 - T283): the built in dismiss challenges (problem generation and checks of `math`, the shake
 * counting of `shake`, the text match of `type`) and the emergency dismiss of five volume key presses.
 * zh-CN: 路线图 P9.1 / P9.3 (T280 - T283): 内置关闭挑战 (`math` 的出题与判定, `shake` 的摇动计数, `type` 的文字比对) 与连按 5 次音量键的紧急关闭.
 */
class ChallengesTest {

    // ------------------------------------------------------------------ math

    private val operands = Regex("""(\d+) ([+\-×]) (\d+)(?: ([+\-]) (\d+))?""")

    private fun evaluate(text: String): Long {
        val match = operands.matchEntire(text) ?: error("unexpected problem text: $text")
        val (a, op, b, op2, c) = match.destructured
        val first = when (op) {
            "+" -> a.toLong() + b.toLong()
            "-" -> a.toLong() - b.toLong()
            else -> a.toLong() * b.toLong()
        }
        return when (op2) {
            "" -> first
            "+" -> first + c.toLong()
            else -> first - c.toLong()
        }
    }

    @Test
    fun `every level writes problems in its ranges whose answer is right and never negative`() {
        val random = Random(42)
        repeat(2_000) {
            for (level in 1..3) {
                val problem = MathChallenge.generate(level, random)
                assertEquals(problem.text, evaluate(problem.text), problem.answer)
                assertTrue(problem.text, problem.answer >= 0)
                val numbers = Regex("""\d+""").findAll(problem.text).map { it.value.toInt() }.toList()
                when (level) {
                    1 -> {
                        assertEquals(2, numbers.size)
                        assertTrue(problem.text, numbers.all { it in 10..99 })
                        assertFalse(problem.text, MathChallenge.TIMES in problem.text)
                    }
                    2 -> {
                        assertEquals(3, numbers.size)
                        assertTrue(problem.text, numbers[0] in 3..9 && numbers[1] in 12..39 && numbers[2] in 10..99)
                        assertTrue(problem.text, " + " in problem.text)
                    }
                    else -> {
                        assertEquals(3, numbers.size)
                        assertTrue(problem.text, numbers[0] in 12..39 && numbers[1] in 12..39 && numbers[2] in 100..999)
                    }
                }
            }
        }
    }

    @Test
    fun `level 1 and 3 use both operations`() {
        val random = Random(7)
        val first = List(200) { MathChallenge.generate(1, random).text }
        assertTrue(first.any { " + " in it } && first.any { " - " in it })
        val third = List(200) { MathChallenge.generate(3, random).text }
        assertTrue(third.any { " + " in it } && third.any { " - " in it })
    }

    @Test
    fun `a round has the default count and level, the problems follow the seed`() {
        val round = MathChallenge(seed = 1L, difficulty = null, count = null)
        assertEquals(2, round.difficulty)
        assertEquals(3, round.count)
        assertEquals(3, round.problems.size)
        assertEquals(round.problems, MathChallenge(1L, null, null).problems)
        assertNotEquals(round.problems, MathChallenge(2L, null, null).problems)
        assertEquals(20, MathChallenge(1L, 9, 99).count)
        assertEquals(3, MathChallenge(1L, 9, 99).difficulty)
        assertEquals(1, MathChallenge(1L, 0, 0).difficulty)
        assertEquals(1, MathChallenge(1L, 0, 0).count)
    }

    @Test
    fun `answers ignore blanks and accept digits of any script`() {
        val problem = MathChallenge.Problem("3 × 12 + 10", 46)
        assertTrue(MathChallenge.check(problem, "46"))
        assertTrue(MathChallenge.check(problem, "  46 "))
        assertTrue(MathChallenge.check(problem, "+46"))
        // Arabic-Indic and full width digits. zh-CN: 阿拉伯-印度数字与全角数字.
        assertTrue(MathChallenge.check(problem, "٤٦"))
        assertTrue(MathChallenge.check(problem, "４６"))
        assertFalse(MathChallenge.check(problem, "47"))
        assertFalse(MathChallenge.check(problem, "4 6"))
        assertFalse(MathChallenge.check(problem, ""))
        assertFalse(MathChallenge.check(problem, "46.0"))
        assertEquals(-5L, MathChallenge.parse("-5"))
        assertNull(MathChallenge.parse("-"))
        assertNull(MathChallenge.parse("1234567890123"))
    }

    // ------------------------------------------------------------------ shake

    private val rest = floatArrayOf(0f, 9.81f, 0f)
    private val jerk = floatArrayOf(0f, 25f, 0f)

    private fun ShakeCounter.feed(sample: FloatArray, at: Long) = sample(sample[0], sample[1], sample[2], at)

    @Test
    fun `a shake counts once per jerk and needs the phone to settle in between`() {
        val counter = ShakeCounter(target = 3)
        assertFalse(counter.feed(rest, 0))
        assertTrue(counter.feed(jerk, 100))
        // Still above the trigger: the same jerk. zh-CN: 仍在触发值之上: 同一次猛甩.
        assertFalse(counter.feed(jerk, 500))
        assertFalse(counter.feed(rest, 600))
        assertTrue(counter.feed(jerk, 700))
        assertEquals(2, counter.count)
        assertFalse(counter.done)
    }

    @Test
    fun `shakes closer than the minimum interval count once`() {
        val counter = ShakeCounter(target = 30)
        assertTrue(counter.feed(jerk, 1_000))
        assertFalse(counter.feed(rest, 1_050))
        assertFalse(counter.feed(jerk, 1_100))
        assertFalse(counter.feed(rest, 1_350))
        assertTrue(counter.feed(jerk, 1_400))
        assertEquals(2, counter.count)
    }

    @Test
    fun `holding or tilting the phone never counts and the count stops at the target`() {
        val counter = ShakeCounter(target = 2)
        // Tilted at rest is still 1 g; 1.8 g is below the trigger. zh-CN: 静止倾斜仍是 1 g; 1.8 g 低于触发值.
        for (t in 0L..20L) assertFalse(counter.feed(floatArrayOf(6.9f, 6.9f, 0f), t * 400))
        for (t in 0L..20L) {
            counter.feed(floatArrayOf(0f, 17.6f, 0f), 10_000 + t * 400)
            counter.feed(rest, 10_200 + t * 400)
        }
        assertEquals(0, counter.count)
        var t = 20_000L
        repeat(5) {
            counter.feed(jerk, t)
            counter.feed(rest, t + 200)
            t += 400
        }
        assertEquals(2, counter.count)
        assertTrue(counter.done)
        assertEquals(2, ShakeCounter(target = 2, count = 7).count)
    }

    // ------------------------------------------------------------------ type

    @Test
    fun `typed text matches ignoring case, blanks at both ends and the Unicode form`() {
        val challenge = TypeChallenge("Good Morning")
        assertTrue(challenge.matches("good morning"))
        assertTrue(challenge.matches("  GOOD MORNING \n"))
        assertFalse(challenge.matches("goodmorning"))
        assertFalse(challenge.matches("good  morning"))
        assertFalse(challenge.matches("good morning!"))
        // e + combining acute equals the precomposed letter. zh-CN: e 加组合重音符等于预组合字母.
        assertTrue(TypeChallenge("Cafe" + 0x0301.toChar()).matches("CAF" + 0x00C9.toChar()))
        assertTrue(TypeChallenge("早上好").matches(" 早上好 "))
    }

    @Test
    fun `the text is the alarm's own, else a sentence picked by the seed`() {
        val sentences = listOf("one", "two", "three", "four")
        assertEquals("Wake up", TypeChallenge.of("  Wake up ", sentences, 1L).text)
        val picked = TypeChallenge.of(null, sentences, 5L).text
        assertTrue(picked in sentences)
        assertEquals(picked, TypeChallenge.of("   ", sentences, 5L).text)
        assertEquals(sentences.size, (0L until 200L).map { TypeChallenge.of(null, sentences, it).text }.toSet().size)
        assertEquals(TypeChallenge.FALLBACK, TypeChallenge.of(null, listOf(" "), 1L).text)
    }

    // ------------------------------------------------------------------ emergency

    @Test
    fun `five volume presses within four seconds dismiss`() {
        val presses = EmergencyPresses()
        for (t in listOf(0L, 500L, 1_000L, 1_500L)) assertFalse(presses.press(t))
        assertEquals(4, presses.count)
        assertTrue(presses.press(3_999L))
        // A new sequence starts. zh-CN: 重新开始计数.
        assertEquals(0, presses.count)
    }

    @Test
    fun `presses older than four seconds drop out`() {
        val presses = EmergencyPresses()
        for (t in listOf(0L, 1_000L, 2_000L, 3_000L)) assertFalse(presses.press(t))
        // The press at 0 is 4 s old now. zh-CN: 0 时刻那次已过 4 s.
        assertFalse(presses.press(4_000L))
        assertEquals(4, presses.count)
        assertTrue(presses.press(4_500L))
        val slow = EmergencyPresses()
        assertFalse((0L..20L).any { slow.press(it * 1_100L) })
    }
}
