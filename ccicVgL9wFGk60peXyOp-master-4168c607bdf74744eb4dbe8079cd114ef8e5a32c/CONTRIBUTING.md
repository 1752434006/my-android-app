# 贡献指南 (Contributing Guide)

感谢你考虑为替手 (TiShou) 项目做出贡献！🎉

本文档将帮助你了解如何参与项目开发。在开始之前，请花几分钟时间阅读以下内容。

## 📋 目录

- [行为准则](#行为准则)
- [开发环境设置](#开发环境设置)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [Pull Request 流程](#pull-request-流程)
- [问题报告](#问题报告)
- [功能建议](#功能建议)

---

## 行为准则

本项目采用 [Contributor Covenant](https://www.contributor-covenant.org/zh-cn/version/2/0/code_of_conduct/) 行为准则。

我们致力于提供一个开放、友好、包容的社区环境。无论你的年龄、体型、种族、宗教信仰、性别认同、性取向、经验水平等，都应受到尊重。

---

## 开发环境设置

### 1. Fork 项目

在 GitHub 上点击 `Fork` 按钮创建你自己的副本。

### 2. 克隆到本地

```bash
git clone https://github.com/YOUR_USERNAME/TiShouAssistant.git
cd TiShouAssistant
```

### 3. 添加上游仓库

```bash
git remote add upstream https://github.com/ORIGINAL_OWNER/TiShouAssistant.git
```

### 4. 创建开发分支

```bash
git checkout -b feature/your-feature-name
```

### 5. 安装依赖

打开 Android Studio，等待 Gradle 自动同步完成。

---

## 开发流程

### 1. 选择任务

查看项目的 [Issues](https://github.com/TiShouAssistant/issues) 或 [TODO.md](TODO.md)，选择一个你想解决的问题。

### 2. 创建分支

分支命名规则：
- `feature/xxx` - 新功能
- `fix/xxx` - Bug 修复
- `docs/xxx` - 文档更新
- `refactor/xxx` - 代码重构
- `test/xxx` - 测试相关

示例：
```bash
git checkout -b feature/add-chart-support
```

### 3. 进行开发

按照项目的代码规范进行开发，确保：
- ✅ 代码能通过编译
- ✅ 单元测试通过
- ✅ 遵循代码风格指南
- ✅ 添加必要的注释

### 4. 提交更改

```bash
git add .
git commit -m "feat: 添加图表支持"
```

### 5. 同步上游

```bash
git fetch upstream
git rebase upstream/main
```

### 6. 推送分支

```bash
git push origin feature/add-chart-support
```

---

## 代码规范

### Kotlin 代码规范

#### 命名约定

```kotlin
// 类名：大驼峰
class OrderInfo { }

// 函数和变量：小驼峰
fun calculatePrice() { }
val orderCount = 0

// 常量：全大写 + 下划线
const val MAX_RETRY_COUNT = 3

// 包名：全小写 + 点号
package com.tishou.assistant.core
```

#### 代码格式

```kotlin
// 使用 4 个空格缩进
class MyClass {
    private val value = 0
    
    fun doSomething() {
        // ...
    }
}

// 运算符两侧留空格
val sum = a + b

// 控制语句括号后留空格
if (condition) {
    // ...
}

// 函数调用括号前不留空格
doSomething()
```

#### 注释规范

```kotlin
/**
 * 计算订单总价
 * 
 * @param items 商品列表
 * @param discount 折扣率
 * @return 最终价格
 */
fun calculateTotal(items: List<Item>, discount: Double): Double {
    // 实现细节
}

// 单行注释使用 //
// 这是单行注释
```

### C++ 代码规范

```cpp
// 类名：大驼峰
class TiShouScheduler { };

// 函数和变量：小驼峰
void processData();
int itemCount = 0;

// 常量：全大写 + 下划线
const int MAX_BUFFER_SIZE = 1024;

// 使用智能指针管理内存
std::unique_ptr<Data> data = std::make_unique<Data>();
```

### XML 资源规范

```xml
<!-- 布局文件使用小写 + 下划线 -->
<LinearLayout
    android:id="@+id/content_layout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    
    <TextView
        android:id="@+id/title_text"
        style="@style/TextAppearance.Material3.HeadlineSmall"
        android:text="@string/app_name" />
        
</LinearLayout>
```

---

## 提交规范

本项目遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/v1.0.0/) 规范。

### 提交类型

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式调整（不影响功能） |
| `refactor` | 代码重构（非新功能、非修复） |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建过程或辅助工具变动 |

### 提交格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 示例

```bash
# 新功能
git commit -m "feat(ui): 添加统计分析图表"

# Bug 修复
git commit -m "fix(ocr): 修复 OCR 识别超时问题"

# 文档更新
git commit -m "docs(readme): 更新快速开始指南"

# 代码重构
git commit -m "refactor(core): 优化调度器线程管理"
```

---

## Pull Request 流程

### 1. 创建 PR

在 GitHub 上进入你的 Fork 仓库，点击 `Compare & pull request`。

### 2. 填写 PR 描述

请使用以下模板：

```markdown
## 📝 变更说明
简要描述此 PR 的目的

## 🔗 关联 Issue
Fixes #123

## ✅ 检查清单
- [ ] 代码已通过编译
- [ ] 添加了必要的测试
- [ ] 更新了相关文档
- [ ] 遵循了代码规范

## 📸 截图 (如适用)
添加 UI 变更的截图
```

### 3. 代码审查

维护者会审查你的代码，可能会提出修改建议。请耐心回应并及时修改。

### 4. 合并

审查通过后，你的代码将被合并到主分支。

---

## 问题报告

发现 Bug？请创建 [Issue](https://github.com/TiShouAssistant/issues)。

### Issue 模板

```markdown
## 🐛 问题描述
清晰简洁地描述这个 Bug

## 🔄 复现步骤
1. 打开应用
2. 点击...
3. 出现错误

## ✅ 预期行为
应该发生什么

## ❌ 实际行为
实际发生了什么

## 📱 设备信息
- 设备型号：小米 14
- 系统版本：澎湃 OS 1.0
- 应用版本：1.0.0

## 📸 截图
添加相关截图
```

---

## 功能建议

有新的想法？欢迎提交功能建议！

### 建议模板

```markdown
## 💡 功能描述
清晰简洁地描述这个功能

## 🎯 使用场景
这个功能能解决什么问题

## 📋 实现思路
如何实现这个功能（可选）

## 🖼️ 设计稿
如有设计稿或示意图请附上
```

---

## 常见问题

### Q: 我可以同时处理多个 Issue 吗？

A: 可以，但建议先完成一个再开始下一个，确保质量。

### Q: 我的 PR 多久会被审查？

A: 通常在 1-3 个工作日内，请耐心等待。

### Q: 如何联系维护者？

A: 可以通过 Issue 评论或邮件联系。

---

## 致谢

感谢所有为替手项目做出贡献的开发者！🙏

你们的每一份贡献都让这个项目变得更好。

---

**最后更新**: 2026-05-31