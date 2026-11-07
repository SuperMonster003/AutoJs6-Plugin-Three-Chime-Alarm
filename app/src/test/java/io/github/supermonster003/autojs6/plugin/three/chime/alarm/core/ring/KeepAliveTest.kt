package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap Q23 (b), T100: the "Keep running in background" setting is off by default and survives a round trip through
 * its storage, and the keep-alive service runs exactly while the setting is on and an alarm is enabled; a running
 * service is not started again and one that has not reached the foreground is not stopped.
 * zh-CN: 路线图 Q23 (b), T100: "保持后台运行" 默认关闭, 经存储往返后保持不变; 保活服务恰在设置打开且有启用的闹钟时运行; 运行中的服务不重复启动, 尚未进入
 * 前台的服务不停止.
 */
class KeepAliveTest {

    private val values = HashMap<String, Boolean>()

    private var writable = true

    private val storage = object : KeepAliveSettings.Storage {
        override fun getBoolean(key: String, default: Boolean) = values[key] ?: default
        override fun putBoolean(key: String, value: Boolean): Boolean {
            if (!writable) return false
            values[key] = value
            return true
        }
    }

    private fun alarm(id: String, enabled: Boolean?) = AlarmRecord(
        id = id,
        source = AlarmContract.SOURCE_USER,
        updatedBy = AlarmContract.SOURCE_USER,
        createdAt = 0,
        updatedAt = 0,
        spec = AlarmSpec(time = AlarmTime(7, 30), enabled = enabled),
    )

    @Test
    fun `the setting is off by default and round trips`() {
        val settings = KeepAliveSettings(storage)
        assertFalse(settings.enabled)
        assertTrue(settings.setEnabled(true))
        assertTrue(settings.enabled)
        assertTrue("a second reader sees the stored value", KeepAliveSettings(storage).enabled)
        assertTrue(settings.setEnabled(false))
        assertFalse(KeepAliveSettings(storage).enabled)
        assertEquals(mapOf(KeepAliveSettings.KEY_ENABLED to false), values)
    }

    @Test
    fun `a failed write keeps the stored value`() {
        val settings = KeepAliveSettings(storage)
        writable = false
        assertFalse(settings.setEnabled(true))
        assertFalse(settings.enabled)
    }

    @Test
    fun `the service runs while the setting is on and an alarm is enabled`() {
        val on = alarm("a_on", enabled = null)
        val off = alarm("a_off", enabled = false)
        assertTrue("an alarm without enabled is enabled", on.enabled)
        assertTrue(KeepAlivePolicy.shouldRun(true, listOf(off, on)))
        assertFalse("no enabled alarm", KeepAlivePolicy.shouldRun(true, listOf(off)))
        assertFalse("no alarm", KeepAlivePolicy.shouldRun(true, emptyList()))
        assertFalse("setting off", KeepAlivePolicy.shouldRun(false, listOf(on)))
    }

    @Test
    fun `starts only a service that is not running and stops only a running one`() {
        assertEquals(KeepAlivePolicy.Action.START, KeepAlivePolicy.action(shouldRun = true, running = false))
        assertEquals(KeepAlivePolicy.Action.NONE, KeepAlivePolicy.action(shouldRun = true, running = true))
        assertEquals(KeepAlivePolicy.Action.STOP, KeepAlivePolicy.action(shouldRun = false, running = true))
        assertEquals(KeepAlivePolicy.Action.NONE, KeepAlivePolicy.action(shouldRun = false, running = false))
    }
}
