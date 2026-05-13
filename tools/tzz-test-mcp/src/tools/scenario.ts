import { readdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import path from "node:path";
import type { Json, JsonObject, ToolCallResult, ToolContext, ToolDefinition } from "../types.js";
import { fail, ok, sanitizeJsonObject, type ToolErrorCode } from "../results.js";
import { ensureScenarioReportPath, redactSecrets, scenarioReportsDir } from "../safety.js";

const SCENARIO_NAMES = [
  "basic_environment",
  "vbd_right_click",
  "single_item_submit_basic",
  "container_template_basic"
] as const;

type ScenarioName = typeof SCENARIO_NAMES[number];
type CleanupMode = "none" | "browser" | "minecraft" | "all";

type ScenarioOptions = {
  scenarioName: ScenarioName;
  worldName: string;
  player: string;
  keepClientOpen: boolean;
  saveReport: boolean;
  screenshot: boolean;
};

type StepResult = {
  id: string;
  tool: string;
  ok: boolean;
  code: string;
  message: string;
  durationMs: number;
  optional: boolean;
  structuredContent: JsonObject;
};

type ScenarioState = {
  context: ToolContext;
  options: ScenarioOptions;
  startedAt: number;
  steps: StepResult[];
  failedStepId: string;
  failedTool: string;
  failureCode: Exclude<ToolErrorCode, "OK">;
  failureMessage: string;
  player: string;
  dimension: string;
  vbdDeviceId: string;
  receiverDeviceId: string;
  channel: string;
  reportPath: string;
  cleanup: JsonObject;
};

const ALLOWED_SCENARIO_TOOLS = new Set([
  "health.check",
  "repo.status",
  "logs.tail",
  "minecraft.start_client",
  "minecraft.status",
  "minecraft.stop",
  "minecraft.wait_webadmin",
  "minecraft.wait_world",
  "minecraft.wait_testbridge",
  "minecraft.prepare_test_world",
  "minecraft.players",
  "minecraft.set_block",
  "minecraft.clear_area",
  "minecraft.give_item",
  "minecraft.clear_inventory",
  "minecraft.set_main_hand",
  "minecraft.use_block",
  "minecraft.inspect_device",
  "minecraft.signal_history",
  "minecraft.doctor_issues",
  "minecraft.command",
  "minecraft.gui_current",
  "minecraft.gui_slots",
  "minecraft.gui_put_item",
  "minecraft.gui_clear_slot",
  "minecraft.gui_set_count",
  "minecraft.gui_save",
  "minecraft.gui_cancel",
  "webadmin.open",
  "webadmin.login",
  "webadmin.goto",
  "webadmin.screenshot",
  "webadmin.console_errors",
  "webadmin.click",
  "webadmin.fill",
  "webadmin.text",
  "webadmin.close"
]);

const DEFAULT_WORLD_NAME = "TZZ_MCP_TEST_WORLD";
const DEFAULT_DIMENSION = "minecraft:overworld";
const DEFAULT_POSITIONS = {
  rightClickVbd: { x: 2, y: -57, z: 0 },
  rightClickReceiver: { x: 4, y: -57, z: 0 },
  singleVbd: { x: 6, y: -57, z: 0 },
  singleReceiver: { x: 8, y: -57, z: 0 },
  containerVbd: { x: 10, y: -57, z: 0 },
  containerReceiver: { x: 12, y: -57, z: 0 }
};

export function scenarioTools(): ToolDefinition[] {
  return [
    scenarioListTool(),
    scenarioRunTool(),
    scenarioReportTool(),
    scenarioCleanupTool()
  ];
}

function scenarioListTool(): ToolDefinition {
  return {
    name: "scenario.list",
    description: "List built-in local Test MCP scenario smoke tests.",
    inputSchema: { type: "object", additionalProperties: false, properties: {} },
    readOnlyHint: true,
    async handler() {
      return ok("Scenario list returned.", {
        scenarios: [
          scenarioInfo("basic_environment", "Start or reuse runClient, wait for WebAdmin/TestBridge, login WebAdmin, capture dashboard diagnostics."),
          scenarioInfo("vbd_right_click", "Create a bounded test VBD/receiver scene and assert production right-click emits signal history."),
          scenarioInfo("single_item_submit_basic", "Open 7.10 single itemSubmit GUI through fixed WebAdmin session API, edit with GUI bridge, save, inspect, and use_block."),
          scenarioInfo("container_template_basic", "Open 7.9 container template GUI through fixed WebAdmin session API, edit with GUI bridge, save, and inspect itemConditions.")
        ],
        reportsDir: "reports/mcp/scenarios",
        noArbitraryShell: true,
        noGitMutation: true,
        noExternalHost: true,
        noMinecraftGuiCoordinateClicking: true
      });
    }
  };
}

function scenarioRunTool(): ToolDefinition {
  return {
    name: "scenario.run",
    description: "Run one built-in local scenario by composing only existing safe MCP/TestBridge tools.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        scenarioName: { type: "string", enum: [...SCENARIO_NAMES] },
        worldName: { type: "string" },
        player: { type: "string" },
        keepClientOpen: { type: "boolean" },
        saveReport: { type: "boolean" },
        screenshot: { type: "boolean" }
      },
      required: ["scenarioName"]
    },
    async handler(args, context) {
      const parsed = parseScenarioOptions(args);
      if (!parsed.ok) {
        return fail(parsed.code, parsed.message, parsed.data);
      }
      const state = createScenarioState(context, parsed.options);
      try {
        switch (parsed.options.scenarioName) {
          case "basic_environment":
            await runBasicEnvironment(state);
            break;
          case "vbd_right_click":
            await runVbdRightClick(state);
            break;
          case "single_item_submit_basic":
            await runSingleItemSubmitBasic(state);
            break;
          case "container_template_basic":
            await runContainerTemplateBasic(state);
            break;
        }
      } catch (error) {
        if (!state.failedStepId) {
          markFailure(state, "scenario_exception", "scenario.run", "COMMAND_FAILED", errorMessage(error));
        }
      } finally {
        if (!state.options.keepClientOpen) {
          state.cleanup = await cleanup(context, "all");
        } else {
          state.cleanup = { skipped: true, keepClientOpen: true };
        }
        if (state.options.saveReport) {
          state.reportPath = writeScenarioReport(state);
        }
      }

      const resultData = summary(state);
      if (state.failedStepId) {
        return fail(state.failureCode, state.failureMessage, resultData);
      }
      return ok("Scenario completed.", resultData);
    }
  };
}

