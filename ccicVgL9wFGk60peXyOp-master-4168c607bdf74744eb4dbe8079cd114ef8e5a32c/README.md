# 替手 (TiShou) - Android 自动化辅助工具

一款基于无障碍服务 + OCR + NDK 的 Android 订单自动抢夺辅助工具，重点适配小米澎湃 OS。

## 🎯 项目目标

开发一款高性能、低延时的订单自动抢夺辅助工具，通过智能识别屏幕内容并自动执行点击操作，帮助用户快速抢取目标订单。

## 🛠️ 技术栈

- **语言**: Kotlin + C++(NDK) + JNI
- **构建工具**: Gradle (阿里云镜像源)
- **UI 框架**: Material Design 3 (融合 iOS 圆润风格)
- **核心技术**: 
  - AccessibilityService (无障碍服务)
  - OCR 文字识别
  - NDK 高性能计算
  - Room 数据库

## 📁 项目结构

```
app/
├── src/main/
│   ├── cpp/                    # NDK C++ 核心引擎
│   │   ├── native-lib.cpp      # JNI 桥接
│   │   ├── tishou_core.cpp     # 核心调度器
│   │   ├── tishou_core.h       # 核心头文件
│   │   └── types.h             # 类型定义
│   ├── java/com/tishou/assistant/
│   │   ├── core/               # 核心模块
│   │   │   └── TiShouCore.kt   # 核心控制器
│   │   ├── data/               # 数据层
│   │   │   ├── ConfigManager.kt    # 配置管理
│   │   │   ├── TiShouDatabase.kt   # Room 数据库
│   │   │   └── LogDatabase.kt      # 日志数据库
│   │   ├── model/              # 数据模型
│   │   │   ├── OrderInfo.kt    # 订单信息
│   │   │   ├── GrabRule.kt     # 抢单规则
│   │   │   └── GrabLog.kt      # 抢单日志
│   │   ├── ocr/                # OCR 模块
│   │   │   ├── OcrManager.kt       # OCR 管理器
│   │   │   └── OcrApiService.kt    # OCR API 服务
│   │   ├── service/            # 后台服务
│   │   │   ├── TiShouAccessibilityService.kt  # 无障碍服务
│   │   │   ├── KeepAliveService.kt            # 保活服务
│   │   │   └── FloatingWindowService.kt       # 悬浮窗服务
│   │   ├── ui/                 # UI 界面
│   │   │   ├── MainActivity.kt         # 主界面
│   │   │   ├── SplashActivity.kt       # 启动页
│   │   │   ├── PermissionGuideActivity.kt  # 权限引导
│   │   │   ├── RuleConfigActivity.kt     # 规则配置
│   │   │   ├── LogActivity.kt            # 日志查看
│   │   │   ├── StatsActivity.kt          # 统计分析
│   │   │   ├── SettingsActivity.kt       # 应用设置
│   │   │   └── view/             # 自定义 View
│   │   │       ├── RoundedCard.kt      # 圆角卡片
│   │   │       └── iOSButton.kt        # iOS 风格按钮
│   └── res/                    # 资源文件
│       ├── layout/             # 布局文件
│       ├── drawable/           # 图标资源
│       ├── values/             # 颜色、字符串、主题
│       └── xml/                # 配置文件
└── build.gradle                # 模块构建配置
```

## ✨ 核心功能

### 1. 智能订单识别
- 通过 OCR 技术自动识别屏幕上的订单信息
- 支持关键词匹配、价格区间筛选、区域过滤
- 正则表达式高级匹配

### 2. 无障碍自动化
- 监控界面变化，实时检测新订单
- 自动执行点击、滑动等操作
- 手势模拟（长按、拖拽等复杂操作）

### 3. 后台保活 (澎湃 OS 适配)
- 前台服务提升优先级
- 持久通知栏显示运行状态
- 电池优化白名单引导
- 自启动广播接收器

### 4. 悬浮窗快捷操作
- iOS 风格圆形悬浮球
- 支持拖拽移动
- 点击展开快捷菜单
- 快速控制服务启停

### 5. 规则配置系统
- 多条件组合规则（与/或逻辑）
- 关键词、价格、区域等多维度筛选
- 规则启用/禁用开关
- SharedPreferences 持久化存储

### 6. 日志与统计
- 实时抢单记录
- 成功率统计
- 响应时间分析
- 日志导出功能 (CSV 格式)

### 7. 精美 UI 设计
- Material Design 3 基础
- iOS 圆润风格融合
- 超大圆角 (24dp+)
- 细腻阴影层次
- 深色模式适配

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17+
- Android SDK 26+ (推荐 34)
- NDK 25+

### 构建步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd TiShouAssistant
```

2. **配置本地属性** (可选)
在 `gradle.properties` 中配置：
```properties
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

3. **同步 Gradle**
打开 Android Studio，等待 Gradle 同步完成

4. **构建 APK**
```bash
./gradlew assembleDebug
```

5. **安装到设备**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📱 使用说明

### 首次启动流程

1. **开启无障碍服务**
   - 进入系统设置 → 无障碍 → 替手助手
   - 开启服务开关

2. **授予悬浮窗权限**
   - 进入系统设置 → 应用管理 → 替手助手 → 权限
   - 开启"显示在其他应用上层"

3. **加入电池优化白名单**
   - 进入系统设置 → 电池与性能 → 应用智能省电
   - 找到替手助手，设置为"无限制"

4. **配置抢单规则**
   - 打开应用，进入"抢单规则"
   - 添加关键词、价格区间等筛选条件

### 主要功能入口

| 功能 | 说明 |
|------|------|
| 启动/停止服务 | 主界面大按钮，一键控制 |
| 抢单规则 | 配置关键词、价格、区域等筛选条件 |
| 运行日志 | 查看抢单记录和系统日志 |
| 统计分析 | 查看成功率和抢单趋势 |
| 应用设置 | OCR API Key、震动反馈等配置 |
| 悬浮球 | 全局快捷操作入口 |

## ⚙️ 配置说明

### OCR API 配置

本项目使用 CSDN OCR API 进行文字识别：

1. 获取 API Key: 访问 [CSDN AI 开放平台](https://ai.csdn.net/)
2. 在应用设置中填入 API Key
3. 保存后即可使用 OCR 功能

### 高级设置

| 选项 | 说明 | 默认值 |
|------|------|--------|
| 自动启动 | 开机自动启动服务 | 关闭 |
| 震动反馈 | 抢单成功时震动提示 | 开启 |
| 声音提示 | 抢单成功时声音提醒 | 开启 |
| 图像预处理 | OCR 前进行图像优化 | 开启 |

## 🔒 隐私与安全

- 所有数据存储在本地，不上传云端
- OCR 识别仅通过官方 API 进行
- 无障碍服务权限仅用于自动化操作
- 不包含任何广告或追踪代码

## ⚠️ 注意事项

1. **合法使用**: 请确保在合法合规的场景下使用本工具
2. **系统兼容**: 澎湃 OS 需关闭"内存扩展"功能以获得最佳保活效果
3. **电量消耗**: 后台持续运行会增加电量消耗，建议充电时使用
4. **网络依赖**: OCR 功能需要网络连接，离线模式下无法识别

## 📄 开源协议

MIT License

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- Email: support@tishou.com
- GitHub Issues: [提交问题](https://github.com/tishou/TiShouAssistant/issues)

---

**⚠️ 免责声明**: 本工具仅供学习研究使用，请勿用于违法违规用途。使用本工具产生的一切后果由使用者自行承担。