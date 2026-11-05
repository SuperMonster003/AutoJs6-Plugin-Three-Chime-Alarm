package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/**
 * Roadmap P2.2 (appendix A section 4.3): `repeatText` in the ten plugin languages. Every describable form (every day,
 * Monday to Friday, weekends, weekly days, every N weeks, monthly days, the last day, the Nth / last weekday, every N
 * days, yearly) in each language, the forms that fall back to the normalized RRULE, the language chosen for a locale
 * and ASCII punctuation in every text.
 * zh-CN: 路线图 P2.2 (附录 A 第 4.3 节): 插件 10 种语言的 `repeatText`. 每种语言的每种可描述形态 (每天, 周一至周五, 周末, 每周某几天, 每 N 周,
 * 每月某几日, 最后一天, 第 N 个 / 最后一个星期几, 每 N 天, 每年), 回退为规范 RRULE 的形态, 各 locale 选用的语言, 以及所有文案只用 ASCII 标点.
 */
class RepeatTextTest {

    /** A Wednesday. zh-CN: 星期三. */
    private val start = LocalDate.of(2026, 11, 4)

    private val rules = listOf(
        "FREQ=DAILY",
        "FREQ=DAILY;INTERVAL=3",
        "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
        "FREQ=WEEKLY;BYDAY=SA,SU",
        "FREQ=WEEKLY;BYDAY=MO,WE",
        "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO",
        "FREQ=MONTHLY;BYMONTHDAY=1,15",
        "FREQ=MONTHLY;BYMONTHDAY=-1",
        "FREQ=MONTHLY;BYDAY=2TU",
        "FREQ=MONTHLY;BYDAY=-1FR",
        "FREQ=YEARLY",
    )

    private val expected = mapOf(
        "zh-CN" to listOf(
            "每天", "每 3 天", "周一至周五", "周末", "每周一, 周三", "每 2 周的周一", "每月 1 日, 15 日", "每月最后一天",
            "每月第 2 个星期二", "每月最后一个星期五", "每年 11 月 4 日",
        ),
        "zh-TW" to listOf(
            "每天", "每 3 天", "週一至週五", "週末", "每週一, 週三", "每 2 週的週一", "每月 1 日, 15 日", "每月最後一天",
            "每月第 2 個星期二", "每月最後一個星期五", "每年 11 月 4 日",
        ),
        "zh-HK" to listOf(
            "每日", "每 3 日", "星期一至星期五", "週末", "逢星期一, 星期三", "每 2 個星期的星期一", "每月 1 日, 15 日", "每月最後一日",
            "每月第 2 個星期二", "每月最後一個星期五", "每年 11 月 4 日",
        ),
        "en-US" to listOf(
            "Every day", "Every 3 days", "Monday to Friday", "Weekends", "Every Monday and Wednesday", "Every 2 weeks on Monday",
            "Every month on the 1st and 15th", "Every month on the last day", "Every month on the second Tuesday",
            "Every month on the last Friday", "Every year on November 4",
        ),
        "ja-JP" to listOf(
            "毎日", "3 日ごと", "月曜日から金曜日", "週末", "毎週月曜日, 水曜日", "2 週間ごとの月曜日", "毎月 1 日, 15 日", "毎月末日",
            "毎月第 2 火曜日", "毎月最終金曜日", "毎年 11 月 4 日",
        ),
        "ko-KR" to listOf(
            "매일", "3일마다", "월요일부터 금요일까지", "주말", "매주 월요일, 수요일", "2주마다 월요일", "매월 1일, 15일", "매월 마지막 날",
            "매월 둘째 화요일", "매월 마지막 금요일", "매년 11월 4일",
        ),
        "es-ES" to listOf(
            "Todos los días", "Cada 3 días", "De lunes a viernes", "Los fines de semana", "Todos los lunes y miércoles",
            "Cada 2 semanas, los lunes", "Los días 1 y 15 de cada mes", "El último día de cada mes", "El segundo martes de cada mes",
            "El último viernes de cada mes", "Cada año el 4 de noviembre",
        ),
        "fr-FR" to listOf(
            "Tous les jours", "Tous les 3 jours", "Du lundi au vendredi", "Le week-end", "Tous les lundis et mercredis",
            "Toutes les 2 semaines, le lundi", "Le 1er et le 15 de chaque mois", "Le dernier jour de chaque mois",
            "Le deuxième mardi de chaque mois", "Le dernier vendredi de chaque mois", "Tous les ans le 4 novembre",
        ),
        "ru-RU" to listOf(
            "Каждый день", "Каждые 3 дня", "С понедельника по пятницу", "По выходным", "По понедельникам и средам",
            "Каждые 2 недели по понедельникам", "1 и 15 числа каждого месяца", "В последний день каждого месяца",
            "Во второй вторник каждого месяца", "В последнюю пятницу каждого месяца", "Каждый год 4 ноября",
        ),
        "ar-EG" to listOf(
            "كل يوم", "كل 3 أيام", "من الاثنين إلى الجمعة", "السبت والأحد", "كل أسبوع يوم الاثنين والأربعاء", "كل أسبوعين يوم الاثنين",
            "في اليوم 1 و15 من كل شهر", "في آخر يوم من كل شهر", "في ثاني ثلاثاء من كل شهر", "في آخر جمعة من كل شهر",
            "كل عام في 4 نوفمبر",
        ),
    )

