package io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool

import org.autojs.plugin.alarm.api.AlarmContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P4.1 (appendix A section 10, T17, T89 - T93): the catalog keeps the rules the host applies when it loads a
 * catalog (host `docs/dev/plugin-tool-protocol-v1.md`, "Catalog" and "Schema subset"), so no tool is dropped on a
 * device: names and lengths, risks, time limits and concurrency, closed objects, the keywords of each node type,
 * `oneOf` and array items, enums and defaults that match their node. It also pins the tool set, the AlarmOptions
 * parameters without local files and with the `holidays` option of 1.2.0 (roadmap P8.7, T192, T193) but without later
 * features, `holiday_check`, and English ASCII descriptions.
 * zh-CN: 路线图 P4.1 (附录 A 第 10 节, T17, T89 - T93): 目录遵守宿主加载目录时的规则 (宿主协议文档 "Catalog" 与 "Schema subset"), 设备上不会有工具
 * 被丢弃: 名称与长度, 风险, 时限与并发, 封闭对象, 各类节点的关键字, `oneOf` 与数组元素, 与节点相符的枚举和默认值. 同时固定工具集合, 不含本地文件,
 * 含 1.2.0 的 `holidays` 选项 (路线图 P8.7, T192, T193) 但不含之后功能的 AlarmOptions 参数, `holiday_check`, 以及英文 ASCII 说明.
 */
class ChimeToolCatalogTest {

    private val catalog: Map<String, Any?> = ToolJson.parseObject(ChimeToolCatalog.JSON)

    @Suppress("UNCHECKED_CAST")
    private val tools: List<Map<String, Any?>> = catalog["tools"] as List<Map<String, Any?>>

    private fun tool(name: String): Map<String, Any?> = tools.single { it["name"] == name }

    @Suppress("UNCHECKED_CAST")
    private fun properties(name: String): Map<String, Map<String, Any?>> =
        (tool(name)["inputSchema"] as Map<String, Any?>)["properties"] as Map<String, Map<String, Any?>>

    @Test
    fun `the catalog names the thirteen tools under the short name chime`() {
        assertEquals("chime", catalog["shortName"])
        assertTrue(Regex("^[a-z][a-z0-9]{1,15}$").matches(ChimeToolCatalog.SHORT_NAME))
        assertEquals(ChimeToolCatalog.TOOLS, tools.map { it["name"] })
        assertEquals(13, tools.size)
        assertTrue("at most 256 KiB", ChimeToolCatalog.JSON.toByteArray().size <= 256 * 1024)
        tools.forEach { tool ->
            val name = tool["name"] as String
            assertTrue(name, Regex("^[a-z][a-z0-9]*(_[a-z0-9]+)*$").matches(name))
            assertTrue(name, "chime_$name".length <= 50)
            assertTrue(name, (tool["title"] as String).length <= 128)
            val description = tool["description"] as String
            assertTrue(name, description.length in 1..4096)
            assertTrue("$name: English ASCII", description.all { it.code in 0x20..0x7E })
            assertTrue(name, tool["risk"] in setOf("readonly", "write", "destructive", "network"))
            assertTrue(name, (tool["timeoutMs"] as Long) in 1..300_000)
            assertTrue(name, (tool["maxConcurrency"] as Long) in 1..4)
            assertEquals(setOf("name", "title", "description", "inputSchema", "risk", "timeoutMs", "maxConcurrency"), tool.keys)
            @Suppress("UNCHECKED_CAST")
            checkNode(tool["inputSchema"] as Map<String, Any?>, name, depth = 1)
        }
    }

    @Test
    fun `risks follow what each tool does`() {
        val risks = tools.associate { it["name"] to it["risk"] }
        assertEquals(
            mapOf(
                "alarm_add" to "write", "alarm_list" to "readonly", "alarm_update" to "write", "alarm_remove" to "destructive",
                "alarm_set_enabled" to "write", "alarm_skip_next" to "write", "alarm_upcoming" to "readonly", "alarm_dismiss" to "write",
                "alarm_snooze" to "write", "timer_start" to "write", "timer_list" to "readonly", "timer_control" to "write",
                "holiday_check" to "readonly",
            ),
            risks,
        )
    }

