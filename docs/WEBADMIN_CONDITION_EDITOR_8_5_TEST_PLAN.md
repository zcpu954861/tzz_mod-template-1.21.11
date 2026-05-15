# 8.5 WebAdmin Condition Editor 测试计划

本文件是 8.5 开发阶段的手动测试计划，不是最终验收报告。

## 前置条件

- 使用新测试世界或确认当前世界可安全写入 WebAdmin 配置。
- 当前分支为 `feature/web-admin-condition-editor`。
- `logs/`、`reports/mcp/`、screenshots、node_modules 不参与提交。
- 不启动 MCP scenario，不生成截图矩阵。

## 自动验证命令

在 `tools\tzz-test-mcp` 目录运行：

```powershell
npm run build
npm test
```

回到仓库根目录运行：

```powershell
.\gradlew.bat clean build
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

## 数据准备

1. 使用 WebAdmin OWNER 或 EDITOR 账号登录。
2. 打开 `条件组` 页面。
3. 新建一个条件组：
   - ID：`demo.start`
   - 名称：`示例开始条件`
   - Root operator：`AND`
   - 节点一：`信号频道匹配`，频道 `demo.start`
   - 节点二：`触发玩家存在`
4. 保存后重新进入详情页。
5. 不刷新浏览器，确认详情页立即回显保存后的 condition type 和字段值；`context_equals` 不得显示为 `always_true`。
6. 打开 DevTools Network，确认 `/assets/app.js` query string 包含 `8.5-condition-editor-p0-3`；Console 中可检查 `window.__TZZ_WEBADMIN_ASSET_VERSION` 为同一版本。

## 浏览器验收步骤

- 条件类型目录：
  - 能看到 8.0 core、8.1 player/context、8.2 state variable、8.3 item/inventory/container、8.4 region/signal/logic chain 条件类型。
  - 中文名称和中文描述为主文案，英文 type id 只作为副文本。

- 条件组列表：
  - 新建条件组显示在列表中。
  - 搜索、启用过滤不重置页面。
  - 点击整行进入详情。

- 条件组详情 / 编辑：
  - 新建条件组弹窗显示“新建草稿，保存时自动获取编辑锁并创建条件组”，不显示缺锁错误。
  - edit lock 状态在页面或 modal 内可见，不只是 toast。
  - 节点主区域是紧凑卡片列表，不显示完整字段表单，也不显示右侧固定编辑面板。
  - 点击子节点 / 子组卡片任意空白区域会打开独立“编辑节点 / 编辑条件组节点” modal。
  - 卡片右侧快捷按钮点击时只执行启用 / 删除等快捷动作，不应同时打开编辑 modal。
  - 编辑 modal 内部可滚动；选择条件类型、触发 validation 或局部刷新后不跳回 modal 顶部。
  - `context_equals`、`state_variable_bool_equals`、`inventory_contains_item` 等节点显示各自字段，不显示“无配置字段”。
  - 条件类型选择器是搜索 + 分类 + 可滚动列表，不是浏览器原生 condition type 下拉、datalist 或旧网格按钮。
  - 类型列表每行显示中文名称、中文描述、type id 和分类；任意时刻只有一个类型有单选高亮。
  - 选择 `context_equals` 后 modal 字段区立即显示真实 type 字段；Console 不出现 `Cannot read properties of null (reading 'groupDefinition')`。
  - operator / scope / targetMode / gamemode / sourceType / boolean expected / count / slot 等字段使用下拉、开关或数字输入，并带中文说明或示例 placeholder。
  - 保存后重新打开详情页，`context_equals`、`state_variable_bool_equals`、`inventory_contains_item`、`container_slot_item_matches`、`region_enabled`、`signal_event_count_compare`、`logic_chain_has_cycle` 不得变成 `always_true`。
  - unknown type / blank type / missing required config 必须中文 validation fail，不能静默保存为 `always_true`。
  - 修改字段后保存失败时输入内容不清空。
  - 有未保存修改时关闭节点 modal 弹出确认；点击“确认并退出”必须一次关闭，重新打开不残留上一次确认状态。
  - 删除条件组不要求输入 ID/name。
  - validation error 使用中文。

- 模拟评估：
  - channel 填 `demo.start` 且玩家 ID 非空时，预览通过。
  - channel 改成 `demo.stop` 时，预览失败并显示中文失败原因。
  - debug tree 显示每个节点结果、失败原因、evaluatedCount。
  - preview 不读取 live world、live player list、live inventory/container、RegionController、SignalBridge 或 Logic Chain Viewer service。

- Realtime / silent refresh：
  - 打开编辑 modal 后触发刷新，modal 不关闭，滚动位置和输入不清空。
  - 新建条件组 modal 在列表页 silent refresh 时不丢失 draft。
  - route-level silent refresh 不重置主页面滚动位置；保存后留在详情页时尽量保持当前位置。

## 响应式检查

- 1366px 宽度下列表、目录、详情编辑区域不重叠。
- 4K 200% 缩放下右侧预览面板不无限拉宽。
- 小窗口下条件节点卡片和预览表单可滚动，不压住保存按钮。
- 右侧“测试评估”面板输入框不横向溢出，长字段在面板内换行或滚动。

## Console / Network 检查

- 写 API 带 `X-TZZ-WebAdmin-CSRF`。
- 写 API 使用 same-origin 请求。
- 保存 / 删除返回 `WebAdminWriteResult`。
- preview / validate 不产生配置写入 realtime 事件。
- Network 中不存在 raw JSON editor 保存接口。
- 浏览器 Console 正常操作时没有 `Unexpected end of input`、`setValueAndClosePopup` 或 `Form submission canceled because the form is not connected` 红色错误。
- 刷新 `/app` 后不应继续加载旧 `7.5-step3-pages-batch2` app.js；HTML/CSS/JS 响应应带 no-store 缓存策略。

## 失败判定

- 任一写操作绕过 CSRF、permission、edit lock、expectedFingerprint、audit 或 realtime。
- preview 写入 store、发 signal、执行 action 或查询 live runtime service。
- 条件组被挂到 VBD / SignalListener / RegionController / ActionRelay / Action / itemSubmit。
- UI 以 raw JSON editor 作为主要编辑入口。
- 旧 WebAdmin 页面路由失效或 silent refresh 重置正在编辑的内容。
