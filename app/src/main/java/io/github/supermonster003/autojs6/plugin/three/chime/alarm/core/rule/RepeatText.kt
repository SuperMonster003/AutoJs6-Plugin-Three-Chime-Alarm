package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * The localized description of a repeat rule (`Alarm.repeatText`, roadmap P2.2, appendix A section 4.3) in the ten
 * languages of the plugin: every day, every N days, Monday to Friday, weekends, every week on some days, every N weeks,
 * days of the month, the last day of the month, the Nth or last weekday of the month, and once a year. Any other rule
 * (`COUNT` / `UNTIL`, `BYWEEKNO`, `BYYEARDAY`, several months, monthly intervals and so on) is described by its
 * normalized RRULE text. A rule without its own days (`FREQ=WEEKLY`, `FREQ=MONTHLY`, `FREQ=YEARLY`) takes them from
 * [start] (the `date` of the alarm) when it is known. The texts live here instead of in string resources so the JVM
 * tests check every language without Android (roadmap T51); punctuation is ASCII in every language.
 *
 * zh-CN: 重复规则的本地化描述 (`Alarm.repeatText`, 路线图 P2.2, 附录 A 第 4.3 节), 覆盖插件的 10 种语言: 每天, 每 N 天, 周一至周五, 周末, 每周某几天,
 * 每 N 周, 每月某几日, 每月最后一天, 每月第 N 个或最后一个星期 X, 每年一次. 其他规则 (`COUNT` / `UNTIL`, `BYWEEKNO`, `BYYEARDAY`, 多个月份, 按月的
 * 间隔等) 以规范化的 RRULE 原文描述. 规则本身没有日子时 (`FREQ=WEEKLY`, `FREQ=MONTHLY`, `FREQ=YEARLY`), 在已知 [start] (闹钟的 `date`) 时从它取.
 * 文案放在这里而非字符串资源, JVM 测试不需要 Android 就能检查每种语言 (路线图 T51); 每种语言都只用 ASCII 标点.
 */
object RepeatText {

    /** The language tags the plugin is translated into. zh-CN: 插件翻译覆盖的语言标签. */
    val LANGUAGES: List<String> = listOf("zh-Hans", "zh-Hant-TW", "zh-Hant-HK", "en", "ja", "ko", "es", "fr", "ru", "ar")

    /** The description of [rule] (any accepted RRULE string) in [locale]. zh-CN: [rule] (任一可接受的 RRULE 字符串) 在 [locale] 下的描述. */
    fun of(rule: String, start: LocalDate? = null, locale: Locale = Locale.getDefault()): String =
        describe(RecurrenceRule.parse(rule), start, phrasesOf(languageOf(locale)))

    /** The plugin language used for [locale] (English for other languages). zh-CN: [locale] 对应的插件语言 (其他语言用英语). */
    fun languageOf(locale: Locale): String = when (locale.language) {
        "zh" -> when {
            locale.country == "HK" || locale.country == "MO" -> "zh-Hant-HK"
            locale.country == "TW" || locale.script == "Hant" -> "zh-Hant-TW"
            else -> "zh-Hans"
        }
        "ja", "ko", "es", "fr", "ru", "ar" -> locale.language
        else -> "en"
    }

    /**
     * The description of an alarm in `holidays.mode` `workdays` that rings on every workday (roadmap P8.3, T184).
     * zh-CN: `holidays.mode` 为 `workdays` 且每个工作日都响的闹钟的描述 (路线图 P8.3, T184).
     */
    fun workdays(locale: Locale = Locale.getDefault()): String = phrasesOf(languageOf(locale)).workdays()

    internal fun describe(rule: RecurrenceRule, start: LocalDate?, phrases: Phrases): String =
        shape(rule, start)?.let { phrases.text(it) } ?: rule.text

    internal fun phrasesOf(language: String): Phrases = when (language) {
        "zh-Hans" -> ZhHans
        "zh-Hant-TW" -> ZhHantTw
        "zh-Hant-HK" -> ZhHantHk
        "ja" -> Ja
        "ko" -> Ko
        "es" -> Es
        "fr" -> Fr
        "ru" -> Ru
        "ar" -> Ar
        else -> En
    }

