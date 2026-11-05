package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm

import android.content.Context
import org.autojs.plugin.alarm.api.AlarmJson
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmValues
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID

/**
 * The alarm document (roadmap P2.1, T21): one JSON file `alarms.json` in device-protected storage, written as a temp
 * file, `fsync`ed and renamed over the old one, so a crash or power loss leaves either the old or the new document.
 *
 * [load] validates every record again ([AlarmRecord.fromValues] with the full options check) and migrates older document
 * versions ([MIGRATIONS]). A damaged record is skipped on its own; an unreadable document, an unknown format or a newer
 * version (after a downgrade) is read as far as possible. Whenever something was skipped, the original file is first
 * copied to `quarantine/` (the newest [MAX_QUARANTINE] copies are kept, T49) and the cleaned document is written back,
 * so the next start is clean and nothing is lost for a later recovery. Records over the limits (300, of which 200
 * enabled) are dropped or disabled in creation order.
 *
 * Not thread-safe: [AlarmRepository] serializes access.
 *
 * zh-CN: 闹钟文档 (路线图 P2.1, T21): 设备加密存储中的一个 JSON 文件 `alarms.json`, 先写临时文件并 `fsync`, 再改名替换旧文件, 崩溃或断电后
 * 留下的要么是旧文档要么是新文档. [load] 对每条记录再做一次完整校验 ([AlarmRecord.fromValues]), 并迁移旧版本文档 ([MIGRATIONS]). 损坏的记录单独跳过;
 * 不可读的文档, 未知格式或更新的版本 (降级后) 尽量读取. 只要跳过了内容, 就先把原文件复制到 `quarantine/` (保留最新 [MAX_QUARANTINE] 份, T49),
 * 再写回清理后的文档, 下次启动即为干净状态, 原内容也不丢失, 便于日后恢复. 超出上限 (300 条, 其中启用 200 条) 的记录按创建顺序丢弃或禁用.
 * 非线程安全, 由 [AlarmRepository] 串行访问.
 */
