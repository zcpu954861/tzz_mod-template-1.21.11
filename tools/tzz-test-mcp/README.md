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
    "TZZ_WEBADMIN_TEST_PASSWORD": "your-local-test-password"
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

这个工具包不会：

- 暴露任意 shell 命令。
- 暴露 `git add` / `git commit` / `git push` / `git merge` / `git tag` / `git reset` / `git checkout` / `git switch`。
- 读取任意文件系统路径。
- 删除或移动文件。
- 通过 Playwright 访问公网目标。
- 在仓库里保存密码。
- 实现 Minecraft TestBridge。
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

## 工具清单

- `health.check`：返回本地 MCP server 和配置摘要。
- `gradle.run`：只运行白名单 Gradle preset。
- `logs.tail`：只读取白名单日志 / 报告尾部。
- `webadmin.open`：用 Playwright 打开本地 WebAdmin。
- `webadmin.login`：用显式参数或环境变量登录，并通过 `/api/auth/me` 验证真实认证状态。
- `webadmin.change_password`：通过已登录 WebAdmin 会话修改当前用户密码。
- `webadmin.owner_set_password`：通过 OWNER 会话设置指定 WebAdmin 用户密码。
- `webadmin.goto`：跳转到 WebAdmin hash route。
- `webadmin.screenshot`：保存截图到 `reports/mcp/screenshots`。
- `webadmin.console_errors`：返回 Console error、page error、失败请求和错误响应。
- `webadmin.click`：点击当前本地 WebAdmin 页面内可见且启用的 selector。
- `webadmin.fill`：填写当前本地 WebAdmin 页面内可见且启用的 selector。
- `webadmin.text`：读取当前本地 WebAdmin 页面内 selector 的文本。
- `report.write`：写 Markdown 报告到 `reports/mcp`。
- `repo.status`：只读返回 git status / log / diff 摘要。
- `minecraft.start_client`：启动固定 Gradle `runClient` dev client preset。
- `minecraft.status`：查看 MCP 管理的 dev client 进程、日志尾部和 WebAdmin ready 状态。
- `minecraft.wait_webadmin`：等待本机 WebAdmin URL 可访问。
- `minecraft.stop`：只停止当前 MCP server 启动的 managed process。

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

本阶段只提供受控 dev runtime launcher，不做 Minecraft TestBridge，也不做 GUI 坐标点击。

可用工具：

- `minecraft.start_client`
- `minecraft.status`
- `minecraft.wait_webadmin`
- `minecraft.stop`

`minecraft.start_client` 固定执行：

```powershell
.\gradlew.bat --no-daemon runClient
```

非 Windows 下使用：

```bash
./gradlew --no-daemon runClient
```

限制：

- 只支持固定 `runClient` preset，不接收任意 Gradle 参数。
- stdout / stderr 写到 `reports/mcp/runtime/*.log`，不写 MCP stdout。
- 如果当前 MCP session 已启动 dev client，再次调用只返回当前状态。
- `minecraft.stop` 只停止当前 MCP server session 自己启动的进程；不会扫描或杀掉任意 Java / Minecraft 进程。
- 如果 dev client 未正常退出，会提示手动关闭，不会扩展为任意进程管理。
- WebAdmin ready 检测只访问 `http://127.0.0.1:18080/`、`localhost` 或 `::1` 这类 loopback 地址。

注意：Fabric `runClient` 启动后，WebAdmin 通常需要进入世界 / integrated server 后才会出现。本阶段不会自动选择世界；后续应通过 dev-only Minecraft TestBridge 做结构化世界启动，而不是 OS 鼠标坐标点击。

## 安全边界

- 没有任意 shell。
- 没有 git mutation 工具；没有 `git push` / `git merge` / `git tag`。
- 没有外网访问；WebAdmin 自动化和 `minecraft.wait_webadmin` 只允许 `127.0.0.1` / `localhost` / `::1` 这类 loopback host。
- 即使配置了 `allowedHosts`，非 loopback host 也会被拒绝。
- `reports/mcp` 是报告输出目录。
- `reports/mcp/screenshots` 是截图输出目录。
- `reports/mcp/runtime` 是 Minecraft dev runtime 日志输出目录。
- 工具输出会脱敏常见 password、cookie、token、CSRF、authorization、session 字段。
- 不处理、不删除、不提交仓库根目录下未跟踪的 `logs/`。

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
2. 手动在 Minecraft dev client 中进入测试世界。
3. `minecraft.wait_webadmin`
4. `webadmin.open`
5. `webadmin.screenshot`
6. `minecraft.status`
7. `minecraft.stop`，如果 dev client 未退出则按提示手动关闭。

## 输出文件

报告和截图都是本地文件：

- `reports/mcp/*.md`
- `reports/mcp/screenshots/*.png`
- `reports/mcp/runtime/*.log`
- `reports/mcp/session.log`

这些输出用于本地测试审计。提交前应确认不要把本地敏感报告或截图误提交。
