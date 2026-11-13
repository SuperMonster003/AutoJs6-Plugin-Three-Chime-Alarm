package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.holiday

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.CancelToken
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.HttpTransport
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.net.fetchBytes
import java.io.IOException

/**
 * One holiday data refresh (roadmap P8.2, Shared-Assets README "Client order"): fetch the signature envelope and the
 * catalog from the `main` branch, verify and parse them, refuse a version below the one in use (rollback) and treat the
 * same version as up to date, download only the calendar files neither the snapshot nor the current set has (release
 * assets of `holiday-calendars-v<n>`), check each against the catalog and parse it, then install the set and switch to it
 * whole. Refreshes run one at a time; the whole data set is refreshed whatever `region` the script names (T175).
 *
 * zh-CN: 一次节假日数据刷新 (路线图 P8.2, Shared-Assets README "客户端顺序"): 从 `main` 分支获取签名信封与目录, 校验并解析, 拒绝低于当前版本的
 * 目录 (回退), 相同版本视为已是最新, 只下载快照与当前数据集都没有的日历文件 (`holiday-calendars-v<n>` 的 Release 资源), 逐个按目录核对并解析,
 * 然后安装并整套切换. 刷新一次只运行一个; 无论脚本指定哪个 `region`, 都刷新整套数据 (T175).
 */
class HolidayRefresher(
    private val store: HolidayStore,
    private val data: HolidayData,
    private val transport: HttpTransport,
    private val catalogUrl: String = CATALOG_URL,
    private val signatureUrl: String = SIGNATURE_URL,
) {

    /** [updated] false: the data in use is already the newest. zh-CN: [updated] 为 false 表示正在使用的数据已是最新. */
    data class Result(val updated: Boolean, val info: HolidayData.Info, val files: Int, val bytes: Long)

    fun interface Progress {
        fun update(done: Long, total: Long)
    }

    private val lock = Any()

    @Throws(IOException::class)
    fun refresh(cancel: CancelToken = CancelToken(), progress: Progress = Progress { _, _ -> }): Result = synchronized(lock) {
        val envelope = transport.fetchBytes(signatureUrl, HolidayCatalog.MAX_SIGNATURE_BYTES, cancel)
        val catalogBytes = transport.fetchBytes(catalogUrl, HolidayCatalog.MAX_FILE_BYTES, cancel)
        val catalog = store.verify(catalogBytes, envelope)
        val current = data.set
        val active = current.catalog.version
        if (catalog.version < active) throw HolidayDataException("the published holiday catalog ${catalog.version} is older than the data in use ($active)")
        if (catalog.version == active) return Result(false, data.info(), 0, 0)
        val missing = catalog.regions.filter { !store.has(current, it.file) }
        val total = missing.sumOf { it.file.size }
        var done = 0L
        progress.update(0, total)
        val downloaded = HashMap<String, ByteArray>(missing.size * 2)
        for (region in missing) {
            cancel.throwIfCancelled()
            val bytes = transport.fetchBytes(region.file.url, region.file.size, cancel) { read ->
                done += read
                progress.update(done, total)
            }
            HolidayDataSet.checkIntegrity(bytes, region.file)
            HolidayCalendar.parse(bytes, region.code)
            downloaded[region.file.name] = bytes
        }
        cancel.throwIfCancelled()
        data.replace(store.install(catalogBytes, envelope, downloaded, current))
        Result(true, data.info(), missing.size, total)
    }

    companion object {
        private const val RAW = "https://raw.githubusercontent.com/${HolidayCatalog.REPOSITORY}/main/holiday-calendars/catalogs/"
        const val CATALOG_URL = RAW + HolidayCatalog.CATALOG_NAME
        const val SIGNATURE_URL = RAW + HolidayCatalog.SIGNATURE_NAME
    }
}

