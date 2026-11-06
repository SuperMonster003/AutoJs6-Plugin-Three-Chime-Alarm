package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Roadmap P3.5, T84: the automatic update check runs at the chosen frequency (every 12 hours by default), never without
 * a network, and on a metered network only when allowed; ignored versions stay apart.
 * zh-CN: 路线图 P3.5, T84: 自动更新检查按所选频率进行 (默认每 12 小时), 无网络时不检查, 计量网络只在允许时检查; 已忽略的版本单独保存.
 */
class AppUpdateStoreTest {

    private var now = TimeUnit.DAYS.toMillis(20_000)
    private val values = HashMap<String, Any>()

    private val store = AppUpdateStore(object : AppUpdateStore.Storage {
        override fun getBoolean(key: String, default: Boolean) = values[key] as Boolean? ?: default
        override fun putBoolean(key: String, value: Boolean) = run { values[key] = value }
        override fun getLong(key: String, default: Long) = values[key] as Long? ?: default
        override fun putLong(key: String, value: Long) = run { values[key] = value }
        override fun getString(key: String) = values[key] as String?
        override fun putString(key: String, value: String) = run { values[key] = value }
        @Suppress("UNCHECKED_CAST")
        override fun getStrings(key: String) = values[key] as Set<String>? ?: emptySet()
        override fun putStrings(key: String, value: Set<String>) = run { values[key] = value }
    }, clock = { now })

    @Test
    fun `every 12 hours by default, on unmetered networks only`() {
        assertEquals(UpdateFrequency.TWELVE_HOURS, store.frequency)
        assertFalse(store.checkOnMetered)
        assertTrue(store.shouldCheckAutomatically(UpdateNetwork.UNMETERED))
        assertFalse(store.shouldCheckAutomatically(UpdateNetwork.METERED))
        assertFalse(store.shouldCheckAutomatically(UpdateNetwork.NONE))

        store.recordAutomaticAttempt()
        now += TimeUnit.HOURS.toMillis(12) - 1
        assertFalse(store.shouldCheckAutomatically(UpdateNetwork.UNMETERED))
        now += 1
        assertTrue(store.shouldCheckAutomatically(UpdateNetwork.UNMETERED))
    }

    @Test
    fun `the frequency sets the interval and off never checks`() {
        store.recordAutomaticAttempt()
        now += TimeUnit.DAYS.toMillis(1)
        store.frequency = UpdateFrequency.WEEKLY
        assertFalse(store.shouldCheckAutomatically(UpdateNetwork.UNMETERED))
        store.frequency = UpdateFrequency.DAILY
        assertTrue(store.shouldCheckAutomatically(UpdateNetwork.UNMETERED))
        store.frequency = UpdateFrequency.OFF
        assertFalse(store.shouldCheckAutomatically(UpdateNetwork.UNMETERED))
        assertEquals(UpdateFrequency.OFF, store.frequency)

        // An unknown stored value reads as the default. zh-CN: 未知的存储值读作默认值.
        values[AppUpdateStore.KEY_FREQUENCY] = "hourly"
        assertEquals(UpdateFrequency.DEFAULT, store.frequency)
    }

    @Test
    fun `metered networks need the switch, and a clock set back is due`() {
        store.checkOnMetered = true
        assertTrue(store.shouldCheckAutomatically(UpdateNetwork.METERED))
        store.recordAutomaticAttempt()
        assertFalse(store.shouldCheckAutomatically(UpdateNetwork.METERED))
        now -= 1
        assertTrue(store.shouldCheckAutomatically(UpdateNetwork.METERED))
    }

    @Test
    fun `ignored versions are kept and released`() {
        store.ignore("v1.1.0")
        store.ignore("v1.2.0")
        assertEquals(setOf("v1.1.0", "v1.2.0"), store.ignoredTags)
        store.stopIgnoring(listOf("v1.1.0"))
        assertEquals(setOf("v1.2.0"), store.ignoredTags)
    }
}