    /** The describable forms. zh-CN: 可描述的形态. */
    internal sealed class Shape {
        data object Daily : Shape()
        data class EveryNDays(val n: Int) : Shape()
        data object Weekdays : Shape()
        data object Weekends : Shape()
        data class Weekly(val days: List<DayOfWeek>) : Shape()
        data class EveryNWeeks(val n: Int, val days: List<DayOfWeek>) : Shape()
        data class MonthlyDays(val days: List<Int>) : Shape()
        data object MonthlyLastDay : Shape()
        data class MonthlyNth(val ordinal: Int, val day: DayOfWeek) : Shape()
        data class Yearly(val month: Int, val day: Int) : Shape()
    }

    private val MONDAY_TO_FRIDAY = DayOfWeek.entries.take(5)
    private val WEEKEND = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    internal fun shape(rule: RecurrenceRule, start: LocalDate?): Shape? {
        if (rule.count != null || rule.until != null || rule.byWeekNo.isNotEmpty() || rule.byYearDay.isNotEmpty()) return null
        if (!rule.plainDays && rule.freq != RecurrenceRule.Freq.MONTHLY) return null
        val days = rule.byDay.map { it.day }
        return when (rule.freq) {
            RecurrenceRule.Freq.DAILY -> when {
                rule.byMonth.isNotEmpty() || rule.byMonthDay.isNotEmpty() || rule.bySetPos.isNotEmpty() -> null
                days.isEmpty() -> if (rule.interval == 1) Shape.Daily else Shape.EveryNDays(rule.interval)
                rule.interval == 1 -> weekly(days)
                else -> null
            }
            RecurrenceRule.Freq.WEEKLY -> {
                if (rule.byMonth.isNotEmpty() || rule.bySetPos.isNotEmpty()) return null
                val weekDays = days.ifEmpty { listOf(start?.dayOfWeek ?: return null) }
                when {
                    rule.interval == 1 -> weekly(weekDays)
                    rule.weekStart != DayOfWeek.MONDAY && weekDays.size > 1 -> null
                    else -> Shape.EveryNWeeks(rule.interval, weekDays)
                }
            }
            RecurrenceRule.Freq.MONTHLY -> {
                if (rule.interval != 1 || rule.byMonth.isNotEmpty()) return null
                monthly(rule, start)
            }
            RecurrenceRule.Freq.YEARLY -> {
                if (rule.interval != 1 || rule.byDay.isNotEmpty() || rule.bySetPos.isNotEmpty()) return null
                when {
                    rule.byMonth.size == 1 && rule.byMonthDay.size == 1 && rule.byMonthDay[0] > 0 -> Shape.Yearly(rule.byMonth[0], rule.byMonthDay[0])
                    rule.byMonth.isEmpty() && rule.byMonthDay.isEmpty() && start != null -> Shape.Yearly(start.monthValue, start.dayOfMonth)
                    rule.byMonth.size == 1 && rule.byMonthDay.isEmpty() && start != null -> Shape.Yearly(rule.byMonth[0], start.dayOfMonth)
                    rule.byMonth.isEmpty() && rule.byMonthDay.size == 1 && rule.byMonthDay[0] > 0 && start != null -> Shape.Yearly(start.monthValue, rule.byMonthDay[0])
                    else -> null
                }
            }
        }
    }

    private fun weekly(days: List<DayOfWeek>): Shape = when (days.sorted()) {
        DayOfWeek.entries -> Shape.Daily
        MONDAY_TO_FRIDAY -> Shape.Weekdays
        WEEKEND -> Shape.Weekends
        else -> Shape.Weekly(days.sorted())
    }

