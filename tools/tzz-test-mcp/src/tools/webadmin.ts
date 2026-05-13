import { readFileSync, writeFileSync } from "node:fs";
import type { JsonObject, ToolContext, ToolDefinition } from "../types.js";
import { fail, ok } from "../results.js";
import { ensureAllowedUrl, ensureHashRoute, ensureReportPath, ensureResponsiveReportPath, redactSecrets, safeName } from "../safety.js";
import type { ToolErrorCode } from "../results.js";

type PlaywrightModule = {
  chromium: {
    launch(options: { headless: boolean }): Promise<unknown>;
  };
};

type ScreenshotScale = "css" | "device";

type ViewportProfile = {
  width: number;
  height: number;
  label: string;
  profileName: string;
  deviceScaleFactor: number;
  screenshotScale: ScreenshotScale;
  note?: string;
};

export function webAdminTools(): ToolDefinition[] {
  return [
    webAdminOpenTool(),
    webAdminLoginTool(),
    webAdminChangePasswordTool(),
    webAdminOwnerSetPasswordTool(),
    webAdminCloseTool(),
    webAdminGotoTool(),
    webAdminSetViewportTool(),
    webAdminScreenshotTool(),
    webAdminResponsiveScreenshotTool(),
    webAdminResponsiveMatrixTool(),
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

function webAdminCloseTool(): ToolDefinition {
  return {
    name: "webadmin.close",
    description: "Close the current Playwright WebAdmin browser/page. This is idempotent and does not delete reports, screenshots, or logs.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        resetDiagnostics: { type: "boolean" }
      }
    },
    async handler(args, context) {
      const hadBrowser = Boolean(context.webAdmin.browser || context.webAdmin.context || context.webAdmin.page);
      await closeBrowser(context);
      if (args.resetDiagnostics === true) {
        context.webAdmin.consoleErrors = [];
        context.webAdmin.pageErrors = [];
        context.webAdmin.failedRequests = [];
        context.webAdmin.badResponses = [];
        context.webAdmin.screenshots = [];
      }
      return ok(hadBrowser ? "WebAdmin browser closed." : "No WebAdmin browser is open.", {
        closed: hadBrowser,
        browserOpen: false,
        resetDiagnostics: args.resetDiagnostics === true
      });
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
        const profile = defaultViewportProfile();
        const browserContext = await createWebAdminBrowserContext(browser, context, profile);
        const page = await call(browserContext, "newPage", []);
        attachDiagnostics(page, context);
        await call(page, "goto", [allowedUrl.toString(), { waitUntil: "domcontentloaded", timeout: timeoutMs }]);
        await waitForLoad(page, timeoutMs);
        context.webAdmin.playwright = playwright;
        context.webAdmin.browser = browser;
        context.webAdmin.context = browserContext;
        context.webAdmin.page = page;
        context.webAdmin.baseUrl = `${allowedUrl.protocol}//${allowedUrl.host}`;
        context.webAdmin.viewportProfile = viewportProfileJson(profile);
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

function webAdminSetViewportTool(): ToolDefinition {
  return {
    name: "webadmin.set_viewport",
    description: "Set the current Playwright WebAdmin page viewport. This is browser viewport resizing only, not an OS screenshot or coordinate click.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        width: { type: "number", minimum: 320, maximum: 7680 },
        height: { type: "number", minimum: 240, maximum: 4320 },
        deviceScaleFactor: { type: "number", minimum: 0.5, maximum: 4 },
        timeoutMs: { type: "number", minimum: 100, maximum: 10000 }
      },
      required: ["width", "height"]
    },
    async handler(args, context) {
      const page = getPage(context);
      if (!page) {
        return fail("VALIDATION_ERROR", "webadmin.open must run before webadmin.set_viewport.");
      }
      const profile = parseViewportProfile(args);
      if (!profile) {
        return fail("VALIDATION_ERROR", "width/height/deviceScaleFactor must be finite numbers inside the allowed viewport/profile range.");
      }
      try {
        const applied = await applyWebAdminViewportProfile(context, profile, optionalNumber(args, "timeoutMs") ?? 500);
        if (applied) {
          return applied;
        }
        return ok("WebAdmin viewport set.", {
          viewport: viewportProfileJson(profile),
          expectedPhysicalWidth: expectedPhysicalWidth(profile),
          expectedPhysicalHeight: expectedPhysicalHeight(profile),
          currentUrl: String(callSync(getPage(context), "url") ?? ""),
          noOsScreenshot: true,
          noCoordinateClicking: true
        });
      } catch (error) {
        return fail("BROWSER_ERROR", errorMessage(error));
      }
    }
  };
}

function webAdminResponsiveScreenshotTool(): ToolDefinition {
  return {
    name: "webadmin.responsive_screenshot",
    description: "Capture one localhost WebAdmin route at one viewport and return the screenshot path plus diagnostics.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        hashRoute: { type: "string" },
        route: { type: "string" },
        profile: { type: "string" },
        width: { type: "number", minimum: 320, maximum: 7680 },
        height: { type: "number", minimum: 240, maximum: 4320 },
        deviceScaleFactor: { type: "number", minimum: 0.5, maximum: 4 },
        screenshotScale: { type: "string", enum: ["css", "device"] },
        name: { type: "string" },
        fullPage: { type: "boolean" },
        timeoutMs: { type: "number", minimum: 1000, maximum: 120000 }
      }
    },
    async handler(args, context) {
      const profile = parseViewportProfile(args);
      if (!profile) {
        return fail("VALIDATION_ERROR", "width/height/deviceScaleFactor must be finite numbers inside the allowed viewport/profile range.");
      }
      const routeInput = typeof args.hashRoute === "string" ? args.hashRoute : args.route;
      let route = "#/dashboard";
      if (typeof routeInput === "string" && routeInput.trim()) {
        try {
          route = ensureHashRoute(routeInput);
        } catch (error) {
          return fail("VALIDATION_ERROR", errorMessage(error));
        }
      }
      const timeoutMs = optionalNumber(args, "timeoutMs") ?? 30000;
      const ready = await ensureWebAdminLoggedIn(context, timeoutMs);
      if (ready) {
        return ready;
      }
      const capture = await captureResponsiveScreenshot(context, route, profile, String(args.name ?? ""), args.fullPage === true, timeoutMs);
      return capture.ok
        ? ok("WebAdmin responsive screenshot captured.", capture.data)
        : fail(capture.code, capture.message, capture.data);
    }
  };
}

