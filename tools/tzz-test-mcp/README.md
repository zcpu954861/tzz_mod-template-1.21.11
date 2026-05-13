# TZZ 本地测试 MCP

## 中文快速开始

这个 MCP server 只面向本地 Codex CLI / IDE 扩展使用，用来在本机执行受控测试、打开本机 WebAdmin、截图、读取浏览器错误并写测试报告。它不保证 Codex 云端环境可以访问你的本机 Minecraft / WebAdmin。

先准备本机环境：

1. 安装 Node.js LTS。
2. 在仓库根目录打开 Windows PowerShell。
3. 安装依赖并编译：

```powershell
cd tools\tzz-test-mcp
npm install
npm run build
```

启动 MCP server：

```powershell
cd tools\tzz-test-mcp
npm run start
```

配置本地 Codex MCP 时，stdio 配置示例：

```json
{
  "name": "tzz-test-mcp",
  "command": "node",
  "args": [
    "E:\\minecraftserver\\fabricmod\\tzz_mod-template-1.21.11\\tools\\tzz-test-mcp\\dist\\index.js"
  ],
  "cwd": "E:\\minecraftserver\\fabricmod\\tzz_mod-template-1.21.11\\tools\\tzz-test-mcp",
  "env": {
    "TZZ_REPO_ROOT": "E:\\minecraftserver\\fabricmod\\tzz_mod-template-1.21.11",
    "TZZ_WEBADMIN_URL": "http://127.0.0.1:18080/",
    "TZZ_WEBADMIN_USERNAME": "your-local-user",
    "TZZ_WEBADMIN_PASSWORD": "your-local-password",
    "TZZ_WEBADMIN_TEST_USERNAME": "mcp_test",
    "TZZ_WEBADMIN_TEST_PASSWORD": "your-local-test-password",
    "TZZ_TESTBRIDGE_ENABLED": "true",
    "TZZ_TESTBRIDGE_TOKEN": "your-local-random-token",
    "TZZ_TEST_WORLD_NAME": "TZZ_MCP_TEST_WORLD"
  }
}
```

也可以在 PowerShell 当前会话里设置 WebAdmin 登录信息：

```powershell
$env:TZZ_WEBADMIN_USERNAME = "your-local-user"
$env:TZZ_WEBADMIN_PASSWORD = "your-local-password"
```

不要把真实密码、token、cookie、CSRF token 写进仓库、配置示例或提交记录。需要登录 WebAdmin 时，只使用环境变量或本机私有配置。

## 作用范围

这个工具包可以：

- 运行白名单内的 Gradle 验证 preset。
- 返回只读 git 状态摘要。
- 读取白名单内的 build / test / WebAdmin 日志尾部。
- 在本机安装 Playwright 后打开本地 WebAdmin。
- 将截图保存到 `reports/mcp/screenshots`。
- 收集浏览器 Console error、page error、失败请求和 400+ 响应。
- 将 Markdown 测试报告写入 `reports/mcp`。
- 通过固定 Gradle `runClient` preset 启动 Minecraft dev client。
- 等待本机 WebAdmin URL 可访问。
- 通过已登录的 localhost WebAdmin 会话修改当前用户密码或 OWNER 设置指定用户密码。
- 在显式启用 dev-only TestBridge 后，执行受控 Minecraft 测试操作，例如读取玩家、放置测试区块、设置玩家物品、模拟右键方块、读取设备 / Signal history / Doctor issues。

这个工具包不会：

- 暴露任意 shell 命令。
- 暴露 `git add` / `git commit` / `git push` / `git merge` / `git tag` / `git reset` / `git checkout` / `git switch`。
- 读取任意文件系统路径。
- 删除或移动文件。
- 通过 Playwright 访问公网目标。
- 在仓库里保存密码。
- 自动化 Minecraft GUI 的鼠标键盘坐标操作。
- 自动选择 / 进入 Minecraft 世界。

## 安装

在工具目录内安装依赖：

```powershell
cd tools\tzz-test-mcp
npm install
npm run build
npm test
```

Playwright 是运行时可选依赖。如果没有安装 Playwright，非浏览器工具仍可用，浏览器工具会返回结构化的 `TOOL_UNAVAILABLE` 错误。

如果要启用 WebAdmin 浏览器自动化，请在本机安装 Playwright Chromium：

```powershell
cd tools\tzz-test-mcp
npm install
npx playwright install chromium
```

## 启动

```powershell
cd tools\tzz-test-mcp
npm run start
```

MCP 使用 stdio。不要把普通日志输出到 stdout；stdout 是 MCP JSON-RPC 通道。