    private fun monthly(rule: RecurrenceRule, start: LocalDate?): Shape? {
        val byDay = rule.byDay
        val setPos = rule.bySetPos
        return when {
            byDay.isEmpty() && setPos.isEmpty() -> when {
                rule.byMonthDay.isEmpty() -> start?.let { Shape.MonthlyDays(listOf(it.dayOfMonth)) }
                rule.byMonthDay == listOf(-1) -> Shape.MonthlyLastDay
                rule.byMonthDay.all { it > 0 } -> Shape.MonthlyDays(rule.byMonthDay)
                else -> null
            }
            rule.byMonthDay.isNotEmpty() || byDay.size != 1 -> null
            byDay[0].ordinal != null && setPos.isEmpty() -> nth(byDay[0].ordinal!!, byDay[0].day)
            byDay[0].ordinal == null && setPos.size == 1 -> nth(setPos[0], byDay[0].day)
            else -> null
        }
    }

    private fun nth(ordinal: Int, day: DayOfWeek): Shape? =
        if (ordinal in 1..5 || ordinal == -1) Shape.MonthlyNth(ordinal, day) else null

    /** The texts of one language. zh-CN: 一种语言的文案. */
    internal abstract class Phrases {
        abstract fun daily(): String
        abstract fun everyNDays(n: Int): String
        abstract fun weekdays(): String
        abstract fun weekends(): String
        abstract fun weekly(days: List<DayOfWeek>): String
        abstract fun everyNWeeks(n: Int, days: List<DayOfWeek>): String
        abstract fun monthlyDays(days: List<Int>): String
        abstract fun monthlyLastDay(): String

        /** [ordinal] is 1 to 5, or -1 for the last one. zh-CN: [ordinal] 为 1 至 5, -1 表示最后一个. */
        abstract fun monthlyNth(ordinal: Int, day: DayOfWeek): String
        abstract fun yearly(month: Int, day: Int): String

        /** Every workday of the holiday calendar (`holidays.mode` `workdays`, T184). zh-CN: 节假日日历的每个工作日 (T184). */
        abstract fun workdays(): String

        fun text(shape: Shape): String = when (shape) {
            Shape.Daily -> daily()
            is Shape.EveryNDays -> everyNDays(shape.n)
            Shape.Weekdays -> weekdays()
            Shape.Weekends -> weekends()
            is Shape.Weekly -> weekly(shape.days)
            is Shape.EveryNWeeks -> everyNWeeks(shape.n, shape.days)
            is Shape.MonthlyDays -> monthlyDays(shape.days)
            Shape.MonthlyLastDay -> monthlyLastDay()
            is Shape.MonthlyNth -> monthlyNth(shape.ordinal, shape.day)
            is Shape.Yearly -> yearly(shape.month, shape.day)
        }

        /** Joins with [separator], the last two with [last]. zh-CN: 以 [separator] 连接, 最后两项之间用 [last]. */
        protected fun <T> List<T>.joinWith(separator: String, last: String = separator): String = when (size) {
            0 -> ""
            1 -> this[0].toString()
            else -> dropLast(1).joinToString(separator) + last + this.last()
        }

        protected fun name(names: List<String>, day: DayOfWeek): String = names[day.value - 1]
    }

    private object ZhHans : Phrases() {
        private val short = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        private val long = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
        override fun daily() = "每天"
        override fun everyNDays(n: Int) = "每 $n 天"
        override fun weekdays() = "周一至周五"
        override fun weekends() = "周末"
        override fun weekly(days: List<DayOfWeek>) = "每" + days.map { name(short, it) }.joinWith(", ")
        override fun everyNWeeks(n: Int, days: List<DayOfWeek>) = "每 $n 周的" + days.map { name(short, it) }.joinWith(", ")
        override fun monthlyDays(days: List<Int>) = "每月 " + days.map { "$it 日" }.joinWith(", ")
        override fun monthlyLastDay() = "每月最后一天"
        override fun monthlyNth(ordinal: Int, day: DayOfWeek) =
            if (ordinal == -1) "每月最后一个${name(long, day)}" else "每月第 $ordinal 个${name(long, day)}"
        override fun yearly(month: Int, day: Int) = "每年 $month 月 $day 日"
        override fun workdays() = "工作日"
    }

