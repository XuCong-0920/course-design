// 顶层构建脚本：只声明插件版本，不在这里应用
// 注意：AGP 9 起内置 Kotlin 支持，**不要再应用 org.jetbrains.kotlin.android**
// （AGP 9 已移除 BaseExtension，KGP 2.2 与之不兼容）。
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("com.android.library") version "9.4.0" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}
