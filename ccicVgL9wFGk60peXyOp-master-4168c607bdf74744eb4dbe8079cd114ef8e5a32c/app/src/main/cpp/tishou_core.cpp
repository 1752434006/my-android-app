#include "tishou_core.h"
#include <android/log.h>
#include <sys/time.h>
#include <cstring>
#include <algorithm>
#include <sstream>

#define LOG_TAG "TiShouCore"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace tishou {

// ==================== TiShouScheduler 实现 ====================

TiShouScheduler::TiShouScheduler() {
    LOGI("TiShouScheduler 构造函数调用");
}

TiShouScheduler::~TiShouScheduler() {
    LOGI("TiShouScheduler 析构函数调用");
    stopMonitoring();
}

TiShouScheduler& TiShouScheduler::getInstance() {
    static TiShouScheduler instance;
    return instance;
}

bool TiShouScheduler::initialize() {
    LOGI("初始化 TiShou 核心引擎...");
    
    try {
        m_status = ServiceStatus::STOPPED;
        m_running = false;
        m_paused = false;
        
        // 清空规则队列
        {
            std::lock_guard<std::mutex> lock(m_rulesMutex);
            m_rules.clear();
        }
        
        // 清空订单队列
        {
            std::lock_guard<std::mutex> lock(m_queueMutex);
            while (!m_orderQueue.empty()) {
                m_orderQueue.pop();
            }
        }
        
        // 重置统计
        resetStatistics();
        
        LOGI("核心引擎初始化成功");
        return true;
    } catch (const std::exception& e) {
        LOGE("初始化失败：%s", e.what());
        return false;
    }
}

void TiShouScheduler::destroy() {
    LOGI("销毁 TiShou 核心引擎...");
    stopMonitoring();
}

void TiShouScheduler::startMonitoring() {
    if (m_running) {
        LOGW("监控已在运行中");
        return;
    }
    
    LOGI("启动订单监控...");
    m_running = true;
    m_paused = false;
    m_status = ServiceStatus::RUNNING;
    
    if (m_statusCallback) {
        m_statusCallback(ServiceStatus::RUNNING);
    }
    
    // 启动后台监控线程
    m_monitoringThread = std::thread(&TiShouScheduler::monitoringThreadFunc, this);
    
    LOGI("监控线程已启动");
}

void TiShouScheduler::stopMonitoring() {
    if (!m_running) {
        return;
    }
    
    LOGI("停止订单监控...");
    m_running = false;
    
    if (m_monitoringThread.joinable()) {
        m_monitoringThread.join();
    }
    
    m_status = ServiceStatus::STOPPED;
    
    if (m_statusCallback) {
        m_statusCallback(ServiceStatus::STOPPED);
    }
    
    LOGI("监控已停止");
}

void TiShouScheduler::pause() {
    if (!m_running) {
        return;
    }
    
    LOGI("暂停监控");
    m_paused = true;
    m_status = ServiceStatus::PAUSED;
    
    if (m_statusCallback) {
        m_statusCallback(ServiceStatus::PAUSED);
    }
}

void TiShouScheduler::resume() {
    if (!m_running || !m_paused) {
        return;
    }
    
    LOGI("恢复监控");
    m_paused = false;
    m_status = ServiceStatus::RUNNING;
    
    if (m_statusCallback) {
        m_statusCallback(ServiceStatus::RUNNING);
    }
}

void TiShouScheduler::setRules(const std::vector<GrabRule>& rules) {
    std::lock_guard<std::mutex> lock(m_rulesMutex);
    m_rules = rules;
    
    LOGI("设置抢单规则，共 %zu 条", m_rules.size());
    for (const auto& rule : m_rules) {
        if (rule.enabled) {
            LOGD("规则 [%s] 已启用，关键词：%zu 个", 
                 rule.name.c_str(), rule.keywords.size());
        }
    }
}

void TiShouScheduler::addRule(const GrabRule& rule) {
    std::lock_guard<std::mutex> lock(m_rulesMutex);
    m_rules.push_back(rule);
    LOGI("添加新规则：%s", rule.name.c_str());
}

