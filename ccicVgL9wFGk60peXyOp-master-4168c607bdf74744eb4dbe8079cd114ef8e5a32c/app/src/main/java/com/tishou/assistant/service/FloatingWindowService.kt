package com.tishou.assistant.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.tishou.assistant.R
import com.tishou.assistant.ui.MainActivity

/**
 * 悬浮窗服务
 * 
 * 功能：
 * 1. 显示 iOS 风格圆形悬浮球
 * 2. 支持拖拽移动
 * 3. 点击展开快捷菜单
 * 4. 快速控制服务启停
 */
class FloatingWindowService : Service() {

    companion object {
        var isRunning: Boolean = false
            private set
        
        /**
         * 启动悬浮窗服务
         */
        fun start(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java)
            context.startService(intent)
        }
        
        /**
         * 停止悬浮窗服务
         */
        fun stop(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatView: View
    private lateinit var params: WindowManager.LayoutParams
    
    // 悬浮球相关
    private var ballView: ImageView? = null
    private var menuView: LinearLayout? = null
    private var isMenuExpanded = false
    
    // 拖拽相关
    private var startX = 0f
    private var startY = 0f
    private var initialX = 0
    private var initialY = 0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_EXPAND_MENU") {
            toggleMenu()
        } else if (intent?.action == "ACTION_TOGGLE_SERVICE") {
            toggleService()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatView()
        isRunning = false
    }

    /**
     * 创建悬浮视图
     */
    @SuppressLint("InflateParams")
    private fun createFloatView() {
        // 加载悬浮球布局
        ballView = ImageView(this).apply {
            setImageResource(R.drawable.ic_floating_ball)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        // 设置布局参数
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - 200
            y = 300
            width = 120
            height = 120
        }

        // 添加触摸监听
        setupTouchListener()

        // 添加到窗口
        windowManager.addView(ballView, params)
        isRunning = true
    }

    /**
     * 获取合适的窗口类型
     */
    private fun getWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    /**
     * 设置触摸监听
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        ballView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startX).toInt()
                    val dy = (event.rawY - startY).toInt()
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(ballView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - startX)
                    val diffY = Math.abs(event.rawY - startY)
                    
                    // 判断是点击还是拖拽
                    if (diffX < 10 && diffY < 10) {
                        // 点击事件
                        toggleMenu()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 切换菜单显示
     */
    @SuppressLint("InflateParams")
    private fun toggleMenu() {
        if (isMenuExpanded) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    /**
     * 显示快捷菜单
     */
    @SuppressLint("InflateParams")
    private fun showMenu() {
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null) as? LinearLayout
        
        menuView?.let { menu ->
            val menuParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = params.x + 140
                y = params.y
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }

            // 设置菜单项点击事件
            menu.findViewById<View>(R.id.btnStart)?.setOnClickListener {
                toggleService()
                hideMenu()
            }
            
            menu.findViewById<View>(R.id.btnSettings)?.setOnClickListener {
                openSettings()
                hideMenu()
            }
            
            menu.findViewById<View>(R.id.btnClose)?.setOnClickListener {
                stopService()
                hideMenu()
            }

            windowManager.addView(menu, menuParams)
            isMenuExpanded = true
        }
    }

    /**
     * 隐藏菜单
     */
    private fun hideMenu() {
        menuView?.let {
            windowManager.removeView(it)
            menuView = null
            isMenuExpanded = false
        }
    }

    /**
     * 切换服务状态
     */
    private fun toggleService() {
        if (TiShouAccessibilityService.isAvailable()) {
            // 停止服务
            KeepAliveService.stop(this)
            Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show()
        } else {
            // 启动服务
            KeepAliveService.start(this)
            Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开设置页面
     */
    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    /**
     * 移除悬浮视图
     */
    private fun removeFloatView() {
        hideMenu()
        ballView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // 忽略移除失败
            }
            ballView = null
        }
    }

    /**
     * 检查悬浮窗权限
     */
    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestOverlayPermission(activity: android.app.Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                activity.startActivityForResult(intent, 1002)
            }
        }
    }
}