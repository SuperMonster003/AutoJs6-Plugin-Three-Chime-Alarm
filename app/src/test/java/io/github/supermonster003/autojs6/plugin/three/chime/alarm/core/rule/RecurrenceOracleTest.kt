package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

/**
 * Roadmap P2.2: the rule engine against a reference written here from RFC 5545 section 3.3.10 alone (the table of the
 * BYxxx parts that expand or limit each FREQ, the defaults taken from DTSTART, BYSETPOS on the set of each period),
 * plus the plugin's documented choices: a start that does not match is not a ring, `exclude` removes dates after
 * `COUNT`, `UNTIL` is inclusive, the period of a `BYWEEKNO` rule is the week-numbering year, and a yearly
 * `BYMONTHDAY` without `BYMONTH`, `BYWEEKNO` and `BYYEARDAY` stays in the month of the start. The reference uses
 * neither lib-recur nor any code of the plugin; week numbers are computed from the fourth day of each week.
 * 1000 random rules with random starts from 1995 to 2035 are compared over the first 40 rings within 100 years,
 * plus the ring after a random moment (the skip-ahead path) and the ring after a skipped one.
 *
 * zh-CN: 路线图 P2.2: 规则引擎与参考实现对拍. 参考实现只依据 RFC 5545 第 3.3.10 节写在这里 (各 FREQ 下 BYxxx 部分扩展或限制的表, 取自 DTSTART
 * 的缺省值, BYSETPOS 作用于每个周期的集合), 再加上插件写明的选择: 不符合规则的起始日不是一次响铃, `exclude` 在 `COUNT` 之后去掉日期, `UNTIL`
 * 含边界, `BYWEEKNO` 规则的周期是周编号年, 没有 `BYMONTH`, `BYWEEKNO` 与 `BYYEARDAY` 的按年 `BYMONTHDAY` 停留在起始日所在月份. 参考实现不使用
 * lib-recur, 也不使用插件的任何代码; 周号由每周的第四天算出. 1000 个随机规则 (起始日在 1995 至 2035 年间随机) 比较 100 年内的前 40 次响铃,
 * 外加随机时刻之后的那次 (跳过前面计算的路径) 与跳过一次后的那次.
 */
class RecurrenceOracleTest {

    /** A rule as the generator builds it; the plugin only ever sees its text. zh-CN: 生成器构造的规则; 插件只看到它的文本. */
    private data class Rule(
        val freq: String,
        val interval: Int = 1,
        val count: Int? = null,
        val untilDate: LocalDate? = null,
        val untilLocal: LocalDateTime? = null,
        val untilUtc: Instant? = null,
        val byMonth: List<Int> = emptyList(),
        val byWeekNo: List<Int> = emptyList(),
        val byYearDay: List<Int> = emptyList(),
        val byMonthDay: List<Int> = emptyList(),
        val byDay: List<Pair<Int?, DayOfWeek>> = emptyList(),
        val bySetPos: List<Int> = emptyList(),
        val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ) {
        val hasByPart: Boolean get() = byMonth.isNotEmpty() || byWeekNo.isNotEmpty() || byYearDay.isNotEmpty() || byMonthDay.isNotEmpty() || byDay.isNotEmpty()

        /** The RRULE text; with [random], in a random part order and case, sometimes with the `RRULE:` prefix. zh-CN: RRULE 文本. */
        fun text(random: Random? = null): String {
            val parts = mutableListOf("FREQ=$freq")
            if (interval != 1 || random?.nextInt(4) == 0) parts += "INTERVAL=$interval"
            count?.let { parts += "COUNT=$it" }
            untilDate?.let { parts += "UNTIL=" + it.format(DateTimeFormatter.BASIC_ISO_DATE) }
            untilLocal?.let { parts += "UNTIL=" + it.format(FLOATING) }
            untilUtc?.let { parts += "UNTIL=" + UTC.format(it) }
            if (byMonth.isNotEmpty()) parts += "BYMONTH=" + byMonth.joinToString(",")
            if (byWeekNo.isNotEmpty()) parts += "BYWEEKNO=" + byWeekNo.joinToString(",")
            if (byYearDay.isNotEmpty()) parts += "BYYEARDAY=" + byYearDay.joinToString(",")
            if (byMonthDay.isNotEmpty()) parts += "BYMONTHDAY=" + byMonthDay.joinToString(",")
            if (byDay.isNotEmpty()) parts += "BYDAY=" + byDay.joinToString(",") { (n, day) ->
                val sign = if (n != null && n > 0 && random?.nextInt(4) == 0) "+" else ""
                sign + (n?.toString() ?: "") + CODES[day.value - 1]
            }
            if (bySetPos.isNotEmpty()) parts += "BYSETPOS=" + bySetPos.joinToString(",")
            if (weekStart != DayOfWeek.MONDAY || random?.nextInt(6) == 0) parts += "WKST=" + CODES[weekStart.value - 1]
            if (random == null) return parts.joinToString(";")
            parts.shuffle(random)
            val text = (if (random.nextInt(5) == 0) "RRULE:" else "") + parts.joinToString(";")
            return if (random.nextInt(5) == 0) text.lowercase() else text
        }
    }

