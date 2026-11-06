package io.github.supermonster003.autojs6.plugin.three.chime.alarm.intent

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.AlarmClock
import androidx.annotation.VisibleForTesting
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R

/**
 * The chooser of `DISMISS_ALARM` when several alarms match (roadmap P2.6, T80). The new alarm page of `SET_ALARM` it
 * used to show is the edit page since roadmap P3.1 ([io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.EditAlarmActivity.draft]).
 * Internal: only [ClockIntentActivity] starts it.
 * zh-CN: `DISMISS_ALARM` 匹配多个闹钟时的选择框 (路线图 P2.6, T80). 它此前显示的 `SET_ALARM` 新建闹钟页自路线图 P3.1 起改为编辑页
 * ([io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit.EditAlarmActivity.draft]). 内部组件: 只由 [ClockIntentActivity] 启动.
 */
class ClockDialogActivity : Activity() {

    /** The shown dialog, for the instrumentation tests. zh-CN: 当前显示的对话框, 供仪器测试使用. */
    @VisibleForTesting
    internal var dialog: AlertDialog? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val built = when (intent.getStringExtra(EXTRA_MODE)) {
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

    companion object {

        private const val EXTRA_MODE = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.extra.CLOCK_DIALOG_MODE"
        private const val EXTRA_IDS = "io.github.supermonster003.autojs6.plugin.three.chime.alarm.extra.CLOCK_DIALOG_IDS"
        private const val MODE_CHOOSE = "choose"

        /** The chooser of the alarms [ids] to dismiss. zh-CN: 选择要关闭的闹钟 [ids]. */
        fun choose(context: Context, ids: List<String>): Intent = Intent(context, ClockDialogActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_MODE, MODE_CHOOSE)
            .putExtra(EXTRA_IDS, ids.toTypedArray())
    }
}
