package com.xucong.wordmemory.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xucong.wordmemory.data.WordRepository
import com.xucong.wordmemory.data.local.dao.DeckSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 词库列表页的状态容器：把 Repository 的冷 Flow 转成有生命周期的 StateFlow。
 * WhileSubscribed(5_000) 让页面旋转/短暂切走时不必重订阅 Room。
 */
class DeckListViewModel(repository: WordRepository) : ViewModel() {

    val decks: StateFlow<List<DeckSummary>> = repository.observeDeckSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    companion object {
        fun factory(repository: WordRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { DeckListViewModel(repository) }
        }
    }
}
