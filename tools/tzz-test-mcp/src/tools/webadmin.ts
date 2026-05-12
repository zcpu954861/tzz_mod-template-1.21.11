import type { JsonObject, ToolContext, ToolDefinition } from "../types.js";
import { fail, ok } from "../results.js";
import { ensureAllowedUrl, ensureHashRoute, ensureReportPath, redactSecrets } from "../safety.js";
import type { ToolErrorCode } from "../results.js";

type PlaywrightModule = {
  chromium: {
    launch(options: { headless: boolean }): Promise<unknown>;
  };
};

export function webAdminTools(): ToolDefinition[] {
  return [
    webAdminOpenTool(),
    webAdminLoginTool(),
    webAdminChangePasswordTool(),
    webAdminOwnerSetPasswordTool(),
    webAdminGotoTool(),
    webAdminScreenshotTool(),
    webAdminConsoleErrorsTool(),
    webAdminClickTool(),
    webAdminFillTool(),
    webAdminTextTool()
  ];
}

function webAdminChangePasswordTool(): ToolDefinition {
  return {
    name: "webadmin.change_password",
    description: "Change the currently logged-in WebAdmin user's password through the localhost WebAdmin API. Passwords are never printed.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        oldPassword: { type: "string" },
        newPassword: { type: "string" },
        confirmPassword: { type: "string" }
      }
    },
    async handler(args, context) {
      const page = getPage(context);
      if (!page) {
        return fail("VALIDATION_ERROR", "webadmin.open and webadmin.login must run before webadmin.change_password.");
      }
      const oldPassword = typeof args.oldPassword === "string" ? args.oldPassword : process.env.TZZ_WEBADMIN_PASSWORD;
      const newPassword = typeof args.newPassword === "string" ? args.newPassword : process.env.TZZ_WEBADMIN_NEW_PASSWORD;
      const confirmPassword = typeof args.confirmPassword === "string" ? args.confirmPassword : newPassword;
      if (!oldPassword || !newPassword || !confirmPassword) {
        return fail("VALIDATION_ERROR", "Missing password inputs. Provide oldPassword/newPassword or TZZ_WEBADMIN_PASSWORD/TZZ_WEBADMIN_NEW_PASSWORD.");
      }
      return await postWebAdminWrite(context, "/api/webadmin/users/me/password", { oldPassword, newPassword, confirmPassword }, "WebAdmin password changed.");
    }
  };
}

function webAdminOwnerSetPasswordTool(): ToolDefinition {
  return {
    name: "webadmin.owner_set_password",
    description: "Set a WebAdmin user's password through the OWNER-only localhost WebAdmin API. This does not create users and never prints passwords.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        username: { type: "string" },
        newPassword: { type: "string" },
        confirmPassword: { type: "string" }
      }
    },
    async handler(args, context) {
      const page = getPage(context);
      if (!page) {
        return fail("VALIDATION_ERROR", "webadmin.open and webadmin.login as OWNER must run before webadmin.owner_set_password.");
      }
      const username = typeof args.username === "string" ? args.username.trim() : process.env.TZZ_WEBADMIN_TEST_USERNAME;
      const newPassword = typeof args.newPassword === "string" ? args.newPassword : process.env.TZZ_WEBADMIN_TEST_PASSWORD;
      const confirmPassword = typeof args.confirmPassword === "string" ? args.confirmPassword : newPassword;
      if (!username || !newPassword || !confirmPassword) {
        return fail("VALIDATION_ERROR", "Missing username/password inputs. Provide username/newPassword or TZZ_WEBADMIN_TEST_USERNAME/TZZ_WEBADMIN_TEST_PASSWORD.");
      }
      return await postWebAdminWrite(
        context,
        `/api/webadmin/users/${encodeURIComponent(username)}/password-reset`,
        { newPassword, confirmPassword },
        "WebAdmin user password set."
      );
    }
  };
}