    private object ZhHantTw : Phrases() {
        private val short = listOf("週一", "週二", "週三", "週四", "週五", "週六", "週日")
        private val long = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
        override fun daily() = "每天"
        override fun everyNDays(n: Int) = "每 $n 天"
        override fun weekdays() = "週一至週五"
        override fun weekends() = "週末"
        override fun weekly(days: List<DayOfWeek>) = "每" + days.map { name(short, it) }.joinWith(", ")
        override fun everyNWeeks(n: Int, days: List<DayOfWeek>) = "每 $n 週的" + days.map { name(short, it) }.joinWith(", ")
        override fun monthlyDays(days: List<Int>) = "每月 " + days.map { "$it 日" }.joinWith(", ")
        override fun monthlyLastDay() = "每月最後一天"
        override fun monthlyNth(ordinal: Int, day: DayOfWeek) =
            if (ordinal == -1) "每月最後一個${name(long, day)}" else "每月第 $ordinal 個${name(long, day)}"
        override fun yearly(month: Int, day: Int) = "每年 $month 月 $day 日"
        override fun workdays() = "工作日"
    }

    private object ZhHantHk : Phrases() {
        private val long = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
        override fun daily() = "每日"
        override fun everyNDays(n: Int) = "每 $n 日"
        override fun weekdays() = "星期一至星期五"
        override fun weekends() = "週末"
        override fun weekly(days: List<DayOfWeek>) = "逢" + days.map { name(long, it) }.joinWith(", ")
        override fun everyNWeeks(n: Int, days: List<DayOfWeek>) = "每 $n 個星期的" + days.map { name(long, it) }.joinWith(", ")
        override fun monthlyDays(days: List<Int>) = "每月 " + days.map { "$it 日" }.joinWith(", ")
        override fun monthlyLastDay() = "每月最後一日"
        override fun monthlyNth(ordinal: Int, day: DayOfWeek) =
            if (ordinal == -1) "每月最後一個${name(long, day)}" else "每月第 $ordinal 個${name(long, day)}"
        override fun yearly(month: Int, day: Int) = "每年 $month 月 $day 日"
        override fun workdays() = "工作日"
    }

    private object En : Phrases() {
        private val names = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        private val ordinals = listOf("first", "second", "third", "fourth", "fifth")
        private val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        private fun list(days: List<DayOfWeek>) = days.map { name(names, it) }.joinWith(", ", " and ")
        private fun ordinal(n: Int) = n.toString() + when {
            n % 100 in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }
        override fun daily() = "Every day"
        override fun everyNDays(n: Int) = "Every $n days"
        override fun weekdays() = "Monday to Friday"
        override fun weekends() = "Weekends"
        override fun weekly(days: List<DayOfWeek>) = "Every " + list(days)
        override fun everyNWeeks(n: Int, days: List<DayOfWeek>) = "Every $n weeks on " + list(days)
        override fun monthlyDays(days: List<Int>) = "Every month on the " + days.map(::ordinal).joinWith(", ", " and ")
        override fun monthlyLastDay() = "Every month on the last day"
        override fun monthlyNth(ordinal: Int, day: DayOfWeek) =
            "Every month on the ${if (ordinal == -1) "last" else ordinals[ordinal - 1]} ${name(names, day)}"
        override fun yearly(month: Int, day: Int) = "Every year on ${months[month - 1]} $day"
        override fun workdays() = "Workdays"
    }

    private object Ja : Phrases() {
        private val names = listOf("月曜日", "火曜日", "水曜日", "木曜日", "金曜日", "土曜日", "日曜日")
        override fun daily() = "毎日"
        override fun everyNDays(n: Int) = "$n 日ごと"
        override fun weekdays() = "月曜日から金曜日"
        override fun weekends() = "週末"
        override fun weekly(days: List<DayOfWeek>) = "毎週" + days.map { name(names, it) }.joinWith(", ")
        override fun everyNWeeks(n: Int, days: List<DayOfWeek>) = "$n 週間ごとの" + days.map { name(names, it) }.joinWith(", ")
        override fun monthlyDays(days: List<Int>) = "毎月 " + days.map { "$it 日" }.joinWith(", ")
        override fun monthlyLastDay() = "毎月末日"
        override fun monthlyNth(ordinal: Int, day: DayOfWeek) =
            if (ordinal == -1) "毎月最終${name(names, day)}" else "毎月第 $ordinal ${name(names, day)}"
        override fun yearly(month: Int, day: Int) = "毎年 $month 月 $day 日"
        override fun workdays() = "勤務日"
    }

