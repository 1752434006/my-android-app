#ifndef TISHOU_CORE_H
#define TISHOU_CORE_H

#include "types.h"
#include <string>
#include <vector>
#include <queue>
#include <mutex>
#include <atomic>
#include <functional>
#include <regex>

namespace tishou {

/**
 * @brief 核心调度器类
 * 
 * 负责订单监控、匹配决策、延时控制等核心功能
 * 使用微秒级高精度定时器实现快速响应
 */
class TiShouScheduler {
public:
    // 单例模式获取实例
    static TiShouScheduler& getInstance();
    
    // 初始化调度器
    bool initialize();
    
    // 销毁调度器
    void destroy();
    
    // 启动监控
    void startMonitoring();
    
    // 停止监控
    void stopMonitoring();
    
    // 暂停/恢复
    void pause();
    void resume();
    
    // 设置抢单规则
    void setRules(const std::vector<GrabRule>& rules);
    
    // 添加规则
    void addRule(const GrabRule& rule);
    
    // 移除规则
    void removeRule(const std::string& ruleId);
    
    // 处理 OCR 识别结果
    void processOcrResult(const OcrResult& ocrResult);
    
    // 执行抢单动作
    void executeGrab(const OrderInfo& order);
    
    // 获取当前状态
    ServiceStatus getStatus() const;
    
    // 获取统计信息
    Statistics getStatistics() const;
    
    // 重置统计
    void resetStatistics();
    
    // 设置回调函数 (通过 JNI 调用 Kotlin)
    using OrderMatchedCallback = std::function<void(const OrderInfo&)>;
    using LogCallback = std::function<void(const LogEntry&)>;
    using StatusCallback = std::function<void(ServiceStatus)>;
    
    void setOrderMatchedCallback(OrderMatchedCallback callback);
    void setLogCallback(LogCallback callback);
    void setStatusCallback(StatusCallback callback);

private:
    TiShouScheduler();
    ~TiShouScheduler();
    
    // 禁止拷贝和赋值
    TiShouScheduler(const TiShouScheduler&) = delete;
    TiShouScheduler& operator=(const TiShouScheduler&) = delete;
    
    // 订单匹配逻辑
    bool matchOrder(const OrderInfo& order, const GrabRule& rule);
    
    // 关键词匹配
    bool matchKeywords(const std::string& text, const std::vector<std::string>& keywords);
    
    // 排除词匹配
    bool matchExcludeWords(const std::string& text, const std::vector<std::string>& excludeWords);
    
    // 价格匹配
    bool matchPrice(const std::string& priceStr, double minPrice, double maxPrice);
    
    // 地点匹配
    bool matchLocation(const std::string& location, const std::vector<std::string>& locations);
    
    // 计算优先级
    int calculatePriority(const OrderInfo& order, const GrabRule& rule);
    
    // 日志记录
    void log(LogLevel level, const std::string& tag, const std::string& message);
    
    // 更新统计
    void updateStatistics(bool success, long responseTimeMs);
    
    // 后台线程函数
    void monitoringThreadFunc();

private:
    std::atomic<ServiceStatus> m_status{ServiceStatus::STOPPED};
    std::atomic<bool> m_running{false};
    std::atomic<bool> m_paused{false};
    
    std::vector<GrabRule> m_rules;
    std::queue<OrderInfo> m_orderQueue;
    
    Statistics m_statistics;
    
    std::mutex m_rulesMutex;
    std::mutex m_queueMutex;
    std::mutex m_statsMutex;
    
    OrderMatchedCallback m_orderMatchedCallback;
    LogCallback m_logCallback;
    StatusCallback m_statusCallback;
    
    std::thread m_monitoringThread;
    
    // 微秒级延时精度
    static constexpr int MICROSECOND_PRECISION = 100;  // 100 微秒
};

// JNI 桥接函数声明 (extern "C")
extern "C" {

// 初始化核心引擎
JNIEXPORT jboolean JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeInit(JNIEnv* env, jobject thiz);

// 销毁核心引擎
JNIEXPORT void JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeDestroy(JNIEnv* env, jobject thiz);

// 启动监控
JNIEXPORT void JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeStartMonitoring(JNIEnv* env, jobject thiz);

// 停止监控
JNIEXPORT void JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeStopMonitoring(JNIEnv* env, jobject thiz);

// 处理 OCR 结果
JNIEXPORT void JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeProcessOcrResult(
    JNIEnv* env, jobject thiz, jstring ocrText);

// 获取服务状态
JNIEXPORT jint JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeGetStatus(JNIEnv* env, jobject thiz);

// 获取统计数据
JNIEXPORT jobject JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeGetStatistics(JNIEnv* env, jobject thiz);

} // extern "C"

} // namespace tishou

#endif // TISHOU_CORE_H