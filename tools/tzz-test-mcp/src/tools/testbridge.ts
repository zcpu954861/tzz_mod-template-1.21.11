import type { Json, JsonObject, ToolDefinition, TzzTestMcpConfig } from "../types.js";
import { fail, ok, type ToolErrorCode } from "../results.js";
import { ensureAllowedUrl, redactSecrets, safeName } from "../safety.js";

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
    testBridgeWaitWorldTool(),
    testBridgePrepareTestAreaTool(),
    testBridgePrepareTestPlayerTool(),
    testBridgePrepareTestWorldTool(),
    testBridgeWaitTool(),
    testBridgeGuiCurrentTool(),
    testBridgeGuiSlotsTool(),
    testBridgeGuiPutItemTool(),
    testBridgeGuiClearSlotTool(),
    testBridgeGuiSetCountTool(),
    testBridgeGuiSaveTool(),
    testBridgeGuiCancelTool(),
    testBridgeClientScreenshotTool()
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

function testBridgeWaitWorldTool(): ToolDefinition {
  return {
    name: "minecraft.wait_world",
    description: "Wait until the localhost TestBridge reports a loaded Minecraft world. This does not click Minecraft GUI coordinates.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        timeoutSeconds: { type: "number", minimum: 1, maximum: 900 },
        requirePlayer: { type: "boolean" }
      }
    },
    async handler(args, context) {
      const timeoutSeconds = optionalIntArg(args, "timeoutSeconds") ?? 180;
      const requirePlayer = boolArg(args, "requirePlayer", false);
      const started = Date.now();
      let last: JsonObject = {};
      while (Date.now() - started < timeoutSeconds * 1000) {
        const response = await testBridgeFetch(context.config, "GET", "status", undefined, false);
        if (response.ok === true) {
          last = response.data;
          const players = Array.isArray(last.onlinePlayers) ? last.onlinePlayers : [];
          const worldReady = last.enabled === true
            && last.tokenConfigured === true
            && last.serverLoaded === true
            && last.worldLoaded === true
            && last.ready === true
            && (!requirePlayer || players.length > 0);
          if (worldReady) {
            return ok("Minecraft world is ready through TestBridge.", {
              durationMs: Date.now() - started,
              status: last
            });
          }
        } else {
          last = { code: response.code, message: response.message };
        }
        await delay(1000);
      }
      return fail("TIMEOUT", "Timed out waiting for Minecraft world readiness.", {
        durationMs: Date.now() - started,
        lastStatus: last
      });
    }
  };
}

function testBridgePrepareTestAreaTool(): ToolDefinition {
  return {
    name: "minecraft.prepare_test_area",
    description: "Clear and optionally floor a bounded loaded test area through TestBridge. It enforces test bounds and max volume.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        dimension: { type: "string" },
        min: positionSchema(),
        max: positionSchema(),
        floorBlockId: { type: "string" },
        placeFloor: { type: "boolean" }
      }
    },
    async handler(args, context) {
      const body: JsonObject = {
        dimension: stringArg(args, "dimension") ?? "",
        floorBlockId: stringArg(args, "floorBlockId") ?? "",
        placeFloor: boolArg(args, "placeFloor", true)
      };
      const min = optionalPositionArg(args, "min");
      const max = optionalPositionArg(args, "max");
      if (min !== undefined) {
        body.min = min;
      }
      if (max !== undefined) {
        body.max = max;
      }
      return await requestTool(context.config, "POST", "world/prepare-area", body, "Test area prepared through TestBridge.");
    }
  };
}

function testBridgePrepareTestPlayerTool(): ToolDefinition {
  return {
    name: "minecraft.prepare_test_player",
    description: "Prepare one online player for local tests by clearing inventory/offhand and teleporting inside the test area.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        dimension: { type: "string" },
        position: positionSchema(),
        clearInventory: { type: "boolean" },
        clearOffhand: { type: "boolean" },
        teleport: { type: "boolean" }
      },
      required: ["player"]
    },
    async handler(args, context) {
      const body: JsonObject = {
        player: stringArg(args, "player") ?? "",
        dimension: stringArg(args, "dimension") ?? "",
        clearInventory: boolArg(args, "clearInventory", true),
        clearOffhand: boolArg(args, "clearOffhand", true),
        teleport: boolArg(args, "teleport", true)
      };
      const position = optionalPositionArg(args, "position");
      if (position !== undefined) {
        body.position = position;
      }
      return await requestTool(context.config, "POST", "world/prepare-player", body, "Test player prepared through TestBridge.");
    }
  };
}

