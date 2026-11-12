package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.timer

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.io.AtomicFiles
import org.autojs.plugin.alarm.api.AlarmJson
import org.autojs.plugin.alarm.api.AlarmValues
import org.autojs.plugin.alarm.api.TimerSpec
import java.io.File
import java.io.IOException

/**
 * One JSON document file in device protected storage (T109): `{format, version, ...}` written atomically. A missing,
 * unreadable or foreign file reads as null, so the timers and the stopwatch start empty rather than fail.
 * zh-CN: 设备加密存储中的一个 JSON 文档文件 (T109): `{format, version, ...}`, 原子写入. 文件缺失, 无法读取或格式不符时读为 null, 计时器与秒表从空
 * 开始而不是失败.
 */
class JsonDocumentFile(private val file: File, private val format: String, private val version: Int) {

    fun read(): Map<String, Any?>? = try {
        if (!file.isFile) {
            null
        } else {
            AlarmJson.parse(file.readText(Charsets.UTF_8))
                ?.takeIf { AlarmValues.string(it, KEY_FORMAT) == format && AlarmValues.int(it, KEY_VERSION) == version }
        }
    } catch (_: IOException) {
        null
    } catch (_: RuntimeException) {
        null
    }

    fun write(body: Map<String, Any?>) {
        val document = LinkedHashMap<String, Any?>().apply {
            put(KEY_FORMAT, format)
            put(KEY_VERSION, version)
            putAll(body)
        }
        try {
            AtomicFiles.write(file, AlarmJson.write(document).toByteArray(Charsets.UTF_8), ".${file.nameWithoutExtension}-")
        } catch (e: IOException) {
            throw AlarmException.ioFailed("cannot save ${file.name}: ${e.message}")
        }
    }

    private companion object {
        const val KEY_FORMAT = "format"
        const val KEY_VERSION = "version"
    }
}

/**
 * The timers file `timers.json` (T109); a damaged entry is skipped, the others load.
 * zh-CN: 计时器文件 `timers.json` (T109); 损坏的条目被跳过, 其余照常读取.
 */
class TimerStore(file: File, private val validate: (Map<String, Any?>) -> TimerSpec) {

    private val document = JsonDocumentFile(file, FORMAT, VERSION)

    fun load(): List<TimerRecord> = AlarmValues.mapList(document.read() ?: return emptyList(), KEY_TIMERS).orEmpty()
        .mapNotNull { runCatching { TimerRecord.fromValues(it, validate) }.getOrNull() }
        .distinctBy { it.id }

    fun save(records: Collection<TimerRecord>) = document.write(mapOf(KEY_TIMERS to records.map { it.toValues() }))

    companion object {
        const val FILE_NAME = "timers.json"
        private const val FORMAT = "three-chime-alarm.timers"
        private const val VERSION = 1
        private const val KEY_TIMERS = "timers"
    }
}
