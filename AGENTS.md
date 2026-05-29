# AGENTS.md

## 0. 文件定位

这是 TZZ Mod / WebAdmin 项目的长期 Codex 工作说明。

本文件只写长期有效的协作规则、工程边界、测试规则和安全规则。
**不要在本文件里写死当前阶段号、当前版本号、当前分支名、当前 tag、当前 commit hash 或某个阶段的临时范围。**

阶段专用信息应写入对应的 `docs/*_CURRENT_CONTEXT.md`、阶段提示词、验收文档或用户当前消息中。

---

## 1. 开始任何任务前必须做

开始任务前，Codex 必须先确认当前真实状态，而不是根据历史记忆猜测。

必须执行或读取：

1. 当前用户消息 / 当前任务提示词。
2. 与当前任务相关的 `docs/*_CURRENT_CONTEXT.md`。
3. 与当前任务相关的 capability matrix / test plan / README 段落。
4. 当前分支、HEAD、远端状态、工作区状态：

```powershell
git status --short --branch
git rev-parse --short HEAD
git branch --show-current
```

如果任务涉及从稳定基线开新分支、merge、tag、release，还必须按用户或 ChatGPT 给出的具体基线执行：

```powershell
git fetch origin --prune --tags
git rev-parse --short origin/master
```

如果当前仓库状态与用户 / ChatGPT 给出的基线不一致，必须停止并报告，不要自行猜测或强行继续。

---

## 1.1 Obsidian 外部项目记忆 / 精确仓库索引

Obsidian vault：

```text
E:\minecraftserver\fabricmod\tzz-mod-docs-obsidian\Tzz Mod work
```

Codex 必须把该 vault 当成滚动式精确仓库索引和外部项目记忆库，而不只是总结文档。

长期规则：

1. 每次涉及 TZZ Mod 开发、重构、性能优化、测试、文档、checkpoint、merge/tag 的任务开始时，先读取相关 Obsidian 笔记。
2. Obsidian 记录功能位置、文件职责、类/函数职责、调用关系、状态边界、数据流、guard/test 规则、历史踩坑、性能热点、版本决策和 deferred 风险。
3. 新增模块、文件、service、guard、测试、数据结构或关键流程时，必须同步补充到 Obsidian 索引。
4. 如果 Obsidian 旧笔记与当前源码不一致，必须修正笔记，标注旧信息 stale / deprecated，并写入新的路径、职责和最后核验 commit / tag。
5. Obsidian 笔记必须结构化，至少包含状态、最后核验版本或 commit、相关源码路径、相关类/函数、职责、不变量、测试入口和相关笔记链接。
6. 禁止把大段源码直接复制进 Obsidian；只记录索引、职责、数据流、设计原因和维护边界。
7. 不得覆盖无关个人笔记；只修改 TZZ Mod 相关目录和索引。
8. Obsidian 是辅助记忆，不替代当前仓库检查、源码验证和自动测试。
9. vault 已知不是主仓库的一部分；不要 `git init`，不要尝试把 vault 当主仓库提交，只在报告中列出创建/修改的笔记路径。

默认优先读取：

- `99_索引/TZZ_Mod_索引.md`
- `00_项目总览/TZZ_Mod_项目总览.md`
- `12_仓库精确索引/功能到文件索引.md`
- `13_源码文件职责/`
- `15_测试与Guard索引/改动后该跑哪些测试.md`

涉及 WebAdmin / Logic Chain / runtime / guard 时，还要读取对应模块目录和流程索引，并用当前源码验证关键结论是否仍准确。

---

## 2. 项目长期目标

TZZ Mod 的长期目标不是单一功能模组，也不是普通后台面板，而是逐步成为面向 Minecraft 小游戏开发的 **小游戏 IDE**。

目标包括：

- 用 WebAdmin / 游戏内工具帮助管理员快速搭建小游戏。
- 让“全员逃走中”类小游戏从原本复杂的数据包 / 命令方块 / scoreboard 工作流，压缩到约 2–3 天可完成基础构建。
- 通过可视化编辑、模板、条件系统、状态变量、触发器、动作系统、诊断和预览降低开发门槛。
- 逐步提供简单版本控制能力，例如草稿、发布、变更历史、diff、快照、恢复、回滚、安全恢复点。
- 不实现复杂 Git 分支 / merge / rebase 这类高级版本控制功能，保持服务器管理员可理解、可操作。

