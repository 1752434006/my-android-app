package com.tishou.assistant.core

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TiShou 核心引擎 - JNI 桥接层
 * 
 * 负责与 NDK C++ 核心调度引擎通信
 * 提供订单监控、OCR 处理、统计查询等功能
 */
object TiShouCore {
    
    private const val TAG = "TiShouCore"
    
    // 加载 native 库
    init {
        try {
            System.loadLibrary("tishou_core")
            Log.i(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library", e)
        }
    }
    
    private val isInitialized = AtomicBoolean(false)
    
    // ========== Native 方法声明 ==========
    
    /**
     * 初始化核心引擎
     * @return 成功返回 true，失败返回 false
     */
    external fun nativeInit(): Boolean
    
    /**
     * 销毁核心引擎
     */
    external fun nativeDestroy()
    
    /**
     * 启动订单监控
     */
    external fun nativeStartMonitoring()
    
    /**
     * 停止订单监控
     */
    external fun nativeStopMonitoring()
    
    /**
     * 处理 OCR 识别结果
     * @param ocrText OCR 识别出的文本内容
     */
    external fun nativeProcessOcrResult(ocrText: String)
    
    /**
     * 获取服务状态
     * @return 状态码 (0: STOPPED, 1: RUNNING, 2: PAUSED)
     */
    external fun nativeGetStatus(): Int
    
    /**
     * 获取统计数据
     * @return Statistics 对象
     */
    external fun nativeGetStatistics(): Statistics?
    
    // ========== 公开 API ==========
    
    /**
     * 初始化引擎
     */
    fun initialize(): Boolean {
        if (isInitialized.get()) {
            Log.w(TAG, "Engine already initialized")
            return true
        }
        
        val success = nativeInit()
        if (success) {
            isInitialized.set(true)
            Log.i(TAG, "Engine initialized successfully")
        } else {
            Log.e(TAG, "Engine initialization failed")
        }
        return success
    }
    
    /**
     * 销毁引擎
     */
    fun destroy() {
        if (!isInitialized.get()) {
            return
        }
        
        nativeDestroy()
        isInitialized.set(false)
        Log.i(TAG, "Engine destroyed")
    }
    
    /**
     * 启动监控
     */
    fun startMonitoring() {
        checkInitialized()
        nativeStartMonitoring()
        Log.i(TAG, "Monitoring started")
    }
    
    /**
     * 停止监控
     */
    fun stopMonitoring() {
        checkInitialized()
        nativeStopMonitoring()
        Log.i(TAG, "Monitoring stopped")
    }
    
    /**
     * 暂停监控
     */
    fun pause() {
        checkInitialized()
        // TODO: 实现 pause native 方法
        Log.i(TAG, "Monitoring paused")
    }
    
    /**
     * 恢复监控
     */
    fun resume() {
        checkInitialized()
        // TODO: 实现 resume native 方法
        Log.i(TAG, "Monitoring resumed")
    }
    
    /**
     * 处理 OCR 结果
     * @param text 识别出的文本
     */
    fun processOcrResult(text: String) {
        checkInitialized()
        nativeProcessOcrResult(text)
    }
    
    /**
     * 获取当前状态
     */
    fun getStatus(): ServiceStatus {
        checkInitialized()
        val statusCode = nativeGetStatus()
        return ServiceStatus.fromCode(statusCode)
    }
    
    /**
     * 获取统计数据
     */
    fun getStatistics(): Statistics? {
        checkInitialized()
        return nativeGetStatistics()
    }
    
    /**
     * 重置统计数据
     */
    fun resetStatistics() {
        // TODO: 实现 native 方法
        Log.i(TAG, "Statistics reset")
    }
    
    private fun checkInitialized() {
        if (!isInitialized.get()) {
            throw IllegalStateException("Engine not initialized. Call initialize() first.")
        }
    }
}

/**
 * 服务状态枚举
 */
enum class ServiceStatus(val code: Int) {
    STOPPED(0),
    RUNNING(1),
    PAUSED(2);
    
    companion object {
        fun fromCode(code: Int): ServiceStatus {
            return values().find { it.code == code } ?: STOPPED
        }
    }
}

/**
 * 统计数据数据类
 */
data class Statistics(
    val totalGrabbed: Int = 0,
    val todayGrabbed: Int = 0,
    val successCount: Int = 0,
    val failCount: Int = 0,
    val avgResponseTimeMs: Long = 0,
    val totalTimeMs: Long = 0,
    val lastResetTime: Long = 0L
) {
    /**
     * 计算成功率
     */
    fun getSuccessRate(): Float {
        val total = successCount + failCount
        return if (total > 0) {
            successCount.toFloat() / total * 100f
        } else {
            0f
        }
    }
    
    /**
     * 计算平均响应时间
     */
    fun getAvgResponseTime(): Long {
        return avgResponseTimeMs
    }
}

/**
 * 订单信息数据类
 */
data class OrderInfo(
    val id: String = "",
    val title: String = "",
    val price: String = "",
    val location: String = "",
    val rawText: String = "",
    val timestamp: Long = 0L,
    val isMatched: Boolean = false,
    val priority: Int = 0
)

/**
 * 抢单规则数据类
 */
data class GrabRule(
    val id: String = "",
    val name: String = "",
    val keywords: List<String> = emptyList(),
    val excludeWords: List<String> = emptyList(),
    val minPrice: Double = 0.0,
    val maxPrice: Double = 999999.0,
    val locations: List<String> = emptyList(),
    val enabled: Boolean = true,
    val priority: Int = 5
)

/**
 * 日志条目数据类
 */
data class LogEntry(
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 日志级别枚举
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR
}