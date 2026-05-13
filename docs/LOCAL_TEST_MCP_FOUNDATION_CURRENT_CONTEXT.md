# Local Test MCP Foundation Current Context

## Stage

Local Test MCP Foundation.

This stage creates a local-only MCP testing bridge so Codex can later execute a small set of controlled test tasks from a local MCP client.

## Goals

- Run whitelisted Gradle validation presets.
- Read whitelisted test/build/WebAdmin log summaries.
- Open local WebAdmin with Playwright when local dependencies are installed.
- Capture screenshots and browser diagnostics.
- Write test reports under `reports/mcp`.
- Keep a clear extension point for a future Minecraft TestBridge stage.

## Step 1 Scope

- Add `tools/tzz-test-mcp` as an isolated local tool package.
- Add a stdio MCP server with a small tool registry.
- Add safety helpers for command, path, URL, output, and secret handling.
- Add safe tools:
  - `health.check`
  - `gradle.run`
  - `logs.tail`
  - `webadmin.open`
  - `webadmin.login`
  - `webadmin.goto`
  - `webadmin.screenshot`
  - `webadmin.console_errors`
  - `webadmin.click`
  - `webadmin.fill`
  - `webadmin.text`
  - `report.write`
  - `repo.status`
- Add documentation for local installation, startup, and smoke usage.
- Add guard coverage for the MCP foundation files and forbidden capabilities.

## Step 1.5 Scope: WebAdmin Password / Test Account Foundation

Step 1.5 adds stable local-login foundations for WebAdmin automation without writing plaintext credentials to the repository.

- WebAdmin exposes a current-user password change API:
  - `POST /api/webadmin/users/me/password`
  - Requires an authenticated session, old password, new password confirmation, CSRF, and same-origin.
  - Reuses the existing `WebAdminPasswordHasher` PBKDF2 storage path.
  - Writes audit through the existing WebAdmin user audit path.
  - Invalidates the same user's other sessions while keeping the current session.
- WebAdmin UI exposes a current-user password change entry in the topbar:
  - The entry is available to the logged-in user without requiring OWNER permissions.
  - The modal asks for current password, new password, and new-password confirmation.
  - Password values are held only in password inputs for the active form submit and are not written to WebAdmin state, browser storage, logs, reports, or URLs.
  - Users no longer need to rely on command-side password resets for routine password changes.
- WebAdmin exposes an OWNER-only password reset/set API:
  - `POST /api/webadmin/users/{username}/password-reset`
  - Requires OWNER, CSRF, and same-origin.
  - Reuses existing PBKDF2 hashing and user store persistence.
  - Does not print or return the password.
  - Invalidates sessions for the target user.
- The MCP server provides localhost WebAdmin helper tools that call those APIs through an already logged-in WebAdmin browser session:
  - `webadmin.change_password`
  - `webadmin.owner_set_password`
- A test account still must be created by normal WebAdmin user administration / command flow before password reset; this step does not directly write user files and does not bypass hashing.
- `webadmin.login` must verify real authentication state before returning success:
  - It checks username / password presence before submitting the login page.
  - It waits for the login form handler, performs a real login button click, and observes `/api/auth/login`.
  - If click does not trigger a login request, it falls back to pressing Enter on the password field and then `form.requestSubmit()`.
  - It validates the session through `/api/auth/me` and only returns success when the current user is known.
  - If no submit request is observed, it returns `SUBMIT_NOT_TRIGGERED` with diagnostic fields such as `buttonFound`, `buttonVisible`, `buttonEnabled`, `clicked`, `loginRequestObserved`, `loginResponseStatus`, `authMeStatus`, and `fallbackUsed`.
  - If authentication fails or the browser remains on `/login`, it returns `AUTH_FAILED` rather than pretending the form submit succeeded.
  - Diagnostic output never includes the password.
- `webadmin.goto` treats protected app routes as authentication-gated:
  - If the page is redirected or stuck on `/login`, it returns `AUTH_REQUIRED`.
  - It must not report a dashboard route as opened when WebAdmin is still unauthenticated.

