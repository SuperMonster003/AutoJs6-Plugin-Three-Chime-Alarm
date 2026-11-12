package io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.AlarmApi
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api.TimerApi
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer.TimerDurations
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmResultKeys
import org.autojs.plugin.alarm.api.AlarmSnapshot
import org.autojs.plugin.alarm.api.OccurrenceSnapshot
import org.autojs.plugin.alarm.api.TimerSnapshot
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The nine 1.0.0 tools of roadmap P4.1 on an [AlarmApi] that records the source `tool` and dismissals by `tool` (T18,
 * T78): [call] takes the tool name without the short name and the validated arguments, and answers the result object.
 * Alarm errors become tool errors ([error], T94); the message keeps the alarm code in front. Results describe an alarm
 * in the `AlarmOptions` shape (T95): `time` as `HH:mm` (`HH:mm:ss` with seconds), `vibrate` as a boolean or the pattern,
 * `snooze` as false or its members, members never set left out, and the moments `nextTrigger` / `at` also as ISO 8601 text
 * in the alarm's zone. Since 1.1.0 the timer tools run on a [TimerApi] that records the source `tool` and cancels
 * ringing timers `by` `tool` (roadmap P7.6, T126 - T129). Pure Kotlin: the JVM tests run every tool on the real
 * repository and timer engine.
 * zh-CN: 路线图 P4.1 的九个 1.0.0 工具, 基于记来源 `tool` 与关闭者 `tool` 的 [AlarmApi] (T18, T78): [call] 接收去掉短名的工具名与已校验的参数,
 * 返回结果对象. 闹钟错误转为工具错误 ([error], T94), 消息前保留闹钟错误码. 结果以 `AlarmOptions` 的形式描述闹钟 (T95): `time` 为 `HH:mm`
 * (有秒时为 `HH:mm:ss`), `vibrate` 为布尔或节奏, `snooze` 为 false 或其成员, 未设置的成员略去, 时刻 `nextTrigger` / `at` 另给出闹钟时区的
 * ISO 8601 文本. 1.1.0 起计时器工具基于记来源 `tool`, 以 `tool` 关闭响铃中计时器的 [TimerApi] (路线图 P7.6, T126 - T129). 纯 Kotlin: JVM 测试在
 * 真实仓库与计时器引擎上运行每个工具.
 */
