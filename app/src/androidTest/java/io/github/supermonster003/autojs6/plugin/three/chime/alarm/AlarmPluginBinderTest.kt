package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.ImportedRingtones
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.ThreeChimeAlarmService
import org.autojs.plugin.alarm.api.AlarmBundles
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.AlarmResultKeys
import org.autojs.plugin.alarm.api.IAlarmEventCallback
import org.autojs.plugin.alarm.api.IAlarmPlugin
import org.autojs.plugin.alarm.api.IAlarmProgressCallback
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The `IAlarmPlugin` methods of the installed plugin through Parcels (roadmap P2.5): the happy path `add` -> `get` ->
 * `list` -> `update` -> `setAlarmEnabled` -> `remove` with its `change` events, refused requests and their error codes,
 * the limits, the methods of undeclared features (`UNSUPPORTED`), bounded event callbacks that are released again,
 * ringtone imports that close the descriptor and outlive the original file (T15), binding hints, and repeated binds
 * that leave no descriptor behind.
 *
 * Every alarm here carries the label prefix `p25-probe`; [cleanUp] removes them, the hint table of this package and
 * the imported test tone, so a run leaves the alarm list as it found it.
 *
 * zh-CN: 经 Parcel 调用已安装插件的 `IAlarmPlugin` 方法 (路线图 P2.5): happy path `add` -> `get` -> `list` -> `update` -> `setAlarmEnabled` ->
 * `remove` 及其 `change` 事件, 被拒请求与错误码, 上限, 未声明功能的方法 (`UNSUPPORTED`), 有上限且会释放的事件回调, 关闭描述符并比原文件存活更久的
 * 铃声导入 (T15), 绑定提示, 以及不留下描述符的反复绑定. 这里的闹钟都带标签前缀 `p25-probe`; [cleanUp] 删除它们, 本包的提示表与导入的测试铃声,
 * 运行后闹钟列表保持原样.
 */
