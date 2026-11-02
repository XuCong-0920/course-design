package com.xucong.wordmemory.ui.review

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.xucong.wordmemory.R
import com.xucong.wordmemory.WordMemoryApp
import com.xucong.wordmemory.databinding.FragmentReviewBinding
import com.xucong.wordmemory.domain.srs.ReviewGrade
import kotlinx.coroutines.launch

/**
 * 复习页：一张卡 → 点「显示答案」→ 三档反馈 → 自动下一张。
 *
 * 答案默认不可见，是为了让「认识 / 模糊 / 不认识」这一判断真的基于回忆，
 * 而不是基于刚看到的释义；这也决定了三档按钮只在 revealed 之后出现。
 */
class ReviewFragment : Fragment(R.layout.fragment_review) {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ReviewViewModel by viewModels {
        ReviewViewModel.factory((requireActivity().application as WordMemoryApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReviewBinding.bind(view)

        binding.revealButton.setOnClickListener { viewModel.reveal() }
        binding.forgotButton.setOnClickListener { viewModel.grade(ReviewGrade.FORGOT) }
        binding.vagueButton.setOnClickListener { viewModel.grade(ReviewGrade.VAGUE) }
        binding.knownButton.setOnClickListener { viewModel.grade(ReviewGrade.KNOWN) }
        binding.restartButton.setOnClickListener { viewModel.startNewRound() }

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

    private fun render(state: ReviewUiState) {
        val word = state.current
        val hasCard = word != null

        binding.progressText.isVisible = hasCard
        binding.countText.isVisible = hasCard
        binding.card.isVisible = hasCard
        binding.actionBar.isVisible = hasCard
        binding.revealButton.isVisible = hasCard && !state.revealed
        binding.gradeGroup.isVisible = hasCard && state.revealed

        val showStatus = !hasCard && !state.loading
        binding.statusText.isVisible = showStatus
        binding.restartButton.isVisible = showStatus

        // 提交期间禁用三档按钮，避免连点把同一张卡记成多次复习
        binding.forgotButton.isEnabled = !state.submitting
        binding.vagueButton.isEnabled = !state.submitting
        binding.knownButton.isEnabled = !state.submitting

        if (!hasCard) {
            binding.statusText.text = getString(
                if (state.total == 0) R.string.review_empty else R.string.review_finished,
            )
            return
        }

        binding.progressText.text = getString(R.string.review_remaining, state.remaining)
        binding.countText.text = getString(
            R.string.review_counts,
            state.knownCount,
            state.vagueCount,
            state.forgotCount,
        )

        binding.termText.text = word.term
        binding.phoneticText.text = word.phonetic
        binding.phoneticText.isVisible = word.phonetic.isNotBlank()

        val revealed = state.revealed
        binding.divider.isVisible = revealed
        binding.meaningText.isVisible = revealed
        binding.meaningText.text = word.meaning
        binding.exampleText.isVisible = revealed && word.example.isNotBlank()
        binding.exampleText.text = word.example
        binding.intervalHint.isVisible = revealed
        binding.intervalHint.text = if (word.intervalDays == 0) {
            getString(R.string.review_hint_new)
        } else {
            getString(R.string.review_hint_interval, word.intervalDays)
        }
    }
}