    private fun locale(tag: String): Locale = Locale.forLanguageTag(tag)

    @Test
    fun `every form in the ten languages`() {
        assertEquals(RepeatText.LANGUAGES, expected.keys.map { RepeatText.languageOf(locale(it)) })
        for ((tag, texts) in expected) {
            assertEquals(tag, texts, rules.map { RepeatText.of(it, start, locale(tag)) })
        }
    }

    @Test
    fun `other inputs of the same forms`() {
        val zh = locale("zh-CN")
        assertEquals("每周一, 周五", RepeatText.of("rrule:freq=weekly;byday=fr,mo", start, zh))
        assertEquals("每天", RepeatText.of("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR,SA,SU", start, zh))
        assertEquals("每天", RepeatText.of("FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR,SA,SU", start, zh))
        assertEquals("周一至周五", RepeatText.of("FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", start, zh))
        assertEquals("每月第 2 个星期二", RepeatText.of("FREQ=MONTHLY;BYDAY=TU;BYSETPOS=2", start, zh))
        assertEquals("每月最后一个星期五", RepeatText.of("FREQ=MONTHLY;BYDAY=FR;BYSETPOS=-1", start, zh))
        assertEquals("每年 12 月 25 日", RepeatText.of("FREQ=YEARLY;BYMONTH=12;BYMONTHDAY=25", null, zh))
        // Parts the rule leaves out come from the start date. zh-CN: 规则省略的部分取自起始日期.
        assertEquals("每周三", RepeatText.of("FREQ=WEEKLY", start, zh))
        assertEquals("每 2 周的周三", RepeatText.of("FREQ=WEEKLY;INTERVAL=2", start, zh))
        assertEquals("每月 4 日", RepeatText.of("FREQ=MONTHLY", start, zh))
        assertEquals("每年 3 月 4 日", RepeatText.of("FREQ=YEARLY;BYMONTH=3", start, zh))
        assertEquals("每年 11 月 20 日", RepeatText.of("FREQ=YEARLY;BYMONTHDAY=20", start, zh))
        // Russian and Arabic counts. zh-CN: 俄语与阿拉伯语的数量形式.
        val ru = locale("ru-RU")
        assertEquals("Каждые 2 дня", RepeatText.of("FREQ=DAILY;INTERVAL=2", start, ru))
        assertEquals("Каждые 5 дней", RepeatText.of("FREQ=DAILY;INTERVAL=5", start, ru))
        assertEquals("Каждые 12 дней", RepeatText.of("FREQ=DAILY;INTERVAL=12", start, ru))
        assertEquals("Каждый 21 день", RepeatText.of("FREQ=DAILY;INTERVAL=21", start, ru))
        assertEquals("Каждые 3 недели по средам", RepeatText.of("FREQ=WEEKLY;INTERVAL=3", start, ru))
        assertEquals("В первое воскресенье каждого месяца", RepeatText.of("FREQ=MONTHLY;BYDAY=1SU", start, ru))
        val ar = locale("ar-EG")
        assertEquals("كل يومين", RepeatText.of("FREQ=DAILY;INTERVAL=2", start, ar))
        assertEquals("كل 11 يومًا", RepeatText.of("FREQ=DAILY;INTERVAL=11", start, ar))
        assertEquals("كل 3 أسابيع يوم الاثنين والأربعاء والجمعة", RepeatText.of("FREQ=WEEKLY;INTERVAL=3;BYDAY=MO,WE,FR", start, ar))
        val en = locale("en-US")
        assertEquals("Every month on the 2nd, 3rd, 11th, 21st and 22nd", RepeatText.of("FREQ=MONTHLY;BYMONTHDAY=2,3,11,21,22", start, en))
        assertEquals("Tous les ans le 1er mai", RepeatText.of("FREQ=YEARLY;BYMONTH=5;BYMONTHDAY=1", start, locale("fr-FR")))
    }

