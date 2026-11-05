package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R
import org.autojs.plugin.alarm.api.AlarmContract

/**
 * The ringing screen (roadmap T5, P2.4), shown over the lock screen and turning the screen on: the time and label of
 * the sounding alarm with snooze and dismiss. It follows the ring state, shows the next alarm of the queue when one
 * ends, and closes when nothing sounds. Coming to the front it asks the ring for audio focus again (T35). The layout is
 * plain views; roadmap P3.3 gives it the designed screen.
 *
 * zh-CN: 响铃界面 (路线图 T5, P2.4), 显示在锁屏之上并点亮屏幕: 正在发声的闹钟的时间与标签, 以及贪睡与关闭按钮. 它跟随响铃状态, 一个闹钟结束时显示队列中的
 * 下一个, 没有闹钟发声时关闭. 到前台时让响铃再次申请音频焦点 (T35). 布局为普通 View, 路线图 P3.3 换成设计好的界面.
 */
class RingActivity : Activity() {

    private val main = Handler(Looper.getMainLooper())

    private lateinit var runtime: AlarmRuntime

    private lateinit var time: TextView

    private lateinit var label: TextView

    private lateinit var snooze: Button

    private var alarmId: String? = null

    private val listener: () -> Unit = { main.post(::render) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = AlarmRuntime.get(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(layout())
        runtime.rings.addListener(listener)
        render()
    }

    override fun onResume() {
        super.onResume()
        Log.i(RingService.TAG, "ring screen resumed ${alarmId ?: "(none)"}")
        RingService.focus(this)
    }

    override fun onDestroy() {
        runtime.rings.removeListener(listener)
        main.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun render() {
        if (isFinishing) return
        val ring = runtime.rings.sounding ?: run {
            finish()
            return
        }
        alarmId = ring.alarmId
        time.text = RingService.timeOf(this, ring.request.scheduledAt)
        label.text = ring.record.spec.label?.takeIf { it.isNotBlank() } ?: getString(R.string.alarm_default_label)
        snooze.visibility = if (ring.canSnooze && runtime.schedule.canSnooze()) View.VISIBLE else View.GONE
    }

    private fun act(what: String, block: (String?) -> Boolean) {
        val id = alarmId
        runtime.executor.execute {
            try {
                Log.i(RingService.TAG, "$what on the ring screen: handled=${block(id)}")
            } catch (e: RuntimeException) {
                Log.e(RingService.TAG, "$what failed", e)
            }
        }
    }

    private fun layout(): View {
        val density = resources.displayMetrics.density
        val padding = (24 * density).toInt()
        time = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 64f)
            gravity = Gravity.CENTER
        }
        label = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            setPadding(0, padding / 2, 0, padding * 2)
        }
        snooze = Button(this).apply {
            text = getString(R.string.ring_action_snooze)
            setOnClickListener { act("snooze") { runtime.rings.snooze(it, null, AlarmContract.BY_USER) } }
        }
        val dismiss = Button(this).apply {
            text = getString(R.string.ring_action_dismiss)
            setOnClickListener { act("dismiss") { runtime.rings.dismiss(it, AlarmContract.BY_USER) } }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val weight = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = padding / 2
                marginEnd = padding / 2
            }
            addView(snooze, LinearLayout.LayoutParams(weight))
            addView(dismiss, LinearLayout.LayoutParams(weight))
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
            addView(time)
            addView(label)
            addView(buttons, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
    }
}
