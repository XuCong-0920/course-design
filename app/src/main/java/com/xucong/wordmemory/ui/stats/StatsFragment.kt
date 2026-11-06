package com.xucong.wordmemory.ui.stats

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
import com.xucong.wordmemory.databinding.FragmentStatsBinding
import kotlinx.coroutines.launch

/** 学习统计：今日进度、连续打卡、累计正确率 + 近 7 天复习量柱状图。 */
class StatsFragment : Fragment(R.layout.fragment_stats) {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: StatsViewModel by viewModels {
        StatsViewModel.factory((requireActivity().application as WordMemoryApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatsBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从复习页切回来时要看到最新数字
        viewModel.refresh()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun render(state: StatsUiState) {
        val stats = state.stats

        binding.dueValue.text = state.dueNow.toString()
        binding.todayValue.text = stats.todayTotal.toString()
        binding.streakValue.text = getString(R.string.stats_value_days, stats.streakDays)
        binding.totalValue.text = getString(R.string.stats_value_times, stats.totalReviewed)
        binding.accuracyValue.text = getString(R.string.stats_value_percent, stats.accuracyPercent)

        binding.weekChart.setData(stats.days)
        binding.weekChart.isVisible = stats.hasData
        binding.emptyText.isVisible = !state.loading && !stats.hasData
        binding.wordsValue.text = getString(R.string.stats_words, state.totalWords)
    }
}
