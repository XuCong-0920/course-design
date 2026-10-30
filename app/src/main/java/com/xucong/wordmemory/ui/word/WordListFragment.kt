package com.xucong.wordmemory.ui.word

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.xucong.wordmemory.R
import com.xucong.wordmemory.WordMemoryApp
import com.xucong.wordmemory.data.local.entity.WordEntity
import com.xucong.wordmemory.databinding.FragmentWordListBinding
import kotlinx.coroutines.launch

/** 某个词库的单词列表；标题取自导航参数 deckName。 */
class WordListFragment : Fragment(R.layout.fragment_word_list) {

    private var _binding: FragmentWordListBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val deckId: Long by lazy { requireArguments().getLong(ARG_DECK_ID) }

    private val viewModel: WordListViewModel by viewModels {
        WordListViewModel.factory(
            (requireActivity().application as WordMemoryApp).repository,
            deckId,
        )
    }

    private val adapter = WordAdapter(::openWord)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWordListBinding.bind(view)

        binding.titleText.text = requireArguments().getString(ARG_DECK_NAME).orEmpty()
        binding.wordList.layoutManager = LinearLayoutManager(requireContext())
        binding.wordList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.words.collect { words ->
                    adapter.submitList(words)
                    binding.emptyText.isVisible = words.isEmpty()
                    binding.wordList.isVisible = words.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.wordList.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun openWord(word: WordEntity) {
        val args = Bundle().apply { putLong(ARG_WORD_ID, word.id) }
        findNavController().navigate(R.id.action_wordList_to_wordDetail, args)
    }

    private companion object {
        const val ARG_DECK_ID = "deckId"
        const val ARG_DECK_NAME = "deckName"
        const val ARG_WORD_ID = "wordId"
    }
}
