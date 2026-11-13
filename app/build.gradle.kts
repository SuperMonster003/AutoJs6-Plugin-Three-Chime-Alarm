import com.android.apksig.ApkVerifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins {
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.three.chime.alarm"

// Minimum host build (roadmap 4.5, T33): the temporary 6641, the first host build that contains the alarm contract of roadmap
// P1.1 - P1.3 (6640 is the baseline e195d5c7f8 without it); roadmap P6.4 writes the final value. Keep it in sync with
// ThreeChimeAlarmContract.REQUIRED_HOST_VERSION and .readme/common.json.
// zh-CN: 最低宿主构建号 (路线图 4.5, T33): 临时值 6641, 即第一个包含路线图 P1.1 - P1.3 闹钟契约的宿主构建 (6640 是不含这些提交的基线
// e195d5c7f8); 路线图 P6.4 回填最终值. 与 ThreeChimeAlarmContract.REQUIRED_HOST_VERSION 及 .readme/common.json 保持一致.
val requiredHostVersion = 6641

val buildTypeDebug = "debug"
val buildTypeRelease = "release"

fun File.sha256(): String = inputStream().buffered().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

// Lock files (new repository specification 5.5): `format=1` first, unique keys, comments and blank lines skipped.
// zh-CN: 锁文件 (新仓库规范 5.5): 首行 `format=1`, 键唯一, 跳过注释与空行.
fun readLock(path: String): Map<String, String> {
    val lockFile = rootProject.file(path)
    check(lockFile.isFile) { "Missing $path" }
    val entries = LinkedHashMap<String, String>()
    lockFile.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { line ->
        val key = line.substringBefore('=').trim()
        check(line.contains('=') && entries.put(key, line.substringAfter('=').trim()) == null) { "Malformed or duplicate entry in $path: $line" }
    }
    check(entries["format"] == "1") { "Unsupported lock format in $path" }
    return entries
}

fun verifiedFile(path: String, expected: String?, label: String): File {
    val file = rootProject.file(path)
    check(file.isFile) { "Missing $path ($label)" }
    check(expected != null && expected.matches(Regex("[0-9a-f]{64}"))) { "Invalid SHA-256 for $label" }
    val actual = file.sha256()
    check(actual == expected) { "SHA-256 mismatch for $path: expected $expected, actual $actual" }
    return file
}

// The host contract AARs (roadmap 4.5): alarm-api (roadmap P1.1) and common-plugin-api, both staged in roadmap P2 from the
// same host commit, and plugin-tool-api (roadmap P4.1) for the alarm_* tools. Release AARs only, staged under libs/ and
// pinned by locks/host-api-aars.lock.
// zh-CN: 宿主契约 AAR (路线图 4.5): alarm-api (路线图 P1.1) 与 common-plugin-api, 均于路线图 P2 从同一宿主提交暂存, 以及供 alarm_* 工具使用的
// plugin-tool-api (路线图 P4.1).
// 只接受 release AAR, 暂存于 libs/ 并由 locks/host-api-aars.lock 固定.
val hostApiAars: List<File> = run {
    val entries = readLock("locks/host-api-aars.lock")
    val names = entries.keys.filter { it.endsWith(".file") }.map { it.removeSuffix(".file") }.sorted()
    check(entries.keys == setOf("format") + names.flatMap { listOf("$it.file", "$it.sha256") }) { "Unexpected keys in locks/host-api-aars.lock" }
    names.map { name ->
        val fileName = entries.getValue("$name.file")
        check(fileName == File(fileName).name && fileName.endsWith(".aar") && !fileName.endsWith("-debug.aar")) { "Invalid $name.file" }
        verifiedFile("libs/$fileName", entries["$name.sha256"], name)
    }
}

run {
    // The extra TLS trust anchor of res/xml/network_security_config.xml (roadmap P8.2, the holiday data refresh on Android
    // 7.0; copied from 3-Macaw Speech cf32b5e): the ISRG Root X1 certificate in DER form, locked in locks/trust-anchors.lock
    // and checked again by TrustAnchorsTest.
    // zh-CN: res/xml/network_security_config.xml 的额外 TLS 信任锚 (路线图 P8.2, Android 7.0 上的节假日数据刷新; 照搬 3-Macaw Speech
    // cf32b5e): DER 格式的 ISRG Root X1 证书, 锁定在 locks/trust-anchors.lock 中, TrustAnchorsTest 再次核对.
    val entries = readLock("locks/trust-anchors.lock")
    val names = entries.keys.filter { it.endsWith(".path") }.map { it.removeSuffix(".path") }.sorted()
    check(names == listOf("isrg-root-x1")) { "locks/trust-anchors.lock must list isrg-root-x1" }
    check(entries.keys == setOf("format") + names.flatMap { name -> listOf("path", "sha256", "url", "subject", "notAfter").map { "$name.$it" } }) {
        "Unexpected keys in locks/trust-anchors.lock"
    }
    names.forEach { name ->
        val path = entries.getValue("$name.path")
        check(path == "app/src/main/res/raw/${name.replace('-', '_')}.der") { "trust anchor $name must be the DER file of res/raw" }
        verifiedFile(path, entries["$name.sha256"], "trust anchor $name")
    }
}

// Native code inventory (roadmap T34): the plugin has no native code of its own, but Compose ui-graphics depends on
// androidx.graphics:graphics-path, whose libandroidx.graphics.path.so is packaged for the four Android ABIs. The APK stays
// one universal package without ABI splits; the release gate verifies that exactly these libraries are present, stored
// uncompressed and 16 KB page aligned (ELF PT_LOAD and zip data offset), the form of 3-Block Compose and 3-Batch Rename.
// zh-CN: 原生代码清单 (路线图 T34): 插件没有自己的原生代码, 但 Compose ui-graphics 依赖 androidx.graphics:graphics-path, 其
// libandroidx.graphics.path.so 按四个 Android ABI 打包. APK 仍是不拆 ABI 的单个通用包; release 门禁校验恰好只有这些库, 未压缩存储且按
// 16 KB 页对齐 (ELF PT_LOAD 与 zip 数据偏移), 与 3-Block Compose 及 3-Batch Rename 相同.
val nativeAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
val allowedNativeLibraries = listOf("libandroidx.graphics.path.so")
val nativePageAlignment = 16384L

fun elfMinimumLoadAlignment(bytes: ByteArray): Long {
    check(bytes.size > 0x40 && bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
        "Not an ELF file"
    }
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val is64 = bytes[4].toInt() == 2
    val programHeaderOffset = if (is64) buffer.getLong(0x20) else (buffer.getInt(0x1C).toLong() and 0xFFFFFFFFL)
    val entrySize = buffer.getShort(if (is64) 0x36 else 0x2A).toInt() and 0xFFFF
    val entryCount = buffer.getShort(if (is64) 0x38 else 0x2C).toInt() and 0xFFFF
    var minimum = Long.MAX_VALUE
    repeat(entryCount) { index ->
        val offset = (programHeaderOffset + index.toLong() * entrySize).toInt()
        if (buffer.getInt(offset) == 1) { // PT_LOAD
            val alignment = if (is64) buffer.getLong(offset + 48) else (buffer.getInt(offset + 28).toLong() and 0xFFFFFFFFL)
            minimum = minOf(minimum, alignment)
        }
    }
    check(minimum != Long.MAX_VALUE) { "No PT_LOAD segment" }
    return minimum
}

/** Data offsets of the `lib/` entries inside [apk], from the central directory and local headers. zh-CN: [apk] 中 `lib/` 条目的数据偏移. */
fun nativeEntryDataOffsets(apk: File): Map<String, Long> = RandomAccessFile(apk, "r").use { file ->
    val length = file.length()
    val tailLength = minOf(length, 22L + 65535L).toInt()
    val tail = ByteArray(tailLength).also { file.seek(length - tailLength); file.readFully(it) }
    val tailBuffer = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
    val eocd = (tail.size - 22 downTo 0).firstOrNull { tailBuffer.getInt(it) == 0x06054b50 }
    checkNotNull(eocd) { "End of central directory not found in ${apk.name}" }
    val entryCount = tailBuffer.getShort(eocd + 10).toInt() and 0xFFFF
    val directorySize = tailBuffer.getInt(eocd + 12).toLong() and 0xFFFFFFFFL
    val directoryOffset = tailBuffer.getInt(eocd + 16).toLong() and 0xFFFFFFFFL
    val directory = ByteArray(directorySize.toInt()).also { file.seek(directoryOffset); file.readFully(it) }
    val directoryBuffer = ByteBuffer.wrap(directory).order(ByteOrder.LITTLE_ENDIAN)
    val offsets = LinkedHashMap<String, Long>()
    var position = 0
    repeat(entryCount) {
        check(directoryBuffer.getInt(position) == 0x02014b50) { "Corrupt central directory in ${apk.name}" }
        val nameLength = directoryBuffer.getShort(position + 28).toInt() and 0xFFFF
        val extraLength = directoryBuffer.getShort(position + 30).toInt() and 0xFFFF
        val commentLength = directoryBuffer.getShort(position + 32).toInt() and 0xFFFF
        val localHeaderOffset = directoryBuffer.getInt(position + 42).toLong() and 0xFFFFFFFFL
        val name = String(directory, position + 46, nameLength, Charsets.UTF_8)
        if (name.startsWith("lib/")) {
            val local = ByteArray(30).also { file.seek(localHeaderOffset); file.readFully(it) }
            val localBuffer = ByteBuffer.wrap(local).order(ByteOrder.LITTLE_ENDIAN)
            check(localBuffer.getInt(0) == 0x04034b50) { "Corrupt local header for $name" }
            val localNameLength = localBuffer.getShort(26).toInt() and 0xFFFF
            val localExtraLength = localBuffer.getShort(28).toInt() and 0xFFFF
            offsets[name] = localHeaderOffset + 30 + localNameLength + localExtraLength
        }
        position += 46 + nameLength + extraLength + commentLength
    }
    offsets
}

/** The whitelist check of roadmap T34; returns the library sizes for the log. zh-CN: 路线图 T34 的白名单校验; 返回各库的大小供日志使用. */
fun verifyNativeLibraries(apk: File): Map<String, Long> {
    val expected = nativeAbis.flatMap { abi -> allowedNativeLibraries.map { "lib/$abi/$it" } }.toSet()
    val sizes = LinkedHashMap<String, Long>()
    ZipFile(apk).use { zip ->
        val entries = zip.entries().asSequence().filter { it.name.startsWith("lib/") || it.name.endsWith(".so") }.toList()
        check(entries.map { it.name }.toSet() == expected) {
            "Roadmap T34: unexpected native library set in ${apk.name}: expected ${expected.sorted()}, actual ${entries.map { it.name }.sorted()}"
        }
        entries.sortedBy { it.name }.forEach { entry ->
            check(entry.method == ZipEntry.STORED) { "${entry.name} must be stored uncompressed so it can be mapped from the APK" }
            val alignment = elfMinimumLoadAlignment(zip.getInputStream(entry).use { it.readBytes() })
            check(alignment >= nativePageAlignment) { "${entry.name} PT_LOAD alignment $alignment is below $nativePageAlignment" }
            sizes[entry.name] = entry.size
        }
    }
    nativeEntryDataOffsets(apk).forEach { (name, offset) ->
        check(offset % nativePageAlignment == 0L) { "$name data offset $offset is not $nativePageAlignment-byte aligned in the APK" }
    }
    return sizes
}

// The legal texts of the notices page (roadmap P3.5): LICENSE (MPL-2.0) and THIRD_PARTY_NOTICES.md of this repository,
// copied into the generated assets/legal/ folder of every variant.
// zh-CN: 声明页的法律文本 (路线图 P3.5): 本仓库的 LICENSE (MPL-2.0) 与 THIRD_PARTY_NOTICES.md, 复制到每个变体生成的 assets/legal/ 文件夹.
val legalTexts: Map<String, File> = mapOf(
    "LICENSE.txt" to rootProject.file("LICENSE"),
    "THIRD_PARTY_NOTICES.md" to rootProject.file("THIRD_PARTY_NOTICES.md"),
)

abstract class LegalAssetsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val names: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copy() {
        val folder = outputDirectory.get().asFile.resolve("legal")
        folder.deleteRecursively()
        folder.mkdirs()
        sources.files.toList().zip(names.get()).forEach { (file, name) -> file.copyTo(folder.resolve(name)) }
    }
}