    /** RFC 5545 section 3.3.10 expansion, written independently of the plugin. zh-CN: 按 RFC 5545 第 3.3.10 节展开, 与插件代码无关. */
    private object Reference {

        fun rings(rule: Rule, start: LocalDate, exclude: Set<LocalDate>, lastYear: Int, limit: Int): List<LocalDate> {
            var dates = dates(rule, start, lastYear)
            rule.count?.let { dates = dates.take(it) }
            return dates.filter { it !in exclude }.takeWhile { allows(rule, it) }.take(limit).toList()
        }

        fun dates(rule: Rule, start: LocalDate, lastYear: Int): Sequence<LocalDate> = sequence {
            var k = 0L
            while (true) {
                val days = period(rule, start, k++, lastYear) ?: break
                val chosen = days.filter { accepts(rule, start, it) }
                yieldAll(positions(chosen, rule.bySetPos).filter { !it.isBefore(start) && it.year <= lastYear })
            }
        }

        /** The days of the [k]-th period, null past [lastYear]. zh-CN: 第 [k] 个周期的日子, 超过 [lastYear] 时为 null. */
        private fun period(rule: Rule, start: LocalDate, k: Long, lastYear: Int): List<LocalDate>? {
            val step = k * rule.interval
            return when (rule.freq) {
                "DAILY" -> start.plusDays(step).takeIf { it.year <= lastYear }?.let { listOf(it) }
                "WEEKLY" -> start.with(TemporalAdjusters.previousOrSame(rule.weekStart)).plusWeeks(step)
                    .takeIf { it.year <= lastYear }?.let { first -> (0L until 7L).map { first.plusDays(it) } }
                "MONTHLY" -> YearMonth.from(start).plusMonths(step)
                    .takeIf { it.year <= lastYear }?.let { month -> (1..month.lengthOfMonth()).map { month.atDay(it) } }
                else -> {
                    val year = start.year + step.toInt()
                    if (year > lastYear) return null
                    if (rule.byWeekNo.isEmpty()) {
                        (1..LocalDate.of(year, 1, 1).lengthOfYear()).map { LocalDate.ofYearDay(year, it) }
                    } else {
                        // The week-numbering year: the weeks whose fourth day falls in [year].
                        // zh-CN: 周编号年: 第四天落在 [year] 的那些周.
                        val from = LocalDate.of(year, 1, 1).minusDays(7)
                        (0L until 380L).map { from.plusDays(it) }.filter { week(it, rule.weekStart).first == year }
                    }
                }
            }
        }

