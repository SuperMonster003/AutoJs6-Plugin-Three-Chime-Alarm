package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.io.AtomicFiles
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmJson
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmValues
import org.autojs.plugin.alarm.api.RingtoneSnapshot
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/**
 * The imported ringtones (roadmap P2.5, T15, T72): `importRingtone` copies the host's read-only file into
 * [ImportedRingtones.DIRECTORY] under device-protected `filesDir`, so the alarm keeps ringing after the original is
 * deleted and before the first unlock. A copy is accepted when it has 1 byte to 20 MiB (more is `LIMIT_EXCEEDED`, the
 * code of the host's own size check) and [verify] finds an audio track (otherwise `IO_FAILED`). The id is the start of
 * the SHA-256 of the content, so importing the same file again answers the stored ringtone with its first title; at
 * most [AlarmLimits.MAX_RINGTONES] different files are kept. The titles live in [INDEX_FILE].
 *
 * zh-CN: 导入的铃声 (路线图 P2.5, T15, T72): `importRingtone` 把宿主给出的只读文件复制到设备加密存储 `filesDir` 下的 [ImportedRingtones.DIRECTORY],
 * 因此原文件删除后以及首次解锁前闹钟仍能响. 副本需为 1 字节至 20 MiB (更大报 `LIMIT_EXCEEDED`, 与宿主自身的大小检查一致), 且 [verify] 能找到音轨
 * (否则报 `IO_FAILED`). id 取内容 SHA-256 的前段, 因此再次导入同一文件时返回已保存的铃声及其首次的标题; 至多保留 [AlarmLimits.MAX_RINGTONES] 个不同文件.
 * 标题保存在 [INDEX_FILE].
 */
