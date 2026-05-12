import { createWriteStream, existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import type { JsonObject, ToolDefinition, TzzTestMcpConfig } from "../types.js";
import { fail, ok, type ToolErrorCode } from "../results.js";
import { ensureAllowedUrl, ensureDirectory, redactSecrets, resolveRepoOutputDir, tailText, timestamp } from "../safety.js";
import { buildGradleSpawnCommand } from "../gradleSpawn.js";
import { testBridgeTools } from "./testbridge.js";

type RuntimeState = {
  child?: ChildProcessWithoutNullStreams;
  pid?: number;
  startedAt?: string;
  preset?: string;
  logFile?: string;
  lastExitCode?: number | null;
  lastSignal?: string;
  lastError?: string;
};

const runtime: RuntimeState = {};
const RUN_CLIENT_ARGS = ["--no-daemon", "runClient"] as const;
const QUICK_PLAY_SINGLEPLAYER_FLAG = "--quickPlaySingleplayer";
const DEFAULT_TEST_WORLD_NAME = "TZZ_MCP_TEST_WORLD";

export function minecraftTools(): ToolDefinition[] {
  return [
    minecraftStartClientTool(),
    minecraftStatusTool(),
    minecraftWaitWebAdminTool(),
    minecraftStopTool(),
    ...testBridgeTools()
  ];
}

function minecraftStartClientTool(): ToolDefinition {
  return {
    name: "minecraft.start_client",
    description: "Start the whitelisted Fabric dev client preset with Gradle runClient. This is not arbitrary shell and does not click the Minecraft GUI.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        autoEnterWorld: { type: "boolean" },
        worldName: { type: "string" },
        waitForWebAdmin: { type: "boolean" },
        waitTimeoutSeconds: { type: "number", minimum: 1, maximum: 900 }
      }
    },
    async handler(args, context) {
      if (isManagedProcessRunning()) {
        return ok("Minecraft dev client is already managed by this MCP session.", await statusData(context.config));
      }
      try {
        const options: { autoEnterWorld: boolean; worldName?: string } = { autoEnterWorld: args.autoEnterWorld === true };
        const requestedWorldName = stringArg(args, "worldName");
        if (requestedWorldName !== undefined) {
          options.worldName = requestedWorldName;
        }
        const launch = startRunClient(context.config, options);
        if (args.waitForWebAdmin === true) {
          const wait = await waitForWebAdmin(context.config, numberArg(args, "waitTimeoutSeconds") ?? 180);
          return ok("Minecraft dev client started; WebAdmin wait completed.", {
            ...(await statusData(context.config)),
            webAdminWait: wait
          });
        }
        return ok("Minecraft dev client started.", {
          preset: "runClient",
          pid: runtime.pid ?? 0,
          startedAt: runtime.startedAt ?? "",
          logFile: launch.logFile,
          autoEnterWorld: launch.autoEnterWorld,
          worldName: launch.worldName,
          runClientArgs: launch.gradleArgs
        });
      } catch (error) {
        runtime.lastError = errorMessage(error);
        if (error instanceof StartClientError) {
          return fail(error.code, runtime.lastError, error.data);
        }
        return fail("COMMAND_FAILED", runtime.lastError);
      }
    }
  };
}

function minecraftStatusTool(): ToolDefinition {
  return {
    name: "minecraft.status",
    description: "Return the MCP-managed runClient process status, runtime log tail, and localhost WebAdmin readiness.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        logLines: { type: "number", minimum: 1, maximum: 500 }
      }
    },
    readOnlyHint: true,
    async handler(args, context) {
      return ok("Minecraft runtime status returned.", await statusData(context.config, numberArg(args, "logLines") ?? 80));
    }
  };
}

function minecraftWaitWebAdminTool(): ToolDefinition {
  return {
    name: "minecraft.wait_webadmin",
    description: "Wait until the configured localhost WebAdmin URL responds. External hosts are rejected.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        url: { type: "string" },
        timeoutSeconds: { type: "number", minimum: 1, maximum: 900 }
      }
    },
    async handler(args, context) {
      try {
        const result = await waitForWebAdmin(context.config, numberArg(args, "timeoutSeconds") ?? 180, stringArg(args, "url"));
        if (result.ready === true) {
          return ok("WebAdmin is reachable.", result);
        }
        return fail("TIMEOUT", "Timed out waiting for localhost WebAdmin.", result);
      } catch (error) {
        return fail("SECURITY_DENIED", errorMessage(error));
      }
    }
  };
}

