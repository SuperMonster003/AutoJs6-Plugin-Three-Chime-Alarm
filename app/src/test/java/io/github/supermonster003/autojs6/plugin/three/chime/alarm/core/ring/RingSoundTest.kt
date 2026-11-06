package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.optionsOf
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.SpeechDirector.Sound.DUCKED
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.SpeechDirector.Sound.FULL
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.SpeechDirector.Sound.OFF
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.SpeechDirector.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Roadmap P2.4 (T14, T61 - T63, T68, T70): the ring settings of stored options, speech timing in the four modes, the
 * independent volume and its restore, the fade in, the uris of imported ringtones.
 * zh-CN: 路线图 P2.4 (T14, T61 - T63, T68, T70): 已存选项的响铃设置, 四种 mode 的朗读时序, 独立音量及其恢复, 渐强, 导入铃声的 uri.
 */
class RingSoundTest {

    private val validator = AlarmOptionsValidator()

    private fun settings(vararg extra: Pair<String, Any?>) = RingSettings.of(validator.options(optionsOf(7, 0, *extra)))

    @Test
    fun `ring settings apply the plugin defaults`() {
        val plain = settings()
        assertEquals(RingSettings(), plain)
        assertEquals(listOf(0L, 600L, 400L), plain.vibration)
        assertEquals("default", plain.soundSource(false))
        assertEquals(10 * MINUTE, plain.autoSilenceMillis)
        assertEquals("after", settings("speak" to mapOf("text" to "hi")).speakMode)

        assertNull(settings("vibrate" to false).vibration)
        assertEquals(RingSettings.DEFAULT_VIBRATION, settings("vibrate" to true).vibration)
        assertEquals(listOf(0L, 100L, 50L), settings("vibratePattern" to longArrayOf(0, 100, 50)).vibration)

        val custom = settings(
            "sound" to mapOf("source" to "content://media/internal/audio/media/7", "volume" to 0.4, "rampSeconds" to 30),
            "snooze" to mapOf("minutes" to 5, "max" to 2),
            "autoSilenceMinutes" to 3,
        )
        assertEquals("content://media/internal/audio/media/7", custom.source)
        assertEquals(0.4, custom.volume!!, 0.0)
        assertEquals(30, custom.rampSeconds)
        assertEquals(Triple(true, 5, 2), Triple(custom.snoozeEnabled, custom.snoozeMinutes, custom.snoozeMax))
        assertEquals(3 * MINUTE, custom.autoSilenceMillis)
        assertEquals(false, settings("snooze" to mapOf("enabled" to false)).snoozeEnabled)
    }

    @Test
    fun `options an alarm leaves out follow the settings defaults (T84)`() {
        val defaults = RingDefaults(sound = "silent", vibrate = false, snoozeEnabled = true, snoozeMinutes = 7, snoozeMax = 5, autoSilenceMinutes = 4)
        fun of(vararg extra: Pair<String, Any?>) = RingSettings.of(validator.options(optionsOf(7, 0, *extra)), defaults)

        val plain = of()
        assertEquals("silent", plain.source)
        assertNull(plain.vibration)
        assertEquals(Triple(true, 7, 5), Triple(plain.snoozeEnabled, plain.snoozeMinutes, plain.snoozeMax))
        assertEquals(4 * MINUTE, plain.autoSilenceMillis)

        // What the alarm sets wins, field by field. zh-CN: 闹钟自己设置的值逐项优先.
        assertEquals(RingSettings.DEFAULT_VIBRATION, of("vibrate" to true).vibration)
        val partial = of("snooze" to mapOf("minutes" to 3), "sound" to mapOf("source" to "default"), "autoSilenceMinutes" to 20)
        assertEquals("default", partial.source)
        assertEquals(Triple(true, 3, 5), Triple(partial.snoozeEnabled, partial.snoozeMinutes, partial.snoozeMax))
        assertEquals(20 * MINUTE, partial.autoSilenceMillis)
        assertFalse(RingSettings.of(validator.options(optionsOf(7, 0)), defaults.copy(snoozeEnabled = false)).snoozeEnabled)
        assertEquals(true, of("snooze" to mapOf("enabled" to true)).snoozeEnabled)
    }