function testBridgePrepareTestWorldTool(): ToolDefinition {
  return {
    name: "minecraft.prepare_test_world",
    description: "Idempotently prepare the loaded local test world, bounded test area, and one optional player through structured TestBridge endpoints.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        dimension: { type: "string" },
        player: { type: "string" },
        area: {
          type: "object",
          additionalProperties: false,
          properties: {
            min: positionSchema(),
            max: positionSchema(),
            floorBlockId: { type: "string" },
            placeFloor: { type: "boolean" }
          }
        },
        playerPosition: positionSchema(),
        prepareArea: { type: "boolean" },
        preparePlayer: { type: "boolean" },
        setDayTime: { type: "boolean" },
        clearWeather: { type: "boolean" }
      }
    },
    async handler(args, context) {
      const areaValue = args.area;
      const area = isObject(areaValue) ? areaValue : {};
      const areaBody: JsonObject = {
        floorBlockId: stringArg(area, "floorBlockId") ?? "",
        placeFloor: boolArg(area, "placeFloor", true)
      };
      const areaMin = optionalPositionArg(area, "min");
      const areaMax = optionalPositionArg(area, "max");
      if (areaMin !== undefined) {
        areaBody.min = areaMin;
      }
      if (areaMax !== undefined) {
        areaBody.max = areaMax;
      }
      const playerSetup: JsonObject = {
        clearInventory: true,
        clearOffhand: true,
        teleport: true
      };
      const playerPosition = optionalPositionArg(args, "playerPosition");
      if (playerPosition !== undefined) {
        playerSetup.position = playerPosition;
      }
      return await requestTool(context.config, "POST", "world/prepare", {
        dimension: stringArg(args, "dimension") ?? "",
        player: stringArg(args, "player") ?? "",
        area: areaBody,
        playerSetup,
        prepareArea: boolArg(args, "prepareArea", true),
        preparePlayer: boolArg(args, "preparePlayer", Boolean(stringArg(args, "player"))),
        setDayTime: boolArg(args, "setDayTime", true),
        clearWeather: boolArg(args, "clearWeather", true),
        idempotent: true
      }, "Test world prepared through TestBridge.");
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

function testBridgeGuiCurrentTool(): ToolDefinition {
  return {
    name: "minecraft.gui_current",
    description: "Read the supported Minecraft WebAdmin template GUI currently open on one player. This never uses OS input automation or screen coordinates.",
    inputSchema: guiPlayerSchema(),
    readOnlyHint: true,
    async handler(args, context) {
      return await requestTool(context.config, "GET", `gui/current?player=${encodeURIComponent(stringArg(args, "player") ?? "")}`, undefined, "Current GUI state returned.");
    }
  };
}

function testBridgeGuiSlotsTool(): ToolDefinition {
  return {
    name: "minecraft.gui_slots",
    description: "Read template slots for supported WebAdmin Minecraft GUIs without touching real inventory.",
    inputSchema: guiPlayerSchema(),
    readOnlyHint: true,
    async handler(args, context) {
      return await requestTool(context.config, "GET", `gui/slots?player=${encodeURIComponent(stringArg(args, "player") ?? "")}`, undefined, "GUI slot state returned.");
    }
  };
}

function testBridgeGuiPutItemTool(): ToolDefinition {
  return {
    name: "minecraft.gui_put_item",
    description: "Put a ghost/template item into a supported WebAdmin Minecraft GUI slot. This does not modify real player inventory.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        target: { type: "string", enum: ["container_template", "single_item_submit"] },
        slot: { type: "number", minimum: 0 },
        slotIndex: { type: "number", minimum: 0 },
        itemId: { type: "string" },
        count: { type: "number", minimum: 1, maximum: 64000 }
      },
      required: ["player", "itemId", "count"]
    },
    async handler(args, context) {
      return await requestTool(context.config, "POST", "gui/put-item", guiSlotBody(args), "GUI template item set.");
    }
  };
}

function testBridgeGuiClearSlotTool(): ToolDefinition {
  return {
    name: "minecraft.gui_clear_slot",
    description: "Clear one template slot in a supported WebAdmin Minecraft GUI. This does not touch real inventory.",
    inputSchema: guiSlotSchema(),
    async handler(args, context) {
      return await requestTool(context.config, "POST", "gui/clear-slot", guiSlotBody(args), "GUI template slot cleared.");
    }
  };
}

