package io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool

/**
 * A small JSON reader and writer for the plugin tools (roadmap P4.1, T89): the tool layer works on plain value trees
 * (`Map<String, Any?>` in insertion order, `List<Any?>`, `String`, `Long` for numbers without a fraction or exponent,
 * `Double` otherwise, `Boolean`, `null`) so the JVM tests run it without Android's `org.json`. The writer also takes
 * `Int` and the other [Number] types, `LongArray` and `IntArray`; non-finite numbers and other types are refused.
 * Strings are written as UTF-8 text with the control characters, `"` and the backslash escaped.
 * zh-CN: 插件工具使用的小型 JSON 读写 (路线图 P4.1, T89): 工具层只处理普通值树 (按插入顺序的 `Map<String, Any?>`, `List<Any?>`, `String`,
 * 无小数与指数的数字为 `Long`, 其他数字为 `Double`, `Boolean`, `null`), 因此 JVM 测试无需 Android 的 `org.json`. 写出时还接受 `Int` 等
 * [Number] 类型, `LongArray` 与 `IntArray`; 非有限数字与其他类型被拒绝. 字符串按 UTF-8 文本写出, 只转义控制字符, `"` 与反斜杠.
 */
internal object ToolJson {

    fun parse(text: String): Any? {
        val parser = Parser(text)
        val value = parser.value()
        parser.end()
        return value
    }

    /** [parse] for a document that must be an object. zh-CN: 文档必须是对象时的 [parse]. */
    @Suppress("UNCHECKED_CAST")
    fun parseObject(text: String): Map<String, Any?> =
        parse(text) as? Map<String, Any?> ?: throw IllegalArgumentException("the JSON document is not an object")

    fun encode(value: Any?): String = StringBuilder().also { write(it, value) }.toString()

    private fun write(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is String -> string(out, value)
            is Boolean -> out.append(value)
            is Double, is Float -> {
                val number = (value as Number).toDouble()
                require(!number.isNaN() && !number.isInfinite()) { "JSON has no $number" }
                if (number == Math.floor(number) && Math.abs(number) < 1e15) out.append(number.toLong()) else out.append(number)
            }
            is Number -> out.append(value.toLong())
            is LongArray -> write(out, value.toList())
            is IntArray -> write(out, value.toList())
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((key, member) in value) {
                    if (!first) out.append(',')
                    first = false
                    string(out, key as? String ?: throw IllegalArgumentException("JSON object keys are strings"))
                    out.append(':')
                    write(out, member)
                }
                out.append('}')
            }
            is Iterable<*> -> {
                out.append('[')
                var first = true
                for (item in value) {
                    if (!first) out.append(',')
                    first = false
                    write(out, item)
                }
                out.append(']')
            }
            else -> throw IllegalArgumentException("cannot write ${value.javaClass.simpleName} as JSON")
        }
    }

    private fun string(out: StringBuilder, text: String) {
        out.append('"')
        for (c in text) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append("\\u").append(String.format("%04x", c.code))
                else -> out.append(c)
            }
        }
        out.append('"')
    }

    private class Parser(private val s: String) {

        private var i = 0

        fun end() {
            ws()
            if (i != s.length) fail("unexpected text after the value")
        }

        fun value(): Any? {
            ws()
            if (i >= s.length) fail("unexpected end")
            return when (s[i]) {
                '{' -> obj()
                '[' -> array()
                '"' -> string()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> number()
            }
        }

        private fun obj(): Map<String, Any?> {
            val out = LinkedHashMap<String, Any?>()
            i++
            ws()
            if (peek('}')) return out.also { i++ }
            while (true) {
                ws()
                if (!peek('"')) fail("expected a key")
                val key = string()
                ws()
                expect(':')
                if (out.containsKey(key)) fail("duplicate key \"$key\"")
                out[key] = value()
                ws()
                if (peek(',')) { i++; continue }
                expect('}')
                return out
            }
        }

        private fun array(): List<Any?> {
            val out = ArrayList<Any?>()
            i++
            ws()
            if (peek(']')) return out.also { i++ }
            while (true) {
                out.add(value())
                ws()
                if (peek(',')) { i++; continue }
                expect(']')
                return out
            }
        }

        private fun string(): String {
            i++
            val out = StringBuilder()
            while (true) {
                if (i >= s.length) fail("unterminated string")
                val c = s[i++]
                when {
                    c == '"' -> return out.toString()
                    c == '\\' -> {
                        if (i >= s.length) fail("unterminated escape")
                        when (val e = s[i++]) {
                            '"' -> out.append('"')
                            '\\' -> out.append('\\')
                            '/' -> out.append('/')
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) fail("short unicode escape")
                                out.append(s.substring(i, i + 4).toIntOrNull(16)?.toChar() ?: fail("bad unicode escape"))
                                i += 4
                            }
                            else -> fail("bad escape \\$e")
                        }
                    }
                    c < ' ' -> fail("control character in a string")
                    else -> out.append(c)
                }
            }
        }

        private fun number(): Any {
            val start = i
            if (peek('-')) i++
            while (i < s.length && s[i] in "0123456789+-.eE") i++
            val text = s.substring(start, i)
            if (text.isEmpty() || text == "-") fail("unexpected character")
            val integral = text.none { it == '.' || it == 'e' || it == 'E' }
            if (integral) text.toLongOrNull()?.let { return it }
            return text.toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() } ?: fail("bad number \"$text\"")
        }

        private fun literal(word: String, value: Boolean?): Boolean? {
            if (!s.startsWith(word, i)) fail("unexpected literal")
            i += word.length
            return value
        }

        private fun ws() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++
        }

        private fun peek(c: Char) = i < s.length && s[i] == c

        private fun expect(c: Char) {
            if (!peek(c)) fail("expected '$c'")
            i++
        }

        private fun fail(message: String): Nothing = throw IllegalArgumentException("invalid JSON at $i: $message")
    }
}
