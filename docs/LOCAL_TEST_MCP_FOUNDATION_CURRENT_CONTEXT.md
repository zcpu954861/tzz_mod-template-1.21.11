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

## Step 2 Planning

Future Minecraft TestBridge work may add in-mod test APIs for controlled game-state checks and scenario orchestration. That future stage must be designed separately and must not be implemented by Step 1.

Step 2 is not allowed to inherit unsafe primitives from Step 1. It must still avoid arbitrary shell, arbitrary file access, public network automation, and raw OS mouse/keyboard control unless a later explicit scope permits otherwise.

Recommended later work should use a dev-only Minecraft TestBridge instead of OS coordinate clicking:

- Open or create a named test world through controlled in-mod hooks.
- Execute approved `/tzz` setup commands.
- Read game state through structured test APIs.
- Drive in-game GUI abstractions by semantic slot/action rather than screen coordinates.