val generateLegalAssets = tasks.register<LegalAssetsTask>("generateLegalAssets") {
    sources.from(legalTexts.values.toList())
    names.set(legalTexts.keys.toList())
    outputDirectory.set(layout.buildDirectory.dir("generated/legal-assets"))
}

android {
    // Keep only the plugin's 10 languages from the Compose / AndroidX resources (roadmap T22).
    // zh-CN: Compose / AndroidX 资源只保留插件的 10 种语言 (路线图 T22).
    androidResources { localeFilters += setOf("en", "ar", "es", "fr", "ja", "ko", "ru", "zh", "zh-rCN", "zh-rHK", "zh-rTW") }
    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    defaultConfig {
        applicationId = globalApplicationId
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Identity strings come from the build and are never translated (new repository specification 11).
        // zh-CN: 身份字符串来自构建, 不翻译 (新仓库规范第 11 节).
        resValue("string", "app_name", "3-Chime Alarm")
        // Brand name answered as PluginInfo.name; never translated (roadmap P0.1). zh-CN: PluginInfo.name 使用的品牌名, 不翻译 (路线图 P0.1).
        resValue("string", "plugin_name", "3-Chime Alarm")
        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_id", "three-chime-alarm")
        resValue("string", "plugin_engine", "alarm")
        resValue("string", "plugin_variant", "default")
        resValue("string", "plugin_requires_host_version", "$requiredHostVersion")
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))
    }

    testOptions { unitTests.isIncludeAndroidResources = true }

    lint {
        abortOnError = true
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    compileOptions {
        // java.time for the alarm core on API 24 / 25 devices (roadmap T47). zh-CN: 闹钟核心在 API 24 / 25 设备上使用 java.time (路线图 T47).
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        resValues = true
    }

    // Single APK (roadmap T20, T34): no `splits` block and PluginInfo.supportedAbis is an explicit empty array; the only
    // native code is the Compose graphics-path library, kept uncompressed so it maps straight from the APK.
    // zh-CN: 单 APK (路线图 T20, T34): 没有 `splits` 块, PluginInfo.supportedAbis 为显式空数组; 唯一的原生代码是 Compose 的
    // graphics-path 库, 不压缩存储, 直接从 APK 映射.
    packaging {
        jniLibs.useLegacyPackaging = false
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
    }

    bundle {
        language.enableSplit = false
        density.enableSplit = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(generateLegalAssets, LegalAssetsTask::outputDirectory)
        variant.outputs.forEach { output ->
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    "${rootProject.name}-v$version.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)

    implementation(files(hostApiAars))

    implementation(libs.core.ktx)
    implementation(libs.lib.recur)
    implementation(libs.kotlinx.coroutines.android)

    // Jetpack Compose (roadmap T22): the BOM is the single version source; activity-compose follows it.
    // zh-CN: Jetpack Compose (路线图 T22): BOM 是唯一的版本来源, activity-compose 随之.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.ui.tooling.preview)
    // The HCT accent rule shared by the standalone plugins. zh-CN: 各独立插件共用的 HCT 强调色规则.
    implementation(libs.material.color.utilities)

    testImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    // Compose UI tests pull Espresso 3.5.0, whose idling sync reflects on InputManager.getInstance (gone on API 36).
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    // Single signed release APK (roadmap T20, T34, the 3-Block Compose form): exactly one APK, a valid signature, only the
    // whitelisted native library (uncompressed, 16 KB aligned), then `{ROOT_PROJECT_NAME}-v{VERSION_NAME}-{CRC32}.apk` in releases/.
    // zh-CN: 单个已签名 release APK (路线图 T20, T34, 3-Block Compose 形态): 恰好一个 APK, 签名有效, 只含白名单中的原生库
    // (未压缩, 16 KB 对齐), 然后以 `{ROOT_PROJECT_NAME}-v{VERSION_NAME}-{CRC32}.apk` 放入 releases/.
    register("appendDigestToReleasedFiles") {
        group = "distribution"
        description = "Builds and verifies the single signed release APK of 3-Chime Alarm, then appends its CRC32 digest"
        dependsOn("assembleRelease")
        doLast {
            check(signs.isValid) { "Release signing is not configured (sign.properties); refusing to collect an unsigned APK" }
            val extension = utils.FILE_EXTENSION_APK
            val prefix = "${rootProject.name}-v${versions.appVersionName.replace("\\s".toRegex(), "-")}".lowercase()
            val expectedName = "$prefix.$extension"
            val source = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease").get().asFile
            val apks = source.listFiles { candidate -> candidate.extension == extension }.orEmpty()
            check(apks.map { it.name } == listOf(expectedName)) {
                "Unexpected release APK set in $source: expected [$expectedName], actual ${apks.map { it.name }.sorted()}"
            }
            val apk = apks.single()
            val verification = ApkVerifier.Builder(apk).build().verify()
            check(verification.isVerified) { "Invalid or unsigned release APK ${apk.name}: ${verification.errors}" }
            verifyNativeLibraries(apk).forEach { (name, size) -> println("Native library: $name $size B") }
            val collectedName = "$prefix-${utils.digestCRC32(apk)}.$extension"
            val destination = file("$rootDir/${buildTypeRelease}s")
            check(destination.isDirectory || destination.mkdirs()) { "Cannot create $destination" }
            apk.copyTo(destination.resolve(collectedName), overwrite = true)
            destination.listFiles { file -> file.name.startsWith("$prefix-") && file.extension == extension }
                .orEmpty()
                .filter { it.name != collectedName }
                .forEach { stale -> check(stale.delete()) { "Cannot remove superseded artifact: $stale" } }
            println("Destination: $destination")
            println(collectedName)
        }
    }
}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}
