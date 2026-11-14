package io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool

import org.autojs.plugin.alarm.api.AlarmLimits

/**
 * The catalog [ChimeToolService] returns from `IPluginToolProvider.getCatalog` (AutoJs6 plugin tool contract v1, roadmap
 * P4.1, appendix A section 10): the short name `chime`, the nine 1.0.0 tools, which AutoJs6 lists as `chime_alarm_add`
 * and so on, since 1.1.0 the three timer tools `timer_start`, `timer_list` and `timer_control` (roadmap P7.6, T126 -
 * T128; the stopwatch is not offered, T17), and since 1.2.0 `holiday_check` (roadmap P8.7, T192). The parameters follow
 * `AlarmOptions` of appendix A section 4.2 within the host's schema subset (T90): no local audio file path (T91), the
 * `holidays` option since 1.2.0 (T193) and the `dismiss` option since 1.3.0 without `challenge: 'script'` (T290); script
 * bindings and `beforeRing` decisions are never offered (T17, T191). The document is static: the
 * host caches it per version code, and a missing exact alarm permission is reported by the call (`NOT_READY`), not by
 * dropping tools. The descriptions reach the context of AI models as written, so they stay English and ASCII and name
 * the error codes; there is nothing to localize (T89).
 * zh-CN: [ChimeToolService] 从 `IPluginToolProvider.getCatalog` 返回的目录 (AutoJs6 插件工具契约 v1, 路线图 P4.1, 附录 A 第 10 节): 短名
 * `chime` 下九个 1.0.0 工具, AutoJs6 列为 `chime_alarm_add` 等, 1.1.0 起另有三个计时器工具 `timer_start`, `timer_list`, `timer_control` (路线图
 * P7.6, T126 - T128; 不开放秒表, T17), 1.2.0 起另有 `holiday_check` (路线图 P8.7, T192). 参数在宿主的 schema 子集内沿用附录 A 第 4.2 节的
 * `AlarmOptions` (T90): 不含本地音频文件路径 (T91), 1.2.0 起含 `holidays` 选项 (T193), 1.3.0 起含不带 `challenge: 'script'` 的 `dismiss`
 * 选项 (T290); 从不开放脚本绑定与 `beforeRing` 决定 (T17, T191). 文档是静态的: 宿主按 versionCode 缓存, 缺少精确闹钟权限由调用报告 (`NOT_READY`), 不从目录里删工具. 说明原样进入 AI 模型的
 * 上下文, 因此保持英文与 ASCII 并写明错误码; 没有需要本地化的文案 (T89).
 */
internal object ChimeToolCatalog {

    const val SHORT_NAME = "chime"
    const val TOOL_ADD = "alarm_add"
    const val TOOL_LIST = "alarm_list"
    const val TOOL_UPDATE = "alarm_update"
    const val TOOL_REMOVE = "alarm_remove"
    const val TOOL_SET_ENABLED = "alarm_set_enabled"
    const val TOOL_SKIP_NEXT = "alarm_skip_next"
    const val TOOL_UPCOMING = "alarm_upcoming"
    const val TOOL_DISMISS = "alarm_dismiss"
    const val TOOL_SNOOZE = "alarm_snooze"
    const val TOOL_TIMER_START = "timer_start"
    const val TOOL_TIMER_LIST = "timer_list"
    const val TOOL_TIMER_CONTROL = "timer_control"
    const val TOOL_HOLIDAY_CHECK = "holiday_check"
    val TOOLS = listOf(
        TOOL_ADD, TOOL_LIST, TOOL_UPDATE, TOOL_REMOVE, TOOL_SET_ENABLED, TOOL_SKIP_NEXT, TOOL_UPCOMING, TOOL_DISMISS, TOOL_SNOOZE,
        TOOL_TIMER_START, TOOL_TIMER_LIST, TOOL_TIMER_CONTROL, TOOL_HOLIDAY_CHECK,
    )

    /** Every call is a quick local store operation. zh-CN: 每个调用都是很快的本地存储操作. */
    const val TIMEOUT_MS = 15_000L

    /** T92: `alarm_list` returns at most this many alarms unless `limit` says otherwise. zh-CN: T92: 默认返回的闹钟数. */
    const val DEFAULT_LIST_LIMIT = 100