    @Test
    fun `stored defaults are clamped to the protocol limits (T84)`() {
        val wild = RingDefaults(sound = " ", snoozeMinutes = 0, snoozeMax = 99, autoSilenceMinutes = 600).sanitized()
        assertEquals(RingDefaults.PROTOCOL.sound, wild.sound)
        assertEquals(Triple(1, 10, 60), Triple(wild.snoozeMinutes, wild.snoozeMax, wild.autoSilenceMinutes))
        assertEquals(RingDefaults.PROTOCOL, RingDefaults.PROTOCOL.sanitized())
        assertEquals(30, RingDefaults(autoSilenceMinutes = 5).autoSilenceFor(challenge = true))
        assertEquals(45, RingDefaults(autoSilenceMinutes = 45).autoSilenceFor(challenge = true))
        assertEquals(5, RingDefaults(autoSilenceMinutes = 5).autoSilenceFor(challenge = false))
    }

    @Test
    fun `a ring without sound of its own plays one when speech fails`() {
        val silent = settings("sound" to mapOf("source" to "silent"), "speak" to mapOf("text" to "hi"))
        assertNull(silent.soundSource(false))
        assertEquals("default", silent.soundSource(true))
        val only = settings("sound" to mapOf("source" to "content://x/1"), "speak" to mapOf("text" to "hi", "mode" to "only"))
        assertNull(only.soundSource(false))
        assertEquals("content://x/1", only.soundSource(true))
        assertNull(settings("sound" to mapOf("source" to "silent")).soundSource(false))
    }

    @Test
    fun `after plays first, then lowers the sound while speaking`() {
        val once = SpeechDirector("after", 0)
        assertEquals(Step(FULL, wakeAt = 5_000), once.start(0))
        assertEquals(Step(FULL, wakeAt = 5_000), once.due(4_999))
        assertEquals(Step(DUCKED, speak = true), once.due(5_000))
        assertEquals(Step(DUCKED, speak = true), once.due(6_000))
        assertEquals(Step(FULL), once.spoken(8_000))
        assertEquals(Step(FULL), once.due(60_000))

        val repeated = SpeechDirector("after", 30)
        repeated.start(0)
        repeated.due(5_000)
        assertEquals(Step(FULL, wakeAt = 38_000), repeated.spoken(8_000))
        assertEquals(Step(DUCKED, speak = true), repeated.due(38_000))
    }

    @Test
    fun `before speaks first without sound, interleave alternates, only never plays`() {
        val before = SpeechDirector("before", 20)
        assertEquals(Step(OFF, speak = true), before.start(0))
        assertEquals(Step(FULL, wakeAt = 23_000), before.spoken(3_000))
        assertEquals(Step(DUCKED, speak = true), before.due(23_000))
        assertEquals(Step(FULL), SpeechDirector("before", 0).run { start(0); spoken(3_000) })

        val interleave = SpeechDirector("interleave", 0)
        assertEquals(Step(FULL, wakeAt = 10_000), interleave.start(0))
        assertEquals(Step(OFF, speak = true), interleave.due(10_000))
        assertEquals(Step(FULL, wakeAt = 22_000), interleave.spoken(12_000))
        assertEquals(Step(OFF, speak = true), interleave.due(22_000))
        assertEquals(Step(FULL, wakeAt = 15_000), SpeechDirector("interleave", 15).start(0))

        val only = SpeechDirector("only", 0)
        assertEquals(Step(OFF, speak = true), only.start(0))
        assertEquals(Step(OFF), only.spoken(3_000))
        val onlyRepeated = SpeechDirector("only", 60)
        onlyRepeated.start(0)
        assertEquals(Step(OFF, wakeAt = 63_000), onlyRepeated.spoken(3_000))
        assertEquals(Step(OFF, speak = true), onlyRepeated.due(63_000))
    }