Passwords must come from local environment variables or explicit MCP tool arguments at runtime. They must not be committed to config, docs, reports, screenshots, or logs.

## Step 2 Scope: Minecraft Dev Runtime Launcher Foundation

Step 2 adds a local launcher foundation for the Fabric dev client so a local Codex MCP client can start the Minecraft development runtime and wait for WebAdmin readiness.

- New MCP tools:
  - `minecraft.start_client`
  - `minecraft.status`
  - `minecraft.wait_webadmin`
  - `minecraft.stop`
- `minecraft.start_client` only runs the fixed Gradle whitelist preset `runClient` with `--no-daemon`.
- Runtime stdout/stderr are written under `reports/mcp/runtime`.
- `minecraft.wait_webadmin` only polls the configured localhost / loopback WebAdmin URL.
- `minecraft.stop` only stops the process started by the current MCP server session and never kills arbitrary Java processes.
- On Windows, `minecraft.stop` first requests a graceful stop for the managed `cmd.exe` / Gradle wrapper process. If that launcher pid has already disappeared while Gradle / Minecraft Java children continue running, it runs a fixed local process query that is constrained by the repo root, managed world name, launch time, and `runClient` command markers (`gradle-wrapper.jar` / `devlaunchinjector.Main`). It then uses fixed `taskkill.exe /pid <candidatePid> /t /f` only for those MCP-managed candidates and waits briefly for the candidates to disappear before reporting success. It does not accept a user-provided pid, does not kill arbitrary Java processes, and does not expose shell execution.
- This step does not enter a Minecraft world automatically.
- This step does not implement Minecraft TestBridge.
- This step does not click Minecraft GUI coordinates or use OS mouse / keyboard automation.

## Safety Boundaries

- No arbitrary shell tool.
- No arbitrary file read/write.
- No file deletion or moving.
- No git mutation tools.
- No cloud or public internet browser automation; WebAdmin automation is localhost-only by default.
- No committed credentials, passwords, tokens, cookies, CSRF tokens, session IDs, or browser profiles.
- No Minecraft TestBridge implementation in Step 1.
- No Minecraft client GUI coordinate clicking.
- No OS-level mouse or keyboard control.
- No image-recognition clicking.
- No automatic world selection or GUI coordinate based "enter world" flow.
- No changes to WebAdmin business features beyond the explicit password / test-account foundation in Step 1.5.
- No changes to Minecraft gameplay logic.
- No processing or committing `logs/`.

## Tool Safety Model

- Gradle commands are fixed presets only.
- Git commands are read-only status/log/diff summaries only.
- Log tailing resolves paths against explicit allowlisted directories.
- Report writing is restricted to `reports/mcp`.
- Screenshot writing is restricted to `reports/mcp/screenshots`.
- Browser automation only allows configured localhost hosts.
- Minecraft runtime launcher only runs fixed Gradle `runClient` preset.
- Runtime process management is limited to the MCP-managed child process.
- Tool output is redacted for common secret headers and credential names.

## Configuration

The default local config is documented in `tools/tzz-test-mcp/config.example.json`.

Credentials must be supplied with environment variables such as:

- `TZZ_WEBADMIN_USERNAME`
- `TZZ_WEBADMIN_PASSWORD`

The example config must not contain real credentials.

## Step 2.5 Scope: Minecraft TestBridge Foundation

Step 2.5 adds a dev-only Minecraft TestBridge foundation so local Codex MCP tools can perform controlled game-state setup and inspection after the Fabric dev client has started and the integrated server/world is loaded.

The TestBridge is intentionally narrow:

- It is hosted under WebAdmin as `/api/testbridge/*`.
- It is default disabled.
- It requires loopback / localhost access.
- It requires `TZZ_TESTBRIDGE_ENABLED=true` for operational endpoints.
- It requires `TZZ_TESTBRIDGE_TOKEN` and request header `X-TZZ-TestBridge-Token` for operational endpoints.
- The token is never returned, logged, or written into reports.
- It writes TestBridge audit entries without secrets.
- It does not provide arbitrary shell, git mutation, external host access, or OS-level Minecraft GUI coordinate clicking.

