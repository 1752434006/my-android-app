package com.tishou.assistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.tishou.assistant.core.TiShouCore
import com.tishou.assistant.core.LogLevel
import com.tishou.assistant.model.OrderInfo
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 替手无障碍服务
 * 
 * 核心功能：
 * 1. 监控界面变化（订单列表刷新、新订单出现）
 * 2. 自动查找并点击目标订单
 * 3. 配合 OCR 识别订单详情
 * 4. 执行复杂手势操作（滑动、长按）
 */
class TiShouAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_ORDER_GRABBED = "com.tishou.action.ORDER_GRABBED"
        
        @Volatile
        private var instance: TiShouAccessibilityService? = null
        
        fun getInstance(): TiShouAccessibilityService? = instance
        
        /**
         * 检查服务是否可用
         */
        fun isAvailable(): Boolean = instance != null && instance!!.isRunning.get()
    }

    private lateinit var core: TiShouCore
    private val isRunning = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    
    // 订单监控相关
    private var isMonitoring = false
    private var targetKeywords = listOf<String>()
    private var checkInterval = 500L // 默认 500ms 检查一次

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        core = TiShouCore.getInstance()
        
        // 配置服务信息
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
        
        core.log("无障碍服务已连接", LogLevel.INFO)
        isRunning.set(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isRunning.get() || !isMonitoring) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(event)
            }
        }
    }

    override fun onInterrupt() {
        core.log("无障碍服务被中断", LogLevel.WARNING)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning.set(false)
        isMonitoring = false
        core.log("无障碍服务已销毁", LogLevel.INFO)
    }

    /**
     * 开始监控订单
     */
    fun startMonitoring(keywords: List<String>, interval: Long = 500L) {
        targetKeywords = keywords
        checkInterval = interval
        isMonitoring = true
        core.log("开始监控订单，关键词：${keywords.joinToString(", ")}", LogLevel.INFO)
    }

    /**
     * 停止监控
     */
    fun stopMonitoring() {
        isMonitoring = false
        core.log("停止订单监控", LogLevel.INFO)
    }

    /**
     * 处理窗口状态变化
     */
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        // 这里可以根据包名判断是否在目标应用中
        core.log("窗口状态变化：$packageName", LogLevel.DEBUG)
        
        if (isMonitoring) {
            // 延迟检查，等待界面稳定
            handler.postDelayed({
                scanForOrders()
            }, 300)
        }
    }

    /**
     * 处理窗口内容变化
     */
    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        if (!isMonitoring) return
        
        // 内容变化时扫描订单
        handler.postDelayed({
            scanForOrders()
        }, checkInterval)
    }

    /**
     * 处理视图点击事件
     */
    private fun handleViewClicked(event: AccessibilityEvent) {
        val text = event.text?.joinToString(", ") ?: ""
        core.log("用户点击：$text", LogLevel.DEBUG)
    }

    /**
     * 扫描订单列表
     */
    private fun scanForOrders() {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // 查找包含关键词的节点
            for (keyword in targetKeywords) {
                val targetNode = findNodeByText(rootNode, keyword)
                if (targetNode != null) {
                    core.log("发现目标订单：$keyword", LogLevel.INFO)
                    handleOrderFound(targetNode, keyword)
                    return
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * 根据文本查找节点
     */
    private fun findNodeByText(root: AccessibilityNodeInfo, keyword: String): AccessibilityNodeInfo? {
        // 广度优先搜索
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            
            // 检查文本内容
            val text = node.text?.toString() ?: ""
            if (text.contains(keyword, ignoreCase = true)) {
                return node
            }
            
            // 检查内容描述
            val desc = node.contentDescription?.toString() ?: ""
            if (desc.contains(keyword, ignoreCase = true)) {
                return node
            }
            
            // 添加子节点到队列
            for (i in 0 until node.childCount) {
                queue.add(node.getChild(i))
            }
        }
        
        return null
    }

    /**
     * 处理找到的订单
     */
    private fun handleOrderFound(node: AccessibilityNodeInfo, keyword: String) {
        // 触发 OCR 识别
        triggerOcr(node)
        
        // 自动点击（根据配置决定是否立即点击）
        if (core.config.autoGrab) {
            clickNode(node)
        }
    }

    /**
     * 触发 OCR 识别
     */
    private fun triggerOcr(node: AccessibilityNodeInfo) {
        try {
            // 获取节点区域
            val rect = Rect()
            node.getBoundsInScreen(rect)
            
            // 通知 OCR 模块截图识别
            // 这里需要与 OcrManager 协作
            core.log("触发 OCR 识别区域：${rect.toShortString()}", LogLevel.DEBUG)
        } catch (e: Exception) {
            core.log("OCR 触发失败：${e.message}", LogLevel.ERROR)
        }
    }

    /**
     * 点击节点
     */
    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (result) {
                core.log("点击成功", LogLevel.INFO)
                
                // 广播抢单事件
                broadcastOrderGrabbed()
            } else {
                core.log("点击失败，尝试手势模拟", LogLevel.WARNING)
                performGestureClick(node)
            }
            result
        } catch (e: Exception) {
            core.log("点击异常：${e.message}", LogLevel.ERROR)
            false
        }
    }

    /**
     * 使用手势模拟点击（更可靠）
     */
    private fun performGestureClick(node: AccessibilityNodeInfo) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        val centerX = rect.centerX().toFloat()
        val centerY = rect.centerY().toFloat()
        
        val path = Path().apply {
            moveTo(centerX, centerY)
            lineTo(centerX, centerY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                core.log("手势点击完成", LogLevel.INFO)
            }
            
            override fun onCancelled(gestureDescription: GestureDescription) {
                core.log("手势点击取消", LogLevel.WARNING)
            }
        }, handler)
    }

    /**
     * 滑动操作
     */
    fun performSwipe(fromX: Float, fromY: Float, toX: Float, toY: Float, duration: Long = 300) {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        
        dispatchGesture(gesture, null, handler)
    }

    /**
     * 向下滑动
     */
    fun swipeDown() {
        val rect = Rect()
        rootInActiveWindow?.getBoundsInScreen(rect)
        
        if (rect.height() > 0) {
            val startX = rect.centerX().toFloat()
            val startY = rect.bottom * 0.8f
            val endY = rect.top * 0.2f
            
            performSwipe(startX, startY, startX, endY, 500)
        }
    }

    /**
     * 向上滑动
     */
    fun swipeUp() {
        val rect = Rect()
        rootInActiveWindow?.getBoundsInScreen(rect)
        
        if (rect.height() > 0) {
            val startX = rect.centerX().toFloat()
            val startY = rect.top * 0.2f
            val endY = rect.bottom * 0.8f
            
            performSwipe(startX, startY, startX, endY, 500)
        }
    }

    /**
     * 广播抢单成功事件
     */
    private fun broadcastOrderGrabbed() {
        val intent = Intent(ACTION_ORDER_GRABBED).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    /**
     * 查找指定 ID 的节点
     */
    fun findNodeById(id: String): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findAccessibilityNodeInfosByViewId(id)?.firstOrNull()
    }

    /**
     * 查找指定类名的节点
     */
    fun findNodeByClassName(className: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        rootInActiveWindow?.let { queue.add(it) }
        
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.className?.toString() == className) {
                return node
            }
            for (i in 0 until node.childCount) {
                queue.add(node.getChild(i))
            }
        }
        return null
    }
}