package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.schedule.AlarmPlanner.Cause
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Roadmap P5.3, T96: the first process start after a boot reconciles as a boot, later starts of the same boot as a
 * restore. zh-CN: 路线图 P5.3, T96: 开机后的第一次进程启动按开机对账, 同一次开机中之后的启动按恢复对账.
 */
class BootMarkerTest {

    private var count: Int? = 7
    private var saved: Int? = null
    private var writes = 0

    private val marker = BootMarker(current = { count }, stored = { saved }, store = { saved = it; writes++ })

    @Test
    fun firstStartAfterTheInstallIsARestoreAndRemembersTheCount() {
        assertEquals(Cause.RESTORE, marker.startCause())
        assertEquals(7, saved)
    }

    @Test
    fun firstStartAfterABootIsABootAndLaterStartsAreRestores() {
        saved = 7
        count = 8
        assertEquals(Cause.BOOT, marker.startCause())
        assertEquals(8, saved)
        assertEquals(Cause.RESTORE, marker.startCause())
        assertEquals(1, writes)
    }

    @Test
    fun anUnreadableCountIsARestoreAndKeepsTheStoredOne() {
        saved = 7
        count = null
        assertEquals(Cause.RESTORE, marker.startCause())
        assertEquals(7, saved)
        assertEquals(0, writes)
    }
}
