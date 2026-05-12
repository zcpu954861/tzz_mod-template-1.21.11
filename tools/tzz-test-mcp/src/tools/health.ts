import os from "node:os";
import type { ToolDefinition } from "../types.js";
import { ok } from "../results.js";

export function healthTool(): ToolDefinition {
  return {
    name: "health.check",
    description: "Return local MCP server health, version, repo root, platform, Node version, and available tools.",
    inputSchema: {
      type: "object",
      additionalProperties: false
    },
    readOnlyHint: true,
    async handler(_args, context) {
      return ok("Local Test MCP server is ready.", {
        version: "0.1.0",
        repoRoot: context.config.repoRoot,
        platform: `${os.platform()} ${os.release()}`,
        nodeVersion: process.version,
        webAdminUrl: context.config.webAdminUrl,
        reportsDir: context.config.reportsDir,
        screenshotsDir: context.config.screenshotsDir,
        allowedHosts: context.config.allowedHosts,
        tools: [...context.tools.keys()].sort()
      });
    }
  };
}