正常情况下，`npm run start` 启动后不会输出普通日志，看起来像是在等待输入。这是正确行为：stdio MCP server 需要由本地 Codex MCP client 发送 JSON-RPC 请求后才会返回 MCP 响应。

## 配置

默认配置示例在 `config.example.json`：

```json
{
  "repoRoot": ".",
  "webAdminUrl": "http://127.0.0.1:18080/",
  "reportsDir": "reports/mcp",
  "screenshotsDir": "reports/mcp/screenshots",
  "allowedHosts": [
    "127.0.0.1",
    "localhost",
    "::1"
  ],
  "gradleTimeoutSeconds": 900,
  "playwrightHeadless": true
}
```

如需使用私有配置文件，可以复制 `config.example.json` 到仓库外或本机私有路径，然后设置：

```powershell
$env:TZZ_TEST_MCP_CONFIG = "E:\path\to\your\config.json"
```

环境变量会覆盖配置文件：

- `TZZ_REPO_ROOT`
- `TZZ_WEBADMIN_URL`
- `TZZ_MCP_REPORTS_DIR`
- `TZZ_MCP_SCREENSHOTS_DIR`
- `TZZ_MCP_ALLOWED_HOSTS`
- `TZZ_MCP_GRADLE_TIMEOUT_SECONDS`
- `TZZ_MCP_PLAYWRIGHT_HEADLESS`
- `TZZ_WEBADMIN_USERNAME`
- `TZZ_WEBADMIN_PASSWORD`
- `TZZ_WEBADMIN_NEW_PASSWORD`
- `TZZ_WEBADMIN_TEST_USERNAME`
- `TZZ_WEBADMIN_TEST_PASSWORD`
- `TZZ_TESTBRIDGE_ENABLED`
- `TZZ_TESTBRIDGE_TOKEN`
- `TZZ_TESTBRIDGE_URL`
- `TZZ_TEST_WORLD_NAME`

## 工具清单

- `health.check`：返回本地 MCP server 和配置摘要。
- `gradle.run`：只运行白名单 Gradle preset。
- `logs.tail`：只读取白名单日志 / 报告尾部。
- `webadmin.open`：用 Playwright 打开本地 WebAdmin。
- `webadmin.login`：用显式参数或环境变量登录，并通过 `/api/auth/me` 验证真实认证状态。
- `webadmin.change_password`：通过已登录 WebAdmin 会话修改当前用户密码。
- `webadmin.owner_set_password`：通过 OWNER 会话设置指定 WebAdmin 用户密码。
- `webadmin.goto`：跳转到 WebAdmin hash route。
- `webadmin.set_viewport`：设置当前 Playwright WebAdmin page CSS viewport 和可选 `deviceScaleFactor`，用于模拟 DPI / 系统缩放。
- `webadmin.screenshot`：保存截图到 `reports/mcp/screenshots`。
- `webadmin.responsive_screenshot`：对一个 WebAdmin route + responsive profile 截图并返回 CSS viewport、deviceScaleFactor、预期物理尺寸、实际截图尺寸和诊断。
- `webadmin.responsive_matrix`：对多个 WebAdmin route / responsive profile 生成截图矩阵和报告。
- `webadmin.console_errors`：返回 Console error、page error、失败请求和错误响应。
- `webadmin.close`：关闭当前 Playwright page / browser / context；未打开时幂等返回。
- `webadmin.click`：点击当前本地 WebAdmin 页面内可见且启用的 selector。
- `webadmin.fill`：填写当前本地 WebAdmin 页面内可见且启用的 selector。
- `webadmin.text`：读取当前本地 WebAdmin 页面内 selector 的文本。
- `report.write`：写 Markdown 报告到 `reports/mcp`。
- `repo.status`：只读返回 git status / log / diff 摘要。
- `minecraft.start_client`：启动固定 Gradle `runClient` dev client preset。
- `minecraft.status`：查看 MCP 管理的 dev client 进程、日志尾部和 WebAdmin ready 状态。
- `minecraft.wait_webadmin`：等待本机 WebAdmin URL 可访问。
- `minecraft.stop`：只停止当前 MCP server 启动的 managed process。
- `minecraft.testbridge_status`：读取 TestBridge 启用 / ready / world / player / 安全状态。
- `minecraft.wait_world`：等待 TestBridge 报告 server/world ready。
- `minecraft.players`：读取在线玩家摘要。
- `minecraft.command`：执行 TestBridge allowlist 内的 Minecraft 命令；危险命令会被拒绝。
- `minecraft.set_block`：在受限测试区域内放置方块。
- `minecraft.clear_area`：在受限测试区域内清理小范围方块。
- `minecraft.prepare_test_area`：结构化清理并铺设受限测试区域。
- `minecraft.prepare_test_player`：结构化准备指定在线玩家。
- `minecraft.prepare_test_world`：幂等准备测试世界 / 区域 / 玩家。
- `minecraft.give_item`：给指定在线玩家普通物品。
- `minecraft.clear_inventory`：清理指定在线玩家背包。
- `minecraft.set_main_hand`：设置指定在线玩家主手物品。
- `minecraft.use_block`：通过生产 `UseBlockCallback` 路径模拟玩家右键方块。
- `minecraft.inspect_device`：只读检查设备状态。
- `minecraft.signal_history`：只读读取 Signal history。
- `minecraft.doctor_issues`：只读读取 Doctor issues。
- `minecraft.wait_testbridge`：等待 TestBridge ready。

