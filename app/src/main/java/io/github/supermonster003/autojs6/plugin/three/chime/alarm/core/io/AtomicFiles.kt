package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.io

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID

/**
 * Whole-file writes that a crash cannot leave half done: the bytes go to a temporary file in the same directory,
 * which is synced and renamed over the target (the alarm list, the binding hints and the ringtone index).
 * zh-CN: 崩溃也不会只写一半的整文件写入: 字节先写进同目录的临时文件, 同步后改名覆盖目标 (闹钟列表, 绑定提示与铃声索引).
 */
object AtomicFiles {

    /** Writes [bytes] to [target]; the temporary file is `<prefix><uuid><suffix>`. zh-CN: 把 [bytes] 写入 [target]. */
    @Throws(IOException::class)
    fun write(target: File, bytes: ByteArray, prefix: String = ".", suffix: String = TEMP_SUFFIX) {
        val dir = target.absoluteFile.parentFile ?: throw IOException("no directory for ${target.name}")
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("cannot create ${dir.path}")
        val temp = File(dir, "$prefix${UUID.randomUUID()}$suffix")
        try {
            FileOutputStream(temp).use { out ->
                out.write(bytes)
                out.flush()
                out.fd.sync()
            }
            replace(temp, target)
        } catch (e: IOException) {
            temp.delete()
            throw e
        }
    }

    /** Renames [source] over [target]. zh-CN: 把 [source] 改名覆盖 [target]. */
    @Throws(IOException::class)
    fun replace(source: File, target: File) {
        if (!source.renameTo(target)) {
            // Android replaces the target atomically; the JVM on Windows refuses, which only happens in unit tests.
            // zh-CN: Android 上改名会原子替换目标; Windows 上的 JVM 会拒绝, 这只出现在单元测试中.
            target.delete()
            if (!source.renameTo(target)) throw IOException("cannot rename ${source.name} to ${target.name}")
        }
        syncDirectory(target.absoluteFile.parentFile)
    }

    /** Makes a rename durable where the platform allows it. zh-CN: 平台允许时让改名落盘. */
    fun syncDirectory(dir: File?) {
        if (dir == null) return
        try {
            RandomAccessFile(dir, "r").use { it.channel.force(true) }
        } catch (_: IOException) {
            // Directories cannot be opened on every platform (Windows); the file itself is already synced.
            // zh-CN: 并非所有平台都能打开目录 (Windows); 文件本身已同步.
        }
    }

    const val TEMP_SUFFIX = ".tmp"
}
