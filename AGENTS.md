# AGENTS.md

## 当前优先上下文

这是 TZZ Mod / WebAdmin 项目的 Codex 工作说明。

开始任何任务前，优先阅读：

`docs/WEBADMIN_UI_REFACTOR_7_5_CURRENT_CONTEXT.md`

如果历史文档与该 current context 冲突，以 current context 为准。

历史 5.x / 6.x / 7.0-7.4 文档只作背景，不代表当前 7.5 UI Refactor 的真实范围、当前实现状态、图标规则或页面落地范围。

---

## 当前项目状态

当前工作重点：

`7.5 WebAdmin UI Refactor`

当前 Step 1 已围绕以下页面和基础层推进：

- 登录页
- App Shell / sidebar / topbar
- Dashboard / 总览页
- SignalBridge
- Receivers / 接收器
- image2 / 矢量化图标体系
- silent refresh
- responsive layout
- disabled / unavailable operations
- current context 与浏览器验收文档

后续任务开始前，应先确认当前分支和工作树状态，不要基于旧文档猜测当前能力。

---

## 工作边界

除非用户明确要求，禁止：

- 修改 Figma
- 新增后端 API
- 新增业务功能
- 启用没有后端支持的写操作
- 一次性做 18 页全量落地
- 做子详情页
- 实现 matcher / itemSubmit
- 实现 Scratch-like editor
- 实现 ConditionEngine
- 大规模拆分前端文件
- commit / push / merge / tag

如果用户要求 checkpoint commit，也必须先运行构建和稳定性测试。

---

## WebAdmin 前端规则

WebAdmin 是 Minecraft / Mod 内置 Web 服务，不是独立前端站点。

当前前端资源主要在：

- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendShell.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendStyles.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java`

不要假设存在 React / Vite / npm 前端工程。

Codex 通常无法启动真实 Minecraft WebAdmin 服务访问 `http://127.0.0.1:18080/`，因此真实浏览器验收由用户执行。

Codex 仍必须完成：

- Java build
- `stabilizationGuardTest`
- `git diff --check`
- JS syntax check（如果涉及生成的前端脚本）
- render smoke test（如果可行）
- 测试 Markdown 更新
- 代码层面的响应式、点击跳转、silent refresh、disabled 状态实现

不要把“无法访问 18080”当作跳过代码层验证的理由。

---

## 设计规则

Figma `1536×864` 只是视觉参考，不是固定尺寸。

禁止：

- 写死 `1536×864`
- 4K 下缩成小画布
- 小窗口下卡片重叠
- 表格压分页
- 按钮压输入框
- 右侧栏越界
- 用大量 absolute 定位硬搬 Figma
- 把 Figma 截图当背景图嵌进前端

前端必须根据浏览器窗口自由拉伸：

- 大屏内容自然扩展
- 表格列可变宽
- right rail 保持合理宽度，不无限拉宽
- 小屏 right rail 可下移
- 表格容器可横向滚动
- 筛选栏可换行
- 分页始终在表格下方

---

## Modal 规则

凡是涉及以下操作：

- 修改参数
- 修改配置
- 写入数据
- 保存设置
- 危险确认

后续都必须使用统一 Modal：

- animated modal
- backdrop blur / 毛玻璃遮罩
- 固定设计尺寸
- viewport 约束
- body 内部滚动
- header/footer 固定
- danger action 使用红色语义
- 不在主页面直接展开复杂编辑表单
- 不在表格行内做复杂编辑

没有完整后端支持的写操作不能因为 UI 已经画出按钮就启用。

---

## 图标规则

WebAdmin 自定义图标应使用当前 7.5 认可的 image2 / 矢量化图标体系。

当前偏好：

- 2D
- 扁平化
- 简约
- 科技风
- 深色后台适配
- 透明背景
- 无背景底图
- 无圆形底 / 方形底画进图标资产
- UI 圆底、hover、glow 由 CSS 控制

禁止：

- emoji
- 字母占位
- 纯字符图标
- 旧图标染色
- 无色旧图标
- 3D 立体风
- Minecraft 方块风 WebAdmin 图标
- 用 image2 重画 Minecraft 原版方块 / 物品材质

Minecraft 原版方块 / 物品图标必须使用原版材质资源。

---

## 后端边界

UI 中很多按钮只是产品方向，不代表后端已实现。