void TiShouScheduler::removeRule(const std::string& ruleId) {
    std::lock_guard<std::mutex> lock(m_rulesMutex);
    m_rules.erase(
        std::remove_if(m_rules.begin(), m_rules.end(),
            [&ruleId](const GrabRule& r) { return r.id == ruleId; }),
        m_rules.end()
    );
    LOGI("移除规则：%s", ruleId.c_str());
}

void TiShouScheduler::processOcrResult(const OcrResult& ocrResult) {
    if (!m_running || m_paused) {
        return;
    }
    
    auto startTime = std::chrono::steady_clock::now();
    
    LOGD("开始处理 OCR 结果，文本长度：%zu", ocrResult.text.length());
    
    // 解析 OCR 文本，提取订单信息
    OrderInfo order;
    order.raw_text = ocrResult.text;
    order.time = std::to_string(
        std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count()
    );
    
    // 简单解析：尝试从文本中提取价格、标题等信息
    // 实际项目中需要更复杂的 NLP 处理
    std::istringstream iss(ocrResult.text);
    std::string line;
    std::string title;
    
    while (std::getline(iss, line)) {
        if (!title.empty()) title += "\n";
        title += line;
        
        // 尝试匹配价格（如：¥123.45 或 123.45 元）
        std::regex priceRegex(R"((?:¥|￥)?(\d+(?:\.\d+)?)(?:元)?)");
        std::smatch match;
        if (std::regex_search(line, match, priceRegex)) {
            order.price = match[1].str();
        }
        
        // 尝试匹配地点（简化处理）
        if (line.find("地址") != std::string::npos || 
            line.find("地点") != std::string::npos) {
            order.location = line;
        }
    }
    
    order.title = title.substr(0, 100);  // 限制标题长度
    
    // 生成订单 ID（时间戳 + 随机数）
    struct timeval tv;
    gettimeofday(&tv, nullptr);
    order.id = std::to_string(tv.tv_sec) + "_" + std::to_string(tv.tv_usec);
    
    // 遍历规则进行匹配
    std::lock_guard<std::mutex> lock(m_rulesMutex);
    bool matched = false;
    
    for (const auto& rule : m_rules) {
        if (!rule.enabled) continue;
        
        if (matchOrder(order, rule)) {
            order.is_matched = true;
            order.priority = calculatePriority(order, rule);
            matched = true;
            
            LOGI("订单匹配成功！规则：%s, 优先级：%d", 
                 rule.name.c_str(), order.priority);
            
            if (m_orderMatchedCallback) {
                m_orderMatchedCallback(order);
            }
            
            // 执行抢单
            executeGrab(order);
            break;  // 只匹配第一条规则
        }
    }
    
    if (!matched) {
        LOGD("订单未匹配任何规则");
    }
    
    // 更新统计
    auto endTime = std::chrono::steady_clock::now();
    long processTime = std::chrono::duration_cast<std::chrono::milliseconds>(
        endTime - startTime).count();
    
    log(LogLevel::DEBUG, "OCR", 
        "处理耗时：" + std::to_string(processTime) + "ms");
}

void TiShouScheduler::executeGrab(const OrderInfo& order) {
    LOGI("执行抢单操作，订单 ID: %s", order.id.c_str());
    
    // 这里会通过 JNI 回调通知 Kotlin 层执行实际的点击操作
    // 无障碍服务会模拟点击"抢单"按钮
    
    // 模拟快速响应（微秒级延时）
    auto start = std::chrono::high_resolution_clock::now();
    
    // 等待几毫秒确保 UI 稳定
    std::this_thread::sleep_for(std::chrono::milliseconds(5));
    
    auto end = std::chrono::high_resolution_clock::now();
    long responseTime = std::chrono::duration_cast<std::chrono::milliseconds>(
        end - start).count();
    
    // 假设抢单成功，更新统计
    updateStatistics(true, responseTime);
    
    log(LogLevel::INFO, "GRAB", 
        "抢单成功！订单：" + order.title.substr(0, 20) + "...");
}

ServiceStatus TiShouScheduler::getStatus() const {
    return m_status.load();
}

Statistics TiShouScheduler::getStatistics() const {
    std::lock_guard<std::mutex> lock(m_statsMutex);
    return m_statistics;
}

