package com.xucong.wordmemory.ui.word

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xucong.wordmemory.data.WordRepository
import com.xucong.wordmemory.data.local.entity.WordEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** 单词列表页状态：某个词库下的全部单词，随 Room 变更自动刷新。 */
class WordListViewModel(
    repository: WordRepository,
    deckId: Long,
) : ViewModel() {

    val words: StateFlow<List<WordEntity>> = repository.observeWords(deckId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    companion object {
        fun factory(repository: WordRepository, deckId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { WordListViewModel(repository, deckId) }
            }
    }
}
