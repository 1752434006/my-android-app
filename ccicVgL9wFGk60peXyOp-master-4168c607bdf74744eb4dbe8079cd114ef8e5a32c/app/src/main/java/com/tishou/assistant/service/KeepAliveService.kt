package com.tishou.assistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tishou.assistant.R
import com.tishou.assistant.core.TiShouCore
import com.tishou.assistant.core.LogLevel
import kotlin.concurrent.thread

/**
 * 前台保活服务
 * 
 * 针对澎湃 OS 优化的后台保活策略：
 * 1. 使用前台服务提升优先级
 * 2. 创建持久通知栏显示运行状态
 * 3. 适配 Android 14/15 前台服务类型要求
 */
class KeepAliveService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "tishou_keepalive_channel"
        
        var isRunning: Boolean = false
            private set
        
        /**
         * 启动保活服务
         */
        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * 停止保活服务
         */
        fun stop(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var core: TiShouCore
    private var monitorThread: Thread? = null
    private var isMonitoring = false

    override fun onCreate() {
        super.onCreate()
        core = TiShouCore.getInstance()
        createNotificationChannel()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动前台服务
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // 启动监控线程
        startMonitoring()
        
        // 返回 START_STICKY 确保服务被杀后重启
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        monitorThread?.interrupt()
        isRunning = false
        core.log("保活服务已停止", LogLevel.INFO)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 任务被移除时尝试重启（澎湃 OS 适配）
        try {
            val restartIntent = Intent(this, KeepAliveService::class.java)
            sendBroadcast(restartIntent)
        } catch (e: Exception) {
            core.log("服务重启失败：${e.message}", LogLevel.ERROR)
        }
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "替手助手 - 运行状态",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示替手助手的运行状态和抢单统计"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.tishou.assistant.ui.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("替手助手运行中")
            .setContentText("正在监控订单，随时准备为您抢单")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .addAction(createStopAction())

        // Android 14+ 需要设置前台服务类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    /**
     * 创建停止操作按钮
     */
    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(this, KeepAliveService::class.java).apply {
            action = "ACTION_STOP_SERVICE"
        }
        
        val pendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_stop,
            "停止服务",
            pendingIntent
        ).build()
    }

    /**
     * 启动监控线程
     */
    private fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitorThread = thread(name = "KeepAliveMonitor") {
            while (isMonitoring && !Thread.currentThread().isInterrupted) {
                try {
                    // 定期更新通知内容
                    updateNotification()
                    Thread.sleep(5000) // 每 5 秒更新一次
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    core.log("监控线程异常：${e.message}", LogLevel.ERROR)
                }
            }
        }
    }

    /**
     * 更新通知内容
     */
    private fun updateNotification() {
        try {
            val stats = core.statistics
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("替手助手运行中")
                .setContentText("今日抢单：${stats.successCount} 成功 / ${stats.failCount} 失败")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true)
                .build()

            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // 忽略更新失败
        }
    }

    /**
     * 检查电池优化白名单
     */
    fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    /**
     * 请求电池优化白名单
     */
    fun requestBatteryOptimizationIgnore(activity: android.app.Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isBatteryOptimizationIgnored()) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                ).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                activity.startActivityForResult(intent, 1001)
            }
        }
    }
}