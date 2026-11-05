package io.github.supermonster003.autojs6.plugin.three.chime.alarm.service

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.RingtoneSnapshot
import java.io.File

/**
 * The system alarm ringtones of `listRingtones` (roadmap P2.5, T74): the alarm sounds of [RingtoneManager] in its own
 * order, the current default alarm sound marked `default` (put first when the list lacks it). A failing query answers
 * an empty list, so the imported ringtones are still listed.
 * zh-CN: `listRingtones` 的系统闹钟铃声 (路线图 P2.5, T74): [RingtoneManager] 的闹钟铃声, 保持其顺序, 当前默认闹钟铃声标为 `default` (列表中没有时放在
 * 最前). 查询失败时返回空列表, 导入的铃声仍会列出.
 */
object SystemRingtones {

    fun list(context: Context): List<RingtoneSnapshot> = try {
        val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val default = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        val defaultKey = default?.let(::keyOf)
        val result = ArrayList<RingtoneSnapshot>()
        manager.cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val uri = manager.getRingtoneUri(cursor.position) ?: continue
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX).orEmpty()
                result += RingtoneSnapshot(uri.toString(), title, AlarmContract.RINGTONE_SYSTEM, defaultKey != null && keyOf(uri) == defaultKey)
            }
        }
        if (default != null && result.none { it.isDefault }) {
            val title = runCatching { RingtoneManager.getRingtone(context, default)?.getTitle(context) }.getOrNull().orEmpty()
            result.add(0, RingtoneSnapshot(default.toString(), title, AlarmContract.RINGTONE_SYSTEM, true))
        }
        result
    } catch (e: RuntimeException) {
        Log.w(TAG, "listRingtones: system ringtones unavailable: ${e.javaClass.simpleName}")
        emptyList()
    }

    /** The uri without its query (`?title=...`), as the default setting stores it. zh-CN: 去掉查询部分的 uri. */
    private fun keyOf(uri: Uri): String = uri.buildUpon().clearQuery().build().toString()

    private const val TAG = "ChimeService"
}

/**
 * The decoding check of imported ringtones (roadmap P2.5, T72): the MIME type of the first audio track
 * [MediaExtractor] finds in the copy, null when it finds none or cannot read the file.
 * zh-CN: 导入铃声的解码检查 (路线图 P2.5, T72): [MediaExtractor] 在副本中找到的第一条音轨的 MIME 类型, 找不到或无法读取时为 null.
 */
object AudioVerifier {

    fun mimeOf(file: File): String? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            (0 until extractor.trackCount).firstNotNullOfOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.takeIf { it.startsWith("audio/") }
            }
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }
}
