import { spawn } from "node:child_process";
import type { ToolDefinition } from "../types.js";
import { fail, ok } from "../results.js";
import { tailText } from "../safety.js";

const READ_ONLY_COMMANDS = {
  status: ["status", "--short", "--branch"],
  log: ["log", "--oneline", "--decorate", "-10"],
  diff_name_status: ["diff", "--name-status"],
  diff_stat: ["diff", "--stat"]
} as const;

export function repoStatusTool(): ToolDefinition {
  return {
    name: "repo.status",
    description: "Return read-only git status, recent commits, and diff summary.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        includeDiff: {
          type: "boolean"
        }
      }
    },
    readOnlyHint: true,
    async handler(args, context) {
      const includeDiff = args.includeDiff !== false;
      const status = await runGit(context.config.repoRoot, READ_ONLY_COMMANDS.status);
      const log = await runGit(context.config.repoRoot, READ_ONLY_COMMANDS.log);
      const diffNameStatus = includeDiff ? await runGit(context.config.repoRoot, READ_ONLY_COMMANDS.diff_name_status) : undefined;
      const diffStat = includeDiff ? await runGit(context.config.repoRoot, READ_ONLY_COMMANDS.diff_stat) : undefined;
      const failed = [status, log, diffNameStatus, diffStat].filter((entry): entry is GitResult => Boolean(entry && entry.exitCode !== 0));
      const data = {
        branchStatus: status.stdoutTail,
        latestCommits: log.stdoutTail,
        diffNameStatus: diffNameStatus?.stdoutTail ?? "",
        diffStat: diffStat?.stdoutTail ?? ""
      };
      if (failed.length > 0) {
        return fail("COMMAND_FAILED", "A read-only git command failed.", data);
      }
      return ok("Repository status read.", data);
    }
  };
}

type GitResult = {
  exitCode: number | null;
  stdoutTail: string;
  stderrTail: string;
};

function runGit(repoRoot: string, args: readonly string[]): Promise<GitResult> {
  return new Promise((resolve) => {
    const child = spawn("git", [...args], {
      cwd: repoRoot,
      shell: false,
      windowsHide: true
    });
    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (chunk: Buffer) => {
      stdout += chunk.toString("utf8");
    });
    child.stderr.on("data", (chunk: Buffer) => {
      stderr += chunk.toString("utf8");
    });
    child.on("error", (error) => {
      stderr += `${error.name}: ${error.message}\n`;
    });
    child.on("close", (exitCode) => {
      resolve({
        exitCode,
        stdoutTail: tailText(stdout, 16384).text,
        stderrTail: tailText(stderr, 16384).text
      });
    });
  });
}
