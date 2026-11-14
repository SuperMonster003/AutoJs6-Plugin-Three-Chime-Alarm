package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

import org.autojs.plugin.alarm.api.AlarmLimits
import kotlin.random.Random

/**
 * The `math` dismiss challenge (roadmap P9.1, T280): [count] problems of [difficulty] 1 - 3, answered one after the
 * other; a wrong answer keeps the problem. The problems of a round come from a [seed] (the hash of its `occurrence`),
 * so a ringing screen that is created again shows the same ones.
 *
 * - 1: `a + b` or `a - b`, both 10 - 99, never negative.
 * - 2: `a × b + c`, a 3 - 9, b 12 - 39, c 10 - 99.
 * - 3: `a × b + c` or `a × b - c`, a and b 12 - 39, c 100 - 999, never negative.
 *
 * zh-CN: 关闭挑战 `math` (路线图 P9.1, T280): 难度 [difficulty] 1 - 3 的 [count] 道题依次作答, 答错保留原题. 一轮的题目由种子 [seed] (其 `occurrence`
 * 的哈希) 生成, 响铃界面重建后显示同样的题目. 1 级: `a + b` 或 `a - b`, 两数 10 - 99, 结果非负; 2 级: `a × b + c`, a 3 - 9, b 12 - 39, c 10 - 99;
 * 3 级: `a × b + c` 或 `a × b - c`, a 与 b 12 - 39, c 100 - 999, 结果非负.
 */
class MathChallenge(seed: Long, difficulty: Int?, count: Int?) {

    /** One problem: the [text] shown (left to right in every language) and its [answer]. zh-CN: 一道题: 显示的 [text] (各语言都从左到右) 与答案 [answer]. */
    data class Problem(val text: String, val answer: Long)

    val difficulty: Int = (difficulty ?: AlarmLimits.DEFAULT_MATH_DIFFICULTY).coerceIn(AlarmLimits.MIN_MATH_DIFFICULTY, AlarmLimits.MAX_MATH_DIFFICULTY)

    val count: Int = (count ?: AlarmLimits.DEFAULT_MATH_COUNT).coerceIn(1, AlarmLimits.MAX_MATH_COUNT)

    val problems: List<Problem> = Random(seed).let { random -> List(this.count) { generate(this.difficulty, random) } }

    companion object {

        /** The multiplication sign of the problem text. zh-CN: 题目文字里的乘号. */
        const val TIMES = "×"

        /** A problem of [difficulty]; see [MathChallenge]. zh-CN: 一道难度为 [difficulty] 的题, 见 [MathChallenge]. */
        fun generate(difficulty: Int, random: Random): Problem = when (difficulty) {
            1 -> {
                val a = random.nextInt(10, 100)
                val b = random.nextInt(10, 100)
                if (random.nextBoolean()) Problem("$a + $b", (a + b).toLong())
                else Problem("${maxOf(a, b)} - ${minOf(a, b)}", (maxOf(a, b) - minOf(a, b)).toLong())
            }
            2 -> {
                val a = random.nextInt(3, 10)
                val b = random.nextInt(12, 40)
                val c = random.nextInt(10, 100)
                Problem("$a $TIMES $b + $c", (a * b + c).toLong())
            }
            else -> {
                val a = random.nextInt(12, 40)
                val b = random.nextInt(12, 40)
                val c = random.nextInt(100, 1000)
                val product = a * b
                if (random.nextBoolean() && product >= c) Problem("$a $TIMES $b - $c", (product - c).toLong())
                else Problem("$a $TIMES $b + $c", (product + c).toLong())
            }
        }

        /**
         * The number [input] means, null when it is not one: surrounding blanks are ignored, digits of any script
         * (Arabic-Indic, full width, ...) count, and a leading `-` or `+` is allowed.
         * zh-CN: [input] 表示的数, 不是数时为 null: 忽略首尾空白, 接受任何文字的数字 (阿拉伯-印度数字, 全角数字等), 允许开头的 `-` 或 `+`.
         */
        fun parse(input: String): Long? {
            val text = input.trim()
            if (text.isEmpty() || text.length > 12) return null
            val sign = when (text[0]) {
                '-', '−' -> -1L
                '+' -> 1L
                else -> 0L
            }
            val digits = if (sign != 0L) text.substring(1).trim() else text
            if (digits.isEmpty()) return null
            var value = 0L
            for (char in digits) {
                val digit = Character.digit(char, 10)
                if (digit < 0) return null
                value = value * 10 + digit
            }
            return if (sign < 0) -value else value
        }

        /** Whether [input] answers [problem]. zh-CN: [input] 是否答对 [problem]. */
        fun check(problem: Problem, input: String): Boolean = parse(input) == problem.answer
    }
}
