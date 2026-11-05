package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.home

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.R

/**
 * The launcher target (roadmap D7). The skeleton shows the plugin description; roadmap P3 replaces it with the alarm list.
 * zh-CN: 启动器目标 (路线图 D7). 骨架只显示插件描述; 路线图 P3 换成闹钟列表.
 */
class HomeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        setContentView(TextView(this).apply {
            text = getString(R.string.plugin_description)
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
        })
    }

}
