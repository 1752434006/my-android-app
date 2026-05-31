package com.tishou.assistant.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.tishou.assistant.R
import com.tishou.assistant.service.TiShouAccessibilityService

/**
 * 权限引导活动
 * 
 * 引导用户开启必要的权限：
 * - 无障碍服务
 * - 悬浮窗权限
 * - 电池优化白名单
 * - 通知权限
 */
class PermissionGuideActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: Button
    private lateinit var indicatorContainer: ViewGroup
    
    private val permissions = listOf(
        PermissionInfo(
            R.drawable.ic_accessibility,
            "无障碍服务",
            "允许替手自动监控屏幕内容并执行点击操作",
            "这是核心功能，必须开启才能自动抢单"
        ),
        PermissionInfo(
            R.drawable.ic_overlay,
            "悬浮窗权限",
            "允许在其它应用上层显示悬浮球",
            "用于快速控制服务启停和查看状态"
        ),
        PermissionInfo(
            R.drawable.ic_battery,
            "电池优化白名单",
            "允许后台持续运行不被系统杀死",
            "澎湃 OS 必开，否则无法后台抢单"
        ),
        PermissionInfo(
            R.drawable.ic_notification,
            "通知权限",
            "允许显示抢单结果和服务状态通知",
            "确保及时收到抢单成功提醒"
        )
    )
    
    private var currentPage = 0
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // 无论是否授权都继续
        if (currentPage < permissions.size - 1) {
            currentPage++
            updatePage()
        } else {
            finishGuide()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_guide)
        
        initViews()
        setupViewPager()
    }
    
    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)
        indicatorContainer = findViewById(R.id.indicatorContainer)
        
        btnNext.setOnClickListener {
            handleNextClick()
        }
        
        btnSkip.setOnClickListener {
            finishGuide()
        }
    }
    
    private fun setupViewPager() {
        viewPager.adapter = PermissionGuideAdapter(this, permissions)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                updateIndicator()
                updateButtonText()
            }
        })
        
        updateIndicator()
        updateButtonText()
    }
    
    private fun handleNextClick() {
        when (currentPage) {
            0 -> {
                // 引导开启无障碍服务
                openAccessibilitySettings()
            }
            1 -> {
                // 引导开启悬浮窗权限
                requestOverlayPermission()
            }
            2 -> {
                // 引导开启电池优化白名单
                requestBatteryOptimizationExemption()
            }
            3 -> {
                // 请求通知权限 (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    finishGuide()
                }
            }
            else -> {
                finishGuide()
            }
        }
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        
        // 延迟后自动切换到下一页
        viewPager.postDelayed({
            if (currentPage < permissions.size - 1) {
                currentPage++
                viewPager.currentItem = currentPage
            }
        }, 1500)
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
        
        // 延迟后自动切换到下一页
        viewPager.postDelayed({
            if (currentPage < permissions.size - 1) {
                currentPage++
                viewPager.currentItem = currentPage
            }
        }, 1500)
    }
    
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                // 已经在白名单中
                viewPager.postDelayed({
                    if (currentPage < permissions.size - 1) {
                        currentPage++
                        viewPager.currentItem = currentPage
                    }
                }, 500)
            } else {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                
                viewPager.postDelayed({
                    if (currentPage < permissions.size - 1) {
                        currentPage++
                        viewPager.currentItem = currentPage
                    }
                }, 1500)
            }
        } else {
            viewPager.postDelayed({
                if (currentPage < permissions.size - 1) {
                    currentPage++
                    viewPager.currentItem = currentPage
                }
            }, 500)
        }
    }
    
    private fun updatePage() {
        viewPager.currentItem = currentPage
    }
    
    private fun updateIndicator() {
        indicatorContainer.removeAllViews()
        
        for (i in permissions.indices) {
            val dot = View(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4,
                    resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 8
                }
                setBackgroundResource(if (i == currentPage) R.drawable.indicator_dot_active else R.drawable.indicator_dot_inactive)
            }
            indicatorContainer.addView(dot)
        }
    }
    
    private fun updateButtonText() {
        if (currentPage == permissions.size - 1) {
            btnNext.text = "完成"
            btnSkip.visibility = View.GONE
        } else {
            btnNext.text = "下一步"
            btnSkip.visibility = View.VISIBLE
        }
    }
    
    private fun finishGuide() {
        // 检查无障碍服务是否已启用
        if (isAccessibilityServiceEnabled()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // 如果无障碍服务未启用，显示提示
            android.widget.Toast.makeText(
                this,
                "无障碍服务未开启，请返回设置中开启",
                android.widget.Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as android.accessibilityservice.AccessibilityServiceManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName &&
                service.resolveInfo.serviceInfo.name == TiShouAccessibilityService::class.java.name
            ) {
                return true
            }
        }
        return false
    }
    
    data class PermissionInfo(
        val iconRes: Int,
        val title: String,
        val description: String,
        val note: String
    )
    
    inner class PermissionGuideAdapter(
        private val activity: PermissionGuideActivity,
        private val permissions: List<PermissionInfo>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<PermissionGuideAdapter.ViewHolder>() {
        
        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.permissionIcon)
            val title: TextView = itemView.findViewById(R.id.permissionTitle)
            val description: TextView = itemView.findViewById(R.id.permissionDescription)
            val note: TextView = itemView.findViewById(R.id.permissionNote)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(activity).inflate(R.layout.item_permission_guide, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val permission = permissions[position]
            holder.icon.setImageResource(permission.iconRes)
            holder.title.text = permission.title
            holder.description.text = permission.description
            holder.note.text = permission.note
        }
        
        override fun getItemCount(): Int = permissions.size
    }
}