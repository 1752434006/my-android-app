package com.tishou.assistant.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 抢单日志数据类
 * 
 * 用于记录每次抢单的详细信息
 */
@Entity(tableName = "grab_logs")
data class GrabLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val timestamp: Long = System.currentTimeMillis(),
    
    val isSuccess: Boolean,
    
    val orderNo: String = "",
    
    val amount: Double = 0.0,
    
    val responseTime: Long = 0L, // 响应时间（毫秒）
    
    val note: String = "", // 备注信息
    
    val screenshotPath: String? = null, // 截图路径（可选）
    
    val ruleId: String? = null, // 匹配的规则 ID
    
    val ruleName: String? = null, // 匹配的规则名称
    
    val platform: String = "", // 平台名称（如：货拉拉、滴滴等）
    
    val location: String = "", // 订单地点
    
    val goodsType: String = "" // 货物类型
) {
    /**
     * 获取日志摘要
     */
    fun toSummary(): String {
        return buildString {
            append(if (isSuccess) "✅" else "❌")
            append(" $orderNo")
            append(" ¥$amount")
            append(" ${responseTime}ms")
        }
    }
    
    /**
     * 格式化显示时间
     */
    fun getFormattedTime(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
        return sdf.format(java.util.Date(timestamp))
    }
}