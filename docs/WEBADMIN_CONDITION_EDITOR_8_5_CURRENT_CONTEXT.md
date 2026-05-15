# 8.5 WebAdmin Condition Editor Current Context

阶段名称：8.5 WebAdmin Condition Editor / WebAdmin 条件组编辑器 MVP。

当前稳定基线：`v1.50.0-condition-region-signal-logic-chain`。

8.5 的目标是把 8.0-8.4 已完成的 ConditionEngine 只读判断能力接入 WebAdmin，提供条件类型目录、条件组持久化、结构化条件树编辑、校验和模拟评估。8.5 只保存和测试 Condition Group 配置，不把条件组挂到任何运行时路径。

## 本阶段已实现范围

- Condition Type Catalog：
  - `GET /api/webadmin/condition-types`
  - 从 `ConditionRegistry.defaultRegistry()` 输出所有已注册 condition type。
  - 输出中文显示名、中文描述、中文字段名、字段类型、必填标记、operator / enum 选项和所属 8.x 能力阶段。
  - 目录只读，不暴露 Java class，不允许修改 condition type。

- Condition Group world-scoped store：
  - 路径：`<world-save-root>/tzz/webadmin/condition_groups.json`。
  - 通过 `WebAdminStoragePaths.resolve(server).directory().resolve("condition_groups.json")` 定位。
  - 数据版本：`version = 1`。
  - group record 包含 `id`、`displayName`、`note`、`iconKey`、`enabled`、`tags`、`groupDefinition`、`createdAt`、`updatedAt`、`updatedBy`、`version`、`fingerprint`。
  - world-scoped，不读写全局 config/tzz 作为主存储。

- Condition Group service / API：
  - `GET /api/webadmin/condition-groups`
  - `GET /api/webadmin/condition-groups/{id}`
  - `POST /api/webadmin/condition-groups`
  - `PATCH /api/webadmin/condition-groups/{id}`
  - `POST /api/webadmin/condition-groups/{id}/delete`
  - `POST /api/webadmin/condition-groups/{id}/validate`
  - `POST /api/webadmin/condition-groups/{id}/preview`
  - VIEWER 可读，EDITOR / OWNER 可写。
  - 写入走 CSRF / same-origin、edit lock、expectedFingerprint、`WebAdminWriteResult`、audit 和 realtime。
  - same-origin 由现有 WebAdmin 写链路判定；无 Origin/Referer 的本机同源请求按既有策略处理，不新增旁路。
  - 删除不要求用户输入 ID/name。

- WebAdmin UI：
  - 新增导航入口：`条件组`。
  - `#/condition-groups`：条件系统总览、条件组列表、条件类型目录。
  - `#/condition-groups/{id}`：条件组详情、条件树、校验结果、模拟评估。
  - 新建 / 编辑使用结构化表单和节点卡片，属于 structured editor，不使用 raw JSON 作为主编辑入口。
  - 二次返修后节点主区域只保留紧凑卡片列表；点击节点 / 子组卡片打开独立编辑 modal，不再使用右侧固定编辑面板。
  - 节点编辑 modal 内部滚动，condition type change、validation refresh 和局部 rerender 保留 modal 内滚动位置。
  - 新建条件组是本地草稿，UI 明确显示“新建草稿，保存时自动获取编辑锁并创建条件组”；已有条件组编辑仍必须先持有 edit lock。
  - 不提供 raw JSON editor 作为主入口。
  - 条件节点支持选择 condition type、根据 catalog field metadata 渲染字段、启用/停用、添加/删除节点、嵌套 group、AND / OR / NOT。
  - condition type 保存后必须按真实 type 回显；unknown type 或缺必填字段必须中文 validation fail，不能静默变成 `always_true`。
  - 二次返修后，condition type 选择器使用搜索 + 分类 + 可滚动列表；中文名称和说明优先，type id 作为副文本；不再使用原生 `<select>` / datalist / 旧网格按钮路径。
  - 节点 modal 维护独立 editingDraft / editingPath；保存 payload 统一由前端 `conditionGroupSavePayload` 构造，避免可见选择与 draft 状态分离。
  - 字段编辑按 catalog metadata 渲染：operator / scope / targetMode / gamemode / sourceType 等固定项使用选择控件，boolean 使用 true / false 控件，integer/count/slot 使用数字输入，无法枚举的 key 提供中文说明和示例 placeholder。
  - 后端保存 / store normalize 路径不再将缺失的 `groupDefinition` fallback 成 `always_true`；`always_true` 只允许作为前端新建空节点的显式初始模板。
  - 主文案为中文，英文 type id 作为技术副文本。

