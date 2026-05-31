package com.tishou.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tishou.assistant.R
import com.tishou.assistant.databinding.ActivitySplashBinding

/**
 * 启动页 Activity
 * 
 * 负责应用启动时的权限检查和引导
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())

    // 需要的权限列表
    private val requiredPermissions = mutableListOf(
        Manifest.permission.POST_NOTIFICATIONS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // 所有权限已授予，检查悬浮窗权限
            checkOverlayPermission()
        } else {
            // 有权限被拒绝，显示提示
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 全屏显示
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 延迟检查权限并跳转
        handler.postDelayed({
            checkPermissions()
        }, 1500)
    }

    /**
     * 检查必要权限
     */
    private fun checkPermissions() {
        val notGrantedPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isEmpty()) {
            // 所有权限已授予
            checkOverlayPermission()
        } else {
            // 申请缺失的权限
            permissionLauncher.launch(notGrantedPermissions.toTypedArray())
        }
    }

    /**
     * 检查悬浮窗权限
     */
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // 需要悬浮窗权限
                showOverlayPermissionGuide()
            } else {
                // 所有权限就绪，跳转主界面
                navigateToMain()
            }
        } else {
            navigateToMain()
        }
    }

    /**
     * 显示悬浮窗权限引导
     */
    private fun showOverlayPermissionGuide() {
        binding.apply {
            permissionTitle.text = "开启悬浮窗权限"
            permissionDesc.text = "替手需要悬浮窗权限来显示抢单状态浮球和快捷操作菜单"
            permissionIcon.setImageResource(android.R.drawable.ic_dialog_info)
            actionButton.text = "去设置"
            cancelButton.visibility = android.view.View.VISIBLE
            
            actionButton.setOnClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
            
            cancelButton.setOnClickListener {
                navigateToMain()
            }
        }
    }

    /**
     * 显示权限被拒绝对话框
     */
    private fun showPermissionDeniedDialog() {
        binding.apply {
            permissionTitle.text = "部分权限未授予"
            permissionDesc.text = "某些功能可能无法正常使用，建议前往设置手动开启"
            permissionIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            actionButton.text = "去设置"
            cancelButton.visibility = android.view.View.VISIBLE
            
            actionButton.setOnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            
            cancelButton.setOnClickListener {
                navigateToMain()
            }
        }
    }

    /**
     * 跳转到主界面
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}