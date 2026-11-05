package io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.AlarmClock
import android.text.InputType
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.TimePicker
import androidx.annotation.VisibleForTesting
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.rule.RepeatText
import org.autojs.plugin.alarm.api.AlarmBundles
import org.autojs.plugin.alarm.api.AlarmOptionKeys
import java.util.Calendar

/**
 * The windows of the system clock intents (roadmap P2.6) until the Compose screens of roadmap P3.1 replace them: the
 * new alarm page of `SET_ALARM` (time, label and the repeat of the intent; saved with source `intent`, T79) and the
 * chooser of `DISMISS_ALARM` when several alarms match (T80). Internal: only [ClockIntentActivity] starts it.
 * zh-CN: 系统时钟 Intent 的窗口 (路线图 P2.6), 直到路线图 P3.1 的 Compose 界面取代它们: `SET_ALARM` 的新建闹钟页 (时间, 标签与 Intent 给出的重复;
 * 以来源 `intent` 保存, T79), 以及 `DISMISS_ALARM` 匹配多个闹钟时的选择框 (T80). 内部组件: 只由 [ClockIntentActivity] 启动.
 */
class ClockDialogActivity : Activity() {

    /** The shown dialog, for the instrumentation tests. zh-CN: 当前显示的对话框, 供仪器测试使用. */
    @VisibleForTesting
    internal var dialog: AlertDialog? = null
        private set
    private var picker: TimePicker? = null
    private var labelInput: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val built = when (intent.getStringExtra(EXTRA_MODE)) {
            MODE_DRAFT -> draftDialog(savedInstanceState)
            MODE_CHOOSE -> chooseDialog()
            else -> null
        }
        if (built == null) {
            finish()
            return
        }
        built.setOnDismissListener { if (!isChangingConfigurations) finish() }
        built.show()
        dialog = built
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        picker?.let {
            outState.putInt(STATE_HOUR, it.hour)
            outState.putInt(STATE_MINUTE, it.minute)
        }
        labelInput?.let { outState.putString(STATE_LABEL, it.text.toString()) }
    }

    override fun onDestroy() {
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        dialog = null
        super.onDestroy()
    }

    private fun builder() = AlertDialog.Builder(this, dialogTheme())

    private fun dialogTheme(): Int {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (night) android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert
    }

    private fun draftDialog(state: Bundle?): AlertDialog {
        val options = intent.getBundleExtra(EXTRA_OPTIONS)?.let { AlarmBundles.fromBundle(it) }.orEmpty()
        val now = Calendar.getInstance()
        val context = builder().context
        val timePicker = TimePicker(context, null, 0, R.style.Widget_ThreeChimeAlarm_TimePicker).apply {
            setIs24HourView(DateFormat.is24HourFormat(this@ClockDialogActivity))
            hour = state?.getInt(STATE_HOUR) ?: options[AlarmOptionKeys.HOUR] as? Int ?: now.get(Calendar.HOUR_OF_DAY)
            minute = state?.getInt(STATE_MINUTE) ?: options[AlarmOptionKeys.MINUTE] as? Int ?: now.get(Calendar.MINUTE)
        }
        val label = EditText(context).apply {
            hint = getString(R.string.clock_label_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine()
            setText(state?.getString(STATE_LABEL) ?: options[AlarmOptionKeys.LABEL] as? String ?: "")
        }
        val padding = dp(24)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, dp(8), padding, 0)
            addView(timePicker, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            (options[AlarmOptionKeys.REPEAT] as? String)?.let { rule ->
                addView(TextView(context).apply {
                    text = runCatching { RepeatText.of(rule) }.getOrDefault(rule)
                    setPadding(dp(4), dp(8), dp(4), dp(8))
                })
            }
        }
        picker = timePicker
        labelInput = label
        return builder()
            .setTitle(R.string.clock_new_alarm_title)
            .setView(ScrollView(context).apply { addView(content) })
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val values = LinkedHashMap(options)
                values[AlarmOptionKeys.HOUR] = timePicker.hour
                values[AlarmOptionKeys.MINUTE] = timePicker.minute
                val text = ClockRequests.labelOf(label.text.toString())
                if (text != null) values[AlarmOptionKeys.LABEL] = text else values.remove(AlarmOptionKeys.LABEL)
                ClockActions.save(this, ClockIntentActivity.nameOf(AlarmClock.ACTION_SET_ALARM), values, reuse = true)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun chooseDialog(): AlertDialog? {
        val repository = AlarmRuntime.get(this).repository
        val records = intent.getStringArrayExtra(EXTRA_IDS).orEmpty().mapNotNull { repository.get(it) }
        if (records.isEmpty()) return null
        val action = ClockIntentActivity.nameOf(AlarmClock.ACTION_DISMISS_ALARM)
        return builder()
            .setTitle(R.string.clock_choose_dismiss_title)
            .setItems(records.map { ClockActions.lineOf(this, it) }.toTypedArray()) { _, which ->
                ClockActions.dismiss(this, action, listOf(records[which]))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {

        private const val EXTRA_MODE = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.extra.CLOCK_DIALOG_MODE"
        private const val EXTRA_OPTIONS = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.extra.CLOCK_DIALOG_OPTIONS"
        private const val EXTRA_IDS = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.extra.CLOCK_DIALOG_IDS"
        private const val MODE_DRAFT = "draft"
        private const val MODE_CHOOSE = "choose"
        private const val STATE_HOUR = "hour"
        private const val STATE_MINUTE = "minute"
        private const val STATE_LABEL = "label"

        /**
         * The new alarm page, filled in with [options] (from [ClockRequests.setAlarm]) when given.
         * zh-CN: 新建闹钟页, 给出 [options] (来自 [ClockRequests.setAlarm]) 时预先填好.
         */
        fun draft(context: Context, options: Map<String, Any?>?): Intent = Intent(context, ClockDialogActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_MODE, MODE_DRAFT)
            .apply { options?.let { putExtra(EXTRA_OPTIONS, AlarmBundles.toBundle(it)) } }

        /** The chooser of the alarms [ids] to dismiss. zh-CN: 选择要关闭的闹钟 [ids]. */
        fun choose(context: Context, ids: List<String>): Intent = Intent(context, ClockDialogActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_MODE, MODE_CHOOSE)
            .putExtra(EXTRA_IDS, ids.toTypedArray())
    }
}
