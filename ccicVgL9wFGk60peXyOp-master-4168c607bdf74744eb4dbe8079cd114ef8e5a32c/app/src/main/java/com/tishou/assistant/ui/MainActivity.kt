package com.tishou.assistant.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tishou.assistant.R
import com.tishou.assistant.core.LogLevel
import com.tishou.assistant.core.TiShouCore
import com.tishou.assistant.databinding.ActivityMainBinding
import com.tishou.assistant.service.KeepAliveService
import com.tishou.assistant.service.TiShouAccessibilityService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 主界面 Activity
 * 
 * 功能：
 * 1. 显示服务运行状态
 * 2. 启动/停止无障碍服务
 * 3. 显示抢单统计
 * 4. 进入设置、规则配置、日志查看等页面
 * 
 * 设计风格：融合 iOS 圆润风格的 Material Design 3
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var core: TiShouCore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化核心
        core = TiShouCore.getInstance(this)
        
        // 绑定视图
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeCoreState()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    /**
     * 设置 UI 事件
     */
    private fun setupUI() {
        // 主开关按钮 - 启动/停止服务
        binding.mainSwitchButton.setOnClickListener {
            if (isAccessibilityServiceEnabled()) {
                stopService()
            } else {
                showAccessibilityGuide()
            }
        }

        // 规则配置入口
        binding.ruleConfigCard.setOnClickListener {
            startActivity(Intent(this, RuleConfigActivity::class.java))
        }

        // 日志查看入口
        binding.logCard.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        // 统计入口
        binding.statsCard.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        // 设置入口
        binding.settingsCard.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 关于入口
        binding.aboutCard.setOnClickListener {
            showAboutDialog()
        }
    }

    /**
     * 观察核心状态变化
     */
    private fun observeCoreState() {
        lifecycleScope.launch {
            core.stateFlow.collectLatest { state ->
                updateUI(state)
            }
        }

        lifecycleScope.launch {
            core.logFlow.collectLatest { logEntry ->
                // 可以在这里更新日志 UI
            }
        }
    }

    /**
     * 更新 UI 状态
     */
    private fun updateUI(state: TiShouCore.CoreState) {
        runOnUiThread {
            // 更新主开关状态
            binding.mainSwitchButton.isChecked = state.isRunning
            
            // 更新状态文本
            binding.statusText.text = when {
                state.isRunning -> "服务运行中"
                isAccessibilityServiceEnabled() -> "点击启动服务"
                else -> "请先开启无障碍权限"
            }

            // 更新统计信息
            binding.todayGrabCount.text = state.todayGrabCount.toString()
            binding.successRateText.text = String.format("%.1f%%", state.successRate)
            binding.lastGrabTimeText.text = state.lastGrabTime?.let { 
                formatRelativeTime(it) 
            } ?: "暂无"

            // 更新按钮样式
            updateSwitchButtonStyle(state.isRunning)
        }
    }

    /**
     * 更新服务状态显示
     */
    private fun updateServiceStatus() {
        val enabled = isAccessibilityServiceEnabled()
        
        if (!enabled) {
            binding.statusCard.setCardBackgroundColor(getColor(R.color.status_warning))
            binding.statusText.text = "无障碍服务未开启"
            binding.mainSwitchButton.text = "去开启"
        } else {
            binding.statusCard.setCardBackgroundColor(getColor(R.color.status_success))
            binding.statusText.text = "服务已就绪"
            binding.mainSwitchButton.text = "启动服务"
        }
    }

    /**
     * 检查无障碍服务是否启用
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )

        for (service in enabledServices) {
            if (service.resolveInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }

    /**
     * 显示无障碍服务开启引导
     */
    private fun showAccessibilityGuide() {
        Toast.makeText(this, "请先开启无障碍服务权限", Toast.LENGTH_LONG).show()
        
        // 跳转到无障碍设置页面
        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    /**
     * 启动服务
     */
    private fun startService() {
        // 启动前台保活服务
        val keepAliveIntent = Intent(this, KeepAliveService::class.java)
        startForegroundService(keepAliveIntent)

        core.startMonitoring()
        
        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show()
    }

    /**
     * 停止服务
     */
    private fun stopService() {
        core.stopMonitoring()
        
        // 停止保活服务
        val keepAliveIntent = Intent(this, KeepAliveService::class.java)
        stopService(keepAliveIntent)
        
        Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show()
    }

    /**
     * 更新开关按钮样式
     */
    private fun updateSwitchButtonStyle(isRunning: Boolean) {
        if (isRunning) {
            binding.mainSwitchButton.setBackgroundColor(getColor(R.color.primary_dark))
            binding.mainSwitchButton.text = "停止服务"
        } else {
            binding.mainSwitchButton.setBackgroundColor(getColor(R.color.primary))
            binding.mainSwitchButton.text = "启动服务"
        }
    }

    /**
     * 格式化相对时间
     */
    private fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            diff < 86400_000 -> "${diff / 3600_000}小时前"
            else -> "${diff / 86400_000}天前"
        }
    }

    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        Toast.makeText(this, "替手 TiShou v1.0.0\nAndroid 自动化辅助工具", Toast.LENGTH_LONG).show()
    }
}