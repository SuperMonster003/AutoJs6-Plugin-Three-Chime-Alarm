package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.appearance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap Q21: the remembered host appearance that notifications and the first frame of every screen use in a new
 * process, before the host provider answers (or before the first unlock, when it cannot).
 * zh-CN: 路线图 Q21: 通知与每个界面的首帧在新进程中, 宿主 Provider 应答之前 (或首次解锁前无法应答时) 使用的记住的宿主外观.
 */
class HostAppearanceMemoryTest {

    private val sample = HostAppearance("zh-Hans-CN", dark = true, primary = 0xFF1E88E5.toInt(), accent = 0xFFFFB300.toInt())

    @Test
    fun `a remembered appearance comes back unchanged`() {
        assertEquals(sample, HostAppearanceReader.recall(HostAppearanceReader.remember(sample)))
        val light = sample.copy(language = "en", dark = false, primary = 0xFF000000.toInt(), accent = -1)
        assertEquals(light, HostAppearanceReader.recall(HostAppearanceReader.remember(light)))
    }

    @Test
    fun `colors are stored as text and read back opaque`() {
        val stored = HostAppearanceReader.remember(sample)
        assertEquals("ff1e88e5", stored["primary"])
        val translucent = stored + ("accent" to "33ffb300")
        assertEquals(0xFFFFB300.toInt(), HostAppearanceReader.recall(translucent)?.accent)
    }

    @Test
    fun `nothing valid stored recalls nothing`() {
        val stored = HostAppearanceReader.remember(sample)
        assertNull("empty file", HostAppearanceReader.recall(emptyMap<String, Any>()))
        assertNull("no dark flag", HostAppearanceReader.recall(stored - "dark"))
        assertNull("no accent", HostAppearanceReader.recall(stored - "accent"))
        assertNull("not a color", HostAppearanceReader.recall(stored + ("primary" to "blue")))
        assertNull("not a language tag", HostAppearanceReader.recall(stored + ("language" to "zh_CN!")))
        assertNull("wrong type", HostAppearanceReader.recall(stored + ("dark" to "true")))
    }

    @Test
    fun `a missing host replaces the known state only once the user unlocked`() {
        assertFalse("before the first unlock the provider cannot answer", HostAppearanceReader.replaces(true, sample, null, unlocked = false))
        assertFalse("nothing known yet, still locked", HostAppearanceReader.replaces(false, null, null, unlocked = false))
        assertTrue("unlocked: AutoJs6 is really missing", HostAppearanceReader.replaces(true, sample, null, unlocked = true))
        assertTrue("unlocked, nothing known yet", HostAppearanceReader.replaces(false, null, null, unlocked = true))
    }

    @Test
    fun `an answer replaces the known state when it differs`() {
        val other = sample.copy(language = "ja")
        assertTrue(HostAppearanceReader.replaces(true, sample, other, unlocked = false))
        assertTrue(HostAppearanceReader.replaces(false, null, sample, unlocked = true))
        assertTrue("a host that came back", HostAppearanceReader.replaces(true, null, sample, unlocked = true))
        assertFalse("unchanged: no write", HostAppearanceReader.replaces(true, sample, sample.copy(), unlocked = true))
        assertFalse("still missing: no write", HostAppearanceReader.replaces(true, null, null, unlocked = true))
    }

    /** Roadmap T98: the ringing screen before the first unlock recreated itself in a loop (G8441, API 28). */
    @Test
    fun `a screen follows the remembered snapshot while the provider cannot answer`() {
        assertEquals("before the first unlock", sample, HostAppearanceReader.follows(true, sample, null, unlocked = false))
        assertNull("nothing remembered, still locked", HostAppearanceReader.follows(true, null, null, unlocked = false))
        assertNull("unlocked: AutoJs6 is really missing", HostAppearanceReader.follows(true, sample, null, unlocked = true))
        val other = sample.copy(language = "ja")
        assertEquals("a new answer", other, HostAppearanceReader.follows(true, sample, other, unlocked = false))
        assertEquals("unchanged", sample, HostAppearanceReader.follows(true, sample, sample.copy(), unlocked = true))
    }
}