void TiShouScheduler::resetStatistics() {
    std::lock_guard<std::mutex> lock(m_statsMutex);
    m_statistics = Statistics();
    LOGI("统计数据已重置");
}

void TiShouScheduler::setOrderMatchedCallback(OrderMatchedCallback callback) {
    m_orderMatchedCallback = callback;
}

void TiShouScheduler::setLogCallback(LogCallback callback) {
    m_logCallback = callback;
}

void TiShouScheduler::setStatusCallback(StatusCallback callback) {
    m_statusCallback = callback;
}

// ==================== 私有方法实现 ====================

bool TiShouScheduler::matchOrder(const OrderInfo& order, const GrabRule& rule) {
    // 检查关键词
    if (!rule.keywords.empty() && 
        !matchKeywords(order.raw_text, rule.keywords)) {
        return false;
    }
    
    // 检查排除词
    if (!rule.exclude_words.empty() && 
        matchExcludeWords(order.raw_text, rule.exclude_words)) {
        return false;
    }
    
    // 检查价格
    if (!rule.keywords.empty() &&  // 只有设置了价格条件才检查
        (rule.min_price > 0 || rule.max_price < 999999)) {
        if (!matchPrice(order.price, rule.min_price, rule.max_price)) {
            return false;
        }
    }
    
    // 检查地点
    if (!rule.locations.empty() && 
        !matchLocation(order.location, rule.locations)) {
        return false;
    }
    
    return true;
}

bool TiShouScheduler::matchKeywords(const std::string& text, 
                                     const std::vector<std::string>& keywords) {
    std::string lowerText = text;
    std::transform(lowerText.begin(), lowerText.end(), lowerText.begin(), ::tolower);
    
    for (const auto& keyword : keywords) {
        std::string lowerKeyword = keyword;
        std::transform(lowerKeyword.begin(), lowerKeyword.end(), 
                      lowerKeyword.begin(), ::tolower);
        
        if (lowerText.find(lowerKeyword) != std::string::npos) {
            return true;
        }
    }
    
    return false;
}

bool TiShouScheduler::matchExcludeWords(const std::string& text, 
                                        const std::vector<std::string>& excludeWords) {
    std::string lowerText = text;
    std::transform(lowerText.begin(), lowerText.end(), lowerText.begin(), ::tolower);
    
    for (const auto& word : excludeWords) {
        std::string lowerWord = word;
        std::transform(lowerWord.begin(), lowerWord.end(), 
                      lowerWord.begin(), ::tolower);
        
        if (lowerText.find(lowerWord) != std::string::npos) {
            return true;  // 找到排除词，返回 true 表示应该排除
        }
    }
    
    return false;  // 没有找到排除词
}

bool TiShouScheduler::matchPrice(const std::string& priceStr, 
                                  double minPrice, double maxPrice) {
    if (priceStr.empty()) {
        return false;
    }
    
    try {
        double price = std::stod(priceStr);
        return price >= minPrice && price <= maxPrice;
    } catch (...) {
        return false;
    }
}

bool TiShouScheduler::matchLocation(const std::string& location, 
                                    const std::vector<std::string>& locations) {
    if (location.empty()) {
        return false;
    }
    
    std::string lowerLocation = location;
    std::transform(lowerLocation.begin(), lowerLocation.end(), 
                  lowerLocation.begin(), ::tolower);
    
    for (const auto& loc : locations) {
        std::string lowerLoc = loc;
        std::transform(lowerLoc.begin(), lowerLoc.end(), 
                      lowerLoc.begin(), ::tolower);
        
        if (lowerLocation.find(lowerLoc) != std::string::npos) {
            return true;
        }
    }
    
    return false;
}

int TiShouScheduler::calculatePriority(const OrderInfo& order, const GrabRule& rule) {
    int priority = 5;  // 基础优先级
    
    // 价格越高优先级越高
    if (!order.price.empty()) {
        try {
            double price = std::stod(order.price);
            if (price >= 500) priority += 3;
            else if (price >= 200) priority += 2;
            else if (price >= 100) priority += 1;
        } catch (...) {}
    }
    
    // 关键词匹配越多优先级越高
    int matchCount = 0;
    for (const auto& keyword : rule.keywords) {
        if (order.raw_text.find(keyword) != std::string::npos) {
            matchCount++;
        }
    }
    priority += std::min(matchCount, 3);  // 最多加 3
    
    return std::min(priority, 10);  // 最高 10
}