## WebAdmin 密码 / 测试账号

为了让 MCP 自动化稳定登录，WebAdmin 现在提供页面入口和两个基础 API：

- 当前登录用户可以在 WebAdmin 右上角用户区域点击“修改密码”。
- 修改密码弹窗要求输入当前密码、新密码和确认新密码。
- 修改成功后当前 session 继续有效；下次登录使用新密码。

- 当前用户改密：`POST /api/webadmin/users/me/password`
- OWNER 设置指定用户密码：`POST /api/webadmin/users/{username}/password-reset`

这两个接口都要求：

- 已登录 WebAdmin。
- CSRF / same-origin 校验。
- 复用现有 PBKDF2 password hash。
- 不返回、不记录、不保存明文密码。
- 记录 WebAdmin 用户审计。

MCP 对应工具：

- `webadmin.change_password`
- `webadmin.owner_set_password`

MCP 自动化建议使用固定测试账号。首次创建测试账号仍走现有 WebAdmin 用户管理 / `/tzz webadmin user create ...` 流程；首次重置或后续维护测试账号密码，可以由 OWNER 使用 WebAdmin/API/MCP helper 完成。普通用户日常改自己的密码，直接在 WebAdmin 页面使用“修改密码”。

典型本地流程：

1. 先用现有 `/tzz webadmin user create ...` 或用户管理流程创建测试账号，例如 `mcp_test`。
2. 用 OWNER 登录 WebAdmin。
3. 调用 `webadmin.owner_set_password`，目标用户为 `mcp_test`，密码来自 `TZZ_WEBADMIN_TEST_PASSWORD` 或工具参数。
4. 后续 MCP 自动化使用 `TZZ_WEBADMIN_USERNAME=mcp_test` 和 `TZZ_WEBADMIN_PASSWORD=<测试密码>` 登录。

不要把 `TZZ_WEBADMIN_PASSWORD`、`TZZ_WEBADMIN_NEW_PASSWORD`、`TZZ_WEBADMIN_TEST_PASSWORD` 写进仓库、README、报告或截图。`config.example.json` 也不包含密码字段。

### WebAdmin 登录排查

`webadmin.login` 不只填写登录表单。它会等待登录页脚本绑定完成，真实点击“登录”按钮，观察 `/api/auth/login` 请求，然后调用 `/api/auth/me` 验证当前 session。只有真正登录成功时才返回 `ok=true`，并返回 `currentUser` / `currentRole`。

如果点击按钮没有触发登录请求，工具会依次尝试：

1. 在密码输入框按 `Enter`。
2. 调用登录表单的 `requestSubmit()`。

如果仍然没有观察到 `/api/auth/login`，会返回 `SUBMIT_NOT_TRIGGERED`，并附带不含密码的诊断字段，例如 `buttonFound`、`buttonVisible`、`buttonEnabled`、`clicked`、`loginRequestObserved`、`loginResponseStatus`、`authMeStatus`、`fallbackUsed` 和失败截图路径。

如果返回 `AUTH_FAILED`：

1. 检查 `TZZ_WEBADMIN_USERNAME` 是否设置。
2. 检查 `TZZ_WEBADMIN_PASSWORD` 是否是当前世界 WebAdmin 用户数据里的最新密码。
3. 检查 Minecraft / WebAdmin 是否已经启动。
4. 如果刚在 WebAdmin 页面修改过密码，需要同步更新 Codex App MCP 环境变量，然后重启 Codex App / MCP server。
5. 查看返回的 `loginRequestObserved` / `loginResponseStatus` / `authMeStatus`。
6. 可以调用 `webadmin.screenshot` 查看当前是否仍停留在登录页。

如果没有设置用户名或密码，`webadmin.login` 会返回 `CONFIG_ERROR`，并只报告 `usernameConfigured` / `passwordConfigured`，不会输出密码值。

`webadmin.goto` 也会检查认证状态。如果目标是 `#/dashboard` 等受保护 route，但当前仍在 `/login`，会返回 `AUTH_REQUIRED`，不会误报 route 已打开。