internal class ChimeTools(
    private val api: AlarmApi,
    private val timers: TimerApi,
    private val deviceZone: () -> ZoneId = ZoneId::systemDefault,
) {

    private val arguments = ChimeToolArguments(deviceZone)

    fun call(tool: String, args: Map<String, Any?>): Map<String, Any?> = try {
        when (tool) {
            ChimeToolCatalog.TOOL_ADD -> alarmResult(api.addAlarm(arguments.add(args)))
            ChimeToolCatalog.TOOL_LIST -> list(args)
            ChimeToolCatalog.TOOL_UPDATE -> update(args)
            ChimeToolCatalog.TOOL_REMOVE -> remove(args)
            ChimeToolCatalog.TOOL_SET_ENABLED -> {
                only(args, "id", "enabled")
                val enabled = args["enabled"] as? Boolean ?: throw AlarmException.invalidOptions("enabled must be true or false")
                alarmResult(api.setAlarmEnabled(mapOf(AlarmOptionKeys.ID to arguments.id(args), AlarmOptionKeys.ENABLED to enabled)))
            }
            ChimeToolCatalog.TOOL_SKIP_NEXT -> {
                only(args, "id", "cancel")
                val request = mapOf(AlarmOptionKeys.ID to arguments.id(args))
                alarmResult(if (arguments.boolean(args, "cancel", false)) api.cancelSkip(request) else api.skipNext(request))
            }
            ChimeToolCatalog.TOOL_UPCOMING -> upcoming(args)
            ChimeToolCatalog.TOOL_DISMISS -> {
                only(args, "id")
                val answer = api.dismiss(listOfNotNull(arguments.optionalId(args)?.let { AlarmOptionKeys.ID to it }).toMap())
                mapOf("handled" to (answer[AlarmResultKeys.HANDLED] == true))
            }
            ChimeToolCatalog.TOOL_SNOOZE -> {
                only(args, "id", "minutes")
                val request = LinkedHashMap<String, Any?>()
                arguments.optionalId(args)?.let { request[AlarmOptionKeys.ID] = it }
                arguments.int(args, "minutes", AlarmLimits.MIN_SNOOZE_MINUTES, AlarmLimits.MAX_SNOOZE_MINUTES, null)?.let { request[AlarmOptionKeys.MINUTES] = it }
                mapOf("handled" to (api.snooze(request)[AlarmResultKeys.HANDLED] == true))
            }
            ChimeToolCatalog.TOOL_TIMER_START -> timerResult(timers.startTimer(arguments.timer(args)))
            ChimeToolCatalog.TOOL_TIMER_LIST -> {
                only(args)
                mapOf("timers" to maps(timers.listTimers(emptyMap())[AlarmResultKeys.TIMERS]).map { timer(TimerSnapshot.fromValues(it)) })
            }
            ChimeToolCatalog.TOOL_TIMER_CONTROL -> control(args)
            else -> throw PluginToolException(PluginToolErrorCodes.UNKNOWN_TOOL, "3-Chime Alarm has no tool $tool")
        }
    } catch (e: AlarmException) {
        throw error(e, timer = tool in TIMER_TOOLS)
    }

    /** T127: `timer_control`; `add` takes `duration` (which no other action takes) and becomes `addTime`. zh-CN: T127: `add` 带 `duration`, 转为 `addTime`. */
    private fun control(args: Map<String, Any?>): Map<String, Any?> {
        only(args, "id", "action", "duration")
        val id = arguments.id(args)
        val action = args["action"]
        val wire = when (action) {
            "pause" -> AlarmContract.TIMER_ACTION_PAUSE
            "resume" -> AlarmContract.TIMER_ACTION_RESUME
            "add" -> AlarmContract.TIMER_ACTION_ADD_TIME
            "reset" -> AlarmContract.TIMER_ACTION_RESET
            "cancel" -> AlarmContract.TIMER_ACTION_CANCEL
            else -> throw AlarmException.invalidOptions("action must be one of ${ChimeToolCatalog.TIMER_ACTIONS.joinToString(", ")}, got ${ChimeToolArguments.describe(action)}")
        }
        val millis = args["duration"]?.let { arguments.duration("duration", it, signed = true) }
        if (action == "add" && millis == null) throw AlarmException.invalidOptions("action add needs duration")
        if (action != "add" && millis != null) throw AlarmException.invalidOptions("duration only goes with action add")
        val request = linkedMapOf<String, Any?>(AlarmOptionKeys.ID to id, AlarmOptionKeys.ACTION to wire)
        millis?.let { request[AlarmOptionKeys.MILLIS] = it }
        val answer = timers.controlTimer(request)
        if (wire == AlarmContract.TIMER_ACTION_CANCEL) return linkedMapOf("id" to id, "removed" to (answer[AlarmResultKeys.REMOVED] == true))
        return timerResult(answer)
    }

    private fun timerResult(answer: Map<String, Any?>): Map<String, Any?> =
        mapOf("timer" to timer(TimerSnapshot.fromValues(map(answer[AlarmResultKeys.TIMER] ?: throw AlarmException(AlarmErrorCodes.INTERNAL, "no timer in the answer")))))

    /**
     * T129: one timer for the tools: the snapshot with `remaining` as clock text, `endsAt` also as ISO 8601 text in the
     * phone's zone, and the ring options in the shape `timer_start` takes.
     * zh-CN: T129: 工具里的一个计时器: 快照, 另给出钟面文本 `remaining`, 手机时区的 ISO 8601 文本 `endsAtTime`, 响铃选项与 `timer_start` 的写法相同.
     */
    fun timer(snapshot: TimerSnapshot): Map<String, Any?> {
        val spec = snapshot.spec
        return LinkedHashMap<String, Any?>().apply {
            put("id", snapshot.id)
            spec.label?.let { put("label", it) }
            put("state", snapshot.state)
            put("durationMillis", snapshot.durationMillis)
            put("remainingMillis", snapshot.remainingMillis)
            put("remaining", TimerDurations.clockText(snapshot.remainingMillis))
            snapshot.endsAt?.let {
                put("endsAt", it)
                put("endsAtTime", isoTime(it, deviceZone()))
            }
            spec.sound?.toValues()?.filterValues { it != null }?.takeIf { it.isNotEmpty() }?.let { put("sound", it) }
            when {
                spec.vibratePattern != null -> put("vibrate", spec.vibratePattern)
                spec.vibrate != null -> put("vibrate", spec.vibrate)
            }
            spec.speak?.toValues()?.filterValues { it != null }?.let { put("speak", it) }
            spec.autoSilenceMinutes?.let { put("autoSilenceMinutes", it) }
        }
    }

    private fun list(args: Map<String, Any?>): Map<String, Any?> {
        only(args, "source", "tag", "enabled", "label", "limit")
        val limit = arguments.int(args, "limit", 1, AlarmLimits.MAX_ALARMS, ChimeToolCatalog.DEFAULT_LIST_LIMIT)!!
        val request = arguments.filter(args)?.let { mapOf(AlarmOptionKeys.FILTER to it) } ?: emptyMap()
        val all = maps(api.listAlarms(request)[AlarmResultKeys.ALARMS])
        return linkedMapOf(
            "alarms" to all.take(limit).map { alarm(AlarmSnapshot.fromValues(it)) },
            "total" to all.size,
            "truncated" to (all.size > limit),
        )
    }

    private fun update(args: Map<String, Any?>): Map<String, Any?> {
        val id = arguments.id(args)
        val patch = arguments.patch(args - "id") {
            api.getAlarm(mapOf(AlarmOptionKeys.ID to id))[AlarmResultKeys.ALARM]?.let { map(it) }
                ?: throw AlarmException.notFound("no alarm with id '$id'")
        }
        return alarmResult(api.updateAlarm(mapOf(AlarmOptionKeys.ID to id, AlarmOptionKeys.PATCH to patch)))
    }

    private fun remove(args: Map<String, Any?>): Map<String, Any?> {
        only(args, "id")
        val id = arguments.id(args)
        return linkedMapOf("id" to id, "removed" to (api.removeAlarm(mapOf(AlarmOptionKeys.ID to id))[AlarmResultKeys.REMOVED] == true))
    }

    private fun upcoming(args: Map<String, Any?>): Map<String, Any?> {
        only(args, "count", "source", "tag", "enabled", "label")
        val request = LinkedHashMap<String, Any?>()
        request[AlarmOptionKeys.COUNT] = arguments.int(args, "count", 1, AlarmLimits.MAX_UPCOMING_COUNT, ChimeToolCatalog.DEFAULT_UPCOMING_COUNT)
        arguments.filter(args)?.let { request[AlarmOptionKeys.FILTER] = it }
        val occurrences = maps(api.upcoming(request)[AlarmResultKeys.OCCURRENCES]).map(OccurrenceSnapshot::fromValues)
        val zones = HashMap<String, ZoneId>()
        return mapOf(
            "occurrences" to occurrences.map { occurrence ->
                val zone = zones.getOrPut(occurrence.alarmId) { zoneOf(api.getAlarm(mapOf(AlarmOptionKeys.ID to occurrence.alarmId))) }
                LinkedHashMap<String, Any?>().apply {
                    put("alarmId", occurrence.alarmId)
                    occurrence.label?.let { put("label", it) }
                    put("at", occurrence.at)
                    put("time", isoTime(occurrence.at, zone))
                }
            },
        )
    }

    private fun alarmResult(answer: Map<String, Any?>): Map<String, Any?> =
        mapOf("alarm" to alarm(AlarmSnapshot.fromValues(map(answer[AlarmResultKeys.ALARM] ?: throw AlarmException(AlarmErrorCodes.INTERNAL, "no alarm in the answer")))))

    private fun zoneOf(answer: Map<String, Any?>): ZoneId =
        (answer[AlarmResultKeys.ALARM] as? Map<*, *>)?.get(AlarmOptionKeys.TIME_ZONE)?.let { runCatching { ZoneId.of(it.toString()) }.getOrNull() } ?: deviceZone()

    /** T95: one alarm in the `AlarmOptions` shape. zh-CN: T95: 以 `AlarmOptions` 形式表示的一个闹钟. */
    fun alarm(snapshot: AlarmSnapshot): Map<String, Any?> {
        val spec = snapshot.spec
        val zone = spec.timeZone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: deviceZone()
        return LinkedHashMap<String, Any?>().apply {
            put("id", snapshot.id)
            spec.label?.let { put("label", it) }
            spec.time?.let { put("time", if (it.second == 0) "%02d:%02d".format(it.hour, it.minute) else "%02d:%02d:%02d".format(it.hour, it.minute, it.second)) }
            spec.date?.let { put("date", it) }
            spec.at?.let {
                put("at", it)
                put("atTime", isoTime(it, zone))
            }
            spec.repeat?.let { put("repeat", it) }
            snapshot.repeatText?.let { put("repeatText", it) }
            spec.exclude?.takeIf { it.isNotEmpty() }?.let { put("exclude", it) }
            spec.timeZone?.let { put("timeZone", it) }
            put("enabled", spec.enabled != false)
            spec.tag?.let { put("tag", it) }
            spec.sound?.toValues()?.filterValues { it != null }?.takeIf { it.isNotEmpty() }?.let { put("sound", it) }
            when {
                spec.vibratePattern != null -> put("vibrate", spec.vibratePattern)
                spec.vibrate != null -> put("vibrate", spec.vibrate)
            }
            spec.speak?.toValues()?.filterValues { it != null }?.let { put("speak", it) }
            spec.snooze?.let { snooze ->
                if (!snooze.enabled) put("snooze", false)
                else put("snooze", LinkedHashMap<String, Any?>().apply {
                    snooze.minutes?.let { put("minutes", it) }
                    snooze.max?.let { put("max", it) }
                })
            }
            spec.autoSilenceMinutes?.let { put("autoSilenceMinutes", it) }
            put("source", snapshot.source)
            put("updatedBy", snapshot.updatedBy)
            put("createdAt", snapshot.createdAt)
            put("updatedAt", snapshot.updatedAt)
            snapshot.nextTrigger?.let {
                put("nextTrigger", it)
                put("nextTriggerTime", isoTime(it, zone))
            }
            put("skipping", snapshot.skipping)
            put("hasBindings", snapshot.hasBindings)
        }
    }

    private fun isoTime(millis: Long, zone: ZoneId): String = ISO.format(Instant.ofEpochMilli(millis).atZone(zone))

    private fun only(args: Map<String, Any?>, vararg names: String) {
        val unknown = args.keys.filter { it !in names }
        if (unknown.isNotEmpty()) throw AlarmException.invalidOptions("unknown argument${if (unknown.size > 1) "s" else ""} ${unknown.joinToString(", ")}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun map(value: Any?): Map<String, Any?> = value as? Map<String, Any?> ?: throw AlarmException(AlarmErrorCodes.INTERNAL, "unexpected answer")

    private fun maps(value: Any?): List<Map<String, Any?>> = (value as? List<*>)?.map { map(it) } ?: emptyList()

    companion object {

        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        /**
         * T94: the alarm error codes as tool error codes. An invalid option or rule, an unknown id and an option of a
         * missing feature are `INVALID_ARGUMENTS`, a ceiling `LIMIT_EXCEEDED`, the missing exact alarm permission
         * `NOT_READY` (with what to do), the rest `FAILED`; the message keeps the alarm code in front.
         * zh-CN: T94: 把闹钟错误码换成工具错误码. 选项或规则不合法, 未知 id 与缺少功能的选项为 `INVALID_ARGUMENTS`, 上限为 `LIMIT_EXCEEDED`, 缺精确闹钟
         * 权限为 `NOT_READY` (并说明怎么做), 其余为 `FAILED`; 消息前保留闹钟错误码.
         */
        fun code(alarmCode: String): String = when (alarmCode) {
            AlarmErrorCodes.INVALID_OPTIONS, AlarmErrorCodes.INVALID_RULE, AlarmErrorCodes.NOT_FOUND, AlarmErrorCodes.UNSUPPORTED -> PluginToolErrorCodes.INVALID_ARGUMENTS
            AlarmErrorCodes.LIMIT_EXCEEDED -> PluginToolErrorCodes.LIMIT_EXCEEDED
            AlarmErrorCodes.PERMISSION_REQUIRED -> PluginToolErrorCodes.NOT_READY
            else -> PluginToolErrorCodes.FAILED
        }

        private val TIMER_TOOLS = setOf(ChimeToolCatalog.TOOL_TIMER_START, ChimeToolCatalog.TOOL_TIMER_LIST, ChimeToolCatalog.TOOL_TIMER_CONTROL)

        /** [timer]: a timer tool, whose permission hint cannot suggest `enabled: false`. zh-CN: [timer]: 计时器工具, 其权限提示不能建议 `enabled: false`. */
        fun error(e: AlarmException, timer: Boolean = false): PluginToolException {
            val detail = e.detail.ifEmpty { "no detail" }
            val hint = when {
                e.code != AlarmErrorCodes.PERMISSION_REQUIRED -> ""
                timer -> " (3-Chime Alarm may not set exact alarms: the user can allow it on its permissions page)"
                else -> " (3-Chime Alarm may not set exact alarms: the user can allow it on its permissions page, or pass enabled false)"
            }
            return PluginToolException(code(e.code), "${e.code}: $detail$hint", e)
        }
    }
}
