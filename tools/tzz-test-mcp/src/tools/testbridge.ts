import type { Json, JsonObject, ToolDefinition, TzzTestMcpConfig } from "../types.js";
import { fail, ok, type ToolErrorCode } from "../results.js";
import { ensureAllowedUrl, redactSecrets } from "../safety.js";

const TOKEN_HEADER = "X-TZZ-TestBridge-Token";
const TESTBRIDGE_TOKEN_ENV = "TZZ_TESTBRIDGE_TOKEN";

export function testBridgeTools(): ToolDefinition[] {
  return [
    testBridgeStatusTool(),
    testBridgePlayersTool(),
    testBridgeCommandTool(),
    testBridgeSetBlockTool(),
    testBridgeClearAreaTool(),
    testBridgeGiveItemTool(),
    testBridgeClearInventoryTool(),
    testBridgeSetMainHandTool(),
    testBridgeUseBlockTool(),
    testBridgeInspectDeviceTool(),
    testBridgeSignalHistoryTool(),
    testBridgeDoctorIssuesTool(),
    testBridgeWaitTool()
  ];
}

function testBridgeStatusTool(): ToolDefinition {
  return {
    name: "minecraft.testbridge_status",
    description: "Read the dev-only localhost TestBridge status. This does not expose the token.",
    inputSchema: { type: "object", additionalProperties: false, properties: {} },
    readOnlyHint: true,
    async handler(_args, context) {
      return await requestTool(context.config, "GET", "status", undefined, "TestBridge status returned.", false);
    }
  };
}

function testBridgePlayersTool(): ToolDefinition {
  return {
    name: "minecraft.players",
    description: "List online Minecraft players through the localhost TestBridge.",
    inputSchema: { type: "object", additionalProperties: false, properties: {} },
    readOnlyHint: true,
    async handler(_args, context) {
      return await requestTool(context.config, "GET", "players", undefined, "Online players returned.");
    }
  };
}

function testBridgeCommandTool(): ToolDefinition {
  return {
    name: "minecraft.command",
    description: "Execute an allowlisted Minecraft command through TestBridge. Dangerous commands are denied and this is never OS shell.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        command: { type: "string" },
        player: { type: "string" }
      },
      required: ["command"]
    },
    async handler(args, context) {
      const command = stringArg(args, "command");
      if (!command) {
        return fail("VALIDATION_ERROR", "command is required.");
      }
      if (isDangerousCommand(command)) {
        return fail("COMMAND_DENIED", "Command is blocked by the local TestBridge denylist.");
      }
      return await requestTool(context.config, "POST", "command", {
        command,
        player: stringArg(args, "player") ?? ""
      }, "Minecraft command executed.");
    }
  };
}

function testBridgeSetBlockTool(): ToolDefinition {
  return {
    name: "minecraft.set_block",
    description: "Place a block inside the restricted TestBridge test area.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        dimension: { type: "string" },
        x: { type: "number" },
        y: { type: "number" },
        z: { type: "number" },
        blockId: { type: "string" },
        properties: { type: "object", additionalProperties: { type: "string" } }
      },
      required: ["x", "y", "z", "blockId"]
    },
    async handler(args, context) {
      return await requestTool(context.config, "POST", "world/set-block", {
        dimension: stringArg(args, "dimension") ?? "",
        x: intArg(args, "x"),
        y: intArg(args, "y"),
        z: intArg(args, "z"),
        blockId: stringArg(args, "blockId") ?? "",
        properties: objectArg(args, "properties")
      }, "Block placed through TestBridge.");
    }
  };
}

function testBridgeClearAreaTool(): ToolDefinition {
  return {
    name: "minecraft.clear_area",
    description: "Clear a bounded area inside the restricted TestBridge test area. Large or out-of-bounds fills are denied.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        dimension: { type: "string" },
        min: positionSchema(),
        max: positionSchema()
      },
      required: ["min", "max"]
    },
    async handler(args, context) {
      return await requestTool(context.config, "POST", "world/clear-area", {
        dimension: stringArg(args, "dimension") ?? "",
        min: positionArg(args, "min"),
        max: positionArg(args, "max")
      }, "Area cleared through TestBridge.");
    }
  };
}