## Minecraft Dev Runtime Launcher

本阶段提供受控 dev runtime launcher，并提供 dev-only TestBridge Foundation。仍然不做 GUI 坐标点击。

可用工具：

- `minecraft.start_client`
- `minecraft.status`
- `minecraft.wait_webadmin`
- `minecraft.stop`

`minecraft.start_client` 默认固定执行：

```powershell
.\gradlew.bat --no-daemon runClient
```

非 Windows 下使用：

```bash
./gradlew --no-daemon runClient
```

如果要尽量自动进入既有测试世界，可调用：

```json
{
  "autoEnterWorld": true,
  "worldName": "TZZ_MCP_TEST_WORLD"
}
```

这会执行固定白名单 preset，并通过 Gradle `runClient --args=...` 传入 Minecraft 原生 quick play 参数：

```powershell
.\gradlew.bat --no-daemon runClient --args="--quickPlaySingleplayer TZZ_MCP_TEST_WORLD"
```

也可以在 MCP 环境变量中设置默认世界名：

```powershell
$env:TZZ_TEST_WORLD_NAME = "TZZ_MCP_TEST_WORLD"
```

`worldName` 是 `run/saves/<worldName>` 的存档目录名，不是世界显示标题。它只允许英文字母、数字、点、下划线、短横线；路径分隔符、`..`、空格、控制字符、绝对路径都会被拒绝。启动前会检查 `run/saves/<worldName>/level.dat`，如果不存在会返回 `NOT_FOUND`，不会误打开其它世界，也不会创建或删除世界。

限制：

- 只支持固定 `runClient` preset，不接收任意 Gradle 参数。
- auto-enter 只允许固定 `--quickPlaySingleplayer <worldName>`，不开放任意 runClient args。
- stdout / stderr 写到 `reports/mcp/runtime/*.log`，不写 MCP stdout。
- 如果当前 MCP session 已启动 dev client，再次调用只返回当前状态。
- `minecraft.stop` 只停止当前 MCP server session 自己启动的进程；不会扫描或杀掉任意 Java / Minecraft 进程。
- Windows 下会先请求 managed `runClient` 启动进程优雅退出；如果最初的 `cmd.exe` / Gradle wrapper pid 已经消失但 Java 子进程仍在运行，会执行固定的本机进程查询。查询只匹配当前 repo root、managed world name、启动时间之后的 `runClient` 进程标记（`gradle-wrapper.jar` / `devlaunchinjector.Main`），然后仅对这些候选 pid 使用固定 `taskkill.exe /pid <candidatePid> /t /f`，并短暂等待候选进程全部消失后再返回成功。工具不接受用户输入 pid，不杀任意 Java / Minecraft 进程，也不开放任意 shell。
- WebAdmin ready 检测只访问 `http://127.0.0.1:18080/`、`localhost` 或 `::1` 这类 loopback 地址。

注意：quick play 只能打开已经存在的测试世界。首次创建 `TZZ_MCP_TEST_WORLD` 仍需要用户在本地手动创建一次，或等待后续 dev-only 世界创建 helper。本工具不会点击 Minecraft 主菜单坐标。

## Minecraft TestBridge Foundation

TestBridge 是本地开发测试桥，只用于本机自动化测试，不用于生产。

### 启用方式

默认关闭。需要同时设置：

```powershell
$env:TZZ_TESTBRIDGE_ENABLED = "true"
$env:TZZ_TESTBRIDGE_TOKEN = "your-local-random-token"
```

Codex MCP 配置里也要给 `tools/tzz-test-mcp` 进程设置同样的 `TZZ_TESTBRIDGE_TOKEN`。MCP 调用 TestBridge 时会通过 HTTP header `X-TZZ-TestBridge-Token` 发送 token。不要把 token 写进仓库、报告、截图或日志。

如果 WebAdmin URL 不是默认值，可设置：

```powershell
$env:TZZ_TESTBRIDGE_URL = "http://127.0.0.1:18080/api/testbridge/"
```

`TZZ_TESTBRIDGE_URL` 仍必须是 `127.0.0.1` / `localhost` / `::1` 这类 loopback 地址。

### Server endpoints

- `GET /api/testbridge/status`
- `GET /api/testbridge/players`
- `POST /api/testbridge/command`
- `POST /api/testbridge/world/set-block`
- `POST /api/testbridge/world/clear-area`
- `POST /api/testbridge/world/prepare-area`
- `POST /api/testbridge/world/prepare-player`
- `POST /api/testbridge/world/prepare`
- `POST /api/testbridge/player/give`
- `POST /api/testbridge/player/clear-inventory`
- `POST /api/testbridge/player/set-main-hand`
- `POST /api/testbridge/player/use-block`
- `GET|POST /api/testbridge/device/inspect`
- `GET /api/testbridge/signal/history`
- `GET /api/testbridge/doctor/issues`