function webAdminOpenTool(): ToolDefinition {
  return {
    name: "webadmin.open",
    description: "Open local WebAdmin with Playwright and capture a startup screenshot.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        url: { type: "string" },
        headless: { type: "boolean" },
        timeoutMs: { type: "number", minimum: 1000, maximum: 120000 }
      }
    },
    async handler(args, context) {
      const playwright = await loadPlaywright();
      if (!playwright) {
        return fail("TOOL_UNAVAILABLE", "Playwright is not installed. Run npm install and npx playwright install chromium in tools/tzz-test-mcp.");
      }
      const url = typeof args.url === "string" ? args.url : context.config.webAdminUrl;
      let allowedUrl: URL;
      try {
        allowedUrl = ensureAllowedUrl(url, context.config);
      } catch (error) {
        return fail("SECURITY_DENIED", error instanceof Error ? error.message : "URL is not allowed.");
      }
      const headless = typeof args.headless === "boolean" ? args.headless : context.config.playwrightHeadless;
      const timeoutMs = typeof args.timeoutMs === "number" ? args.timeoutMs : 30000;
      try {
        await closeBrowser(context);
        const browser = await playwright.chromium.launch({ headless });
        const browserContext = await call(browser, "newContext", [{ ignoreHTTPSErrors: false }]);
        await call(browserContext, "route", ["**/*", (route: unknown) => {
          const request = getRequest(route);
          const requestUrl = String(callSync(request, "url") ?? "");
          try {
            ensureAllowedUrl(requestUrl, context.config);
            void call(route, "continue", []);
          } catch (error) {
            context.webAdmin.failedRequests.push({
              url: requestUrl,
              error: error instanceof Error ? error.message : "Blocked by local URL allowlist"
            });
            void call(route, "abort", ["blockedbyclient"]);
          }
        }]);
        const page = await call(browserContext, "newPage", []);
        attachDiagnostics(page, context);
        await call(page, "goto", [allowedUrl.toString(), { waitUntil: "domcontentloaded", timeout: timeoutMs }]);
        await waitForLoad(page, timeoutMs);
        context.webAdmin.playwright = playwright;
        context.webAdmin.browser = browser;
        context.webAdmin.context = browserContext;
        context.webAdmin.page = page;
        context.webAdmin.baseUrl = `${allowedUrl.protocol}//${allowedUrl.host}`;
        const screenshot = await saveScreenshot(context, "open", true);
        const title = String(await call(page, "title", []));
        const currentUrl = String(callSync(page, "url") ?? "");
        return ok("WebAdmin opened.", {
          title,
          currentUrl,
          screenshotPath: screenshot
        });
      } catch (error) {
        return fail("BROWSER_ERROR", errorMessage(error));
      }
    }
  };
}

