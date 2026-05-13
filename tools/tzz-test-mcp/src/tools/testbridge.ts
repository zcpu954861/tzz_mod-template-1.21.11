import { writeFileSync } from "node:fs";
import type { Json, JsonObject, ToolDefinition, TzzTestMcpConfig } from "../types.js";
import { fail, ok, type ToolErrorCode } from "../results.js";
import { ensureAllowedUrl, ensureResponsiveReportPath, redactSecrets, safeName } from "../safety.js";

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
    testBridgeGuiSelectRequirementTool(),
    testBridgeGuiAddRequirementTool(),
    testBridgeGuiDeleteRequirementTool(),
    testBridgeGuiSetCountModeTool(),
    testBridgeGuiSetRequirementEnabledTool(),
    testBridgeGuiSetMatcherOptionsTool(),
    testBridgeGuiSetConsumeTool(),
    testBridgeGuiSetGlobalTool(),
    testBridgeGuiSaveTool(),
    testBridgeGuiCancelTool(),
    testBridgeClientScreenshotTool(),
    testBridgeClientSetWindowSizeTool(),
    testBridgeClientSetGuiScaleTool(),
    testBridgeClientScreenshotMatrixTool()
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

function testBridgeGuiSelectRequirementTool(): ToolDefinition {
  return {
    name: "minecraft.gui_select_requirement",
    description: "Select one itemSubmit requirement row in the supported unified itemSubmit Minecraft GUI.",
    inputSchema: guiRequirementSlotSchema(),
    async handler(args, context) {
      const validation = requireRequirementSlot(args, "minecraft.gui_select_requirement");
      if (validation) {
        return validation;
      }
      return await requestTool(context.config, "POST", "gui/select-requirement", guiSlotBody(args), "GUI requirement selected.");
    }
  };
}

function testBridgeGuiAddRequirementTool(): ToolDefinition {
  return {
    name: "minecraft.gui_add_requirement",
    description: "Add a new itemSubmit requirement in the unified itemSubmit Minecraft GUI. Optional itemId/count pre-fills the ghost template and never touches real inventory.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        target: { type: "string", enum: ["single_item_submit"] },
        itemId: { type: "string" },
        count: { type: "number", minimum: 1, maximum: 64000 }
      },
      required: ["player"]
    },
    async handler(args, context) {
      return await requestTool(context.config, "POST", "gui/add-requirement", guiSlotBody(args), "GUI requirement added.");
    }
  };
}

function testBridgeGuiDeleteRequirementTool(): ToolDefinition {
  return {
    name: "minecraft.gui_delete_requirement",
    description: "Delete the selected itemSubmit requirement through the unified GUI delete-confirm path.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        target: { type: "string", enum: ["single_item_submit"] },
        slot: { type: "number", minimum: 0 },
        slotIndex: { type: "number", minimum: 0 },
        confirmed: { type: "boolean" }
      },
      required: ["player"]
    },
    async handler(args, context) {
      const validation = requireRequirementSlot(args, "minecraft.gui_delete_requirement");
      if (validation) {
        return validation;
      }
      return await requestTool(context.config, "POST", "gui/delete-requirement", guiSlotBody(args), "GUI requirement delete handled.");
    }
  };
}

function testBridgeGuiSetCountModeTool(): ToolDefinition {
  return {
    name: "minecraft.gui_set_count_mode",
    description: "Set countMode for the selected itemSubmit requirement.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        target: { type: "string", enum: ["single_item_submit"] },
        slot: { type: "number", minimum: 0 },
        slotIndex: { type: "number", minimum: 0 },
        countMode: { type: "string", enum: ["at_least", "exactly", "at_most", "ignore"] }
      },
      required: ["player", "countMode"]
    },
    async handler(args, context) {
      const validation = requireRequirementSlot(args, "minecraft.gui_set_count_mode");
      if (validation) {
        return validation;
      }
      return await requestTool(context.config, "POST", "gui/set-count-mode", guiSlotBody(args), "GUI requirement countMode set.");
    }
  };
}

