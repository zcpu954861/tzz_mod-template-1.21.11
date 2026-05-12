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

## Later Planning

Recommended later work should build on the dev-only TestBridge instead of OS coordinate clicking:

- Open or create a named test world through controlled in-mod hooks.
- Execute approved `/tzz` setup commands.
- Read game state through structured test APIs.
- Drive in-game GUI abstractions by semantic slot/action rather than screen coordinates.
- Add Step 3 semantic GUI tools such as `mc.gui.current`, `mc.gui.slot`, `mc.gui.click_template_slot`, `mc.gui.save`, and `mc.gui.cancel`.
