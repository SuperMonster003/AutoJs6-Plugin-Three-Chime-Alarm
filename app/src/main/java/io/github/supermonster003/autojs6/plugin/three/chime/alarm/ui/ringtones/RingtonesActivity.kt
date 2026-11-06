package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.ringtones

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmException
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.ImportedRingtones
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.ring.RingtoneLibrary
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring.RingPlayer
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.SystemRingtones
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.ChimeScreen
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.RowDefaults
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.components.TopBarAction
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.defaultText
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.soundText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmErrorCodes
import org.autojs.plugin.alarm.api.AlarmLimits
import org.autojs.plugin.alarm.api.RingtoneSnapshot
import java.io.File
import java.io.FileNotFoundException

/**
 * The ringtone page (roadmap P3.3, T85): picks the sound of an alarm (from the edit page, with "Default") or the plugin
 * default sound (from the settings screen, without it). It lists the choices of the sound dialog it replaces (Default,
 * the system alarm sound, silence), then the imported ringtones with the import row, then the alarm sounds of the
 * device: the same ringtones `ringtones.list()` answers, so files a script imported are listed too. A row only selects;
 * its play button previews the sound once on the alarm stream; the check button returns the choice, and back with a
 * changed choice asks first. Imports (the system file picker, copied into the plugin, at most 20 MiB) and deletions
 * take effect at once and are not undone by back.
 * zh-CN: 铃声页 (路线图 P3.3, T85): 选择闹钟的铃声 (从编辑页进入, 带 "默认") 或插件的默认铃声 (从设置页进入, 不带). 先列出它所取代的铃声对话框的选项
 * (默认, 系统闹钟铃声, 静音), 再列导入的铃声与导入行, 最后列设备的闹钟铃声: 与 `ringtones.list()` 返回的铃声相同, 因此脚本导入的文件也会列出.
 * 点按行只做选择; 行上的播放按钮在闹钟音频流上试听一次; 勾选按钮返回所选项, 所选项有改动时返回键先询问. 导入 (系统文件选择器, 复制进插件, 至多
 * 20 MiB) 与删除立即生效, 返回键不会撤销.
 */
class RingtonesActivity : StandaloneActivity() {

    private var busy by mutableStateOf(false)

    @Composable
    override fun Content() {
        val request = remember { RingtonePicker.requestOf(intent) }
        RingtonesScreen(
            request = request,
            onBusyChange = { busy = it },
            onCancel = ::finish,
            onConfirm = { source ->
                setResult(Activity.RESULT_OK, RingtonePicker.resultOf(source))
                finish()
            },
        )
    }

    /** An import or a deletion in progress keeps the page. zh-CN: 导入或删除进行中时不重建页面. */
    override fun holdsAppearance(): Boolean = busy
}

/**
 * What [RingtonesActivity] starts with: [source] is the current choice (null for "Default"), and [fallback] the sound
 * "Default" stands for, null when the page offers no "Default" (the plugin default itself).
 * zh-CN: [RingtonesActivity] 的初始值: [source] 为当前选择 (null 表示 "默认"), [fallback] 为 "默认" 所代表的铃声, 页面不提供 "默认" 时 (插件默认值
 * 本身) 为 null.
 */
internal data class RingtoneRequest(val source: String?, val fallback: String?)

/** The confirmed choice: a sound source, or null for "Default". zh-CN: 确定的选择: 铃声来源, null 表示 "默认". */
internal data class RingtoneChoice(val source: String?)

/** Opens [RingtonesActivity]; the result is null when the page was left without confirming. zh-CN: 打开铃声页; 未确定就离开时结果为 null. */
internal class RingtonePicker : ActivityResultContract<RingtoneRequest, RingtoneChoice?>() {

    override fun createIntent(context: Context, input: RingtoneRequest): Intent =
        Intent(context, RingtonesActivity::class.java).putExtra(EXTRA_SOURCE, input.source).putExtra(EXTRA_FALLBACK, input.fallback)