    @Test
    fun `the option parameters follow AlarmOptions without local files and later features`() {
        assertEquals(ChimeToolArguments.OPTION_KEYS, properties("alarm_add").keys)
        assertEquals(setOf("id") + ChimeToolArguments.OPTION_KEYS + "clear", properties("alarm_update").keys)
        val text = ChimeToolCatalog.JSON
        assertFalse("no special file parameters (T91)", text.contains("x-autojs6-type"))
        listOf("dismiss", "challenge", "beforeRing", "bind", "script\"").forEach { assertFalse("$it is not offered (T17)", properties("alarm_add").containsKey(it)) }
        assertFalse(text.contains("\"beforeRing\""))
        assertEquals(setOf("source", "tag", "enabled", "label", "limit"), properties("alarm_list").keys)
        assertEquals(setOf("count", "source", "tag", "enabled", "label"), properties("alarm_upcoming").keys)
        assertEquals(setOf("id"), properties("alarm_remove").keys)
        assertEquals(setOf("id", "enabled"), properties("alarm_set_enabled").keys)
        assertEquals(setOf("id", "cancel"), properties("alarm_skip_next").keys)
        assertEquals(setOf("id"), properties("alarm_dismiss").keys)
        assertEquals(setOf("id", "minutes"), properties("alarm_snooze").keys)
        @Suppress("UNCHECKED_CAST")
        val clear = (properties("alarm_update").getValue("clear")["items"] as Map<String, Any?>)["enum"]
        assertEquals(ChimeToolCatalog.CLEARABLE, clear)
        assertNotNull(properties("alarm_upcoming").getValue("count")["default"])
    }

    @Test
    fun `the holidays option and holiday_check follow the holiday contract (T192, T193)`() {
        val holidays = properties("alarm_add").getValue("holidays")
        assertEquals(properties("alarm_add").getValue("holidays"), properties("alarm_update").getValue("holidays"))
        assertEquals(listOf("mode"), holidays["required"])
        assertEquals(false, holidays["additionalProperties"])
        @Suppress("UNCHECKED_CAST")
        val members = holidays["properties"] as Map<String, Map<String, Any?>>
        assertEquals(setOf("mode", "region"), members.keys)
        assertEquals(AlarmContract.HOLIDAY_MODES.toList(), members.getValue("mode")["enum"])
        assertTrue("holidays can be cleared", "holidays" in ChimeToolCatalog.CLEARABLE)

        assertEquals(setOf("date", "region"), properties("holiday_check").keys)
        @Suppress("UNCHECKED_CAST")
        val check = tool("holiday_check")["inputSchema"] as Map<String, Any?>
        assertFalse("every argument has a default", check.containsKey("required"))
        assertEquals(members.getValue("region") - "description", properties("holiday_check").getValue("region") - "description")
        val description = tool("holiday_check")["description"] as String
        listOf("isHoliday", "isWorkday", "estimated", "HOLIDAY_DATA_MISSING", "INVALID_ARGUMENTS").forEach { assertTrue(it, description.contains(it)) }
    }

    @Test
    fun `the timer tools take durations and the timer actions but not the stopwatch (T17, T126 - T128)`() {
        assertEquals(ChimeToolArguments.TIMER_KEYS, properties("timer_start").keys)
        assertEquals(emptySet<String>(), properties("timer_list").keys)
        assertEquals(setOf("id", "action", "duration"), properties("timer_control").keys)
        @Suppress("UNCHECKED_CAST")
        val start = tool("timer_start")["inputSchema"] as Map<String, Any?>
        assertEquals(listOf("duration"), start["required"])
        @Suppress("UNCHECKED_CAST")
        val control = tool("timer_control")["inputSchema"] as Map<String, Any?>
        assertEquals(listOf("id", "action"), control["required"])
        assertEquals(ChimeToolCatalog.TIMER_ACTIONS, properties("timer_control").getValue("action")["enum"])
        assertEquals(properties("alarm_add").getValue("sound"), properties("timer_start").getValue("sound"))
        val durations = properties("timer_start").getValue("duration")["oneOf"] as List<*>
        assertEquals(1_000L, (durations[0] as Map<*, *>)["minimum"])
        assertEquals(360_000_000L, (durations[0] as Map<*, *>)["maximum"])
        val changes = properties("timer_control").getValue("duration")["oneOf"] as List<*>
        assertEquals(-360_000_000L, (changes[0] as Map<*, *>)["minimum"])
        assertFalse("the stopwatch is not offered (T17)", tools.any { (it["name"] as String).contains("stopwatch") })
    }