所有新增功能都应服务于：

```text
降低配置成本
减少重复劳动
提升可调试性
提升可复用性
避免破坏旧逻辑
```

---

## 3. 长期架构边界

项目核心系统包括但不限于：

- SignalBridge：事件总线 / channel 联动。
- ActionEngine：统一动作执行。
- RegionController：区域 enter / exit / stay 控制。
- SignalDevice / VirtualBlockDevice：世界方块和虚拟设备触发源。
- itemSubmit / container / interaction：物品、容器、交互触发能力。
- ConditionEngine：只读条件判断 / gate。
- StateVariable：全局 / 玩家状态变量。
- WebAdmin：配置、编辑、调试、可视化、未来文档与模板中心。
- 未来 GameController / MissionSystem / PhaseController：高层小游戏流程编排。

关键原则：

```text
ConditionEngine 只判断，不写状态，不发信号，不执行动作。
SignalBridge 是事件总线，不是状态数据库。
StateVariable / GameState 保存状态。
Action / Controlled Action 修改世界或玩家。
GameController / MissionSystem 负责未来玩法流程编排。
```

---

## 4. 阶段边界规则

每个阶段必须严格遵守用户 / ChatGPT 当前提示词中的范围。

除非当前任务明确要求，禁止擅自扩展到：

- 具体逃走中任务 / 关卡。
- GameController / MissionSystem / PhaseController。
- 新 runtime integration。
- 新 WebAdmin API。
- 新 WebAdmin UI。
- 新 action 类型。
- 新 raw JSON editor。
- 任意 NBT path / 脚本表达式。
- MCP scenario。
- 启动 Minecraft。
- 生成截图矩阵。
- Figma 修改。
- commit / push / merge / tag。

如果某个功能“看起来顺手能做”，但不在当前阶段范围内，必须作为 deferred 记录，不要直接实现。

---

## 5. 子智能体规则

对非平凡实现、审查、返修、checkpoint 前检查、merge/tag 前检查，必须使用子智能体。
子智能体不是可选项。

要求：

- 根据任务复杂度创建合适数量的只读子智能体。
- 复杂阶段通常至少 5 个；涉及 UI + runtime + tests 的阶段可使用 6 个或更多。
- 子智能体负责审查、发现风险、提出建议。
- 代码修改由主智能体最终整合。
- 子智能体不得 commit / push / merge / tag。
- 子智能体不得在未经主智能体整合的情况下并行修改同一文件。
- 如果 Codex 环境无法真实使用子智能体，必须停止并报告，不能假装完成。

最终报告必须包含：

```text
子智能体角色
审查轮次
每个子智能体结论
阻断项
建议项
主智能体采纳 / 不采纳原因
```

常见子智能体角色：

- 阶段边界 / 架构审查。
- 旧逻辑兼容审查。
- Runtime context / snapshot 审查。
- WebAdmin UI / UX 审查。
- API / 权限 / CSRF / audit / realtime 审查。
- 测试 / guard / docs / Git 卫生审查。

---

## 6. WebAdmin 前端规则

WebAdmin 是 Minecraft / Mod 内置 Web 服务，不是独立前端站点。

当前前端资源主要在 Java 字符串资源中，例如：

- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendShell.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendStyles.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java`

不要假设存在 React / Vite / 独立前端工程，除非当前仓库代码已经明确存在。

Codex 通常无法启动真实 Minecraft WebAdmin 服务访问本地浏览器，因此真实 UI 体验由用户人工验收。
但 Codex 仍必须完成代码层验证：

- Java build。
- WebAdmin 相关单元测试 / service test。
- stabilization guard。
- JS syntax check，如涉及前端脚本。
- render / route / marker smoke test，如项目已有。
- `git diff --check`。

不能把“无法访问真实 WebAdmin”当作跳过验证的理由。

---

## 7. WebAdmin UI / UX 规则

WebAdmin 必须保持一致的暗色后台风格。

通用要求：

- 用户可见主文案使用中文。
- 技术 ID 只能作为副文本。
- edit lock 不能只靠 toast；被锁定时按钮应 disabled 或明确显示锁状态。
- validation error 不清空用户输入。
- 删除确认不要求用户输入完整 ID / 名称，除非当前任务明确要求。
- 复杂编辑使用 modal / drawer / 单独编辑面板，不在主页面无限堆表单。
- action list / condition node list / requirement list 默认使用紧凑 summary card。
- 点击整张卡片或整行应进入详情 / 编辑，不能只依赖很小的文字按钮。
- 未保存修改关闭时必须 dirty confirm。
- realtime refresh 不应闪屏、跳顶、关闭 modal、清空输入、重置筛选或分页。

响应式要求：

- 不写死固定分辨率。
- 不把 Figma 截图当背景图。
- 大屏自然扩展。
- 小屏可滚动、可换行。
- 表格容器可横向滚动。
- modal / drawer 受 viewport 约束，内部滚动。
- 输入框、下拉框、按钮不得互相覆盖或撑破容器。

---

## 8. Modal 规则

凡是涉及以下操作，应优先使用统一 modal / drawer：

- 修改参数。
- 修改配置。
- 写入数据。
- 保存设置。
- 危险确认。
- 子节点 / 子组 / action / requirement 的详细编辑。

统一 modal 要求：

- 有打开 / 关闭动画。
- 有 backdrop blur / 遮罩。
- 固定设计尺寸但受 viewport 约束。
- body 内部滚动。
- header / footer 保持可用。
- danger action 使用红色语义。
- rerender / validation / realtime 后尽量保留滚动位置。
- 不在表格行内做复杂编辑。
- 不在主页面展开大量复杂表单。

---

## 9. WebAdmin 写操作规则

没有完整后端支持的写操作必须 disabled / unavailable。
不能因为 UI 有按钮就新增 API 或发送写请求。

高风险写操作必须具备：

- 权限检查。
- CSRF / same-origin。
- edit lock。
- expectedFingerprint。
- validation。
- `WebAdminWriteResult` 或项目等价写结果。
- audit。
- realtime event。
- 错误处理和中文提示。
- 保存失败不清空输入。

写操作必须保持世界隔离。世界级配置应优先放在世界存档下的 `tzz/webadmin/` 或当前项目约定位置，不要随意写入全局 config。

---

## 10. Realtime / Silent Refresh 规则

WebAdmin realtime / silent refresh 必须非扰动。

禁止：

- 整页 reload。
- 白屏 / 黑屏闪烁。
- 重置滚动位置。
- 重置筛选条件。
- 重置输入内容。
- 重置分页。
- 关闭已打开 modal。
- 重建整个 app shell。
- 用户正在编辑时静默覆盖草稿。

允许：

- route-level silent refresh。
- stale-while-revalidate。
- visible row detail cache。
- document hidden 时暂停刷新或标记 dirty。
- visible 后静默刷新一次。
- 保存成功后局部刷新详情。

---

## 11. Condition / Runtime Gate 规则

当任务涉及 ConditionEngine 或 runtime gate 时，必须遵守：

- 未配置 conditionGroupId 时，旧逻辑必须完全不变。
- 未配置 conditionGroupId 时，不读取 condition group store，不构造 EvaluationContext，不 evaluate。
- 配置 conditionGroupId 后，只能作为外层 gate。
- gate true 后进入旧逻辑原流程。
- gate false 后不进入旧副作用逻辑。
- gate false 不 emit signal、不 consume item、不执行 action、不移动物品、不写状态。
- missing / disabled / invalid / incompatible group 的处理必须安全且中文可读。
- 不兼容的 condition group 不能出现在可用列表。
- 后端也必须拒绝 incompatible binding，不能只靠前端隐藏。
- available list / compatibility profile / runtime builder 必须保持一致。

不得为了接入 condition 改写旧业务语义，例如 itemSubmit consume、container change detection、SignalBridge emit、RegionController enter/exit/stay、ActionEngine action order 等。

---

## 12. 文档与帮助中心方向

系统越来越复杂，后续需要 WebAdmin 内置文档 / 帮助中心 / 知识库。

每个复杂阶段都应尽量补充：

- context 文档。
- capability matrix。
- 用户可见 help text。
- 字段中文说明。
- 示例配置。
- 常见错误原因。
- Doctor / debugger 可跳转的解释材料。

未来文档中心应覆盖：

- SignalBridge / channel。
- SignalEmitter / SignalReceiver / ActionRelay / VirtualBlockDevice。
- SignalListener。
- RegionController。
- ActionEngine。
- ConditionEngine / condition group。
- StateVariable。
- item / inventory / container conditions。
- Region / Signal / LogicChain conditions。
- runtime gate。
- WebAdmin 编辑流程。
- 常见配置模板。
- 常见故障排查。
- 简单版本历史 / rollback 使用方式。

不要把文档中心硬塞进不相关阶段；可以作为独立阶段推进。

---

## 13. 图标规则

WebAdmin 自定义图标应使用项目当前认可的图标体系。

偏好：

- 2D。
- 扁平化。
- 简约。
- 科技风。
- 深色后台适配。
- 透明背景。
- UI 圆底、hover、glow 由 CSS 控制。

禁止：

- emoji。
- 字母占位。
- 纯字符图标。
- 旧图标简单染色。
- 3D 立体风。
- 把 Minecraft 方块风用于 WebAdmin 自定义图标。
- 用生成模型重画 Minecraft 原版方块 / 物品材质。

Minecraft 原版方块 / 物品图标必须使用原版材质资源。

---

## 14. 测试与验证命令

常规修改后通常需要运行：

```powershell
cd tools\tzz-test-mcp
npm run build
npm test
```

回到仓库根目录：

```powershell
.\gradlew.bat clean build
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

