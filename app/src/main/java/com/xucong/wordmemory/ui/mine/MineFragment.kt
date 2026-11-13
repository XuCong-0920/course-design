package com.xucong.wordmemory.ui.mine

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.xucong.wordmemory.R
import com.xucong.wordmemory.companion.CompanionBridge
import com.xucong.wordmemory.databinding.FragmentMineBinding
import com.xucong.wordmemory.notification.ReminderNotifications
import com.xucong.wordmemory.notification.ReminderPrefs
import com.xucong.wordmemory.notification.ReminderScheduler
import kotlinx.coroutines.launch

/**
 * 「我的」页：提醒设置 + 权限降级引导 + 伴生模块状态。
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

        binding.openNotificationSettings.setOnClickListener { openNotificationSettings() }
    }

    override fun onResume() {
        super.onResume()
        // 权限可能刚在系统设置里被改过，每次回到页面重新判断
        val canPost = ReminderNotifications.canPost(requireContext())
        binding.permissionHint.isVisible = !canPost
        binding.openNotificationSettings.isVisible = !canPost
        refreshCompanionStatus()
    }

    /**
     * 伴生模块的状态来自它自己的只读入口，而不是直接查库：
     * 这里能显示数字，说明绑定链路（Application 注入 → 模块读取）真的通了。
     */
    private fun refreshCompanionStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            val total = CompanionBridge.totalWordCount()
            val due = CompanionBridge.dueCount()
            binding.companionInfo.text = if (CompanionBridge.isReady() && total >= 0 && due >= 0) {
                getString(R.string.mine_companion_status, CompanionBridge.MODULE_NAME, total, due)
            } else {
                getString(R.string.mine_companion_unbound, CompanionBridge.MODULE_NAME)
            }
        }
    }

    /** 降级路径：没有通知权限时给一个能直达系统开关的按钮，而不是只留一行红字。 */
    private fun openNotificationSettings() {
        val packageName = requireContext().packageName
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
        }
        runCatching { startActivity(intent) }
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
