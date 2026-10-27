// 顶层构建脚本：只声明插件版本，不在这里应用
// 注意：AGP 9 起内置 Kotlin 支持，**不要再应用 org.jetbrains.kotlin.android**，
// 否则会报 "Cannot add extension with name 'kotlin'"（官方迁移文档：migrate-to-built-in-kotlin）。
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("com.android.library") version "9.4.0" apply false
}