### 安全边界

- TestBridge default disabled。
- TestBridge loopback-only。
- TestBridge token required。
- No token logged。
- `minecraft.command` 有 allowlist / denylist；`stop`、`op`、`deop`、`ban`、`kick`、`whitelist`、`save-off`、`save-on`、`pardon`、`reload` 等危险命令会被拒绝。
- `minecraft.set_block` 只能在默认测试区域 `x=-128..128`、`y=-64..320`、`z=-128..128` 内操作。
- `minecraft.clear_area` 也有最大体积限制 `4096`。
- `minecraft.prepare_test_area` 复用同样的测试区域边界和 `4096` 最大体积限制，不会强制加载区块。
- `minecraft.prepare_test_player` 只作用于指定在线玩家。
- `minecraft.prepare_test_world` 是幂等组合工具，不删除世界，不影响测试区域外内容。
- `minecraft.give_item` 有数量限制，不支持 raw NBT / components 路径编辑。
- `minecraft.clear_inventory` 只作用于指定在线玩家。
- `minecraft.inspect_device`、`minecraft.signal_history`、`minecraft.doctor_issues` 是只读。
- `minecraft.use_block` 复用生产 `UseBlockCallback`，用于测试 VBD 右键交互时走真实 handler 链路。

### 本轮仍不做

- 不自动点击 Minecraft 主菜单坐标。
- 不做 OS 级鼠标键盘操作。
- 不做图像识别点击。
- 不做 P3b / 7.10 游戏内 GUI 语义操作。
- 不创建或删除 Minecraft 世界；auto-enter 只打开已存在的测试世界。

## Auto Enter Test World / Prepare Tools

推荐本地流程：

1. 确认本地已存在 `run/saves/TZZ_MCP_TEST_WORLD/level.dat`。
2. 在 Codex App MCP 环境变量中设置 `TZZ_TEST_WORLD_NAME=TZZ_MCP_TEST_WORLD`。
3. 调用 `minecraft.start_client`，参数 `autoEnterWorld=true`。
4. 调用 `minecraft.wait_webadmin`。
5. 调用 `minecraft.wait_world`，需要玩家在线时可传 `requirePlayer=true`。
6. 调用 `minecraft.wait_testbridge`。
7. 调用 `minecraft.prepare_test_world`，可传 `player` 来同时清理背包、清空副手并传送到测试区。

`minecraft.prepare_test_area` 默认清理一个小的已加载测试空间并铺设 `minecraft:stone` 地面；自定义 `min/max` 仍必须位于 TestBridge 测试区域内，且体积不能超过 `4096`。`minecraft.prepare_test_player` 默认只影响指定玩家，不影响其它玩家。

这些准备工具都是结构化 TestBridge endpoint，不依赖 `minecraft.command setblock/give/clear/tp` 拼接测试准备步骤。

## Minecraft GUI Operation Abstraction Foundation

Step 4 增加受控的 Minecraft GUI 语义操作工具，用来操作已经打开的 WebAdmin 测试 GUI。它不点击屏幕坐标，不使用 OS 鼠标键盘控制，也不支持任意 Minecraft GUI。

当前支持的 GUI：

- `container_template`：7.9 P3b 容器内容变化模板 GUI。
- `single_item_submit`：7.10 单物品 itemSubmit 模板 GUI。

新增 MCP 工具：

- `minecraft.gui_current`：读取指定玩家当前 GUI 类型、标题、sessionId、deviceId、dirty/session 状态。
- `minecraft.gui_slots`：读取受支持 GUI 的模板槽状态。
- `minecraft.gui_put_item`：把 `itemId/count` 放入 ghost/template 槽，不修改真实玩家物品。
- `minecraft.gui_clear_slot`：清空模板槽，不影响真实背包。
- `minecraft.gui_set_count`：设置模板数量，沿用 GUI 现有 clamp 规则。
- `minecraft.gui_save`：走当前 GUI 的既有 session 保存路径。
- `minecraft.gui_cancel`：走当前 GUI 的既有取消路径。
- `minecraft.client_screenshot`：请求 Minecraft 客户端自己保存当前 framebuffer 截图到 `reports/mcp/screenshots`，不是 OS 截屏，不点击坐标。
- `minecraft.client_set_window_size`：通过 client payload 设置 Minecraft 客户端窗口尺寸，不使用 OS 鼠标拖拽。
- `minecraft.client_set_gui_scale`：通过 client payload 设置或恢复 Minecraft GUI scale，不写 `options.txt`。
- `minecraft.client_screenshot_matrix`：按窗口尺寸 / GUI scale 生成 Minecraft 客户端截图矩阵和报告。
- `scenario.list`：列出内置场景 smoke。
- `scenario.run`：运行一个固定白名单内的本地测试场景。
- `scenario.report`：读取最近的场景报告摘要。
- `scenario.cleanup`：关闭当前 MCP session 管理的测试客户端和 WebAdmin 页面，不删除本地输出。