function minecraftStopTool(): ToolDefinition {
  return {
    name: "minecraft.stop",
    description: "Stop only the runClient process started by this MCP session. It never kills arbitrary Java processes.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        timeoutSeconds: { type: "number", minimum: 1, maximum: 60 }
      }
    },
    async handler(args, context) {
      if (!isManagedProcessRunning() || !runtime.child) {
        return ok("No MCP-managed Minecraft dev client is running.", await statusData(context.config));
      }
      const child = runtime.child;
      const timeoutMs = (numberArg(args, "timeoutSeconds") ?? 10) * 1000;
      child.kill("SIGTERM");
      const stopped = await waitForExit(child, timeoutMs);
      if (stopped) {
        return ok("MCP-managed Minecraft dev client stopped.", await statusData(context.config));
      }
      return fail("COMMAND_FAILED", "Managed process did not exit after SIGTERM; close the Minecraft dev client manually.", await statusData(context.config));
    }
  };
}

function startRunClient(config: TzzTestMcpConfig, options: { autoEnterWorld: boolean; worldName?: string }): { logFile: string; autoEnterWorld: boolean; worldName: string; gradleArgs: string[] } {
  const runtimeDir = ensureDirectory(resolveRepoOutputDir(config.repoRoot, path.join(config.reportsDir, "runtime")));
  const logFile = path.join(runtimeDir, `${timestamp()}-runClient.log`);
  const stream = createWriteStream(logFile, { encoding: "utf8" });
  const launch = buildRunClientArgs(config, options);
  let spawnCommand;
  try {
    spawnCommand = buildGradleSpawnCommand(config.repoRoot, launch.gradleArgs);
  } catch (error) {
    stream.end(`${errorMessage(error)}\n`);
    throw error;
  }
  const child = spawn(spawnCommand.command, spawnCommand.args, {
    cwd: config.repoRoot,
    shell: false,
    windowsHide: false,
    env: process.env
  });

  runtime.child = child;
  runtime.pid = child.pid ?? 0;
  runtime.startedAt = new Date().toISOString();
  runtime.preset = "runClient";
  runtime.logFile = logFile;
  delete runtime.lastExitCode;
  delete runtime.lastSignal;
  delete runtime.lastError;

  child.stdout.on("data", (chunk: Buffer) => stream.write(redactSecrets(chunk.toString("utf8"))));
  child.stderr.on("data", (chunk: Buffer) => stream.write(redactSecrets(chunk.toString("utf8"))));
  child.on("error", (error) => {
    runtime.lastError = redactSecrets(error.message);
    stream.write(`${error.name}: ${runtime.lastError}\n`);
  });
  child.on("close", (exitCode, signal) => {
    runtime.lastExitCode = exitCode;
    runtime.lastSignal = signal ?? "";
    stream.end(`\n[process closed exitCode=${exitCode ?? ""} signal=${signal ?? ""}]\n`);
  });
  return {
    logFile,
    autoEnterWorld: launch.autoEnterWorld,
    worldName: launch.worldName,
    gradleArgs: launch.gradleArgs
  };
}

function buildRunClientArgs(config: TzzTestMcpConfig, options: { autoEnterWorld: boolean; worldName?: string }): { gradleArgs: string[]; autoEnterWorld: boolean; worldName: string } {
  const gradleArgs: string[] = [...RUN_CLIENT_ARGS];
  if (options.autoEnterWorld !== true) {
    return {
      gradleArgs,
      autoEnterWorld: false,
      worldName: ""
    };
  }
  const worldName = sanitizeWorldName(options.worldName ?? process.env.TZZ_TEST_WORLD_NAME ?? DEFAULT_TEST_WORLD_NAME);
  const levelDat = path.join(config.repoRoot, "run", "saves", worldName, "level.dat");
  if (!existsSync(levelDat)) {
    throw new StartClientError("NOT_FOUND", `测试世界不存在，请先创建 ${worldName}。`, {
      worldName,
      expectedLevelDat: levelDat
    });
  }
  gradleArgs.push(`--args=${QUICK_PLAY_SINGLEPLAYER_FLAG} ${worldName}`);
  return {
    gradleArgs,
    autoEnterWorld: true,
    worldName
  };
}

