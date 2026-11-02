package com.xucong.wordmemory

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.xucong.wordmemory.databinding.ActivityMainBinding
import com.xucong.wordmemory.notification.ReminderNotifications
import com.xucong.wordmemory.notification.ReminderScheduler

/**
 * 应用唯一 Activity：承载 Navigation 的 NavHostFragment 与底部导航。
 * 各页面均为 Fragment，避免多 Activity 带来的返回栈与状态管理问题。
 *
 * 同时负责两件「进程级」的事：申请通知权限、把提醒任务排上（排程是幂等的，每次启动都能安全调用）。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // 拒绝也不阻断任何功能：提醒任务照常排，只是到点发不出通知（canPost 会拦住），
        // 复习页里的「到期数量」仍是完整的降级信息，不需要额外提示。
        if (granted) ReminderScheduler.scheduleDaily(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)

        applyReminder(intent)
        ensureNotificationPermission()
        ReminderScheduler.scheduleDaily(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyReminder(intent)
    }

    /** 从通知点进来时直接落在复习页，省掉「首页 → 复习」这一步。 */
    private fun applyReminder(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderNotifications.EXTRA_OPEN_REVIEW, false) == true) {
            binding.bottomNav.selectedItemId = R.id.reviewFragment
            intent.removeExtra(ReminderNotifications.EXTRA_OPEN_REVIEW)
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
