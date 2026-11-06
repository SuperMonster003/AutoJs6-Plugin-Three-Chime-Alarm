package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.assertAlarmError
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.astral
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes.IO_FAILED
import org.autojs.plugin.alarm.api.AlarmErrorCodes.LIMIT_EXCEEDED
import org.autojs.plugin.alarm.api.AlarmLimits
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Roadmap P2.5 (T15, T72): imported ringtones are private copies that outlive the original, deduplicated by content,
 * limited in size and count, and checked for decodable audio.
 * zh-CN: 路线图 P2.5 (T15, T72): 导入的铃声是比原文件存活更久的私有副本, 按内容去重, 限制大小与数量, 并检查是否为可解码的音频.
 */
class RingtoneLibraryTest {

    @get:Rule
    val temp = TemporaryFolder()

    private var now = 1_000L

    private val verified = ArrayList<String>()

    private fun library(maxBytes: Long = 64) = RingtoneLibrary(temp.root, { file ->
        verified += file.name
        if (file.readBytes().first() == AUDIO) "audio/ogg" else null
    }, { now }, maxBytes)

    private fun audio(text: String) = byteArrayOf(AUDIO) + text.toByteArray()

    private fun leftovers(dir: File) = dir.listFiles().orEmpty().filter { it.name.startsWith(".") }

    @Test
    fun `an import is a private copy that outlives the original`() {
        val original = temp.newFile("Wake Up.ogg").apply { writeBytes(audio("bells")) }
        val library = library()
        val ringtone = original.inputStream().use { library.import(it, original.absolutePath) }
        assertTrue(original.delete())

        assertEquals("Wake Up", ringtone.title)
        assertEquals(AlarmContract.RINGTONE_IMPORTED, ringtone.source)
        assertFalse(ringtone.isDefault)
        val copy = ImportedRingtones.fileOf(temp.root, ringtone.uri)!!
        assertArrayEquals(audio("bells"), copy.readBytes())
        assertEquals(RingtoneLibrary.ID_CHARS, copy.name.length)
        assertEquals(listOf(ringtone), library.list())
        assertEquals("a new process lists the same title", listOf(ringtone), library().list())
        assertTrue(leftovers(library.directory).isEmpty())
    }

    @Test
    fun `the same content is stored once and keeps its first title`() {
        val library = library()
        val first = library.import(ByteArrayInputStream(audio("same")), "first.mp3")
        now += 1
        val second = library.import(ByteArrayInputStream(audio("same")), "second.mp3")
        assertEquals(first, second)
        assertEquals(1, verified.size)
        now += 1
        val other = library.import(ByteArrayInputStream(audio("other")), "other.mp3")
        assertEquals(listOf(first, other), library.list())
    }

    @Test
    fun `size, content and count limits`() {
        val library = library()
        assertAlarmError(LIMIT_EXCEEDED, "64") { library.import(ByteArrayInputStream(ByteArray(65) { AUDIO }), "big.mp3") }
        library.import(ByteArrayInputStream(ByteArray(64) { AUDIO }), "edge.mp3")
        assertAlarmError(IO_FAILED, "empty") { library.import(ByteArrayInputStream(ByteArray(0)), "empty.mp3") }
        assertAlarmError(IO_FAILED, "decoded") { library.import(ByteArrayInputStream("text".toByteArray()), "text.mp3") }
        assertEquals(1, library.list().size)
        assertTrue(leftovers(library.directory).isEmpty())

        for (i in 1 until AlarmLimits.MAX_RINGTONES) library.import(ByteArrayInputStream(audio("tone $i")), "tone $i.mp3")
        assertEquals(AlarmLimits.MAX_RINGTONES, library.list().size)
        assertAlarmError(LIMIT_EXCEEDED, "${AlarmLimits.MAX_RINGTONES}") { library.import(ByteArrayInputStream(audio("one more")), "more.mp3") }
        assertEquals("a stored file is still answered at the limit", "tone 1", library.import(ByteArrayInputStream(audio("tone 1")), "x.mp3").title)
    }

    @Test
    fun `a stale temporary file is cleaned and an unindexed copy is listed by its id`() {
        val library = library()
        library.directory.mkdirs()
        File(library.directory, ".import-1.tmp").writeBytes(ByteArray(3))
        val ringtone = library.import(ByteArrayInputStream(audio("a")), "a.wav")
        assertTrue(leftovers(library.directory).isEmpty())
        File(library.directory, RingtoneLibrary.INDEX_FILE).writeText("[broken", Charsets.UTF_8)
        val id = ImportedRingtones.idOf(ringtone.uri)!!
        assertEquals(listOf(id), library.list().map { it.title })
    }

    @Test
    fun `a deleted ringtone leaves the list and can be imported again`() {
        val library = library()
        val first = library.import(ByteArrayInputStream(audio("one")), "one.ogg")
        now += 1
        val second = library.import(ByteArrayInputStream(audio("two")), "two.ogg")

        assertTrue(library.delete(first.uri))
        assertFalse(ImportedRingtones.fileOf(temp.root, first.uri)!!.exists())
        assertEquals(listOf(second), library.list())
        assertEquals("the title leaves the index", listOf(second), library().list())
        assertFalse("deleting again", library.delete(first.uri))
        assertFalse("not an imported ringtone", library.delete("content://media/internal/audio/media/1"))
        assertFalse("outside the directory", library.delete("chime-ringtone://imported/../index.json"))
        assertFalse("the index is no ringtone", library.delete("chime-ringtone://imported/index"))
        assertTrue(File(library.directory, RingtoneLibrary.INDEX_FILE).isFile)

        now += 1
        val again = library.import(ByteArrayInputStream(audio("one")), "one again.ogg")
        assertEquals("the same content has the same uri", first.uri, again.uri)
        assertEquals("one again", again.title)
        assertEquals(listOf(second, again), library.list())
        assertTrue(leftovers(library.directory).isEmpty())
    }

    @Test
    fun `titles drop the path and the extension and fit the label limit`() {
        assertEquals("Morning", RingtoneLibrary.titleOf("/sdcard/Music/Morning.mp3"))
        assertEquals("Morning", RingtoneLibrary.titleOf("C:\\Music\\Morning.mp3"))
        assertEquals("song.final", RingtoneLibrary.titleOf("song.final.ogg"))
        assertEquals(".ogg", RingtoneLibrary.titleOf(".ogg"))
        assertEquals(RingtoneLibrary.DEFAULT_TITLE, RingtoneLibrary.titleOf("  "))
        assertEquals(RingtoneLibrary.DEFAULT_TITLE, RingtoneLibrary.titleOf(null))
        val long = astral(AlarmLimits.MAX_LABEL_CHARS + 5)
        val title = RingtoneLibrary.titleOf("$long.mp3")
        assertEquals(AlarmLimits.MAX_LABEL_CHARS, title.codePointCount(0, title.length))
    }

    private companion object {
        const val AUDIO: Byte = 0x4F
    }
}