示例：

```json
{"tool":"minecraft.gui_current","arguments":{"player":"Steve"}}
{"tool":"minecraft.gui_slots","arguments":{"player":"Steve"}}
{"tool":"minecraft.gui_put_item","arguments":{"player":"Steve","target":"single_item_submit","itemId":"minecraft:diamond","count":3}}
{"tool":"minecraft.gui_set_count","arguments":{"player":"Steve","target":"single_item_submit","count":3}}
{"tool":"minecraft.gui_save","arguments":{"player":"Steve"}}
{"tool":"minecraft.client_screenshot","arguments":{"player":"Steve","name":"single-item-submit-compact-layout"}}
```

容器模板 GUI 使用 `slot` 或 `slotIndex` 指定模板槽：

```json
{"tool":"minecraft.gui_put_item","arguments":{"player":"Steve","target":"container_template","slot":0,"itemId":"minecraft:diamond","count":1}}
```

安全边界：

- GUI 工具仍走 `/api/testbridge/gui/*`，必须 loopback + `X-TZZ-TestBridge-Token`。
- TestBridge token 不会发给客户端 payload。
- 只对当前打开的受支持 GUI 生效；其它屏幕返回 `UNSUPPORTED_GUI`，没有屏幕返回 `GUI_NOT_OPEN`。
- `gui_put_item` 只写 GUI draft 的 ghost/template 数据，不改真实玩家背包，不改真实世界容器。
- `gui_save` / `gui_cancel` 复用 GUI 已有保存/取消路径，不直接写 `SignalDeviceData` JSON。
- 本阶段不自动打开 P3b / 7.10 GUI；请先通过 WebAdmin 或现有 session 入口打开目标 GUI，再调用这些工具。

### Minecraft 客户端截图

`minecraft.client_screenshot` 用于游戏内 GUI / 视觉验收。它走 TestBridge 的 loopback + token HTTP endpoint，再通过 nonce-bound client payload 让目标 Minecraft 客户端调用原版客户端截图 API 捕获当前 framebuffer。它不是 OS 截屏，不截其它窗口，不使用鼠标键盘坐标点击，也不做图像识别点击。

示例：打开 7.10 single itemSubmit GUI 后截图：

```json
{"tool":"minecraft.client_screenshot","arguments":{"player":"Steve","name":"single-item-submit-compact-layout","timeoutMs":8000}}
```

示例：打开 7.9 container template GUI 后截图：

```json
{"tool":"minecraft.client_screenshot","arguments":{"player":"Steve","name":"container-template-compact-layout"}}
```

返回值会包含截图路径，例如：

```json
{
  "path": "E:\\minecraftserver\\fabricmod\\tzz_mod-template-1.21.11\\reports\\mcp\\screenshots\\2026-xx-xxTxx-xx-xx-single-item-submit-compact-layout.png",
  "screenType": "single_item_submit"
}
```

截图文件统一写入 `reports/mcp/screenshots`，文件名会安全化并加时间戳，不覆盖旧文件。报告可以引用返回的路径，但这些截图属于本地测试输出，提交前不要把 `reports/mcp` 或 screenshots 加入 git。

### 响应式 / 分辨率截图矩阵长期规则

以后新增或修改任何 Minecraft 游戏内 UI，都必须先跑截图矩阵，再交给用户人工验收。截图矩阵至少覆盖小分辨率、1080p、2K、4K 或当前环境可用的等价尺寸；游戏内 UI 还必须覆盖多个 GUI scale，至少覆盖 GUI scale 2 / 3 / 4。Codex 负责自动截图、整理报告和指出明显 warning，但最终 UI 是否通过必须由用户确认。用户确认前不得 checkpoint / merge。

以后新增或修改任何 WebAdmin WebUI，也必须先跑 WebAdmin responsive profile 截图矩阵。WebAdmin 截图矩阵必须区分：

- CSS viewport size：浏览器布局实际看到的 CSS 像素。
- physical screenshot size：截图 PNG 的物理像素。
- `deviceScaleFactor`：模拟 Windows / 浏览器 DPI 缩放。

不要只用 `3840x2160` CSS viewport 代表 4K。真实 4K 显示器常见缩放是 150% / 200%，浏览器 CSS viewport 通常小于 3840x2160。默认 WebAdmin profiles：

