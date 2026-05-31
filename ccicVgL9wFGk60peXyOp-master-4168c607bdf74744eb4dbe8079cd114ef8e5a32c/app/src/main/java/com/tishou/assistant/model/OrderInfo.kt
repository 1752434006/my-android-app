package com.tishou.assistant.model

/**
 * 订单信息数据模型
 * 
 * 用于存储从 OCR 识别结果中解析出的订单相关信息
 */
data class OrderInfo(
    /** 订单号 */
    var orderNo: String? = null,
    
    /** 订单金额 */
    var amount: Double = 0.0,
    
    /** 下单时间 */
    var time: String? = null,
    
    /** 发货地/目的地 */
    var location: String? = null,
    
    /** 货物类型 */
    var goodsType: String? = null,
    
    /** 重量 */
    var weight: Double = 0.0,
    
    /** 原始 OCR 文本 */
    var rawText: String = "",
    
    /** 按行分割的文本 */
    var lines: List<String> = emptyList(),
    
    /** 是否已抢单 */
    var grabbed: Boolean = false,
    
    /** 抢单时间 */
    var grabTime: Long = 0L,
    
    /** 抢单结果（成功/失败） */
    var grabResult: GrabResult = GrabResult.PENDING,
    
    /** 备注信息 */
    var note: String? = null
) {
    /**
     * 抢单结果枚举
     */
    enum class GrabResult {
        PENDING,    // 待处理
        SUCCESS,    // 抢单成功
        FAILED,     // 抢单失败
        TIMEOUT     // 超时
    }

    /**
     * 检查订单信息是否完整
     */
    fun isValid(): Boolean {
        return rawText.isNotBlank() && lines.isNotEmpty()
    }

    /**
     * 获取订单摘要信息（用于日志显示）
     */
    fun toSummary(): String {
        val builder = StringBuilder()
        
        orderNo?.let { builder.append("单号：$it; ") }
        if (amount > 0) builder.append("金额：¥$amount; ")
        time?.let { builder.append("时间：$it; ") }
        location?.let { builder.append("地点：$it; ") }
        goodsType?.let { builder.append("货物：$it; ") }
        if (weight > 0) builder.append("重量：${weight}吨; ")
        
        return if (builder.isEmpty()) "无关键信息" else builder.toString()
    }

    /**
     * 转换为 JSON 格式（用于存储或传输）
     */
    fun toJson(): String {
        return buildString {
            append("{")
            append("\"orderNo\":\"${orderNo ?: ""}\",")
            append("\"amount\":$amount,")
            append("\"time\":\"${time ?: ""}\",")
            append("\"location\":\"${location ?: ""}\",")
            append("\"goodsType\":\"${goodsType ?: ""}\",")
            append("\"weight\":$weight,")
            append("\"grabbed\":$grabbed,")
            append("\"grabTime\":$grabTime,")
            append("\"grabResult\":\"$grabResult\",")
            append("\"note\":\"${note ?: ""}\"")
            append("}")
        }
    }

    /**
     * 从 JSON 字符串解析（简化版，实际可用 Gson/Moshi）
     */
    companion object {
        fun fromJson(json: String): OrderInfo {
            val info = OrderInfo()
            // 简化解析，实际项目中建议使用 Gson 或 Moshi
            return info
        }
    }

    override fun toString(): String {
        return "OrderInfo(orderNo=$orderNo, amount=$amount, time=$time, location=$location, goodsType=$goodsType, weight=$weight)"
    }
}

/**
 * 抢单规则配置
 */
