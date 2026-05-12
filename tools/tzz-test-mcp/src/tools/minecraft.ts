import { appendFileSync, createWriteStream, existsSync, readFileSync } from "node:fs";
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
  startedAtMs?: number;
  preset?: string;
  worldName?: string;
  autoEnterWorld?: boolean;
  logFile?: string;
  lastExitCode?: number | null;
  lastSignal?: string;
  lastError?: string;
};

const runtime: RuntimeState = {};
const RUN_CLIENT_ARGS = ["--no-daemon", "runClient"] as const;
const QUICK_PLAY_SINGLEPLAYER_FLAG = "--quickPlaySingleplayer";
const DEFAULT_TEST_WORLD_NAME = "TZZ_MCP_TEST_WORLD";
const WINDOWS_MANAGED_PROCESS_TREE_STOP = "taskkill.exe";
const WINDOWS_MANAGED_RUNCLIENT_PROCESS_QUERY = "powershell.exe";
const WINDOWS_RUNCLIENT_PROCESS_QUERY_SCRIPT = `
& {
param(
  [string]$repo,
  [string]$world,
  [Int64]$startedMs
)
$ErrorActionPreference = 'Stop'
$started = [DateTimeOffset]::FromUnixTimeMilliseconds($startedMs).UtcDateTime.AddSeconds(-60)
$comparison = [System.StringComparison]::OrdinalIgnoreCase
$items = @()
Get-CimInstance Win32_Process | ForEach-Object {
  $cmd = [string]$_.CommandLine
  if ([string]::IsNullOrWhiteSpace($cmd)) { return }
  $name = [string]$_.Name
  if ($name -ne 'java.exe' -and $name -ne 'java') { return }
  if ($cmd.IndexOf($repo, $comparison) -lt 0) { return }
  $isWrapper = $cmd.IndexOf('gradle-wrapper.jar', $comparison) -ge 0 -and $cmd.IndexOf('runClient', $comparison) -ge 0
  $isClient = $cmd.IndexOf('devlaunchinjector.Main', $comparison) -ge 0 -and $cmd.IndexOf('runClient', $comparison) -ge 0
  if (-not ($isWrapper -or $isClient)) { return }
  if ($world -and $cmd.IndexOf($world, $comparison) -lt 0) { return }
  $created = $_.CreationDate
  if ($created -and $created.ToUniversalTime() -lt $started) { return }
  $items += [pscustomobject]@{
    pid = [int]$_.ProcessId
    parentPid = [int]$_.ParentProcessId
    name = $name
    kind = $(if ($isWrapper) { 'gradle-wrapper' } else { 'minecraft-client' })
  }
}
@($items) | ConvertTo-Json -Compress
}
`;

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
      if (process.platform === "win32" && runtime.pid && runtime.pid > 0) {
        const forced = await stopWindowsManagedProcessTree(context.config, child, Math.min(45000, Math.max(15000, timeoutMs)));
        if (forced.ok) {
          markRuntimeStoppedAfterFallback("windows-managed-process-tree");
          return ok("MCP-managed Minecraft dev client stopped after Windows managed process tree fallback.", {
            ...(await statusData(context.config)),
            stopFallback: forced
          });
        }
        runtime.lastError = forced.error;
        return fail("COMMAND_FAILED", "Managed process did not exit after SIGTERM or Windows managed process tree fallback; close the Minecraft dev client manually.", {
          ...(await statusData(context.config)),
          stopFallback: forced
        });
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
  runtime.startedAtMs = Date.now();
  runtime.startedAt = new Date(runtime.startedAtMs).toISOString();
  runtime.preset = "runClient";
  runtime.worldName = launch.worldName;
  runtime.autoEnterWorld = launch.autoEnterWorld;
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
    worldName: runtime.worldName ?? "",
    autoEnterWorld: runtime.autoEnterWorld === true,
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

function appendRuntimeLog(line: string): void {
  const file = runtime.logFile;
  if (!file) {
    return;
  }
  try {
    appendFileSync(file, `${line}\n`, "utf8");
  } catch {
    // Runtime log append failures should not mask stop results.
  }
}

function isManagedProcessRunning(): boolean {
  const child = runtime.child;
  return Boolean(child && child.exitCode === null && runtime.lastExitCode === undefined);
}

function markRuntimeStoppedAfterFallback(signal: string): void {
  runtime.lastExitCode = runtime.lastExitCode ?? null;
  runtime.lastSignal = runtime.lastSignal || signal;
  delete runtime.child;
}

function waitForExit(child: ChildProcessWithoutNullStreams, timeoutMs: number): Promise<boolean> {
  if (child.exitCode !== null) {
    return Promise.resolve(true);
  }
  return new Promise((resolve) => {
    let settled = false;
    const finish = (value: boolean) => {
      if (settled) {
        return;
      }
      settled = true;
      clearTimeout(timer);
      child.off("close", onClose);
      resolve(value);
    };
    const onClose = () => finish(true);
    const timer = setTimeout(() => finish(false), timeoutMs);
    child.once("close", onClose);
  });
}

async function stopWindowsManagedProcessTree(config: TzzTestMcpConfig, child: ChildProcessWithoutNullStreams, timeoutMs: number): Promise<{ ok: boolean; command: string; pid: number; exitCode: number | null; signal: string; error: string; discoveredPids: number[]; remainingPids: number[]; attempts: JsonObject[] }> {
  const attempts: JsonObject[] = [];
  const managedPid = runtime.pid ?? 0;
  let firstError = "";
  if (managedPid > 0) {
    const initial = await taskkillWindowsPid(managedPid, timeoutMs);
    attempts.push(initial);
    if (!initial.ok && initial.error) {
      firstError = initial.error;
    }
  }
  if (await waitForExit(child, 3000)) {
    return {
      ok: true,
      command: WINDOWS_MANAGED_PROCESS_TREE_STOP,
      pid: managedPid,
      exitCode: null,
      signal: "",
      error: "",
      discoveredPids: [],
      remainingPids: [],
      attempts
    };
  }
  const discovered = await findWindowsManagedRunClientProcesses(config);
  const discoveredPids = discovered.map((item) => item.pid);
  for (const candidate of discovered) {
    if (candidate.pid === managedPid) {
      continue;
    }
    attempts.push(await taskkillWindowsPid(candidate.pid, timeoutMs));
  }
  const childStopped = await waitForExit(child, 3000);
  const remaining = await waitForNoWindowsManagedRunClientProcesses(config, Math.max(20000, timeoutMs));
  const remainingPids = remaining.map((item) => item.pid);
  const stoppedByCandidates = discoveredPids.length > 0 && remainingPids.length === 0;
  return {
    ok: childStopped || stoppedByCandidates,
    command: WINDOWS_MANAGED_PROCESS_TREE_STOP,
    pid: managedPid,
    exitCode: null,
    signal: childStopped ? "child-close" : (stoppedByCandidates ? "safe-candidate-stop" : ""),
    error: remainingPids.length ? `managed runClient processes still running: ${remainingPids.join(", ")}` : firstError,
    discoveredPids,
    remainingPids,
    attempts
  };
}

function taskkillWindowsPid(pid: number, timeoutMs: number): Promise<JsonObject & { ok: boolean; error: string }> {
  return new Promise((resolve) => {
    const pidText = String(pid);
    appendRuntimeLog(`[mcp stop fallback] ${WINDOWS_MANAGED_PROCESS_TREE_STOP} /pid ${pidText} /t /f`);
    const killer = spawn(WINDOWS_MANAGED_PROCESS_TREE_STOP, ["/pid", pidText, "/t", "/f"], {
      shell: false,
      windowsHide: true,
      env: process.env
    });
    let stderr = "";
    let exitCode: number | null = null;
    let signal = "";
    let settled = false;
    const finish = (okValue: boolean, error = "") => {
      if (settled) {
        return;
      }
      settled = true;
      clearTimeout(timer);
      killer.off("close", onKillerClose);
      killer.off("error", onKillerError);
      resolve({
        ok: okValue,
        command: WINDOWS_MANAGED_PROCESS_TREE_STOP,
        pid,
        exitCode,
        signal,
        error: redactSecrets(error || stderr.trim())
      });
    };
    const onKillerClose = (code: number | null, closeSignal: NodeJS.Signals | null) => {
      exitCode = code;
      signal = closeSignal ?? "";
      finish(code === 0);
    };
    const onKillerError = (error: Error) => {
      stderr += `${error.name}: ${error.message}\n`;
      finish(false, error.message);
    };
    const timer = setTimeout(() => {
      finish(false, `${WINDOWS_MANAGED_PROCESS_TREE_STOP} timed out`);
    }, timeoutMs);
    killer.stdout.on("data", (chunk: Buffer) => appendRuntimeLog(`[mcp stop fallback stdout] ${redactSecrets(chunk.toString("utf8").trim())}`));
    killer.stderr.on("data", (chunk: Buffer) => {
      const text = chunk.toString("utf8");
      stderr += text;
      appendRuntimeLog(`[mcp stop fallback stderr] ${redactSecrets(text.trim())}`);
    });
    killer.once("close", onKillerClose);
    killer.once("error", onKillerError);
  });
}

function findWindowsManagedRunClientProcesses(config: TzzTestMcpConfig): Promise<Array<{ pid: number; parentPid: number; name: string; kind: string }>> {
  return new Promise((resolve) => {
    const startedAtMs = runtime.startedAtMs ?? Date.now();
    appendRuntimeLog(`[mcp stop fallback] ${WINDOWS_MANAGED_RUNCLIENT_PROCESS_QUERY} fixed runClient process query`);
    const query = spawn(WINDOWS_MANAGED_RUNCLIENT_PROCESS_QUERY, [
      "-NoProfile",
      "-NonInteractive",
      "-ExecutionPolicy",
      "Bypass",
      "-Command",
      WINDOWS_RUNCLIENT_PROCESS_QUERY_SCRIPT,
      config.repoRoot,
      runtime.worldName ?? "",
      String(startedAtMs)
    ], {
      shell: false,
      windowsHide: true,
      env: process.env
    });
    let stdout = "";
    let stderr = "";
    let settled = false;
    const finish = () => {
      if (settled) {
        return;
      }
      settled = true;
      clearTimeout(timer);
      query.off("close", finish);
      query.off("error", onError);
      if (stderr.trim()) {
        appendRuntimeLog(`[mcp stop fallback query stderr] ${redactSecrets(stderr.trim())}`);
      }
      resolve(parseWindowsRunClientProcessQuery(stdout));
    };
    const onError = (error: Error) => {
      stderr += `${error.name}: ${error.message}\n`;
      finish();
    };
    const timer = setTimeout(() => {
      stderr += `${WINDOWS_MANAGED_RUNCLIENT_PROCESS_QUERY} timed out\n`;
      query.kill("SIGTERM");
      finish();
    }, 5000);
    query.stdout.on("data", (chunk: Buffer) => {
      stdout += chunk.toString("utf8");
    });
    query.stderr.on("data", (chunk: Buffer) => {
      stderr += chunk.toString("utf8");
    });
    query.once("close", finish);
    query.once("error", onError);
  });
}

async function waitForNoWindowsManagedRunClientProcesses(config: TzzTestMcpConfig, timeoutMs: number): Promise<Array<{ pid: number; parentPid: number; name: string; kind: string }>> {
  const deadline = Date.now() + Math.max(1000, timeoutMs);
  let remaining: Array<{ pid: number; parentPid: number; name: string; kind: string }> = [];
  while (Date.now() < deadline) {
    remaining = await findWindowsManagedRunClientProcesses(config);
    if (remaining.length === 0) {
      return [];
    }
    await delay(500);
  }
  return remaining;
}

function parseWindowsRunClientProcessQuery(stdout: string): Array<{ pid: number; parentPid: number; name: string; kind: string }> {
  const text = stdout.trim();
  if (!text) {
    return [];
  }
  try {
    const parsed = JSON.parse(text) as unknown;
    const items = Array.isArray(parsed) ? parsed : [parsed];
    return items.map((item) => {
      const data = item && typeof item === "object" ? item as Record<string, unknown> : {};
      return {
        pid: Number(data.pid ?? 0),
        parentPid: Number(data.parentPid ?? 0),
        name: String(data.name ?? ""),
        kind: String(data.kind ?? "")
      };
    }).filter((item) => Number.isInteger(item.pid) && item.pid > 0);
  } catch (error) {
    appendRuntimeLog(`[mcp stop fallback query parse error] ${errorMessage(error)}`);
    return [];
  }
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
