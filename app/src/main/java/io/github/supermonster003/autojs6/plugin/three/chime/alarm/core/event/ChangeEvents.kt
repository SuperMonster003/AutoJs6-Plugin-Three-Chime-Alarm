package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.event

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmChange
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmEvent
import java.security.SecureRandom

/**
 * The `change` event of a change of the alarm list (appendix A section 5.1, roadmap P2.5): every add, update and
 * removal, whoever made it (Binder, plugin tools, system clock intents, the plugin's screens), with `action` and
 * `source` (T18). A change has no `scheduledAt`; its `occurrence` is a value of its own (T73),
 * `<alarm id>@change~<firedAt>~<nonce>`, so two changes in the same millisecond still differ.
 * zh-CN: 闹钟列表变更的 `change` 事件 (附录 A 第 5.1 节, 路线图 P2.5): 每次添加, 修改与删除, 无论由谁发起 (Binder, 插件工具, 系统时钟 Intent,
 * 插件界面), 带 `action` 与 `source` (T18). 变更没有 `scheduledAt`; 其 `occurrence` 自成一值 (T73), 形如 `<闹钟 id>@change~<firedAt>~<随机串>`,
 * 同一毫秒内的两次变更也不相同.
 */
object ChangeEvents {

    fun of(change: AlarmChange, firedAt: Long, nonce: String = nonce()): AlarmEvent = AlarmEvent(
        type = AlarmContract.EVENT_CHANGE,
        kind = AlarmContract.KIND_ALARM,
        alarmId = change.alarmId,
        occurrence = occurrenceOf(change.alarmId, firedAt, nonce),
        firedAt = firedAt,
        label = (change.after ?: change.before)?.spec?.label,
        action = change.action,
        source = change.source,
    )

    fun occurrenceOf(alarmId: String, firedAt: Long, nonce: String): String = "$alarmId@change~$firedAt~$nonce"

    /** Six random base-36 characters. zh-CN: 六个随机的 36 进制字符. */
    fun nonce(): String = buildString(NONCE_CHARS) {
        repeat(NONCE_CHARS) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    private const val NONCE_CHARS = 6

    private const val ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz"

    private val random = SecureRandom()
}
