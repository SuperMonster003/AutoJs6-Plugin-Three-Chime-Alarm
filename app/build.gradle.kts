import com.android.apksig.ApkVerifier
import org.gradle.api.provider.Property
import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.three.chime.alarm"

// Minimum host build (roadmap 4.5): the baseline 6640 (host e195d5c7f8, whose VERSION_BUILD is the versionCode) until roadmap
// P1.3 writes the temporary value that contains the host contract and P6.4 the final one. Keep it in sync with
// ThreeChimeAlarmContract.REQUIRED_HOST_VERSION and .readme/common.json.
// zh-CN: 最低宿主构建号 (路线图 4.5): 基线 6640 (宿主 e195d5c7f8, 其 VERSION_BUILD 即 versionCode), 路线图 P1.3 写入含宿主契约的临时值,
// P6.4 回填最终值. 与 ThreeChimeAlarmContract.REQUIRED_HOST_VERSION 及 .readme/common.json 保持一致.
val requiredHostVersion = 6640

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

// The host contract AARs (roadmap 4.5): common-plugin-api now, alarm-api from roadmap P2 (built from the host contract module
// of P1.1). Release AARs only, staged under libs/ and pinned by locks/host-api-aars.lock.
// zh-CN: 宿主契约 AAR (路线图 4.5): 现在只有 common-plugin-api, alarm-api 自路线图 P2 起加入 (来自 P1.1 的宿主契约模块).
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

android {
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

    buildFeatures {
        aidl = true
        buildConfig = true
        resValues = true
    }

    // Single APK (roadmap T20): the plugin ships only bytecode and ordinary resources, so there is no `splits` block and
    // PluginInfo.supportedAbis is an explicit empty array; appendDigestToReleasedFiles verifies the APK has no `lib/`.
    // zh-CN: 单 APK (路线图 T20): 插件只有字节码与普通资源, 因此没有 `splits` 块, PluginInfo.supportedAbis 为显式空数组;
    // appendDigestToReleasedFiles 校验 APK 不含 `lib/`.
    packaging {
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
    implementation(files(hostApiAars))

    implementation(libs.core.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    // Single signed release APK (roadmap T20, the 3-Block Compose form): exactly one APK, a valid signature, no native code,
    // then `{ROOT_PROJECT_NAME}-v{VERSION_NAME}-{CRC32}.apk` in releases/.
    // zh-CN: 单个已签名 release APK (路线图 T20, 3-Block Compose 形态): 恰好一个 APK, 签名有效, 不含原生代码, 然后以
    // `{ROOT_PROJECT_NAME}-v{VERSION_NAME}-{CRC32}.apk` 放入 releases/.
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
            ZipFile(apk).use { zip ->
                val natives = zip.entries().asSequence().map { it.name }.filter { it.startsWith("lib/") || it.endsWith(".so") }.toList()
                check(natives.isEmpty()) { "Roadmap T20: ${apk.name} must not contain native code, found ${natives.sorted()}" }
            }
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