- `small_854x480`：CSS `854x480`，`deviceScaleFactor=1`。
- `hd_1280x720_100`：CSS `1280x720`，`deviceScaleFactor=1`。
- `fhd_1920x1080_100`：CSS `1920x1080`，`deviceScaleFactor=1`。
- `qhd_2560x1440_100`：CSS `2560x1440`，`deviceScaleFactor=1`。
- `uhd_4k_150_scaled`：CSS `2560x1440`，`deviceScaleFactor=1.5`，模拟 Windows 4K 150% 推荐缩放，预期物理截图 `3840x2160`。
- `uhd_4k_200_scaled`：CSS `1920x1080`，`deviceScaleFactor=2`，模拟 Windows 4K 200% 缩放，预期物理截图 `3840x2160`。
- `uhd_3840x2160_css_extreme`：CSS `3840x2160`，`deviceScaleFactor=1`，只是极端 CSS 工作区，不等价于普通 4K 缩放视觉。

WebAdmin 示例：

```json
{"tool":"webadmin.responsive_matrix","arguments":{"routes":["#/dashboard","#/devices","#/doctor"],"profiles":[{"name":"small_854x480","width":854,"height":480,"deviceScaleFactor":1},{"name":"hd_1280x720_100","width":1280,"height":720,"deviceScaleFactor":1},{"name":"fhd_1920x1080_100","width":1920,"height":1080,"deviceScaleFactor":1},{"name":"qhd_2560x1440_100","width":2560,"height":1440,"deviceScaleFactor":1},{"name":"uhd_4k_150_scaled","width":2560,"height":1440,"deviceScaleFactor":1.5,"screenshotScale":"device"},{"name":"uhd_4k_200_scaled","width":1920,"height":1080,"deviceScaleFactor":2,"screenshotScale":"device"},{"name":"uhd_3840x2160_css_extreme","width":3840,"height":2160,"deviceScaleFactor":1}],"name":"webadmin-ui-check"}}
```

WebAdmin responsive report 会列出 `profileName`、CSS viewport、`deviceScaleFactor`、预期物理截图尺寸、实际 PNG 尺寸、route 和截图路径。如果实际截图尺寸与预期物理尺寸不一致，报告会写 warning。4K 视觉验收必须至少包含一个 scaled profile，例如 `2560x1440 @ 1.5` 或 `1920x1080 @ 2`。

`webadmin.responsive_screenshot` 和 `webadmin.responsive_matrix` 默认截当前 viewport，而不是整页截图，这样实际 PNG 尺寸才能和 expected physical size 对齐。需要整页截图时可以显式传 `fullPage=true`，但整页截图高度可能大于 viewport 高度。

切换 `deviceScaleFactor` 时，Playwright 需要重建 browser context。工具会保留当前 WebAdmin `storageState`，并按 profile 优先顺序截图，减少重复登录和长矩阵超时。

Minecraft 示例：

```json
{"tool":"minecraft.client_screenshot_matrix","arguments":{"player":"Steve","targetGui":"single_item_submit","sizes":[{"width":854,"height":480},{"width":1920,"height":1080},{"width":2560,"height":1440},{"width":3840,"height":2160}],"guiScales":[2,3,4],"name":"single-item-submit-layout"}}
```

矩阵报告写入 `reports/mcp/responsive/*.md`，截图仍写入 `reports/mcp/screenshots/*.png`。本阶段不做自动图像识别，截图需要用户人工验收。用户确认前不得 checkpoint。若本机无法真实设置 4K Minecraft 窗口，工具会在报告中写 warning，不会假装成功。

## Scenario Test Orchestration Foundation

Step 5 增加一层场景编排工具，用来把已有安全 MCP / TestBridge / WebAdmin 工具组合成可重复的本地 smoke。场景工具不会执行任意 shell，不提供 git mutation，不访问外网，不点击 Minecraft GUI 坐标，也不直接写 `SignalDeviceData` JSON。

新增 MCP 工具：

- `scenario.list`
- `scenario.run`
- `scenario.report`
- `scenario.cleanup`
- `webadmin.close`

当前内置场景：

- `basic_environment`：启动或复用 dev client，`autoEnterWorld=true` 进入测试世界，等待 WebAdmin / TestBridge / world ready，执行 `minecraft.prepare_test_world`，登录 WebAdmin，打开 dashboard，收集 console errors / screenshot / Doctor issues，并写报告。
- `vbd_right_click`：准备 VBD + receiver，启用右键交互，调用 `minecraft.use_block`，断言 signal event / signal history，并写报告。
- `single_item_submit_basic`：准备 VBD + receiver，通过固定 WebAdmin session API 打开 7.10 single itemSubmit GUI，用 Step 4 GUI 工具放入 diamond、设置数量、保存，然后 inspect / use_block / signal history 验证。
- `container_template_basic`：准备容器 VBD + receiver，通过固定 WebAdmin session API 打开 7.9 container template GUI，用 Step 4 GUI 工具写入模板槽并保存，然后 inspect itemConditions / Doctor issues。

