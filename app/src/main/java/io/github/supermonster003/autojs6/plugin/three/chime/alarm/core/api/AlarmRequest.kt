package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.api

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmValues

/**
 * One Binder request of `IAlarmPlugin` (roadmap P2.5, appendix A section 11) as a value tree without the
 * `contractVersion` stamp. [of] refuses a contract version the plugin does not speak (`UNSUPPORTED`) and every key
 * outside the method's keys (`INVALID_OPTIONS`); the typed readers refuse a missing required key or a mistyped value
 * with `INVALID_OPTIONS`. A key present with a null value reads as absent.
 *
 * zh-CN: `IAlarmPlugin` 的一个 Binder 请求 (路线图 P2.5, 附录 A 第 11 节), 去掉 `contractVersion` 标记后的值树. [of] 拒绝插件不支持的契约版本
 * (`UNSUPPORTED`) 与方法键之外的任何键 (`INVALID_OPTIONS`); 类型化读取对缺少必需键或类型不符报 `INVALID_OPTIONS`. 值为 null 的键视为不存在.
 */
class AlarmRequest private constructor(val values: Map<String, Any?>) {

    fun string(key: String): String? {
        val raw = values[key] ?: return null
        return raw as? String ?: throw AlarmException.invalidOptions("'$key' must be a string")
    }

    fun requireString(key: String): String = string(key) ?: throw missing(key)

    fun boolean(key: String): Boolean? {
        val raw = values[key] ?: return null
        return raw as? Boolean ?: throw AlarmException.invalidOptions("'$key' must be a boolean")
    }

    fun requireBoolean(key: String): Boolean = boolean(key) ?: throw missing(key)

    fun int(key: String): Int? {
        if (values[key] == null) return null
        return AlarmValues.int(values, key) ?: throw AlarmException.invalidOptions("'$key' must be an integer")
    }

    fun long(key: String): Long? {
        if (values[key] == null) return null
        return AlarmValues.long(values, key) ?: throw AlarmException.invalidOptions("'$key' must be an integer")
    }

    fun requireLong(key: String): Long = long(key) ?: throw missing(key)

    fun map(key: String): Map<String, Any?>? {
        if (values[key] == null) return null
        return AlarmValues.map(values, key) ?: throw AlarmException.invalidOptions("'$key' must be an object")
    }

    fun requireMap(key: String): Map<String, Any?> = map(key) ?: throw missing(key)

    fun stringList(key: String): List<String>? {
        if (values[key] == null) return null
        return AlarmValues.stringList(values, key) ?: throw AlarmException.invalidOptions("'$key' must be an array of strings")
    }

    /**
     * A count within [min] - [max], [default] when absent: below [min] is `INVALID_OPTIONS`, above [max] is
     * `LIMIT_EXCEEDED` (appendix A section 11).
     * zh-CN: [min] - [max] 内的数量, 不存在时为 [default]: 小于 [min] 报 `INVALID_OPTIONS`, 大于 [max] 报 `LIMIT_EXCEEDED`.
     */
    fun count(key: String, min: Int, max: Int, default: Int): Int {
        val value = int(key) ?: return default
        if (value < min) throw AlarmException.invalidOptions("'$key' must be at least $min, got $value")
        if (value > max) throw AlarmException.limitExceeded("'$key' is at most $max, got $value")
        return value
    }

    companion object {

        /**
         * Reads [values] for a method whose request keys are [allowed]; `null` accepts every key (the alarm options,
         * which the validator checks itself).
         * zh-CN: 按请求键 [allowed] 读取 [values]; `null` 表示接受所有键 (闹钟选项由校验器自行检查).
         */
        fun of(values: Map<String, Any?>, allowed: Set<String>?): AlarmRequest {
            val stamp = values[AlarmContract.KEY_CONTRACT_VERSION]
            if (stamp != null) {
                val version = AlarmValues.int(values, AlarmContract.KEY_CONTRACT_VERSION)
                if (version == null || !AlarmContract.supportsContractVersion(version)) {
                    throw AlarmException.unsupported("contract version $stamp is not supported (plugin speaks ${AlarmContract.CONTRACT_VERSION})")
                }
            }
            val body = values.filterKeys { it != AlarmContract.KEY_CONTRACT_VERSION }
            if (allowed != null) {
                body.keys.firstOrNull { it !in allowed }?.let { throw AlarmException.invalidOptions("unknown option '${clip(it)}'") }
            }
            return AlarmRequest(body)
        }

        private fun missing(key: String) = AlarmException.invalidOptions("missing option '$key'")

        private fun clip(text: String): String = if (text.length <= 64) text else text.substring(0, 64) + "..."
    }
}