function scenarioReportTool(): ToolDefinition {
  return {
    name: "scenario.report",
    description: "Read the most recent local scenario report summary from reports/mcp/scenarios.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        nameContains: { type: "string" },
        limit: { type: "number", minimum: 1, maximum: 20 }
      }
    },
    readOnlyHint: true,
    async handler(args, context) {
      const dir = scenarioReportsDir(context.config);
      const needle = typeof args.nameContains === "string" ? args.nameContains.trim().toLowerCase() : "";
      const limit = typeof args.limit === "number" && Number.isFinite(args.limit) ? Math.max(1, Math.min(20, Math.trunc(args.limit))) : 5;
      const reports = readdirSync(dir)
        .filter((entry) => entry.endsWith(".md"))
        .map((entry) => {
          const file = path.join(dir, entry);
          const stat = statSync(file);
          return { file, name: entry, modifiedMs: stat.mtimeMs, sizeBytes: stat.size };
        })
        .filter((entry) => !needle || entry.name.toLowerCase().includes(needle))
        .sort((a, b) => b.modifiedMs - a.modifiedMs)
        .slice(0, limit);
      const latest = reports[0];
      return ok("Scenario reports returned.", {
        reportsDir: dir,
        count: reports.length,
        reports: reports.map((entry) => ({
          path: entry.file,
          name: entry.name,
          sizeBytes: entry.sizeBytes,
          modifiedTime: new Date(entry.modifiedMs).toISOString()
        })),
        latestPreview: latest ? redactSecrets(readFileSync(latest.file, "utf8").slice(0, 4000)) : ""
      });
    }
  };
}

function scenarioCleanupTool(): ToolDefinition {
  return {
    name: "scenario.cleanup",
    description: "Close scenario-managed browser/runtime resources without deleting logs, reports, screenshots, or worlds.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        mode: { type: "string", enum: ["browser", "minecraft", "all", "none"] }
      }
    },
    async handler(args, context) {
      const mode = cleanupMode(args.mode);
      const result = await cleanup(context, mode);
      return ok("Scenario cleanup completed.", result);
    }
  };
}

function scenarioInfo(name: ScenarioName, description: string): JsonObject {
  return {
    name,
    description,
    reportPath: "reports/mcp/scenarios",
    safeToolCompositionOnly: true
  };
}

function parseScenarioOptions(args: JsonObject): { ok: true; options: ScenarioOptions } | { ok: false; code: Exclude<ToolErrorCode, "OK">; message: string; data: JsonObject } {
  const rawName = typeof args.scenarioName === "string" ? args.scenarioName : "";
  if (!SCENARIO_NAMES.includes(rawName as ScenarioName)) {
    return {
      ok: false,
      code: "VALIDATION_ERROR",
      message: `Unknown scenarioName: ${rawName}`,
      data: { allowedScenarios: [...SCENARIO_NAMES] }
    };
  }
  return {
    ok: true,
    options: {
      scenarioName: rawName as ScenarioName,
      worldName: typeof args.worldName === "string" && args.worldName.trim() ? args.worldName.trim() : (process.env.TZZ_TEST_WORLD_NAME ?? DEFAULT_WORLD_NAME),
      player: typeof args.player === "string" && args.player.trim() ? args.player.trim() : "",
      keepClientOpen: args.keepClientOpen === true,
      saveReport: args.saveReport !== false,
      screenshot: args.screenshot !== false
    }
  };
}

