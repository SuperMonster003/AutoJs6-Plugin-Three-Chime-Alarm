package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import java.io.File

/**
 * The uris of imported ringtones (roadmap P2.4, P2.5, T70): `importRingtone` copies a file into [DIRECTORY] under
 * device-protected `filesDir` (playable before the first unlock, T7) and answers `chime-ringtone://imported/<id>` as the
 * `sound.source` of alarms. Ids are plain names, so a uri never reaches outside the directory.
 * zh-CN: 导入铃声的 uri (路线图 P2.4, P2.5, T70): `importRingtone` 把文件复制到设备加密存储 `filesDir` 下的 [DIRECTORY] (首次解锁前可播放, T7),
 * 返回 `chime-ringtone://imported/<id>` 作为闹钟的 `sound.source`. id 只是普通名称, 因此 uri 不会指向目录之外.
 */
object ImportedRingtones {

    const val SCHEME = "chime-ringtone"

    const val AUTHORITY = "imported"

    /** The directory of the copies under device-protected `filesDir`. zh-CN: 副本在设备加密存储 `filesDir` 下的目录. */
    const val DIRECTORY = "ringtones"

    private const val PREFIX = "$SCHEME://$AUTHORITY/"

    private val ID = Regex("[A-Za-z0-9_-]{1,64}")

    fun uriOf(id: String): String {
        require(ID.matches(id)) { "invalid ringtone id '$id'" }
        return PREFIX + id
    }

    /** Whether [source] names an imported ringtone (valid or not). zh-CN: [source] 是否指向导入铃声 (无论是否有效). */
    fun isImported(source: String): Boolean = source.startsWith("$SCHEME:")

    /** The id of an imported ringtone uri, null for any other or a malformed one. zh-CN: 导入铃声 uri 的 id, 其他或格式错误的 uri 为 null. */
    fun idOf(source: String): String? = source.takeIf { it.startsWith(PREFIX) }?.substring(PREFIX.length)?.takeIf(ID::matches)

    /** The copy of [source] under [filesDir], null when [source] is not a valid imported uri. zh-CN: [source] 在 [filesDir] 下的副本. */
    fun fileOf(filesDir: File, source: String): File? = idOf(source)?.let { File(File(filesDir, DIRECTORY), it) }
}