    /** T92: `alarm_upcoming` without `count`. zh-CN: T92: 未给 `count` 时的条数. */
    const val DEFAULT_UPCOMING_COUNT = 5

    /** T93: the option names `alarm_update` may clear. zh-CN: T93: `alarm_update` 可以清除的选项名. */
    val CLEARABLE = listOf("repeat", "exclude", "holidays", "timeZone", "label", "tag", "sound", "speak", "snooze", "dismiss", "autoSilenceMinutes")

    /** T127: the actions of `timer_control`; `add` is `addTime` of the contract. zh-CN: T127: `timer_control` 的动作; `add` 即契约的 `addTime`. */
    val TIMER_ACTIONS = listOf("pause", "resume", "add", "reset", "cancel")

    private const val ID_DESCRIPTION = "Alarm id as returned by chime_alarm_add or chime_alarm_list (starts with a_)."

    private val ID = """"id": {"type": "string", "minLength": 1, "maxLength": 128, "description": "$ID_DESCRIPTION"}"""

    private val SOURCES = """"user", "script", "tool", "intent""""

    private val FILTER = """
          "source": {"oneOf": [
            {"type": "string", "enum": [$SOURCES]},
            {"type": "array", "items": {"type": "string", "enum": [$SOURCES]}, "minItems": 1, "maxItems": 4}
          ], "description": "Only alarms created by this source (or one of these sources): user (the 3-Chime Alarm app), script (AutoJs6 scripts), tool (these tools), intent (other apps and voice assistants through the system clock intents)."},
          "tag": {"type": "string", "maxLength": ${AlarmLimits.MAX_TAG_CHARS}, "description": "Only alarms with exactly this tag."},
          "enabled": {"type": "boolean", "description": "Only enabled (true) or disabled (false) alarms."},
          "label": {"type": "string", "maxLength": ${AlarmLimits.MAX_LABEL_CHARS}, "description": "Only alarms with exactly this label."}"""

    private val SOUND = """"sound": {"type": "object", "properties": {
            "source": {"type": "string", "minLength": 1, "maxLength": 2048, "description": "default (the system alarm sound), silent, or a ringtone content: URI. Local file paths are not accepted."},
            "volume": {"type": "number", "minimum": 0, "maximum": 1, "description": "Own volume 0 - 1; omitted, the alarm volume of the phone."},
            "rampSeconds": {"type": "integer", "minimum": 0, "maximum": ${AlarmLimits.MAX_RAMP_SECONDS}, "description": "Fade-in time in seconds, 0 (default) for none."}
          }, "additionalProperties": false, "description": "Ring sound; omitted members use the 3-Chime Alarm settings."}"""

    private val VIBRATE = """"vibrate": {"oneOf": [
            {"type": "boolean"},
            {"type": "array", "items": {"type": "integer", "minimum": 0, "maximum": 60000}, "minItems": 1, "maxItems": ${AlarmLimits.MAX_VIBRATE_PATTERN}, "description": "Pattern in milliseconds: wait, vibrate, wait, vibrate, ..."}
          ], "description": "Vibrate while ringing; omitted, the 3-Chime Alarm setting (on by default)."}"""

    private val SPEAK = """"speak": {"type": "object", "properties": {
            "text": {"type": "string", "minLength": 1, "maxLength": ${AlarmLimits.MAX_SPEAK_TEXT_CHARS}, "description": "Text read aloud when the alarm rings."},
            "engine": {"type": "string", "minLength": 1, "maxLength": 128, "description": "Text-to-speech engine package; omitted, the system default engine."},
            "language": {"type": "string", "minLength": 1, "maxLength": 35, "description": "BCP 47 language tag such as en or zh-CN."},
            "rate": {"type": "number", "exclusiveMinimum": 0, "maximum": 10},
            "pitch": {"type": "number", "exclusiveMinimum": 0, "maximum": 10},
            "mode": {"type": "string", "enum": ["before", "after", "interleave", "only"], "description": "before: read, then ring; after (default): ring, then read over the lowered sound; interleave: alternate; only: read without ringing."},
            "repeatInterval": {"type": "integer", "minimum": 0, "maximum": ${AlarmLimits.MAX_SPEAK_REPEAT_INTERVAL_SECONDS}, "description": "Seconds between readings, 0 to read once."}
          }, "required": ["text"], "additionalProperties": false, "description": "Read text aloud when the alarm rings."}"""