function createScenarioState(context: ToolContext, options: ScenarioOptions): ScenarioState {
  return {
    context,
    options,
    startedAt: Date.now(),
    steps: [],
    failedStepId: "",
    failedTool: "",
    failureCode: "COMMAND_FAILED",
    failureMessage: "",
    player: options.player,
    dimension: DEFAULT_DIMENSION,
    vbdDeviceId: "",
    receiverDeviceId: "",
    channel: "",
    reportPath: "",
    cleanup: {}
  };
}

async function runBasicEnvironment(state: ScenarioState): Promise<void> {
  await ensureEnvironment(state, true);
  await step(state, "doctor_issues", "minecraft.doctor_issues", {});
}

async function runVbdRightClick(state: ScenarioState): Promise<void> {
  await ensureEnvironment(state, true);
  state.channel = uniqueChannel("step5.vbd");
  const vbd = DEFAULT_POSITIONS.rightClickVbd;
  const receiver = DEFAULT_POSITIONS.rightClickReceiver;
  await prepareRightClickScene(state, vbd, receiver, state.channel);
  const inspect = await step(state, "inspect_vbd", "minecraft.inspect_device", inspectArgs(state, vbd));
  state.vbdDeviceId = stringAt(inspect.structuredContent, ["device", "id"]);
  const use = await step(state, "use_block", "minecraft.use_block", {
    player: state.player,
    dimension: state.dimension,
    ...vbd,
    hand: "main_hand",
    side: "up"
  });
  assertSignalEvent(state, "assert_vbd_signal_event", use.structuredContent, state.channel);
  const history = await step(state, "signal_history", "minecraft.signal_history", { channel: state.channel, limit: 20 });
  assertHistoryContains(state, "assert_vbd_signal_history", history.structuredContent, state.channel);
  await step(state, "doctor_issues", "minecraft.doctor_issues", {});
  await maybeScreenshot(state, "scenario-vbd-right-click");
}

async function runSingleItemSubmitBasic(state: ScenarioState): Promise<void> {
  await ensureEnvironment(state, true);
  state.channel = uniqueChannel("step5.single");
  const vbd = DEFAULT_POSITIONS.singleVbd;
  const receiver = DEFAULT_POSITIONS.singleReceiver;
  await prepareRightClickScene(state, vbd, receiver, state.channel);
  const inspect = await step(state, "inspect_single_vbd", "minecraft.inspect_device", inspectArgs(state, vbd));
  state.vbdDeviceId = stringAt(inspect.structuredContent, ["device", "id"]);
  if (!state.vbdDeviceId) {
    throw scenarioFailure(state, "assert_single_device_id", "minecraft.inspect_device", "COMMAND_FAILED", "single_item_submit_basic could not resolve VBD device id.");
  }
  await startTemplateSession(state, "single_item_submit", state.vbdDeviceId);
  await waitGuiType(state, "single_item_submit");
  await step(state, "single_gui_slots_before", "minecraft.gui_slots", { player: state.player });
  await step(state, "single_gui_put_item", "minecraft.gui_put_item", {
    player: state.player,
    target: "single_item_submit",
    itemId: "minecraft:diamond",
    count: 3
  });
  await step(state, "single_gui_set_count", "minecraft.gui_set_count", {
    player: state.player,
    target: "single_item_submit",
    count: 3
  });
  const slotsAfter = await step(state, "single_gui_slots_after", "minecraft.gui_slots", { player: state.player });
  assertDeepContains(state, "assert_single_gui_diamond_draft", slotsAfter.structuredContent, "minecraft:diamond");
  await step(state, "single_gui_save", "minecraft.gui_save", { player: state.player });
  const after = await step(state, "inspect_single_after_save", "minecraft.inspect_device", { deviceId: state.vbdDeviceId });
  assertNumberAtLeast(state, "assert_single_requirement_saved", after.structuredContent, ["device", "itemSubmit", "requirementCount"], 1);
  const overviewAfter = await fetchTemplateOverview(state, "single_item_submit", state.vbdDeviceId, "single_template_overview_after_save");
  assertDeepContains(state, "assert_single_diamond_saved", overviewAfter.structuredContent, "minecraft:diamond");
  await command(state, "single_item_submit_enable", `tzz signal blockDevice itemSubmit enable ${posText(vbd)}`);
  const enabled = await step(state, "inspect_single_after_enable", "minecraft.inspect_device", { deviceId: state.vbdDeviceId });
  assertBooleanTrue(state, "assert_single_item_submit_enabled", enabled.structuredContent, ["device", "itemSubmit", "enabled"]);
  await step(state, "single_clear_inventory", "minecraft.clear_inventory", { player: state.player });
  await step(state, "single_set_main_hand", "minecraft.set_main_hand", { player: state.player, itemId: "minecraft:diamond", count: 3 });
  const use = await step(state, "single_use_block", "minecraft.use_block", {
    player: state.player,
    dimension: state.dimension,
    ...vbd,
    hand: "main_hand",
    side: "up"
  });
  assertSignalEvent(state, "assert_single_signal_event", use.structuredContent, state.channel);
  await step(state, "single_signal_history", "minecraft.signal_history", { channel: state.channel, limit: 20 });
  await maybeScreenshot(state, "scenario-single-item-submit");
}

