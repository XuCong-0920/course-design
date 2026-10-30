package com.xucong.wordmemory.ui.word

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
import com.xucong.wordmemory.data.local.entity.WordEntity
import com.xucong.wordmemory.databinding.FragmentWordDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** 单词详情页：列表 → 详情的第二跳，展示词条内容与 SRS 复习状态。 */
class WordDetailFragment : Fragment(R.layout.fragment_word_detail) {

    private var _binding: FragmentWordDetailBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val wordId: Long by lazy { requireArguments().getLong(ARG_WORD_ID) }

    private val viewModel: WordDetailViewModel by viewModels {
        WordDetailViewModel.factory(
            (requireActivity().application as WordMemoryApp).repository,
            wordId,
        )
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWordDetailBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.word.collect(::render)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun render(word: WordEntity?) {
        if (word == null) return

        binding.termText.text = word.term
        binding.phoneticText.text = word.phonetic
        binding.phoneticText.isVisible = word.phonetic.isNotBlank()
        binding.meaningText.text = word.meaning
        binding.exampleText.text = word.example.ifBlank { "—" }

        binding.srsText.text = getString(
            R.string.word_detail_srs_value,
            word.repetition,
            word.intervalDays,
            word.easeFactor,
        )

        val now = System.currentTimeMillis()
        binding.dueText.text = if (word.dueAt <= now) {
            getString(R.string.word_detail_due_now)
        } else {
            getString(R.string.word_detail_due_later, dateFormat.format(Date(word.dueAt)))
        }
    }

    private companion object {
        const val ARG_WORD_ID = "wordId"
    }
}