function webAdminLoginTool(): ToolDefinition {
  return {
    name: "webadmin.login",
    description: "Login to local WebAdmin with explicit credentials or TZZ_WEBADMIN_USERNAME/TZZ_WEBADMIN_PASSWORD.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        username: { type: "string" },
        password: { type: "string" },
        rememberMe: { type: "boolean" }
      }
    },
    async handler(args, context) {
      const page = getPage(context);
      if (!page) {
        return fail("VALIDATION_ERROR", "webadmin.open must run before webadmin.login.");
      }
      const username = typeof args.username === "string" ? args.username : process.env.TZZ_WEBADMIN_USERNAME;
      const password = typeof args.password === "string" ? args.password : process.env.TZZ_WEBADMIN_PASSWORD;
      const usernameConfigured = Boolean(username && username.trim());
      const passwordConfigured = Boolean(password);
      if (!usernameConfigured || !passwordConfigured) {
        return fail("CONFIG_ERROR", "Missing WebAdmin credentials. Provide username/password or TZZ_WEBADMIN_USERNAME/TZZ_WEBADMIN_PASSWORD.", {
          usernameConfigured,
          passwordConfigured,
          username: usernameConfigured ? String(username).trim() : ""
        });
      }
      const usernameValue = String(username).trim();
      const passwordValue = String(password);
      const diagnostics = createLoginDiagnostics();
      try {
        const base = context.webAdmin.baseUrl ?? new URL(context.config.webAdminUrl).origin;
        await call(page, "goto", [`${base}/login`, { waitUntil: "domcontentloaded", timeout: 30000 }]);
        await waitForLoad(page, 30000);
        diagnostics.loginHandlerReady = await waitForLoginHandler(page, 10000);
        const usernameField = await requireVisibleEnabled(page, "#username", 10000);
        await fillAndDispatch(usernameField, usernameValue, 10000);
        const filledUsername = String(await call(usernameField, "inputValue", []) ?? "");
        if (filledUsername !== usernameValue) {
          return fail("BROWSER_ERROR", "WebAdmin username input did not retain the supplied username.", {
            usernameConfigured: true,
            passwordConfigured: true,
            username: usernameValue,
            ...diagnostics
          });
        }
        const passwordField = await requireVisibleEnabled(page, "#password", 10000);
        await fillAndDispatch(passwordField, passwordValue, 10000);
        if (args.rememberMe === true) {
          await clickSelector(page, "#remember");
        }
        const loginButton = await findLoginButton(page, diagnostics, 10000);
        await submitLogin(page, loginButton, passwordField, diagnostics);
        const auth = await waitForAuthenticated(page, 15000);
        diagnostics.authMeStatus = auth.status;
        if (!auth.authenticated) {
          const screenshotPath = await saveScreenshot(context, "login-failed", true).catch(() => "");
          diagnostics.screenshotPath = screenshotPath;
          const code: Exclude<ToolErrorCode, "OK"> = diagnostics.loginRequestObserved ? "AUTH_FAILED" : "SUBMIT_NOT_TRIGGERED";
          const message = diagnostics.loginRequestObserved
            ? auth.message || "WebAdmin authentication failed."
            : "WebAdmin login submit did not trigger /api/auth/login.";
          return fail(code, message, {
            currentUrl: auth.currentUrl,
            currentUser: "",
            currentRole: "",
            usernameConfigured: true,
            passwordConfigured: true,
            username: usernameValue,
            authStatus: auth.status,
            loginMessage: auth.loginMessage,
            pageErrorText: auth.loginMessage,
            ...diagnostics
          });
        }
        if (isLoginUrl(auth.currentUrl)) {
          await call(page, "goto", [`${base}/app#/dashboard`, { waitUntil: "domcontentloaded", timeout: 30000 }]).catch(() => undefined);
          await waitForLoad(page, 30000);
        }
        const currentUrl = String(callSync(page, "url") ?? auth.currentUrl);
        return ok("WebAdmin login verified.", {
          currentUrl,
          currentUser: auth.username,
          currentRole: auth.role,
          usernameConfigured: true,
          passwordConfigured: true,
          ...diagnostics
        });
      } catch (error) {
        const screenshotPath = await saveScreenshot(context, "login-failed", true).catch(() => "");
        diagnostics.screenshotPath = screenshotPath;
        if (error instanceof WebAdminToolError) {
          return fail(error.code, error.message, {
            currentUrl: String(callSync(page, "url") ?? ""),
            usernameConfigured: true,
            passwordConfigured: true,
            username: usernameValue,
            ...diagnostics
          });
        }
        const code: Exclude<ToolErrorCode, "OK"> = diagnostics.clicked ? "BUTTON_CLICK_FAILED" : "BROWSER_ERROR";
        return fail(code, errorMessage(error), {
          currentUrl: String(callSync(page, "url") ?? ""),
          usernameConfigured: true,
          passwordConfigured: true,
          username: usernameValue,
          ...diagnostics
        });
      }
    }
  };
}