    @Test
    fun `a failed engine leaves the full sound and no more speech`() {
        val director = SpeechDirector("interleave", 0)
        director.start(0)
        director.due(10_000)
        assertEquals(Step(FULL), director.unavailable())
        assertEquals(Step(FULL), director.due(20_000))
        assertEquals(Step(FULL), director.spoken(21_000))
        assertEquals(Step(FULL), SpeechDirector("only", 0).run { start(0); unavailable() })
    }

    @Test
    fun `the independent volume is borrowed once and given back`() {
        val stream = FakeStream(min = 1, max = 7, index = 3)
        val memory = FakeMemory()
        val volume = RingVolume(stream, memory)

        volume.apply(1.0)
        assertEquals(7, stream.index)
        assertEquals(3, memory.saved)
        // A queued ring with its own volume does not take the borrowed volume for the user's. zh-CN: 排队的响铃不会把借用的音量当成用户的音量.
        volume.apply(0.5)
        assertEquals(4, stream.index)
        assertEquals(3, memory.saved)
        // A ring without a volume plays at the user's volume. zh-CN: 不带音量的响铃使用用户的音量.
        volume.apply(null)
        assertEquals(3, stream.index)
        assertNull(memory.saved)
        volume.restore()
        assertEquals(3, stream.index)

        // A process that died while ringing gives the volume back on the next start. zh-CN: 响铃中死亡的进程在下次启动时归还音量.
        volume.apply(0.0)
        assertEquals(1, stream.index)
        val restarted = RingVolume(FakeStream(min = 1, max = 7, index = 1), memory)
        restarted.apply(0.8)
        assertEquals(3, memory.saved)
        restarted.restore()
        assertNull(memory.saved)

        assertEquals(listOf(0, 4, 8, 11, 15), listOf(0.0, 0.25, 0.5, 0.75, 1.0).map { RingVolume.indexOf(it, 0, 15) })
        assertEquals(15, RingVolume.indexOf(3.0, 0, 15))
        assertEquals(0f, RingVolume.playerGain(0.0))
        assertEquals(1f, RingVolume.playerGain(0.3))
        assertEquals(1f, RingVolume.playerGain(null))
    }

    @Test
    fun `the fade in grows with the square of the time`() {
        assertEquals(1f, RingVolume.rampGain(0, 0))
        assertEquals(0.01f, RingVolume.rampGain(0, 30))
        assertEquals(0.25f, RingVolume.rampGain(15_000, 30), 1e-6f)
        assertEquals(1f, RingVolume.rampGain(30_000, 30))
        assertEquals(1f, RingVolume.rampGain(90_000, 30))
    }

    @Test
    fun `imported ringtone uris stay inside their directory`() {
        val uri = ImportedRingtones.uriOf("a1_B-2")
        assertEquals("chime-ringtone://imported/a1_B-2", uri)
        assertTrue(ImportedRingtones.isImported(uri))
        assertEquals("a1_B-2", ImportedRingtones.idOf(uri))
        val filesDir = File("files")
        assertEquals(File(File(filesDir, "ringtones"), "a1_B-2"), ImportedRingtones.fileOf(filesDir, uri))

        assertFalse(ImportedRingtones.isImported("content://media/internal/audio/media/7"))
        assertNull(ImportedRingtones.idOf("content://media/internal/audio/media/7"))
        for (bad in listOf("chime-ringtone://imported/../x", "chime-ringtone://imported/a/b", "chime-ringtone://imported/", "chime-ringtone://other/a")) {
            assertTrue(bad, ImportedRingtones.isImported(bad))
            assertNull(bad, ImportedRingtones.fileOf(filesDir, bad))
        }
        for (bad in listOf("", "../x", "a b", "x".repeat(65))) {
            assertThrows(IllegalArgumentException::class.java) { ImportedRingtones.uriOf(bad) }
        }
    }

    private class FakeStream(override val min: Int, override val max: Int, override var index: Int) : RingVolume.Stream

    private class FakeMemory : RingVolume.Memory {
        override var saved: Int? = null
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