data class GrabRule(
    /** 规则 ID */
    val id: String = java.util.UUID.randomUUID().toString(),
    
    /** 规则名称 */
    var name: String = "",
    
    /** 关键词列表（包含任一即匹配） */
    var keywords: MutableList<String> = mutableListOf(),
    
    /** 排除关键词列表（包含任一即不匹配） */
    var excludeKeywords: MutableList<String> = mutableListOf(),
    
    /** 最低金额 */
    var minAmount: Double = 0.0,
    
    /** 最高金额 */
    var maxAmount: Double = Double.MAX_VALUE,
    
    /** 指定发货地（空表示不限） */
    var locations: MutableList<String> = mutableListOf(),
    
    /** 指定货物类型（空表示不限） */
    var goodsTypes: MutableList<String> = mutableListOf(),
    
    /** 是否启用 */
    var enabled: Boolean = true,
    
    /** 优先级（数字越大优先级越高） */
    var priority: Int = 0,
    
    /** 创建时间 */
    val createTime: Long = System.currentTimeMillis(),
    
    /** 最后修改时间 */
    var updateTime: Long = System.currentTimeMillis()
) {
    /**
     * 检查订单是否匹配此规则
     */
    fun matches(order: OrderInfo): Boolean {
        // 检查金额范围
        if (order.amount > 0) {
            if (order.amount < minAmount || order.amount > maxAmount) {
                return false
            }
        }

        // 检查地点
        if (locations.isNotEmpty() && order.location != null) {
            val locationMatch = locations.any { 
                order.location!!.contains(it, ignoreCase = true) 
            }
            if (!locationMatch) return false
        }

        // 检查货物类型
        if (goodsTypes.isNotEmpty() && order.goodsType != null) {
            val goodsMatch = goodsTypes.any { 
                order.goodsType!!.contains(it, ignoreCase = true) 
            }
            if (!goodsMatch) return false
        }

        // 检查关键词
        val text = order.rawText.lowercase()
        
        // 排除词检查
        if (excludeKeywords.isNotEmpty()) {
            val excluded = excludeKeywords.any { 
                text.contains(it.lowercase()) 
            }
            if (excluded) return false
        }

        // 包含词检查（至少匹配一个）
        if (keywords.isNotEmpty()) {
            val matched = keywords.any { 
                text.contains(it.lowercase()) 
            }
            if (!matched) return false
        }

        return true
    }

    /**
     * 复制规则对象
     */
    fun copy(): GrabRule {
        return GrabRule(
            id = id,
            name = name,
            keywords = keywords.toMutableList(),
            excludeKeywords = excludeKeywords.toMutableList(),
            minAmount = minAmount,
            maxAmount = maxAmount,
            locations = locations.toMutableList(),
            goodsTypes = goodsTypes.toMutableList(),
            enabled = enabled,
            priority = priority,
            createTime = createTime,
            updateTime = updateTime
        )
    }
}

/**
 * 抢单统计信息
 */
data class GrabStatistics(
    /** 日期（格式：yyyy-MM-dd） */
    val date: String = "",
    
    /** 总尝试次数 */
    var totalAttempts: Int = 0,
    
    /** 成功次数 */
    var successCount: Int = 0,
    
    /** 失败次数 */
    var failCount: Int = 0,
    
    /** 超时次数 */
    var timeoutCount: Int = 0,
    
    /** 成功率 */
    val successRate: Float
        get() = if (totalAttempts > 0) successCount.toFloat() / totalAttempts * 100f else 0f,
    
    /** 平均响应时间（毫秒） */
    var avgResponseTime: Long = 0L,
    
    /** 抢到的订单总金额 */
    var totalAmount: Double = 0.0
) {
    /**
     * 更新统计数据
     */
    fun recordResult(result: OrderInfo.GrabResult, responseTime: Long = 0L, amount: Double = 0.0) {
        totalAttempts++
        when (result) {
            OrderInfo.GrabResult.SUCCESS -> {
                successCount++
                totalAmount += amount
            }
            OrderInfo.GrabResult.FAILED -> failCount++
            OrderInfo.GrabResult.TIMEOUT -> timeoutCount++
            else -> {}
        }
        
        // 更新平均响应时间
        if (responseTime > 0) {
            avgResponseTime = ((avgResponseTime * (totalAttempts - 1)) + responseTime) / totalAttempts
        }
    }
}