async function runContainerTemplateBasic(state: ScenarioState): Promise<void> {
  await ensureEnvironment(state, true);
  state.channel = uniqueChannel("step5.container");
  const vbd = DEFAULT_POSITIONS.containerVbd;
  const receiver = DEFAULT_POSITIONS.containerReceiver;
  await step(state, "container_set_chest", "minecraft.set_block", { dimension: state.dimension, ...vbd, blockId: "minecraft:chest" });
  await step(state, "container_set_receiver", "minecraft.set_block", { dimension: state.dimension, ...receiver, blockId: "tzz_mod:signal_receiver" });
  await command(state, "container_bind_vbd", `tzz signal blockDevice bind ${posText(vbd)} ${state.channel}`);
  await command(state, "container_change_channel", `tzz signal blockDevice containerChangeChannel ${posText(vbd)} ${state.channel}`);
  await command(state, "container_enable", `tzz signal blockDevice container ${posText(vbd)} enable`);
  await command(state, "container_bind_receiver", `tzz signal device bind ${posText(receiver)} ${state.channel}`);
  const inspect = await step(state, "inspect_container_vbd", "minecraft.inspect_device", inspectArgs(state, vbd));
  state.vbdDeviceId = stringAt(inspect.structuredContent, ["device", "id"]);
  if (!state.vbdDeviceId) {
    throw scenarioFailure(state, "assert_container_device_id", "minecraft.inspect_device", "COMMAND_FAILED", "container_template_basic could not resolve VBD device id.");
  }
  await startTemplateSession(state, "container_template", state.vbdDeviceId);
  await waitGuiType(state, "container_template");
  await step(state, "container_gui_slots_before", "minecraft.gui_slots", { player: state.player });
  await step(state, "container_gui_put_item", "minecraft.gui_put_item", {
    player: state.player,
    target: "container_template",
    slot: 0,
    itemId: "minecraft:diamond",
    count: 1
  });
  await step(state, "container_gui_set_count", "minecraft.gui_set_count", {
    player: state.player,
    target: "container_template",
    slot: 0,
    count: 1
  });
  const slotsAfter = await step(state, "container_gui_slots_after", "minecraft.gui_slots", { player: state.player });
  assertDeepContains(state, "assert_container_gui_diamond_draft", slotsAfter.structuredContent, "minecraft:diamond");
  await step(state, "container_gui_save", "minecraft.gui_save", { player: state.player });
  const after = await step(state, "inspect_container_after_save", "minecraft.inspect_device", { deviceId: state.vbdDeviceId });
  assertNumberAtLeast(state, "assert_container_itemcondition_saved", after.structuredContent, ["device", "itemConditions", "count"], 1);
  const overviewAfter = await fetchTemplateOverview(state, "container_template", state.vbdDeviceId, "container_template_overview_after_save");
  assertDeepContains(state, "assert_container_diamond_saved", overviewAfter.structuredContent, "minecraft:diamond");
  await step(state, "container_doctor_issues", "minecraft.doctor_issues", {});
  await maybeScreenshot(state, "scenario-container-template");
}

async function ensureEnvironment(state: ScenarioState, loginWebAdmin: boolean): Promise<void> {
  await step(state, "health", "health.check", {});
  await step(state, "repo_status", "repo.status", {});
  await step(state, "start_client", "minecraft.start_client", {
    autoEnterWorld: true,
    worldName: state.options.worldName,
    waitForWebAdmin: true,
    waitTimeoutSeconds: 240
  });
  await step(state, "wait_world", "minecraft.wait_world", { timeoutSeconds: 240, requirePlayer: true });
  await step(state, "wait_webadmin", "minecraft.wait_webadmin", { timeoutSeconds: 180 });
  await step(state, "wait_testbridge", "minecraft.wait_testbridge", { timeoutSeconds: 180 });
  const players = await step(state, "players", "minecraft.players", {});
  state.player = state.options.player || firstPlayerName(players.structuredContent);
  if (!state.player) {
    throw scenarioFailure(state, "assert_player_available", "minecraft.players", "NOT_FOUND", "No online player is available for the scenario.");
  }
  await step(state, "prepare_test_world", "minecraft.prepare_test_world", { player: state.player });
  if (loginWebAdmin) {
    await step(state, "webadmin_open", "webadmin.open", {});
    await step(state, "webadmin_login", "webadmin.login", {});
    await step(state, "webadmin_dashboard", "webadmin.goto", { hashRoute: "#/dashboard" });
    await step(state, "webadmin_console_errors", "webadmin.console_errors", {}, true);
    await maybeScreenshot(state, `scenario-${state.options.scenarioName}-dashboard`);
  }
}