    private const val REGION_CODE = "Region code of the holiday data: ISO 3166-1 alpha-2 such as CN, HK, TW, JP or US, with a subdivision where the data has one, such as US-CA or DE-BY"

    private val HOLIDAYS = """"holidays": {"type": "object", "properties": {
            "mode": {"type": "string", "enum": ["skip", "only", "workdays"], "description": "skip: no ring on public holidays; only: ring only on public holidays; workdays: ring on every workday, a day that is neither a weekend day of the region nor a holiday, plus the makeup workdays the data lists (mainland China and Taiwan move workdays to weekends around some holidays). workdays needs time and either no repeat or a daily repeat with interval 1."},
            "region": {"type": "string", "minLength": 2, "maxLength": 128, "description": "$REGION_CODE. Omitted, the default region of the 3-Chime Alarm settings, also after it changes."}
          }, "required": ["mode"], "additionalProperties": false, "description": "Holiday filter (3-Chime Alarm 1.2.0) from the public holidays of a region. A year without holiday data rings by the repeat rule alone. chime_holiday_check tells how a day counts."}"""

    private val AUTO_SILENCE = """"autoSilenceMinutes": {"type": "integer", "minimum": ${AlarmLimits.MIN_AUTO_SILENCE_MINUTES}, "maximum": ${AlarmLimits.MAX_AUTO_SILENCE_MINUTES}, "description": "Stop ringing after this many minutes without an answer; omitted, the 3-Chime Alarm setting (10)."}"""

    private val ALARM_AUTO_SILENCE = """"autoSilenceMinutes": {"type": "integer", "minimum": ${AlarmLimits.MIN_AUTO_SILENCE_MINUTES}, "maximum": ${AlarmLimits.MAX_AUTO_SILENCE_MINUTES}, "description": "Stop ringing after this many minutes without an answer; omitted, the 3-Chime Alarm setting (10), at least ${AlarmLimits.DEFAULT_CHALLENGE_AUTO_SILENCE_MINUTES} with a dismiss challenge."}"""

    /** T290: `dismiss` of appendix A section 8 without `script` (T17). zh-CN: T290: 附录 A 第 8 节的 `dismiss`, 不含 `script` (T17). */
    private val DISMISS = """"dismiss": {"type": "object", "properties": {
            "challenge": {"type": "string", "enum": [${ChimeToolArguments.TOOL_CHALLENGES.joinToString(", ") { "\"$it\"" }}], "description": "What the user must do on the ringing screen to dismiss the alarm: none (default), math (solve arithmetic problems), shake (shake the phone), type (type a text). The alarm keeps ringing during the challenge; chime_alarm_dismiss still dismisses it at once."},
            "math": {"type": "object", "properties": {
              "difficulty": {"type": "integer", "minimum": ${AlarmLimits.MIN_MATH_DIFFICULTY}, "maximum": ${AlarmLimits.MAX_MATH_DIFFICULTY}, "description": "1 easy, 2 (default) medium, 3 hard."},
              "count": {"type": "integer", "minimum": 1, "maximum": ${AlarmLimits.MAX_MATH_COUNT}, "description": "Problems to solve, ${AlarmLimits.DEFAULT_MATH_COUNT} by default."}
            }, "additionalProperties": false, "description": "For challenge math."},
            "shake": {"type": "object", "properties": {
              "count": {"type": "integer", "minimum": 1, "maximum": ${AlarmLimits.MAX_SHAKE_COUNT}, "description": "Shakes, ${AlarmLimits.DEFAULT_SHAKE_COUNT} by default."}
            }, "additionalProperties": false, "description": "For challenge shake. A phone without an accelerometer asks math problems instead."},
            "type": {"type": "object", "properties": {
              "text": {"type": "string", "maxLength": ${AlarmLimits.MAX_TYPE_TEXT_CHARS}, "description": "Text to type; case and spaces around it do not matter. Omitted, a random sentence."}
            }, "additionalProperties": false, "description": "For challenge type."},
            "allowSnooze": {"type": "boolean", "description": "Whether the user may snooze during the challenge, true by default."}
          }, "additionalProperties": false, "description": "Dismiss challenge (3-Chime Alarm 1.3.0). Safety valves: with a challenge the ring stops by itself after autoSilenceMinutes (at least ${AlarmLimits.DEFAULT_CHALLENGE_AUTO_SILENCE_MINUTES} when omitted), and pressing a volume key 5 times dismisses it (emergency dismiss, on unless turned off in the settings). Alarms of AutoJs6 scripts may come back with challenge script (rings until a script dismisses it), which tools cannot set (INVALID_OPTIONS); changing other members keeps it."}"""

