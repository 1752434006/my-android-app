# CMakeLists.txt for TiShou Android Assistant
# NDK 核心引擎编译配置

cmake_minimum_required(VERSION 3.22.1)

# 项目配置
project("tishou_core")

# 启用 C++17 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 编译优化选项
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -ffast-math")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -DDEBUG")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")

# 包含目录
include_directories(
    ${CMAKE_SOURCE_DIR}/app/src/main/cpp/include
    ${CMAKE_SOURCE_DIR}/app/src/main/cpp
)

# 源文件
set(SOURCES
    app/src/main/cpp/native-lib.cpp
    app/src/main/cpp/tishou_core.cpp
    app/src/main/cpp/jni_bridge.cpp
    app/src/main/cpp/ocr_preprocessor.cpp
)

# 头文件
set(HEADERS
    app/src/main/cpp/tishou_core.h
    app/src/main/cpp/jni_bridge.h
    app/src/main/cpp/ocr_preprocessor.h
    app/src/main/cpp/types.h
)

# 创建共享库
add_library(
    tishou_core
    SHARED
    ${SOURCES}
    ${HEADERS}
)

# 链接系统库
find_library(
    log-lib
    log
)

find_library(
    android-lib
    android
)

# 链接库
target_link_libraries(
    tishou_core
    ${log-lib}
    ${android-lib}
)

# 输出编译信息
message(STATUS "TiShou Core Library Configuration:")
message(STATUS "  - C++ Standard: C++17")
message(STATUS "  - Optimization: Release build with -O3")
message(STATUS "  - ABI Filters: armeabi-v7a, arm64-v8a, x86, x86_64")