function webAdminGotoTool(): ToolDefinition {
  return {
    name: "webadmin.goto",
    description: "Navigate within the current local WebAdmin origin to a hash route.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        hashRoute: { type: "string" },
        timeoutMs: { type: "number", minimum: 1000, maximum: 120000 }
      },
      required: ["hashRoute"]
    },
    async handler(args, context) {
      const page = getPage(context);
      if (!page) {
        return fail("VALIDATION_ERROR", "webadmin.open must run before webadmin.goto.");
      }
      let route: string;
      try {
        route = ensureHashRoute(args.hashRoute);
      } catch (error) {
        return fail("VALIDATION_ERROR", errorMessage(error));
      }
      try {
        const base = context.webAdmin.baseUrl ?? new URL(context.config.webAdminUrl).origin;
        const timeoutMs = typeof args.timeoutMs === "number" ? args.timeoutMs : 30000;
        await call(page, "goto", [`${base}/app${route}`, { waitUntil: "domcontentloaded", timeout: timeoutMs }]);
        await waitForLoad(page, timeoutMs);
        const title = String(await call(page, "title", []));
        const currentUrl = String(callSync(page, "url") ?? "");
        const auth = await probeAuth(page);
        if (isLoginUrl(currentUrl) || !auth.authenticated) {
          return fail("AUTH_REQUIRED", "当前未登录，无法进入目标 WebAdmin route。", {
            hashRoute: route,
            title,
            currentUrl,
            currentUser: "",
            currentRole: "",
            authStatus: auth.status,
            authMessage: auth.message
          });
        }
        return ok("WebAdmin route opened.", {
          hashRoute: route,
          title,
          currentUrl,
          currentUser: auth.username,
          currentRole: auth.role
        });
      } catch (error) {
        return fail("BROWSER_ERROR", errorMessage(error));
      }
    }
  };
}

function webAdminScreenshotTool(): ToolDefinition {
  return {
    name: "webadmin.screenshot",
    description: "Save a screenshot of the current WebAdmin page under reports/mcp/screenshots.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        name: { type: "string" },
        fullPage: { type: "boolean" }
      }
    },
    async handler(args, context) {
      if (!getPage(context)) {
        return fail("VALIDATION_ERROR", "webadmin.open must run before webadmin.screenshot.");
      }
      try {
        const name = typeof args.name === "string" ? args.name : "webadmin";
        const fullPage = args.fullPage !== false;
        const file = await saveScreenshot(context, name, fullPage);
        return ok("Screenshot saved.", { path: file });
      } catch (error) {
        return fail("BROWSER_ERROR", errorMessage(error));
      }
    }
  };
}

function webAdminConsoleErrorsTool(): ToolDefinition {
  return {
    name: "webadmin.console_errors",
    description: "Return captured console errors, page errors, failed requests, and HTTP error responses.",
    inputSchema: {
      type: "object",
      additionalProperties: false
    },
    readOnlyHint: true,
    async handler(_args, context) {
      return ok("WebAdmin diagnostics returned.", {
        consoleErrors: context.webAdmin.consoleErrors,
        pageErrors: context.webAdmin.pageErrors,
        failedRequests: context.webAdmin.failedRequests,
        badResponses: context.webAdmin.badResponses
      });
    }
  };
}

function webAdminClickTool(): ToolDefinition {
  return {
    name: "webadmin.click",
    description: "Click a visible and enabled selector on the current WebAdmin page.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        selector: { type: "string" },
        timeoutMs: { type: "number", minimum: 1000, maximum: 60000 }
      },
      required: ["selector"]
    },
    async handler(args, context) {
      const page = getPage(context);
      if (!page) {
        return fail("VALIDATION_ERROR", "webadmin.open must run before webadmin.click.");
      }
      try {
        const selector = requiredString(args, "selector");
        await clickSelector(page, selector, typeof args.timeoutMs === "number" ? args.timeoutMs : 10000);
        return ok("Element clicked.", { selector });
      } catch (error) {
        return fail("BROWSER_ERROR", errorMessage(error));
      }
    }
  };
}