    private val OPTIONS = """
          "time": {"oneOf": [
            {"type": "string", "minLength": 4, "maxLength": 8, "description": "24-hour wall-clock time HH:mm or HH:mm:ss, such as 07:30."},
            {"type": "object", "properties": {"hour": {"type": "integer", "minimum": 0, "maximum": 23}, "minute": {"type": "integer", "minimum": 0, "maximum": 59}, "second": {"type": "integer", "minimum": 0, "maximum": 59}}, "required": ["hour", "minute"], "additionalProperties": false}
          ], "description": "Ring time on the wall clock (of timeZone, or of the phone). Exclusive with at."},
          "date": {"oneOf": [
            {"type": "string", "minLength": 10, "maxLength": 10, "description": "YYYY-MM-DD"},
            {"type": "integer", "minimum": 0, "description": "A Unix time in milliseconds; its day in timeZone (or the phone's zone) is used."}
          ], "description": "With time: the day of a one-shot alarm, or the first day (DTSTART) of a repeating one. Omitted: a one-shot alarm takes the next day its time comes, a repeating alarm starts today."},
          "at": {"oneOf": [
            {"type": "integer", "minimum": 0, "description": "Unix time in milliseconds."},
            {"type": "string", "minLength": 16, "maxLength": 40, "description": "ISO 8601 date and time such as 2026-11-07T07:30:00+08:00; without an offset it is read in timeZone (or the phone's zone)."}
          ], "description": "Exact moment of a one-shot alarm, instead of time. Cannot repeat."},
          "repeat": {"oneOf": [
            {"type": "string", "minLength": 1, "maxLength": 512, "description": "An RFC 5545 RRULE such as FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR or FREQ=MONTHLY;BYDAY=-1FR (an RRULE: prefix is allowed)."},
            {"type": "array", "items": {"type": "integer", "minimum": 0, "maximum": 6}, "minItems": 1, "maxItems": 7, "description": "Weekdays, 0 Sunday to 6 Saturday: [1, 2, 3, 4, 5] is Monday to Friday."},
            {"type": "object", "properties": {
              "freq": {"type": "string", "enum": ["daily", "weekly", "monthly", "yearly"]},
              "interval": {"type": "integer", "minimum": 1, "maximum": 1000, "description": "Every N periods, default 1."},
              "byDay": {"type": "array", "items": {"type": "string", "minLength": 2, "maxLength": 4}, "minItems": 1, "maxItems": 64, "description": "RRULE weekdays MO TU WE TH FR SA SU, with an optional ordinal for monthly or yearly rules: 2TU is the second Tuesday, -1FR the last Friday."},
              "byMonthDay": {"type": "array", "items": {"type": "integer", "minimum": -31, "maximum": 31}, "minItems": 1, "maxItems": 62, "description": "Days of the month, negative from the end (-1 is the last day); not 0."},
              "byMonth": {"type": "array", "items": {"type": "integer", "minimum": 1, "maximum": 12}, "minItems": 1, "maxItems": 12},
              "byYearDay": {"type": "array", "items": {"type": "integer", "minimum": -366, "maximum": 366}, "minItems": 1, "maxItems": 64},
              "byWeekNo": {"type": "array", "items": {"type": "integer", "minimum": -53, "maximum": 53}, "minItems": 1, "maxItems": 64},
              "bySetPos": {"type": "array", "items": {"type": "integer", "minimum": -366, "maximum": 366}, "minItems": 1, "maxItems": 64},
              "count": {"type": "integer", "minimum": 1, "maximum": 100000, "description": "Total number of rings; exclusive with until."},
              "until": {"oneOf": [{"type": "string", "minLength": 8, "maxLength": 16, "description": "YYYY-MM-DD, or an RRULE UNTIL such as 20271231T235959Z"}, {"type": "integer", "minimum": 0, "description": "Unix time in milliseconds"}], "description": "Last day or moment of the rule."},
              "weekStart": {"type": "integer", "enum": [0, 1], "description": "0 Sunday, 1 Monday (default)."}
            }, "required": ["freq"], "additionalProperties": false, "description": "A structured rule; the fields are the RRULE parts."}
          ], "description": "Repeat rule; omitted for a one-shot alarm. Rules finer than a day (HOURLY, BYHOUR, ...) fail with INVALID_RULE: an alarm rings once a day at its time."},
          "exclude": {"type": "array", "items": {"type": "string", "minLength": 10, "maxLength": 10}, "maxItems": ${AlarmLimits.MAX_EXCLUDE_DATES}, "description": "Days (YYYY-MM-DD) on which a repeating alarm does not ring."},
          $HOLIDAYS,
          "timeZone": {"type": "string", "minLength": 1, "maxLength": 64, "description": "IANA zone such as Asia/Shanghai for time and date; omitted, the alarm follows the phone's zone."},
          "label": {"type": "string", "maxLength": ${AlarmLimits.MAX_LABEL_CHARS}, "description": "Text shown on the ringing screen and in the alarm list."},
          "enabled": {"type": "boolean", "description": "Whether the alarm is armed; new alarms default to true."},
          "tag": {"type": "string", "maxLength": ${AlarmLimits.MAX_TAG_CHARS}, "description": "Free group tag for filtering with chime_alarm_list."},
          $SOUND,
          $VIBRATE,
          $SPEAK,
          "snooze": {"oneOf": [
            {"type": "boolean", "description": "false: the alarm cannot be snoozed; true: the 3-Chime Alarm settings."},
            {"type": "object", "properties": {"minutes": {"type": "integer", "minimum": ${AlarmLimits.MIN_SNOOZE_MINUTES}, "maximum": ${AlarmLimits.MAX_SNOOZE_MINUTES}}, "max": {"type": "integer", "minimum": 1, "maximum": ${AlarmLimits.MAX_SNOOZE_MAX}, "description": "Snoozes per ring."}}, "additionalProperties": false}
          ], "description": "Snooze; omitted members use the 3-Chime Alarm settings (10 minutes, 3 times by default)."},
          $DISMISS,
          $ALARM_AUTO_SILENCE"""