async function prepareRightClickScene(state: ScenarioState, vbd: Position, receiver: Position, channel: string): Promise<void> {
  await step(state, "set_vbd_block", "minecraft.set_block", { dimension: state.dimension, ...vbd, blockId: "minecraft:stone" });
  await step(state, "set_receiver_block", "minecraft.set_block", { dimension: state.dimension, ...receiver, blockId: "tzz_mod:signal_receiver" });
  await command(state, "bind_vbd", `tzz signal blockDevice bind ${posText(vbd)} ${channel}`);
  await command(state, "set_interact_channel", `tzz signal blockDevice interactChannel ${posText(vbd)} ${channel}`);
  await command(state, "enable_interaction", `tzz signal blockDevice interaction ${posText(vbd)} enable`);
  await command(state, "bind_receiver", `tzz signal device bind ${posText(receiver)} ${channel}`);
}

async function fetchTemplateOverview(
  state: ScenarioState,
  type: "single_item_submit" | "container_template",
  deviceId: string,
  id: string
): Promise<StepResult> {
  const started = Date.now();
  try {
    const page = state.context.webAdmin.page;
    if (!page) {
      throw scenarioFailure(state, id, "webadmin.api", "AUTH_REQUIRED", "WebAdmin browser is not open for template overview.");
    }
    const result = await call(page, "evaluate", [async (input: { type: string; deviceId: string }) => {
      const pathPart = input.type === "single_item_submit" ? "single-item-submit" : "container-template";
      const response = await fetch(`/api/webadmin/virtual-block-devices/${encodeURIComponent(input.deviceId)}/${pathPart}`, {
        method: "GET",
        credentials: "same-origin"
      });
      const json = await response.json();
      return {
        status: response.status,
        ok: response.ok,
        json
      };
    }, { type, deviceId }]) as JsonObject;
    const json = objectAt(result, ["json"]);
    const payload = objectAt(json, ["data"]);
    const success = Boolean(result.ok) && Boolean(json.ok ?? true);
    const message = success ? "Template overview returned." : String(json.message ?? "Template overview request failed.");
    const structuredContent = sanitizeJsonObject({ ok: success, code: success ? "OK" : "COMMAND_FAILED", message, ...result, payload });
    const entry: StepResult = {
      id,
      tool: "webadmin.fixed_template_overview",
      ok: success,
      code: success ? "OK" : "COMMAND_FAILED",
      message,
      durationMs: Date.now() - started,
      optional: false,
      structuredContent
    };
    state.steps.push(entry);
    if (!success) {
      throw scenarioFailure(state, id, "webadmin.fixed_template_overview", "COMMAND_FAILED", message, structuredContent);
    }
    return entry;
  } catch (error) {
    if (error instanceof ScenarioAbort) {
      throw error;
    }
    throw scenarioFailure(state, id, "webadmin.fixed_template_overview", "COMMAND_FAILED", errorMessage(error));
  }
}

async function startTemplateSession(state: ScenarioState, type: "single_item_submit" | "container_template", deviceId: string): Promise<void> {
  const id = `${type}_session_start`;
  const started = Date.now();
  try {
    const page = state.context.webAdmin.page;
    if (!page) {
      throw scenarioFailure(state, id, "webadmin.api", "AUTH_REQUIRED", "WebAdmin browser is not open for template session start.");
    }
    const result = await call(page, "evaluate", [async (input: { type: string; deviceId: string; player: string }) => {
      const asRecord = (value: unknown): Record<string, unknown> =>
        value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
      const childRecord = (value: unknown, key: string): Record<string, unknown> => asRecord(asRecord(value)[key]);
      const stringField = (value: unknown, key: string): string => {
        const field = asRecord(value)[key];
        return typeof field === "string" ? field : "";
      };
      const csrfResponse = await fetch("/api/webadmin/write/capabilities", { method: "GET", credentials: "same-origin" });
      const csrfJson = await csrfResponse.json();
      const csrfToken = csrfJson?.data?.csrf?.token ?? "";
      const pathPart = input.type === "single_item_submit" ? "single-item-submit" : "container-template";
      const targetType = input.type === "single_item_submit" ? "virtual_block_device_single_item_submit" : "virtual_block_device_container_template";
      const overviewResponse = await fetch(`/api/webadmin/virtual-block-devices/${encodeURIComponent(input.deviceId)}/${pathPart}`, {
        method: "GET",
        credentials: "same-origin"
      });
      const overview = await overviewResponse.json();
      const overviewData = childRecord(overview, "data");
      const lockResponse = await fetch("/api/webadmin/edit-locks/acquire", {
        method: "POST",
        credentials: "same-origin",
        headers: {
          "Content-Type": "application/json",
          "X-TZZ-WebAdmin-CSRF": csrfToken
        },
        body: JSON.stringify({ targetType, targetId: input.deviceId })
      });
      const lockJson = await lockResponse.json();
      const lockEnvelope = childRecord(lockJson, "data");
      const lockResultData = childRecord(lockEnvelope, "data");
      const lock = Object.keys(childRecord(lockResultData, "lock")).length > 0
        ? childRecord(lockResultData, "lock")
        : childRecord(lockEnvelope, "lock");
      const lockId = stringField(lock, "lockId");
      const expectedFingerprint = stringField(overviewData, "expectedFingerprint");
      const startResponse = await fetch(`/api/webadmin/virtual-block-devices/${encodeURIComponent(input.deviceId)}/${pathPart}-session/start`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
          "Content-Type": "application/json",
          "X-TZZ-WebAdmin-CSRF": csrfToken
        },
        body: JSON.stringify({
          deviceId: input.deviceId,
          targetPlayerName: input.player,
          targetPlayerUuid: "",
          lockId,
          expectedFingerprint
        })
      });
      const startJson = await startResponse.json();
      return {
        overviewStatus: overviewResponse.status,
        overviewOk: overviewResponse.ok,
        overview,
        lockStatus: lockResponse.status,
        lockOk: lockResponse.ok,
        lockJson,
        startStatus: startResponse.status,
        startOk: startResponse.ok,
        startJson,
        lockId,
        lockIdPresent: lockId.length > 0,
        expectedFingerprint,
        expectedFingerprintPresent: expectedFingerprint.length > 0
      };
    }, { type, deviceId, player: state.player }]) as JsonObject;
    const startJson = objectAt(result, ["startJson"]);
    const startData = objectAt(startJson, ["data"]);
    const success = Boolean(startData.success ?? startJson.success ?? false);
    const message = String(startData.message ?? startJson.message ?? "Template session start returned no success flag.");
    const structuredContent = sanitizeJsonObject({ ok: success, code: success ? "OK" : "COMMAND_FAILED", message, ...result });
    state.steps.push({
      id,
      tool: "webadmin.fixed_template_session_start",
      ok: success,
      code: success ? "OK" : "COMMAND_FAILED",
      message,
      durationMs: Date.now() - started,
      optional: false,
      structuredContent
    });
    if (!success) {
      throw scenarioFailure(state, id, "webadmin.fixed_template_session_start", "NEEDS_WEBADMIN_SELECTOR", message, structuredContent);
    }
  } catch (error) {
    if (error instanceof ScenarioAbort) {
      throw error;
    }
    throw scenarioFailure(state, id, "webadmin.fixed_template_session_start", "NEEDS_WEBADMIN_SELECTOR", errorMessage(error));
  }
}

