package com.xucong.wordmemory.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.xucong.wordmemory.databinding.FragmentPlaceholderBinding

/**
 * 骨架阶段各页面共用的占位实现。
 *
 * 用抽象属性而不是构造参数传文案：Fragment 必须保留无参构造函数，
 * 否则进程被回收后重建会抛 InstantiationException。
 */
abstract class BasePlaceholderFragment : Fragment() {

    @get:StringRes
    protected abstract val titleRes: Int

    @get:StringRes
    protected abstract val hintRes: Int

    private var _binding: FragmentPlaceholderBinding? = null
    protected val binding get() = requireNotNull(_binding) { "binding 只在 onCreateView 与 onDestroyView 之间可用" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleText.setText(titleRes)
        binding.hintText.setText(hintRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
