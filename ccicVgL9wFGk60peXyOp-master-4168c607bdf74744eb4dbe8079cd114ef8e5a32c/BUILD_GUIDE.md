# TiShou Android 项目构建指南

## ✅ 已完成的配置修复

您的项目现已补全所有缺失的 Gradle 配置文件：

### 新增文件列表
- ✅ `settings.gradle` - 项目配置和模块声明
- ✅ `gradlew` - Unix Gradle Wrapper 脚本（已添加执行权限）
- ✅ `gradlew.bat` - Windows Gradle Wrapper 脚本
- ✅ `gradle/wrapper/gradle-wrapper.properties` - Gradle 版本配置（8.2）
- ✅ `gradle/wrapper/gradle-wrapper.jar` - Gradle Wrapper JAR
- ✅ `local.properties` - SDK 路径配置模板

### 修复的文件
- ✅ `build.gradle` - 更新为 AGP 8.x 新语法（plugins DSL）

---

## 📋 本地打包步骤

由于当前容器环境未安装 Android SDK，您需要在**本地计算机**上完成打包：

### 方式一：使用 Android Studio（推荐）

1. **打开项目**
   ```bash
   # 在 Android Studio 中打开项目根目录
   ```

2. **同步 Gradle**
   - Android Studio 会自动检测并下载 Gradle 8.2
   - 等待右下角 "Gradle Sync" 完成

3. **构建 APK**
   - 点击菜单：`Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 或使用快捷键：`Ctrl + Shift + A` (Win/Linux) / `Cmd + Shift + A` (Mac)，输入 "Build APK"

4. **获取 APK 文件**
   - 构建完成后，APK 位于：`app/build/outputs/apk/debug/app-debug.apk`
   - 点击通知栏的 "locate" 可直接打开文件夹

### 方式二：使用命令行

#### 前置条件
确保已安装：
- **JDK 17**（必须）
- **Android SDK**（包含 Android 14/API 34）
- **Android NDK 25.2.9519653**（用于 native 构建）

#### 配置环境变量
```bash
# Linux/macOS
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Windows (PowerShell)
$env:ANDROID_HOME = "$env:USERPROFILE\AppData\Local\Android\Sdk"
$env:PATH += ";$env:ANDROID_HOME\tools;$env:ANDROID_HOME\platform-tools"
```

#### 执行构建
```bash
# 清理并构建 Debug APK
./gradlew clean assembleDebug

# 构建 Release APK（需要签名配置）
./gradlew clean assembleRelease
```

#### 输出位置
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## 🔧 常见问题解决

### 问题 1：SDK location not found
**错误信息：**
```
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable
```

**解决方案：**
编辑 `local.properties` 文件，设置正确的 SDK 路径：
```properties
sdk.dir=/Users/yourname/Library/Android/sdk  # macOS
# 或
sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk  # Windows
# 或
sdk.dir=/home/yourname/Android/Sdk  # Linux
```

### 问题 2：NDK version mismatch
**错误信息：**
```
NDK version 25.2.9519653 not found
```

**解决方案：**
1. 打开 Android Studio → `Tools` → `SDK Manager`
2. 切换到 `SDK Tools` 标签页
3. 勾选 `NDK (Side by side)` 版本 25.2.9519653
4. 点击 Apply 下载安装

或在 `local.properties` 中指定 NDK 路径：
```properties
ndk.dir=/Users/yourname/Library/Android/sdk/ndk/25.2.9519653
```

### 问题 3：Java version error
**错误信息：**
```
This version of the Android Support plugin requires Java 17
```

**解决方案：**
确保使用 JDK 17：
```bash
java -version  # 应显示 "17.x.x"
```

在 Android Studio 中设置：
- `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle`
- 设置 `Gradle JDK` 为 JDK 17

### 问题 4：Build failed - CMake not found
**错误信息：**
```
CMake is required for external native build
```

**解决方案：**
1. 打开 Android Studio → `Tools` → `SDK Manager`
2. 切换到 `SDK Tools` 标签页
3. 勾选 `CMake` 和 `LLDB`
4. 点击 Apply 安装

---

## 📦 APK 签名（发布正式版本）

### 生成签名密钥
```bash
keytool -genkey -v -keystore tishou-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias tishou
```

### 配置签名
在 `app/build.gradle` 的 `android` 块中添加：
```groovy
signingConfigs {
    release {
        storeFile file('../tishou-release-key.jks')
        storePassword 'your_store_password'
        keyAlias 'tishou'
        keyPassword 'your_key_password'
    }
}

buildTypes {
    release {
        signingConfig signingConfigs.release
        // ...其他配置
    }
}
```

### 构建已签名的 Release APK
```bash
./gradlew clean assembleRelease
```

---

## 📊 构建配置说明

### 项目使用的技术栈
| 组件 | 版本 |
|------|------|
| Gradle | 8.2 |
| Android Gradle Plugin | 8.2.0 |
| Kotlin | 1.9.20 |
| Compile SDK | 34 (Android 14) |
| Target SDK | 34 |
| Min SDK | 26 (Android 8.0) |
| NDK | 25.2.9519653 |
| Java Version | 17 |

### 支持的 ABI 架构
- armeabi-v7a（32 位 ARM）
- arm64-v8a（64 位 ARM）
- x86（32 位 Intel）
- x86_64（64 位 Intel）

### 构建特性
- ✅ ViewBinding 启用
- ✅ AIDL 支持
- ✅ D8 脱糖
- ✅ R8 代码优化
- ✅ ProGuard 混淆（Release 模式）
- ✅ 分 ABI 打包（减小 APK 体积）

---

## 🚀 快速验证构建

最简单的验证方式：
```bash
# 1. 清理构建缓存
./gradlew clean

# 2. 构建 Debug APK
./gradlew assembleDebug

# 3. 检查输出
ls -lh app/build/outputs/apk/debug/
```

如果看到 `app-debug.apk` 文件（约 20-50MB），则构建成功！

---

## 📞 需要帮助？

如果遇到其他构建问题，请提供：
1. 完整的错误日志
2. 执行的命令
3. 操作系统和 Android Studio 版本

祝构建顺利！🎉