package com.tishou.assistant.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tishou.assistant.R
import com.tishou.assistant.core.Statistics
import com.tishou.assistant.core.TiShouCore
import com.tishou.assistant.databinding.ActivityStatsBinding

/**
 * 统计分析界面 Activity
 * 
 * 功能：
 * 1. 显示抢单统计数据（总数、成功率、平均响应时间）
 * 2. 展示七日抢单趋势图表
 * 3. 展示成功率趋势图表
 * 4. 支持重置统计数据
 * 
 * 设计风格：融合 iOS 圆润风格的 Material Design 3
 */
class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private lateinit var core: TiShouCore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化核心
        core = TiShouCore.getInstance(this)
        
        // 绑定视图
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadStatistics()
    }

    /**
     * 设置 UI 事件
     */
    private fun setupUI() {
        // 返回按钮
        binding.backButton.setOnClickListener {
            finish()
        }

        // 重置统计按钮
        binding.resetStatsButton.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    /**
     * 加载统计数据
     */
    private fun loadStatistics() {
        val stats = core.getStatistics() ?: Statistics()
        
        // 更新总览数据
        binding.totalGrabCount.text = stats.totalGrabbed.toString()
        binding.successRate.text = String.format("%.1f%%", stats.getSuccessRate())
        binding.avgResponseTime.text = "${stats.avgResponseTimeMs}ms"
        
        // TODO: 集成 MPAndroidChart 后实现图表绘制
        // drawSevenDayChart(stats)
        // drawSuccessRateChart(stats)
    }

    /**
     * 显示重置确认对话框
     */
    private fun showResetConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("重置统计数据")
            .setMessage("确定要重置所有统计数据吗？此操作不可恢复。")
            .setPositiveButton("重置") { _, _ ->
                resetStatistics()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 重置统计数据
     */
    private fun resetStatistics() {
        core.resetStatistics()
        loadStatistics()
        Toast.makeText(this, "统计数据已重置", Toast.LENGTH_SHORT).show()
    }

    /**
     * 绘制七日抢单趋势图（待实现）
     * 需要集成 MPAndroidChart 库
     */
    private fun drawSevenDayChart(stats: Statistics) {
        // TODO: 使用 MPAndroidChart 的 LineChart 或 BarChart
        // 1. 获取近 7 天的抢单数据
        // 2. 设置 X 轴标签（日期）
        // 3. 设置 Y 轴数据（抢单数量）
        // 4. 配置图表样式（颜色、线条、填充等）
    }

    /**
     * 绘制成功率趋势图（待实现）
     * 需要集成 MPAndroidChart 库
     */
    private fun drawSuccessRateChart(stats: Statistics) {
        // TODO: 使用 MPAndroidChart 的 LineChart
        // 1. 获取近 7 天的成功率数据
        // 2. 设置 X 轴标签（日期）
        // 3. 设置 Y 轴数据（成功率百分比）
        // 4. 配置图表样式
    }
}