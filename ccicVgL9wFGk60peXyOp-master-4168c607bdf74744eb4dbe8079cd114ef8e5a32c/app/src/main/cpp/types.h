#ifndef TISHOU_TYPES_H
#define TISHOU_TYPES_H

#include <string>
#include <vector>
#include <chrono>
#include <memory>

namespace tishou {

// 时间类型别名
using TimePoint = std::chrono::steady_clock::time_point;
using Duration = std::chrono::milliseconds;
using MicroDuration = std::chrono::microseconds;

// 订单数据结构
struct OrderInfo {
    std::string id;              // 订单 ID
    std::string title;           // 订单标题
    std::string price;           // 订单价格
    std::string location;        // 订单地点
    std::string time;            // 发布时间
    std::string raw_text;        // 原始识别文本
    int priority;                // 优先级 (1-10)
    bool is_matched;             // 是否匹配规则
    
    OrderInfo() : priority(0), is_matched(false) {}
};

// 抢单规则结构
struct GrabRule {
    std::string id;              // 规则 ID
    std::string name;            // 规则名称
    std::vector<std::string> keywords;      // 关键词列表
    std::vector<std::string> exclude_words; // 排除词列表
    double min_price;            // 最低价格
    double max_price;            // 最高价格
    std::vector<std::string> locations;     // 指定地点
    bool enabled;                // 是否启用
    
    GrabRule() : min_price(0), max_price(999999), enabled(true) {}
};

// OCR 识别结果
struct OcrResult {
    bool success;                // 识别是否成功
    std::string text;            // 识别文本
    float confidence;            // 置信度 (0-1)
    int width;                   // 图片宽度
    int height;                  // 图片高度
    long process_time_ms;        // 处理耗时 (毫秒)
    
    OcrResult() : success(false), confidence(0), width(0), height(0), process_time_ms(0) {}
};

// 服务状态枚举
enum class ServiceStatus {
    STOPPED,                     // 已停止
    RUNNING,                     // 运行中
    PAUSED,                      // 已暂停
    ERROR                        // 错误状态
};

// 日志级别
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR
};

// 日志条目
struct LogEntry {
    LogLevel level;
    std::string tag;
    std::string message;
    long timestamp;
    
    LogEntry(LogLevel l, const std::string& t, const std::string& m)
        : level(l), tag(t), message(m) {
        timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count();
    }
};

// 统计信息
struct Statistics {
    int total_grabbed;           // 总抢单数
    int today_grabbed;           // 今日抢单数
    int success_count;           // 成功次数
    int fail_count;              // 失败次数
    long total_time_ms;          // 总耗时
    long avg_response_time_ms;   // 平均响应时间
    
    Statistics() 
        : total_grabbed(0), today_grabbed(0), success_count(0), 
          fail_count(0), total_time_ms(0), avg_response_time_ms(0) {}
    
    float get_success_rate() const {
        int total = success_count + fail_count;
        return total > 0 ? (float)success_count / total * 100.0f : 0.0f;
    }
};

} // namespace tishou

#endif // TISHOU_TYPES_H