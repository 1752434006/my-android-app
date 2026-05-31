#include <jni.h>
#include <android/log.h>
#include "tishou_core.h"

#define LOG_TAG "JNIBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace tishou;

// 全局引用，用于回调 Kotlin 层
static JavaVM* g_java_vm = nullptr;
static jobject g_core_instance = nullptr;
static jclass g_statistics_class = nullptr;
static jclass g_log_entry_class = nullptr;

/**
 * 缓存 Java 类和方法引用
 */
jint cacheJavaReferences(JNIEnv* env) {
    // 获取 Statistics 类引用
    jclass statsClass = env->FindClass("com/tishou/assistant/core/Statistics");
    if (statsClass == nullptr) {
        LOGE("找不到 Statistics 类");
        return JNI_ERR;
    }
    g_statistics_class = (jclass)env->NewGlobalRef(statsClass);
    env->DeleteLocalRef(statsClass);
    
    // 获取 LogEntry 类引用
    jclass logClass = env->FindClass("com/tishou/assistant/core/LogEntry");
    if (logClass == nullptr) {
        LOGE("找不到 LogEntry 类");
        return JNI_ERR;
    }
    g_log_entry_class = (jclass)env->NewGlobalRef(logClass);
    env->DeleteLocalRef(logClass);
    
    return JNI_OK;
}

/**
 * 清理 Java 引用
 */
void cleanupJavaReferences(JNIEnv* env) {
    if (g_statistics_class != nullptr) {
        env->DeleteGlobalRef(g_statistics_class);
        g_statistics_class = nullptr;
    }
    
    if (g_log_entry_class != nullptr) {
        env->DeleteGlobalRef(g_log_entry_class);
        g_log_entry_class = nullptr;
    }
    
    if (g_core_instance != nullptr) {
        env->DeleteGlobalRef(g_core_instance);
        g_core_instance = nullptr;
    }
}

/**
 * 将 C++ Statistics 转换为 Java Statistics 对象
 */
jobject createJavaStatistics(JNIEnv* env, const Statistics& stats) {
    if (g_statistics_class == nullptr) {
        return nullptr;
    }
    
    // 获取构造函数方法 ID
    jmethodID constructor = env->GetMethodID(
        g_statistics_class, 
        "<init>", 
        "(IIIJJJ)V"  // (totalGrabbed, todayGrabbed, successCount, failCount, avgResponseTimeMs, totalTimeMs, lastResetTime)
    );
    
    if (constructor == nullptr) {
        LOGE("找不到 Statistics 构造函数");
        return nullptr;
    }
    
    // 创建对象
    jobject javaStats = env->NewObject(
        g_statistics_class,
        constructor,
        static_cast<jint>(stats.total_grabbed),
        static_cast<jint>(stats.today_grabbed),
        static_cast<jint>(stats.success_count),
        static_cast<jint>(stats.fail_count),
        static_cast<jlong>(stats.avg_response_time_ms),
        static_cast<jlong>(stats.total_time_ms),
        static_cast<jlong>(0)  // lastResetTime
    );
    
    return javaStats;
}

/**
 * 将 C++ LogEntry 转换为 Java LogEntry 对象
 */
jobject createJavaLogEntry(JNIEnv* env, const LogEntry& entry) {
    if (g_log_entry_class == nullptr) {
        return nullptr;
    }
    
    // 获取 LogLevel 枚举类
    jclass logLevelClass = env->FindClass("com/tishou/assistant/core/LogLevel");
    if (logLevelClass == nullptr) {
        return nullptr;
    }
    
    // 根据级别获取对应的枚举值
    const char* levelName = "";
    switch (entry.level) {
        case LogLevel::VERBOSE: levelName = "VERBOSE"; break;
        case LogLevel::DEBUG: levelName = "DEBUG"; break;
        case LogLevel::INFO: levelName = "INFO"; break;
        case LogLevel::WARNING: levelName = "WARNING"; break;
        case LogLevel::ERROR: levelName = "ERROR"; break;
    }
    
    jfieldID levelField = env->GetStaticFieldID(
        logLevelClass,
        levelName,
        "Lcom/tishou/assistant/core/LogLevel;"
    );
    
    if (levelField == nullptr) {
        env->DeleteLocalRef(logLevelClass);
        return nullptr;
    }
    
    jobject levelEnum = env->GetStaticObjectField(logLevelClass, levelField);
    env->DeleteLocalRef(logLevelClass);
    
    // 获取 LogEntry 构造函数
    jmethodID constructor = env->GetMethodID(
        g_log_entry_class,
        "<init>",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V"
    );
    
    if (constructor == nullptr) {
        return nullptr;
    }
    
    jstring tag = env->NewStringUTF(entry.tag.c_str());
    jstring message = env->NewStringUTF(entry.message.c_str());
    
    jobject javaEntry = env->NewObject(
        g_log_entry_class,
        constructor,
        levelEnum,
        tag,
        message,
        static_cast<jlong>(entry.timestamp)
    );
    
    env->DeleteLocalRef(tag);
    env->DeleteLocalRef(message);
    env->DeleteLocalRef(levelEnum);
    
    return javaEntry;
}

/**
 * 设置核心实例的全局引用
 */
void setCoreInstance(JNIEnv* env, jobject instance) {
    if (g_core_instance != nullptr) {
        env->DeleteGlobalRef(g_core_instance);
    }
    g_core_instance = env->NewGlobalRef(instance);
}

/**
 * 获取 JavaVM 指针
 */
JavaVM* getJavaVM() {
    return g_java_vm;
}

/**
 * JNI_OnLoad - 库加载时调用
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    
    g_java_vm = vm;
    
    // 缓存 Java 类引用
    if (cacheJavaReferences(env) != JNI_OK) {
        return JNI_ERR;
    }
    
    LOGI("JNI 库加载成功");
    return JNI_VERSION_1_6;
}

/**
 * JNI_OnUnload - 库卸载时调用
 */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    
    cleanupJavaReferences(env);
    LOGI("JNI 库卸载完成");
}