# 更新日志 (Changelog)

本文档记录替手 (TiShou) 项目的所有重要更新。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [未发布]

### 新增
- 完整的 Android 项目架构 (Kotlin + NDK + JNI)
- 无障碍服务自动监控和点击功能
- OCR 文字识别模块 (支持 CSDN OCR API)
- 后台保活服务 (澎湃 OS 适配)
- 悬浮窗快捷操作
- iOS 圆润风格 Material Design 3 UI
- 抢单规则配置系统
- 日志记录与导出功能
- 统计分析界面
- 权限引导流程
- 启动页和首次启动引导

### 技术特性
- NDK 高性能核心调度引擎
- Room 数据库持久化
- SharedPreferences 配置存储
- 前台服务保活
- 手势模拟 (点击、滑动、长按)
- 图像预处理 (JNI 调用)
- 多线程安全机制

### UI 组件
- RoundedCard 圆角卡片组件
- iOSButton iOS 风格按钮组件
- Material Design 3 卡片列表
- ViewPager2 引导页面
- RecyclerView 适配器
- 自定义通知栏样式

### 数据模型
- OrderInfo 订单信息
- GrabRule 抢单规则
- GrabLog 抢单日志
- GrabRecord 抢单记录
- Configuration 配置项

### 服务模块
- TiShouAccessibilityService 无障碍服务
- KeepAliveService 后台保活服务
- FloatingWindowService 悬浮窗服务

### 界面 Activity
- SplashActivity 启动页
- PermissionGuideActivity 权限引导
- MainActivity 主界面
- RuleConfigActivity 规则配置
- LogActivity 日志查看
- StatsActivity 统计分析
- SettingsActivity 应用设置

### 工具类
- TiShouCore 核心控制器
- OcrManager OCR 管理器
- OcrApiService OCR API 服务
- ConfigManager 配置管理器
- TiShouDatabase Room 数据库
- LogDatabase 日志数据库

---

## [1.0.0] - 2026-05-31

### 初始版本
首个完整功能版本发布。

#### 核心功能
- ✅ 无障碍服务自动化
- ✅ OCR 文字识别
- ✅ 后台保活 (澎湃 OS 适配)
- ✅ 悬浮窗快捷操作
- ✅ 抢单规则配置
- ✅ 日志记录与统计
- ✅ 精美 UI 设计

#### 技术栈
- Kotlin 1.9+
- Android SDK 26-34
- NDK 25+
- Material Design 3
- Room 2.6+

---

## 版本说明

### 版本号规则
- **主版本号 (Major)**: 不兼容的 API 变更或重大功能重构
- **次版本号 (Minor)**: 向后兼容的功能性新增
- **修订号 (Patch)**: 向后兼容的问题修复

### 符号说明
- `新增` - 新增功能
- `变更` - 现有功能的变更
- `弃用` - 即将移除的功能
- `移除` - 已移除的功能
- `修复` - Bug 修复
- `安全` - 安全性改进

---

## 计划中的功能

### v1.1.0 (计划中)
- [ ] 集成 MPAndroidChart 图表库
- [ ] 七日抢单趋势图
- [ ] 成功率趋势图
- [ ] 配置导入/导出功能完善
- [ ] 夜间模式优化

### v1.2.0 (计划中)
- [ ] 多 OCR 引擎支持 (PaddleOCR、Tesseract)
- [ ] 离线 OCR 识别
- [ ] 云端同步配置
- [ ] 多设备协同

### v2.0.0 (规划中)
- [ ] 插件系统架构
- [ ] 脚本自定义支持
- [ ] AI 智能决策引擎
- [ ] 多平台支持 (iOS)

---

## 贡献者

感谢所有为替手项目做出贡献的开发者！

如需参与贡献，请查看项目的 [贡献指南](CONTRIBUTING.md)。

---

**最后更新**: 2026-05-31