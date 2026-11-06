package io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool

import org.autojs.plugin.alarm.api.AlarmLimits

/**
 * The catalog [ChimeToolService] returns from `IPluginToolProvider.getCatalog` (AutoJs6 plugin tool contract v1, roadmap
 * P4.1, appendix A section 10): the short name `chime` and the nine 1.0.0 tools, which AutoJs6 lists as `chime_alarm_add`
 * and so on. The parameters follow `AlarmOptions` of appendix A section 4.2 within the host's schema subset (T90): no local
 * audio file path (T91) and none of the options of later features (`holidays` joins with 1.2.0, `dismiss` with 1.3.0
 * without `challenge: 'script'`); script bindings and `beforeRing` are never offered (T17). The document is static: the
 * host caches it per version code, and a missing exact alarm permission is reported by the call (`NOT_READY`), not by
 * dropping tools. The descriptions reach the context of AI models as written, so they stay English and ASCII and name
 * the error codes; there is nothing to localize (T89).
 * zh-CN: [ChimeToolService] 从 `IPluginToolProvider.getCatalog` 返回的目录 (AutoJs6 插件工具契约 v1, 路线图 P4.1, 附录 A 第 10 节): 短名
 * `chime` 下九个 1.0.0 工具, AutoJs6 列为 `chime_alarm_add` 等. 参数在宿主的 schema 子集内沿用附录 A 第 4.2 节的 `AlarmOptions` (T90): 不含
 * 本地音频文件路径 (T91), 也不含之后功能的选项 (`holidays` 随 1.2.0 加入, `dismiss` 随 1.3.0 加入且不含 `challenge: 'script'`); 从不开放脚本绑定与
 * `beforeRing` (T17). 文档是静态的: 宿主按 versionCode 缓存, 缺少精确闹钟权限由调用报告 (`NOT_READY`), 不从目录里删工具. 说明原样进入 AI 模型的
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
    val TOOLS = listOf(TOOL_ADD, TOOL_LIST, TOOL_UPDATE, TOOL_REMOVE, TOOL_SET_ENABLED, TOOL_SKIP_NEXT, TOOL_UPCOMING, TOOL_DISMISS, TOOL_SNOOZE)

    /** Every call is a quick local store operation. zh-CN: 每个调用都是很快的本地存储操作. */
    const val TIMEOUT_MS = 15_000L

    /** T92: `alarm_list` returns at most this many alarms unless `limit` says otherwise. zh-CN: T92: 默认返回的闹钟数. */
    const val DEFAULT_LIST_LIMIT = 100

    /** T92: `alarm_upcoming` without `count`. zh-CN: T92: 未给 `count` 时的条数. */
    const val DEFAULT_UPCOMING_COUNT = 5

    /** T93: the option names `alarm_update` may clear. zh-CN: T93: `alarm_update` 可以清除的选项名. */
    val CLEARABLE = listOf("repeat", "exclude", "timeZone", "label", "tag", "sound", "speak", "snooze", "autoSilenceMinutes")

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
          "timeZone": {"type": "string", "minLength": 1, "maxLength": 64, "description": "IANA zone such as Asia/Shanghai for time and date; omitted, the alarm follows the phone's zone."},
          "label": {"type": "string", "maxLength": ${AlarmLimits.MAX_LABEL_CHARS}, "description": "Text shown on the ringing screen and in the alarm list."},
          "enabled": {"type": "boolean", "description": "Whether the alarm is armed; new alarms default to true."},
          "tag": {"type": "string", "maxLength": ${AlarmLimits.MAX_TAG_CHARS}, "description": "Free group tag for filtering with chime_alarm_list."},
          "sound": {"type": "object", "properties": {
            "source": {"type": "string", "minLength": 1, "maxLength": 2048, "description": "default (the system alarm sound), silent, or a ringtone content: URI. Local file paths are not accepted."},
            "volume": {"type": "number", "minimum": 0, "maximum": 1, "description": "Own volume 0 - 1; omitted, the alarm volume of the phone."},
            "rampSeconds": {"type": "integer", "minimum": 0, "maximum": ${AlarmLimits.MAX_RAMP_SECONDS}, "description": "Fade-in time in seconds, 0 (default) for none."}
          }, "additionalProperties": false, "description": "Ring sound; omitted members use the 3-Chime Alarm settings."},
          "vibrate": {"oneOf": [
            {"type": "boolean"},
            {"type": "array", "items": {"type": "integer", "minimum": 0, "maximum": 60000}, "minItems": 1, "maxItems": ${AlarmLimits.MAX_VIBRATE_PATTERN}, "description": "Pattern in milliseconds: wait, vibrate, wait, vibrate, ..."}
          ], "description": "Vibrate while ringing; omitted, the 3-Chime Alarm setting (on by default)."},
          "speak": {"type": "object", "properties": {
            "text": {"type": "string", "minLength": 1, "maxLength": ${AlarmLimits.MAX_SPEAK_TEXT_CHARS}, "description": "Text read aloud when the alarm rings."},
            "engine": {"type": "string", "minLength": 1, "maxLength": 128, "description": "Text-to-speech engine package; omitted, the system default engine."},
            "language": {"type": "string", "minLength": 1, "maxLength": 35, "description": "BCP 47 language tag such as en or zh-CN."},
            "rate": {"type": "number", "exclusiveMinimum": 0, "maximum": 10},
            "pitch": {"type": "number", "exclusiveMinimum": 0, "maximum": 10},
            "mode": {"type": "string", "enum": ["before", "after", "interleave", "only"], "description": "before: read, then ring; after (default): ring, then read over the lowered sound; interleave: alternate; only: read without ringing."},
            "repeatInterval": {"type": "integer", "minimum": 0, "maximum": ${AlarmLimits.MAX_SPEAK_REPEAT_INTERVAL_SECONDS}, "description": "Seconds between readings, 0 to read once."}
          }, "required": ["text"], "additionalProperties": false, "description": "Read text aloud when the alarm rings."},
          "snooze": {"oneOf": [
            {"type": "boolean", "description": "false: the alarm cannot be snoozed; true: the 3-Chime Alarm settings."},
            {"type": "object", "properties": {"minutes": {"type": "integer", "minimum": ${AlarmLimits.MIN_SNOOZE_MINUTES}, "maximum": ${AlarmLimits.MAX_SNOOZE_MINUTES}}, "max": {"type": "integer", "minimum": 1, "maximum": ${AlarmLimits.MAX_SNOOZE_MAX}, "description": "Snoozes per ring."}}, "additionalProperties": false}
          ], "description": "Snooze; omitted members use the 3-Chime Alarm settings (10 minutes, 3 times by default)."},
          "autoSilenceMinutes": {"type": "integer", "minimum": ${AlarmLimits.MIN_AUTO_SILENCE_MINUTES}, "maximum": ${AlarmLimits.MAX_AUTO_SILENCE_MINUTES}, "description": "Stop ringing after this many minutes without an answer; omitted, the 3-Chime Alarm setting (10)."}"""

    private const val ALARM_RESULT = "Each alarm comes back with its id, its options as set (time as HH:mm or HH:mm:ss, repeat as a normalized RRULE with repeatText, a readable description), enabled, source and updatedBy (user / script / tool / intent), createdAt / updatedAt, nextTrigger (Unix milliseconds, absent when it will not ring again) with nextTriggerTime (ISO 8601 in the alarm's zone; an alarm set with at also has atTime), skipping (the next ring is skipped) and hasBindings (AutoJs6 scripts are bound to it)."

    private const val OPTION_ERRORS = "INVALID_ARGUMENTS (an invalid option or rule: the message starts with INVALID_OPTIONS or INVALID_RULE and names the problem), LIMIT_EXCEEDED (300 alarms, 200 enabled ones, or a text over its limit), NOT_READY (Android 12 - 12L only: 3-Chime Alarm may not set exact alarms; pass enabled false, or let the user allow it on the permissions page of 3-Chime Alarm)"

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
      "description": "Changes options of an alarm of 3-Chime Alarm; options left out keep their value. time replaces the time and drops at, at drops time and date (a repeating alarm then also needs repeat in clear). sound, speak and snooze objects change only the members given. clear resets the named options to their defaults (no repeat, the phone's zone, the 3-Chime Alarm settings, ...). The alarm is marked as last changed by a tool. Returns {alarm}. $ALARM_RESULT Errors: $OPTION_ERRORS; INVALID_ARGUMENTS with NOT_FOUND for an unknown id; INVALID_ARGUMENTS when nothing would change.",
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
    }
  ]
}"""
}