@RunWith(AndroidJUnit4::class)
class AlarmPluginBinderTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val runtime: AlarmRuntime
        get() = AlarmRuntime.get(context)

    private val label = "$LABEL_PREFIX-${UUID.randomUUID().toString().take(8)}"

    private val tempFiles = ArrayList<File>()

    private val importedIds = ArrayList<String>()

    @After
    fun cleanUp() {
        runtime.repository.list().filter { it.spec.label?.startsWith(LABEL_PREFIX) == true }.forEach { runtime.repository.remove(it.id, AlarmContract.SOURCE_TOOL) }
        runtime.hints.replace(context.packageName, mapOf("hints" to emptyList<Map<String, Any?>>())) { true }
        importedIds.forEach { File(runtime.ringtones.directory, it).delete() }
        tempFiles.forEach { it.delete() }
    }

    @Test
    fun theAlarmListRoundTripsThroughParcels() = withPlugin { plugin ->
        val events = CopyOnWriteArrayList<Map<String, Any?>>()
        val callback = object : IAlarmEventCallback.Stub() {
            override fun onEvent(event: Bundle?) {
                events += AlarmBundles.fromBundle(event)
            }
        }
        assertNoError(plugin.registerEventCallback(callback, request("events" to listOf(AlarmContract.EVENT_CHANGE))))
        try {
            val added = alarmOf(plugin.addAlarm(request("hour" to 6, "minute" to 45, "label" to label, "repeat" to "FREQ=WEEKLY;BYDAY=MO")))
            val id = added["id"] as String
            assertEquals(AlarmContract.SOURCE_SCRIPT, added[AlarmResultKeys.SOURCE])
            assertEquals(AlarmContract.SOURCE_SCRIPT, added[AlarmResultKeys.UPDATED_BY])
            assertEquals(label, added["label"])
            assertNotNull("an enabled alarm has its next trigger", added[AlarmResultKeys.NEXT_TRIGGER])

            assertEquals(id, alarmOf(plugin.getAlarm(request("id" to id)))["id"])
            val listed = answerOf(plugin.listAlarms(request("filter" to mapOf("label" to label))))[AlarmResultKeys.ALARMS] as List<*>
            assertEquals(listOf(id), listed.map { (it as Map<*, *>)["id"] })

            val renamed = "$label-renamed"
            val updated = alarmOf(plugin.updateAlarm(request("id" to id, "patch" to mapOf("label" to renamed))))
            assertEquals(renamed, updated["label"])
            assertEquals(AlarmContract.SOURCE_SCRIPT, updated[AlarmResultKeys.UPDATED_BY])
            assertEquals(false, alarmOf(plugin.setAlarmEnabled(request("id" to id, "enabled" to false)))["enabled"])

            assertEquals(true, answerOf(plugin.removeAlarm(request("id" to id)))[AlarmResultKeys.REMOVED])
            assertNull("an unknown id answers no alarm", answerOf(plugin.getAlarm(request("id" to id)))[AlarmResultKeys.ALARM])

            val changes = await(4) { events.filter { it["alarmId"] == id } }
            assertEquals(listOf(AlarmContract.CHANGE_ADD, AlarmContract.CHANGE_UPDATE, AlarmContract.CHANGE_UPDATE, AlarmContract.CHANGE_REMOVE), changes.map { it["action"] })
            assertTrue(changes.all { it["type"] == AlarmContract.EVENT_CHANGE && it["source"] == AlarmContract.SOURCE_SCRIPT })
            assertEquals("each change has an occurrence of its own", 4, changes.map { it["occurrence"] }.toSet().size)
        } finally {
            plugin.unregisterEventCallback(callback)
        }
    }

    @Test
    fun refusedRequestsKeepTheirErrorCodes() = withPlugin { plugin ->
        assertError(AlarmErrorCodes.INVALID_OPTIONS, plugin.addAlarm(request("hour" to 24, "minute" to 0, "label" to label)))
        assertError(AlarmErrorCodes.INVALID_OPTIONS, plugin.addAlarm(request("hour" to 7, "minute" to 0, "label" to label, "script" to "/sdcard/a.js")))
        assertError(AlarmErrorCodes.LIMIT_EXCEEDED, plugin.addAlarm(request("hour" to 7, "minute" to 0, "label" to "x".repeat(AlarmLimits.MAX_LABEL_CHARS + 1))))
        assertError(AlarmErrorCodes.INVALID_RULE, plugin.addAlarm(request("hour" to 7, "minute" to 0, "label" to label, "repeat" to "FREQ=SECONDLY")))
        assertError(AlarmErrorCodes.UNSUPPORTED, plugin.listAlarms(Bundle().apply { putInt(AlarmContract.KEY_CONTRACT_VERSION, 2) }))
        // A value outside the value tree is dropped while decoding, so the required id is missing.
        // zh-CN: 值树之外的值在解码时丢弃, 因此缺少必需的 id.
        assertError(AlarmErrorCodes.INVALID_OPTIONS, plugin.getAlarm(AlarmBundles.of().apply { putParcelable("id", Intent()) }))
        assertError(AlarmErrorCodes.NOT_FOUND, plugin.updateAlarm(request("id" to "a_p25missing", "patch" to mapOf("label" to "x"))))
        assertEquals(false, answerOf(plugin.removeAlarm(request("id" to "a_p25missing")))[AlarmResultKeys.REMOVED])
        assertError(AlarmErrorCodes.NOT_FOUND, plugin.skipNext(request("id" to "a_p25missing")))
        assertEquals(false, answerOf(plugin.dismiss(request("id" to "a_p25missing")))[AlarmResultKeys.HANDLED])
        assertError(AlarmErrorCodes.LIMIT_EXCEEDED, plugin.upcoming(request("count" to AlarmLimits.MAX_UPCOMING_COUNT + 1)))
        assertError(AlarmErrorCodes.INVALID_OPTIONS, plugin.upcoming(request("count" to 0)))
        assertError(AlarmErrorCodes.LIMIT_EXCEEDED, plugin.preview(request("options" to mapOf("hour" to 7, "minute" to 0), "count" to AlarmLimits.MAX_PREVIEW_COUNT + 1)))
        assertError(AlarmErrorCodes.INVALID_RULE, plugin.preview(request("options" to mapOf("hour" to 7, "minute" to 0, "repeat" to "FREQ=HOURLY"))))
        val times = answerOf(plugin.preview(request("options" to mapOf("hour" to 7, "minute" to 0, "repeat" to "FREQ=DAILY"), "count" to 3)))[AlarmResultKeys.TIMES] as LongArray
        assertEquals(3, times.size)
    }

    @Test
    fun featuresThisBuildDoesNotDeclareAnswerUnsupported() = withPlugin { plugin ->
        val empty = request()
        listOf(
            plugin.decideBeforeRing(empty), plugin.startTimer(empty), plugin.getTimer(empty), plugin.listTimers(empty),
            plugin.controlTimer(empty), plugin.stopwatch(empty), plugin.holidayRegions(empty), plugin.checkDate(empty),
            plugin.listHolidays(empty), plugin.holidayInfo(empty),
        ).forEach { assertError(AlarmErrorCodes.UNSUPPORTED, it) }
        val failed = AtomicReference<Bundle>()
        val latch = CountDownLatch(1)
        val progress = object : IAlarmProgressCallback.Stub() {
            override fun onProgress(opId: Int, done: Long, total: Long) = Unit
            override fun onCompleted(opId: Int, result: Bundle?) = latch.countDown()
            override fun onFailed(opId: Int, error: Bundle?) {
                failed.set(error)
                latch.countDown()
            }
        }
        plugin.refreshHolidays(empty, progress)
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertError(AlarmErrorCodes.UNSUPPORTED, failed.get())
        plugin.cancelOperation(1)
    }

    @Test
    fun eventCallbacksAreBoundedAndReleased() = withPlugin { plugin ->
        val baseline = runtime.callbacks.size
        val first = Recorder()
        repeat(3) { assertNoError(plugin.registerEventCallback(first, request())) }
        assertEquals("a registered callback replaces its event set", baseline + 1, runtime.callbacks.size)
        assertError(AlarmErrorCodes.INVALID_OPTIONS, plugin.registerEventCallback(first, request("events" to listOf("wake"))))
        assertEquals(baseline + 1, runtime.callbacks.size)

        val more = List(AlarmLimits.MAX_EVENT_CALLBACKS - baseline - 1) { Recorder() }
        more.forEach { assertNoError(plugin.registerEventCallback(it, request("events" to listOf(AlarmContract.EVENT_RING)))) }
        assertEquals(AlarmLimits.MAX_EVENT_CALLBACKS, runtime.callbacks.size)
        assertError(AlarmErrorCodes.LIMIT_EXCEEDED, plugin.registerEventCallback(Recorder(), request()))

        (more + first).forEach { plugin.unregisterEventCallback(it) }
        plugin.unregisterEventCallback(first)
        assertEquals("unregistering releases every callback", baseline, runtime.callbacks.size)
    }

    @Test
    fun importedRingtonesAreCopiedAndTheDescriptorIsClosed() = withPlugin { plugin ->
        val tone = temp("p25-tone.wav").apply { writeBytes(wav(UUID.randomUUID().hashCode())) }
        val descriptors = openDescriptors()
        var ringtone: Map<*, *>? = null
        repeat(IMPORTS) {
            ParcelFileDescriptor.open(tone, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                ringtone = answerOf(plugin.importRingtone(pfd, request("displayName" to "Chime P25.wav", "mimeType" to "audio/wav")))[AlarmResultKeys.RINGTONE] as Map<*, *>
            }
        }
        val imported = requireNotNull(ringtone)
        val uri = imported[AlarmResultKeys.URI] as String
        ImportedRingtones.idOf(uri)?.let { importedIds += it }
        assertEquals("Chime P25", imported[AlarmResultKeys.TITLE])
        assertEquals(AlarmContract.RINGTONE_IMPORTED, imported[AlarmResultKeys.RINGTONE_SOURCE])
        assertTrue(tone.delete())
        assertTrue("the private copy outlives the original (T15)", ImportedRingtones.fileOf(runtime.ringtones.directory.parentFile!!, uri)?.isFile == true)
        val listed = answerOf(plugin.listRingtones(request()))[AlarmResultKeys.RINGTONES] as List<*>
        assertTrue(listed.any { (it as Map<*, *>)[AlarmResultKeys.URI] == uri })

        val text = temp("p25-text.wav").apply { writeText("not audio") }
        val big = temp("p25-big.wav").apply { outputStream().use { out -> out.channel.write(ByteBuffer.allocate(1), AlarmLimits.MAX_RINGTONE_BYTES) } }
        assertEquals(AlarmLimits.MAX_RINGTONE_BYTES + 1, big.length())
        ParcelFileDescriptor.open(text, ParcelFileDescriptor.MODE_READ_ONLY).use {
            assertError(AlarmErrorCodes.IO_FAILED, plugin.importRingtone(it, request("displayName" to "text.wav")))
        }
        ParcelFileDescriptor.open(big, ParcelFileDescriptor.MODE_READ_ONLY).use {
            assertError(AlarmErrorCodes.LIMIT_EXCEEDED, plugin.importRingtone(it, request("displayName" to "big.wav")))
        }
        ParcelFileDescriptor.open(text, ParcelFileDescriptor.MODE_READ_ONLY).use {
            assertError(AlarmErrorCodes.INVALID_OPTIONS, plugin.importRingtone(it, request()))
        }
        assertError(AlarmErrorCodes.INVALID_OPTIONS, plugin.importRingtone(null, request("displayName" to "none.wav")))
        assertTrue("imports leave no descriptor open", openDescriptors() - descriptors <= DESCRIPTOR_SLACK)
    }

    @Test
    fun bindingHintsMarkAlarmsAndRepeatedBindsLeaveNothingOpen() {
        withPlugin { plugin ->
            val id = alarmOf(plugin.addAlarm(request("hour" to 5, "minute" to 15, "label" to label)))["id"] as String
            val answer = answerOf(plugin.setBindingHints(request("hints" to listOf(
                mapOf("alarmId" to id, "events" to listOf(AlarmContract.EVENT_RING)),
                mapOf("alarmId" to "a_p25gone", "events" to listOf(AlarmContract.EVENT_RING)),
            ))))
            assertEquals(listOf("a_p25gone"), answer[AlarmResultKeys.UNKNOWN_IDS])
            assertEquals(true, alarmOf(plugin.getAlarm(request("id" to id)))[AlarmResultKeys.HAS_BINDINGS])
            assertError(AlarmErrorCodes.INVALID_OPTIONS, plugin.setBindingHints(request("hints" to listOf(mapOf("alarmId" to id, "events" to listOf("wake"))))))
            assertEquals("a refused table keeps the previous one", true, alarmOf(plugin.getAlarm(request("id" to id)))[AlarmResultKeys.HAS_BINDINGS])
            assertNoError(plugin.setBindingHints(request("hints" to emptyList<Map<String, Any?>>())))
            assertEquals(false, alarmOf(plugin.getAlarm(request("id" to id)))[AlarmResultKeys.HAS_BINDINGS])
            assertEquals(true, answerOf(plugin.removeAlarm(request("id" to id)))[AlarmResultKeys.REMOVED])
        }
        val descriptors = openDescriptors()
        repeat(BINDS) { withPlugin { plugin -> assertNoError(plugin.listAlarms(request())) } }
        assertTrue("binds leave no descriptor open", openDescriptors() - descriptors <= DESCRIPTOR_SLACK)
    }

    // ------------------------------------------------------------------ helpers

    private class Recorder : IAlarmEventCallback.Stub() {
        override fun onEvent(event: Bundle?) = Unit
    }

    private fun request(vararg values: Pair<String, Any?>): Bundle = AlarmBundles.of(linkedMapOf(*values))

    private fun answerOf(bundle: Bundle?): Map<String, Any?> {
        assertNoError(bundle)
        return AlarmBundles.fromBundle(bundle)
    }

    private fun alarmOf(bundle: Bundle?): Map<*, *> = answerOf(bundle)[AlarmResultKeys.ALARM] as Map<*, *>

    private fun assertNoError(bundle: Bundle?) {
        assertNotNull(bundle)
        assertNull("unexpected error ${AlarmBundles.errorOf(bundle)}", AlarmBundles.errorOf(bundle))
        assertEquals(AlarmContract.CONTRACT_VERSION, bundle!!.getInt(AlarmContract.KEY_CONTRACT_VERSION))
    }

    private fun assertError(code: String, bundle: Bundle?) {
        val error = AlarmBundles.errorOf(bundle)
        assertEquals("expected $code", code, error?.first)
        val message = error?.second.orEmpty()
        assertFalse("the error has a message", message.isBlank())
        assertTrue(message.toByteArray(Charsets.UTF_8).size <= AlarmLimits.MAX_ERROR_MESSAGE_BYTES)
    }

    private fun <T> await(count: Int, read: () -> List<T>): List<T> {
        val deadline = System.currentTimeMillis() + 5_000
        while (read().size < count && System.currentTimeMillis() < deadline) Thread.sleep(20)
        return read()
    }

    private fun temp(name: String): File = File(context.cacheDir, name).also { tempFiles += it }

    private fun openDescriptors(): Int = File("/proc/self/fd").list()?.size ?: 0

    /** A short 8 kHz mono PCM WAV; [seed] makes its content (and so its id) unique per run. */
    private fun wav(seed: Int): ByteArray {
        val samples = 800
        val data = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(samples) { data.putShort(((it * 37 + seed) % 2000 - 1000).toShort()) }
        return ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + samples * 2); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(8000); putInt(16000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(samples * 2); put(data.array())
        }.array()
    }

    private fun withPlugin(block: (IAlarmPlugin) -> Unit) {
        val binder = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        val intent = Intent().setComponent(ComponentName(context, ThreeChimeAlarmService::class.java))
        assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("timed out binding the alarm service", connected.await(10, TimeUnit.SECONDS))
            block(IAlarmPlugin.Stub.asInterface(MarshallingBinder(binder.get())))
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val LABEL_PREFIX = "p25-probe"
        const val IMPORTS = 10
        const val BINDS = 10
        const val DESCRIPTOR_SLACK = 3
    }
}
