package com.xucong.wordmemory.ui.word

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.xucong.wordmemory.R
import com.xucong.wordmemory.WordMemoryApp
import com.xucong.wordmemory.data.local.entity.WordEntity
import com.xucong.wordmemory.databinding.FragmentWordDetailBinding
import com.xucong.wordmemory.domain.dict.DictEntry
import com.xucong.wordmemory.ui.common.PronunciationPlayer
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
        val app = requireActivity().application as WordMemoryApp
        WordDetailViewModel.factory(app.repository, app.dictionaryRepository, wordId)
    }

    private val player = PronunciationPlayer()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWordDetailBinding.bind(view)

        binding.speakButton.setOnClickListener { speak() }
        binding.onlineButton.setOnClickListener { viewModel.loadOnline(forceRefresh = true) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.word.collect { word ->
                        render(word)
                        if (word != null) viewModel.loadOnlineIfNeeded()
                    }
                }
                launch { viewModel.online.collect(::renderOnline) }
            }
        }
    }

    override fun onDestroyView() {
        player.stop()
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

    private fun renderOnline(state: WordDetailViewModel.OnlineUiState) {
        binding.onlineButton.isEnabled = !state.loading
        binding.onlineButton.text = getString(
            if (state.loading) R.string.word_detail_online_loading else R.string.word_detail_online,
        )

        val entry = state.entry
        binding.onlineCard.isVisible = entry != null || state.error != null
        if (entry == null) {
            binding.onlineStatusText.text = state.error?.let {
                getString(R.string.word_detail_online_failed, it)
            }.orEmpty()
            binding.onlineDefinitionText.isVisible = false
            binding.onlineExampleText.isVisible = false
            return
        }

        binding.onlineStatusText.text = buildString {
            append(getString(R.string.word_detail_online_status))
            if (entry.phonetic.isNotBlank()) {
                append(" · 音标 ").append(entry.phonetic)
            }
            if (state.error != null) {
                append(" · ").append(getString(R.string.word_detail_online_failed, state.error))
            }
        }
        binding.onlineDefinitionText.isVisible = entry.definitions.isNotEmpty()
        binding.onlineDefinitionText.text = entry.definitions.joinToString("\n")
        binding.onlineExampleText.isVisible = entry.examples.isNotEmpty()
        binding.onlineExampleText.text = entry.examples.joinToString("\n\n") { example ->
            formatExample(example)
        }
    }

    private fun formatExample(example: com.xucong.wordmemory.domain.dict.DictExample): String =
        if (example.chinese.isBlank()) example.english else "${example.english}\n${example.chinese}"

    private fun speak() {
        val url = viewModel.pronunciationUrl() ?: return
        player.play(url) { message ->
            if (!isAdded) return@play
            Snackbar.make(
                binding.root,
                getString(R.string.word_detail_speak_failed, message),
                Snackbar.LENGTH_LONG,
            ).show()
        }
    }

    private companion object {
        const val ARG_WORD_ID = "wordId"
    }
}
