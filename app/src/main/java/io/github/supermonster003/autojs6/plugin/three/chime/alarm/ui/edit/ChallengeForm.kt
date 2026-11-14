package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit

import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmDismiss
import org.autojs.plugin.alarm.api.AlarmLimits

/**
 * The fields of the dismiss challenge dialog of the edit page (roadmap P9.5, T291): the challenge, the math difficulty
 * and count, the shake count, the text to type (empty for a random sentence) and whether the user may snooze. The page
 * offers the built-in challenges; `script` shows only while a script set it, and choosing it keeps the script's
 * settings. A value the user leaves as it was keeps its stored form (an omitted value stays omitted), so an untouched
 * dialog changes nothing.
 * zh-CN: 编辑页关闭挑战对话框的字段 (路线图 P9.5, T291): 挑战, 算术难度与题数, 摇动次数, 要输入的文字 (为空时随机句子), 以及用户能否贪睡. 本页提供内置挑战;
 * `script` 只在脚本设置了它时出现, 选择它即保留脚本的设置. 用户未改动的值保持其存储形式 (省略的值仍省略), 因此未改动的对话框不产生改动.
 */
internal data class ChallengeForm(
    val challenge: String,
    val mathDifficulty: Int,
    val mathCount: Int,
    val shakeCount: Int,
    val typeText: String,
    val allowSnooze: Boolean,
) {

    /**
     * The dismiss option of this form for an alarm that has [original]: null drops the option (no challenge).
     * zh-CN: 本表单对应的关闭选项, [original] 为闹钟原有的选项: null 表示移除该选项 (无挑战).
     */
    fun toDismiss(original: AlarmDismiss?): AlarmDismiss? {
        val snooze = keep(original?.allowSnooze, true, allowSnooze)
        return when (challenge) {
            AlarmContract.CHALLENGE_NONE -> original?.takeIf { it.challenge == null || it.challenge == AlarmContract.CHALLENGE_NONE }
            // Only a script sets `script`; the page keeps its settings. zh-CN: 只有脚本设置 `script`; 本页保留其设置.
            AlarmContract.CHALLENGE_SCRIPT -> original?.takeIf { it.challenge == AlarmContract.CHALLENGE_SCRIPT }?.copy(allowSnooze = snooze)
            else -> {
                val same = original?.takeIf { it.challenge == challenge }
                AlarmDismiss(
                    challenge = challenge,
                    mathDifficulty = if (challenge == AlarmContract.CHALLENGE_MATH) keep(same?.mathDifficulty, AlarmLimits.DEFAULT_MATH_DIFFICULTY, mathDifficulty) else null,
                    mathCount = if (challenge == AlarmContract.CHALLENGE_MATH) keep(same?.mathCount, AlarmLimits.DEFAULT_MATH_COUNT, mathCount) else null,
                    shakeCount = if (challenge == AlarmContract.CHALLENGE_SHAKE) keep(same?.shakeCount, AlarmLimits.DEFAULT_SHAKE_COUNT, shakeCount) else null,
                    typeText = if (challenge == AlarmContract.CHALLENGE_TYPE) text(same?.typeText) else null,
                    allowSnooze = snooze,
                )
            }
        }
    }

    /** The text to type: blank is a random sentence; an unchanged text keeps its stored form. zh-CN: 要输入的文字: 空白为随机句子; 未改动时保持存储形式. */
    private fun text(stored: String?): String? {
        val chosen = typeText.trim().ifEmpty { null }
        return if (chosen == stored?.trim()?.ifEmpty { null }) stored else chosen
    }

    companion object {

        /** The steps of the shake count slider. zh-CN: 摇动次数滑块的步长. */
        const val SHAKE_STEP = 5

        /** The form of a stored dismiss option. zh-CN: 已存关闭选项对应的表单. */
        fun of(dismiss: AlarmDismiss?): ChallengeForm = ChallengeForm(
            challenge = dismiss?.challenge ?: AlarmContract.CHALLENGE_NONE,
            mathDifficulty = dismiss?.mathDifficulty ?: AlarmLimits.DEFAULT_MATH_DIFFICULTY,
            mathCount = dismiss?.mathCount ?: AlarmLimits.DEFAULT_MATH_COUNT,
            shakeCount = dismiss?.shakeCount ?: AlarmLimits.DEFAULT_SHAKE_COUNT,
            typeText = dismiss?.typeText.orEmpty(),
            allowSnooze = dismiss?.allowSnooze ?: true,
        )

        /**
         * The challenges the dialog offers: none and the built-in ones, then `script` while [dismiss] has it.
         * zh-CN: 对话框提供的挑战: 无与内置挑战, [dismiss] 为 `script` 时再加上它.
         */
        fun choices(dismiss: AlarmDismiss?): List<String> {
            val builtIn = listOf(AlarmContract.CHALLENGE_NONE, AlarmContract.CHALLENGE_MATH, AlarmContract.CHALLENGE_SHAKE, AlarmContract.CHALLENGE_TYPE)
            return if (dismiss?.challenge == AlarmContract.CHALLENGE_SCRIPT) builtIn + AlarmContract.CHALLENGE_SCRIPT else builtIn
        }

        /** [chosen], or [stored] (maybe omitted) when [chosen] is what it resolves to. zh-CN: [chosen]; 与 [stored] 的实际值相同时保留 [stored] (可能为省略). */
        private fun <T : Any> keep(stored: T?, default: T, chosen: T): T? = if (chosen == (stored ?: default)) stored else chosen
    }
}