function testBridgeGuiSetRequirementEnabledTool(): ToolDefinition {
  return {
    name: "minecraft.gui_set_requirement_enabled",
    description: "Enable or disable one itemSubmit requirement in the unified GUI.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        target: { type: "string", enum: ["single_item_submit"] },
        slot: { type: "number", minimum: 0 },
        slotIndex: { type: "number", minimum: 0 },
        enabled: { type: "boolean" }
      },
      required: ["player", "enabled"]
    },
    async handler(args, context) {
      const validation = requireRequirementSlot(args, "minecraft.gui_set_requirement_enabled");
      if (validation) {
        return validation;
      }
      return await requestTool(context.config, "POST", "gui/set-requirement-enabled", guiSlotBody(args), "GUI requirement enabled flag set.");
    }
  };
}

function testBridgeGuiSetMatcherOptionsTool(): ToolDefinition {
  return {
    name: "minecraft.gui_set_matcher_options",
    description: "Set matcher options for one itemSubmit requirement in the unified GUI.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        target: { type: "string", enum: ["single_item_submit"] },
        slot: { type: "number", minimum: 0 },
        slotIndex: { type: "number", minimum: 0 },
        matchDamage: { type: "boolean" },
        matchCustomName: { type: "boolean" },
        matchLore: { type: "boolean" },
        matchCustomData: { type: "boolean" },
        matchComponents: { type: "boolean" },
        options: {
          type: "object",
          additionalProperties: false,
          properties: {
            matchDamage: { type: "boolean" },
            matchCustomName: { type: "boolean" },
            matchLore: { type: "boolean" },
            matchCustomData: { type: "boolean" },
            matchComponents: { type: "boolean" }
          }
        }
      },
      required: ["player"]
    },
    async handler(args, context) {
      const validation = requireRequirementSlot(args, "minecraft.gui_set_matcher_options");
      if (validation) {
        return validation;
      }
      return await requestTool(context.config, "POST", "gui/set-matcher-options", guiSlotBody(args), "GUI matcher options set.");
    }
  };
}

function testBridgeGuiSetConsumeTool(): ToolDefinition {
  return {
    name: "minecraft.gui_set_consume",
    description: "Set global consume switch/order and per-requirement consumeCount for the unified itemSubmit GUI.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        target: { type: "string", enum: ["single_item_submit"] },
        slot: { type: "number", minimum: 0 },
        slotIndex: { type: "number", minimum: 0 },
        consumeEnabled: { type: "boolean" },
        consumeOrder: { type: "string", enum: ["hotbar_first", "main_inventory_first"] },
        consumeCount: { type: "number", minimum: 1, maximum: 64000 }
      },
      required: ["player", "consumeCount"]
    },
    async handler(args, context) {
      const validation = requireRequirementSlot(args, "minecraft.gui_set_consume");
      if (validation) {
        return validation;
      }
      return await requestTool(context.config, "POST", "gui/set-consume", guiSlotBody(args), "GUI consume settings set.");
    }
  };
}

function testBridgeGuiSetGlobalTool(): ToolDefinition {
  return {
    name: "minecraft.gui_set_global",
    description: "Set global itemSubmit fields in the unified GUI without changing raw SignalDeviceData.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        target: { type: "string", enum: ["single_item_submit"] },
        itemSubmitEnabled: { type: "boolean" },
        consumeEnabled: { type: "boolean" },
        consumeOrder: { type: "string", enum: ["hotbar_first", "main_inventory_first"] },
        vanillaPolicy: { type: "string", enum: ["allow", "require_item_match"] }
      },
      required: ["player"]
    },
    async handler(args, context) {
      return await requestTool(context.config, "POST", "gui/set-global", guiSlotBody(args), "GUI global itemSubmit settings set.");
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

function testBridgeClientSetWindowSizeTool(): ToolDefinition {
  return {
    name: "minecraft.client_set_window_size",
    description: "Resize the Minecraft client window through the TestBridge client payload. This does not use OS input automation.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        width: { type: "number", minimum: 320, maximum: 7680 },
        height: { type: "number", minimum: 240, maximum: 4320 },
        timeoutMs: { type: "number", minimum: 1000, maximum: 120000 }
      },
      required: ["player", "width", "height"]
    },
    async handler(args, context) {
      const player = stringArg(args, "player") ?? "";
      if (!player.trim()) {
        return fail("VALIDATION_ERROR", "player is required.");
      }
      const width = intArg(args, "width");
      const height = intArg(args, "height");
      if (width < 320 || width > 7680 || height < 240 || height > 4320) {
        return fail("VALIDATION_ERROR", "width/height are outside the allowed Minecraft client window range.");
      }
      return await requestTool(context.config, "POST", "client/window-size", {
        player,
        width,
        height,
        timeoutMs: optionalIntArg(args, "timeoutMs") ?? 15000,
        noOsMouseKeyboard: true,
        noCoordinateClicking: true
      }, "Minecraft client window size set.");
    }
  };
}