function webAdminFillTool(): ToolDefinition {
  return {
    name: "webadmin.fill",
    description: "Fill a visible and enabled selector on the current WebAdmin page.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        selector: { type: "string" },
        value: { type: "string" },
        timeoutMs: { type: "number", minimum: 1000, maximum: 60000 }
      },
      required: ["selector", "value"]
    },
    async handler(args, context) {
      const page = getPage(context);
      if (!page) {
        return fail("VALIDATION_ERROR", "webadmin.open must run before webadmin.fill.");
      }
      try {
        const selector = requiredString(args, "selector");
        const value = requiredString(args, "value");
        await fillSelector(page, selector, value, typeof args.timeoutMs === "number" ? args.timeoutMs : 10000);
        return ok("Element filled.", { selector, valueLength: value.length });
      } catch (error) {
        return fail("BROWSER_ERROR", errorMessage(error));
      }
    }
  };
}

function webAdminTextTool(): ToolDefinition {
  return {
    name: "webadmin.text",
    description: "Read text content from a selector on the current WebAdmin page.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        selector: { type: "string" },
        timeoutMs: { type: "number", minimum: 1000, maximum: 60000 }
      },
      required: ["selector"]
    },
    readOnlyHint: true,
    async handler(args, context) {
      const page = getPage(context);
      if (!page) {
        return fail("VALIDATION_ERROR", "webadmin.open must run before webadmin.text.");
      }
      try {
        const selector = requiredString(args, "selector");
        const locator = firstLocator(page, selector);
        await call(locator, "waitFor", [{ state: "visible", timeout: typeof args.timeoutMs === "number" ? args.timeoutMs : 10000 }]);
        const text = String(await call(locator, "textContent", []) ?? "");
        return ok("Element text read.", { selector, text: redactSecrets(text.trim()) });
      } catch (error) {
        return fail("BROWSER_ERROR", errorMessage(error));
      }
    }
  };
}

function createLoginDiagnostics(): JsonObject {
  return {
    buttonFound: false,
    buttonVisible: false,
    buttonEnabled: false,
    loginHandlerReady: false,
    clicked: false,
    enterPressed: false,
    requestSubmitUsed: false,
    loginRequestObserved: false,
    loginResponseStatus: 0,
    authMeStatus: 0,
    fallbackUsed: "",
    pageErrorText: "",
    screenshotPath: ""
  };
}

async function fillAndDispatch(locator: unknown, value: string, timeoutMs: number): Promise<void> {
  await call(locator, "fill", [value, { timeout: timeoutMs }]);
  await call(locator, "dispatchEvent", ["input"]).catch(() => undefined);
  await call(locator, "dispatchEvent", ["change"]).catch(() => undefined);
}

async function waitForLoginHandler(page: unknown, timeoutMs: number): Promise<boolean> {
  try {
    await call(page, "waitForFunction", [() => {
      const form = document.getElementById("login-form") as HTMLFormElement | null;
      return Boolean(form && typeof form.onsubmit === "function");
    }, null, { timeout: timeoutMs }]);
    return true;
  } catch {
    return false;
  }
}

async function findLoginButton(page: unknown, diagnostics: JsonObject, timeoutMs: number): Promise<unknown> {
  const candidates = [
    callSync(page, "getByRole", ["button", { name: /登录/ }]),
    firstLocator(page, "#login-form button[type=\"submit\"]"),
    firstLocator(page, "#login-form button")
  ].filter(Boolean);
  for (const candidate of candidates) {
    try {
      await call(candidate, "waitFor", [{ state: "visible", timeout: Math.min(timeoutMs, 3000) }]);
      diagnostics.buttonFound = true;
      diagnostics.buttonVisible = true;
      const enabled = Boolean(await call(candidate, "isEnabled", []));
      diagnostics.buttonEnabled = enabled;
      if (!enabled) {
        throw new WebAdminToolError("BUTTON_DISABLED", "WebAdmin login button is disabled.");
      }
      return candidate;
    } catch (error) {
      if (error instanceof WebAdminToolError) {
        throw error;
      }
    }
  }
  throw new WebAdminToolError("SELECTOR_NOT_FOUND", "WebAdmin login button was not found or was not visible.");
}

