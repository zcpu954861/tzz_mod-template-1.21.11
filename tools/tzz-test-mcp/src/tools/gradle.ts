import { createWriteStream } from "node:fs";
import path from "node:path";
import { spawn } from "node:child_process";
import type { JsonObject, ToolDefinition } from "../types.js";
import { fail, ok } from "../results.js";
import { ensureDirectory, safeName, tailText, timestamp } from "../safety.js";
import { buildGradleSpawnCommand } from "../gradleSpawn.js";

const PRESETS: Record<string, string[]> = {
  clean_build: ["clean", "build"],
  "clean build": ["clean", "build"],
  build: ["build"],
  test: ["test"],
  local_test_mcp_guard: ["localTestMcpGuardTest"],
  localTestMcpGuardTest: ["localTestMcpGuardTest"],
  stabilization_guard: ["stabilizationGuardTest"],
  stabilizationGuardTest: ["stabilizationGuardTest"],
  stabilization_guard_rerun: ["stabilizationGuardTest", "--rerun-tasks"],
  "stabilizationGuardTest --rerun-tasks": ["stabilizationGuardTest", "--rerun-tasks"]
};

export function gradleRunTool(): ToolDefinition {
  return {
    name: "gradle.run",
    description: "Run a whitelisted Gradle preset. This is not an arbitrary shell.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        commandPreset: {
          type: "string",
          enum: ["clean_build", "clean build", "build", "test", "local_test_mcp_guard", "localTestMcpGuardTest", "stabilization_guard", "stabilizationGuardTest", "stabilization_guard_rerun", "stabilizationGuardTest --rerun-tasks"]
        },
        taskName: {
          type: "string",
          enum: ["clean_build", "clean build", "build", "test", "local_test_mcp_guard", "localTestMcpGuardTest", "stabilization_guard", "stabilizationGuardTest", "stabilization_guard_rerun", "stabilizationGuardTest --rerun-tasks"]
        },
        timeoutSeconds: {
          type: "number",
          minimum: 1,
          maximum: 3600
        }
      }
    },
    async handler(args, context) {
      const requested = stringArg(args, "commandPreset") || stringArg(args, "taskName") || "";
      const gradleArgs = PRESETS[requested];
      if (!gradleArgs) {
        return fail("VALIDATION_ERROR", "gradle.run requires a whitelisted commandPreset or taskName.", {
          allowedPresets: Object.keys(PRESETS)
        });
      }
      const timeoutSeconds = numberArg(args, "timeoutSeconds") ?? context.config.gradleTimeoutSeconds;
      const result = await runGradle(context.config.repoRoot, gradleArgs, requested, timeoutSeconds, context.config.reportsDir);
      const data: JsonObject = {
        preset: requested,
        exitCode: result.exitCode,
        signal: result.signal ?? "",
        durationMs: result.durationMs,
        stdoutTail: result.stdoutTail,
        stderrTail: result.stderrTail,
        outputTruncated: result.outputTruncated,
        logFile: result.logFile
      };
      if (result.exitCode === 0) {
        return ok(`Gradle preset passed: ${requested}`, data);
      }
      return fail(result.timedOut ? "TIMEOUT" : "COMMAND_FAILED", `Gradle preset failed: ${requested}`, data);
    }
  };
}

type CommandResult = {
  exitCode: number | null;
  signal?: string;
  durationMs: number;
  stdoutTail: string;
  stderrTail: string;
  outputTruncated: boolean;
  logFile: string;
  timedOut: boolean;
};

function runGradle(repoRoot: string, gradleArgs: string[], preset: string, timeoutSeconds: number, reportsDir: string): Promise<CommandResult> {
  return new Promise((resolve) => {
    const start = Date.now();
    const logDir = ensureDirectory(path.resolve(repoRoot, reportsDir, "gradle"));
    const logFile = path.join(logDir, `${timestamp()}-${safeName(preset)}.log`);
    const stream = createWriteStream(logFile, { encoding: "utf8" });
    let spawnCommand;
    try {
      spawnCommand = buildGradleSpawnCommand(repoRoot, gradleArgs);
    } catch (error) {
      const message = `${error instanceof Error ? error.message : String(error)}\n`;
      stream.write(message);
      stream.end();
      const stderrTail = tailText(message, 32768);
      resolve({
        exitCode: 1,
        signal: "",
        durationMs: Date.now() - start,
        stdoutTail: "",
        stderrTail: stderrTail.text,
        outputTruncated: stderrTail.truncated,
        logFile,
        timedOut: false
      });
      return;
    }
    const child = spawn(spawnCommand.command, spawnCommand.args, {
      cwd: repoRoot,
      shell: false,
      windowsHide: true,
      env: process.env
    });
    let stdout = "";
    let stderr = "";
    let timedOut = false;
    const timeout = setTimeout(() => {
      timedOut = true;
      child.kill("SIGTERM");
    }, Math.max(1, timeoutSeconds) * 1000);

    child.stdout.on("data", (chunk: Buffer) => {
      const text = chunk.toString("utf8");
      stdout += text;
      stream.write(text);
    });
    child.stderr.on("data", (chunk: Buffer) => {
      const text = chunk.toString("utf8");
      stderr += text;
      stream.write(text);
    });
    child.on("error", (error) => {
      stderr += `${error.name}: ${error.message}\n`;
    });
    child.on("close", (exitCode, signal) => {
      clearTimeout(timeout);
      stream.end();
      const stdoutTail = tailText(stdout, 32768);
      const stderrTail = tailText(stderr, 32768);
      resolve({
        exitCode,
        signal: signal ?? "",
        durationMs: Date.now() - start,
        stdoutTail: stdoutTail.text,
        stderrTail: stderrTail.text,
        outputTruncated: stdoutTail.truncated || stderrTail.truncated,
        logFile,
        timedOut
      });
    });
  });
}

function stringArg(args: JsonObject, key: string): string | undefined {
  const value = args[key];
  return typeof value === "string" ? value : undefined;
}

function numberArg(args: JsonObject, key: string): number | undefined {
  const value = args[key];
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}