function testBridgeGiveItemTool(): ToolDefinition {
  return {
    name: "minecraft.give_item",
    description: "Give a normal item stack to one online player through TestBridge. Raw NBT/components are intentionally unsupported.",
    inputSchema: playerItemSchema(),
    async handler(args, context) {
      return await requestTool(context.config, "POST", "player/give", playerItemBody(args), "Item inserted into player inventory.");
    }
  };
}

function testBridgeClearInventoryTool(): ToolDefinition {
  return {
    name: "minecraft.clear_inventory",
    description: "Clear only the specified online player's main inventory/hotbar through TestBridge.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: { player: { type: "string" } },
      required: ["player"]
    },
    async handler(args, context) {
      return await requestTool(context.config, "POST", "player/clear-inventory", {
        player: stringArg(args, "player") ?? ""
      }, "Player inventory cleared.");
    }
  };
}

function testBridgeSetMainHandTool(): ToolDefinition {
  return {
    name: "minecraft.set_main_hand",
    description: "Set the specified online player's main-hand item through TestBridge.",
    inputSchema: playerItemSchema(),
    async handler(args, context) {
      return await requestTool(context.config, "POST", "player/set-main-hand", playerItemBody(args), "Player main hand set.");
    }
  };
}

function testBridgeUseBlockTool(): ToolDefinition {
  return {
    name: "minecraft.use_block",
    description: "Simulate a player right-clicking a loaded block by invoking Minecraft's UseBlockCallback production path.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        dimension: { type: "string" },
        x: { type: "number" },
        y: { type: "number" },
        z: { type: "number" },
        hand: { type: "string", enum: ["main_hand", "off_hand"] },
        side: { type: "string", enum: ["up", "down", "north", "south", "east", "west"] }
      },
      required: ["player", "x", "y", "z"]
    },
    async handler(args, context) {
      return await requestTool(context.config, "POST", "player/use-block", {
        player: stringArg(args, "player") ?? "",
        dimension: stringArg(args, "dimension") ?? "",
        x: intArg(args, "x"),
        y: intArg(args, "y"),
        z: intArg(args, "z"),
        hand: stringArg(args, "hand") ?? "main_hand",
        side: stringArg(args, "side") ?? "up"
      }, "Block use simulated through production UseBlockCallback.");
    }
  };
}

function testBridgeInspectDeviceTool(): ToolDefinition {
  return {
    name: "minecraft.inspect_device",
    description: "Read one signal device through TestBridge by deviceId or exact dimension/x/y/z. This is read-only.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        deviceId: { type: "string" },
        dimension: { type: "string" },
        x: { type: "number" },
        y: { type: "number" },
        z: { type: "number" }
      }
    },
    readOnlyHint: true,
    async handler(args, context) {
      return await requestTool(context.config, "POST", "device/inspect", {
        deviceId: stringArg(args, "deviceId") ?? "",
        dimension: stringArg(args, "dimension") ?? "",
        x: optionalIntArg(args, "x") ?? 0,
        y: optionalIntArg(args, "y") ?? 0,
        z: optionalIntArg(args, "z") ?? 0
      }, "Device inspection returned.");
    }
  };
}

function testBridgeSignalHistoryTool(): ToolDefinition {
  return {
    name: "minecraft.signal_history",
    description: "Read recent signal history through TestBridge. This is read-only.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        channel: { type: "string" },
        limit: { type: "number", minimum: 1, maximum: 200 }
      }
    },
    readOnlyHint: true,
    async handler(args, context) {
      const params = new URLSearchParams();
      const channel = stringArg(args, "channel");
      if (channel) {
        params.set("channel", channel);
      }
      const limit = optionalIntArg(args, "limit");
      if (limit !== undefined) {
        params.set("limit", String(limit));
      }
      const suffix = params.toString() ? `signal/history?${params}` : "signal/history";
      return await requestTool(context.config, "GET", suffix, undefined, "Signal history returned.");
    }
  };
}

