#include <jni.h>
#include <string>
#include "tishou_core.h"

extern "C" {

// JNI 方法注册表
static JNINativeMethod methods[] = {
    {"nativeInit", "()Z", (void*)Java_com_tishou_assistant_core_TiShouCore_nativeInit},
    {"nativeDestroy", "()V", (void*)Java_com_tishou_assistant_core_TiShouCore_nativeDestroy},
    {"nativeStartMonitoring", "()V", (void*)Java_com_tishou_assistant_core_TiShouCore_nativeStartMonitoring},
    {"nativeStopMonitoring", "()V", (void*)Java_com_tishou_assistant_core_TiShouCore_nativeStopMonitoring},
    {"nativeProcessOcrResult", "(Ljava/lang/String;)V", (void*)Java_com_tishou_assistant_core_TiShouCore_nativeProcessOcrResult},
    {"nativeGetStatus", "()I", (void*)Java_com_tishou_assistant_core_TiShouCore_nativeGetStatus},
    {"nativeGetStatistics", "()Lcom/tishou/assistant/core/Statistics;", (void*)Java_com_tishou_assistant_core_TiShouCore_nativeGetStatistics},
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    // 查找 TiShouCore 类
    jclass clazz = env->FindClass("com/tishou/assistant/core/TiShouCore");
    if (clazz == nullptr) {
        return -1;
    }

    // 注册 native 方法
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        return -1;
    }

    return JNI_VERSION_1_6;
}

} // extern "C"