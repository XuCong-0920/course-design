# 单词记忆 App · B4（Android 课程设计）

- **学号 / 姓名**：202405100228 / 徐聪
- **课程**：《Android 移动终端应用开发技术》（2 学分，主 App）+《移动互联网应用开发实战》（1 学分，伴生模块）
- **选题**：B4 单词记忆（指定题目）
- **一句话定位**：本地优先的单词记忆 App —— 用艾宾浩斯遗忘曲线调度每日复习队列，支持词库导入、拼写/选择测试与学习统计。

## 核心功能

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| F1 | 词库管理 | 词库列表 / 新建 / 删除；单词增删改（单词、音标、释义、例句） |
| F2 | 词库导入 | 内置示例词库 + CSV / JSON 导入（容错：跳过坏行、导入结果汇总） |
| F3 | 每日复习队列 | 基于遗忘曲线的到期单词队列；"认识 / 模糊 / 不认识"三档反馈驱动调度 |
| F4 | 测试模式 | 拼写题 + 选择题，限时/不限时，交卷后给出正确率与错词回炉 |
| F5 | 学习统计 | 今日待复习数、已复习数、连续学习天数、近 7 天正确率 |

**强制技术点（B4 要求）**

- **自定义算法**：SRS（间隔重复）调度算法 —— 见 `docs/02-技术方案.md`，不依赖第三方库。
- **通知**：每日复习提醒 + 逾期提醒（WorkManager + Notification，通知内可直接"开始复习 / 稍后提醒"）。

## 技术栈与架构

- 语言：Kotlin；UI：XML View + ViewBinding（Jetpack Compose 作为备选，不混用）
- Jetpack：Navigation、ViewModel、LiveData/Flow、Room、WorkManager、DataStore、RecyclerView
- 架构：MVVM + Repository，协程 + 结构化并发，单 Activity + 多 Fragment
- 无网络依赖（词库导入走本地文件 / SAF），保证离线可用

## 界面清单（≥4 个，含"列表 → 详情"）

1. 词库列表（Home）→ 2. 单词详情 / 词库内单词列表
3. 今日复习（卡片翻转 + 三档反馈）
4. 测试模式（选择 / 拼写）
5. 学习统计（图表 + 连续天数）
6. 设置（每日提醒时间、每日上限、导入导出）

## 伴生模块设想（1 学分）

- **首选 P3 通知提醒中心**：独立可运行的通知调度单元，读取主 App 的"今日待复习数"，定时推送提醒，通知内提供"开始复习 / 稍后 10 分钟 / 今日免打扰"三个动作（WorkManager + Notification）。
- **备选 P1 桌面小组件**：主屏 AppWidget 展示"今日待复习 N 个"与一键进入复习（AppWidget / RemoteViews）。
- 与主 App 的数据打通：通过同一 Room 数据库的只读 DAO（或 ContentProvider）读取待复习统计，模块可独立启动演示。

## 仓库结构（主 App 与伴生模块同仓库，目录区分）

```
course-design/
├─ app/                          # 主 App（2 学分）
│  ├─ build.gradle.kts           # applicationId com.xucong.wordmemory
│  └─ src/main/
│     ├─ java/com/xucong/wordmemory/
│     │  ├─ MainActivity.kt              # 唯一 Activity：NavHost + BottomNav
│     │  └─ ui/
│     │     ├─ common/BasePlaceholderFragment.kt
│     │     ├─ home/HomeFragment.kt      # 今日
│     │     ├─ review/ReviewFragment.kt  # 复习
│     │     ├─ stats/StatsFragment.kt    # 统计
│     │     └─ mine/MineFragment.kt      # 我的（含伴生模块连通性验证）
│     └─ res/
│        ├─ layout/activity_main.xml、fragment_placeholder.xml
│        ├─ navigation/nav_main.xml      # 4 个目的地
│        ├─ menu/menu_bottom_nav.xml     # 底部导航，item id 与目的地 id 一致
│        └─ values/strings.xml、themes.xml
├─ companion/                    # 伴生模块（1 学分，Android Library）
│  └─ src/main/java/com/xucong/wordmemory/companion/CompanionBridge.kt
├─ gradle/wrapper/               # Gradle 9.6.0 wrapper
├─ docs/                         # 需求、技术方案、测试记录、答辩材料
├─ README.md
├─ .gitignore
└─ .gitattributes
```

## 构建与运行

环境基线：

| 项 | 版本 |
| --- | --- |
| AGP | 9.4.0 |
| Gradle | 9.6.0（wrapper 已提交，无需本机装 Gradle） |
| compileSdk / targetSdk | 37 |
| minSdk | 24 |
| JDK | 17+（Android Studio 自带 JBR 即可） |
| Kotlin | AGP 9 **内置**，不额外应用 `org.jetbrains.kotlin.android` |

命令行构建：

```bash
./gradlew :app:assembleDebug      # 产物 app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug       # 连接设备后直接安装
```

用 Android Studio 打开根目录即可，首次同步需联网下载 AGP 与 androidx 依赖；
`local.properties`（SDK 路径）由 Android Studio 自动生成，已在 `.gitignore` 中忽略。

> **踩坑记录**：AGP 9 起内置 Kotlin 支持，若同时应用 `org.jetbrains.kotlin.android`
> 会报 `Cannot add extension with name 'kotlin'`。本仓库因此只在根 `build.gradle.kts`
> 声明 `com.android.application` / `com.android.library`，不再声明 Kotlin 插件。

## 提交规范

`feat:` 新功能 · `fix:` 修复 · `docs:` 文档 · `refactor:` 重构 · `test:` 测试 · `chore:` 构建/杂项

示例：`feat: 完成词库列表页与 Room 数据层`、`feat: 实现 SRS 调度算法与单元测试`、`docs: 补充数据库设计说明`

要求：一个功能拆成多次提交（UI → 逻辑 → 联调），全周期 ≥ 15 次有意义的提交，禁止 `update`、`111` 这类信息。

## 里程碑（对应 10 次实验课）

| 次 | 里程碑 | 现场检查点 |
| --- | --- | --- |
| 1 | 选题审核 + 功能清单 + 伴生模块设想 + 建仓首次提交 | 口头讲需求 |
| 2 | 原型图 + 技术选型 + 数据模型 | 出示设计图 |
| 3 | 主 App 骨架 + 导航 + 基础 UI | 当场运行空壳 |
| 4 | 核心功能①：数据持久化（Room） | 演示存 / 取 |
| 5 | 核心功能②：自定义算法（SRS 调度） | 演示真实调度结果 |
| 6 | 核心功能③ + 主流程整合（复习 / 测试串起来） | 演示主流程 |
| 7 | 权限、异常、空态 + 伴生模块立项与骨架 | 演示权限降级 + 模块骨架 |
| 8 | 伴生模块核心功能 + 主 App UI 打磨 | 演示模块读写主 App 数据 |
| 9 | 伴生模块完成 + 录屏 / 问答卷 / PPT | 收集异步包 |
| 10 | 正式提交 + 抽查答辩 | 面谈抽查 |
