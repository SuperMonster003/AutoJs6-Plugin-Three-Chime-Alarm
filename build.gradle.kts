// @Hint: declared here, applied by the modules.
//  ! The platform-versions plugin publishes the AGP version as a system property before this script runs, so the plugin
//  ! versions below come from one central decision and are never hardcoded. The Compose compiler plugin follows the Kotlin version the
//  ! same way (roadmap T22, P3.1).
//  ! zh-CN: platform-versions 插件在本脚本执行前以系统属性发布 AGP 版本, 因此下列插件版本来自同一个中央决策, 不做硬编码.
//  ! Compose 编译器插件同样跟随 Kotlin 版本 (路线图 T22, P3.1).
plugins {
    id("com.android.application") version System.getProperty("gradle.agp.version") apply false
    id("org.jetbrains.kotlin.plugin.compose") version System.getProperty("gradle.kotlin.version") apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

tasks {
    register<Delete>("clean").configure {
        delete(rootProject.layout.buildDirectory)
    }
}