async function waitGuiType(state: ScenarioState, expectedType: string): Promise<void> {
  const deadline = Date.now() + 30000;
  let latest: StepResult | undefined;
  while (Date.now() < deadline) {
    latest = await step(state, `gui_current_${expectedType}`, "minecraft.gui_current", { player: state.player }, true);
    const type = stringAt(latest.structuredContent, ["type"]);
    if (type === expectedType) {
      return;
    }
    await delay(750);
  }
  throw scenarioFailure(state, `assert_gui_${expectedType}`, "minecraft.gui_current", "TIMEOUT", `Timed out waiting for GUI type ${expectedType}.`, latest?.structuredContent ?? {});
}

async function maybeScreenshot(state: ScenarioState, name: string): Promise<void> {
  if (!state.options.screenshot) {
    return;
  }
  await step(state, `screenshot_${name}`, "webadmin.screenshot", { name, fullPage: true }, true);
}

async function command(state: ScenarioState, id: string, rawCommand: string): Promise<StepResult> {
  return await step(state, id, "minecraft.command", { command: rawCommand, player: state.player });
}

async function step(state: ScenarioState, id: string, toolName: string, args: JsonObject, optional = false): Promise<StepResult> {
  if (!ALLOWED_SCENARIO_TOOLS.has(toolName)) {
    throw scenarioFailure(state, id, toolName, "SECURITY_DENIED", `Tool is not allowed in scenarios: ${toolName}`);
  }
  const tool = state.context.tools.get(toolName);
  if (!tool) {
    throw scenarioFailure(state, id, toolName, "VALIDATION_ERROR", `Tool is not registered: ${toolName}`);
  }
  const started = Date.now();
  let result: ToolCallResult;
  try {
    result = await tool.handler(args, state.context);
  } catch (error) {
    result = fail("IO_ERROR", errorMessage(error));
  }
  const structured = sanitizeJsonObject((result.structuredContent ?? {}) as JsonObject);
  const code = String(structured.code ?? (result.isError ? "COMMAND_FAILED" : "OK"));
  const message = String(structured.message ?? result.content.map((entry) => entry.text).join("\n"));
  const item: StepResult = {
    id,
    tool: toolName,
    ok: result.isError !== true,
    code,
    message,
    durationMs: Date.now() - started,
    optional,
    structuredContent: structured
  };
  state.steps.push(item);
  if (!item.ok && !optional) {
    throw scenarioFailure(state, id, toolName, normalizeFailureCode(code), message, structured);
  }
  return item;
}

async function cleanup(context: ToolContext, mode: CleanupMode): Promise<JsonObject> {
  const result: JsonObject = { mode, deletedFiles: false };
  if (mode === "none") {
    return result;
  }
  if (mode === "minecraft" || mode === "all") {
    result.minecraft = await callToolForCleanup(context, "minecraft.stop", { timeoutSeconds: 20 });
  }
  if (mode === "browser" || mode === "all") {
    result.webadmin = await callToolForCleanup(context, "webadmin.close", {});
  }
  return result;
}

