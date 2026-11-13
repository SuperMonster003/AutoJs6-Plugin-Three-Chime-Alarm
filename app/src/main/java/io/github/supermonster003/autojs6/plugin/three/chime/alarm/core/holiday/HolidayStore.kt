package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.io.AtomicFiles
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Read access to the holiday snapshot bundled with the APK (`assets/holiday-calendars`, roadmap P8.2): the catalog, its
 * signature envelope and the calendar files under their release asset names. [DirectorySource] reads a directory (JVM
 * tests); the plugin reads its assets.
 * zh-CN: 对 APK 内置节假日快照 (`assets/holiday-calendars`, 路线图 P8.2) 的读取: 目录, 签名信封, 以及以 Release 资源名保存的日历文件.
 * [DirectorySource] 读取一个目录 (JVM 测试); 插件读取自身的 assets.
 */
interface HolidaySource {

    /** The bytes of [name], or null when the snapshot has no such file. zh-CN: [name] 的字节, 快照中没有该文件时为 null. */
    @Throws(IOException::class)
    fun read(name: String): ByteArray?

    fun has(name: String): Boolean
}

class DirectorySource(private val dir: File) : HolidaySource {

    override fun read(name: String): ByteArray? = File(dir, name).takeIf { it.isFile }?.readBytes()

    override fun has(name: String): Boolean = File(dir, name).isFile
}

/**
 * Where the holiday data lives (roadmap P8.2, T174): the bundled snapshot, and at most one downloaded set in
 * `<dir>/set-<catalogVersion>/` named by the pointer file `<dir>/current`. A downloaded set holds its catalog, the
 * signature envelope and only the calendar files the snapshot lacks; a file name carries the first 8 hex digits of its
 * hash, so the snapshot's file of the same name is the same content. Every load verifies the signature again, and a set
 * that does not verify, or is not newer than the snapshot (after an update of the plugin), is ignored: the snapshot is
 * used. [install] writes a new set beside the old one and switches the pointer atomically, so a crash leaves either the
 * old or the new set complete (Shared-Assets README, client order step 5).
 *
 * zh-CN: 节假日数据的存放 (路线图 P8.2, T174): 内置快照, 以及至多一套下载的数据集, 位于 `<dir>/set-<catalogVersion>/`, 由指针文件 `<dir>/current`
 * 指明. 下载数据集包含其目录, 签名信封, 以及快照没有的日历文件; 文件名带哈希的前 8 位十六进制数, 因此快照中的同名文件内容相同. 每次加载都重新校验
 * 签名, 校验失败或不比快照新 (插件更新之后) 的数据集被忽略, 改用快照. [install] 在旧数据集旁写入新数据集再原子切换指针, 崩溃时留下的旧数据集或
 * 新数据集都是完整的 (Shared-Assets README, 客户端顺序第 5 步).
 */