    private const val ALARM_RESULT = "Each alarm comes back with its id, its options as set (time as HH:mm or HH:mm:ss, repeat as a normalized RRULE with repeatText, a readable description), enabled, source and updatedBy (user / script / tool / intent), createdAt / updatedAt, nextTrigger (Unix milliseconds, absent when it will not ring again) with nextTriggerTime (ISO 8601 in the alarm's zone; an alarm set with at also has atTime), skipping (the next ring is skipped) and hasBindings (AutoJs6 scripts are bound to it)."

    private const val OPTION_ERRORS = "INVALID_ARGUMENTS (an invalid option or rule: the message starts with INVALID_OPTIONS or INVALID_RULE and names the problem), LIMIT_EXCEEDED (300 alarms, 200 enabled ones, or a text over its limit), NOT_READY (Android 12 - 12L only: 3-Chime Alarm may not set exact alarms; pass enabled false, or let the user allow it on the permissions page of 3-Chime Alarm)"

    private val TIMER_ID = """"id": {"type": "string", "minLength": 1, "maxLength": 128, "description": "Timer id as returned by chime_timer_start or chime_timer_list (starts with t_)."}"""

    private val TIMER_OPTIONS = """
          "duration": {"oneOf": [
            {"type": "integer", "minimum": ${AlarmLimits.MIN_TIMER_MILLIS}, "maximum": ${AlarmLimits.MAX_TIMER_MILLIS}, "description": "Milliseconds."},
            {"type": "string", "minLength": 2, "maxLength": 32, "description": "Hours, minutes and seconds such as 25m, 1h30m or 90s: h, m and s in this order, each at most once."}
          ], "description": "How long the timer runs, 1 s - 100 h."},
          "label": {"type": "string", "maxLength": ${AlarmLimits.MAX_LABEL_CHARS}, "description": "Text shown on the ringing screen and in the timer notification."},
          $SOUND,
          $VIBRATE,
          $SPEAK,
          $AUTO_SILENCE"""