    @Test
    fun `forms without a phrase fall back to the normalized RRULE`() {
        for (rule in listOf(
            "FREQ=DAILY;COUNT=5",
            "FREQ=WEEKLY;UNTIL=20261231;BYDAY=MO",
            "FREQ=YEARLY;BYWEEKNO=1",
            "FREQ=YEARLY;BYYEARDAY=100",
            "FREQ=YEARLY;INTERVAL=2",
            "FREQ=YEARLY;BYMONTH=1,7;BYMONTHDAY=1",
            "FREQ=YEARLY;BYDAY=20MO",
            "FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU",
            "FREQ=MONTHLY;INTERVAL=2;BYMONTHDAY=1",
            "FREQ=MONTHLY;BYMONTH=6;BYMONTHDAY=1",
            "FREQ=MONTHLY;BYMONTHDAY=-2",
            "FREQ=MONTHLY;BYMONTHDAY=13;BYDAY=FR",
            "FREQ=MONTHLY;BYDAY=MO,TU;BYSETPOS=1",
            "FREQ=MONTHLY;BYDAY=MO;BYSETPOS=6",
            "FREQ=WEEKLY;BYMONTH=6;BYDAY=MO",
            "FREQ=WEEKLY;BYDAY=MO,TU;BYSETPOS=1",
            "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE;WKST=SU",
            "FREQ=DAILY;INTERVAL=2;BYDAY=MO",
            "FREQ=DAILY;BYMONTH=1",
        )) {
            for (tag in expected.keys) assertEquals("$rule in $tag", rule, RepeatText.of(rule, start, locale(tag)))
        }
        // Without a start date the forms that need it fall back too. zh-CN: 没有起始日期时, 需要它的形态也回退.
        assertEquals("FREQ=WEEKLY", RepeatText.of("FREQ=WEEKLY", null, locale("en-US")))
        assertEquals("FREQ=MONTHLY", RepeatText.of("FREQ=MONTHLY", null, locale("en-US")))
        assertEquals("FREQ=YEARLY", RepeatText.of("FREQ=YEARLY", null, locale("en-US")))
        // The fallback is the normalized form. zh-CN: 回退的是规范形式.
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=13;BYDAY=FR", RepeatText.of("RRULE:byday=fr;freq=monthly;bymonthday=13", start, locale("ja-JP")))
        // A weekly rule with another week start still has a phrase when the week start cannot matter.
        // zh-CN: 周起始日不影响结果时, 换了周起始日的每周规则仍有文案.
        assertEquals("Every 2 weeks on Monday", RepeatText.of("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO;WKST=SU", start, locale("en-US")))
    }

    @Test
    fun `the language of a locale`() {
        for ((tag, language) in listOf(
            "zh" to "zh-Hans", "zh-CN" to "zh-Hans", "zh-Hans-CN" to "zh-Hans", "zh-SG" to "zh-Hans",
            "zh-TW" to "zh-Hant-TW", "zh-Hant" to "zh-Hant-TW", "zh-Hant-TW" to "zh-Hant-TW",
            "zh-HK" to "zh-Hant-HK", "zh-Hant-HK" to "zh-Hant-HK", "zh-MO" to "zh-Hant-HK",
            "en" to "en", "en-GB" to "en", "ja" to "ja", "ko-KR" to "ko", "es-MX" to "es", "fr-CA" to "fr", "ru" to "ru", "ar-SA" to "ar",
            "de-DE" to "en", "pt-BR" to "en", "it" to "en",
        )) {
            assertEquals(tag, language, RepeatText.languageOf(locale(tag)))
        }
    }

    @Test
    fun `every text uses ASCII punctuation only`() {
        val samples = rules + listOf(
            "FREQ=DAILY;INTERVAL=2", "FREQ=DAILY;INTERVAL=11", "FREQ=DAILY;INTERVAL=21", "FREQ=WEEKLY;BYDAY=MO,WE,FR",
            "FREQ=WEEKLY;INTERVAL=3;BYDAY=TU,TH,SA", "FREQ=MONTHLY;BYMONTHDAY=1,2,3", "FREQ=MONTHLY;BYDAY=3SU", "FREQ=MONTHLY;BYDAY=5SA",
            "FREQ=YEARLY;BYMONTH=1;BYMONTHDAY=1",
        )
        for (tag in expected.keys) {
            for (rule in samples) {
                val text = RepeatText.of(rule, start, locale(tag))
                val bad = text.filter { it.code > 127 && Character.getType(it).toByte() in PUNCTUATION }
                assertTrue("'$text' ($rule, $tag) has non-ASCII punctuation '$bad'", bad.isEmpty())
                assertTrue("'$text' ($rule, $tag) should not end with punctuation", text.last().isLetterOrDigit() || Character.getType(text.last()).toByte() == Character.NON_SPACING_MARK)
            }
        }
    }

    companion object {
        private val PUNCTUATION = setOf(
            Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION, Character.END_PUNCTUATION,
            Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION,
        )
    }
}