class HolidayStore(
    private val dir: File,
    private val bundled: HolidaySource,
    private val verifier: CatalogSignatureVerifier,
    private val log: (String, Throwable?) -> Unit = { _, _ -> },
) {

    /** The verified bundled snapshot; a broken snapshot is a build error the JVM tests catch. zh-CN: 已校验的内置快照. */
    val bundledSet: HolidayDataSet by lazy {
        val catalog = bundled.read(HolidayCatalog.CATALOG_NAME) ?: throw HolidayDataException("the bundled holiday catalog is missing")
        val envelope = bundled.read(HolidayCatalog.SIGNATURE_NAME) ?: throw HolidayDataException("the bundled holiday signature is missing")
        open(catalog, envelope, null)
    }

    /**
     * The data set to use now: the downloaded set when it verifies and is newer than the snapshot, else the snapshot.
     * zh-CN: 当前应使用的数据集: 下载数据集校验通过且比快照新时用它, 否则用快照.
     */
    @Throws(HolidayDataException::class)
    fun load(): HolidayDataSet {
        val base = bundledSet
        val version = pointer() ?: return base
        if (version <= base.catalog.version) return base
        val setDir = File(dir, SET_PREFIX + version)
        return try {
            open(File(setDir, HolidayCatalog.CATALOG_NAME).readBytes(), File(setDir, HolidayCatalog.SIGNATURE_NAME).readBytes(), setDir)
                .also { check(it.catalog.version == version) { "set $version holds catalog ${it.catalog.version}" } }
        } catch (e: IOException) {
            log("downloaded holiday set $version is unusable, using the bundled snapshot", e)
            base
        } catch (e: IllegalStateException) {
            log("downloaded holiday set $version is unusable, using the bundled snapshot", e)
            base
        }
    }

    /**
     * Verifies [catalogBytes] against [envelopeBytes] and parses it (client order steps 1 - 2).
     * zh-CN: 用 [envelopeBytes] 校验 [catalogBytes] 并解析 (客户端顺序第 1 - 2 步).
     */
    @Throws(HolidayDataException::class)
    fun verify(catalogBytes: ByteArray, envelopeBytes: ByteArray): HolidayCatalog {
        verifier.verify(catalogBytes, envelopeBytes)
        return HolidayCatalog.parse(catalogBytes)
    }

    /** True when [file] is available without a download for a set after [current]. zh-CN: [current] 之后的数据集无需下载即可得到 [file] 时为 true. */
    fun has(current: HolidayDataSet, file: HolidayCatalog.FileRef): Boolean =
        bundled.has(file.name) || current.directory?.let { File(it, file.name).isFile } == true

    /**
     * Installs the verified catalog [catalogBytes] with the calendar files [downloaded] (by asset name; every other file
     * comes from the snapshot or the set of [current]) and answers the new set. Every file is checked against the catalog
     * before the pointer moves; older sets are deleted afterwards.
     * zh-CN: 安装已校验的目录 [catalogBytes] 及日历文件 [downloaded] (按资源名; 其余文件来自快照或 [current] 的数据集), 返回新数据集. 指针切换前按目录
     * 核对每个文件; 之后删除旧数据集.
     */
    @Throws(IOException::class)
    fun install(catalogBytes: ByteArray, envelopeBytes: ByteArray, downloaded: Map<String, ByteArray>, current: HolidayDataSet): HolidayDataSet {
        val catalog = verify(catalogBytes, envelopeBytes)
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("cannot create ${dir.name}")
        val target = File(dir, SET_PREFIX + catalog.version)
        val temp = File(dir, SET_PREFIX + catalog.version + TEMP_SUFFIX)
        temp.deleteRecursively()
        if (!temp.mkdirs()) throw IOException("cannot create ${temp.name}")
        try {
            for (region in catalog.regions) {
                val file = region.file
                if (bundled.has(file.name)) continue
                val bytes = downloaded[file.name]
                    ?: current.directory?.let { File(it, file.name) }?.takeIf { it.isFile }?.readBytes()
                    ?: throw HolidayDataException("${file.name} was not downloaded")
                HolidayDataSet.checkIntegrity(bytes, file)
                writeSynced(File(temp, file.name), bytes)
            }
            writeSynced(File(temp, HolidayCatalog.CATALOG_NAME), catalogBytes)
            writeSynced(File(temp, HolidayCatalog.SIGNATURE_NAME), envelopeBytes)
            AtomicFiles.syncDirectory(temp)
            if (target.exists() && target != current.directory) target.deleteRecursively()
            if (target.exists()) throw HolidayDataException("set ${catalog.version} is in use")
            if (!temp.renameTo(target)) throw IOException("cannot move ${temp.name}")
            AtomicFiles.syncDirectory(dir)
            AtomicFiles.write(File(dir, POINTER_NAME), "${catalog.version}\n".toByteArray(Charsets.US_ASCII))
        } catch (e: IOException) {
            temp.deleteRecursively()
            throw e
        }
        dir.listFiles()?.filter { it.name.startsWith(SET_PREFIX) && it != target }?.forEach { it.deleteRecursively() }
        return HolidayDataSet(catalog, target) { ref -> read(target, ref) }
    }

    private fun open(catalogBytes: ByteArray, envelopeBytes: ByteArray, setDir: File?): HolidayDataSet {
        val catalog = verify(catalogBytes, envelopeBytes)
        return HolidayDataSet(catalog, setDir) { ref -> read(setDir, ref) }
    }

    private fun read(setDir: File?, ref: HolidayCatalog.FileRef): ByteArray? =
        setDir?.let { File(it, ref.name) }?.takeIf { it.isFile }?.readBytes() ?: bundled.read(ref.name)

    private fun pointer(): Int? = try {
        File(dir, POINTER_NAME).takeIf { it.isFile }?.readText(Charsets.US_ASCII)?.trim()?.toIntOrNull()
    } catch (e: IOException) {
        log("cannot read the holiday set pointer", e)
        null
    }

    private fun writeSynced(file: File, bytes: ByteArray) {
        FileOutputStream(file).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
    }

    companion object {
        const val POINTER_NAME = "current"
        const val SET_PREFIX = "set-"
        private const val TEMP_SUFFIX = ".tmp"
    }
}
