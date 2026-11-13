package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.ThreeChimeAlarmInfoService
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.service.ThreeChimeAlarmService
import org.autojs.plugin.alarm.api.AlarmCapabilityKeys
import org.autojs.plugin.alarm.api.AlarmContract
import org.autojs.plugin.alarm.api.AlarmFeatures
import org.autojs.plugin.alarm.api.IAlarmPlugin
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The host-facing activation and discovery contract against the installed APK (roadmap P0.4, P2.5): the Wake activity,
 * discovery of exactly one service per formal action and category, explicit binding with the expected Binder descriptors,
 * `getInfo()` marshalled through a real Parcel with the transaction code the host uses, and the `IAlarmPlugin` Binder
 * with its capabilities (`alarmFeatures` = `["alarms"]`, the permission states). [AlarmPluginBinderTest] covers the methods.
 *
 * The services run in the plugin's own process, so the calls below use `transact` with explicit Parcels or a
 * [MarshallingBinder] instead of plain `asInterface`, which would return the local object and skip marshalling.
 *
 * zh-CN: 针对已安装 APK 验证面向宿主的激活与发现契约 (路线图 P0.4, P2.5): Wake Activity, 每组正式 action 与 category 恰好发现一个服务,
 * 显式绑定与 Binder 描述符, 以宿主使用的事务号经真实 Parcel 编组的 `getInfo()`, 以及 `IAlarmPlugin` Binder 与其能力 (`alarmFeatures` 为
 * `["alarms"]`, 权限状态). 各方法由 [AlarmPluginBinderTest] 覆盖. 服务与测试同进程, 因此下文用显式 Parcel 调用 `transact` 或经
 * [MarshallingBinder], 不直接用会返回本地对象而跳过编组的 `asInterface`.
 */
