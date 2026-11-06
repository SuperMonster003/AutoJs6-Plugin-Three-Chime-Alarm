package io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Roadmap P4.1 (T89): the small JSON reader and writer of the tool layer: value types, escapes, round trips and the
 * documents it refuses.
 * zh-CN: 路线图 P4.1 (T89): 工具层的小型 JSON 读写: 值类型, 转义, 往返, 以及它拒绝的文档.
 */
class ToolJsonTest {

    @Test
    fun `values keep their types and order`() {
        val parsed = ToolJson.parseObject("""{"b": 1, "a": [true, false, null, -2.5, 1e3, "x"], "c": {"d": 9007199254740993}}""")
        assertEquals(listOf("b", "a", "c"), parsed.keys.toList())
        assertEquals(1L, parsed["b"])
        assertEquals(listOf(true, false, null, -2.5, 1000.0, "x"), parsed["a"])
        assertEquals(mapOf("d" to 9007199254740993L), parsed["c"])
    }

    @Test
    fun `strings escape and round trip`() {
        val text = "quote \" backslash \\ tab \t newline \n bell ${7.toChar()} \u4e2d\u6587 \ud83d\udd14"
        val encoded = ToolJson.encode(mapOf("t" to text))
        assertEquals(text, ToolJson.parseObject(encoded)["t"])
        assertEquals("""{"t":"a\"b\\c\nd\u0001"}""", ToolJson.encode(mapOf("t" to "a\"b\\c\nd" + 1.toChar())))
        assertEquals("A/B", ToolJson.parse("\"\\u0041\\/\\u0042\""))
    }

    @Test
    fun `numbers, arrays and maps are written compactly`() {
        assertEquals("""{"i":3,"l":4,"d":0.5,"w":2,"a":[1,2],"n":null}""", ToolJson.encode(linkedMapOf("i" to 3, "l" to 4L, "d" to 0.5, "w" to 2.0, "a" to longArrayOf(1, 2), "n" to null)))
        assertThrows(IllegalArgumentException::class.java) { ToolJson.encode(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { ToolJson.encode(Any()) }
    }

    @Test
    fun `malformed documents are refused`() {
        listOf("", "{", "{\"a\":}", "[1,]", "{\"a\":1}x", "{\"a\":1,\"a\":2}", "\"\\x\"", "tru", "-", "\"a\nb\"").forEach { text ->
            assertThrows(text, IllegalArgumentException::class.java) { ToolJson.parse(text) }
        }
        assertThrows(IllegalArgumentException::class.java) { ToolJson.parseObject("[]") }
    }
}