function testBridgeClientSetGuiScaleTool(): ToolDefinition {
  return {
    name: "minecraft.client_set_gui_scale",
    description: "Set or restore Minecraft GUI scale through the TestBridge client payload. This does not write options.txt.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        guiScale: { type: "number", minimum: 0, maximum: 4 },
        restoreOriginal: { type: "boolean" },
        timeoutMs: { type: "number", minimum: 1000, maximum: 120000 }
      },
      required: ["player"]
    },
    async handler(args, context) {
      const player = stringArg(args, "player") ?? "";
      if (!player.trim()) {
        return fail("VALIDATION_ERROR", "player is required.");
      }
      const restoreOriginal = boolArg(args, "restoreOriginal", false);
      const guiScale = optionalIntArg(args, "guiScale");
      if (!restoreOriginal && (guiScale === undefined || guiScale < 0 || guiScale > 4)) {
        return fail("VALIDATION_ERROR", "guiScale must be 0..4 unless restoreOriginal=true.");
      }
      return await requestTool(context.config, "POST", "client/gui-scale", {
        player,
        guiScale: guiScale ?? 0,
        restoreOriginal,
        timeoutMs: optionalIntArg(args, "timeoutMs") ?? 15000,
        doesNotWriteOptionsFile: true
      }, restoreOriginal ? "Minecraft GUI scale restored." : "Minecraft GUI scale set.");
    }
  };
}