async function submitLogin(page: unknown, loginButton: unknown, passwordField: unknown, diagnostics: JsonObject): Promise<void> {
  let clickError = "";
  const clickStatus = await submitAndObserveLogin(page, diagnostics, "click", async () => {
    await call(loginButton, "click", [{ timeout: 10000 }]);
    diagnostics.clicked = true;
  });
  if (clickStatus > 0) {
    return;
  }
  if (diagnostics.clicked !== true) {
    clickError = "Login button click did not complete.";
  }

  const enterStatus = await submitAndObserveLogin(page, diagnostics, "enter", async () => {
    diagnostics.enterPressed = true;
    await call(passwordField, "press", ["Enter", { timeout: 10000 }]);
  });
  if (enterStatus > 0) {
    return;
  }

  const submitStatus = await submitAndObserveLogin(page, diagnostics, "requestSubmit", async () => {
    diagnostics.requestSubmitUsed = true;
    const submitted = Boolean(await call(page, "evaluate", [() => {
      const form = document.getElementById("login-form") as HTMLFormElement | null;
      if (!form) {
        return false;
      }
      if (typeof form.requestSubmit === "function") {
        form.requestSubmit();
      } else {
        form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
      }
      return true;
    }]));
    if (!submitted) {
      throw new WebAdminToolError("SELECTOR_NOT_FOUND", "WebAdmin login form was not found for requestSubmit fallback.");
    }
  });
  if (submitStatus > 0) {
    return;
  }

  if (clickError) {
    diagnostics.pageErrorText = clickError;
  }
}

async function submitAndObserveLogin(
  page: unknown,
  diagnostics: JsonObject,
  fallbackName: string,
  submit: () => Promise<void>
): Promise<number> {
  const responsePromise = waitForLoginResponse(page, 5000);
  if (fallbackName !== "click") {
    const previous = String(diagnostics.fallbackUsed || "");
    diagnostics.fallbackUsed = previous ? `${previous},${fallbackName}` : fallbackName;
  }
  try {
    await submit();
  } catch (error) {
    diagnostics.pageErrorText = errorMessage(error);
    return 0;
  }
  const status = await responsePromise;
  if (status > 0) {
    diagnostics.loginRequestObserved = true;
    diagnostics.loginResponseStatus = status;
  }
  return status;
}

async function waitForLoginResponse(page: unknown, timeoutMs: number): Promise<number> {
  try {
    const response = await call(page, "waitForResponse", [(candidate: unknown) => {
      const url = String(callSync(candidate, "url") ?? "");
      return url.includes("/api/auth/login");
    }, { timeout: timeoutMs }]);
    return Number(callSync(response, "status") ?? 0);
  } catch {
    return 0;
  }
}

async function loadPlaywright(): Promise<PlaywrightModule | undefined> {
  try {
    return await import("playwright") as PlaywrightModule;
  } catch {
    return undefined;
  }
}

function attachDiagnostics(page: unknown, context: ToolContext): void {
  void call(page, "on", ["console", (message: unknown) => {
    const type = String(callSync(message, "type") ?? "");
    if (type === "error") {
      context.webAdmin.consoleErrors.push(redactSecrets(String(callSync(message, "text") ?? "")));
    }
  }]);
  void call(page, "on", ["pageerror", (error: Error) => {
    context.webAdmin.pageErrors.push(redactSecrets(error.stack ?? error.message));
  }]);
  void call(page, "on", ["requestfailed", (request: unknown) => {
    context.webAdmin.failedRequests.push({
      url: redactSecrets(String(callSync(request, "url") ?? "")),
      failure: redactSecrets(String(callSync(callSync(request, "failure"), "errorText") ?? "request failed"))
    });
  }]);
  void call(page, "on", ["response", (response: unknown) => {
    const status = Number(callSync(response, "status") ?? 0);
    if (status >= 400) {
      context.webAdmin.badResponses.push({
        status,
        url: redactSecrets(String(callSync(response, "url") ?? ""))
      });
    }
  }]);
}

async function saveScreenshot(context: ToolContext, name: string, fullPage: boolean): Promise<string> {
  const page = getPage(context);
  if (!page) {
    throw new Error("No WebAdmin page is open");
  }
  const file = ensureReportPath(context.config, name, ".png");
  await call(page, "screenshot", [{ path: file, fullPage }]);
  context.webAdmin.screenshots.push(file);
  return file;
}