如果当前阶段未涉及 MCP，是否运行 MCP 命令以用户 / ChatGPT 当前提示词为准。
如果 Gradle guard 因并行测试编译目录竞争失败，顺序重跑；仍失败则停止报告。

如果涉及前端脚本生成，应尽量执行：

- JS syntax check。
- route / handler / marker guard。
- render smoke test，如可行。
- 关键 UI 文案 / CSS marker 检查。

不要启动 Minecraft、跑 MCP scenario 或生成截图矩阵，除非当前任务明确要求。

---

## 15. 测试文档规则

WebAdmin / API / runtime 阶段需要人工验收时，测试文档应由 ChatGPT 提供或由当前任务明确要求。

测试文档应包含：

- 前置条件。
- 数据准备步骤。
- 精确命令或 UI 步骤。
- 浏览器验收步骤。
- 游戏内验收步骤，如涉及 runtime。
- 失败判定。
- disabled / unavailable 边界。
- 响应式检查。
- console / network 检查。
- checkpoint 通过标准。

不要使用裸父命令作为测试命令，例如：

```text
/tzz signal
/tzz regionctl
```

命令必须来自当前代码注册，或者明确要求用 TAB 补全确认当前精确命令。不要根据历史记忆猜命令。

---

## 16. Git 规则

除非用户明确要求：

- 不 commit。
- 不 push。
- 不 merge。
- 不 tag。

checkpoint commit 规则：

1. 用户确认可以 checkpoint 后才执行。
2. checkpoint 前必须运行当前提示词要求的验证命令。
3. 禁止 `git add .`。
4. 只能显式暂存本阶段相关文件。
5. 不提交 `logs/`。
6. 不提交 `reports/mcp/`、screenshots、node_modules、build、run、`.gradle/`。
7. 报告 commit hash、message、验证结果、git status。
8. checkpoint 只 push feature 分支，不 merge、不 tag、不 push master，除非用户明确要求。

merge/tag 规则：

1. 必须先核验 feature HEAD 和 origin/master。
2. feature 最终验证。
3. no-ff merge 到 master。
4. master 合并后验证。
5. push master。
6. create annotated tag。
7. push tag。
8. 最终核验 master/tag。
9. 任一步失败都停止，不继续后续步骤。
10. 失败发布状态不得强行 push；必要时回到 feature 修复并 reset 本地 master。

---

## 17. Git 卫生

长期规则：

- `logs/` 如未跟踪，默认不要处理、不要删除、不要提交。
- 不提交 `reports/mcp/`。
- 不提交 screenshots。
- 不提交 node_modules。
- 不提交 build。
- 不提交 run。
- 不提交 `.gradle/`。
- 不提交临时生成文件，除非当前任务明确要求纳入版本控制。

每次报告必须说明：

```text
当前分支
HEAD
git status --short --branch
验证结果
是否 commit / push / merge / tag
logs/ 是否未处理
是否有无关文件
```

---

## 18. 旧文档使用说明

旧文档可以作为历史背景，但不代表当前阶段真实范围。

如果旧文档与以下来源冲突，以后者为准：

1. 用户当前消息。
2. ChatGPT 当前阶段提示词。
3. 最新 `docs/*_CURRENT_CONTEXT.md`。
4. 当前代码。
5. 当前测试结果。

不要因为旧文档里写了某个版本、阶段或禁止项，就覆盖当前任务的明确要求。
如果不确定，停止并询问用户或报告冲突。