    override fun parseResult(resultCode: Int, intent: Intent?): RingtoneChoice? =
        if (resultCode == Activity.RESULT_OK && intent != null) RingtoneChoice(intent.getStringExtra(EXTRA_SOURCE)) else null

    companion object {

        private const val EXTRA_SOURCE = "source"
        private const val EXTRA_FALLBACK = "fallback"

        fun requestOf(intent: Intent): RingtoneRequest {
            val fallback = intent.getStringExtra(EXTRA_FALLBACK)
            val source = intent.getStringExtra(EXTRA_SOURCE) ?: if (fallback == null) AlarmContract.SOUND_DEFAULT else null
            return RingtoneRequest(source, fallback)
        }

        fun resultOf(source: String?): Intent = Intent().putExtra(EXTRA_SOURCE, source)
    }
}

@Composable
private fun RingtonesScreen(request: RingtoneRequest, onBusyChange: (Boolean) -> Unit, onCancel: () -> Unit, onConfirm: (String?) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selected by rememberSaveable { mutableStateOf(request.source) }
    var fallback by rememberSaveable { mutableStateOf(request.fallback) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var discarding by rememberSaveable { mutableStateOf(false) }
    var system by remember { mutableStateOf<List<RingtoneSnapshot>?>(null) }
    var imported by remember { mutableStateOf<List<RingtoneSnapshot>?>(null) }
    var busy by remember { mutableStateOf(false) }
    val previewFailed = stringResource(R.string.ringtones_preview_failed)
    val preview = remember { RingtonePreview(app) { scope.launch(Dispatchers.Main.immediate) { snackbar.showSnackbar(previewFailed) } } }

    // Imports and deletions may happen elsewhere (a script, another page), so the lists are read at every resume.
    // zh-CN: 导入与删除可能发生在别处 (脚本, 其他页面), 因此每次恢复时重新读取列表.
    suspend fun reload() {
        val (tones, own) = withContext(Dispatchers.IO) { SystemRingtones.list(app) to AlarmRuntime.get(app).ringtones.list() }
        system = tones
        imported = own
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> scope.launch(Dispatchers.Main.immediate) { reload() }
                // A ring, the file picker or leaving the page stops the preview. zh-CN: 响铃, 文件选择器或离开页面都会停止试听.
                Lifecycle.Event.ON_STOP -> preview.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            preview.stop()
        }
    }
    SideEffect { onBusyChange(busy) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch(Dispatchers.Main.immediate) {
            val result = withContext(Dispatchers.IO) { runCatching { RingtoneImports.import(app, AlarmRuntime.get(app).ringtones, uri) } }
            busy = false
            reload()
            result.onSuccess { ringtone ->
                selected = ringtone.uri
                snackbar.showSnackbar(resources.getString(R.string.ringtones_import_done, ringtone.title))
            }.onFailure { e ->
                Log.w(TAG, "import failed: ${e.javaClass.simpleName}: ${e.message}")
                snackbar.showSnackbar(RingtoneImports.messageOf(resources, e, imported?.size ?: 0))
            }
        }
    }

    fun delete(ringtone: RingtoneSnapshot) {
        deleting = null
        busy = true
        if (preview.playing?.endsWith(ringtone.uri) == true) preview.stop()
        scope.launch(Dispatchers.Main.immediate) {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val runtime = AlarmRuntime.get(app)
                    runtime.ringtones.delete(ringtone.uri)
                    // The plugin default that named it goes back to the system alarm sound (T85). zh-CN: 指向它的插件默认值改回系统闹钟铃声 (T85).
                    val defaults = runtime.defaults.get()
                    if (defaults.sound == ringtone.uri) runtime.defaults.set(defaults.copy(sound = AlarmContract.SOUND_DEFAULT))
                }
            }
            busy = false
            if (fallback == ringtone.uri) fallback = AlarmContract.SOUND_DEFAULT
            if (selected == ringtone.uri) selected = if (request.fallback != null) null else AlarmContract.SOUND_DEFAULT
            reload()
            result.onSuccess {
                snackbar.showSnackbar(resources.getString(R.string.ringtones_deleted, ringtone.title))
            }.onFailure { e ->
                Log.w(TAG, "delete failed: ${e.javaClass.simpleName}: ${e.message}")
                snackbar.showSnackbar(resources.getString(R.string.ringtones_delete_failed))
            }
        }
    }

    val changed = selected != request.source
    BackHandler(enabled = changed) { discarding = true }
    ChimeScreen(
        title = stringResource(R.string.edit_sound),
        onBack = { if (changed) discarding = true else onCancel() },
        snackbarHostState = snackbar,
        actions = {
            TopBarAction(R.drawable.ic_check_24, stringResource(R.string.ui_dialog_confirm), "chime.ringtones.confirm", enabled = !busy) {
                preview.stop()
                onConfirm(selected)
            }
        },
    ) { padding ->
        val own = imported
        val device = system
        val loaded = own != null && device != null
        val all = device.orEmpty() + own.orEmpty()
        val missing = request.source?.takeIf { source ->
            loaded && source != AlarmContract.SOUND_DEFAULT && source != AlarmContract.SOUND_SILENT && all.none { it.uri == source }
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("chime.ringtones.list")) {
            fallback?.let { value ->
                item {
                    ToneRow(
                        title = defaultText(soundText(value, if (loaded) all else null)),
                        note = stringResource(R.string.edit_default_note),
                        selected = selected == null,
                        onSelect = { selected = null },
                        tag = "chime.ringtones.fallback",
                        preview = preview,
                        source = value.takeUnless { it == AlarmContract.SOUND_SILENT },
                    )
                }
            }
            item {
                ToneRow(stringResource(R.string.edit_sound_system), null, selected == AlarmContract.SOUND_DEFAULT, { selected = AlarmContract.SOUND_DEFAULT },
                    "chime.ringtones.item.${AlarmContract.SOUND_DEFAULT}", preview, AlarmContract.SOUND_DEFAULT)
            }
            item {
                ToneRow(stringResource(R.string.edit_sound_silent), null, selected == AlarmContract.SOUND_SILENT, { selected = AlarmContract.SOUND_SILENT },
                    "chime.ringtones.item.${AlarmContract.SOUND_SILENT}", preview, null)
            }
            if (missing != null) {
                item { ToneRow(stringResource(R.string.edit_sound_missing), missing, selected == missing, { selected = missing }, "chime.ringtones.missing", preview, null) }
            }
            item { SectionHeader(stringResource(R.string.ringtones_imported)) }
            items(own.orEmpty()) { tone ->
                ToneRow(tone.title.ifEmpty { tone.uri }, null, selected == tone.uri, { selected = tone.uri }, "chime.ringtones.item.${tone.uri}", preview, tone.uri,
                    onDelete = { deleting = tone.uri })
            }
            if (own?.isEmpty() == true) item { Note(stringResource(R.string.ringtones_imported_none)) }
            item {
                SettingsRow(
                    stringResource(R.string.ringtones_import),
                    summary = stringResource(R.string.ringtones_import_summary, RingtoneImports.MAX_MIB),
                    icon = R.drawable.ic_upload_24,
                    showChevron = false,
                    enabled = !busy && own != null,
                    onClick = {
                        preview.stop()
                        importer.launch(RingtoneImports.TYPES)
                    },
                    modifier = Modifier.testTag("chime.ringtones.import"),
                )
            }
            item { SectionHeader(stringResource(R.string.ringtones_system)) }
            items(device.orEmpty()) { tone ->
                ToneRow(tone.title.ifEmpty { tone.uri }, if (tone.isDefault) stringResource(R.string.ringtones_system_current) else null, selected == tone.uri,
                    { selected = tone.uri }, "chime.ringtones.item.${tone.uri}", preview, tone.uri)
            }
            if (device?.isEmpty() == true) item { Note(stringResource(R.string.ringtones_system_none)) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    val target = deleting?.let { uri -> imported?.firstOrNull { it.uri == uri } }
    if (target != null) {
        var usage by remember(target.uri) { mutableStateOf(0 to false) }
        LaunchedEffect(target.uri) {
            usage = withContext(Dispatchers.IO) {
                val runtime = AlarmRuntime.get(app)
                runtime.repository.list().count { it.spec.sound?.source == target.uri } to (runtime.defaults.get().sound == target.uri)
            }
        }
        val (used, isDefault) = usage
        val message = buildString {
            append(stringResource(R.string.ringtones_delete_message, target.title))
            if (used > 0) append(' ').append(pluralStringResource(R.plurals.ringtones_delete_used, used, used))
            if (isDefault) append(' ').append(stringResource(R.string.ringtones_delete_default))
        }
        MessageDialog(
            title = stringResource(R.string.ringtones_delete_title),
            message = message,
            onDismissRequest = { deleting = null },
            onConfirm = { delete(target) },
            confirmText = stringResource(R.string.home_delete),
            tag = "chime.ringtones.deleteDialog",
        )
    }
    if (discarding) {
        MessageDialog(
            title = stringResource(R.string.edit_discard_title),
            message = stringResource(R.string.ringtones_discard_message),
            onDismissRequest = { discarding = false },
            onConfirm = {
                discarding = false
                onCancel()
            },
            confirmText = stringResource(R.string.edit_discard),
            tag = "chime.ringtones.discardDialog",
        )
    }
}

/**
 * One choice: a radio, the title with an optional note, and the preview (when [source] is playable) and delete buttons.
 * Each row previews under its own [tag], so "Default" and the sound it stands for show their own state.
 * zh-CN: 一个选项: 单选按钮, 标题与可选的说明, 以及试听 ([source] 可播放时) 与删除按钮. 每行以自己的 [tag] 试听, 因此 "默认" 与它所代表的铃声各自显示状态.
 */
@Composable
private fun ToneRow(
    title: String,
    note: String?,
    selected: Boolean,
    onSelect: () -> Unit,
    tag: String,
    preview: RingtonePreview,
    source: String?,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .defaultMinSize(minHeight = if (note.isNullOrEmpty()) 56.dp else 72.dp)
            .padding(start = RowDefaults.horizontalPadding, end = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!note.isNullOrEmpty()) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (source != null) {
            val key = "$tag|$source"
            val playing = preview.playing == key
            IconButton(onClick = { preview.toggle(key, source) }, modifier = Modifier.testTag("$tag.preview")) {
                Icon(
                    painterResource(if (playing) R.drawable.ic_stop_24 else R.drawable.ic_play_arrow_24),
                    contentDescription = stringResource(if (playing) R.string.ringtones_stop else R.string.ringtones_preview),
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.testTag("$tag.delete")) {
                Icon(painterResource(R.drawable.ic_delete_24), contentDescription = stringResource(R.string.home_delete))
            }
        }
        if (source == null && onDelete == null) Spacer(Modifier.width(RowDefaults.horizontalPadding - 4.dp))
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = RowDefaults.horizontalPadding, vertical = 8.dp),
    )
}

/**
 * The preview of the ringtone page (T85): one [MediaPlayer] on the alarm stream ([RingPlayer.ATTRIBUTES], so it is heard
 * at the alarm volume), played once without looping, fading, vibration or audio focus. [playing] is the key of the row
 * being previewed; a failure stops it and calls [onFailed].
 * zh-CN: 铃声页的试听 (T85): 闹钟音频流上的一个 [MediaPlayer] ([RingPlayer.ATTRIBUTES], 因此以闹钟音量播放), 只播一遍, 不循环, 不渐强, 不振动, 不申请
 * 音频焦点. [playing] 为正在试听的行的键; 失败时停止并调用 [onFailed].
 */
internal class RingtonePreview(private val context: Context, private val onFailed: () -> Unit) {

    var playing by mutableStateOf<String?>(null)
        private set

    private var player: MediaPlayer? = null

    fun toggle(key: String, source: String) {
        if (playing == key) stop() else play(key, source)
    }

    private fun play(key: String, source: String) {
        stop()
        val uri = uriOf(context, source)
        if (uri == null) {
            onFailed()
            return
        }
        val next = MediaPlayer()
        try {
            next.setAudioAttributes(RingPlayer.ATTRIBUTES)
            next.setDataSource(context, uri)
            next.setOnPreparedListener { if (player === it) it.start() }
            next.setOnCompletionListener { if (player === it) stop() }
            next.setOnErrorListener { failed, what, extra ->
                if (player === failed) {
                    Log.w(TAG, "preview failed: what=$what extra=$extra")
                    stop()
                    onFailed()
                }
                true
            }
            player = next
            playing = key
            next.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, "preview failed: ${e.javaClass.simpleName}")
            if (player === next) {
                player = null
                playing = null
            }
            next.release()
            onFailed()
        }
    }

    fun stop() {
        player?.release()
        player = null
        playing = null
    }

    companion object {

        /**
         * The playable uri of [source]: the default alarm sound setting, the copy of an imported ringtone, or the uri
         * itself; null for silence and for an imported ringtone without its copy.
         * zh-CN: [source] 的可播放 uri: 默认闹钟铃声设置, 导入铃声的副本, 或 uri 本身; 静音与没有副本的导入铃声为 null.
         */
        fun uriOf(context: Context, source: String): Uri? = when {
            source == AlarmContract.SOUND_SILENT -> null
            source == AlarmContract.SOUND_DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ImportedRingtones.isImported(source) ->
                ImportedRingtones.fileOf(context.createDeviceProtectedStorageContext().filesDir, source)?.takeIf(File::isFile)?.let(Uri::fromFile)
            else -> Uri.parse(source)
        }
    }
}