async function closeBrowser(context: ToolContext): Promise<void> {
  const browser = context.webAdmin.browser;
  if (browser) {
    await call(browser, "close", []).catch(() => undefined);
  }
  context.webAdmin.browser = undefined;
  context.webAdmin.context = undefined;
  context.webAdmin.page = undefined;
}

async function postWebAdminWrite(context: ToolContext, path: string, body: Record<string, unknown>, successMessage: string) {
  const page = getPage(context);
  if (!page) {
    return fail("VALIDATION_ERROR", "webadmin.open must run before this WebAdmin write tool.");
  }
  const base = context.webAdmin.baseUrl ?? new URL(context.config.webAdminUrl).origin;
  try {
    ensureAllowedUrl(base, context.config);
    const result = await call(page, "evaluate", [async (payload: { path: string; body: Record<string, unknown> }) => {
      const capabilitiesResponse = await fetch("/api/webadmin/write/capabilities", {
        method: "GET",
        credentials: "same-origin"
      });
      const capabilitiesJson = await capabilitiesResponse.json();
      const csrfToken = capabilitiesJson?.data?.csrf?.token ?? "";
      const response = await fetch(payload.path, {
        method: "POST",
        credentials: "same-origin",
        headers: {
          "Content-Type": "application/json",
          "X-TZZ-WebAdmin-CSRF": csrfToken
        },
        body: JSON.stringify(payload.body)
      });
      const text = await response.text();
      let json: unknown;
      try {
        json = JSON.parse(text);
      } catch {
        json = { rawText: text };
      }
      return {
        status: response.status,
        ok: response.ok,
        json
      };
    }, { path, body }]) as JsonObject;
    const status = Number(result.status ?? 0);
    const envelope = isJsonObject(result.json) ? result.json : {};
    const data = isJsonObject(envelope.data) ? envelope.data : {};
    if (status < 200 || status >= 300 || envelope.ok === false || data.success === false) {
      return fail("COMMAND_FAILED", String(data.message ?? "WebAdmin password write failed."), {
        status,
        result: data
      });
    }
    return ok(successMessage, {
      status,
      result: data
    });
  } catch (error) {
    return fail("BROWSER_ERROR", errorMessage(error));
  }
}

async function waitForLoad(page: unknown, timeoutMs: number): Promise<void> {
  await call(page, "waitForLoadState", ["domcontentloaded", { timeout: timeoutMs }]).catch(() => undefined);
  await call(page, "waitForTimeout", [250]).catch(() => undefined);
}

async function clickSelector(page: unknown, selector: string, timeoutMs = 10000): Promise<void> {
  const locator = firstLocator(page, selector);
  await call(locator, "waitFor", [{ state: "visible", timeout: timeoutMs }]);
  const enabled = Boolean(await call(locator, "isEnabled", []));
  if (!enabled) {
    throw new Error(`Element is not enabled: ${selector}`);
  }
  await call(locator, "click", [{ timeout: timeoutMs }]);
}

async function fillSelector(page: unknown, selector: string, value: string, timeoutMs = 10000): Promise<void> {
  const locator = await requireVisibleEnabled(page, selector, timeoutMs);
  await call(locator, "fill", [value, { timeout: timeoutMs }]);
}

async function requireVisibleEnabled(page: unknown, selector: string, timeoutMs = 10000): Promise<unknown> {
  const locator = firstLocator(page, selector);
  try {
    await call(locator, "waitFor", [{ state: "visible", timeout: timeoutMs }]);
  } catch {
    throw new WebAdminToolError("SELECTOR_NOT_FOUND", `WebAdmin selector not found or not visible: ${selector}`);
  }
  const enabled = Boolean(await call(locator, "isEnabled", []));
  if (!enabled) {
    throw new WebAdminToolError("BUTTON_DISABLED", `WebAdmin element is disabled: ${selector}`);
  }
  return locator;
}