function testBridgeClientScreenshotMatrixTool(): ToolDefinition {
  return {
    name: "minecraft.client_screenshot_matrix",
    description: "Capture Minecraft client screenshots across window sizes and GUI scales. It does not judge visuals; user review is required before checkpoint.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        player: { type: "string" },
        name: { type: "string" },
        targetGui: { type: "string", enum: ["current", "single_item_submit", "container_template"] },
        sizes: {
          type: "array",
          items: {
            type: "object",
            additionalProperties: false,
            properties: {
              width: { type: "number" },
              height: { type: "number" },
              label: { type: "string" }
            },
            required: ["width", "height"]
          }
        },
        guiScales: { type: "array", items: { type: "number" } },
        timeoutMs: { type: "number", minimum: 1000, maximum: 120000 },
        settleMs: { type: "number", minimum: 100, maximum: 10000 },
        restoreOriginal: { type: "boolean" }
      }
    },
    async handler(args, context) {
      const player = await resolvePlayerName(context.config, stringArg(args, "player"));
      if (!player.ok) {
        return fail(player.code, player.message, player.data ?? {});
      }
      const targetGui = stringArg(args, "targetGui") ?? "current";
      const sizes = parseMatrixSizes(args.sizes);
      const guiScales = parseGuiScales(args.guiScales);
      const timeoutMs = optionalIntArg(args, "timeoutMs") ?? 60000;
      const settleMs = optionalIntArg(args, "settleMs") ?? 1000;
      const restoreOriginal = boolArg(args, "restoreOriginal", true);
      const name = safeName(stringArg(args, "name") ?? `minecraft-${targetGui}-screenshot-matrix`, "minecraft-screenshot-matrix");
      const started = new Date().toISOString();
      const warnings: string[] = [];
      const captures: JsonObject[] = [];
      let originalWindow: JsonObject | undefined;
      let originalGuiScale: JsonObject | undefined;

      if (targetGui !== "current") {
        const current = await testBridgeFetch(context.config, "GET", `gui/current?player=${encodeURIComponent(player.player)}`, undefined, true);
        if (!current.ok) {
          return fail(current.code, current.message, current.data ?? {});
        }
        if (current.data.type !== targetGui) {
          warnings.push(`Target GUI ${targetGui} is not currently open; current type is ${String(current.data.type ?? "unknown")}.`);
        }
      }

      for (const size of sizes) {
        for (const guiScale of guiScales) {
          const cell: JsonObject = {
            ok: false,
            player: player.player,
            targetGui,
            width: size.width,
            height: size.height,
            label: size.label ?? "",
            guiScale,
            manualVisualReviewRequired: true
          };
          const resize = await testBridgeFetch(context.config, "POST", "client/window-size", {
            player: player.player,
            width: size.width,
            height: size.height,
            timeoutMs: 15000,
            noOsMouseKeyboard: true,
            noCoordinateClicking: true
          }, true);
          if (resize.ok) {
            originalWindow ??= resize.data.previousWindow as JsonObject | undefined;
            cell.window = resize.data;
          } else {
            cell.code = resize.code;
            cell.message = resize.message;
            captures.push(cell);
            continue;
          }
          const scale = await testBridgeFetch(context.config, "POST", "client/gui-scale", {
            player: player.player,
            guiScale,
            timeoutMs: 15000,
            doesNotWriteOptionsFile: true
          }, true);
          if (scale.ok) {
            originalGuiScale ??= scale.data.originalGuiScaleState as JsonObject | undefined;
            cell.guiScaleResult = scale.data;
          } else {
            cell.code = scale.code;
            cell.message = scale.message;
            captures.push(cell);
            continue;
          }
          await delay(settleMs);
          const screenshotName = `${name}-${targetGui}-${size.label ?? "window"}-${size.width}x${size.height}-scale${guiScale}`;
          const shot = await testBridgeFetch(context.config, "POST", "client/screenshot", {
            player: player.player,
            name: screenshotName,
            fullWindow: true,
            timeoutMs,
            noOsScreenshot: true,
            noCoordinateClicking: true
          }, true);
          if (shot.ok) {
            cell.ok = true;
            cell.code = "OK";
            cell.message = "OK";
            cell.screenshotPath = shot.data.path ?? "";
            cell.screenshot = shot.data;
            if (typeof shot.data.path === "string" && shot.data.path.trim()) {
              context.webAdmin.screenshots.push(shot.data.path);
            }
          } else {
            cell.code = shot.code;
            cell.message = shot.message;
          }
          captures.push(cell);
        }
      }

      const restoreResults: JsonObject[] = [];
      if (restoreOriginal) {
        if (originalWindow && typeof originalWindow.width === "number" && typeof originalWindow.height === "number") {
          const restored = await testBridgeFetch(context.config, "POST", "client/window-size", {
            player: player.player,
            width: originalWindow.width,
            height: originalWindow.height,
            timeoutMs: 15000,
            noOsMouseKeyboard: true,
            noCoordinateClicking: true
          }, true);
          restoreResults.push({ type: "window", ok: restored.ok, result: restored.ok ? restored.data : { code: restored.code, message: restored.message } });
        } else {
          warnings.push("Original Minecraft window size was not available; window restore skipped.");
        }
        const restoredScale = await testBridgeFetch(context.config, "POST", "client/gui-scale", {
          player: player.player,
          restoreOriginal: true,
          timeoutMs: 15000,
          doesNotWriteOptionsFile: true
        }, true);
        restoreResults.push({ type: "guiScale", ok: restoredScale.ok, result: restoredScale.ok ? restoredScale.data : { code: restoredScale.code, message: restoredScale.message } });
        if (!originalGuiScale) {
          warnings.push("Original GUI scale state was not captured before restore request.");
        }
      }

      const passed = captures.filter((entry) => entry.ok === true).length;
      const failed = captures.length - passed;
      const reportPath = writeMinecraftMatrixReport(context.config, {
        name,
        started,
        finished: new Date().toISOString(),
        player: player.player,
        targetGui,
        sizes: sizes.map((size) => ({ width: size.width, height: size.height, label: size.label ?? "" })),
        guiScales,
        captures,
        warnings,
        restoreResults,
        passed,
        failed,
        manualVisualReviewRequired: true,
        userApprovalRequiredBeforeCheckpoint: true
      });
      return ok(failed > 0 ? "Minecraft client screenshot matrix completed with failures." : "Minecraft client screenshot matrix captured.", {
        reportPath,
        player: player.player,
        targetGui,
        screenshots: captures.map((entry) => entry.screenshotPath).filter((value): value is string => typeof value === "string" && value.length > 0),
        passed,
        failed,
        warnings,
        restoreResults,
        manualVisualReviewRequired: true,
        userApprovalRequiredBeforeCheckpoint: true
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
    case "CLIENT_WINDOW_NOT_READY":
    case "SCREENSHOT_BUSY":
    case "UNSUPPORTED_ENVIRONMENT":
      return "COMMAND_FAILED";
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

function guiRequirementSlotSchema(): JsonObject {
  return {
    type: "object",
    additionalProperties: false,
    properties: {
      player: { type: "string" },
      target: { type: "string", enum: ["single_item_submit"] },
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
    reason: stringArg(args, "reason") ?? "",
    countMode: stringArg(args, "countMode") ?? "",
    consumeOrder: stringArg(args, "consumeOrder") ?? "",
    vanillaPolicy: stringArg(args, "vanillaPolicy") ?? "",
    consumeCount: optionalIntArg(args, "consumeCount") ?? 0
  };
  for (const key of ["enabled", "confirmed", "itemSubmitEnabled", "consumeEnabled", "matchDamage", "matchCustomName", "matchLore", "matchCustomData", "matchComponents"]) {
    if (typeof args[key] === "boolean") {
      body[key] = args[key] as boolean;
    }
  }
  const options: JsonObject = {};
  if (args.options && typeof args.options === "object" && !Array.isArray(args.options)) {
    for (const key of ["matchDamage", "matchCustomName", "matchLore", "matchCustomData", "matchComponents"]) {
      const value = (args.options as JsonObject)[key];
      if (typeof value === "boolean") {
        options[key] = value;
      }
    }
  }
  for (const key of ["matchDamage", "matchCustomName", "matchLore", "matchCustomData", "matchComponents"]) {
    if (typeof args[key] === "boolean") {
      options[key] = args[key] as boolean;
    }
  }
  if (Object.keys(options).length > 0) {
    body.options = options;
  }
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

function requireRequirementSlot(args: JsonObject, toolName: string) {
  if (optionalIntArg(args, "slot") === undefined && optionalIntArg(args, "slotIndex") === undefined) {
    return fail("VALIDATION_ERROR", `${toolName} requires slot or slotIndex for the requirement row.`);
  }
  return undefined;
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

type MatrixSize = { width: number; height: number; label?: string };

const DEFAULT_MINECRAFT_MATRIX_SIZES: MatrixSize[] = [
  { width: 854, height: 480, label: "small" },
  { width: 1280, height: 720, label: "720p" },
  { width: 1920, height: 1080, label: "1080p" },
  { width: 2560, height: 1440, label: "2k" },
  { width: 3840, height: 2160, label: "4k" }
];

const DEFAULT_GUI_SCALES = [2, 3, 4];

function parseMatrixSizes(value: Json | undefined): MatrixSize[] {
  if (!Array.isArray(value)) {
    return DEFAULT_MINECRAFT_MATRIX_SIZES;
  }
  const sizes = value
    .map((entry) => isObject(entry) ? matrixSize(entry) : undefined)
    .filter((entry): entry is MatrixSize => Boolean(entry));
  return sizes.length > 0 ? sizes : DEFAULT_MINECRAFT_MATRIX_SIZES;
}

function matrixSize(value: JsonObject): MatrixSize | undefined {
  const width = typeof value.width === "number" && Number.isFinite(value.width) ? Math.trunc(value.width) : 0;
  const height = typeof value.height === "number" && Number.isFinite(value.height) ? Math.trunc(value.height) : 0;
  if (width < 320 || width > 7680 || height < 240 || height > 4320) {
    return undefined;
  }
  const label = typeof value.label === "string" ? safeName(value.label, "") : "";
  return label ? { width, height, label } : { width, height };
}

function parseGuiScales(value: Json | undefined): number[] {
  if (!Array.isArray(value)) {
    return DEFAULT_GUI_SCALES;
  }
  const scales = value
    .filter((entry): entry is number => typeof entry === "number" && Number.isFinite(entry))
    .map((entry) => Math.trunc(entry))
    .filter((entry) => entry >= 2 && entry <= 4);
  return Array.from(new Set(scales.length > 0 ? scales : DEFAULT_GUI_SCALES));
}

async function resolvePlayerName(
  config: TzzTestMcpConfig,
  requested: string | undefined
): Promise<{ ok: true; player: string } | { ok: false; code: Exclude<ToolErrorCode, "OK">; message: string; data?: JsonObject }> {
  if (requested && requested.trim()) {
    return { ok: true, player: requested.trim() };
  }
  const players = await testBridgeFetch(config, "GET", "players", undefined, true);
  if (!players.ok) {
    const failure: { ok: false; code: Exclude<ToolErrorCode, "OK">; message: string; data?: JsonObject } = {
      ok: false,
      code: players.code,
      message: players.message
    };
    if (players.data !== undefined) {
      failure.data = players.data;
    }
    return failure;
  }
  const online = Array.isArray(players.data.players) ? players.data.players : Array.isArray(players.data.onlinePlayers) ? players.data.onlinePlayers : [];
  for (const entry of online) {
    if (isObject(entry) && typeof entry.name === "string" && entry.name.trim()) {
      return { ok: true, player: entry.name.trim() };
    }
  }
  return { ok: false, code: "NOT_FOUND", message: "No online player is available for Minecraft screenshot matrix." };
}

function writeMinecraftMatrixReport(config: TzzTestMcpConfig, summary: JsonObject): string {
  const reportPath = ensureResponsiveReportPath(config, summary.name ?? "minecraft-client-screenshot-matrix");
  const captures = Array.isArray(summary.captures) ? summary.captures as JsonObject[] : [];
  const lines = [
    "# Minecraft Client Screenshot Matrix",
    "",
    `- Started: ${summary.started ?? ""}`,
    `- Finished: ${summary.finished ?? ""}`,
    `- Player: ${summary.player ?? ""}`,
    `- Target GUI: ${summary.targetGui ?? ""}`,
    `- Passed: ${summary.passed ?? 0}`,
    `- Failed: ${summary.failed ?? 0}`,
    "",
    "本阶段不做自动图像识别，截图需要用户人工验收。用户确认前不得 checkpoint。",
    "",
    "## Screenshots",
    "",
    "| Window | GUI Scale | Result | Screenshot | Message |",
    "|---:|---:|---|---|---|"
  ];
  for (const capture of captures) {
    lines.push(`| ${capture.width ?? ""}x${capture.height ?? ""} | ${capture.guiScale ?? ""} | ${capture.ok === true ? "PASS" : `FAIL ${capture.code ?? ""}`} | ${capture.screenshotPath ?? ""} | ${capture.message ?? ""} |`);
  }
  const warnings = Array.isArray(summary.warnings) ? summary.warnings : [];
  if (warnings.length > 0) {
    lines.push("", "## Warnings", "");
    for (const warning of warnings) {
      lines.push(`- ${redactSecrets(String(warning))}`);
    }
  }
  lines.push("", "## Cleanup / Restore", "", "```json", JSON.stringify(summary.restoreResults ?? [], null, 2), "```");
  lines.push("", "## Raw Summary", "", "```json", JSON.stringify(summary, null, 2), "```", "");
  writeFileSync(reportPath, lines.join("\n"), "utf8");
  return reportPath;
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