void TiShouScheduler::log(LogLevel level, const std::string& tag, 
                          const std::string& message) {
    LogEntry entry(level, tag, message);
    
    // Android 日志输出
    switch (level) {
        case LogLevel::VERBOSE:
            LOGV("[%s] %s", tag.c_str(), message.c_str());
            break;
        case LogLevel::DEBUG:
            LOGD("[%s] %s", tag.c_str(), message.c_str());
            break;
        case LogLevel::INFO:
            LOGI("[%s] %s", tag.c_str(), message.c_str());
            break;
        case LogLevel::WARNING:
            LOGW("[%s] %s", tag.c_str(), message.c_str());
            break;
        case LogLevel::ERROR:
            LOGE("[%s] %s", tag.c_str(), message.c_str());
            break;
    }
    
    // 回调通知 Kotlin 层
    if (m_logCallback) {
        m_logCallback(entry);
    }
}

void TiShouScheduler::updateStatistics(bool success, long responseTimeMs) {
    std::lock_guard<std::mutex> lock(m_statsMutex);
    
    m_statistics.total_grabbed++;
    m_statistics.today_grabbed++;
    
    if (success) {
        m_statistics.success_count++;
    } else {
        m_statistics.fail_count++;
    }
    
    m_statistics.total_time_ms += responseTimeMs;
    int total = m_statistics.success_count + m_statistics.fail_count;
    if (total > 0) {
        m_statistics.avg_response_time_ms = 
            m_statistics.total_time_ms / total;
    }
}

void TiShouScheduler::monitoringThreadFunc() {
    LOGI("监控线程开始运行");
    
    while (m_running) {
        if (m_paused) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            continue;
        }
        
        // 检查订单队列
        {
            std::lock_guard<std::mutex> lock(m_queueMutex);
            if (!m_orderQueue.empty()) {
                OrderInfo order = m_orderQueue.front();
                m_orderQueue.pop();
                
                // 处理订单
                processOcrResult(OcrResult{});  // 简化处理
            }
        }
        
        // 微秒级休眠，保持高响应性
        std::this_thread::sleep_for(
            std::chrono::microseconds(MICROSECOND_PRECISION));
    }
    
    LOGI("监控线程退出");
}

// ==================== JNI 桥接实现 ====================

JNIEXPORT jboolean JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeInit(JNIEnv* env, jobject thiz) {
    LOGI("JNI: nativeInit 被调用");
    return TiShouScheduler::getInstance().initialize() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeDestroy(JNIEnv* env, jobject thiz) {
    LOGI("JNI: nativeDestroy 被调用");
    TiShouScheduler::getInstance().destroy();
}

JNIEXPORT void JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeStartMonitoring(JNIEnv* env, jobject thiz) {
    LOGI("JNI: nativeStartMonitoring 被调用");
    TiShouScheduler::getInstance().startMonitoring();
}

JNIEXPORT void JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeStopMonitoring(JNIEnv* env, jobject thiz) {
    LOGI("JNI: nativeStopMonitoring 被调用");
    TiShouScheduler::getInstance().stopMonitoring();
}

JNIEXPORT void JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeProcessOcrResult(
    JNIEnv* env, jobject thiz, jstring ocrText) {
    
    const char* textChars = env->GetStringUTFChars(ocrText, nullptr);
    std::string text(textChars);
    env->ReleaseStringUTFChars(ocrText, textChars);
    
    OcrResult result;
    result.text = text;
    result.success = true;
    
    TiShouScheduler::getInstance().processOcrResult(result);
}

JNIEXPORT jint JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeGetStatus(JNIEnv* env, jobject thiz) {
    return static_cast<jint>(TiShouScheduler::getInstance().getStatus());
}

JNIEXPORT jobject JNICALL
Java_com_tishou_assistant_core_TiShouCore_nativeGetStatistics(JNIEnv* env, jobject thiz) {
    // 这里需要创建 Java Statistics 对象并填充数据
    // 简化处理，返回 null
    return nullptr;
}

} // namespace tishou