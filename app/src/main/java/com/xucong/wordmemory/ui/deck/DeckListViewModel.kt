package com.xucong.wordmemory.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xucong.wordmemory.data.ImportOutcome
import com.xucong.wordmemory.data.WordRepository
import com.xucong.wordmemory.data.local.dao.DeckSummary
import com.xucong.wordmemory.data.remote.DictionaryRepository
import com.xucong.wordmemory.domain.dict.DailySentencePicker
import com.xucong.wordmemory.domain.importer.ImportPreview
import java.util.Calendar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 词库列表页的状态容器：把 Repository 的冷 Flow 转成有生命周期的 StateFlow。
 * WhileSubscribed(5_000) 让页面旋转/短暂切走时不必重订阅 Room。
 */
class DeckListViewModel(
    private val repository: WordRepository,
    private val dictionaryRepository: DictionaryRepository,
) : ViewModel() {

    /** 每日例句的来源，决定卡片底部显示哪句说明。 */
    enum class DailySource { ONLINE, LOCAL, OFFLINE }

    data class DailyUiState(
        val loading: Boolean = true,
        val term: String = "",
        val english: String = "",
        val chinese: String = "",
        val source: DailySource = DailySource.LOCAL,
        val empty: Boolean = false,
    )

    val decks: StateFlow<List<DeckSummary>> = repository.observeDeckSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _daily = MutableStateFlow(DailyUiState())
    val daily: StateFlow<DailyUiState> = _daily.asStateFlow()

    /** 一次性事件：导入结束后弹 Snackbar。用 SharedFlow 而不是 State，避免旋转屏幕后又弹一次。 */
    private val _importEvents = MutableSharedFlow<ImportOutcome>(extraBufferCapacity = 1)
    val importEvents: SharedFlow<ImportOutcome> = _importEvents.asSharedFlow()

    /**
     * 每日例句：选词是本地确定的（同一天同一个词），英文句子优先用在线结果，
     * 断网或没查到就退回词库里自带的例句，再没有就只显示单词。
     */
    fun loadDailySentence() {
        viewModelScope.launch {
            val words = runCatching { repository.allWords() }.getOrDefault(emptyList())
            if (words.isEmpty()) {
                _daily.value = DailyUiState(loading = false, empty = true)
                return@launch
            }

            val dayIndex = DailySentencePicker.dayIndex(startOfToday())
            val word = DailySentencePicker.pick(words, dayIndex) ?: return@launch
            val localExample = word.example.trim()
            val lookup = runCatching { dictionaryRepository.lookup(word.term) }.getOrNull()
            val remote = lookup?.getOrNull()?.examples?.firstOrNull()

            _daily.value = when {
                remote != null -> DailyUiState(
                    loading = false,
                    term = word.term,
                    english = remote.english,
                    chinese = remote.chinese,
                    source = DailySource.ONLINE,
                )
                localExample.isNotEmpty() -> DailyUiState(
                    loading = false,
                    term = word.term,
                    english = localExample,
                    source = if (lookup?.isFailure == true) DailySource.OFFLINE else DailySource.LOCAL,
                )
                else -> DailyUiState(
                    loading = false,
                    term = word.term,
                    source = DailySource.OFFLINE,
                )
            }
        }
    }

    /**
     * 写入预览里的词。词库自动出现，因为列表订阅的是 Room 的 Flow 而不是内存缓存。
     */
    fun import(preview: ImportPreview) {
        viewModelScope.launch {
            val outcome = repository.importDeck(
                deckName = preview.deckName,
                description = IMPORTED_DESCRIPTION,
                words = preview.words,
            )
            _importEvents.tryEmit(outcome)
        }
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        private const val IMPORTED_DESCRIPTION = "由文件导入"

        fun factory(
            repository: WordRepository,
            dictionaryRepository: DictionaryRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { DeckListViewModel(repository, dictionaryRepository) }
        }
    }
}
