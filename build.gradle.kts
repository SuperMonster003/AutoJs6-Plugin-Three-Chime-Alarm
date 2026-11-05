// @Hint: declared here, applied by the modules.
//  ! The platform-versions plugin publishes the AGP version as a system property before this script runs, so the plugin
//  ! version below comes from one central decision and is never hardcoded. The Compose compiler plugin joins in roadmap P3.1.
//  ! zh-CN: platform-versions 插件在本脚本执行前以系统属性发布 AGP 版本, 因此下列插件版本来自同一个中央决策, 不做硬编码.
//  ! Compose 编译器插件在路线图 P3.1 加入.
plugins {
    id("com.android.application") version System.getProperty("gradle.agp.version") apply false
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
