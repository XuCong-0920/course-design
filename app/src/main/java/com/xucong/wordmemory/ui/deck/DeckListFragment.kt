package com.xucong.wordmemory.ui.deck

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
import com.xucong.wordmemory.data.local.dao.DeckSummary
import com.xucong.wordmemory.databinding.FragmentDeckListBinding
import kotlinx.coroutines.launch

/**
 * 词库列表页：列表 → 详情的第一跳。
 * UI 只订阅 ViewModel 暴露的 StateFlow，数据来源与线程切换都在数据层完成。
 */
class DeckListFragment : Fragment(R.layout.fragment_deck_list) {

    private var _binding: FragmentDeckListBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: DeckListViewModel by viewModels {
        DeckListViewModel.factory((requireActivity().application as WordMemoryApp).repository)
    }

    private val adapter = DeckAdapter(::openDeck)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDeckListBinding.bind(view)

        binding.deckList.layoutManager = LinearLayoutManager(requireContext())
        binding.deckList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.decks.collect { decks ->
                    adapter.submitList(decks)
                    binding.emptyText.isVisible = decks.isEmpty()
                    binding.deckList.isVisible = decks.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.deckList.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun openDeck(deck: DeckSummary) {
        val args = Bundle().apply {
            putLong(ARG_DECK_ID, deck.id)
            putString(ARG_DECK_NAME, deck.name)
        }
        findNavController().navigate(R.id.action_deckList_to_wordList, args)
    }

    private companion object {
        const val ARG_DECK_ID = "deckId"
        const val ARG_DECK_NAME = "deckName"
    }
}