- 资源一致性：
- 8.5 P0 二次返修后 asset version 更新为 `8.5-condition-editor-p0-3`。
  - WebAdmin HTML / CSS / JS 响应带 `Cache-Control: no-store, max-age=0`，避免浏览器继续加载旧 `app.js`。
  - 页面带 `tzz-webadmin-asset-version` meta 与 `data-asset-version`，便于人工验收确认加载的是新资源。

- Preview / Simulation evaluate：
  - 预览只读取手动构造的模拟 `ConditionEvaluationContext`。
  - 当前 MVP 支持基础 context、player snapshot 和手动输入的 GLOBAL / PLAYER state variable snapshot。
  - state variable preview snapshot 只来自手动模拟输入，不读取 live store。
  - preview 会先执行 condition validation，未通过校验时不进入 evaluation。
  - item / inventory / container snapshot 与 region / signal / logic chain snapshot 的完整结构化模拟输入 deferred。
  - preview 不写 store、不 emit signal、不执行 action、不查询 live world/runtime service。

## 明确不做

8.5 不做：

- 不接入 VBD runtime。
- 不接入 interactionItem runtime。
- 不接入 itemSubmit runtime。
- 不接入 container runtime。
- 不接入 SignalListener runtime。
- 不接入 RegionController runtime。
- 不接入 ActionRelay runtime。
- 不把 condition group 挂到任何设备、监听器、区域、Action 或 itemSubmit 上。
- 不做 Action runtime condition gate。
- 不做 GameController / MissionSystem / PhaseController。
- 不做具体逃走中任务、游戏开始 / 结束 / 结算。
- 不做 Scratch 拖拽完整版。
- 不做全局逻辑图编辑器。
- 不做 raw JSON editor 作为主入口。
- 不做任意 NBT path 或通用脚本表达式。
- 不查询 live world、live player list、live inventory / container、live RegionController、live SignalBridge、live Logic Chain Viewer service。
- 不新增 MCP tool。
- 不跑 MCP scenario。
- 不生成截图。
- 不启动 Minecraft。

## 权限 / 写入边界

- Catalog / list / detail：VIEWER 可读。
- create / update / delete：EDITOR / OWNER 可写。
- 写 API 必须满足：
  - `X-TZZ-WebAdmin-CSRF`
  - same-origin / referer 校验
  - edit lock：targetType = `condition_group`
  - `expectedFingerprint`
  - `WebAdminWriteResult`
  - audit
  - realtime event：`condition_group_changed`

## Validation

覆盖：

- 条件组 ID 为空。
- 条件组名称为空。
- duplicate id。
- `groupDefinition` 缺失。
- empty group。
- NOT children count 不合法。
- unknown condition type。
- missing required field。
- invalid operator / invalid value。
- maxDepth / maxNodes 由 ConditionEvaluator 保持。
- expectedFingerprint mismatch。
- edit lock missing / conflict。
- permission denied。
- CSRF missing / invalid。
- delete not found。

所有 validation error 面向 WebAdmin 返回中文消息。

## Deferred

- 8.5 暂不提供完整可视化条件图编辑器。
- 8.5 暂不提供 item / inventory / container snapshot 的完整 simulation form。
- 8.5 暂不提供 region / signal / logic chain snapshot 的完整 simulation form。
- 8.6 / 8.7 再讨论把 Condition Group 挂到 VBD、SignalListener、RegionController、ActionRelay、Action、itemSubmit 等 runtime。

## 测试与验收

基础验证：

- `cd tools\tzz-test-mcp && npm run build && npm test`
- `.\gradlew.bat clean build`
- `.\gradlew.bat stabilizationGuardTest --rerun-tasks`
- `.\gradlew.bat localTestMcpGuardTest --rerun-tasks`
- `git diff --check`

本阶段不跑 MCP scenario、不生成截图、不启动 Minecraft。真实浏览器验收由用户执行。
