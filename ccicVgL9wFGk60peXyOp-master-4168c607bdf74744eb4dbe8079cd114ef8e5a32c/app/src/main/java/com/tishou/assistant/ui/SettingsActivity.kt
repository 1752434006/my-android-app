package com.tishou.assistant.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tishou.assistant.R
import com.tishou.assistant.databinding.ActivitySettingsBinding

/**
 * 应用设置界面 Activity
 * 
 * 功能：
 * 1. 通用设置（自动启动、震动反馈、声音提示）
 * 2. OCR 设置（API Key 配置、图像预处理）
 * 3. 高级设置（配置导入导出、清除数据）
 * 
 * 设计风格：融合 iOS 圆润风格的 Material Design 3
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 绑定视图
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadSettings()
    }

    /**
     * 设置 UI 事件
     */
    private fun setupUI() {
        // 返回按钮
        binding.backButton.setOnClickListener {
            finish()
        }

        // 自动启动服务开关
        binding.autoStartSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveAutoStartSetting(isChecked)
        }

        // 震动反馈开关
        binding.vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveVibrationSetting(isChecked)
        }

        // 声音提示开关
        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveSoundSetting(isChecked)
        }

        // 图像预处理开关
        binding.preprocessSwitch.setOnCheckedChangeListener { _, isChecked ->
            savePreprocessSetting(isChecked)
        }

        // OCR API Key 输入框
        binding.ocrApiKeyInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val apiKey = binding.ocrApiKeyInput.text.toString().trim()
                saveOcrApiKey(apiKey)
            }
        }

        // 导出配置
        binding.exportConfigCard.setOnClickListener {
            exportConfiguration()
        }

        // 导入配置
        binding.importConfigCard.setOnClickListener {
            importConfiguration()
        }

        // 清除数据
        binding.clearDataButton.setOnClickListener {
            showClearDataConfirmationDialog()
        }
    }

    /**
     * 加载设置
     */
    private fun loadSettings() {
        val prefs = getSharedPreferences("tishou_settings", MODE_PRIVATE)
        
        // 加载通用设置
        binding.autoStartSwitch.isChecked = prefs.getBoolean("auto_start", false)
        binding.vibrationSwitch.isChecked = prefs.getBoolean("vibration", true)
        binding.soundSwitch.isChecked = prefs.getBoolean("sound", true)
        binding.preprocessSwitch.isChecked = prefs.getBoolean("preprocess", true)
        
        // 加载 OCR 设置
        val apiKey = prefs.getString("ocr_api_key", "") ?: ""
        binding.ocrApiKeyInput.setText(apiKey)
    }

    /**
     * 保存自动启动设置
     */
    private fun saveAutoStartSetting(enabled: Boolean) {
        val prefs = getSharedPreferences("tishou_settings", MODE_PRIVATE)
        prefs.edit().putBoolean("auto_start", enabled).apply()
        Toast.makeText(this, "自动启动已${if (enabled) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
    }

    /**
     * 保存震动反馈设置
     */
    private fun saveVibrationSetting(enabled: Boolean) {
        val prefs = getSharedPreferences("tishou_settings", MODE_PRIVATE)
        prefs.edit().putBoolean("vibration", enabled).apply()
    }

    /**
     * 保存声音提示设置
     */
    private fun saveSoundSetting(enabled: Boolean) {
        val prefs = getSharedPreferences("tishou_settings", MODE_PRIVATE)
        prefs.edit().putBoolean("sound", enabled).apply()
    }

    /**
     * 保存图像预处理设置
     */
    private fun savePreprocessSetting(enabled: Boolean) {
        val prefs = getSharedPreferences("tishou_settings", MODE_PRIVATE)
        prefs.edit().putBoolean("preprocess", enabled).apply()
    }

    /**
     * 保存 OCR API Key
     */
    private fun saveOcrApiKey(apiKey: String) {
        if (apiKey.isEmpty()) {
            return
        }
        val prefs = getSharedPreferences("tishou_settings", MODE_PRIVATE)
        prefs.edit().putString("ocr_api_key", apiKey).apply()
        Toast.makeText(this, "API Key 已保存", Toast.LENGTH_SHORT).show()
    }

    /**
     * 导出配置文件
     */
    private fun exportConfiguration() {
        // TODO: 实现配置导出功能
        // 1. 读取 SharedPreferences 中的所有配置
        // 2. 序列化为 JSON 格式
        // 3. 使用 FileProvider 创建分享 Intent
        // 4. 启动系统分享选择器
        Toast.makeText(this, "配置导出功能开发中", Toast.LENGTH_SHORT).show()
    }

    /**
     * 导入配置文件
     */
    private fun importConfiguration() {
        // TODO: 实现配置导入功能
        // 1. 使用 ActivityResultLauncher 启动文件选择器
        // 2. 读取选中的 JSON 配置文件
        // 3. 解析并更新 SharedPreferences
        // 4. 刷新 UI 显示
        Toast.makeText(this, "配置导入功能开发中", Toast.LENGTH_SHORT).show()
    }

    /**
     * 显示清除数据确认对话框
     */
    private fun showClearDataConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("清除所有数据")
            .setMessage("确定要清除所有配置和抢单记录吗？此操作不可恢复！")
            .setPositiveButton("清除") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 清除所有数据
     */
    private fun clearAllData() {
        // 清除 SharedPreferences
        val prefs = getSharedPreferences("tishou_settings", MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        // 清除数据库（如果有）
        // TiShouDatabase.getInstance(this).clearAllTables()
        
        Toast.makeText(this, "所有数据已清除", Toast.LENGTH_SHORT).show()
        
        // 重启应用或返回主页
        finish()
    }
}