    private object Ko : Phrases() {
        private val names = listOf("월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일")
        private val ordinals = listOf("첫째", "둘째", "셋째", "넷째", "다섯째")
        override fun daily() = "매일"
        override fun everyNDays(n: Int) = "${n}일마다"
        override fun weekdays() = "월요일부터 금요일까지"
        override fun weekends() = "주말"
        override fun weekly(days: List<DayOfWeek>) = "매주 " + days.map { name(names, it) }.joinWith(", ")
        override fun everyNWeeks(n: Int, days: List<DayOfWeek>) = "${n}주마다 " + days.map { name(names, it) }.joinWith(", ")
        override fun monthlyDays(days: List<Int>) = "매월 " + days.map { "${it}일" }.joinWith(", ")
        override fun monthlyLastDay() = "매월 마지막 날"
        override fun monthlyNth(ordinal: Int, day: DayOfWeek) =
            "매월 ${if (ordinal == -1) "마지막" else ordinals[ordinal - 1]} ${name(names, day)}"
        override fun yearly(month: Int, day: Int) = "매년 ${month}월 ${day}일"
        override fun workdays() = "근무일"
    }

    private object Es : Phrases() {
        private val singular = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
        private val plural = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábados", "domingos")
        private val ordinals = listOf("primer", "segundo", "tercer", "cuarto", "quinto")
        private val months = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        private fun list(days: List<DayOfWeek>) = days.map { name(plural, it) }.joinWith(", ", " y ")
        override fun daily() = "Todos los días"
        override fun everyNDays(n: Int) = "Cada $n días"
        override fun weekdays() = "De lunes a viernes"
        override fun weekends() = "Los fines de semana"
        override fun weekly(days: List<DayOfWeek>) = "Todos los " + list(days)
        override fun everyNWeeks(n: Int, days: List<DayOfWeek>) = "Cada $n semanas, los " + list(days)
        override fun monthlyDays(days: List<Int>) =
            (if (days.size == 1) "El día " else "Los días ") + days.joinWith(", ", " y ") + " de cada mes"
        override fun monthlyLastDay() = "El último día de cada mes"
        override fun monthlyNth(ordinal: Int, day: DayOfWeek) =
            "El ${if (ordinal == -1) "último" else ordinals[ordinal - 1]} ${name(singular, day)} de cada mes"
        override fun yearly(month: Int, day: Int) = "Cada año el $day de ${months[month - 1]}"
        override fun workdays() = "Días laborables"
    }

    private object Fr : Phrases() {
        private val singular = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
        private val ordinals = listOf("premier", "deuxième", "troisième", "quatrième", "cinquième")
        private val months = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")
        private fun day(n: Int) = if (n == 1) "1er" else n.toString()
        override fun daily() = "Tous les jours"
        override fun everyNDays(n: Int) = "Tous les $n jours"
        override fun weekdays() = "Du lundi au vendredi"
        override fun weekends() = "Le week-end"
        override fun weekly(days: List<DayOfWeek>) = "Tous les " + days.map { name(singular, it) + "s" }.joinWith(", ", " et ")
        override fun everyNWeeks(n: Int, days: List<DayOfWeek>) =
            "Toutes les $n semaines, le " + days.map { name(singular, it) }.joinWith(", le ", " et le ")
        override fun monthlyDays(days: List<Int>) = "Le " + days.map(::day).joinWith(", le ", " et le ") + " de chaque mois"
        override fun monthlyLastDay() = "Le dernier jour de chaque mois"
        override fun monthlyNth(ordinal: Int, day: DayOfWeek) =
            "Le ${if (ordinal == -1) "dernier" else ordinals[ordinal - 1]} ${name(singular, day)} de chaque mois"
        override fun yearly(month: Int, day: Int) = "Tous les ans le ${day(day)} ${months[month - 1]}"
        override fun workdays() = "Jours ouvrés"
    }