@RunWith(AndroidJUnit4::class)
class ThreeChimeAlarmContractTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val packageName: String
        get() = context.packageName

    @Test
    fun wakeActivityFollowsTheHostActivationContract() {
        val metaData = requireNotNull(context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData)
        assertEquals(".WakeActivity", metaData.getString("org.autojs.plugin.WAKE_ACTIVITY"))
        assertEquals(context.getString(R.string.plugin_author), metaData.getString("org.autojs.plugin.info.AUTHOR"))

        val component = ComponentName(packageName, "$packageName.WakeActivity")
        val activityInfo = context.packageManager.getActivityInfo(component, 0)
        assertTrue(activityInfo.exported)
        assertTrue(activityInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, activityInfo.permission)
        assertEquals(android.R.style.Theme_NoDisplay, activityInfo.theme)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH != 0)

        val matches = queryActivities(Intent("org.autojs.plugin.action.WAKE").addCategory(Intent.CATEGORY_DEFAULT).setPackage(packageName))
        assertEquals("the WAKE action resolves exactly one activity", listOf(component.className), matches)
    }

    @Test
    fun infoServiceAnswersTheAlarmIdentity() {
        val serviceInfo = discoverSingleService(
            ThreeChimeAlarmContract.INFO_ACTION, ThreeChimeAlarmContract.INFO_CATEGORY, ThreeChimeAlarmInfoService::class.java.name,
        )
        assertEquals(ThreeChimeAlarmContract.REQUIRED_HOST_VERSION.toString(), serviceInfo.metaData?.get(PluginCapabilityKeys.REQUIRES_HOST_VERSION)?.toString())
        withBoundService(serviceInfo) { binder ->
            assertEquals(IPluginInfoProvider.DESCRIPTOR, binder.interfaceDescriptor)
            val info = transactPluginInfo(binder)
            assertIdentity(info)
            val capabilities = requireNotNull(info.capabilities)
            assertEquals(
                setOf(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ThreeChimeAlarmContract.CAPABILITY_CONTRACT_VERSION, ThreeChimeAlarmContract.CAPABILITY_FEATURES),
                capabilities.keySet(),
            )
            assertEquals(ThreeChimeAlarmContract.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
            assertEquals(1, capabilities.getInt(ThreeChimeAlarmContract.CAPABILITY_CONTRACT_VERSION))
            // Roadmap T25: version 1.1.0 adds timers and stopwatch once they work end to end (P7.3).
            assertArrayEquals(FEATURES, capabilities.getStringArray(ThreeChimeAlarmContract.CAPABILITY_FEATURES))
        }
    }

    @Test
    fun capabilityServiceAnswersTheAlarmContract() {
        val serviceInfo = discoverSingleService(
            ThreeChimeAlarmContract.SERVICE_ACTION, ThreeChimeAlarmContract.SERVICE_CATEGORY, ThreeChimeAlarmService::class.java.name,
        )
        assertEquals(ThreeChimeAlarmContract.REQUIRED_HOST_VERSION.toString(), serviceInfo.metaData?.get(PluginCapabilityKeys.REQUIRES_HOST_VERSION)?.toString())
        withBoundService(serviceInfo) { binder ->
            assertEquals(IAlarmPlugin.DESCRIPTOR, binder.interfaceDescriptor)
            assertEquals(ThreeChimeAlarmContract.CAPABILITY_DESCRIPTOR, binder.interfaceDescriptor)
            // Marshalled through Parcels: the local object is hidden from asInterface (see AlarmPluginBinderTest).
            val plugin = IAlarmPlugin.Stub.asInterface(MarshallingBinder(binder))
            assertIdentity(plugin.info)
            val capabilities = plugin.capabilities
            assertEquals(ThreeChimeAlarmContract.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
            assertEquals(1, capabilities.getInt(ThreeChimeAlarmContract.CAPABILITY_CONTRACT_VERSION))
            assertArrayEquals(FEATURES, capabilities.getStringArray(ThreeChimeAlarmContract.CAPABILITY_FEATURES))
            val permissions = requireNotNull(capabilities.getBundle(AlarmCapabilityKeys.PERMISSIONS))
            assertEquals(AlarmContract.PERMISSIONS, permissions.keySet())
        }
    }

    @Test
    fun repeatedBindingsAnswerTheSameIdentity() {
        val serviceInfo = discoverSingleService(
            ThreeChimeAlarmContract.INFO_ACTION, ThreeChimeAlarmContract.INFO_CATEGORY, ThreeChimeAlarmInfoService::class.java.name,
        )
        val ids = (1..3).map {
            var id: String? = null
            withBoundService(serviceInfo) { binder -> id = transactPluginInfo(binder).id }
            id
        }
        assertEquals(List(3) { ThreeChimeAlarmContract.PLUGIN_ID }, ids)
    }

    private fun assertIdentity(info: PluginInfo) {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        assertEquals("3-Chime Alarm", info.name)
        assertEquals(context.getString(R.string.plugin_name), info.name)
        assertEquals(context.getString(R.string.plugin_description), info.description)
        assertEquals(ThreeChimeAlarmContract.INSTRUCTION_REFERENCE, info.instruction)
        assertTrue("the instruction resource exists", context.resources.getIdentifier("plugin_instruction", "raw", packageName) != 0)
        assertEquals("SuperMonster003", info.author)
        assertEquals(ThreeChimeAlarmContract.PLUGIN_ID, info.id)
        assertEquals(context.getString(R.string.plugin_id), info.id)
        assertEquals(ThreeChimeAlarmContract.ENGINE, info.engine)
        assertEquals(ThreeChimeAlarmContract.VARIANT, info.variant)
        assertEquals(packageInfo.versionName, info.versionName)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        assertEquals(versionCode, info.versionCode)
        assertEquals(context.getString(R.string.plugin_version_date), info.versionDate)
        // Roadmap T20: no native code, so an explicit empty array (never null).
        assertArrayEquals(emptyArray<String>(), requireNotNull(info.supportedAbis))
    }

    /** `getInfo()` is the first transaction of IPluginInfoProvider. */
    private fun transactPluginInfo(binder: IBinder): PluginInfo {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(IPluginInfoProvider.DESCRIPTOR)
            assertTrue("getInfo() transaction must be handled", binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0))
            reply.readException()
            assertTrue("getInfo() must not return null", reply.readInt() != 0)
            return pluginInfoCreator.createFromParcel(reply)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun queryActivities(intent: Intent): List<String> {
        @Suppress("DEPRECATION")
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.name }
    }

    private fun discoverSingleService(action: String, category: String, expectedClassName: String): ServiceInfo {
        val intent = Intent(action).addCategory(category).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
        assertEquals("$action / $category must resolve exactly one service: ${matches.map { it.serviceInfo.name }}", 1, matches.size)
        val serviceInfo = matches.single().serviceInfo
        assertEquals(packageName, serviceInfo.packageName)
        assertEquals(expectedClassName, serviceInfo.name)
        assertTrue("$expectedClassName must be exported", serviceInfo.exported)
        assertTrue("$expectedClassName must be enabled", serviceInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, serviceInfo.permission)
        assertEquals("services run in the plugin's default process", packageName, serviceInfo.processName)
        return serviceInfo
    }

    private fun withBoundService(serviceInfo: ServiceInfo, block: (IBinder) -> Unit) {
        val binderReference = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binderReference.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit

            override fun onNullBinding(name: ComponentName) {
                connected.countDown()
            }
        }
        val explicit = Intent().setComponent(ComponentName(serviceInfo.packageName, serviceInfo.name))
        assertTrue("bindService returned false", context.bindService(explicit, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("timed out waiting for ${serviceInfo.name}", connected.await(10, TimeUnit.SECONDS))
            block(requireNotNull(binderReference.get()) { "${serviceInfo.name} returned a null binding" })
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"

        /** Roadmap T25 / P7.3: what version 1.1.0 declares. zh-CN: 路线图 T25 / P7.3: 1.1.0 版声明的功能. */
        val FEATURES = arrayOf(AlarmFeatures.ALARMS, AlarmFeatures.TIMERS, AlarmFeatures.STOPWATCH, AlarmFeatures.HOLIDAYS)

        @Suppress("UNCHECKED_CAST")
        val pluginInfoCreator: Parcelable.Creator<PluginInfo> =
            PluginInfo::class.java.getField("CREATOR").get(null) as Parcelable.Creator<PluginInfo>
    }

}
