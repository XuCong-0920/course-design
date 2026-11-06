package com.xucong.wordmemory.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xucong.wordmemory.data.WordRepository
import com.xucong.wordmemory.domain.quiz.QuizBuilder
import com.xucong.wordmemory.domain.quiz.QuizMode
import com.xucong.wordmemory.domain.quiz.QuizQuestion
import com.xucong.wordmemory.domain.quiz.QuizWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class QuizUiState(
    val loading: Boolean = true,
    val mode: QuizMode = QuizMode.CHOICE,
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    /** 选择题已选的选项下标；未作答为 null。 */
    val selectedOption: Int? = null,
    /** 拼写题的输入框内容。 */
    val spellingInput: String = "",
    /** 本题是否已作答（作答后才显示对错与「下一题」）。 */
    val answered: Boolean = false,
    val lastCorrect: Boolean = false,
    val correctCount: Int = 0,
    val wrongTerms: List<String> = emptyList(),
    val finished: Boolean = false,
) {
    val current: QuizQuestion? get() = questions.getOrNull(index)
    val total: Int get() = questions.size
    val progressText: String get() = if (questions.isEmpty()) "0 / 0" else "${index + 1} / $total"
    val accuracyPercent: Int get() = if (questions.isEmpty()) 0 else correctCount * 100 / total
}

/**
 * 测试模式：抽一整套题、逐题判分、最后给分数与错词。
 *
 * 刻意**不写数据库**：测试是自测手段，不应该改变 SRS 调度；
 * 想让错词进复习队列，走复习页的「不认识」才是正确入口。
 */
class QuizViewModel(
    private val repository: WordRepository,
    private val deckId: Long,
    val mode: QuizMode,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizUiState(mode = mode))
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    init {
        start()
    }

    fun start() {
        viewModelScope.launch {
            val words = withContext(Dispatchers.IO) {
                repository.wordsOf(deckId).map {
                    QuizWord(it.id, it.term, it.phonetic, it.meaning, it.example)
                }
            }
            _state.value = QuizUiState(
                loading = false,
                mode = mode,
                questions = QuizBuilder.build(words, QuizBuilder.DEFAULT_COUNT),
            )
        }
    }

    fun choose(optionIndex: Int) {
        val snapshot = _state.value
        val question = snapshot.current ?: return
        if (snapshot.answered) return

        val correct = optionIndex == question.correctIndex
        _state.value = snapshot.copy(
            selectedOption = optionIndex,
            answered = true,
            lastCorrect = correct,
            correctCount = snapshot.correctCount + if (correct) 1 else 0,
            wrongTerms = if (correct) snapshot.wrongTerms else snapshot.wrongTerms + question.word.term,
        )
    }

    fun updateSpelling(text: String) {
        _state.update { if (it.answered) it else it.copy(spellingInput = text) }
    }

    fun submitSpelling() {
        val snapshot = _state.value
        val question = snapshot.current ?: return
        if (snapshot.answered) return

        val correct = QuizBuilder.isSpellingCorrect(snapshot.spellingInput, question.word.term)
        _state.value = snapshot.copy(
            answered = true,
            lastCorrect = correct,
            correctCount = snapshot.correctCount + if (correct) 1 else 0,
            wrongTerms = if (correct) snapshot.wrongTerms else snapshot.wrongTerms + question.word.term,
        )
    }

    fun next() {
        val snapshot = _state.value
        if (!snapshot.answered) return

        val nextIndex = snapshot.index + 1
        _state.value = if (nextIndex >= snapshot.questions.size) {
            snapshot.copy(finished = true)
        } else {
            snapshot.copy(
                index = nextIndex,
                selectedOption = null,
                spellingInput = "",
                answered = false,
                lastCorrect = false,
            )
        }
    }

    companion object {
        fun factory(repository: WordRepository, deckId: Long, mode: QuizMode) = viewModelFactory {
            initializer { QuizViewModel(repository, deckId, mode) }
        }
    }
}