function testBridgeDoctorIssuesTool(): ToolDefinition {
  return {
    name: "minecraft.doctor_issues",
    description: "Read current Doctor issues through TestBridge. This is read-only.",
    inputSchema: { type: "object", additionalProperties: false, properties: {} },
    readOnlyHint: true,
    async handler(_args, context) {
      return await requestTool(context.config, "GET", "doctor/issues", undefined, "Doctor issues returned.");
    }
  };
}

function testBridgeWaitTool(): ToolDefinition {
  return {
    name: "minecraft.wait_testbridge",
    description: "Wait until the localhost TestBridge reports enabled, ready, and token configured.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        timeoutSeconds: { type: "number", minimum: 1, maximum: 900 }
      }
    },
    async handler(args, context) {
      const timeoutSeconds = optionalIntArg(args, "timeoutSeconds") ?? 180;
      const started = Date.now();
      let last: JsonObject = {};
      while (Date.now() - started < timeoutSeconds * 1000) {
        const response = await testBridgeFetch(context.config, "GET", "status", undefined, false);
        if (response.ok === true) {
          last = response.data;
          if (last.enabled === true && last.ready === true && last.tokenConfigured === true) {
            return ok("TestBridge is ready.", {
              durationMs: Date.now() - started,
              status: last
            });
          }
        } else {
          last = { code: response.code, message: response.message };
        }
        await delay(1000);
      }
      return fail("TIMEOUT", "Timed out waiting for TestBridge readiness.", {
        durationMs: Date.now() - started,
        lastStatus: last
      });
    }
  };
}

async function requestTool(
  config: TzzTestMcpConfig,
  method: "GET" | "POST",
  path: string,
  body: JsonObject | undefined,
  successMessage: string,
  requireToken = true
) {
  const response = await testBridgeFetch(config, method, path, body, requireToken);
  if (response.ok) {
    return ok(successMessage, response.data ?? {});
  }
  return fail(response.code, response.message, response.data ?? {});
}

async function testBridgeFetch(
  config: TzzTestMcpConfig,
  method: "GET" | "POST",
  path: string,
  body: JsonObject | undefined,
  requireToken: boolean
): Promise<{ ok: true; data: JsonObject } | { ok: false; code: Exclude<ToolErrorCode, "OK">; message: string; data?: JsonObject }> {
  let url: URL;
  try {
    url = testBridgeUrl(config, path);
  } catch (error) {
    return { ok: false, code: "SECURITY_DENIED", message: errorMessage(error) };
  }
  const token = process.env[TESTBRIDGE_TOKEN_ENV] ?? "";
  if (requireToken && !token.trim()) {
    return {
      ok: false,
      code: "CONFIG_ERROR",
      message: "Missing TZZ_TESTBRIDGE_TOKEN. Configure it in the local Codex MCP environment."
    };
  }
  const headers: Record<string, string> = { "Accept": "application/json" };
  if (method === "POST") {
    headers["Content-Type"] = "application/json";
  }
  if (token.trim()) {
    headers[TOKEN_HEADER] = token.trim();
  }
  try {
    const init: RequestInit = {
      method,
      headers,
      redirect: "manual"
    };
    if (body !== undefined) {
      init.body = JSON.stringify(body);
    }
    const response = await fetch(url, init);
    const text = await response.text();
    let parsed: JsonObject = {};
    if (text.trim()) {
      parsed = JSON.parse(text) as JsonObject;
    }
    if (response.ok && parsed.ok === true) {
      return { ok: true, data: objectOrEmpty(parsed.data) };
    }
    const error = objectOrEmpty(parsed.error);
    return {
      ok: false,
      code: normalizeCode(String(error.code ?? response.statusText ?? "TESTBRIDGE_AUTH_FAILED")),
      message: redactSecrets(String(error.message ?? `TestBridge HTTP ${response.status}`)),
      data: {
        status: response.status,
        url: url.toString()
      }
    };
  } catch (error) {
    return { ok: false, code: "COMMAND_FAILED", message: errorMessage(error), data: { url: url.toString() } };
  }
}

