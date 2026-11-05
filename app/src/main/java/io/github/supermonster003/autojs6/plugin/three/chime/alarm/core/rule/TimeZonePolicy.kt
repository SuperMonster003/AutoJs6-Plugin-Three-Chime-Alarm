package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import org.autojs.plugin.alarm.api.AlarmSpec
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * How a wall-clock alarm time becomes an instant (roadmap T9, P2.2): the alarm follows the device zone unless it has a
 * fixed `timeZone`. A time skipped by a daylight saving gap rings at the first moment after the jump (02:30 on a spring
 * forward night rings at 03:00), a time repeated by an overlap rings once, at the first (earlier) of the two moments.
 *
 * zh-CN: 墙上时间如何变为时刻 (路线图 T9, P2.2): 闹钟跟随设备时区, 除非设了固定 `timeZone`. 夏令时跳过的时刻在跳变后的第一刻响 (春季拨快那晚的
 * 02:30 在 03:00 响); 重叠而重复的时刻只响一次, 取两者中较早的那一刻.
 */
object TimeZonePolicy {

    /** The zone an alarm is computed in. zh-CN: 闹钟计算所用的时区. */
    fun zoneOf(spec: AlarmSpec, deviceZone: ZoneId): ZoneId = spec.timeZone?.let(::zone) ?: deviceZone

    /** The zone of an IANA id, `INVALID_OPTIONS` when the id is unknown. zh-CN: IANA 时区 id 对应的时区, 未知时报 `INVALID_OPTIONS`. */
    fun zone(id: String): ZoneId = try {
        ZoneId.of(id)
    } catch (_: DateTimeException) {
        throw AlarmException.invalidOptions("timeZone '$id' is not a known IANA time zone")
    }

    /** The instant [local] rings at in [zone]. zh-CN: [local] 在 [zone] 中响铃的时刻. */
    fun instantOf(local: LocalDateTime, zone: ZoneId): Instant {
        val rules = zone.rules
        val offsets = rules.getValidOffsets(local)
        return when {
            offsets.size == 1 -> local.toInstant(offsets[0])
            // A gap: the wall-clock time does not exist, ring when the clock jumps over it.
            // zh-CN: 空档: 该墙上时间不存在, 在时钟跳过它的那一刻响.
            offsets.isEmpty() -> rules.getTransition(local).instant
            // An overlap: the earlier offset is the one before the transition (the larger one), so the first moment.
            // zh-CN: 重叠: 较早的时刻对应跳变前的偏移 (较大的那个).
            else -> local.toInstant(offsets.maxBy { it.totalSeconds })
        }
    }

    /** [instantOf] as epoch milliseconds. zh-CN: 以毫秒时间戳表示的 [instantOf]. */
    fun millisOf(local: LocalDateTime, zone: ZoneId): Long = instantOf(local, zone).toEpochMilli()
}