示例：

```json
{"tool":"scenario.list","arguments":{}}
{"tool":"scenario.run","arguments":{"scenarioName":"basic_environment","worldName":"TZZ_MCP_TEST_WORLD","keepClientOpen":false,"saveReport":true,"screenshot":true}}
{"tool":"scenario.cleanup","arguments":{"mode":"all"}}
```

报告输出：

- 场景报告写入 `reports/mcp/scenarios/*.md`。
- 场景截图仍写入 `reports/mcp/screenshots/*.png`。
- 场景失败时会停止当前场景步骤、记录失败步骤、写失败报告，并根据 `keepClientOpen` 决定是否关闭 managed Minecraft client。
- `scenario.cleanup` 只调用 `minecraft.stop` 和 `webadmin.close`，不删除 `logs/`、`reports/mcp`、截图、世界或仓库文件。

场景 runner 只允许调用内置安全工具白名单。`single_item_submit_basic` 和 `container_template_basic` 打开 GUI 时使用固定 WebAdmin API、CSRF、edit lock 和 expected fingerprint，不绕过已有保存校验；真正保存仍由 `minecraft.gui_save` 走现有 GUI/session save path。

## 安全边界

- 没有任意 shell。
- 没有 git mutation 工具；没有 `git push` / `git merge` / `git tag`。
- 没有外网访问；WebAdmin 自动化和 `minecraft.wait_webadmin` 只允许 `127.0.0.1` / `localhost` / `::1` 这类 loopback host。
- 即使配置了 `allowedHosts`，非 loopback host 也会被拒绝。
- `reports/mcp` 是报告输出目录。
- `reports/mcp/screenshots` 是截图输出目录。
- `reports/mcp/responsive` 是响应式 / 分辨率截图矩阵报告输出目录。
- `reports/mcp/runtime` 是 Minecraft dev runtime 日志输出目录。
- 工具输出会脱敏常见 password、cookie、token、CSRF、authorization、session 字段，包括 `X-TZZ-TestBridge-Token` / `TZZ_TESTBRIDGE_TOKEN`。
- 不处理、不删除、不提交仓库根目录下未跟踪的 `logs/`。
- 截图矩阵仍禁止 OS 截屏、Minecraft GUI 坐标点击、外网访问、任意 shell 和 git mutation。

## 最小 Smoke

不启动 Minecraft / WebAdmin 时：

1. `health.check`
2. `repo.status`
3. `gradle.run`，参数 `commandPreset = "stabilization_guard"`
4. `report.write` 写一个简短 Markdown 报告

本地 WebAdmin 已启动时：

1. `webadmin.open`
2. `webadmin.login`
3. `webadmin.goto`，参数 `hashRoute = "#/dashboard"`
4. `webadmin.console_errors`
5. `webadmin.screenshot`

Minecraft dev runtime 本地手工 smoke：

1. `minecraft.start_client`
2. 如已有 `TZZ_MCP_TEST_WORLD`，可传 `autoEnterWorld=true`；否则手动在 Minecraft dev client 中进入测试世界。
3. `minecraft.wait_webadmin`
4. `webadmin.open`
5. `webadmin.screenshot`
6. `minecraft.status`
7. `minecraft.stop`，如果 dev client 未退出则按提示手动关闭。

TestBridge 本地手工 smoke：

1. 设置 `TZZ_TESTBRIDGE_ENABLED=true` 和 `TZZ_TESTBRIDGE_TOKEN`。
2. `minecraft.start_client`，可传 `autoEnterWorld=true` 和 `worldName="TZZ_MCP_TEST_WORLD"`。
3. `minecraft.wait_world`
4. `minecraft.wait_webadmin`
5. `minecraft.testbridge_status`
6. `minecraft.players`
7. `minecraft.prepare_test_world`，可传 `player`。
8. 在测试区域内调用 `minecraft.set_block`
9. 调用 `minecraft.inspect_device` / `minecraft.signal_history` / `minecraft.doctor_issues` 做只读检查。

## 输出文件

报告和截图都是本地文件：

- `reports/mcp/*.md`
- `reports/mcp/screenshots/*.png`
- `reports/mcp/runtime/*.log`
- `reports/mcp/session.log`

这些输出用于本地测试审计。提交前应确认不要把本地敏感报告或截图误提交。