function testBridgeUrl(config: TzzTestMcpConfig, suffix: string): URL {
  const rawBase = process.env.TZZ_TESTBRIDGE_URL || new URL("/api/testbridge/", config.webAdminUrl).toString();
  const base = ensureAllowedUrl(rawBase, config);
  const normalizedBase = base.toString().endsWith("/") ? base.toString() : `${base.toString()}/`;
  const cleanSuffix = suffix.replace(/^\/+/, "");
  return ensureAllowedUrl(new URL(cleanSuffix, normalizedBase).toString(), config);
}

function normalizeCode(code: string): Exclude<ToolErrorCode, "OK"> {
  switch (code) {
    case "TESTBRIDGE_DISABLED":
      return "TESTBRIDGE_DISABLED";
    case "TESTBRIDGE_NOT_READY":
      return "TESTBRIDGE_NOT_READY";
    case "COMMAND_DENIED":
      return "COMMAND_DENIED";
    case "NOT_FOUND":
      return "NOT_FOUND";
    case "VALIDATION_FAILED":
    case "VALIDATION_ERROR":
      return "VALIDATION_ERROR";
    case "BOUNDS_DENIED":
    case "TESTBRIDGE_FORBIDDEN":
    case "TESTBRIDGE_TOKEN_REQUIRED":
    case "TESTBRIDGE_TOKEN_INVALID":
      return "SECURITY_DENIED";
    case "METHOD_NOT_ALLOWED":
      return "VALIDATION_ERROR";
    default:
      return "TESTBRIDGE_AUTH_FAILED";
  }
}

function isDangerousCommand(raw: string): boolean {
  const command = raw.trim().replace(/^\/+/, "").toLowerCase();
  const root = command.split(/\s+/, 1)[0] ?? "";
  return ["stop", "op", "deop", "ban", "kick", "whitelist", "save-off", "save-on", "pardon", "reload"].includes(root);
}

function playerItemSchema(): JsonObject {
  return {
    type: "object",
    additionalProperties: false,
    properties: {
      player: { type: "string" },
      itemId: { type: "string" },
      count: { type: "number", minimum: 1, maximum: 2304 }
    },
    required: ["player", "itemId", "count"]
  };
}

function playerItemBody(args: JsonObject): JsonObject {
  return {
    player: stringArg(args, "player") ?? "",
    itemId: stringArg(args, "itemId") ?? "",
    count: intArg(args, "count")
  };
}

function positionSchema(): JsonObject {
  return {
    type: "object",
    additionalProperties: false,
    properties: {
      x: { type: "number" },
      y: { type: "number" },
      z: { type: "number" }
    },
    required: ["x", "y", "z"]
  };
}

function positionArg(args: JsonObject, key: string): JsonObject {
  const value = args[key];
  if (!isObject(value)) {
    return {};
  }
  return {
    x: intArg(value, "x"),
    y: intArg(value, "y"),
    z: intArg(value, "z")
  };
}

function objectArg(args: JsonObject, key: string): JsonObject {
  const value = args[key];
  if (!isObject(value)) {
    return {};
  }
  const output: JsonObject = {};
  for (const [entryKey, entryValue] of Object.entries(value)) {
    if (typeof entryValue === "string") {
      output[entryKey] = entryValue;
    }
  }
  return output;
}

function stringArg(args: JsonObject, key: string): string | undefined {
  const value = args[key];
  return typeof value === "string" && value.trim() ? value.trim() : undefined;
}

function intArg(args: JsonObject, key: string): number {
  const value = optionalIntArg(args, key);
  if (value === undefined) {
    throw new Error(`${key} must be a finite number.`);
  }
  return value;
}

function optionalIntArg(args: JsonObject, key: string): number | undefined {
  const value = args[key];
  return typeof value === "number" && Number.isFinite(value) ? Math.trunc(value) : undefined;
}

function objectOrEmpty(value: Json | undefined): JsonObject {
  return isObject(value) ? value : {};
}

function isObject(value: Json | undefined): value is JsonObject {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function errorMessage(error: unknown): string {
  return redactSecrets(error instanceof Error ? error.message : String(error));
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