async function callToolForCleanup(context: ToolContext, toolName: string, args: JsonObject): Promise<JsonObject> {
  const tool = context.tools.get(toolName);
  if (!tool) {
    return { ok: false, code: "VALIDATION_ERROR", message: `Missing cleanup tool ${toolName}` };
  }
  try {
    const result = await tool.handler(args, context);
    return sanitizeJsonObject((result.structuredContent ?? { ok: result.isError !== true, message: result.content.map((entry) => entry.text).join("\n") }) as JsonObject);
  } catch (error) {
    return { ok: false, code: "IO_ERROR", message: errorMessage(error) };
  }
}

function writeScenarioReport(state: ScenarioState): string {
  const file = ensureScenarioReportPath(state.context.config, state.options.scenarioName);
  const finishedAt = Date.now();
  const lines = [
    `# Scenario ${state.options.scenarioName}`,
    "",
    `Start: ${new Date(state.startedAt).toISOString()}`,
    `End: ${new Date(finishedAt).toISOString()}`,
    `Duration: ${finishedAt - state.startedAt} ms`,
    `Result: ${state.failedStepId ? "FAIL" : "PASS"}`,
    `World: ${state.options.worldName}`,
    `Player: ${state.player || "-"}`,
    `Channel: ${state.channel || "-"}`,
    "",
    "## Steps",
    "",
    "| Step | Tool | Result | Code | Duration | Message |",
    "|---|---|---:|---|---:|---|",
    ...state.steps.map((entry) => `| ${escapeCell(entry.id)} | ${escapeCell(entry.tool)} | ${entry.ok ? "PASS" : "FAIL"} | ${escapeCell(entry.code)} | ${entry.durationMs} ms | ${escapeCell(entry.message)} |`),
    "",
    "## Failure",
    "",
    state.failedStepId ? `Failed step: ${state.failedStepId} (${state.failedTool})\n\n${state.failureCode}: ${state.failureMessage}` : "None",
    "",
    "## Cleanup",
    "",
    "```json",
    JSON.stringify(state.cleanup, null, 2),
    "```",
    "",
    "## Screenshots",
    "",
    ...(state.context.webAdmin.screenshots.length ? state.context.webAdmin.screenshots.map((screenshot) => `- ${screenshot}`) : ["None"]),
    "",
    "## Step Data",
    "",
    "```json",
    JSON.stringify(state.steps.map((entry) => ({
      id: entry.id,
      tool: entry.tool,
      ok: entry.ok,
      code: entry.code,
      message: entry.message,
      durationMs: entry.durationMs,
      structuredContent: entry.structuredContent
    })), null, 2),
    "```",
    ""
  ];
  writeFileSync(file, redactSecrets(lines.join("\n")), "utf8");
  return file;
}

function summary(state: ScenarioState): JsonObject {
  const failed = state.steps.filter((entry) => !entry.ok && !entry.optional).length;
  const optionalFailed = state.steps.filter((entry) => !entry.ok && entry.optional).length;
  return sanitizeJsonObject({
    scenarioName: state.options.scenarioName,
    ok: !state.failedStepId,
    durationMs: Date.now() - state.startedAt,
    stepCount: state.steps.length,
    passed: state.steps.filter((entry) => entry.ok).length,
    failed,
    optionalFailed,
    failedStepId: state.failedStepId,
    failedTool: state.failedTool,
    reportPath: state.reportPath,
    screenshotPaths: state.context.webAdmin.screenshots,
    cleanup: state.cleanup,
    steps: state.steps.map((entry) => ({
      id: entry.id,
      tool: entry.tool,
      ok: entry.ok,
      code: entry.code,
      message: entry.message,
      durationMs: entry.durationMs
    })),
    noArbitraryShell: true,
    noGitMutation: true,
    noExternalHost: true,
    noMinecraftGuiCoordinateClicking: true
  });
}

function assertSignalEvent(state: ScenarioState, id: string, data: JsonObject, channel: string): void {
  const events = arrayAt(data, ["signalEvents"]);
  const matched = events.some((entry) => isObject(entry) && String(entry.channel ?? "") === channel);
  if (!matched) {
    throw scenarioFailure(state, id, "assert.signalEvents", "COMMAND_FAILED", `No signalEvents entry found for channel ${channel}.`, data);
  }
  addAssertStep(state, id, true, `Signal event found for ${channel}.`, { channel });
}

function assertHistoryContains(state: ScenarioState, id: string, data: JsonObject, channel: string): void {
  const events = arrayAt(data, ["events"]);
  const matched = events.some((entry) => isObject(entry) && String(entry.channel ?? "") === channel);
  if (!matched) {
    throw scenarioFailure(state, id, "assert.signal_history", "COMMAND_FAILED", `No signal history entry found for channel ${channel}.`, data);
  }
  addAssertStep(state, id, true, `Signal history contains ${channel}.`, { channel });
}

function assertNumberAtLeast(state: ScenarioState, id: string, data: JsonObject, pathKeys: string[], minimum: number): void {
  const value = numberAt(data, pathKeys);
  if (value < minimum) {
    throw scenarioFailure(state, id, "assert.field", "COMMAND_FAILED", `Expected ${pathKeys.join(".")} >= ${minimum}, got ${value}.`, data);
  }
  addAssertStep(state, id, true, `${pathKeys.join(".")} >= ${minimum}.`, { value });
}