Server-side P0 endpoints:

- `GET /api/testbridge/status`
- `GET /api/testbridge/players`
- `POST /api/testbridge/command`
- `POST /api/testbridge/world/set-block`
- `POST /api/testbridge/world/clear-area`
- `POST /api/testbridge/player/give`
- `POST /api/testbridge/player/clear-inventory`
- `POST /api/testbridge/player/set-main-hand`
- `POST /api/testbridge/player/use-block`
- `GET|POST /api/testbridge/device/inspect`
- `GET /api/testbridge/signal/history`
- `GET /api/testbridge/doctor/issues`

MCP P0 tools:

- `minecraft.testbridge_status`
- `minecraft.players`
- `minecraft.command`
- `minecraft.set_block`
- `minecraft.clear_area`
- `minecraft.give_item`
- `minecraft.clear_inventory`
- `minecraft.set_main_hand`
- `minecraft.use_block`
- `minecraft.inspect_device`
- `minecraft.signal_history`
- `minecraft.doctor_issues`
- `minecraft.wait_testbridge`

Safety restrictions:

- `minecraft.command` is allowlisted and denies dangerous roots such as `stop`, `op`, `deop`, `ban`, `kick`, `whitelist`, `save-off`, `save-on`, `pardon`, and `reload`.
- World mutation tools are restricted to the default test area `x=-128..128`, `y=-64..320`, `z=-128..128`.
- `minecraft.clear_area` also enforces `maxClearVolume=4096`.
- `minecraft.give_item` is count-limited and does not accept raw NBT or arbitrary component path editing.
- `minecraft.clear_inventory` only affects the specified online player.
- `minecraft.set_main_hand` sets only the specified player's main hand.
- `minecraft.use_block` invokes Minecraft's `UseBlockCallback` production path, so VBD right-click tests reuse the same handler chain as real player interaction.
- `minecraft.inspect_device`, `minecraft.signal_history`, and `minecraft.doctor_issues` are read-only inspection endpoints.

Step 2.5 still does not automatically enter a Minecraft world. Future work should add a separate dev-only auto-enter-world helper or in-mod test world bootstrap. It must not use OS mouse coordinate clicking.

## Step 3 Scope: Auto Enter Test World + Scenario Preparation Foundation

Step 3 reduces the remaining manual world setup step without adding OS-level UI automation. It uses Minecraft's native quick play singleplayer argument for an existing local test world and structured TestBridge preparation endpoints after the integrated server is loaded.

Auto-enter strategy:

- `minecraft.start_client` accepts `autoEnterWorld=true` and `worldName`.
- `worldName` defaults to `TZZ_TEST_WORLD_NAME` or `TZZ_MCP_TEST_WORLD`.
- The world name is a save-folder id, not a display title.
- `worldName` is sanitized: only ASCII letters, digits, `.`, `_`, and `-`; path separators, `..`, NUL, absolute paths, spaces, and control characters are rejected.
- The launcher checks `run/saves/<worldName>/level.dat` before starting. If the world does not exist, it returns `NOT_FOUND` with a clear message and does not open another world.
- Auto-enter appends only the fixed quick play argument `--quickPlaySingleplayer <worldName>` through the Gradle `runClient --args=...` path. It does not expose arbitrary runClient args.
- The Minecraft client window remains visible (`windowsHide=false`).

Step 3 server-side TestBridge endpoints:

- `POST /api/testbridge/world/prepare-area`
- `POST /api/testbridge/world/prepare-player`
- `POST /api/testbridge/world/prepare`

Step 3 MCP tools:

- `minecraft.wait_world`
- `minecraft.prepare_test_area`
- `minecraft.prepare_test_player`
- `minecraft.prepare_test_world`

Preparation semantics:

- `minecraft.wait_world` waits for TestBridge status to report enabled, token configured, server loaded, world loaded, and optionally at least one player.
- `minecraft.prepare_test_area` clears a bounded loaded test area and can lay a simple floor. It still uses the TestBridge test bounds `x=-128..128`, `y=-64..320`, `z=-128..128` and the `maxClearVolume=4096` limit.
- `minecraft.prepare_test_player` is scoped to one online player. It can clear that player's inventory/offhand and teleport the player to a coordinate inside the test area.
- `minecraft.prepare_test_world` is idempotent and composes world readiness, area preparation, optional player preparation, day time, and clear weather. It does not delete worlds and does not affect blocks outside the bounded test area.
- Preparation endpoints are still loopback-only, token-protected, audited, and do not log the token.

Step 3 still does not:

- Create a missing Minecraft world.
- Delete or overwrite user worlds.
- Click Minecraft GUI coordinates.
- Use OS-level mouse / keyboard control.
- Use image-recognition clicking.
- Automate P3b / 7.10 in-game GUI interactions.
- Add arbitrary shell, git mutation, external host access, or dangerous Minecraft commands.

## Step 4 Scope: Minecraft GUI Operation Abstraction Foundation

Step 4 adds a dev-only semantic GUI operation layer for supported WebAdmin Minecraft test GUIs. It lets local Codex MCP tools inspect and operate the current supported in-game screen by screen type, slot id, and action name. It still does not use OS mouse / keyboard automation, image recognition, or coordinate clicking.

Supported GUI types:

- `container_template`: the 7.9 P3b container content-change template GUI.
- `single_item_submit`: the 7.10 single itemSubmit template GUI.
- `unsupported`: any other currently open screen.
- `none`: no current screen.

Architecture:

- HTTP endpoints stay under `/api/testbridge/gui/*`.
- The HTTP TestBridge route remains loopback-only and token-protected.
- Server-side TestBridge does not directly access `MinecraftClient.currentScreen`.
- GUI operations use a dev-only client screen payload round trip:
  - server sends a nonce-bound S2C GUI operation request to the target player;
  - the target client executes the operation on the current supported screen;
  - the client returns a nonce-bound C2S result;
  - the HTTP request returns the structured result or a clear error.
- The TestBridge token is never sent to the client payload.
- Unsupported screens return `UNSUPPORTED_GUI`; no screen returns `GUI_NOT_OPEN`.

Step 4 endpoints:

- `GET /api/testbridge/gui/current`
- `GET /api/testbridge/gui/slots`
- `POST /api/testbridge/gui/put-item`
- `POST /api/testbridge/gui/clear-slot`
- `POST /api/testbridge/gui/set-count`
- `POST /api/testbridge/gui/save`
- `POST /api/testbridge/gui/cancel`
- `POST /api/testbridge/client/screenshot`

Step 4 MCP tools:

- `minecraft.gui_current`
- `minecraft.gui_slots`
- `minecraft.gui_put_item`
- `minecraft.gui_clear_slot`
- `minecraft.gui_set_count`
- `minecraft.gui_save`
- `minecraft.gui_cancel`
- `minecraft.client_screenshot`

GUI operation semantics:

- `minecraft.gui_current` returns current GUI type, title, target player, session id, device id, dirty state, and terminal/session flags.
- `minecraft.gui_slots` returns editable template slots for supported GUIs. Container template slots are indexed by slot number; single itemSubmit exposes one `submit_template` slot.
- `minecraft.gui_put_item` creates or replaces a ghost/template item from an item id/count spec and does not modify real player inventory or real world containers.
- `minecraft.gui_clear_slot` clears only the ghost/template slot.
- `minecraft.gui_set_count` uses the existing GUI count rules. Container template stack counts clamp to the item max stack count. Single itemSubmit keeps requirement count separate from display stack count.
- `minecraft.gui_save` calls the existing GUI session save path and does not directly write `SignalDeviceData`.
- `minecraft.gui_cancel` calls the existing GUI cancel path and releases the existing session/lock lifecycle.
- `minecraft.client_screenshot` asks the target Minecraft client to capture its current framebuffer through the same nonce-bound client payload bridge. It is intended for GUI and visual validation, including compact-layout checks and GUI scale compatibility.
- Client screenshots are saved under `reports/mcp/screenshots` with sanitized timestamped file names. Reports can reference the returned path, but screenshot files remain local test output and should not be committed.
- The screenshot tool does not use OS screenshots, does not capture other windows, does not click screen coordinates, and does not send the TestBridge token to the client payload.