        private fun accepts(rule: Rule, start: LocalDate, day: LocalDate): Boolean {
            if (rule.byMonth.isNotEmpty() && day.monthValue !in rule.byMonth) return false
            if (rule.byWeekNo.isNotEmpty()) {
                val (year, number) = week(day, rule.weekStart)
                val weeks = week(LocalDate.of(year, 12, 28), rule.weekStart).second
                if (rule.byWeekNo.none { (if (it > 0) it else weeks + 1 + it) == number }) return false
            }
            if (rule.byYearDay.isNotEmpty() && rule.byYearDay.none { (if (it > 0) it else day.lengthOfYear() + 1 + it) == day.dayOfYear }) return false
            if (rule.byMonthDay.isNotEmpty() && rule.byMonthDay.none { (if (it > 0) it else day.lengthOfMonth() + 1 + it) == day.dayOfMonth }) return false
            if (rule.byDay.isNotEmpty() && rule.byDay.none { (n, weekday) -> weekday == day.dayOfWeek && (n == null || nth(rule, day, n)) }) return false
            // What the rule leaves out comes from DTSTART. zh-CN: 规则省略的部分取自 DTSTART.
            return when (rule.freq) {
                "WEEKLY" -> rule.byDay.isNotEmpty() || day.dayOfWeek == start.dayOfWeek
                "MONTHLY" -> rule.byDay.isNotEmpty() || rule.byMonthDay.isNotEmpty() || day.dayOfMonth == start.dayOfMonth
                "YEARLY" -> when {
                    rule.byWeekNo.isNotEmpty() -> rule.byDay.isNotEmpty() || rule.byMonthDay.isNotEmpty() || rule.byYearDay.isNotEmpty() || day.dayOfWeek == start.dayOfWeek
                    rule.byYearDay.isNotEmpty() -> true
                    rule.byMonthDay.isNotEmpty() -> rule.byMonth.isNotEmpty() || day.monthValue == start.monthValue
                    rule.byDay.isNotEmpty() -> true
                    else -> (rule.byMonth.isNotEmpty() || day.monthValue == start.monthValue) && day.dayOfMonth == start.dayOfMonth
                }
                else -> true
            }
        }

        /** Whether [day] is the [n]-th of its weekday in the month (MONTHLY, or YEARLY with BYMONTH) or the year. */
        private fun nth(rule: Rule, day: LocalDate, n: Int): Boolean {
            val inMonth = rule.freq == "MONTHLY" || rule.byMonth.isNotEmpty()
            val index = if (inMonth) day.dayOfMonth else day.dayOfYear
            val length = if (inMonth) day.lengthOfMonth() else day.lengthOfYear()
            return n == (index - 1) / 7 + 1 || n == -((length - index) / 7 + 1)
        }

        /** The week-numbering year and week number of [day]: the week belongs to the year of its fourth day. */
        fun week(day: LocalDate, weekStart: DayOfWeek): Pair<Int, Int> {
            val fourth = day.with(TemporalAdjusters.previousOrSame(weekStart)).plusDays(3)
            return fourth.year to (fourth.dayOfYear - 1) / 7 + 1
        }

        private fun positions(days: List<LocalDate>, setPos: List<Int>): List<LocalDate> =
            if (setPos.isEmpty()) days else days.filterIndexed { i, _ -> setPos.any { p -> (if (p > 0) p - 1 else days.size + p) == i } }

        private fun allows(rule: Rule, day: LocalDate): Boolean {
            val local = day.atTime(TIME)
            rule.untilDate?.let { return !day.isAfter(it) }
            rule.untilLocal?.let { return !local.isAfter(it) }
            rule.untilUtc?.let { return !local.atZone(ZONE).toInstant().isAfter(it) }
            return true
        }
    }

    private fun specOf(rule: String, start: LocalDate, exclude: Set<LocalDate>) = AlarmSpec(
        time = AlarmTime(TIME.hour, TIME.minute),
        date = start.toString(),
        repeat = RecurrenceRule.normalize(rule),
        exclude = exclude.sorted().map(LocalDate::toString).ifEmpty { null },
    )

