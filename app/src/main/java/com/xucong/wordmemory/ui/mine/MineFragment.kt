package com.xucong.wordmemory.ui.mine

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.xucong.wordmemory.R
import com.xucong.wordmemory.companion.CompanionBridge
import com.xucong.wordmemory.databinding.FragmentMineBinding
import com.xucong.wordmemory.notification.ReminderNotifications
import com.xucong.wordmemory.notification.ReminderPrefs
import com.xucong.wordmemory.notification.ReminderScheduler

/**
 * 「我的」页：提醒设置 + 伴生模块状态。
 *
 * 「立即发一条测试提醒」不是调试残留：验收要当场演示通知，而周期任务最早也要等到设定时刻，
 * 留一个手动触发入口既方便演示，也能让用户确认通知权限是否真的生效。
 */
class MineFragment : Fragment(R.layout.fragment_mine) {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMineBinding.bind(view)
        val context = requireContext()

        binding.reminderTimeText.text = getString(R.string.mine_reminder_time, ReminderPrefs.hour(context))

        binding.reminderSwitch.isChecked = ReminderPrefs.isEnabled(context)
        binding.reminderSwitch.setOnCheckedChangeListener { _, enabled ->
            ReminderPrefs.setEnabled(context, enabled)
            if (enabled) {
                ReminderScheduler.scheduleDaily(context)
            } else {
                ReminderScheduler.cancelAll(context)
            }
        }

        binding.testReminderButton.setOnClickListener {
            ReminderNotifications.showReminder(context, TEST_DUE_COUNT)
        }

        binding.companionInfo.text = getString(
            R.string.mine_companion_status,
            CompanionBridge.MODULE_NAME,
            CompanionBridge.isReady(),
        )
    }

    override fun onResume() {
        super.onResume()
        // 权限可能刚在系统设置里被改过，每次回到页面重新判断
        binding.permissionHint.isVisible = !ReminderNotifications.canPost(requireContext())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        /** 测试提醒里写死的「待复习数量」，仅用于演示通知长什么样。 */
        const val TEST_DUE_COUNT = 5
    }
}