    // ------------------------------------------------------------------ the host's schema subset

    private fun checkNode(node: Map<String, Any?>, path: String, depth: Int) {
        assertTrue("$path: at most 8 levels", depth <= 8)
        (node["description"] as? String)?.let { assertTrue("$path: description", it.length <= 4096 && it.all { c -> c.code in 0x20..0x7E }) }
        (node["title"] as? String)?.let { assertTrue("$path: title", it.length <= 128) }
        if (node.containsKey("oneOf")) {
            assertTrue("$path: keywords beside oneOf ${node.keys}", (node.keys - CHOICE_KEYS).isEmpty())
            val choices = node["oneOf"] as List<*>
            assertTrue("$path: oneOf needs 2 or more", choices.size >= 2)
            val types = choices.map { (it as Map<*, *>)["type"] }
            assertEquals("$path: oneOf branches must not overlap", types.size, types.toSet().size)
            assertFalse("$path: integer and number overlap", "integer" in types && "number" in types)
            choices.forEachIndexed { index, choice ->
                @Suppress("UNCHECKED_CAST")
                checkNode(choice as Map<String, Any?>, "$path.oneOf[$index]", depth + 1)
            }
            return
        }
        val type = node["type"] as? String
        assertTrue("$path: type $type", type in KEYS_BY_TYPE)
        assertTrue("$path: keywords ${node.keys - KEYS_BY_TYPE.getValue(type!!)} for $type", (node.keys - KEYS_BY_TYPE.getValue(type)).isEmpty())
        when (type) {
            "object" -> {
                assertEquals("$path: objects are closed", false, node["additionalProperties"])
                @Suppress("UNCHECKED_CAST")
                val properties = node["properties"] as? Map<String, Map<String, Any?>> ?: emptyMap()
                assertTrue("$path: at most 64 properties", properties.size <= 64)
                (node["required"] as? List<*>)?.forEach { assertTrue("$path: required $it", it in properties) }
                properties.forEach { (name, child) ->
                    assertTrue(name.length in 1..64)
                    checkNode(child, "$path.$name", depth + 1)
                }
            }
            "string" -> assertTrue("$path: lengths", ((node["minLength"] as? Long) ?: 0) <= ((node["maxLength"] as? Long) ?: Long.MAX_VALUE))
            "array" -> {
                @Suppress("UNCHECKED_CAST")
                val items = node["items"] as Map<String, Any?>
                assertFalse("$path: items must not be a oneOf", items.containsKey("oneOf"))
                assertTrue("$path: items must not be arrays", items["type"] != "array")
                checkNode(items, "$path[]", depth + 1)
            }
        }
        (node["enum"] as? List<*>)?.let { values ->
            assertTrue("$path: enum on scalars", type in setOf("string", "integer", "number", "boolean"))
            assertEquals("$path: enum repeats", values.size, values.toSet().size)
            values.forEach { assertTrue("$path: enum value $it matches $type", matches(type, it)) }
        }
        if (node.containsKey("default")) {
            val value = node["default"]
            assertTrue("$path: default $value matches", matches(type, value))
            (node["enum"] as? List<*>)?.let { assertTrue("$path: default in enum", value in it) }
            (node["minimum"] as? Number)?.let { assertTrue("$path: default >= minimum", (value as Number).toDouble() >= it.toDouble()) }
            (node["maximum"] as? Number)?.let { assertTrue("$path: default <= maximum", (value as Number).toDouble() <= it.toDouble()) }
        }
    }

    private fun matches(type: String, value: Any?): Boolean = when (type) {
        "string" -> value is String
        "integer" -> value is Long
        "number" -> value is Number
        "boolean" -> value is Boolean
        else -> false
    }

    private companion object {
        val COMMON = setOf("type", "title", "description", "default", "enum")
        val CHOICE_KEYS = setOf("oneOf", "title", "description", "default")
        val KEYS_BY_TYPE = mapOf(
            "object" to COMMON + setOf("properties", "required", "additionalProperties"),
            "string" to COMMON + setOf("minLength", "maxLength"),
            "integer" to COMMON + setOf("minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum"),
            "number" to COMMON + setOf("minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum"),
            "boolean" to COMMON,
            "array" to COMMON + setOf("items", "minItems", "maxItems"),
        )
    }
}
