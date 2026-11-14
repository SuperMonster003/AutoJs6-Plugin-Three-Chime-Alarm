package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random

/**
 * The `type` dismiss challenge (roadmap P9.1, T282): the user types [text] (the alarm's `type.text`, or one sentence of
 * the built in list of the screen language picked by the round's seed when it is omitted). The input matches when it
 * equals the text after Unicode NFC normalization, ignoring case and the blanks at both ends.
 * zh-CN: 关闭挑战 `type` (路线图 P9.1, T282): 用户输入 [text] (闹钟的 `type.text`; 省略时按本轮种子从界面语言的内置句库中选一句). 输入与文字在 Unicode NFC
 * 规范化后相同即通过, 忽略大小写与首尾空白.
 */
class TypeChallenge(val text: String) {

    fun matches(input: String): Boolean = normalize(input) == normalize(text)

    companion object {

        /** The text to type: [own] when it has any, else one of [sentences] picked by [seed]. zh-CN: 要输入的文字: [own] 非空白时用它, 否则按 [seed] 从 [sentences] 选一句. */
        fun of(own: String?, sentences: List<String>, seed: Long): TypeChallenge {
            val text = own?.takeIf { it.isNotBlank() }?.trim()
                ?: sentences.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { it[Random(seed).nextInt(it.size)] }
                ?: FALLBACK
            return TypeChallenge(text)
        }

        fun normalize(text: String): String = Normalizer.normalize(text.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)

        /** The text when no sentence is available (never expected). zh-CN: 没有可用句子时的文字 (不应出现). */
        const val FALLBACK = "I am awake"
    }
}
