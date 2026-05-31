package com.tishou.assistant.data

import android.content.Context
import android.content.SharedPreferences
import com.tishou.assistant.core.GrabRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 配置管理器
 * 
 * 负责管理应用的所有配置项，包括：
 * - 用户偏好设置
 * - 抢单规则
 * - 系统配置
 * 
 * 使用 SharedPreferences 进行持久化存储
 */
class ConfigManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "tishou_config",
        Context.MODE_PRIVATE
    )

    private val _rulesFlow = MutableStateFlow<List<GrabRule>>(emptyList())
    val rulesFlow: StateFlow<List<GrabRule>> = _rulesFlow

    init {
        loadRules()
    }

    // ========== 抢单规则管理 ==========

    /**
     * 获取所有抢单规则
     */
    fun getRules(): List<GrabRule> {
        val json = prefs.getString(KEY_RULES, null) ?: return emptyList()
        return parseRules(json)
    }

    /**
     * 保存抢单规则列表
     */
    fun saveRules(rules: List<GrabRule>) {
        val json = rulesToJson(rules)
        prefs.edit().putString(KEY_RULES, json).apply()
        _rulesFlow.value = rules
    }

    /**
     * 添加单个规则
     */
    fun addRule(rule: GrabRule) {
        val rules = getRules().toMutableList()
        rules.add(rule)
        saveRules(rules)
    }

    /**
     * 更新规则
     */
    fun updateRule(rule: GrabRule) {
        val rules = getRules().toMutableList()
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            rules[index] = rule
            saveRules(rules)
        }
    }

    /**
     * 删除规则
     */
    fun deleteRule(ruleId: String) {
        val rules = getRules().toMutableList()
        rules.removeAll { it.id == ruleId }
        saveRules(rules)
    }

    /**
     * 启用/禁用规则
     */
    fun toggleRule(ruleId: String, enabled: Boolean) {
        val rules = getRules().toMutableList()
        val index = rules.indexOfFirst { it.id == ruleId }
        if (index >= 0) {
            rules[index] = rules[index].copy(enabled = enabled)
            saveRules(rules)
        }
    }

    /**
     * 获取启用的规则
     */
    fun getEnabledRules(): List<GrabRule> {
        return getRules().filter { it.enabled }
    }

    private fun loadRules() {
        _rulesFlow.value = getRules()
    }

    private fun parseRules(json: String): List<GrabRule> {
        val rules = mutableListOf<GrabRule>()
        val array = JSONArray(json)
        
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val rule = GrabRule(
                id = obj.optString("id", ""),
                name = obj.optString("name", ""),
                keywords = parseStringList(obj.optJSONArray("keywords")),
                excludeWords = parseStringList(obj.optJSONArray("excludeWords")),
                minPrice = obj.optDouble("minPrice", 0.0),
                maxPrice = obj.optDouble("maxPrice", 999999.0),
                locations = parseStringList(obj.optJSONArray("locations")),
                enabled = obj.optBoolean("enabled", true),
                priority = obj.optInt("priority", 5)
            )
            rules.add(rule)
        }
        
        return rules
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    private fun rulesToJson(rules: List<GrabRule>): String {
        val array = JSONArray()
        
        rules.forEach { rule ->
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("keywords", JSONArray(rule.keywords))
                put("excludeWords", JSONArray(rule.excludeWords))
                put("minPrice", rule.minPrice)
                put("maxPrice", rule.maxPrice)
                put("locations", JSONArray(rule.locations))
                put("enabled", rule.enabled)
                put("priority", rule.priority)
            }
            array.put(obj)
        }
        
        return array.toString()
    }

    // ========== 通用配置项 ==========

    /**
     * 保存字符串配置
     */
    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    /**
     * 获取字符串配置
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * 保存整数配置
     */
    fun setInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    /**
     * 获取整数配置
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    /**
     * 保存布尔配置
     */
    fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    /**
     * 获取布尔配置
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    /**
     * 保存长整型配置
     */
    fun setLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    /**
     * 获取长整型配置
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    /**
     * 清除所有配置
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_RULES = "grab_rules"
        
        // 配置键常量
        const val KEY_OCR_API_KEY = "ocr_api_key"
        const val KEY_OCR_API_SECRET = "ocr_api_secret"
        const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_AUTO_START = "auto_start"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_LOG_LEVEL = "log_level"
        const val KEY_MAX_LOG_DAYS = "max_log_days"

        @Volatile
        private var INSTANCE: ConfigManager? = null

        fun getInstance(context: Context): ConfigManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ConfigManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}