function webAdminResponsiveMatrixTool(): ToolDefinition {
  return {
    name: "webadmin.responsive_matrix",
    description: "Capture a localhost WebAdmin responsive screenshot matrix across routes and viewports. It does not judge visuals; user review is required before checkpoint.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        routes: { type: "array", items: { type: "string" } },
        profiles: {
          type: "array",
          items: {
            type: "object",
            additionalProperties: false,
            properties: {
              name: { type: "string" },
              profile: { type: "string" },
              width: { type: "number" },
              height: { type: "number" },
              label: { type: "string" },
              deviceScaleFactor: { type: "number" },
              screenshotScale: { type: "string", enum: ["css", "device"] },
              note: { type: "string" }
            },
            required: ["width", "height"]
          }
        },
        sizes: {
          type: "array",
          items: {
            type: "object",
            additionalProperties: false,
            properties: {
              name: { type: "string" },
              profile: { type: "string" },
              width: { type: "number" },
              height: { type: "number" },
              label: { type: "string" },
              deviceScaleFactor: { type: "number" },
              screenshotScale: { type: "string", enum: ["css", "device"] },
              note: { type: "string" }
            },
            required: ["width", "height"]
          }
        },
        name: { type: "string" },
        fullPage: { type: "boolean" },
        timeoutMs: { type: "number", minimum: 1000, maximum: 120000 }
      }
    },
    async handler(args, context) {
      let routes: string[];
      try {
        routes = parseRoutes(args.routes);
      } catch (error) {
        return fail("VALIDATION_ERROR", errorMessage(error));
      }
      const sizes = parseViewportList(args.profiles ?? args.sizes);
      const timeoutMs = optionalNumber(args, "timeoutMs") ?? 30000;
      const ready = await ensureWebAdminLoggedIn(context, timeoutMs);
      const started = new Date().toISOString();
      const cells: JsonObject[] = [];
      const warnings: string[] = [];
      if (ready) {
        warnings.push(String(ready.structuredContent?.message ?? "WebAdmin login/open failed."));
      } else {
        for (const size of sizes) {
          for (const route of routes) {
            const capture = await captureResponsiveScreenshot(context, route, size, String(args.name ?? "webadmin-responsive"), args.fullPage === true, timeoutMs);
            cells.push(capture.data);
            const warning = typeof capture.data.warning === "string" ? capture.data.warning : "";
            if (warning) {
              warnings.push(`${route} ${size.profileName}: ${warning}`);
            }
          }
        }
      }
      const passed = cells.filter((entry) => entry.ok === true).length;
      const failed = cells.filter((entry) => entry.ok !== true).length;
      const reportPath = writeResponsiveWebAdminReport(context, {
        name: safeName(args.name ?? "webadmin-responsive-matrix", "webadmin-responsive-matrix"),
        started,
        finished: new Date().toISOString(),
        routes,
        profiles: sizes.map(viewportProfileJson),
        cells,
        warnings,
        passed,
        failed
      });
      return ok(failed > 0 ? "WebAdmin responsive matrix completed with failures." : "WebAdmin responsive matrix captured.", {
        reportPath,
        routes,
        profiles: sizes.map(viewportProfileJson),
        screenshots: cells.map((entry) => entry.screenshotPath).filter((value): value is string => typeof value === "string" && value.length > 0),
        passed,
        failed,
        warnings,
        manualVisualReviewRequired: true,
        userApprovalRequiredBeforeCheckpoint: true
      });
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

async function saveScreenshot(context: ToolContext, name: string, fullPage: boolean, screenshotScale?: ScreenshotScale): Promise<string> {
  const page = getPage(context);
  if (!page) {
    throw new Error("No WebAdmin page is open");
  }
  const file = ensureReportPath(context.config, name, ".png");
  const options: JsonObject = { path: file, fullPage, timeout: 30000 };
  if (screenshotScale) {
    options.scale = screenshotScale;
  }
  await withTimeout(call(page, "screenshot", [options]), 35000, "WebAdmin screenshot timed out");
  context.webAdmin.screenshots.push(file);
  return file;
}

const DEFAULT_WEBADMIN_VIEWPORTS: ViewportProfile[] = [
  { width: 854, height: 480, label: "small_854x480", profileName: "small_854x480", deviceScaleFactor: 1, screenshotScale: "device" },
  { width: 1280, height: 720, label: "hd_1280x720_100", profileName: "hd_1280x720_100", deviceScaleFactor: 1, screenshotScale: "device" },
  { width: 1920, height: 1080, label: "fhd_1920x1080_100", profileName: "fhd_1920x1080_100", deviceScaleFactor: 1, screenshotScale: "device" },
  { width: 2560, height: 1440, label: "qhd_2560x1440_100", profileName: "qhd_2560x1440_100", deviceScaleFactor: 1, screenshotScale: "device" },
  {
    width: 2560,
    height: 1440,
    label: "uhd_4k_150_scaled",
    profileName: "uhd_4k_150_scaled",
    deviceScaleFactor: 1.5,
    screenshotScale: "device",
    note: "Simulates a 3840x2160 display at Windows 150% scaling."
  },
  {
    width: 1920,
    height: 1080,
    label: "uhd_4k_200_scaled",
    profileName: "uhd_4k_200_scaled",
    deviceScaleFactor: 2,
    screenshotScale: "device",
    note: "Simulates a 3840x2160 display at Windows 200% scaling."
  },
  {
    width: 3840,
    height: 2160,
    label: "uhd_3840x2160_css_extreme",
    profileName: "uhd_3840x2160_css_extreme",
    deviceScaleFactor: 1,
    screenshotScale: "device",
    note: "4K CSS viewport / extreme workspace; this is not the normal visual scale of a 4K display using 150% or 200% OS scaling."
  }
];

const DEFAULT_WEBADMIN_ROUTES = [
  "#/dashboard",
  "#/devices",
  "#/virtual-block-devices",
  "#/doctor",
  "#/history",
  "#/settings"
];

async function setViewportAndSettle(page: unknown, width: number, height: number, settleMs: number): Promise<void> {
  await call(page, "setViewportSize", [{ width, height }]);
  await call(page, "waitForTimeout", [Math.max(100, Math.min(5000, settleMs))]).catch(() => undefined);
}

async function createWebAdminBrowserContext(browser: unknown, context: ToolContext, profile: ViewportProfile, storageState?: unknown): Promise<unknown> {
  const options: JsonObject = {
    ignoreHTTPSErrors: false,
    viewport: { width: profile.width, height: profile.height },
    deviceScaleFactor: profile.deviceScaleFactor
  };
  if (storageState && typeof storageState === "object") {
    options.storageState = storageState as JsonObject;
  }
  const browserContext = await call(browser, "newContext", [options]);
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
  return browserContext;
}

async function applyWebAdminViewportProfile(
  context: ToolContext,
  profile: ViewportProfile,
  settleMs: number
) {
  const page = getPage(context);
  if (!page) {
    return fail("VALIDATION_ERROR", "webadmin.open must run before webadmin.set_viewport.");
  }
  const active = context.webAdmin.viewportProfile;
  const activeScale = typeof active?.deviceScaleFactor === "number" ? active.deviceScaleFactor : 1;
  const needsContextRecreate = Math.abs(activeScale - profile.deviceScaleFactor) > 0.0001;
  if (!needsContextRecreate) {
    await setViewportAndSettle(page, profile.width, profile.height, settleMs);
    context.webAdmin.viewportProfile = viewportProfileJson(profile);
    return undefined;
  }
  const browser = context.webAdmin.browser;
  const oldContext = context.webAdmin.context;
  const baseUrl = context.webAdmin.baseUrl ?? new URL(context.config.webAdminUrl).origin;
  if (!browser) {
    return fail("BROWSER_ERROR", "WebAdmin browser is not open.");
  }
  const storageState = oldContext ? await call(oldContext, "storageState", []).catch(() => undefined) : undefined;
  context.webAdmin.context = undefined;
  context.webAdmin.page = undefined;
  if (oldContext) {
    await withTimeout(call(oldContext, "close", []), 5000, "WebAdmin context close timed out").catch(() => undefined);
  }
  const browserContext = await createWebAdminBrowserContext(browser, context, profile, storageState);
  const newPage = await call(browserContext, "newPage", []);
  attachDiagnostics(newPage, context);
  await call(newPage, "goto", [`${baseUrl}/app#/dashboard`, { waitUntil: "domcontentloaded", timeout: 30000 }]);
  await waitForLoad(newPage, 30000);
  context.webAdmin.context = browserContext;
  context.webAdmin.page = newPage;
  context.webAdmin.baseUrl = baseUrl;
  context.webAdmin.viewportProfile = viewportProfileJson(profile);
  return undefined;
}

async function ensureWebAdminLoggedIn(context: ToolContext, timeoutMs: number) {
  let page = getPage(context);
  if (!page) {
    const openTool = context.tools.get("webadmin.open");
    if (!openTool) {
      return fail("TOOL_UNAVAILABLE", "webadmin.open is unavailable.");
    }
    const opened = await openTool.handler({ timeoutMs }, context);
    if (opened.isError) {
      return opened;
    }
    page = getPage(context);
  }
  if (!page) {
    return fail("BROWSER_ERROR", "WebAdmin page did not open.");
  }
  const auth = await probeAuth(page);
  if (auth.authenticated) {
    return undefined;
  }
  const loginTool = context.tools.get("webadmin.login");
  if (!loginTool) {
    return fail("TOOL_UNAVAILABLE", "webadmin.login is unavailable.");
  }
  const login = await loginTool.handler({}, context);
  return login.isError ? login : undefined;
}

async function captureResponsiveScreenshot(
  context: ToolContext,
  route: string,
  size: ViewportProfile,
  name: string,
  fullPage: boolean,
  timeoutMs: number
): Promise<{ ok: true; data: JsonObject } | { ok: false; code: Exclude<ToolErrorCode, "OK">; message: string; data: JsonObject }> {
  if (!getPage(context)) {
    return {
      ok: false,
      code: "BROWSER_ERROR",
      message: "No WebAdmin page is open.",
      data: cellBase(route, size, false)
    };
  }
  const before = diagnosticsCounts(context);
  const started = Date.now();
  const perStepTimeoutMs = Math.max(3000, Math.min(timeoutMs, 15000));
  try {
    const base = context.webAdmin.baseUrl ?? new URL(context.config.webAdminUrl).origin;
    const applied = await applyWebAdminViewportProfile(context, size, 500);
    if (applied) {
      const code = typeof applied.structuredContent?.code === "string" ? applied.structuredContent.code as Exclude<ToolErrorCode, "OK"> : "BROWSER_ERROR";
      return { ok: false, code, message: String(applied.structuredContent?.message ?? "WebAdmin viewport profile failed."), data: cellBase(route, size, false) };
    }
    const ready = await ensureWebAdminLoggedIn(context, perStepTimeoutMs);
    if (ready) {
      const code = typeof ready.structuredContent?.code === "string" ? ready.structuredContent.code as Exclude<ToolErrorCode, "OK"> : "AUTH_REQUIRED";
      return { ok: false, code, message: String(ready.structuredContent?.message ?? "WebAdmin login/open failed."), data: cellBase(route, size, false) };
    }
    const activePage = getPage(context);
    if (!activePage) {
      return { ok: false, code: "BROWSER_ERROR", message: "No WebAdmin page is open.", data: cellBase(route, size, false) };
    }
    await call(activePage, "goto", [`${base}/app${route}`, { waitUntil: "domcontentloaded", timeout: perStepTimeoutMs }]);
    await waitForLoad(activePage, perStepTimeoutMs);
    const auth = await probeAuth(activePage);
    if (!auth.authenticated || isLoginUrl(String(callSync(activePage, "url") ?? ""))) {
      const data = cellBase(route, size, false);
      data.code = "AUTH_REQUIRED";
      data.message = "当前未登录，无法截图目标 WebAdmin route。";
      data.currentUrl = String(callSync(activePage, "url") ?? "");
      data.diagnostics = diagnosticsDelta(context, before);
      return { ok: false, code: "AUTH_REQUIRED", message: String(data.message), data };
    }
    const routeSlug = route.replace(/^#\//, "").replace(/[^\w.-]+/g, "-") || "root";
    const screenshotPath = await saveScreenshot(context, `${safeName(name || "responsive", "responsive")}-${routeSlug}-${size.profileName}-${size.width}x${size.height}-dsf${String(size.deviceScaleFactor).replace(".", "-")}`, fullPage, size.screenshotScale);
    const screenshotSize = readPngDimensions(screenshotPath);
    const layout = await responsiveLayoutProbe(activePage).catch((error) => ({ probeError: errorMessage(error) }));
    const data = cellBase(route, size, true);
    data.code = "OK";
    data.message = "OK";
    data.currentUrl = String(callSync(activePage, "url") ?? "");
    data.title = String(await call(activePage, "title", []).catch(() => ""));
    data.screenshotPath = screenshotPath;
    data.fullPage = fullPage;
    if (screenshotSize) {
      data.screenshotActualWidth = screenshotSize.width;
      data.screenshotActualHeight = screenshotSize.height;
      if (!fullPage && size.screenshotScale === "device" && (screenshotSize.width !== expectedPhysicalWidth(size) || screenshotSize.height !== expectedPhysicalHeight(size))) {
        data.warning = `Screenshot actual size ${screenshotSize.width}x${screenshotSize.height} differs from expected physical ${expectedPhysicalWidth(size)}x${expectedPhysicalHeight(size)}.`;
      }
    } else {
      data.warning = "Unable to read PNG screenshot dimensions for physical-size verification.";
    }
    data.durationMs = Date.now() - started;
    data.diagnostics = diagnosticsDelta(context, before);
    data.layout = layout as JsonObject;
    return { ok: true, data };
  } catch (error) {
    const data = cellBase(route, size, false);
    data.code = "BROWSER_ERROR";
    data.message = errorMessage(error);
    data.durationMs = Date.now() - started;
    data.currentUrl = String(callSync(getPage(context), "url") ?? "");
    data.diagnostics = diagnosticsDelta(context, before);
    return { ok: false, code: "BROWSER_ERROR", message: String(data.message), data };
  }
}

function writeResponsiveWebAdminReport(context: ToolContext, summary: JsonObject): string {
  const reportPath = ensureResponsiveReportPath(context.config, summary.name ?? "webadmin-responsive-matrix");
  const cells = Array.isArray(summary.cells) ? summary.cells as JsonObject[] : [];
  const lines = [
    `# WebAdmin Responsive Screenshot Matrix`,
    "",
    `- Started: ${summary.started ?? ""}`,
    `- Finished: ${summary.finished ?? ""}`,
    `- Passed: ${summary.passed ?? 0}`,
    `- Failed: ${summary.failed ?? 0}`,
    "",
    "本阶段不做自动图像识别，截图需要用户人工验收。用户确认前不得 checkpoint。",
    "",
    "WebAdmin 4K 视觉验收不能只看 3840x2160 CSS viewport。真实 4K 显示器常见 Windows / browser scaling 是 150% 或 200%，因此必须同时检查 4K scaled profiles，例如 2560x1440 @ 1.5 和 1920x1080 @ 2。`uhd_3840x2160_css_extreme` 只代表极端 CSS 工作区，不等价于普通 4K 缩放视觉。",
    "",
    "## Screenshots",
    "",
    "| Route | Profile | CSS viewport | DSF | Expected physical | Actual screenshot | Result | Screenshot | Diagnostics |",
    "|---|---|---:|---:|---:|---:|---|---|---|"
  ];
  for (const cell of cells) {
    const diagnostics = isJsonObject(cell.diagnostics) ? cell.diagnostics : {};
    const errorCount = Number(diagnostics.consoleErrors ?? 0) + Number(diagnostics.pageErrors ?? 0) + Number(diagnostics.failedRequests ?? 0) + Number(diagnostics.badResponses ?? 0);
    lines.push(`| ${cell.route ?? ""} | ${cell.profileName ?? cell.label ?? ""} | ${cell.cssViewportWidth ?? cell.width ?? ""}x${cell.cssViewportHeight ?? cell.height ?? ""} | ${cell.deviceScaleFactor ?? ""} | ${cell.expectedPhysicalWidth ?? ""}x${cell.expectedPhysicalHeight ?? ""} | ${cell.screenshotActualWidth ?? "unknown"}x${cell.screenshotActualHeight ?? "unknown"} | ${cell.ok === true ? "PASS" : `FAIL ${cell.code ?? ""}`} | ${cell.screenshotPath ?? ""} | ${errorCount} new issues |`);
  }
  const warnings = Array.isArray(summary.warnings) ? summary.warnings : [];
  if (warnings.length > 0) {
    lines.push("", "## Warnings", "");
    for (const warning of warnings) {
      lines.push(`- ${redactSecrets(String(warning))}`);
    }
  }
  lines.push("", "## Raw Summary", "", "```json", JSON.stringify(summary, null, 2), "```", "");
  writeFileSync(reportPath, lines.join("\n"), "utf8");
  return reportPath;
}

function cellBase(route: string, size: ViewportProfile, cellOk: boolean): JsonObject {
  return {
    ok: cellOk,
    route,
    width: size.width,
    height: size.height,
    label: size.label,
    profileName: size.profileName,
    cssViewportWidth: size.width,
    cssViewportHeight: size.height,
    deviceScaleFactor: size.deviceScaleFactor,
    screenshotScale: size.screenshotScale,
    expectedPhysicalWidth: expectedPhysicalWidth(size),
    expectedPhysicalHeight: expectedPhysicalHeight(size),
    profileNote: size.note ?? "",
    manualVisualReviewRequired: true
  };
}

async function responsiveLayoutProbe(page: unknown): Promise<JsonObject> {
  return await call(page, "evaluate", [() => {
    const body = document.body;
    const documentElement = document.documentElement;
    return {
      innerWidth: window.innerWidth,
      innerHeight: window.innerHeight,
      bodyScrollWidth: body ? body.scrollWidth : 0,
      bodyClientWidth: body ? body.clientWidth : 0,
      documentScrollWidth: documentElement ? documentElement.scrollWidth : 0,
      documentClientWidth: documentElement ? documentElement.clientWidth : 0,
      horizontalOverflow: Boolean(documentElement && documentElement.scrollWidth > documentElement.clientWidth + 2),
      mainTextLength: body ? Math.min(body.innerText.length, 200000) : 0
    };
  }]) as JsonObject;
}

type DiagnosticCounts = {
  consoleErrors: number;
  pageErrors: number;
  failedRequests: number;
  badResponses: number;
};

function diagnosticsCounts(context: ToolContext): DiagnosticCounts {
  return {
    consoleErrors: context.webAdmin.consoleErrors.length,
    pageErrors: context.webAdmin.pageErrors.length,
    failedRequests: context.webAdmin.failedRequests.length,
    badResponses: context.webAdmin.badResponses.length
  };
}

function diagnosticsDelta(context: ToolContext, before: DiagnosticCounts): JsonObject {
  return {
    consoleErrors: context.webAdmin.consoleErrors.length - before.consoleErrors,
    pageErrors: context.webAdmin.pageErrors.length - before.pageErrors,
    failedRequests: context.webAdmin.failedRequests.length - before.failedRequests,
    badResponses: context.webAdmin.badResponses.length - before.badResponses
  };
}

function parseRoutes(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return DEFAULT_WEBADMIN_ROUTES;
  }
  const routes: string[] = [];
  for (const entry of value) {
    if (typeof entry !== "string" || !entry.trim()) {
      continue;
    }
    routes.push(ensureHashRoute(entry));
  }
  return routes.length > 0 ? routes : DEFAULT_WEBADMIN_ROUTES;
}

function parseViewportList(value: unknown): ViewportProfile[] {
  if (!Array.isArray(value)) {
    return DEFAULT_WEBADMIN_VIEWPORTS;
  }
  const sizes = value.map((entry) => isJsonObject(entry) ? parseViewportProfile(entry) : undefined).filter((entry): entry is ViewportProfile => Boolean(entry));
  return sizes.length > 0 ? sizes : DEFAULT_WEBADMIN_VIEWPORTS;
}

function parseViewportProfile(args: JsonObject): ViewportProfile | undefined {
  const profile = typeof args.profile === "string" ? defaultProfileByName(args.profile) : undefined;
  const width = optionalNumber(args, "width");
  const height = optionalNumber(args, "height");
  const resolvedWidth = width ?? profile?.width;
  const resolvedHeight = height ?? profile?.height;
  if (!resolvedWidth || !resolvedHeight || resolvedWidth < 320 || resolvedWidth > 7680 || resolvedHeight < 240 || resolvedHeight > 4320) {
    return undefined;
  }
  const deviceScaleFactor = optionalFloat(args, "deviceScaleFactor") ?? profile?.deviceScaleFactor ?? 1;
  if (deviceScaleFactor < 0.5 || deviceScaleFactor > 4) {
    return undefined;
  }
  const screenshotScale = args.screenshotScale === "css" ? "css" : "device";
  const explicitName = typeof args.name === "string" ? args.name : typeof args.profile === "string" ? args.profile : "";
  const label = typeof args.label === "string" ? args.label : explicitName || profile?.label || `${resolvedWidth}x${resolvedHeight}@${deviceScaleFactor}`;
  const profileName = safeName(label, `${resolvedWidth}x${resolvedHeight}-dsf${String(deviceScaleFactor).replace(".", "-")}`);
  const note = typeof args.note === "string" ? args.note : profile?.note;
  return {
    width: resolvedWidth,
    height: resolvedHeight,
    label: profileName,
    profileName,
    deviceScaleFactor,
    screenshotScale,
    ...(note ? { note } : {})
  };
}

function defaultViewportProfile(): ViewportProfile {
  return DEFAULT_WEBADMIN_VIEWPORTS[2] ?? {
    width: 1920,
    height: 1080,
    label: "fhd_1920x1080_100",
    profileName: "fhd_1920x1080_100",
    deviceScaleFactor: 1,
    screenshotScale: "device"
  };
}

function defaultProfileByName(name: string): ViewportProfile | undefined {
  const normalized = safeName(name, "");
  return DEFAULT_WEBADMIN_VIEWPORTS.find((profile) => profile.profileName === normalized || profile.label === normalized);
}

function viewportProfileJson(profile: ViewportProfile): JsonObject {
  return {
    name: profile.profileName,
    label: profile.label,
    width: profile.width,
    height: profile.height,
    cssViewportWidth: profile.width,
    cssViewportHeight: profile.height,
    deviceScaleFactor: profile.deviceScaleFactor,
    screenshotScale: profile.screenshotScale,
    expectedPhysicalWidth: expectedPhysicalWidth(profile),
    expectedPhysicalHeight: expectedPhysicalHeight(profile),
    note: profile.note ?? ""
  };
}

function expectedPhysicalWidth(profile: ViewportProfile): number {
  return Math.round(profile.width * (profile.screenshotScale === "device" ? profile.deviceScaleFactor : 1));
}

function expectedPhysicalHeight(profile: ViewportProfile): number {
  return Math.round(profile.height * (profile.screenshotScale === "device" ? profile.deviceScaleFactor : 1));
}

function readPngDimensions(file: string): { width: number; height: number } | undefined {
  try {
    const buffer = readFileSync(file);
    if (buffer.length < 24) {
      return undefined;
    }
    const signature = buffer.subarray(0, 8).toString("hex");
    if (signature !== "89504e470d0a1a0a") {
      return undefined;
    }
    return {
      width: buffer.readUInt32BE(16),
      height: buffer.readUInt32BE(20)
    };
  } catch {
    return undefined;
  }
}

function optionalNumber(args: JsonObject, key: string): number | undefined {
  const value = args[key];
  return typeof value === "number" && Number.isFinite(value) ? Math.trunc(value) : undefined;
}

function optionalFloat(args: JsonObject, key: string): number | undefined {
  const value = args[key];
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

async function closeBrowser(context: ToolContext): Promise<void> {
  const page = context.webAdmin.page;
  const browserContext = context.webAdmin.context;
  const browser = context.webAdmin.browser;
  context.webAdmin.browser = undefined;
  context.webAdmin.context = undefined;
  context.webAdmin.page = undefined;
  context.webAdmin.baseUrl = undefined;
  context.webAdmin.viewportProfile = undefined;
  if (page) {
    await withTimeout(call(page, "close", []), 5000, "WebAdmin page close timed out").catch(() => undefined);
  }
  if (browserContext) {
    await withTimeout(call(browserContext, "close", []), 5000, "WebAdmin context close timed out").catch(() => undefined);
  }
  if (browser) {
    await withTimeout(call(browser, "close", []), 5000, "WebAdmin browser close timed out").catch(() => undefined);
  }
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
    const result = await withTimeout(call(page, "evaluate", [async () => {
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
    }]), 10000, "WebAdmin auth probe timed out") as JsonObject;
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

async function withTimeout<T>(promise: Promise<T>, timeoutMs: number, message: string): Promise<T> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  try {
    return await Promise.race([
      promise,
      new Promise<T>((_resolve, reject) => {
        timer = setTimeout(() => reject(new Error(message)), timeoutMs);
      })
    ]);
  } finally {
    if (timer) {
      clearTimeout(timer);
    }
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
