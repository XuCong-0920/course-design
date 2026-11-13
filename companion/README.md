# companion 模块（伴生模块）

《移动互联网应用开发实战》（1 学分）要求的伴生模块，与主 App 放在**同一个仓库**，以目录区分。

## 它是什么

一个 Android Library 模块，负责「主 App 之外的周边能力」，当前承担两件事：

1. **与主 App 的数据契约**：`DueDataProvider` 接口 + `CompanionBridge` 单例注册点。
2. **提醒能力的公共常量**：通知渠道 id（`REMINDER_CHANNEL_ID`）。

## 依赖方向（重要）

```
:app  ──依赖──▶  :companion          （主 App 依赖模块）
:companion ──✗──▶ :app               （模块不反向依赖主 App）
```

模块里没有 `import com.xucong.wordmemory.data.*`，只认 `DueDataProvider` 接口。这样做的三个理由：

- 模块能单独编译、单独跑测试，不拖上 Room/KSP；
- 以后要把模块拆成独立 APK 或独立进程，只需把 `bind()` 的实现换成 AIDL / ContentProvider，调用方（`MineFragment`）一行不用改；
- 依赖成环是 Android 多模块最常见的翻车点，这里从一开始就掐掉。

## 接口

```kotlin
interface DueDataProvider {
    suspend fun totalWordCount(): Int
    suspend fun dueCount(now: Long = System.currentTimeMillis()): Int
}

object CompanionBridge {
    const val MODULE_NAME = "companion"
    const val REMINDER_CHANNEL_ID = "word_review_reminder"
    const val NOT_BOUND = -1          // 未绑定时返回，避免把 0 当成真实数据

    fun bind(provider: DueDataProvider)   // 主 App 在 Application.onCreate 里注入
    fun unbind()
    fun isReady(): Boolean
    suspend fun totalWordCount(): Int
    suspend fun dueCount(now: Long): Int
}
```

## 主 App 侧怎么接

`WordMemoryApp.onCreate()` 里：

```kotlin
CompanionBridge.bind(object : DueDataProvider {
    override suspend fun totalWordCount(): Int = repository.totalWordCount()
    override suspend fun dueCount(now: Long): Int = repository.dueNow(now)
})
```

「我的」页把结果直接展示出来（`伴生模块 companion 已连接：主 App 共 22 个词，当前 20 个待复习`）——**数字能显示，就说明绑定链路真的通了**，这是验收时可以当场指认的证据。

## 后续计划

- 第 8–9 次课：把每日提醒的通知渠道与调度从 `:app` 迁移到本模块，模块对外暴露 `ReminderApi.schedule()/cancel()`。
- 可选：模块独立的设置页（提醒时间、免打扰时段），以 `Fragment` 形式提供、由主 App 挂载。
