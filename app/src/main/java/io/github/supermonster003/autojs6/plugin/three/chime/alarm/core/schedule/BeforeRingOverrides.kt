package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmSpec

/**
 * The `ring` overrides of `beforeRing` decisions (roadmap P8.4): only `label`, `sound`, `speak`, `vibrate` and
 * `vibratePattern`, written like a patch (T29: null goes back to the default). Several decisions merge in arrival order,
 * a later key replacing an earlier one; `vibrate: false` without a pattern drops an earlier pattern, as in a patch.
 * zh-CN: `beforeRing` 决定中的 `ring` 覆盖 (路线图 P8.4): 只允许 `label`, `sound`, `speak`, `vibrate` 与 `vibratePattern`, 写法同 patch (T29: null 回到默认值).
 * 多个决定按到达顺序合并, 后到的键取代先到的; 不带模式的 `vibrate: false` 去掉先前的模式, 与 patch 相同.
 */
object BeforeRingOverrides {

    /** The longest `reason` of a `skip` decision (protocol "beforeRing"). zh-CN: `skip` 决定的 `reason` 最长字符数. */
    const val MAX_REASON_CHARS = 100

    /**
     * Checks the keys of [overrides] and their values on [spec]: an unknown key or an invalid value is `INVALID_OPTIONS`.
     * zh-CN: 检查 [overrides] 的键及其在 [spec] 上的值: 未知键或无效值报 `INVALID_OPTIONS`.
     */
    fun check(spec: AlarmSpec, overrides: Map<String, Any?>, validator: AlarmOptionsValidator) {
        overrides.keys.firstOrNull { it !in AlarmContract.BEFORE_RING_OVERRIDE_KEYS }?.let {
            throw AlarmException.invalidOptions("'${AlarmOptionKeys.OVERRIDES}.$it' cannot be overridden; allowed: ${AlarmContract.BEFORE_RING_OVERRIDE_KEYS.joinToString()}")
        }
        if (overrides.isNotEmpty()) validator.patch(spec, overrides)
    }

    /** [current] with [next] merged after it. zh-CN: 在 [current] 之后合并 [next]. */
    fun merge(current: Map<String, Any?>?, next: Map<String, Any?>): Map<String, Any?> {
        val merged = LinkedHashMap(current.orEmpty())
        if (next[AlarmOptionKeys.VIBRATE] == false && !next.containsKey(AlarmOptionKeys.VIBRATE_PATTERN)) merged.remove(AlarmOptionKeys.VIBRATE_PATTERN)
        merged.putAll(next)
        return merged
    }

    /**
     * [spec] with [overrides] applied; [spec] itself when there are none or they no longer apply (the alarm was edited
     * since the decision), so the alarm always rings.
     * zh-CN: 应用 [overrides] 后的 [spec]; 没有覆盖或覆盖已不再适用 (决定之后闹钟被修改) 时为 [spec] 本身, 闹钟总能响.
     */
    fun apply(spec: AlarmSpec, overrides: Map<String, Any?>?, validator: AlarmOptionsValidator): AlarmSpec {
        if (overrides.isNullOrEmpty()) return spec
        return try {
            validator.patch(spec, overrides)
        } catch (_: AlarmException) {
            spec
        }
    }
}