class RingtoneLibrary(
    filesDir: File,
    private val verify: (File) -> String?,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxBytes: Long = AlarmLimits.MAX_RINGTONE_BYTES,
) {

    val directory = File(filesDir, ImportedRingtones.DIRECTORY)

    private val indexFile = File(directory, INDEX_FILE)

    private val lock = Any()

    private data class Entry(val title: String, val mime: String?, val size: Long, val importedAt: Long)

    /**
     * Copies [input] (closed by the caller) and answers its ringtone; [displayName] becomes the title without its
     * extension, `ringtone` when blank.
     * zh-CN: 复制 [input] (由调用方关闭) 并返回其铃声; [displayName] 去掉扩展名后作为标题, 为空时用 `ringtone`.
     */
    fun import(input: InputStream, displayName: String?): RingtoneSnapshot = synchronized(lock) {
        if (!directory.isDirectory && !directory.mkdirs()) throw AlarmException.ioFailed("cannot create the ringtone directory")
        directory.listFiles { f -> f.isFile && f.name.startsWith(".") && f.name.endsWith(AtomicFiles.TEMP_SUFFIX) }?.forEach { it.delete() }
        val temp = File(directory, "$TEMP_PREFIX${clock()}${AtomicFiles.TEMP_SUFFIX}")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            temp.outputStream().use { out ->
                val buffer = ByteArray(BUFFER_BYTES)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maxBytes) throw AlarmException.limitExceeded("a ringtone file is at most ${sizeText(maxBytes)}")
                    digest.update(buffer, 0, read)
                    out.write(buffer, 0, read)
                }
                out.flush()
                out.fd.sync()
            }
            if (total == 0L) throw AlarmException.ioFailed("the ringtone file is empty")
            val id = digest.digest().joinToString("") { "%02x".format(it) }.substring(0, ID_CHARS)
            val index = loadIndex()
            val target = File(directory, id)
            if (target.isFile) {
                val entry = index[id] ?: Entry(id, null, target.length(), target.lastModified())
                return snapshotOf(id, entry)
            }
            if (stored().size >= AlarmLimits.MAX_RINGTONES) throw AlarmException.limitExceeded("at most ${AlarmLimits.MAX_RINGTONES} imported ringtones")
            val mime = verify(temp) ?: throw AlarmException.ioFailed("the file is not audio that can be decoded")
            AtomicFiles.replace(temp, target)
            val entry = Entry(titleOf(displayName), mime, total, clock())
            index[id] = entry
            try {
                saveIndex(index)
            } catch (e: IOException) {
                target.delete()
                throw e
            }
            snapshotOf(id, entry)
        } catch (e: IOException) {
            throw AlarmException.ioFailed("cannot copy the ringtone: ${e.message}")
        } finally {
            temp.delete()
        }
    }

    /** The imported ringtones, oldest first. zh-CN: 导入的铃声, 最早的在前. */
    fun list(): List<RingtoneSnapshot> = synchronized(lock) {
        val index = loadIndex()
        stored()
            .map { file -> file.name to (index[file.name] ?: Entry(file.name, null, file.length(), file.lastModified())) }
            .sortedWith(compareBy({ it.second.importedAt }, { it.first }))
            .map { (id, entry) -> snapshotOf(id, entry) }
    }

    /**
     * Deletes the imported ringtone [uri] from the ringtone page (roadmap P3.3, T85): the copy first, then its title; false
     * when [uri] names no stored copy. Alarms that still name it ring the default alarm sound (T70). A title left behind
     * by a failed index write is harmless: only stored copies are listed, and importing the content again writes a new
     * title.
     * zh-CN: 在铃声页删除导入的铃声 [uri] (路线图 P3.3, T85): 先删副本, 再删标题; [uri] 没有对应的副本时返回 false. 仍指向它的闹钟改响默认闹钟铃声 (T70).
     * 索引写入失败而残留的标题无害: 列表只列存在的副本, 再次导入同一内容时会写入新标题.
     */
    fun delete(uri: String): Boolean = synchronized(lock) {
        val id = ImportedRingtones.idOf(uri) ?: return false
        val file = File(directory, id)
        if (!file.isFile) return false
        if (!file.delete()) throw AlarmException.ioFailed("cannot delete the ringtone")
        val index = loadIndex()
        if (index.remove(id) != null) {
            try {
                saveIndex(index)
            } catch (_: IOException) {
                // zh-CN: 见上文, 残留的标题无害.
            }
        }
        true
    }

    private fun stored(): List<File> = directory.listFiles { f -> f.isFile && ImportedRingtones.isId(f.name) }?.toList().orEmpty()

    private fun snapshotOf(id: String, entry: Entry) = RingtoneSnapshot(ImportedRingtones.uriOf(id), entry.title, AlarmContract.RINGTONE_IMPORTED, false)

    private fun loadIndex(): LinkedHashMap<String, Entry> {
        val map = LinkedHashMap<String, Entry>()
        try {
            if (!indexFile.isFile) return map
            val document = AlarmJson.parse(indexFile.readText(Charsets.UTF_8)) ?: return map
            for (row in AlarmValues.mapList(document, KEY_RINGTONES).orEmpty()) {
                val id = AlarmValues.string(row, KEY_ID)?.takeIf(ImportedRingtones::isId) ?: continue
                map[id] = Entry(
                    title = AlarmValues.string(row, KEY_TITLE) ?: id,
                    mime = AlarmValues.string(row, KEY_MIME),
                    size = AlarmValues.long(row, KEY_SIZE) ?: 0L,
                    importedAt = AlarmValues.long(row, KEY_IMPORTED_AT) ?: 0L,
                )
            }
        } catch (_: IOException) {
            // zh-CN: 读不出的索引视为空, 文件仍以 id 作标题列出.
        } catch (_: RuntimeException) {
            // zh-CN: 格式错误的索引同样视为空.
        }
        return map
    }

    @Throws(IOException::class)
    private fun saveIndex(index: Map<String, Entry>) {
        val document = linkedMapOf(
            KEY_FORMAT to FORMAT,
            KEY_VERSION to VERSION,
            KEY_RINGTONES to index.map { (id, entry) ->
                LinkedHashMap<String, Any?>().apply {
                    put(KEY_ID, id)
                    put(KEY_TITLE, entry.title)
                    entry.mime?.let { put(KEY_MIME, it) }
                    put(KEY_SIZE, entry.size)
                    put(KEY_IMPORTED_AT, entry.importedAt)
                }
            },
        )
        AtomicFiles.write(indexFile, AlarmJson.write(document).toByteArray(Charsets.UTF_8), ".$INDEX_FILE-")
    }

    companion object {

        const val INDEX_FILE = "index.json"

        /** Hex characters of the id (128 bits of the digest). zh-CN: id 的十六进制字符数 (摘要的 128 位). */
        const val ID_CHARS = 32

        const val DEFAULT_TITLE = "ringtone"

        private const val FORMAT = "three-chime-alarm.ringtones"
        private const val VERSION = 1
        private const val KEY_FORMAT = "format"
        private const val KEY_VERSION = "version"
        private const val KEY_RINGTONES = "ringtones"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_MIME = "mime"
        private const val KEY_SIZE = "size"
        private const val KEY_IMPORTED_AT = "importedAt"
        private const val TEMP_PREFIX = ".import-"
        private const val BUFFER_BYTES = 64 * 1024
        private const val MIB = 1024L * 1024

        /** `20 MiB` for whole MiB, otherwise the bytes. zh-CN: 整 MiB 时写 `20 MiB`, 否则写字节数. */
        private fun sizeText(bytes: Long): String = if (bytes >= MIB && bytes % MIB == 0L) "${bytes / MIB} MiB" else "$bytes bytes"

        /**
         * The title of [displayName]: the last path segment without its extension, at most 100 characters (code points).
         * zh-CN: [displayName] 的标题: 最后一个路径段去掉扩展名, 至多 100 个字符 (码点).
         */
        fun titleOf(displayName: String?): String {
            val name = displayName.orEmpty().substringAfterLast('/').substringAfterLast('\\').trim()
            val dot = name.lastIndexOf('.')
            val base = (if (dot > 0) name.substring(0, dot) else name).trim()
            if (base.isEmpty()) return DEFAULT_TITLE
            val points = base.codePointCount(0, base.length)
            return if (points <= AlarmLimits.MAX_LABEL_CHARS) base else base.substring(0, base.offsetByCodePoints(0, AlarmLimits.MAX_LABEL_CHARS))
        }
    }
}
