package com.xucong.wordmemory.ui.word

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xucong.wordmemory.data.WordRepository
import com.xucong.wordmemory.data.local.entity.WordEntity
import com.xucong.wordmemory.data.remote.DictionaryRepository
import com.xucong.wordmemory.domain.dict.DictEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 详情页状态：本地词条来自 Room 的 Flow，在线补充是一次性请求，
 * 两者分开，避免「联网中」把本地内容也一起变成加载态。
 */
class WordDetailViewModel(
    repository: WordRepository,
    private val dictionaryRepository: DictionaryRepository,
    wordId: Long,
) : ViewModel() {

    data class OnlineUiState(
        val loading: Boolean = false,
        val entry: DictEntry? = null,
        val error: String? = null,
    )

    val word: StateFlow<WordEntity?> = repository.observeWord(wordId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _online = MutableStateFlow(OnlineUiState())
    val online: StateFlow<OnlineUiState> = _online.asStateFlow()

    private var autoLoaded = false

    /** 进页面自动查一次（命中缓存时不产生网络请求），失败也可以手动重试。 */
    fun loadOnlineIfNeeded() {
        if (autoLoaded) return
        autoLoaded = true
        loadOnline()
    }

    fun loadOnline(forceRefresh: Boolean = false) {
        val term = word.value?.term?.trim().orEmpty()
        if (term.isEmpty() || _online.value.loading) return

        _online.value = _online.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val result = dictionaryRepository.lookup(term, forceRefresh)
            _online.value = result.fold(
                onSuccess = { OnlineUiState(loading = false, entry = it) },
                onFailure = { error ->
                    OnlineUiState(
                        loading = false,
                        entry = _online.value.entry,
                        error = error.message ?: "联网失败",
                    )
                },
            )
        }
    }

    fun pronunciationUrl(): String? =
        word.value?.term?.trim()?.takeIf { it.isNotEmpty() }
            ?.let(dictionaryRepository::audioUrl)

    companion object {
        fun factory(
            repository: WordRepository,
            dictionaryRepository: DictionaryRepository,
            wordId: Long,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { WordDetailViewModel(repository, dictionaryRepository, wordId) }
        }
    }
}
