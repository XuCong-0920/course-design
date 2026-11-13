package com.xucong.wordmemory

import android.app.Application
import com.xucong.wordmemory.companion.CompanionBridge
import com.xucong.wordmemory.companion.DueDataProvider
import com.xucong.wordmemory.data.WordRepository
import com.xucong.wordmemory.data.local.WordDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 全局单例容器：只持有数据库与仓库，ViewModel 通过工厂从这里取。
 * 种子的写入放在 Application 里，保证进任意页面时数据都已在准备/已就绪。
 */
class WordMemoryApp : Application() {

    val database: WordDatabase by lazy { WordDatabase.get(this) }

    val repository: WordRepository by lazy { WordRepository(database) }

    override fun onCreate() {
        super.onCreate()
        // 把主 App 的只读数据能力注册给伴生模块：模块只看到 DueDataProvider 接口，
        // 不依赖 Room / Entity，编译期也不会反向依赖主 App。
        CompanionBridge.bind(
            object : DueDataProvider {
                override suspend fun totalWordCount(): Int = repository.totalWordCount()
                override suspend fun dueCount(now: Long): Int = repository.dueNow(now)
            },
        )
        applicationScope.launch { repository.ensureSeeded() }
    }

    companion object {
        /** 与进程同生命周期的协程作用域，用于不应被页面销毁打断的初始化任务。 */
        val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
