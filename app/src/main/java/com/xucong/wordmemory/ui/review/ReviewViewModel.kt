package com.xucong.wordmemory.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xucong.wordmemory.data.WordRepository
import com.xucong.wordmemory.data.local.entity.WordEntity
import com.xucong.wordmemory.domain.srs.ReviewGrade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 复习页状态。
 *
 * [current] 为 null 时页面进入「结束态」，具体是「今天没有到期词」还是「一轮复习完了」
 * 由 [total] 区分：total == 0 表示原本就没有到期词。
 */
data class ReviewUiState(
    val loading: Boolean = true,
    val current: WordEntity? = null,
    /** 是否已翻开答案（未翻开时三档按钮不显示，避免「没看就点认识」）。 */
    val revealed: Boolean = false,
    /** 提交中，用来挡住连点造成的重复入库。 */
    val submitting: Boolean = false,
    val total: Int = 0,
    val remaining: Int = 0,
    val forgotCount: Int = 0,
    val vagueCount: Int = 0,
    val knownCount: Int = 0,
)

/**
 * 复习页的调度器：一轮开始取一次到期队列，答完一个推进一步。
 *
 * 有意**不做实时重查**：答错的词 10 分钟后才到期，本轮内不应该立刻再出现；
 * 重新开始一轮（restart）才会重新查库，这时刚答错的词若还没到 10 分钟也不会进来。
 */
class ReviewViewModel(private val repository: WordRepository) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private var queue: List<WordEntity> = emptyList()
    private var index = 0

    init {
        startNewRound()
    }

    fun startNewRound() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            queue = repository.dueQueue()
            index = 0
            _state.value = ReviewUiState(
                loading = false,
                current = queue.firstOrNull(),
                total = queue.size,
                remaining = queue.size,
            )
        }
    }

    fun reveal() {
        if (_state.value.current == null) return
        _state.update { it.copy(revealed = true) }
    }

    /** 记录一次反馈并推进到下一张卡。 */
    fun grade(grade: ReviewGrade) {
        val snapshot = _state.value
        val word = snapshot.current ?: return
        if (!snapshot.revealed || snapshot.submitting) return

        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            repository.submitReview(word, grade)

            index++
            val next = queue.getOrNull(index)
            _state.update { s ->
                s.copy(
                    current = next,
                    revealed = false,
                    submitting = false,
                    remaining = (queue.size - index).coerceAtLeast(0),
                    forgotCount = s.forgotCount + if (grade == ReviewGrade.FORGOT) 1 else 0,
                    vagueCount = s.vagueCount + if (grade == ReviewGrade.VAGUE) 1 else 0,
                    knownCount = s.knownCount + if (grade == ReviewGrade.KNOWN) 1 else 0,
                )
            }
        }
    }

    companion object {
        fun factory(repository: WordRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ReviewViewModel(repository) }
        }
    }
}