    /** The plugin's rings, null for `INVALID_RULE`. zh-CN: 插件给出的响铃, `INVALID_RULE` 时为 null. */
    private fun plugin(spec: AlarmSpec, lastYear: Int, limit: Int): List<LocalDate>? = try {
        NextOccurrence.occurrences(spec, ZONE).takeWhile { it.local.year <= lastYear }.take(limit).map { it.local.toLocalDate() }.toList()
    } catch (e: AlarmException) {
        assertEquals(spec.repeat, AlarmErrorCodes.INVALID_RULE, e.code)
        null
    }

    private fun instant(day: LocalDate): Long = day.atTime(TIME).atZone(ZONE).toInstant().toEpochMilli()

    @Test
    fun `the reference and the plugin give the RFC 5545 examples`() {
        val mo = DayOfWeek.MONDAY
        val tu = DayOfWeek.TUESDAY
        val we = DayOfWeek.WEDNESDAY
        val th = DayOfWeek.THURSDAY
        val fr = DayOfWeek.FRIDAY
        val su = DayOfWeek.SUNDAY
        val weekdays = listOf(mo, tu, we, th, fr).map { null to it }
        val examples = listOf(
            Triple(Rule("DAILY", interval = 10, count = 5), "1997-09-02", "1997-09-02 1997-09-12 1997-09-22 1997-10-02 1997-10-12"),
            Triple(Rule("WEEKLY", interval = 2, count = 4, byDay = listOf(null to tu, null to su)), "1997-08-05", "1997-08-05 1997-08-10 1997-08-19 1997-08-24"),
            Triple(Rule("WEEKLY", interval = 2, count = 4, byDay = listOf(null to tu, null to su), weekStart = su), "1997-08-05", "1997-08-05 1997-08-17 1997-08-19 1997-08-31"),
            Triple(Rule("MONTHLY", count = 6, byDay = listOf(-2 to mo)), "1997-09-22", "1997-09-22 1997-10-20 1997-11-17 1997-12-22 1998-01-19 1998-02-16"),
            Triple(Rule("MONTHLY", byMonthDay = listOf(-3)), "1997-09-28", "1997-09-28 1997-10-29 1997-11-28 1997-12-29 1998-01-29 1998-02-26"),
            Triple(Rule("MONTHLY", interval = 18, count = 10, byMonthDay = (10..15).toList()), "1997-09-10", "1997-09-10 1997-09-11 1997-09-12 1997-09-13 1997-09-14 1997-09-15 1999-03-10 1999-03-11 1999-03-12 1999-03-13"),
            Triple(Rule("MONTHLY", byMonthDay = listOf(13), byDay = listOf(null to fr)), "1997-09-02", "1998-02-13 1998-03-13 1998-11-13 1999-08-13 2000-10-13"),
            Triple(Rule("MONTHLY", count = 3, byDay = listOf(null to tu, null to we, null to th), bySetPos = listOf(3)), "1997-09-04", "1997-09-04 1997-10-07 1997-11-06"),
            Triple(Rule("MONTHLY", byDay = weekdays, bySetPos = listOf(-2)), "1997-09-29", "1997-09-29 1997-10-30 1997-11-27 1997-12-30 1998-01-29 1998-02-26 1998-03-30"),
            Triple(Rule("MONTHLY", count = 5, byMonthDay = listOf(15, 30)), "2007-01-15", "2007-01-15 2007-01-30 2007-02-15 2007-03-15 2007-03-30"),
            Triple(Rule("YEARLY", interval = 3, count = 10, byYearDay = listOf(1, 100, 200)), "1997-01-01", "1997-01-01 1997-04-10 1997-07-19 2000-01-01 2000-04-09 2000-07-18 2003-01-01 2003-04-10 2003-07-19 2006-01-01"),
            Triple(Rule("YEARLY", byDay = listOf(20 to mo)), "1997-05-19", "1997-05-19 1998-05-18 1999-05-17"),
            Triple(Rule("YEARLY", byWeekNo = listOf(20), byDay = listOf(null to mo)), "1997-05-12", "1997-05-12 1998-05-11 1999-05-17"),
            Triple(Rule("YEARLY", byMonth = listOf(3), byDay = listOf(null to th)), "1997-03-13", "1997-03-13 1997-03-20 1997-03-27 1998-03-05 1998-03-12 1998-03-19 1998-03-26"),
            Triple(Rule("YEARLY", interval = 4, byMonth = listOf(11), byMonthDay = (2..8).toList(), byDay = listOf(null to tu)), "1996-11-05", "1996-11-05 2000-11-07 2004-11-02"),
        )
        for ((rule, start, rings) in examples) {
            val expected = rings.split(' ').map(LocalDate::parse)
            val day = LocalDate.parse(start)
            assertEquals(rule.text(), expected, Reference.rings(rule, day, emptySet(), day.year + 100, expected.size))
            assertEquals(rule.text(), expected, plugin(specOf(rule.text(), day, emptySet()), day.year + 100, expected.size))
        }
        assertEquals(2020 to 53, Reference.week(LocalDate.of(2021, 1, 3), DayOfWeek.MONDAY))
        assertEquals(2026 to 1, Reference.week(LocalDate.of(2025, 12, 29), DayOfWeek.MONDAY))
    }