function sanitizeWorldName(rawWorldName: string): string {
  const worldName = rawWorldName.trim();
  if (!worldName) {
    throw new StartClientError("VALIDATION_ERROR", "worldName 不能为空。");
  }
  if (worldName.length > 64) {
    throw new StartClientError("VALIDATION_ERROR", "worldName 不能超过 64 个字符。");
  }
  if (worldName.includes("\0") || worldName.includes("/") || worldName.includes("\\") || worldName.includes("..") || path.isAbsolute(worldName)) {
    throw new StartClientError("SECURITY_DENIED", "worldName 不能包含路径分隔符、..、NUL 或绝对路径。");
  }
  if (!/^[A-Za-z0-9._-]+$/.test(worldName)) {
    throw new StartClientError("VALIDATION_ERROR", "worldName 只能包含英文字母、数字、点、下划线和短横线。");
  }
  return worldName;
}

async function statusData(config: TzzTestMcpConfig, logLines = 80): Promise<JsonObject> {
  const webAdminReady = await checkWebAdmin(config);
  return {
    managedProcessRunning: isManagedProcessRunning(),
    pid: runtime.pid ?? 0,
    startedAt: runtime.startedAt ?? "",
    preset: runtime.preset ?? "",
    logFile: runtime.logFile ?? "",
    lastExitCode: runtime.lastExitCode ?? null,
    lastSignal: runtime.lastSignal ?? "",
    lastError: runtime.lastError ?? "",
    webAdminReady,
    logTail: readRuntimeLogTail(logLines)
  };
}

async function waitForWebAdmin(config: TzzTestMcpConfig, timeoutSeconds: number, rawUrl?: string): Promise<JsonObject> {
  const url = ensureAllowedUrl(rawUrl || config.webAdminUrl, config);
  const started = Date.now();
  let lastStatus = 0;
  let lastError = "";
  while (Date.now() - started < Math.max(1, timeoutSeconds) * 1000) {
    const checked = await checkWebAdmin(config, url.toString());
    lastStatus = Number(checked.status ?? 0);
    lastError = String(checked.error ?? "");
    if (checked.ready === true) {
      return {
        ready: true,
        url: url.toString(),
        status: lastStatus,
        durationMs: Date.now() - started
      };
    }
    await delay(1000);
  }
  return {
    ready: false,
    url: url.toString(),
    status: lastStatus,
    error: lastError,
    durationMs: Date.now() - started
  };
}

async function checkWebAdmin(config: TzzTestMcpConfig, rawUrl?: string): Promise<JsonObject> {
  try {
    const url = ensureAllowedUrl(rawUrl || config.webAdminUrl, config);
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 2000);
    try {
      const response = await fetch(url, { method: "GET", signal: controller.signal, redirect: "manual" });
      return {
        ready: response.status > 0 && response.status < 500,
        status: response.status,
        url: url.toString()
      };
    } finally {
      clearTimeout(timer);
    }
  } catch (error) {
    return {
      ready: false,
      status: 0,
      error: errorMessage(error)
    };
  }
}

function readRuntimeLogTail(lines: number): string {
  const file = runtime.logFile;
  if (!file || !existsSync(file)) {
    return "";
  }
  const text = readFileSync(file, "utf8");
  const selected = text.split(/\r?\n/).slice(-Math.max(1, Math.min(500, Math.floor(lines)))).join("\n");
  return tailText(selected, 32768).text;
}

function isManagedProcessRunning(): boolean {
  const child = runtime.child;
  return Boolean(child && child.exitCode === null && runtime.lastExitCode === undefined);
}

function waitForExit(child: ChildProcessWithoutNullStreams, timeoutMs: number): Promise<boolean> {
  if (child.exitCode !== null) {
    return Promise.resolve(true);
  }
  return new Promise((resolve) => {
    const timer = setTimeout(() => resolve(false), timeoutMs);
    child.once("close", () => {
      clearTimeout(timer);
      resolve(true);
    });
  });
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function stringArg(args: JsonObject, key: string): string | undefined {
  const value = args[key];
  return typeof value === "string" && value.trim() ? value.trim() : undefined;
}

function numberArg(args: JsonObject, key: string): number | undefined {
  const value = args[key];
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function errorMessage(error: unknown): string {
  return redactSecrets(error instanceof Error ? error.message : String(error));
}

class StartClientError extends Error {
  readonly code: Exclude<ToolErrorCode, "OK">;
  readonly data: JsonObject;

  constructor(code: Exclude<ToolErrorCode, "OK">, message: string, data: JsonObject = {}) {
    super(message);
    this.code = code;
    this.data = data;
  }
}
