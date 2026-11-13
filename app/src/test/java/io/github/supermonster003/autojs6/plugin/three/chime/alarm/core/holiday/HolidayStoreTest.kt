package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.CancelToken
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.HttpResponse
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.HttpTransport
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.OperationCancelledException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.time.LocalDate

/**
 * The downloaded holiday data sets (roadmap P8.2, T174 / T175): a refresh fetches the signed catalog, downloads only the
 * calendar files neither the snapshot nor the current set has, checks each against the catalog and switches the pointer
 * after the whole set is written; the same version is up to date, an older one is a rollback; a wrong signature, a
 * tampered file or a cancellation leave the data in use untouched; a damaged or outdated downloaded set falls back to the
 * snapshot. Synthetic catalogs signed with a test key stand in for Shared-Assets.
 * zh-CN: 下载的节假日数据集 (路线图 P8.2, T174 / T175): 刷新获取已签名的目录, 只下载快照与当前数据集都没有的日历文件, 逐个按目录核对, 整套写完后
 * 再切换指针; 相同版本视为最新, 更旧的版本是回退; 签名错误, 文件被篡改或被取消时正在使用的数据保持不变; 损坏或过时的下载数据集回退到快照. 用测试
 * 密钥签名的合成目录代替 Shared-Assets.
 */
class HolidayStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val key = TestKey()
    private lateinit var bundledDir: File
    private lateinit var storeDir: File
    private val served = HashMap<String, ByteArray>()
    private val requested = ArrayList<String>()
    private val logged = ArrayList<String>()

    private val transport = HttpTransport { url, _, cancel ->
        cancel.throwIfCancelled()
        requested += url
        val bytes = served[url]
        if (bytes == null) HttpResponse(404, -1, null, null) {} else HttpResponse(200, bytes.size.toLong(), null, ByteArrayInputStream(bytes)) {}
    }

    private val aa1 = calendar("AA", "New Year", 1)
    private val bb1 = calendar("BB", "Founding Day", 2)
    private val bb2 = calendar("BB", "Founding Day (moved)", 3)
    private val cc2 = calendar("CC", "Harvest Day", 4)

    @Before
    fun setUp() {
        bundledDir = temp.newFolder("bundled")
        storeDir = temp.newFolder("files", "holidays")
        writeSnapshot(bundledDir, 1, listOf(aa1 at 1, bb1 at 1))
    }

    private fun store(bundled: File = bundledDir) = HolidayStore(storeDir, DirectorySource(bundled), key.verifier) { message, _ -> logged += message }

    private fun serve(version: Int, files: List<Placed>, signer: TestKey = key, tamper: (Placed) -> ByteArray = { it.calendar.bytes }) {
        served.clear()
        val catalog = catalogBytes(version, files)
        served[HolidayRefresher.CATALOG_URL] = catalog
        served[HolidayRefresher.SIGNATURE_URL] = signer.envelope(catalog)
        files.forEach { served[it.url] = tamper(it) }
    }

    @Test
    fun aNewerCatalogDownloadsOnlyTheMissingFilesAndSurvivesARestart() {
        val store = store()
        val data = HolidayData(store.load()) { "AA" }
        assertEquals(1, data.set.catalog.version)
        serve(2, listOf(aa1 at 1, bb2 at 2, cc2 at 2))
        val progress = ArrayList<Pair<Long, Long>>()
        val result = HolidayRefresher(store, data, transport).refresh { done, total -> progress += done to total }
        assertTrue(result.updated)
        assertEquals(2, result.files)
        assertEquals((bb2.bytes.size + cc2.bytes.size).toLong(), result.bytes)
        assertEquals("holiday-calendars-v2", result.info.version)
        assertEquals(listOf(HolidayRefresher.SIGNATURE_URL, HolidayRefresher.CATALOG_URL, (bb2 at 2).url, (cc2 at 2).url), requested)
        assertEquals(result.bytes to result.bytes, progress.last())
        assertEquals(2, data.set.catalog.version)
        assertEquals("2", File(storeDir, HolidayStore.POINTER_NAME).readText().trim())
        assertEquals(
            "the set holds its catalog, the envelope and only the files the snapshot lacks",
            setOf(HolidayCatalog.CATALOG_NAME, HolidayCatalog.SIGNATURE_NAME, (bb2 at 2).name, (cc2 at 2).name),
            File(storeDir, "set-2").list()!!.toSet(),
        )
        assertTrue(data.dayIn(LocalDate.of(2026, 3, 4), "CC", java.util.Locale.ROOT).holiday)

        val restarted = HolidayData(store().load()) { "AA" }
        assertEquals(2, restarted.set.catalog.version)
        assertTrue("AA still comes from the snapshot", restarted.dayIn(LocalDate.of(2026, 1, 1), "AA", java.util.Locale.ROOT).holiday)
        assertTrue(restarted.dayIn(LocalDate.of(2026, 1, 3), "BB", java.util.Locale.ROOT).holiday)
        assertFalse("BB moved its day in version 2", restarted.dayIn(LocalDate.of(2026, 1, 2), "BB", java.util.Locale.ROOT).holiday)
        assertTrue(logged.isEmpty())
    }

    @Test
    fun theSameVersionIsUpToDateAndAnOlderOneIsARollback() {
        val store = store()
        val data = HolidayData(store.load()) { "AA" }
        serve(1, listOf(aa1 at 1, bb1 at 1))
        val same = HolidayRefresher(store, data, transport).refresh()
        assertFalse(same.updated)
        assertEquals(0, same.files)
        assertEquals("only the envelope and the catalog are fetched", 2, requested.size)
        assertFalse(File(storeDir, HolidayStore.POINTER_NAME).exists())

        serve(2, listOf(aa1 at 1, bb2 at 2))
        HolidayRefresher(store, data, transport).refresh()
        serve(1, listOf(aa1 at 1, bb1 at 1))
        val error = expectHolidayData { HolidayRefresher(store, data, transport).refresh() }
        assertTrue(error.message, error.message!!.contains("older"))
        assertEquals(2, data.set.catalog.version)
    }

    @Test
    fun aLaterRefreshReusesTheFilesOfTheCurrentSetAndDropsTheOldSet() {
        val store = store()
        val data = HolidayData(store.load()) { "AA" }
        serve(2, listOf(aa1 at 1, bb2 at 2, cc2 at 2))
        HolidayRefresher(store, data, transport).refresh()
        val dd3 = calendar("DD", "River Day", 5)
        serve(3, listOf(aa1 at 1, bb2 at 2, cc2 at 2, dd3 at 3))
        requested.clear()
        val result = HolidayRefresher(store, data, transport).refresh()
        assertEquals(1, result.files)
        assertEquals(listOf(HolidayRefresher.SIGNATURE_URL, HolidayRefresher.CATALOG_URL, (dd3 at 3).url), requested)
        assertEquals(listOf(HolidayStore.POINTER_NAME, "set-3"), storeDir.list()!!.sorted())
        assertTrue(File(storeDir, "set-3/" + (cc2 at 2).name).isFile)
        assertTrue(HolidayData(store().load()) { null }.dayIn(LocalDate.of(2026, 3, 4), "CC", java.util.Locale.ROOT).holiday)
    }

    @Test
    fun aTamperedFileOrAWrongSignatureLeavesTheDataInUse() {
        val store = store()
        val data = HolidayData(store.load()) { "AA" }
        serve(2, listOf(aa1 at 1, bb2 at 2, cc2 at 2)) { placed ->
            placed.calendar.bytes.copyOf().also { if (placed.calendar.code == "CC") it[it.size - 3] = 'X'.code.toByte() }
        }
        val tampered = expectHolidayData { HolidayRefresher(store, data, transport).refresh() }
        assertTrue(tampered.message, tampered.message!!.contains("SHA-256"))
        assertUntouched(data)

        serve(2, listOf(aa1 at 1, bb2 at 2, cc2 at 2), signer = TestKey())
        expectHolidayData { HolidayRefresher(store, data, transport).refresh() }
        assertUntouched(data)

        serve(2, listOf(aa1 at 1, bb2 at 2, cc2 at 2))
        served[HolidayRefresher.CATALOG_URL] = served.getValue(HolidayRefresher.CATALOG_URL).copyOf().also { it[it.size - 2] = ' '.code.toByte() }
        expectHolidayData { HolidayRefresher(store, data, transport).refresh() }
        assertUntouched(data)
    }

    @Test
    fun aCancelledRefreshLeavesTheDataInUse() {
        val store = store()
        val data = HolidayData(store.load()) { "AA" }
        serve(2, listOf(aa1 at 1, bb2 at 2, cc2 at 2))
        val cancel = CancelToken()
        val cancelling = HttpTransport { url, start, token ->
            if (url == (cc2 at 2).url) cancel.cancel()
            transport.get(url, start, token)
        }
        try {
            HolidayRefresher(store, data, cancelling).refresh(cancel)
            fail("expected a cancellation")
        } catch (_: OperationCancelledException) {
        }
        assertUntouched(data)
    }

    @Test
    fun aDamagedOrOutdatedDownloadedSetFallsBackToTheSnapshot() {
        val store = store()
        val data = HolidayData(store.load()) { "AA" }
        serve(2, listOf(aa1 at 1, bb2 at 2, cc2 at 2))
        HolidayRefresher(store, data, transport).refresh()

        val newer = temp.newFolder("bundled-3")
        writeSnapshot(newer, 3, listOf(aa1 at 1, bb1 at 1))
        assertEquals("an updated plugin with a newer snapshot ignores the older download", 3, store(newer).load().catalog.version)

        File(storeDir, "set-2/" + HolidayCatalog.CATALOG_NAME).appendText(" ")
        assertEquals(1, store().load().catalog.version)
        assertTrue(logged.toString(), logged.single().contains("set 2 is unusable"))
    }

    private fun assertUntouched(data: HolidayData) {
        assertEquals(1, data.set.catalog.version)
        assertFalse(File(storeDir, HolidayStore.POINTER_NAME).exists())
        assertTrue("no set or temporary directory is left", storeDir.list().orEmpty().isEmpty())
        assertEquals(1, store().load().catalog.version)
    }

    private fun expectHolidayData(block: () -> Unit): HolidayDataException {
        try {
            block()
        } catch (e: HolidayDataException) {
            return e
        }
        throw AssertionError("expected HolidayDataException")
    }

    private fun writeSnapshot(dir: File, version: Int, files: List<Placed>) {
        val catalog = catalogBytes(version, files)
        File(dir, HolidayCatalog.CATALOG_NAME).writeBytes(catalog)
        File(dir, HolidayCatalog.SIGNATURE_NAME).writeBytes(key.envelope(catalog))
        files.forEach { File(dir, it.name).writeBytes(it.calendar.bytes) }
    }

    /** A synthetic calendar of 2026 with one holiday on January [day]. zh-CN: 2026 年的合成日历, 1 月 [day] 日为节假日. */
    class Calendar(val code: String, val bytes: ByteArray) {
        val sha256: String = HolidayDataSet.sha256Hex(bytes)
    }

    data class Placed(val calendar: Calendar, val release: Int) {
        val name: String get() = "holiday-calendar-${calendar.code}-${calendar.sha256.take(8)}.json"
        val tag: String get() = "holiday-calendars-v$release"
        val url: String get() = "https://github.com/${HolidayCatalog.REPOSITORY}/releases/download/$tag/$name"
    }

    private infix fun Calendar.at(release: Int) = Placed(this, release)

    private fun calendar(code: String, holiday: String, day: Int): Calendar {
        val month = if (code == "CC") "03-04" else "01-%02d".format(day)
        val json = """{"calendar":{"2026":{"holidays":{"$month":[0]},"workdays":[]}},"defaultLanguage":"en","estimatedFrom":null,""" +
            """"hasMakeupWorkdays":false,"languages":["en"],"name":"Region $code","names":[{"en":"$holiday"}],"region":"$code",""" +
            """"schemaVersion":1,"source":{"name":"python-holidays","package":"holidays","version":"0.108"},"subdivisions":{},""" +
            """"weekend":[6,7],"years":[2026,2026]}"""
        return Calendar(code, json.toByteArray(Charsets.UTF_8))
    }

    private fun catalogBytes(version: Int, files: List<Placed>): ByteArray {
        val regions = files.sortedBy { it.calendar.code }.joinToString(",") { placed ->
            """{"code":"${placed.calendar.code}","estimatedFrom":null,"file":{"name":"${placed.name}","releaseTag":"${placed.tag}",""" +
                """"sha256":"${placed.calendar.sha256}","size":${placed.calendar.bytes.size},"url":"${placed.url}"},""" +
                """"hasMakeupWorkdays":false,"name":"Region ${placed.calendar.code}","subdivisions":[],"years":[2026,2026]}"""
        }
        return ("""{"catalogVersion":$version,"family":"${HolidayCatalog.FAMILY}","generatedAt":"2026-11-1${version}T00:00:00Z",""" +
            """"regions":[$regions],"releaseTag":"holiday-calendars-v$version","repository":"${HolidayCatalog.REPOSITORY}",""" +
            """"schemaVersion":1,"source":{"homepage":"https://example.invalid","license":"MIT","name":"python-holidays","version":"0.108"},""" +
            """"years":[2020,2035]}""" + "\n").toByteArray(Charsets.UTF_8)
    }

    /** A P-256 key pair and its verifier, standing in for the Shared-Assets signing key. zh-CN: 代替 Shared-Assets 签名密钥的 P-256 密钥对. */
    class TestKey {
        private val pair: KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        private val der: ByteArray = pair.public.encoded
        val keyId: String = CatalogSignatureVerifier.keyIdForDer(der)
        val verifier = CatalogSignatureVerifier(der, keyId)

        fun envelope(catalog: ByteArray): ByteArray {
            val signature = Signature.getInstance("SHA256withECDSA").run {
                initSign(pair.private)
                update(catalog)
                sign()
            }
            return ("""{"algorithm":"${CatalogSignatureVerifier.ALGORITHM}","catalog":"${HolidayCatalog.CATALOG_NAME}",""" +
                """"keyId":"$keyId","signature":"${Base64Strict.encode(signature)}"}""" + "\n").toByteArray(Charsets.US_ASCII)
        }
    }
}