    @Test
    fun `1000 random rules agree with the reference`() {
        val random = Random(20261105)
        val mismatches = ArrayList<String>()
        val seen = HashMap<String, Int>()
        var compared = 0
        var invalid = 0
        var rings = 0
        repeat(RULES) {
            val start = LocalDate.of(1995, 1, 1).plusDays(random.nextLong(0, 41L * 365))
            val rule = randomRule(random, start)
            val text = rule.text(random)
            val exclude = if (random.nextInt(5) == 0) (1..random.nextInt(1, 4)).mapTo(HashSet()) { start.plusDays(random.nextLong(0, 90)) } else emptySet()
            val lastYear = start.year + 100
            val expected = Reference.rings(rule, start, exclude, lastYear, LIMIT)
            val spec = specOf(text, start, exclude)
            val actual = try {
                plugin(spec, lastYear, LIMIT)
            } catch (e: RuntimeException) {
                mismatches += "$text from $start: $e"
                return@repeat
            }
            listOfNotNull(
                rule.freq, "COUNT".takeIf { rule.count != null }, "UNTIL".takeIf { rule.untilDate != null || rule.untilLocal != null || rule.untilUtc != null },
                "BYMONTH".takeIf { rule.byMonth.isNotEmpty() }, "BYWEEKNO".takeIf { rule.byWeekNo.isNotEmpty() }, "BYYEARDAY".takeIf { rule.byYearDay.isNotEmpty() },
                "BYMONTHDAY".takeIf { rule.byMonthDay.isNotEmpty() }, "BYDAY".takeIf { rule.byDay.isNotEmpty() }, "ordinal".takeIf { rule.byDay.any { it.first != null } },
                "BYSETPOS".takeIf { rule.bySetPos.isNotEmpty() }, "WKST".takeIf { rule.weekStart != DayOfWeek.MONDAY }, "exclude".takeIf { exclude.isNotEmpty() },
            ).forEach { seen.merge(it, 1, Int::plus) }
            if (actual == null) {
                invalid++
                if (Reference.dates(rule, start, lastYear).any()) mismatches += "$text from $start: INVALID_RULE, reference ${expected.take(3)}"
                return@repeat
            }
            compared++
            rings += actual.size
            if (actual != expected) {
                val at = actual.indices.firstOrNull { it >= expected.size || actual[it] != expected[it] } ?: actual.size
                mismatches += "$text from $start: ring $at is ${actual.getOrNull(at)}, reference ${expected.getOrNull(at)}"
                return@repeat
            }
            if (expected.size >= 3) {
                val i = random.nextInt(expected.size - 2)
                val after = instant(expected[i]) + random.nextLong(0, instant(expected[i + 1]) - instant(expected[i]))
                val next = NextOccurrence.next(spec, after, ZONE)?.local?.toLocalDate()
                if (next != expected[i + 1]) mismatches += "$text from $start: next after ${Instant.ofEpochMilli(after)} is $next, reference ${expected[i + 1]}"
                val skipped = NextOccurrence.next(spec, after, ZONE, skipAt = instant(expected[i + 1]))?.local?.toLocalDate()
                if (skipped != expected[i + 2]) mismatches += "$text from $start: next skipping ${expected[i + 1]} is $skipped, reference ${expected[i + 2]}"
            }
        }
        assertTrue("${mismatches.size} mismatches:\n" + mismatches.take(20).joinToString("\n"), mismatches.isEmpty())
        // The generator covers every part. zh-CN: 生成器覆盖每个部分.
        for (part in listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY", "COUNT", "UNTIL", "BYMONTH", "BYMONTHDAY", "BYDAY", "exclude", "WKST")) {
            assertTrue("$part in ${seen[part]} rules", (seen[part] ?: 0) >= 100)
        }
        for (part in listOf("BYWEEKNO", "BYYEARDAY", "ordinal", "BYSETPOS")) assertTrue("$part in ${seen[part]} rules", (seen[part] ?: 0) >= 25)
        assertTrue("compared $compared, invalid $invalid", compared >= 900)
        assertTrue("$rings rings", rings >= 15_000)
        println("oracle: $compared rules compared, $invalid INVALID_RULE, $rings rings, parts $seen")
    }