/**
 * Imports a document of the system file picker into [RingtoneLibrary] (T85): the size the provider reports is checked
 * before copying (the library checks again while copying), and the display name becomes the title.
 * zh-CN: 把系统文件选择器选中的文档导入 [RingtoneLibrary] (T85): 复制前先检查提供方报告的大小 (复制时铃声库还会再检查), 显示名称作为标题.
 */
internal object RingtoneImports {

    /** The types offered by the file picker; some providers report Ogg files as `application/ogg`. zh-CN: 有的提供方把 Ogg 文件报为 `application/ogg`. */
    val TYPES = arrayOf("audio/*", "application/ogg")

    val MAX_MIB: Int = (AlarmLimits.MAX_RINGTONE_BYTES / (1024L * 1024L)).toInt()

    fun import(context: Context, library: RingtoneLibrary, uri: Uri): RingtoneSnapshot {
        val resolver = context.contentResolver
        var name: String? = null
        var size: Long? = null
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameColumn >= 0 && !cursor.isNull(nameColumn)) name = cursor.getString(nameColumn)
                    if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn)
                }
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "import: cannot query the document: ${e.javaClass.simpleName}")
        }
        if ((size ?: 0L) > AlarmLimits.MAX_RINGTONE_BYTES) throw AlarmException.limitExceeded("the file has $size bytes")
        val input = try {
            resolver.openInputStream(uri)
        } catch (_: FileNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        } ?: throw AlarmException.ioFailed("cannot open the document")
        return input.use { library.import(it, name ?: uri.lastPathSegment) }
    }

    /**
     * The message of a failed import: the count limit, the size limit, or a file that cannot be read or decoded.
     * zh-CN: 导入失败的提示: 数量上限, 大小上限, 或文件读不到 / 无法解码.
     */
    fun messageOf(resources: Resources, e: Throwable, importedCount: Int): String = when {
        e is AlarmException && e.code == AlarmErrorCodes.LIMIT_EXCEEDED && importedCount >= AlarmLimits.MAX_RINGTONES ->
            resources.getQuantityString(R.plurals.ringtones_import_full, AlarmLimits.MAX_RINGTONES, AlarmLimits.MAX_RINGTONES)
        e is AlarmException && e.code == AlarmErrorCodes.LIMIT_EXCEEDED -> resources.getString(R.string.ringtones_import_too_large, MAX_MIB)
        else -> resources.getString(R.string.ringtones_import_failed)
    }
}

private const val TAG = "ChimeRingtones"
