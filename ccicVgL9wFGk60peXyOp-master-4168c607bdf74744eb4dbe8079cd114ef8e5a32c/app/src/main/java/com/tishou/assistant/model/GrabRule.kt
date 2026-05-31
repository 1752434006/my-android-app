package com.tishou.assistant.model

/**
 * 抢单规则数据类
 * 
 * 用于配置抢单关键词、价格区间、区域筛选等规则
 */
data class GrabRule(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val excludeKeywords: List<String> = emptyList(),
    val minPrice: Double = 0.0,
    val maxPrice: Double = Double.MAX_VALUE,
    val locations: List<String> = emptyList(),
    val goodsTypes: List<String> = emptyList(),
    val enabled: Boolean = true,
    val priority: Int = 0, // 优先级，数字越大优先级越高
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 检查订单是否匹配此规则
     */
    fun matchesOrder(order: OrderInfo): Boolean {
        if (!enabled) return false
        
        // 检查价格区间
        if (order.amount < minPrice || order.amount > maxPrice) {
            return false
        }
        
        // 检查排除词
        for (exclude in excludeKeywords) {
            if (order.rawText.contains(exclude, ignoreCase = true)) {
                return false
            }
        }
        
        // 检查关键词（至少匹配一个）
        var keywordMatched = false
        for (keyword in keywords) {
            if (order.rawText.contains(keyword, ignoreCase = true)) {
                keywordMatched = true
                break
            }
        }
        
        if (keywords.isNotEmpty() && !keywordMatched) {
            return false
        }
        
        // 检查地点（如果设置了）
        if (locations.isNotEmpty()) {
            var locationMatched = false
            for (location in locations) {
                if (order.location.contains(location, ignoreCase = true)) {
                    locationMatched = true
                    break
                }
            }
            if (!locationMatched) {
                return false
            }
        }
        
        // 检查货物类型（如果设置了）
        if (goodsTypes.isNotEmpty()) {
            var goodsTypeMatched = false
            for (goodsType in goodsTypes) {
                if (order.goodsType.contains(goodsType, ignoreCase = true)) {
                    goodsTypeMatched = true
                    break
                }
            }
            if (!goodsTypeMatched) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * 获取规则的摘要信息
     */
    fun toSummary(): String {
        return buildString {
            append("[$name] ")
            append("关键词：${keywords.joinToString(", ")}")
            if (excludeKeywords.isNotEmpty()) {
                append(" | 排除：${excludeKeywords.joinToString(", ")}")
            }
            append(" | 价格：${minPrice}")
            if (maxPrice != Double.MAX_VALUE) {
                append("-$maxPrice")
            }
            if (locations.isNotEmpty()) {
                append(" | 地点：${locations.joinToString(", ")}")
            }
        }
    }
}