    private fun randomRule(random: Random, start: LocalDate): Rule {
        val freq = listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")[random.nextInt(4)]
        val interval = if (random.nextBoolean()) 1 else random.nextInt(2, 6)
        val weekStart = if (random.nextInt(4) == 0) DayOfWeek.of(random.nextInt(1, 8)) else DayOfWeek.MONDAY
        var rule = Rule(freq, interval, weekStart = weekStart)
        rule = when (freq) {
            "DAILY" -> rule.copy(
                byMonth = maybe(random, 4) { months(random) },
                byMonthDay = maybe(random, 4) { monthDays(random) },
                byDay = maybe(random, 2) { plainDays(random) },
            )
            "WEEKLY" -> rule.copy(byDay = maybe(random, 4, 3) { plainDays(random) }, byMonth = maybe(random, 6) { months(random) })
            "MONTHLY" -> when (random.nextInt(10)) {
                0, 1 -> rule
                2, 3, 4 -> rule.copy(byMonthDay = monthDays(random))
                5, 6 -> rule.copy(byDay = ordinalDays(random, 5))
                7, 8 -> rule.copy(byDay = plainDays(random), bySetPos = maybe(random, 4, 3) { positions(random, 5) })
                else -> rule.copy(byMonthDay = if (random.nextBoolean()) listOf(13) else (1..7).toList(), byDay = plainDays(random, 2))
            }.let { if (random.nextInt(5) == 0) it.copy(byMonth = months(random)) else it }
            else -> when (random.nextInt(20)) {
                0, 1 -> rule
                2 -> rule.copy(byMonth = months(random))
                3, 4, 5 -> rule.copy(byMonth = months(random), byMonthDay = monthDays(random))
                6 -> rule.copy(byMonthDay = monthDays(random))
                7, 8 -> rule.copy(byMonth = months(random), byDay = ordinalDays(random, 5))
                9 -> rule.copy(byDay = ordinalDays(random, 53))
                10 -> rule.copy(byDay = plainDays(random, 2), bySetPos = maybe(random, 2) { positions(random, 60) })
                11, 12 -> rule.copy(byYearDay = yearDays(random), byDay = maybe(random, 3) { plainDays(random) }, byMonth = maybe(random, 5) { months(random) })
                13, 14, 15, 16 -> rule.copy(byWeekNo = weekNumbers(random), byDay = maybe(random, 5, 3) { plainDays(random) }, byMonth = maybe(random, 5) { months(random) })
                17 -> rule.copy(byMonth = months(random), byDay = plainDays(random), bySetPos = positions(random, 10))
                18 -> rule.copy(byWeekNo = weekNumbers(random), byYearDay = yearDays(random))
                else -> rule.copy(byMonth = listOf(2), byMonthDay = listOf(listOf(29), listOf(-1), listOf(28, 29))[random.nextInt(3)])
            }
        }
        if (rule.bySetPos.isEmpty() && rule.hasByPart && random.nextInt(8) == 0) rule = rule.copy(bySetPos = positions(random, 8))
        val end = { start.plusDays(random.nextLong(0, 3000)) }
        val clock = { listOf(TIME, TIME.minusSeconds(1), LocalTime.of(23, 30), LocalTime.of(23, 29, 59), LocalTime.of(random.nextInt(24), random.nextInt(60)))[random.nextInt(5)] }
        return when (random.nextInt(10)) {
            0, 1 -> rule.copy(count = random.nextInt(1, 41))
            2 -> rule.copy(untilDate = end())
            3 -> rule.copy(untilLocal = end().atTime(clock()))
            4 -> rule.copy(untilUtc = end().atTime(clock()).toInstant(ZoneOffset.UTC))
            else -> rule
        }
    }

