package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm

import org.autojs.plugin.alarm.api.AlarmJson
import org.autojs.plugin.alarm.api.AlarmSpec
import org.autojs.plugin.alarm.api.AlarmTime
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Roadmap P2.1 (T21, T49): the alarm document survives a round trip, skips damaged records on their own and keeps a
 * quarantine copy of whatever it could not read.
 * zh-CN: 路线图 P2.1 (T21, T49): 闹钟文档往返不失真, 损坏的记录单独跳过, 读不了的内容保留隔离副本.
 */
class AlarmStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val validator = AlarmOptionsValidator()

    private var now = 1_793_000_000_000L

    private fun store() = AlarmStore(File(temp.root, "alarms"), clock = { now })

    private fun record(id: String, hour: Int = 7, enabled: Boolean = true, source: String = "script") = AlarmRecord(
        id = id,
        source = source,
        updatedBy = source,
        createdAt = 1_000L,
        updatedAt = 2_000L,
        spec = AlarmSpec(time = AlarmTime(hour, 0), date = "2026-11-06", enabled = enabled),
    )

    private fun writeDocument(store: AlarmStore, alarms: List<Any?>, version: Int = AlarmStore.VERSION) {
        store.directory.mkdirs()
        val text = AlarmJson.write(linkedMapOf("format" to AlarmStore.FORMAT, "version" to version, "alarms" to alarms))
        store.file.writeText(text, Charsets.UTF_8)
    }

    @Test
    fun `every option and the plugin state survive a round trip`() {
        val spec = validator.options(AlarmOptionsValidatorTest.FULL + mapOf("label" to astral(3) + " Wake up"))
        val state = AlarmRuntimeState(
            nextTrigger = 1_793_100_000_000L,
            skipAt = 1_793_100_000_000L,
            handledThrough = 1_793_000_000_000L,
            occurrence = "a_000000000001:1793000000000:0",
            snoozeUntil = 1_793_000_600_000L,
            snoozeCount = 2,
            dateImplicit = true,
        )
        val records = listOf(
            AlarmRecord("a_000000000001", "user", "tool", 10L, 20L, spec, state),
            AlarmRecord("a_000000000002", "intent", "intent", 30L, 30L, AlarmSpec(at = 1_800_000_000_000L, enabled = false)),
        )
        store().save(records)
        val result = store().load(validator::options)
        assertTrue(result.clean)
        assertEquals(records, result.records)
        assertNull(result.quarantined)
        assertFalse(store().quarantineDirectory.exists())
    }

    @Test
    fun `a damaged record is skipped on its own and the original is quarantined`() {
        val store = store()
        val good = record("a_good").toValues()
        val badHour = record("a_bad").toValues().toMutableMap().apply { put("options", mapOf("hour" to 25, "minute" to 0)) }
        val badSource = record("a_who").toValues().toMutableMap().apply { put("source", "nobody") }
        val badId = record("a_good").toValues().toMutableMap().apply { put("id", "x_1") }
        writeDocument(store, listOf(good, badHour, "not an object", badSource, badId, good))
        val original = store.file.readBytes()

        val result = store.load(validator::options)
        assertEquals(listOf("a_good"), result.records.map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 5), result.damaged.map { it.index })
        assertTrue(result.damaged[0].reason, result.damaged[0].reason.contains("hour"))
        assertEquals("duplicate id", result.damaged[4].reason)
        val copy = assertNotNullFile(result.quarantined)
        assertArrayEquals(original, copy.readBytes())

        // The cleaned document was written back: the next start is clean.
        // zh-CN: 清理后的文档已写回, 下次启动是干净的.
        val again = store().load(validator::options)
        assertTrue(again.clean)
        assertEquals(result.records, again.records)
    }

    @Test
    fun `an unreadable document is quarantined and replaced by an empty one`() {
        val store = store()
        store.directory.mkdirs()
        store.file.writeText("{\"format\": \"three-chime-alarm.alarms\", \"version\": 1, \"alarms\": [", Charsets.UTF_8)
        val result = store.load(validator::options)
        assertTrue(result.records.isEmpty())
        assertEquals(-1, result.damaged.single().index)
        assertNotNullFile(result.quarantined)
        assertTrue(store().load(validator::options).clean)

        writeDocument(store, emptyList(), version = 0)
        val old = store().load(validator::options)
        assertTrue(old.damaged.single().reason, old.damaged.single().reason.contains("cannot be migrated"))
    }

    @Test
    fun `a newer document keeps the records and drops the options it does not know`() {
        val store = store()
        val future = record("a_future").toValues().toMutableMap().apply {
            put("options", mapOf("hour" to 7, "minute" to 0, "enabled" to true, "weather" to "sunny"))
            put("state", mapOf("nextTrigger" to 5L, "mood" to "calm"))
        }
        writeDocument(store, listOf(future), version = AlarmStore.VERSION + 1)
        val result = store.load(validator::options)
        assertEquals(AlarmStore.VERSION + 1, result.newerVersion)
        assertEquals(AlarmTime(7, 0), result.records.single().spec.time)
        assertEquals(5L, result.records.single().state.nextTrigger)
        assertNotNullFile(result.quarantined)
    }

    @Test
    fun `limits are applied in creation order when loading`() {
        val store = store()
        val enabled = (1..205).map { record("a_e$it").toValues() }
        val disabled = (1..100).map { record("a_d$it", enabled = false).toValues() }
        writeDocument(store, enabled + disabled)
        val result = store.load(validator::options)
        assertEquals(300, result.records.size)
        assertEquals(200, result.records.count { it.enabled })
        assertEquals((201..205).map { "a_e$it" }, result.disabledOverLimit)
        assertEquals((96..100).map { it + 204 }, result.damaged.map { it.index })
        assertNotNullFile(result.quarantined)
    }

    @Test
    fun `saving leaves no temp file and loading removes leftovers`() {
        val store = store()
        store.save(listOf(record("a_one")))
        assertEquals(listOf(AlarmStore.FILE_NAME), store.directory.list()!!.sorted())
        File(store.directory, ".alarms-crashed.tmp").writeText("partial")
        assertEquals(1, store().load(validator::options).records.size)
        assertEquals(listOf(AlarmStore.FILE_NAME), store.directory.list()!!.sorted())
    }

    @Test
    fun `only the newest quarantine copies are kept`() {
        repeat(AlarmStore.MAX_QUARANTINE + 2) {
            now += 1_000L
            val store = store()
            store.directory.mkdirs()
            store.file.writeText("garbage $it", Charsets.UTF_8)
            store.load(validator::options)
        }
        val copies = store().quarantineDirectory.list()!!.sorted()
        assertEquals(AlarmStore.MAX_QUARANTINE, copies.size)
        assertEquals("garbage 6", File(store().quarantineDirectory, copies.last()).readText())
    }

    private fun assertNotNullFile(file: File?): File {
        assertNotNull("a quarantine copy", file)
        assertTrue(file!!.isFile)
        return file
    }
}
