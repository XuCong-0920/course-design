package com.xucong.wordmemory.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xucong.wordmemory.data.WordRepository
import com.xucong.wordmemory.domain.stats.StatsCalculator
import com.xucong.wordmemory.domain.stats.StudyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class StatsUiState(
    val loading: Boolean = true,
    val dueNow: Int = 0,
    val totalWords: Int = 0,
    val stats: StudyStats = StudyStats(),
)

/**
 * 统计页：每次进入（refresh）都重新查一遍库。
 *
 * 不用 Flow 实时订阅是有意的 —— 统计只在切到本页时看，实时刷新会让用户在复习页疯狂翻卡时
 * 白白触发一堆重算；从复习页返回时 onResume 会再算一次，结果一样新鲜。
 */
class StatsViewModel(private val repository: WordRepository) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val todayStart = startOfToday(now)
            // 多取一天，保证「连续打卡」跨到窗口之外的记录也能被算上
            val windowStart = todayStart - (StatsCalculator.WINDOW_DAYS + 60L) * StatsCalculator.DAY_MILLIS

            val loaded = withContext(Dispatchers.IO) {
                val events = repository.reviewEventsSince(windowStart)
                val due = repository.dueNow(now)
                val words = repository.totalWordCount()
                Triple(events, due, words)
            }

            _state.value = StatsUiState(
                loading = false,
                dueNow = loaded.second,
                totalWords = loaded.third,
                stats = StatsCalculator.build(loaded.first, todayStart),
            )
        }
    }

    private fun startOfToday(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        fun factory(repository: WordRepository) = viewModelFactory {
            initializer { StatsViewModel(repository) }
        }
    }
}
