package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool.ChimeToolCatalog
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.tool.ChimeToolService
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.tool.api.IPluginToolCallback
import org.autojs.plugin.tool.api.IPluginToolProvider
import org.autojs.plugin.tool.api.PluginToolAcceptKeys
import org.autojs.plugin.tool.api.PluginToolCapabilityKeys
import org.autojs.plugin.tool.api.PluginToolCatalogKeys
import org.autojs.plugin.tool.api.PluginToolContract
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorKeys
import org.autojs.plugin.tool.api.PluginToolInvokeKeys
import org.autojs.plugin.tool.api.PluginToolResultKeys
import org.autojs.plugin.tool.api.PluginToolValues
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P4.1 (E2): the plugin tool provider through its Binder interface as AutoJs6 calls it for `plugins.tools.call`
 * (every call goes through Parcels, [MarshallingBinder]): discovery and the capability, the catalog, then each 1.0.0
 * tool on alarms tagged [TAG] (add -> list -> update -> set_enabled -> skip_next -> upcoming -> remove), the 1.1.0 timer
 * tools on a 25-minute timer labelled [TAG] (start -> list -> pause -> add -> resume -> reset -> cancel, roadmap P7.6), the
 * refusals of the base class and the tool errors. Rings are never started: the alarms lie 30 days ahead, the timer is
 * cancelled long before it ends, and dismiss / snooze name an unknown id. Every alarm and timer the test made is removed.
 * Results are `ChimeDeviceRun` log lines with counts only.
 * zh-CN: 路线图 P4.1 (E2): 像 AutoJs6 为 `plugins.tools.call` 调用那样经 Binder 接口测试插件工具提供方 (每次调用都经 Parcel, [MarshallingBinder]): 发现与能力,
 * 目录, 然后在标签为 [TAG] 的闹钟上测试每个 1.0.0 工具 (add -> list -> update -> set_enabled -> skip_next -> upcoming -> remove), 在名称为 [TAG] 的
 * 25 分钟计时器上测试 1.1.0 的计时器工具 (start -> list -> pause -> add -> resume -> reset -> cancel, 路线图 P7.6), 基类的拒绝与工具错误.
 * 从不开始响铃: 闹钟都在 30 天后, 计时器远在到点前取消, dismiss / snooze 使用不存在的 id. 测试建的闹钟与计时器全部删除. 结果为只含数量的
 * `ChimeDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
class ChimeToolServiceTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val created = ArrayList<String>()
    private val timers = ArrayList<String>()

    @After
    fun tearDown() {
        if (created.isEmpty() && timers.isEmpty()) return
        withProvider { provider ->
            created.forEach { call(provider, ChimeToolCatalog.TOOL_REMOVE, JSONObject().put("id", it)) }
            timers.forEach { call(provider, ChimeToolCatalog.TOOL_TIMER_CONTROL, JSONObject().put("id", it).put("action", "cancel")) }
        }
    }

    @Test
    fun discoveryCapabilityAndCatalog() = withProvider { provider ->
        assertEquals(PluginToolContract.CONTRACT_VERSION, provider.info.capabilities?.getInt(PluginToolCapabilityKeys.CONTRACT_VERSION))
        val answer = provider.getCatalog(Bundle().apply { putInt(PluginToolInvokeKeys.CONTRACT_VERSION, PluginToolContract.CONTRACT_VERSION) })
        val catalog = JSONObject(answer.getString(PluginToolCatalogKeys.CATALOG_JSON)!!)
        assertEquals(ChimeToolCatalog.SHORT_NAME, catalog.getString("shortName"))
        val tools = catalog.getJSONArray("tools").let { array -> (0 until array.length()).map { array.getJSONObject(it).getString("name") } }
        assertEquals(ChimeToolCatalog.TOOLS, tools)
        log("catalog tools=${tools.size} bytes=${catalog.toString().length}")
    }

    @Test
    fun everyToolRoundTrips() = withProvider { provider ->
        val exact = AlarmRuntime.get(context).port.canScheduleExact()
        val day = LocalDate.now().plusDays(30).toString()
        if (!exact) {
            assertEquals(PluginToolErrorCodes.NOT_READY, call(provider, ChimeToolCatalog.TOOL_ADD, JSONObject().put("time", "04:00").put("tag", TAG)).code)
        }
        val added = call(provider, ChimeToolCatalog.TOOL_ADD, JSONObject()
            .put("time", "04:10").put("date", day).put("repeat", JSONArray(listOf(1, 2, 3, 4, 5))).put("label", "p41 weekdays").put("tag", TAG)
            .put("enabled", exact).put("sound", JSONObject().put("source", "silent")).put("snooze", JSONObject().put("minutes", 5)))
            .result().getJSONObject("alarm")
        val id = added.getString("id").also(created::add)
        assertEquals("04:10", added.getString("time"))
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", added.getString("repeat"))
        assertEquals(AlarmContract.SOURCE_TOOL, added.getString("source"))
        assertEquals(exact, added.getBoolean("enabled"))

        val second = call(provider, ChimeToolCatalog.TOOL_ADD, JSONObject().put("time", "04:20").put("date", day).put("tag", TAG).put("enabled", false))
            .result().getJSONObject("alarm").getString("id").also(created::add)

        val listed = call(provider, ChimeToolCatalog.TOOL_LIST, JSONObject().put("tag", TAG).put("source", "tool")).result()
        assertEquals(2, listed.getInt("total"))
        assertEquals(listOf(id, second), listed.getJSONArray("alarms").let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } })

        val updated = call(provider, ChimeToolCatalog.TOOL_UPDATE, JSONObject().put("id", id).put("label", "p41 changed").put("sound", JSONObject().put("rampSeconds", 20)))
            .result().getJSONObject("alarm")
        assertEquals("p41 changed", updated.getString("label"))
        assertEquals("silent", updated.getJSONObject("sound").getString("source"))
        assertEquals(20, updated.getJSONObject("sound").getInt("rampSeconds"))
        assertEquals(AlarmContract.SOURCE_TOOL, updated.getString("updatedBy"))

        val on = call(provider, ChimeToolCatalog.TOOL_SET_ENABLED, JSONObject().put("id", second).put("enabled", exact)).result().getJSONObject("alarm")
        assertEquals(exact, on.getBoolean("enabled"))
        if (exact) {
            val skipped = call(provider, ChimeToolCatalog.TOOL_SKIP_NEXT, JSONObject().put("id", id)).result().getJSONObject("alarm")
            assertTrue(skipped.getBoolean("skipping"))
            assertFalse(call(provider, ChimeToolCatalog.TOOL_SKIP_NEXT, JSONObject().put("id", id).put("cancel", true)).result().getJSONObject("alarm").getBoolean("skipping"))
            val occurrences = call(provider, ChimeToolCatalog.TOOL_UPCOMING, JSONObject().put("count", 3).put("tag", TAG)).result().getJSONArray("occurrences")
            assertEquals(3, occurrences.length())
            // On a weekday the repeating alarm (04:10) rings first that day, on a weekend the one-shot alarm (04:20).
            val weekday = LocalDate.parse(day).dayOfWeek.value <= 5
            assertEquals(if (weekday) id else second, occurrences.getJSONObject(0).getString("alarmId"))
            assertTrue(occurrences.getJSONObject(0).getString("time").startsWith(day))
        }

        assertEquals(false, call(provider, ChimeToolCatalog.TOOL_DISMISS, JSONObject().put("id", "a_missing")).result().getBoolean("handled"))
        assertEquals(false, call(provider, ChimeToolCatalog.TOOL_SNOOZE, JSONObject().put("id", "a_missing").put("minutes", 3)).result().getBoolean("handled"))

        assertTrue(call(provider, ChimeToolCatalog.TOOL_REMOVE, JSONObject().put("id", id)).result().getBoolean("removed"))
        assertTrue(call(provider, ChimeToolCatalog.TOOL_REMOVE, JSONObject().put("id", second)).result().getBoolean("removed"))
        assertFalse(call(provider, ChimeToolCatalog.TOOL_REMOVE, JSONObject().put("id", id)).result().getBoolean("removed"))
        created.clear()
        assertEquals(0, call(provider, ChimeToolCatalog.TOOL_LIST, JSONObject().put("tag", TAG)).result().getInt("total"))
        log("roundtrip exact=$exact tools=9")
    }

    @Test
    fun timerToolsRoundTrip() = withProvider { provider ->
        val exact = AlarmRuntime.get(context).port.canScheduleExact()
        if (!exact) {
            val refused = call(provider, ChimeToolCatalog.TOOL_TIMER_START, JSONObject().put("duration", "25m").put("label", TAG))
            assertEquals(PluginToolErrorCodes.NOT_READY, refused.code)
            assertFalse(refused.message!!.contains("enabled false"))
            log("timers exact=false NOT_READY")
            return@withProvider
        }
        val started = call(provider, ChimeToolCatalog.TOOL_TIMER_START, JSONObject().put("duration", "25m").put("label", TAG).put("sound", JSONObject().put("source", "silent")))
            .result().getJSONObject("timer")
        val id = started.getString("id").also(timers::add)
        assertTrue(id.startsWith("t_"))
        assertEquals(AlarmContract.TIMER_RUNNING, started.getString("state"))
        assertEquals(25 * 60_000L, started.getLong("durationMillis"))
        assertTrue(started.has("endsAt") && started.has("endsAtTime"))
        assertEquals(AlarmContract.SOURCE_TOOL, AlarmRuntime.get(context).timers.get(id)!!.source)

        val listed = call(provider, ChimeToolCatalog.TOOL_TIMER_LIST, JSONObject()).result().getJSONArray("timers")
        assertTrue((0 until listed.length()).any { listed.getJSONObject(it).getString("id") == id })

        val paused = call(provider, ChimeToolCatalog.TOOL_TIMER_CONTROL, JSONObject().put("id", id).put("action", "pause")).result().getJSONObject("timer")
        assertEquals(AlarmContract.TIMER_PAUSED, paused.getString("state"))
        assertFalse(paused.has("endsAt"))
        val left = paused.getLong("remainingMillis")
        val shorter = call(provider, ChimeToolCatalog.TOOL_TIMER_CONTROL, JSONObject().put("id", id).put("action", "add").put("duration", "-1m")).result().getJSONObject("timer")
        assertEquals(left - 60_000L, shorter.getLong("remainingMillis"))
        val longer = call(provider, ChimeToolCatalog.TOOL_TIMER_CONTROL, JSONObject().put("id", id).put("action", "add").put("duration", 120_000)).result().getJSONObject("timer")
        assertEquals(left + 60_000L, longer.getLong("remainingMillis"))
        assertEquals(AlarmContract.TIMER_RUNNING, call(provider, ChimeToolCatalog.TOOL_TIMER_CONTROL, JSONObject().put("id", id).put("action", "resume")).result().getJSONObject("timer").getString("state"))
        val reset = call(provider, ChimeToolCatalog.TOOL_TIMER_CONTROL, JSONObject().put("id", id).put("action", "reset")).result().getJSONObject("timer")
        assertEquals(AlarmContract.TIMER_PAUSED, reset.getString("state"))
        assertEquals(25 * 60_000L, reset.getLong("remainingMillis"))

        val text = call(provider, ChimeToolCatalog.TOOL_TIMER_START, JSONObject().put("duration", "25"))
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, text.code)
        assertTrue(text.message!!.startsWith("INVALID_OPTIONS: "))
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, ChimeToolCatalog.TOOL_TIMER_CONTROL, JSONObject().put("id", id).put("action", "add")).code)
        val missing = call(provider, ChimeToolCatalog.TOOL_TIMER_CONTROL, JSONObject().put("id", "t_missing").put("action", "pause"))
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, missing.code)
        assertTrue(missing.message!!.startsWith("NOT_FOUND: "))

        assertTrue(call(provider, ChimeToolCatalog.TOOL_TIMER_CONTROL, JSONObject().put("id", id).put("action", "cancel")).result().getBoolean("removed"))
        assertFalse(call(provider, ChimeToolCatalog.TOOL_TIMER_CONTROL, JSONObject().put("id", id).put("action", "cancel")).result().getBoolean("removed"))
        timers.clear()
        log("timers exact=true tools=3 refusals=3")
    }

    @Test
    fun refusalsAndToolErrors() = withProvider { provider ->
        assertEquals(PluginToolErrorCodes.UNKNOWN_TOOL, call(provider, "stopwatch_start", JSONObject()).code)
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, ChimeToolCatalog.TOOL_ADD, JSONObject().put("time", "7 am")).code)
        val local = call(provider, ChimeToolCatalog.TOOL_ADD, JSONObject().put("time", "04:00").put("enabled", false).put("sound", JSONObject().put("source", "/sdcard/a.mp3")))
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, local.code)
        assertTrue(local.message!!.startsWith("INVALID_OPTIONS: "))
        val rule = call(provider, ChimeToolCatalog.TOOL_ADD, JSONObject().put("time", "04:00").put("enabled", false).put("repeat", "FREQ=HOURLY"))
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, rule.code)
        assertTrue(rule.message!!.startsWith("INVALID_RULE: "))
        val missing = call(provider, ChimeToolCatalog.TOOL_UPDATE, JSONObject().put("id", "a_missing").put("label", "x"))
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, missing.code)
        assertTrue(missing.message!!.startsWith("NOT_FOUND: "))
        assertEquals(PluginToolErrorCodes.LIMIT_EXCEEDED, call(provider, ChimeToolCatalog.TOOL_ADD, JSONObject().put("time", "04:00").put("enabled", false).put("label", "x".repeat(101))).code)
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, ChimeToolCatalog.TOOL_UPCOMING, JSONObject().put("count", 101)).code)
        log("refusals 7")
    }

    /** A finished call: the result or the error from the refusal of `invoke` or the callback. zh-CN: 已结束的调用. */
    private class Answer(val code: String?, val message: String?, val bundle: Bundle?) {
        fun result(): JSONObject {
            code?.let { throw AssertionError("the call failed: $it $message") }
            return JSONObject(bundle!!.getString(PluginToolResultKeys.RESULT_JSON)!!)
        }
    }

    private fun call(provider: IPluginToolProvider, tool: String, arguments: JSONObject): Answer {
        val done = CountDownLatch(1)
        val answer = AtomicReference<Answer>()
        val callback = object : IPluginToolCallback.Stub() {
            override fun onProgress(progress: Bundle?) = Unit

            override fun onResult(bundle: Bundle?, descriptors: Array<out ParcelFileDescriptor>?) {
                answer.set(Answer(null, null, bundle))
                done.countDown()
            }

            override fun onError(error: Bundle?) {
                answer.set(Answer(error?.getString(PluginToolErrorKeys.CODE), error?.getString(PluginToolErrorKeys.MESSAGE), null))
                done.countDown()
            }
        }
        val request = Bundle().apply {
            putInt(PluginToolInvokeKeys.CONTRACT_VERSION, PluginToolContract.CONTRACT_VERSION)
            putString(PluginToolInvokeKeys.INVOCATION_ID, UUID.randomUUID().toString())
            putString(PluginToolInvokeKeys.TOOL, tool)
            putString(PluginToolInvokeKeys.ARGUMENTS_JSON, arguments.toString())
            putString(PluginToolInvokeKeys.INPUTS_JSON, "[]")
            putString(PluginToolInvokeKeys.OUTPUTS_JSON, "[]")
            putLong(PluginToolInvokeKeys.TIMEOUT_MS, ChimeToolCatalog.TIMEOUT_MS)
            putString(PluginToolInvokeKeys.CONSUMER, PluginToolValues.CONSUMER_SCRIPT)
        }
        val accepted = provider.invoke(request, emptyArray(), callback)
        if (!accepted.getBoolean(PluginToolAcceptKeys.ACCEPTED)) {
            return Answer(accepted.getString(PluginToolAcceptKeys.ERROR_CODE), accepted.getString(PluginToolAcceptKeys.ERROR_MESSAGE), null)
        }
        assertTrue("$tool did not answer within 30 s", done.await(30, TimeUnit.SECONDS))
        return answer.get()
    }

    private fun <T> withProvider(block: (IPluginToolProvider) -> T): T {
        val intent = Intent(PluginToolContract.SERVICE_ACTION).setPackage(context.packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(intent, 0)
        assertEquals("the tool provider discovery must resolve exactly one service", 1, matches.size)
        val service = matches.single().serviceInfo
        assertEquals(ChimeToolService::class.java.name, service.name)
        assertEquals("org.autojs.permission.PLUGIN", service.permission)
        assertEquals(context.packageName, service.processName)
        val latch = CountDownLatch(1)
        val binder = AtomicReference<IBinder?>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                binder.set(service)
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        assertTrue(context.bindService(Intent(intent).setComponent(ComponentName(service.packageName, service.name)), connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("timed out binding the tool provider", latch.await(15, TimeUnit.SECONDS))
            assertNotNull(binder.get())
            return block(IPluginToolProvider.Stub.asInterface(MarshallingBinder(binder.get()!!)))
        } finally {
            context.unbindService(connection)
        }
    }

    private fun log(text: String) {
        Log.i("ChimeDeviceRun", "tools $text")
    }

    private companion object {
        const val TAG = "p41-tool-test"
    }
}
