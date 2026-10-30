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

/** 单词详情页状态；null 表示仍在加载或已被删除。 */
class WordDetailViewModel(
    repository: WordRepository,
    wordId: Long,
) : ViewModel() {

    val word: StateFlow<WordEntity?> = repository.observeWord(wordId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    companion object {
        fun factory(repository: WordRepository, wordId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { WordDetailViewModel(repository, wordId) }
            }
    }
}