    private object Ru : Phrases() {
        // Dative plural ("по понедельникам"), accusative singular ("в среду") and the grammatical gender of each day.
        // zh-CN: 复数与格 ("по понедельникам"), 单数宾格 ("в среду") 与各星期名称的语法性别.
        private val dative = listOf("понедельникам", "вторникам", "средам", "четвергам", "пятницам", "субботам", "воскресеньям")
        private val accusative = listOf("понедельник", "вторник", "среду", "четверг", "пятницу", "субботу", "воскресенье")
        private val gender = listOf('m', 'm', 'f', 'm', 'f', 'f', 'n')
        private val ordinals = mapOf(
            'm' to listOf("первый", "второй", "третий", "четвёртый", "пятый", "последний"),
            'f' to listOf("первую", "вторую", "третью", "четвёртую", "пятую", "последнюю"),
            'n' to listOf("первое", "второе", "третье", "четвёртое", "пятое", "последнее"),
        )
        private val months = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
        private fun plural(n: Int, one: String, few: String, many: String) = when {
            n % 10 == 1 && n % 100 != 11 -> one
            n % 10 in 2..4 && n % 100 !in 12..14 -> few
            else -> many
        }
        private fun list(days: List<DayOfWeek>) = days.map { name(dative, it) }.joinWith(", ", " и ")
        override fun daily() = "Каждый день"
        override fun everyNDays(n: Int) = plural(n, "Каждый $n день", "Каждые $n дня", "Каждые $n дней")
        override fun weekdays() = "С понедельника по пятницу"
        override fun weekends() = "По выходным"
        override fun weekly(days: List<DayOfWeek>) = "По " + list(days)
        override fun everyNWeeks(n: Int, days: List<DayOfWeek>) =
            plural(n, "Каждую $n неделю", "Каждые $n недели", "Каждые $n недель") + " по " + list(days)
        override fun monthlyDays(days: List<Int>) = days.joinWith(", ", " и ") + " числа каждого месяца"
        override fun monthlyLastDay() = "В последний день каждого месяца"
        override fun monthlyNth(ordinal: Int, day: DayOfWeek): String {
            val word = ordinals.getValue(gender[day.value - 1])[if (ordinal == -1) 5 else ordinal - 1]
            val preposition = if (word.startsWith("втор")) "Во" else "В"
            return "$preposition $word ${name(accusative, day)} каждого месяца"
        }
        override fun yearly(month: Int, day: Int) = "Каждый год $day ${months[month - 1]}"
        override fun workdays() = "По рабочим дням"
    }

    private object Ar : Phrases() {
        private val names = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
        private val bare = listOf("اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت", "أحد")
        private val ordinals = listOf("أول", "ثاني", "ثالث", "رابع", "خامس")
        private val months = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        private fun list(days: List<DayOfWeek>) = days.map { name(names, it) }.joinWith(" و")
        override fun daily() = "كل يوم"
        override fun everyNDays(n: Int) = when {
            n == 2 -> "كل يومين"
            n in 3..10 -> "كل $n أيام"
            else -> "كل $n يومًا"
        }
        override fun weekdays() = "من الاثنين إلى الجمعة"
        override fun weekends() = "السبت والأحد"
        override fun weekly(days: List<DayOfWeek>) = "كل أسبوع يوم " + list(days)
        override fun everyNWeeks(n: Int, days: List<DayOfWeek>) = when {
            n == 2 -> "كل أسبوعين"
            n in 3..10 -> "كل $n أسابيع"
            else -> "كل $n أسبوعًا"
        } + " يوم " + list(days)
        override fun monthlyDays(days: List<Int>) = "في اليوم " + days.joinWith(" و") + " من كل شهر"
        override fun monthlyLastDay() = "في آخر يوم من كل شهر"
        override fun monthlyNth(ordinal: Int, day: DayOfWeek) =
            "في ${if (ordinal == -1) "آخر" else ordinals[ordinal - 1]} ${name(bare, day)} من كل شهر"
        override fun yearly(month: Int, day: Int) = "كل عام في $day ${months[month - 1]}"
        override fun workdays() = "أيام العمل"
    }
}