function assertBooleanTrue(state: ScenarioState, id: string, data: JsonObject, pathKeys: string[]): void {
  const value = booleanAt(data, pathKeys);
  if (value !== true) {
    throw scenarioFailure(state, id, "assert.field", "COMMAND_FAILED", `Expected ${pathKeys.join(".")} to be true.`, data);
  }
  addAssertStep(state, id, true, `${pathKeys.join(".")} is true.`, { value });
}

function assertDeepContains(state: ScenarioState, id: string, data: JsonObject, needle: string): void {
  if (!JSON.stringify(data).includes(needle)) {
    throw scenarioFailure(state, id, "assert.contains", "COMMAND_FAILED", `Expected scenario data to contain ${needle}.`, data);
  }
  addAssertStep(state, id, true, `Data contains ${needle}.`, { needle });
}

function addAssertStep(state: ScenarioState, id: string, success: boolean, message: string, data: JsonObject): void {
  state.steps.push({
    id,
    tool: "assert",
    ok: success,
    code: success ? "OK" : "COMMAND_FAILED",
    message,
    durationMs: 0,
    optional: false,
    structuredContent: sanitizeJsonObject({ ok: success, code: success ? "OK" : "COMMAND_FAILED", message, ...data })
  });
}

function scenarioFailure(
  state: ScenarioState,
  id: string,
  tool: string,
  code: Exclude<ToolErrorCode, "OK">,
  message: string,
  data: JsonObject = {}
): ScenarioAbort {
  markFailure(state, id, tool, code, message);
  if (!state.steps.some((entry) => entry.id === id)) {
    state.steps.push({
      id,
      tool,
      ok: false,
      code,
      message,
      durationMs: 0,
      optional: false,
      structuredContent: sanitizeJsonObject({ ok: false, code, message, ...data })
    });
  }
  return new ScenarioAbort(message);
}

function markFailure(state: ScenarioState, id: string, tool: string, code: Exclude<ToolErrorCode, "OK">, message: string): void {
  if (state.failedStepId) {
    return;
  }
  state.failedStepId = id;
  state.failedTool = tool;
  state.failureCode = code;
  state.failureMessage = message;
}

function inspectArgs(state: ScenarioState, position: Position): JsonObject {
  return { dimension: state.dimension, ...position };
}

function firstPlayerName(data: JsonObject): string {
  const players = arrayAt(data, ["players"]);
  const first = players.find((entry) => isObject(entry) && typeof entry.name === "string");
  return isObject(first) ? String(first.name ?? "") : "";
}

function uniqueChannel(prefix: string): string {
  return `${prefix}.${Date.now()}`.toLowerCase();
}

function posText(position: Position): string {
  return `${position.x} ${position.y} ${position.z}`;
}

function cleanupMode(value: Json | undefined): CleanupMode {
  return value === "browser" || value === "minecraft" || value === "all" || value === "none" ? value : "all";
}

function normalizeFailureCode(code: string): Exclude<ToolErrorCode, "OK"> {
  if (code === "OK") {
    return "COMMAND_FAILED";
  }
  return code as Exclude<ToolErrorCode, "OK">;
}

function stringAt(data: JsonObject, pathKeys: string[]): string {
  const value = valueAt(data, pathKeys);
  return typeof value === "string" ? value : "";
}

function numberAt(data: JsonObject, pathKeys: string[]): number {
  const value = valueAt(data, pathKeys);
  return typeof value === "number" && Number.isFinite(value) ? value : Number.NaN;
}

function booleanAt(data: JsonObject, pathKeys: string[]): boolean | undefined {
  const value = valueAt(data, pathKeys);
  return typeof value === "boolean" ? value : undefined;
}

function objectAt(data: JsonObject, pathKeys: string[]): JsonObject {
  const value = valueAt(data, pathKeys);
  return isObject(value) ? value : {};
}

function arrayAt(data: JsonObject, pathKeys: string[]): Json[] {
  const value = valueAt(data, pathKeys);
  return Array.isArray(value) ? value : [];
}

function valueAt(data: JsonObject, pathKeys: string[]): Json | undefined {
  let current: Json | undefined = data;
  for (const key of pathKeys) {
    if (!isObject(current)) {
      return undefined;
    }
    current = current[key];
  }
  return current;
}

function isObject(value: Json | undefined): value is JsonObject {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

async function call(target: unknown, method: string, args: unknown[]): Promise<unknown> {
  const fn = getMethod(target, method);
  return await fn.apply(target, args);
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

function escapeCell(value: string): string {
  return redactSecrets(String(value ?? "")).replace(/\|/g, "\\|").replace(/\r?\n/g, " ").slice(0, 240);
}

function errorMessage(error: unknown): string {
  return redactSecrets(error instanceof Error ? error.message : String(error));
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

type Position = { x: number; y: number; z: number };

class ScenarioAbort extends Error {
}