class AlarmStore(
    val directory: File,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** A record [load] skipped; [index] is its position in the document, -1 for the document itself. zh-CN: [load] 跳过的记录. */
    data class Damaged(val index: Int, val id: String?, val reason: String)

    data class LoadResult(
        val records: List<AlarmRecord>,
        val damaged: List<Damaged> = emptyList(),
        /** Ids disabled because more than 200 were enabled. zh-CN: 启用数超过 200 时被禁用的 id. */
        val disabledOverLimit: List<String> = emptyList(),
        /** The copy of the original document, when one was made. zh-CN: 原文档的副本 (若有). */
        val quarantined: File? = null,
        /** The document version before migration, when it was older. zh-CN: 迁移前的文档版本 (若更旧). */
        val migratedFrom: Int? = null,
        /** The document version when it was newer than [VERSION]. zh-CN: 文档版本比 [VERSION] 新时的版本号. */
        val newerVersion: Int? = null,
    ) {
        val clean: Boolean get() = damaged.isEmpty() && disabledOverLimit.isEmpty() && migratedFrom == null && newerVersion == null
    }

    val file: File get() = File(directory, FILE_NAME)

    val quarantineDirectory: File get() = File(directory, QUARANTINE_DIRECTORY)

    fun load(validate: (Map<String, Any?>) -> AlarmSpec): LoadResult {
        removeLeftoverTempFiles()
        val file = file
        if (!file.isFile) return LoadResult(emptyList())
        val bytes = try {
            if (file.length() > MAX_FILE_BYTES) return rescue(emptyList(), listOf(Damaged(-1, null, "document is larger than $MAX_FILE_BYTES bytes")))
            file.readBytes()
        } catch (e: IOException) {
            throw AlarmException.ioFailed("cannot read ${file.name}: ${e.message}")
        }
        val document = AlarmJson.parse(String(bytes, Charsets.UTF_8))
            ?: return rescue(emptyList(), listOf(Damaged(-1, null, "document is not a JSON object")))
        if (AlarmValues.string(document, KEY_FORMAT) != FORMAT) {
            return rescue(emptyList(), listOf(Damaged(-1, null, "document format is not $FORMAT")))
        }
        val version = AlarmValues.int(document, KEY_VERSION)
            ?: return rescue(emptyList(), listOf(Damaged(-1, null, "document version is missing")))
        var current = document
        var migratedFrom: Int? = null
        var newerVersion: Int? = null
        when {
            version > VERSION -> newerVersion = version
            version < VERSION -> {
                var step = version
                while (step < VERSION) {
                    val migration = MIGRATIONS[step] ?: return rescue(emptyList(), listOf(Damaged(-1, null, "document version $version cannot be migrated")))
                    current = migration(current)
                    step++
                }
                migratedFrom = version
            }
        }
        // A newer document may carry options this version does not know: they are dropped instead of the whole record.
        // zh-CN: 更新的文档可能带有本版本不认识的选项: 丢弃这些选项, 而不是丢弃整条记录.
        val read: (Map<String, Any?>) -> AlarmSpec =
            if (newerVersion == null) validate else { options -> validate(options.filterKeys(AlarmSpec.KEYS::contains)) }
        val damaged = ArrayList<Damaged>()
        val records = ArrayList<AlarmRecord>()
        val ids = HashSet<String>()
        val entries = current[KEY_ALARMS] as? List<*>
        if (entries == null && current.containsKey(KEY_ALARMS)) damaged += Damaged(-1, null, "alarms is not an array")
        entries?.forEachIndexed { index, entry ->
            @Suppress("UNCHECKED_CAST")
            val values = (entry as? Map<*, *>)?.takeIf { map -> map.keys.all { it is String } } as Map<String, Any?>?
            if (values == null) {
                damaged += Damaged(index, null, "entry is not an object")
                return@forEachIndexed
            }
            val id = values[AlarmRecord.KEY_ID] as? String
            val record = try {
                AlarmRecord.fromValues(values, read)
            } catch (e: AlarmException) {
                damaged += Damaged(index, id, e.message ?: e.code)
                return@forEachIndexed
            } catch (e: IllegalArgumentException) {
                damaged += Damaged(index, id, AlarmException.of(e).message ?: "invalid record")
                return@forEachIndexed
            }
            when {
                !ids.add(record.id) -> damaged += Damaged(index, record.id, "duplicate id")
                records.size >= AlarmLimits.MAX_ALARMS -> damaged += Damaged(index, record.id, "more than ${AlarmLimits.MAX_ALARMS} alarms")
                else -> records += record
            }
        }
        val disabled = ArrayList<String>()
        var enabledCount = 0
        for (i in records.indices) {
            val record = records[i]
            if (!record.enabled) continue
            if (enabledCount < AlarmLimits.MAX_ENABLED_ALARMS) {
                enabledCount++
            } else {
                records[i] = record.copy(spec = record.spec.copy(enabled = false), state = record.state.copy(nextTrigger = null, skipAt = null, snoozeUntil = null, snoozeCount = 0))
                disabled += record.id
            }
        }
        val result = LoadResult(records, damaged, disabled, null, migratedFrom, newerVersion)
        if (result.clean) return result
        return if (damaged.isEmpty() && newerVersion == null && disabled.isEmpty()) {
            // A plain migration keeps no copy of the old version: it carried no data the new one lacks.
            // zh-CN: 单纯的迁移不保留旧版本副本: 旧版本没有新版本缺少的数据.
            save(records)
            result
        } else {
            rescue(records, damaged, disabled, migratedFrom, newerVersion)
        }
    }

    fun save(records: List<AlarmRecord>) {
        val text = AlarmJson.write(
            linkedMapOf(
                KEY_FORMAT to FORMAT,
                KEY_VERSION to VERSION,
                KEY_ALARMS to records.map(AlarmRecord::toValues),
            ),
        )
        writeAtomically(file, text.toByteArray(Charsets.UTF_8))
    }

    /** Copies the original to quarantine, then writes [records] back. zh-CN: 把原文件复制到隔离目录, 再写回 [records]. */
    private fun rescue(
        records: List<AlarmRecord>,
        damaged: List<Damaged>,
        disabled: List<String> = emptyList(),
        migratedFrom: Int? = null,
        newerVersion: Int? = null,
    ): LoadResult {
        val copy = quarantine()
        save(records)
        return LoadResult(records, damaged, disabled, copy, migratedFrom, newerVersion)
    }

    private fun quarantine(): File? {
        val source = file
        if (!source.isFile) return null
        val dir = quarantineDirectory
        if (!dir.isDirectory && !dir.mkdirs()) throw AlarmException.ioFailed("cannot create ${dir.name}")
        val stamp = clock().coerceAtLeast(0).toString().padStart(STAMP_DIGITS, '0')
        var target = File(dir, "$QUARANTINE_PREFIX$stamp$QUARANTINE_SUFFIX")
        var n = 1
        while (target.exists()) target = File(dir, "$QUARANTINE_PREFIX$stamp-${n++}$QUARANTINE_SUFFIX")
        try {
            source.copyTo(target)
        } catch (e: IOException) {
            throw AlarmException.ioFailed("cannot copy ${source.name} to quarantine: ${e.message}")
        }
        dir.listFiles { f -> f.isFile && f.name.startsWith(QUARANTINE_PREFIX) && f.name.endsWith(QUARANTINE_SUFFIX) }
            ?.sortedByDescending { it.name }
            ?.drop(MAX_QUARANTINE)
            ?.forEach { it.delete() }
        return target
    }

    private fun removeLeftoverTempFiles() {
        directory.listFiles { f -> f.isFile && f.name.startsWith(TEMP_PREFIX) && f.name.endsWith(TEMP_SUFFIX) }?.forEach { it.delete() }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        if (bytes.size > MAX_FILE_BYTES) throw AlarmException.limitExceeded("alarm document would exceed $MAX_FILE_BYTES bytes")
        val dir = target.parentFile ?: directory
        if (!dir.isDirectory && !dir.mkdirs()) throw AlarmException.ioFailed("cannot create ${dir.path}")
        val temp = File(dir, "$TEMP_PREFIX${UUID.randomUUID()}$TEMP_SUFFIX")
        try {
            FileOutputStream(temp).use { out ->
                out.write(bytes)
                out.flush()
                out.fd.sync()
            }
            if (!temp.renameTo(target)) {
                // Android replaces the target atomically; the JVM on Windows refuses, which only happens in unit tests.
                // zh-CN: Android 上改名会原子替换目标; Windows 上的 JVM 会拒绝, 这只出现在单元测试中.
                target.delete()
                if (!temp.renameTo(target)) throw IOException("cannot rename ${temp.name} to ${target.name}")
            }
            syncDirectory(dir)
        } catch (e: IOException) {
            temp.delete()
            throw AlarmException.ioFailed("cannot save ${target.name}: ${e.message}")
        }
    }

    /** Makes the rename durable where the platform allows it. zh-CN: 平台允许时让改名落盘. */
    private fun syncDirectory(dir: File) {
        try {
            RandomAccessFile(dir, "r").use { it.channel.force(true) }
        } catch (_: IOException) {
            // Directories cannot be opened on every platform (Windows); the file itself is already synced.
            // zh-CN: 并非所有平台都能打开目录 (Windows); 文件本身已同步.
        }
    }

    companion object {

        const val FILE_NAME = "alarms.json"
        const val FORMAT = "three-chime-alarm.alarms"
        const val VERSION = 1

        const val KEY_FORMAT = "format"
        const val KEY_VERSION = "version"
        const val KEY_ALARMS = "alarms"

        const val QUARANTINE_DIRECTORY = "quarantine"
        const val MAX_QUARANTINE = 5

        /** 300 full records stay far below this; a larger file is not a document this plugin wrote. zh-CN: 300 条完整记录远小于此值. */
        const val MAX_FILE_BYTES = 8L * 1024 * 1024

        private const val TEMP_PREFIX = ".alarms-"
        private const val TEMP_SUFFIX = ".tmp"
        private const val QUARANTINE_PREFIX = "alarms-"
        private const val QUARANTINE_SUFFIX = ".json"
        private const val STAMP_DIGITS = 13

        /** Document migrations by source version: entry `n` turns version `n` into `n + 1`. zh-CN: 按源版本登记的文档迁移. */
        val MIGRATIONS: Map<Int, (Map<String, Any?>) -> Map<String, Any?>> = emptyMap()

        /** The store in device-protected storage, readable before the first unlock (Direct Boot, T8). zh-CN: 设备加密存储中的存储, 首次解锁前可读. */
        fun of(context: Context): AlarmStore {
            val storage = context.createDeviceProtectedStorageContext()
            return AlarmStore(File(storage.filesDir, DIRECTORY))
        }

        const val DIRECTORY = "alarms"
    }
}