    /** [block] with the probability [times] / [of], otherwise nothing. zh-CN: 以 [times] / [of] 的概率取 [block], 否则为空. */
    private fun <T> maybe(random: Random, of: Int, times: Int = 1, block: () -> List<T>): List<T> =
        if (random.nextInt(of) < times) block() else emptyList()

    private fun some(random: Random, max: Int, value: () -> Int): List<Int> = (1..random.nextInt(1, max + 1)).map { value() }.distinct()

    private fun signed(random: Random, max: Int, negativeOneIn: Int): Int = random.nextInt(1, max + 1).let { if (random.nextInt(negativeOneIn) == 0) -it else it }

    private fun months(random: Random) = some(random, 3) { random.nextInt(1, 13) }

    private fun monthDays(random: Random) = some(random, 3) { if (random.nextInt(5) == 0) listOf(29, 30, 31, -1)[random.nextInt(4)] else signed(random, 31, 4) }

    private fun yearDays(random: Random) = some(random, 3) { if (random.nextInt(3) == 0) listOf(1, 59, 60, 365, 366, -1, -366)[random.nextInt(7)] else signed(random, 366, 3) }

    private fun weekNumbers(random: Random) = some(random, 3) { if (random.nextInt(2) == 0) listOf(1, 52, 53, -1, -53)[random.nextInt(5)] else signed(random, 53, 4) }

    private fun positions(random: Random, max: Int) = some(random, 2) { signed(random, max, 2) }

    private fun plainDays(random: Random, max: Int = 4): List<Pair<Int?, DayOfWeek>> =
        (1..random.nextInt(1, max + 1)).map { null to DayOfWeek.of(random.nextInt(1, 8)) }.distinct()

    private fun ordinalDays(random: Random, max: Int): List<Pair<Int?, DayOfWeek>> =
        (1..random.nextInt(1, 3)).map { signed(random, if (random.nextBoolean()) minOf(max, 5) else max, 3) to DayOfWeek.of(random.nextInt(1, 8)) }.distinct()

    companion object {
        private const val RULES = 1000
        private const val LIMIT = 40
        private val ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
        private val TIME: LocalTime = LocalTime.of(7, 30)
        private val CODES = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
        private val FLOATING: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        private val UTC: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    }
}