没有完整后端支持的操作必须：

- disabled
- unavailable
- 不发送 `POST`
- 不发送 `PATCH`
- 不发送 `DELETE`
- 不假装功能可用
- 不打开真实编辑表单

高风险写操作必须等以下能力完整后才允许启用：

- 权限检查
- CSRF / same-origin
- audit
- edit lock
- `WebAdminWriteResult`
- 回滚 / 错误处理边界

不要因为 Figma 或 UI 中有按钮就新增 API 或启用写操作。

---

## Realtime / Silent Refresh 规则

WebAdmin realtime / silent refresh 必须非扰动。

禁止：

- 整页 reload
- 白屏 / 黑屏闪烁
- 重置滚动位置
- 重置筛选条件
- 重置输入内容
- 重置分页
- 关闭已打开 modal
- 重建整个 app shell

允许：

- route-level silent refresh
- stale-while-revalidate
- visible row detail cache
- document hidden 时暂停或标记 dirty
- visible 后静默刷新一次

Receivers 页面当前要求：

- 接收器列表可通过现有详情数据源补齐 `pulseTicks`
- 默认值应来自当前代码常量，不要猜
- 修改 pulseTicks 后应通过 silent refresh 更新
- 不新增后端 API

---

## 常用验证命令

修改代码后通常需要运行：

```powershell
.\gradlew.bat clean build
.\gradlew.bat stabilizationGuardTest --rerun-tasks
git diff --check
```

如果涉及前端脚本生成，应尽量执行：

- JS syntax check
- render smoke test
- icon registry smoke test
- 关键页面 mock render

如果 PATH 上的 `node.exe` 不可用，可使用 Codex / 环境中可用的 bundled Node，但必须在报告中说明。

---

## Git 规则

除非用户明确要求：

- 不 commit
- 不 push
- 不 merge
- 不 tag

如果用户要求 checkpoint commit：

1. 先运行构建和稳定性测试。
2. 只暂存本轮相关文件。
3. 不提交 `logs/`。
4. 报告 commit hash、commit message、git status。
5. 不 push / merge / tag，除非用户明确要求。

---

## 测试文档规则

WebAdmin / API / service 阶段需要测试时，优先创建或更新 Markdown 测试文件。

测试文件必须包含：

- 新世界前置条件
- 数据准备步骤
- 精确命令
- 浏览器验收步骤
- 失败判定
- disabled / unavailable 边界
- 响应式检查
- Console / Network 检查

不要使用裸父命令作为测试命令，例如：

- `/tzz signal`
- `/tzz regionctl`

命令必须来自当前代码注册，或者明确要求使用 TAB 补全确认当前精确命令。不要根据历史记忆猜命令。

---

## 当前 7.5 Step 1 相关文件

当前 Step 1 常见相关文件包括：

- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendShell.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendStyles.java`
- `src/main/resources/assets/tzz_mod/webadmin/icons/`
- `docs/WEBADMIN_UI_REFACTOR_7_5_CURRENT_CONTEXT.md`
- `测试_7.5_WebAdmin前端重构第一阶段浏览器验收.md`

`logs/` 如未跟踪，默认不要处理、不要删除、不要提交。

---

## 旧文档使用说明

旧文档仍可作为历史背景，但不代表当前 7.5 UI Refactor 状态。

可能误导当前阶段的文档包括但不限于：

- 6.x readonly / realtime / write foundation 文档
- 7.0-7.4 editing / listener config 文档
- 7.0-7.4 regression test 文档
- 早期 WebAdmin 设计草稿

如果旧文档与当前代码或 `docs/WEBADMIN_UI_REFACTOR_7_5_CURRENT_CONTEXT.md` 冲突，以 current context 和当前代码为准。

---

## 子智能体

我允许你自主规划我下达的任务,指令,合理灵活运用子智能体提升效率,任何你认为可行的时候都可以,子智能体全权由你

在对话结束前,关闭子智能体,下次对话开始需要时重新创建

你可以自行使用子智能体辅助，但必须遵守：
- 子智能体默认只读或文档任务
- 代码修改只能由主智能体最终整合
- 不允许多个子智能体同时修改同一个文件
- 不允许子智能体在未经许可情况下新增后端 API / 业务功能
- 不允许子智能体 commit / push / merge / tag
- 所有子智能体结果必须由主智能体汇总后统一落地

---