package com.xucong.wordmemory.ui.deck

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.xucong.wordmemory.R
import com.xucong.wordmemory.WordMemoryApp
import com.xucong.wordmemory.data.local.dao.DeckSummary
import com.xucong.wordmemory.databinding.FragmentDeckListBinding
import com.xucong.wordmemory.domain.importer.ImportPreview
import com.xucong.wordmemory.domain.importer.ImportProblem
import com.xucong.wordmemory.domain.importer.WordFileImporter
import com.xucong.wordmemory.ui.common.DocumentTextReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 词库列表页：列表 → 详情的第一跳。
 * UI 只订阅 ViewModel 暴露的 StateFlow，数据来源与线程切换都在数据层完成。
 */
class DeckListFragment : Fragment(R.layout.fragment_deck_list) {

    private var _binding: FragmentDeckListBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: DeckListViewModel by viewModels {
        val app = requireActivity().application as WordMemoryApp
        DeckListViewModel.factory(app.repository, app.dictionaryRepository)
    }

    private val adapter = DeckAdapter(::openDeck)

    /**
     * SAF 选文件：两个入口分开注册，因为 MIME 过滤不一样。
     * `arrayOf("text/plain", …)` 里故意带上 text/plain —— 很多国产文件管理器把 .csv/.json
     * 都报成 text/plain，只给 `text/csv` 会出现「文件是灰的点不了」。
     */
    private val pickCsv = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::handlePickedFile)
    }
    private val pickJson = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::handlePickedFile)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDeckListBinding.bind(view)

        binding.deckList.layoutManager = LinearLayoutManager(requireContext())
        binding.deckList.adapter = adapter

        binding.importButton.setOnClickListener { showFormatPicker() }
        viewModel.loadDailySentence()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.decks.collect { decks ->
                        adapter.submitList(decks)
                        binding.emptyText.isVisible = decks.isEmpty()
                        binding.deckList.isVisible = decks.isNotEmpty()
                    }
                }
                launch { viewModel.daily.collect(::renderDaily) }
                launch {
                    viewModel.importEvents.collect { outcome ->
                        if (!isAdded) return@collect
                        val message = if (outcome.imported) {
                            getString(R.string.import_done, outcome.inserted, outcome.skipped)
                        } else {
                            getString(R.string.import_empty)
                        }
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
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

    /**
     * 每日例句卡片：加载中、空词库、有句子（在线/本地/离线）三种形态都在这里收口，
     * 不让状态判断散在 XML 里。
     */
    private fun renderDaily(state: DeckListViewModel.DailyUiState) {
        binding.dailyTermText.text = state.term

        when {
            state.loading -> {
                binding.dailyTermText.isVisible = false
                binding.dailySentenceText.text = getString(R.string.daily_loading)
                binding.dailyStatusText.text = ""
            }
            state.empty -> {
                binding.dailyTermText.isVisible = false
                binding.dailySentenceText.text = getString(R.string.daily_empty)
                binding.dailyStatusText.text = ""
            }
            state.english.isBlank() -> {
                binding.dailyTermText.isVisible = true
                binding.dailySentenceText.text = getString(R.string.daily_no_sentence)
                binding.dailyStatusText.text = getString(sourceLabel(state.source))
            }
            else -> {
                binding.dailyTermText.isVisible = true
                binding.dailySentenceText.text = if (state.chinese.isBlank()) {
                    state.english
                } else {
                    "${state.english}\n${state.chinese}"
                }
                binding.dailyStatusText.text = getString(sourceLabel(state.source))
            }
        }
    }

    private fun sourceLabel(source: DeckListViewModel.DailySource): Int = when (source) {
        DeckListViewModel.DailySource.ONLINE -> R.string.daily_source_online
        DeckListViewModel.DailySource.LOCAL -> R.string.daily_source_local
        DeckListViewModel.DailySource.OFFLINE -> R.string.daily_source_offline
    }

    // ------------------------------------------------------------ 导入

    private fun showFormatPicker() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.import_choose_title)
            .setItems(
                arrayOf(
                    getString(R.string.import_format_csv),
                    getString(R.string.import_format_json),
                ),
            ) { _, which ->
                if (which == 0) {
                    pickCsv.launch(CSV_MIME_TYPES)
                } else {
                    pickJson.launch(JSON_MIME_TYPES)
                }
            }
            .setNegativeButton(R.string.import_cancel, null)
            .show()
    }

    private fun handlePickedFile(uri: Uri) {
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    val name = DocumentTextReader.displayName(context, uri)
                    val text = DocumentTextReader.readText(context, uri)
                    WordFileImporter.parse(name, text)
                }
            }

            parsed.onSuccess { preview -> showPreviewDialog(preview) }
                .onFailure { error ->
                    if (!isAdded) return@onFailure
                    Snackbar.make(
                        binding.root,
                        getString(R.string.import_failed, error.message ?: error.javaClass.simpleName),
                        Snackbar.LENGTH_LONG,
                    ).show()
                }
        }
    }

    /**
     * 先给一份「读到了什么、跳过了什么」，用户确认后再写库。
     * 不做静默导入：坏行必须让人看见，否则会以为整份都进去了。
     */
    private fun showPreviewDialog(preview: ImportPreview) {
        if (!isAdded) return

        val body = buildString {
            append(
                getString(
                    R.string.import_summary_body,
                    preview.deckName,
                    preview.words.size,
                    preview.problems.size,
                    preview.duplicateCount,
                ),
            )
            preview.problems.take(MAX_PROBLEMS_SHOWN).forEach { problem: ImportProblem ->
                append('\n')
                append(getString(R.string.import_problem_line, problem.line, problem.reason))
            }
            val rest = preview.problems.size - MAX_PROBLEMS_SHOWN
            if (rest > 0) {
                append('\n')
                append(getString(R.string.import_problems_more, rest))
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.import_summary_title)
            .setMessage(body)
            .setPositiveButton(R.string.import_confirm) { _, _ ->
                if (preview.isEmpty) {
                    Snackbar.make(binding.root, R.string.import_empty, Snackbar.LENGTH_LONG).show()
                } else {
                    viewModel.import(preview)
                }
            }
            .setNegativeButton(R.string.import_cancel, null)
            .show()
    }

    private companion object {
        const val ARG_DECK_ID = "deckId"
        const val ARG_DECK_NAME = "deckName"
        const val MAX_PROBLEMS_SHOWN = 5

        val CSV_MIME_TYPES = arrayOf(
            "text/csv",
            "text/comma-separated-values",
            "application/csv",
            "text/plain",
            "*/*",
        )
        val JSON_MIME_TYPES = arrayOf(
            "application/json",
            "text/json",
            "text/plain",
            "*/*",
        )
    }
}