Step 4 still does not:

- Support arbitrary Minecraft screens.
- Click Minecraft GUI coordinates.
- Use OS-level input control or image-recognition clicking.
- Use OS screenshots.
- Directly write `SignalDeviceData` JSON.
- Bypass existing WebAdmin session save / cancel validation.
- Automate full P3b / 7.10 GUI opening flows; use existing WebAdmin or session tools to open the supported GUI first.

## Step 5 Scope: Scenario Test Orchestration Foundation

Step 5 adds a local scenario runner that composes existing safe MCP / TestBridge / WebAdmin tools into repeatable smoke tests. The scenario runner is an orchestration layer only: it does not add arbitrary shell execution, does not add git mutation, does not access external hosts, does not click Minecraft GUI coordinates, and does not bypass TestBridge token / loopback checks.

Step 5 MCP tools:

- `scenario.list`
- `scenario.run`
- `scenario.report`
- `scenario.cleanup`
- `webadmin.close`

Built-in scenarios:

- `basic_environment`: starts or reuses `runClient`, auto-enters the configured test world, waits for WebAdmin/TestBridge/world readiness, prepares the test world, logs in to WebAdmin, opens dashboard, captures browser diagnostics, reads Doctor issues, writes a report, and then applies cleanup according to `keepClientOpen`.
- `vbd_right_click`: prepares a bounded VBD + receiver scene, configures right-click interaction through fixed `/tzz` commands, calls `minecraft.use_block`, checks signal events/history, captures diagnostics, and writes a report.
- `single_item_submit_basic`: prepares a VBD + receiver scene, opens the 7.10 single itemSubmit GUI through the existing fixed WebAdmin session API with CSRF, lock, and expected fingerprint, edits the supported GUI through Step 4 semantic tools, saves through the existing GUI save path, inspects the saved requirement, performs `use_block`, checks signal history, and writes a report.
- `container_template_basic`: prepares a bounded container VBD + receiver scene, opens the 7.9 container template GUI through the existing fixed WebAdmin session API with CSRF, lock, and expected fingerprint, edits the supported GUI through Step 4 semantic tools, saves through the existing GUI save path, inspects saved itemConditions, reads Doctor issues, and writes a report.

Scenario execution model:

- `scenario.run` has a fixed scenario allowlist and only calls `SCENARIO_ALLOWED_TOOLS` / `ALLOWED_SCENARIO_TOOLS` from the existing safe MCP tool registry.
- The scenario step runner stops on failure and records the failed step id, tool name, code, message, and sanitized structured data.
- Scenario reports are written under `reports/mcp/scenarios`.
- Reports include start/end time, pass/fail, step table, failure details, screenshot paths, selected structured data, and cleanup result.
- `scenario.cleanup` closes only resources managed by the MCP session: it can call `minecraft.stop` and `webadmin.close`; it does not delete logs, reports, screenshots, worlds, or other files.
- `webadmin.close` closes the current Playwright page/context/browser if one is open and is idempotent when no browser is open.

Step 5 still does not:

- Create arbitrary scenario programs supplied by the user.
- Execute arbitrary shell commands.
- Expose git mutation.
- Access external WebAdmin or public internet hosts.
- Click Minecraft GUI coordinates.
- Support arbitrary Minecraft screens.
- Delete reports, screenshots, logs, worlds, or repository files during cleanup.

## Later Planning

Recommended later work should build on the dev-only TestBridge instead of OS coordinate clicking:

- Create a missing named test world through controlled in-mod hooks if quick play existing-world startup is not enough.
- Execute approved `/tzz` setup commands.
- Read game state through structured test APIs.
- Add higher-level scenario suites that open the target WebAdmin GUI, operate it through Step 4 semantic tools, inspect device state, run `minecraft.use_block`, check signal history, and write a report.
- Add later GUI field operations for single itemSubmit count mode / consume / matcher options / vanilla policy if needed.
