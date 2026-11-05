package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm

import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import org.autojs.plugin.alarm.api.AlarmValues

/**
 * The `filter` Bundle of `listAlarms` and `upcoming` (protocol "Methods"): `sources`, `tag`, `enabled` and `label`
 * (exact match; a RegExp label filter of the script is applied by the host on the answer). Absent members match all.
 *
 * zh-CN: `listAlarms` 与 `upcoming` 的 `filter` Bundle (协议 "Methods"): `sources`, `tag`, `enabled` 与 `label` (精确匹配; 脚本的正则标签过滤
 * 由宿主在应答上执行). 缺省的成员匹配全部.
 */
data class AlarmFilter(
    val sources: Set<String>? = null,
    val tag: String? = null,
    val enabled: Boolean? = null,
    val label: String? = null,
) {

    fun matches(record: AlarmRecord): Boolean =
        (sources == null || record.source in sources) &&
            (tag == null || record.spec.tag == tag) &&
            (enabled == null || record.enabled == enabled) &&
            (label == null || record.spec.label == label)

    companion object {

        val ALL = AlarmFilter()

        private val KEYS: Set<String> = linkedSetOf(
            AlarmOptionKeys.FILTER_SOURCES, AlarmOptionKeys.FILTER_TAG, AlarmOptionKeys.FILTER_ENABLED, AlarmOptionKeys.FILTER_LABEL,
        )

        /** Reads the filter group; unknown keys and mistyped members throw `INVALID_OPTIONS`. zh-CN: 读取过滤分组, 未知键与类型不符报 `INVALID_OPTIONS`. */
        fun fromValues(values: Map<String, Any?>?): AlarmFilter {
            if (values == null) return ALL
            values.keys.firstOrNull { it !in KEYS }?.let { throw AlarmException.invalidOptions("unknown option 'filter.$it'") }
            val sources = values[AlarmOptionKeys.FILTER_SOURCES]?.let {
                val list = AlarmValues.stringList(values, AlarmOptionKeys.FILTER_SOURCES)
                    ?: throw AlarmException.invalidOptions("filter.sources must be an array of strings")
                list.firstOrNull { source -> source !in AlarmContract.SOURCES }?.let { source ->
                    throw AlarmException.invalidOptions("filter.sources must contain ${AlarmContract.SOURCES.joinToString(" / ")}, got '$source'")
                }
                list.toSet()
            }
            val tag = values[AlarmOptionKeys.FILTER_TAG]?.let {
                it as? String ?: throw AlarmException.invalidOptions("filter.tag must be a string")
            }
            val enabled = values[AlarmOptionKeys.FILTER_ENABLED]?.let {
                it as? Boolean ?: throw AlarmException.invalidOptions("filter.enabled must be a boolean")
            }
            val label = values[AlarmOptionKeys.FILTER_LABEL]?.let {
                it as? String ?: throw AlarmException.invalidOptions("filter.label must be a string")
            }
            return AlarmFilter(sources, tag, enabled, label)
        }
    }
}
