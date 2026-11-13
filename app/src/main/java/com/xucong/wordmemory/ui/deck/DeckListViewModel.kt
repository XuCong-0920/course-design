package com.xucong.wordmemory.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xucong.wordmemory.data.ImportOutcome
import com.xucong.wordmemory.data.WordRepository
import com.xucong.wordmemory.data.local.dao.DeckSummary
import com.xucong.wordmemory.domain.importer.ImportPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 词库列表页的状态容器：把 Repository 的冷 Flow 转成有生命周期的 StateFlow。
 * WhileSubscribed(5_000) 让页面旋转/短暂切走时不必重订阅 Room。
 */
class DeckListViewModel(private val repository: WordRepository) : ViewModel() {

    val decks: StateFlow<List<DeckSummary>> = repository.observeDeckSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** 一次性事件：导入结束后弹 Snackbar。用 SharedFlow 而不是 State，避免旋转屏幕后又弹一次。 */
    private val _importEvents = MutableSharedFlow<ImportOutcome>(extraBufferCapacity = 1)
    val importEvents: SharedFlow<ImportOutcome> = _importEvents.asSharedFlow()

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

    companion object {
        private const val IMPORTED_DESCRIPTION = "由文件导入"

        fun factory(repository: WordRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { DeckListViewModel(repository) }
        }
    }
}
