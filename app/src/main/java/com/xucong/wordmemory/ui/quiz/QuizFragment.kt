package com.xucong.wordmemory.ui.quiz

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.xucong.wordmemory.R
import com.xucong.wordmemory.WordMemoryApp
import com.xucong.wordmemory.databinding.FragmentQuizBinding
import com.xucong.wordmemory.domain.quiz.QuizMode
import kotlinx.coroutines.launch

/** 测试模式：选择题 / 拼写题，逐题判分，最后给分数与错词。 */
class QuizFragment : Fragment(R.layout.fragment_quiz) {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val deckId: Long by lazy { requireArguments().getLong(ARG_DECK_ID) }
    private val mode: QuizMode by lazy {
        if (requireArguments().getString(ARG_MODE) == MODE_SPELLING) QuizMode.SPELLING else QuizMode.CHOICE
    }

    private val viewModel: QuizViewModel by viewModels {
        QuizViewModel.factory(
            (requireActivity().application as WordMemoryApp).repository,
            deckId,
            mode,
        )
    }

    private val optionButtons: List<MaterialButton> by lazy {
        listOf(binding.option0, binding.option1, binding.option2, binding.option3)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizBinding.bind(view)

        val deckName = requireArguments().getString(ARG_DECK_NAME).orEmpty()
        binding.deckTitle.text = getString(
            if (mode == QuizMode.SPELLING) R.string.quiz_title_spelling else R.string.quiz_title_choice,
            deckName,
        )

        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener { viewModel.choose(index) }
        }
        binding.submitSpelling.setOnClickListener { viewModel.submitSpelling() }
        binding.nextButton.setOnClickListener { viewModel.next() }
        binding.restartButton.setOnClickListener { viewModel.start() }
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        binding.spellingInput.doAfterTextChanged { text ->
            viewModel.updateSpelling(text?.toString().orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun render(state: QuizUiState) {
        val question = state.current

        binding.progressText.text = state.progressText
        binding.progressBar.max = state.total.coerceAtLeast(1)
        binding.progressBar.progress = (state.index + 1).coerceAtMost(state.total)

        binding.questionGroup.isVisible = !state.finished && question != null
        binding.resultGroup.isVisible = state.finished

        if (state.finished) {
            binding.resultScore.text = getString(
                R.string.quiz_result_score,
                state.total,
                state.correctCount,
                state.accuracyPercent,
            )
            binding.resultWrong.text = if (state.wrongTerms.isEmpty()) {
                getString(R.string.quiz_result_all_correct)
            } else {
                getString(R.string.quiz_result_wrong, state.wrongTerms.joinToString("、"))
            }
            return
        }

        if (question == null) {
            // 词库为空：不崩，给一句可读提示
            binding.promptText.text = getString(R.string.quiz_empty)
            binding.promptHint.text = ""
            binding.optionGroup.isVisible = false
            binding.spellingGroup.isVisible = false
            binding.feedbackText.text = ""
            binding.nextButton.isVisible = false
            return
        }

        if (state.mode == QuizMode.CHOICE) {
            // 题干是单词，选项是释义
            binding.promptText.text = question.word.term
            binding.promptHint.text = question.word.phonetic
            binding.optionGroup.isVisible = true
            binding.spellingGroup.isVisible = false
            renderOptions(state, question.options, question.correctIndex)
        } else {
            // 题干是释义，答案是拼写；给首字母与长度当提示
            binding.promptText.text = question.word.meaning
            binding.promptHint.text = spellingHint(question.word.term)
            binding.optionGroup.isVisible = false
            binding.spellingGroup.isVisible = true
            binding.submitSpelling.isEnabled = !state.answered
            binding.spellingInput.isEnabled = !state.answered
            if (!state.answered && binding.spellingInput.text?.toString() != state.spellingInput) {
                binding.spellingInput.setText(state.spellingInput)
            }
        }

        binding.feedbackText.isVisible = state.answered
        if (state.answered) {
            binding.feedbackText.text = if (state.lastCorrect) {
                getString(R.string.quiz_feedback_correct)
            } else {
                getString(R.string.quiz_feedback_wrong, question.word.term, question.word.meaning)
            }
        }

        binding.nextButton.isVisible = state.answered
        binding.nextButton.text = getString(
            if (state.index == state.total - 1) R.string.quiz_see_result else R.string.quiz_next,
        )
    }

    private fun renderOptions(state: QuizUiState, options: List<String>, correctIndex: Int) {
        optionButtons.forEachIndexed { index, button ->
            val optionText = options.getOrNull(index)
            button.isVisible = optionText != null
            if (optionText == null) return@forEachIndexed

            button.isEnabled = !state.answered
            button.text = when {
                !state.answered -> optionText
                index == correctIndex -> getString(R.string.quiz_option_correct, optionText)
                index == state.selectedOption -> getString(R.string.quiz_option_wrong, optionText)
                else -> optionText
            }
            button.alpha = if (!state.answered || index == correctIndex || index == state.selectedOption) 1f else 0.5f
        }
    }

    /** 拼写提示：首字母 + 长度，比如 "a _ _ _ _ _ _（7 个字母）"。 */
    private fun spellingHint(term: String): String {
        val letters = term.replace(" ", "")
        if (letters.isEmpty()) return ""
        val masked = buildString {
            append(term.first())
            for (index in 1 until term.length) {
                append(if (term[index] == ' ') "  " else " _")
            }
        }
        return getString(R.string.quiz_spelling_hint_value, masked, letters.length)
    }

    private companion object {
        const val ARG_DECK_ID = "deckId"
        const val ARG_DECK_NAME = "deckName"
        const val ARG_MODE = "mode"
        const val MODE_SPELLING = "spelling"
    }
}