    private const val TIMER_RESULT = "Each timer comes back with its id, label (when set), state (running, paused, or ringing: sounding or snoozed), durationMillis (the full duration), remainingMillis with remaining (the time left as M:SS or H:MM:SS; 0 while ringing), endsAt (Unix milliseconds, only while running) with endsAtTime (ISO 8601 in the phone's zone), and the ring options as set (sound, vibrate, speak, autoSilenceMinutes)."

    private const val TIMER_NOT_READY = "NOT_READY (Android 12 - 12L only: 3-Chime Alarm may not set exact alarms; let the user allow it on the permissions page of 3-Chime Alarm)"

    val JSON: String = """{
  "shortName": "$SHORT_NAME",
  "tools": [
    {
      "name": "$TOOL_ADD",
      "title": "Add an alarm (3-Chime Alarm)",
      "description": "Creates an alarm in 3-Chime Alarm on the phone. It rings with the full-screen alarm screen and its notification, also when the phone is locked or sleeping. Give time (a daily wall-clock time, with an optional date or repeat rule) or at (one exact moment). Without repeat the alarm rings once and turns itself off. The alarm is marked as created by a tool. Returns {alarm}. $ALARM_RESULT Errors: $OPTION_ERRORS.",
      "inputSchema": {
        "type": "object",
        "properties": {$OPTIONS
        },
        "additionalProperties": false
      },
      "risk": "write",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_LIST",
      "title": "List alarms (3-Chime Alarm)",
      "description": "Lists the alarms of 3-Chime Alarm on the phone, in the order the app shows them, optionally filtered. Returns {alarms, total, truncated}: total counts every match, truncated is true when limit cut the list. $ALARM_RESULT",
      "inputSchema": {
        "type": "object",
        "properties": {$FILTER,
          "limit": {"type": "integer", "minimum": 1, "maximum": ${AlarmLimits.MAX_ALARMS}, "default": $DEFAULT_LIST_LIMIT, "description": "Most alarms to return."}
        },
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 4
    },
    {
      "name": "$TOOL_UPDATE",
      "title": "Change an alarm (3-Chime Alarm)",
      "description": "Changes options of an alarm of 3-Chime Alarm; options left out keep their value. time replaces the time and drops at, at drops time and date (a repeating alarm then also needs repeat in clear). sound, speak, snooze and dismiss objects change only the members given; holidays replaces the whole filter. clear resets the named options to their defaults (no repeat, no holiday filter, the phone's zone, the 3-Chime Alarm settings, ...). The alarm is marked as last changed by a tool. Returns {alarm}. $ALARM_RESULT Errors: $OPTION_ERRORS; INVALID_ARGUMENTS with NOT_FOUND for an unknown id; INVALID_ARGUMENTS when nothing would change.",
      "inputSchema": {
        "type": "object",
        "properties": {
          $ID,$OPTIONS,
          "clear": {"type": "array", "items": {"type": "string", "enum": [${CLEARABLE.joinToString(", ") { "\"$it\"" }}]}, "minItems": 1, "maxItems": ${CLEARABLE.size}, "description": "Options to reset to their default; an option cannot be both given and cleared."}
        },
        "required": ["id"],
        "additionalProperties": false
      },
      "risk": "write",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_REMOVE",
      "title": "Delete an alarm (3-Chime Alarm)",
      "description": "Deletes an alarm of 3-Chime Alarm, whoever created it. Returns {id, removed}: removed is false when no alarm has that id (not an error).",
      "inputSchema": {
        "type": "object",
        "properties": {
          $ID
        },
        "required": ["id"],
        "additionalProperties": false
      },
      "risk": "destructive",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_SET_ENABLED",
      "title": "Turn an alarm on or off (3-Chime Alarm)",
      "description": "Turns an alarm of 3-Chime Alarm on or off; an alarm already in that state comes back unchanged. Returns {alarm}. $ALARM_RESULT Errors: INVALID_ARGUMENTS with NOT_FOUND for an unknown id, LIMIT_EXCEEDED (200 enabled alarms), NOT_READY (Android 12 - 12L only: 3-Chime Alarm may not set exact alarms).",
      "inputSchema": {
        "type": "object",
        "properties": {
          $ID,
          "enabled": {"type": "boolean", "description": "true to turn on, false to turn off."}
        },
        "required": ["id", "enabled"],
        "additionalProperties": false
      },
      "risk": "write",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_SKIP_NEXT",
      "title": "Skip the next ring of an alarm (3-Chime Alarm)",
      "description": "Skips only the next ring of a repeating alarm of 3-Chime Alarm, which then rings again as usual; for a one-shot alarm it turns the alarm off. With cancel true it undoes the skip. An alarm that is off, already skipping or will not ring again comes back unchanged. Returns {alarm} (skipping tells whether the next ring is skipped). $ALARM_RESULT Errors: INVALID_ARGUMENTS with NOT_FOUND for an unknown id.",
      "inputSchema": {
        "type": "object",
        "properties": {
          $ID,
          "cancel": {"type": "boolean", "default": false, "description": "Undo a skip instead."}
        },
        "required": ["id"],
        "additionalProperties": false
      },
      "risk": "write",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_UPCOMING",
      "title": "Upcoming alarm rings (3-Chime Alarm)",
      "description": "The next rings of the enabled alarms of 3-Chime Alarm, earliest first, with skipped rings and excluded days left out. Returns {occurrences}: each has alarmId, label (when set), at (Unix milliseconds) and time (ISO 8601 in the alarm's zone).",
      "inputSchema": {
        "type": "object",
        "properties": {
          "count": {"type": "integer", "minimum": 1, "maximum": ${AlarmLimits.MAX_UPCOMING_COUNT}, "default": $DEFAULT_UPCOMING_COUNT, "description": "Number of rings."},$FILTER
        },
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 4
    },
    {
      "name": "$TOOL_DISMISS",
      "title": "Stop a ringing alarm (3-Chime Alarm)",
      "description": "Dismisses an alarm of 3-Chime Alarm that is ringing or snoozed, ending this ring; a repeating alarm rings again next time. Without id it dismisses every ringing or snoozed alarm. Returns {handled}: false when nothing was ringing or snoozed (not an error).",
      "inputSchema": {
        "type": "object",
        "properties": {
          $ID
        },
        "additionalProperties": false
      },
      "risk": "write",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_SNOOZE",
      "title": "Snooze a ringing alarm (3-Chime Alarm)",
      "description": "Snoozes an alarm of 3-Chime Alarm that is ringing now: it stops and rings again after the snooze time. Without id it snoozes every alarm that is sounding or waiting to sound. Returns {handled}: false when no such alarm could be snoozed (none ringing, already snoozed, snoozes used up or not allowed, or the snooze cannot be scheduled; not an error).",
      "inputSchema": {
        "type": "object",
        "properties": {
          $ID,
          "minutes": {"type": "integer", "minimum": ${AlarmLimits.MIN_SNOOZE_MINUTES}, "maximum": ${AlarmLimits.MAX_SNOOZE_MINUTES}, "description": "Snooze time for this snooze only; omitted, the alarm's own."}
        },
        "additionalProperties": false
      },
      "risk": "write",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_TIMER_START",
      "title": "Start a timer (3-Chime Alarm)",
      "description": "Starts a countdown timer in 3-Chime Alarm on the phone. At zero it rings with the same full-screen screen and notification as an alarm, also when the phone is locked or sleeping; it can be snoozed, and it is deleted once it is dismissed or silenced. While it runs, a notification shows the time left with pause, +1 minute and cancel buttons. A timer survives a restart of the phone, and the time the phone was off counts. At most ${AlarmLimits.MAX_TIMERS} timers at once. The timer is marked as started by a tool. Returns {timer}. $TIMER_RESULT Errors: INVALID_ARGUMENTS (a duration outside 1 s - 100 h or an invalid option: the message starts with INVALID_OPTIONS and names the problem), LIMIT_EXCEEDED (${AlarmLimits.MAX_TIMERS} timers, or a text over its limit), $TIMER_NOT_READY.",
      "inputSchema": {
        "type": "object",
        "properties": {$TIMER_OPTIONS
        },
        "required": ["duration"],
        "additionalProperties": false
      },
      "risk": "write",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_TIMER_LIST",
      "title": "List timers (3-Chime Alarm)",
      "description": "Lists the timers of 3-Chime Alarm on the phone in the order they were started (at most ${AlarmLimits.MAX_TIMERS}). Returns {timers}. $TIMER_RESULT",
      "inputSchema": {
        "type": "object",
        "properties": {},
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 4
    },
    {
      "name": "$TOOL_TIMER_CONTROL",
      "title": "Pause, change or cancel a timer (3-Chime Alarm)",
      "description": "Controls a timer of 3-Chime Alarm. pause stops the countdown, resume continues it, add changes the time left by duration (negative to take time off; the time left must stay within 1 s - 100 h), reset puts the timer back to its full duration, paused, and cancel deletes it (a ringing or snoozed timer is stopped first). pause, resume, add and reset return {timer} and fail on a ringing or snoozed timer, which cancel, chime_alarm_dismiss or chime_alarm_snooze handle. cancel returns {id, removed}: removed is false when no timer has that id (not an error). $TIMER_RESULT Errors: INVALID_ARGUMENTS (an invalid duration, duration with another action than add, or a ringing timer: the message starts with INVALID_OPTIONS), INVALID_ARGUMENTS with NOT_FOUND for an unknown id, $TIMER_NOT_READY.",
      "inputSchema": {
        "type": "object",
        "properties": {
          $TIMER_ID,
          "action": {"type": "string", "enum": [${TIMER_ACTIONS.joinToString(", ") { "\"$it\"" }}], "description": "What to do with the timer."},
          "duration": {"oneOf": [
            {"type": "integer", "minimum": -${AlarmLimits.MAX_TIMER_MILLIS}, "maximum": ${AlarmLimits.MAX_TIMER_MILLIS}, "description": "Milliseconds, negative to take time off."},
            {"type": "string", "minLength": 2, "maxLength": 33, "description": "Hours, minutes and seconds such as 5m, 1h30m or -1m, with a leading minus to take time off."}
          ], "description": "Only with action add: the time to add to the time left (negative to take it off)."}
        },
        "required": ["id", "action"],
        "additionalProperties": false
      },
      "risk": "write",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 1
    },
    {
      "name": "$TOOL_HOLIDAY_CHECK",
      "title": "Check a day for holidays (3-Chime Alarm)",
      "description": "Tells whether a day is a public holiday or a workday in a region, from the holiday data of 3-Chime Alarm (the public holidays of python-holidays, with the makeup workdays of mainland China and Taiwan). A workday is a day that is neither a weekend day of the region nor a holiday, or a makeup workday. The same data drives the holidays option of chime_alarm_add. Returns {date, region, isHoliday, isWorkday, name, estimated}: region is the code the answer is for, name the holiday's name in the phone's language (absent on other days), estimated true for a year the data computes from the usual rules because the official schedule is not out yet (makeup workdays may be missing). Errors: INVALID_ARGUMENTS (an invalid date or an unknown region: the message starts with INVALID_OPTIONS; a year outside the data: the message starts with HOLIDAY_DATA_MISSING and gives the years covered), FAILED (damaged holiday data).",
      "inputSchema": {
        "type": "object",
        "properties": {
          "date": {"oneOf": [
            {"type": "string", "minLength": 10, "maxLength": 10, "description": "YYYY-MM-DD"},
            {"type": "integer", "minimum": 0, "description": "A Unix time in milliseconds; its day in the phone's zone is used."}
          ], "description": "The day to check; omitted, today in the phone's zone."},
          "region": {"type": "string", "minLength": 2, "maxLength": 128, "description": "$REGION_CODE. Omitted, the default region of the 3-Chime Alarm settings."}
        },
        "additionalProperties": false
      },
      "risk": "readonly",
      "timeoutMs": $TIMEOUT_MS,
      "maxConcurrency": 4
    }
  ]
}"""
}