function testBridgeGuiSetCountTool(): ToolDefinition {
  return {
    name: "minecraft.gui_set_count",
    description: "Set the template count for a supported WebAdmin Minecraft GUI. Existing GUI clamp rules are applied by the client screen.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        target: { type: "string", enum: ["container_template", "single_item_submit"] },
        slot: { type: "number", minimum: 0 },
        slotIndex: { type: "number", minimum: 0 },
        count: { type: "number", minimum: 1, maximum: 64000 }
      },
      required: ["player", "count"]
    },
    async handler(args, context) {
      return await requestTool(context.config, "POST", "gui/set-count", guiSlotBody(args), "GUI template count set.");
    }
  };
}

function testBridgeGuiSaveTool(): ToolDefinition {
  return {
    name: "minecraft.gui_save",
    description: "Save the supported WebAdmin Minecraft GUI through its existing session save path.",
    inputSchema: guiPlayerSchema(),
    async handler(args, context) {
      return await requestTool(context.config, "POST", "gui/save", guiSlotBody(args), "GUI save requested.");
    }
  };
}

function testBridgeGuiCancelTool(): ToolDefinition {
  return {
    name: "minecraft.gui_cancel",
    description: "Cancel the supported WebAdmin Minecraft GUI through its existing cancel path.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        reason: { type: "string" }
      },
      required: ["player"]
    },
    async handler(args, context) {
      return await requestTool(context.config, "POST", "gui/cancel", guiSlotBody(args), "GUI cancel requested.");
    }
  };
}

function testBridgeClientScreenshotTool(): ToolDefinition {
  return {
    name: "minecraft.client_screenshot",
    description: "Ask the Minecraft client to save its current framebuffer under reports/mcp/screenshots through the token-protected TestBridge payload. This is not an OS screenshot and never clicks coordinates.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        name: { type: "string" },
        fullWindow: { type: "boolean" },
        timeoutMs: { type: "number", minimum: 1000, maximum: 120000 }
      },
      required: ["player"]
    },
    async handler(args, context) {
      const player = stringArg(args, "player") ?? "";
      if (!player.trim()) {
        return fail("VALIDATION_ERROR", "player is required.");
      }
      const response = await testBridgeFetch(context.config, "POST", "client/screenshot", {
        player,
        name: safeName(stringArg(args, "name") ?? "minecraft-client", "minecraft-client"),
        fullWindow: boolArg(args, "fullWindow", true),
        timeoutMs: optionalIntArg(args, "timeoutMs") ?? 60000,
        noOsScreenshot: true,
        noCoordinateClicking: true
      }, true);
      if (!response.ok) {
        return fail(response.code, response.message, response.data ?? {});
      }
      const data = response.data ?? {};
      if (typeof data.path === "string" && data.path.trim()) {
        context.webAdmin.screenshots.push(data.path);
      }
      return ok("Minecraft client screenshot saved.", {
        ...data,
        reportCanReferenceScreenshotPath: true
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
    case "GUI_NOT_OPEN":
      return "GUI_NOT_OPEN";
    case "UNSUPPORTED_GUI":
      return "UNSUPPORTED_GUI";
    case "SCREEN_MISMATCH":
      return "VALIDATION_ERROR";
    case "CLIENT_TIMEOUT":
      return "TIMEOUT";
    case "CLIENT_TESTBRIDGE_UNAVAILABLE":
    case "SCREEN_OPERATION_FAILED":
      return "COMMAND_FAILED";
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

function guiPlayerSchema(): JsonObject {
  return {
    type: "object",
    additionalProperties: false,
    properties: { player: { type: "string" } },
    required: ["player"]
  };
}

function guiSlotSchema(): JsonObject {
  return {
    type: "object",
    additionalProperties: false,
    properties: {
      player: { type: "string" },
      target: { type: "string", enum: ["container_template", "single_item_submit"] },
      slot: { type: "number", minimum: 0 },
      slotIndex: { type: "number", minimum: 0 }
    },
    required: ["player"]
  };
}

function guiSlotBody(args: JsonObject): JsonObject {
  const body: JsonObject = {
    player: stringArg(args, "player") ?? "",
    target: stringArg(args, "target") ?? "",
    itemId: stringArg(args, "itemId") ?? "",
    count: optionalIntArg(args, "count") ?? 0,
    reason: stringArg(args, "reason") ?? ""
  };
  const slot = optionalIntArg(args, "slot");
  if (slot !== undefined) {
    body.slot = slot;
  }
  const slotIndex = optionalIntArg(args, "slotIndex");
  if (slotIndex !== undefined) {
    body.slotIndex = slotIndex;
  }
  return body;
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

function optionalPositionArg(args: JsonObject, key: string): JsonObject | undefined {
  const value = args[key];
  if (!isObject(value)) {
    return undefined;
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

function boolArg(args: JsonObject, key: string, fallback: boolean): boolean {
  const value = args[key];
  return typeof value === "boolean" ? value : fallback;
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
