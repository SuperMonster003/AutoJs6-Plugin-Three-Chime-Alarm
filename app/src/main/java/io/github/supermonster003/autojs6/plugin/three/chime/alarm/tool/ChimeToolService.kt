package io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool

import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.AlarmRuntime
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.pluginRuntimeFields
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.toPluginInfo
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.tool.api.PluginToolCall
import org.autojs.plugin.tool.api.PluginToolProviderService
import org.autojs.plugin.tool.api.PluginToolResult

/**
 * The plugin tool provider of 3-Chime Alarm (AutoJs6 plugin tool contract v1, roadmap P4.1, D5): AutoJs6 binds it with
 * `org.autojs.plugin.TOOL_PROVIDER` behind the plugin permission and forwards the calls of MCP Server, 3-Stove Agent and
 * `plugins.tools`. It runs in the plugin's only process beside the alarm service, so the tools share its repository,
 * scheduler and ring state ([AlarmRuntime]); changes are recorded with source `tool` (T18). The contract library's base
 * class runs each call on a worker thread; [ChimeTools] does the work on plain value trees. Arguments, labels and spoken
 * texts are never logged.
 * zh-CN: 3-Chime Alarm 的插件工具提供方 (AutoJs6 插件工具契约 v1, 路线图 P4.1, D5): AutoJs6 以 `org.autojs.plugin.TOOL_PROVIDER` 在插件权限下绑定它,
 * 转发 MCP Server, 3-Stove Agent 与 `plugins.tools` 的调用. 它与闹钟服务同处插件唯一的进程, 因此工具共用其仓库, 调度器与响铃状态 ([AlarmRuntime]);
 * 变更以来源 `tool` 记录 (T18). 契约库的基类在工作线程上执行每次调用; [ChimeTools] 在普通值树上完成工作. 参数, 标签与朗读文字从不写日志.
 */
class ChimeToolService : PluginToolProviderService() {

    private val tools by lazy { ChimeTools(AlarmRuntime.get(this).toolApi) }

    override fun createPluginInfo(): PluginInfo = pluginRuntimeFields().toPluginInfo()

    override fun createCatalogJson(): String = ChimeToolCatalog.JSON

    override fun onCall(call: PluginToolCall): PluginToolResult {
        Log.i(TAG, "Tool ${call.tool} called by ${call.consumer ?: "an unnamed consumer"}")
        val result = tools.call(call.tool, ToolJson.parseObject(call.arguments.toString()))
        return PluginToolResult(ToolJson.encode(result))
    }

    private companion object {
        const val TAG = "ChimeTools"
    }
}
