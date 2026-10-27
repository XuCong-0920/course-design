package com.xucong.wordmemory.ui.mine

import android.os.Bundle
import android.view.View
import com.xucong.wordmemory.R
import com.xucong.wordmemory.companion.CompanionBridge
import com.xucong.wordmemory.ui.common.BasePlaceholderFragment

class MineFragment : BasePlaceholderFragment() {
    override val titleRes: Int = R.string.mine_title
    override val hintRes: Int = R.string.mine_placeholder

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 验证 app 与 companion 两个 module 的依赖是否真的打通
        binding.hintText.append(
            "\n\n伴生模块：${CompanionBridge.MODULE_NAME}（就绪=${CompanionBridge.isReady()}）",
        )
    }
}