async function optionalText(page: unknown, selector: string): Promise<string> {
  try {
    const locator = firstLocator(page, selector);
    return String(await call(locator, "textContent", []) ?? "").trim();
  } catch {
    return "";
  }
}

function firstLocator(page: unknown, selector: string): unknown {
  const locator = callSync(page, "locator", [selector]);
  return callSync(locator, "first") ?? locator;
}

function getPage(context: ToolContext): unknown | undefined {
  return context.webAdmin.page;
}

function getRequest(route: unknown): unknown {
  return callSync(route, "request") ?? {};
}

async function call(target: unknown, method: string, args: unknown[]): Promise<unknown> {
  const fn = getMethod(target, method);
  return await fn.apply(target, args);
}

function callSync(target: unknown, method: string, args: unknown[] = []): unknown {
  try {
    const fn = getMethod(target, method);
    return fn.apply(target, args);
  } catch {
    return undefined;
  }
}

function getMethod(target: unknown, method: string): (...args: unknown[]) => unknown {
  if (!target || typeof target !== "object") {
    throw new Error(`Cannot call ${method} on empty target`);
  }
  const value = (target as Record<string, unknown>)[method];
  if (typeof value !== "function") {
    throw new Error(`Target does not support method: ${method}`);
  }
  return value as (...args: unknown[]) => unknown;
}

function requiredString(args: JsonObject, key: string): string {
  const value = args[key];
  if (typeof value !== "string" || !value.trim()) {
    throw new Error(`${key} is required`);
  }
  return value;
}

type AuthProbe = {
  authenticated: boolean;
  status: number;
  username: string;
  role: string;
  message: string;
  currentUrl: string;
  loginMessage: string;
};

async function waitForAuthenticated(page: unknown, timeoutMs: number): Promise<AuthProbe> {
  const started = Date.now();
  let latest = await probeAuth(page);
  while (Date.now() - started < timeoutMs) {
    latest = await probeAuth(page);
    if (latest.authenticated) {
      return latest;
    }
    await call(page, "waitForTimeout", [400]).catch(() => undefined);
  }
  return latest;
}

async function probeAuth(page: unknown): Promise<AuthProbe> {
  const currentUrl = String(callSync(page, "url") ?? "");
  const loginMessage = await optionalText(page, "#message");
  try {
    const result = await call(page, "evaluate", [async () => {
      const response = await fetch("/api/auth/me", {
        method: "GET",
        credentials: "same-origin"
      });
      const text = await response.text();
      let json: unknown;
      try {
        json = JSON.parse(text);
      } catch {
        json = { rawText: text };
      }
      return {
        status: response.status,
        ok: response.ok,
        json
      };
    }]) as JsonObject;
    const status = Number(result.status ?? 0);
    const envelope = isJsonObject(result.json) ? result.json : {};
    const data = isJsonObject(envelope.data) ? envelope.data : {};
    const error = isJsonObject(envelope.error) ? envelope.error : {};
    const username = typeof data.username === "string" ? data.username : "";
    const role = typeof data.role === "string" ? data.role : "";
    const message = String(error.message ?? loginMessage ?? "");
    return {
      authenticated: status >= 200 && status < 300 && envelope.ok !== false && Boolean(username),
      status,
      username,
      role,
      message,
      currentUrl,
      loginMessage
    };
  } catch (error) {
    return {
      authenticated: false,
      status: 0,
      username: "",
      role: "",
      message: errorMessage(error) || loginMessage,
      currentUrl,
      loginMessage
    };
  }
}

function isLoginUrl(currentUrl: string): boolean {
  try {
    return new URL(currentUrl).pathname === "/login";
  } catch {
    return currentUrl.includes("/login");
  }
}

class WebAdminToolError extends Error {
  constructor(readonly code: Exclude<ToolErrorCode, "OK">, message: string) {
    super(message);
  }
}

function errorMessage(error: unknown): string {
  return redactSecrets(error instanceof Error ? error.message : String(error));
}

function isJsonObject(value: unknown): value is JsonObject {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}
