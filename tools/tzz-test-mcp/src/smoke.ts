import { existsSync } from "node:fs";
import path from "node:path";
import { loadConfig } from "./config.js";
import { handleRequest, toolListForSmoke } from "./server.js";
import type { ToolContext, ToolDefinition } from "./types.js";

async function main(): Promise<void> {
  const tools = toolListForSmoke();
  const required = [
    "health.check",
    "gradle.run",
    "logs.tail",
    "webadmin.open",
    "webadmin.login",
    "webadmin.change_password",
    "webadmin.owner_set_password",
    "webadmin.goto",
    "webadmin.screenshot",
    "webadmin.console_errors",
    "webadmin.click",
    "webadmin.fill",
    "webadmin.text",
    "report.write",
    "repo.status",
    "minecraft.start_client",
    "minecraft.status",
    "minecraft.wait_webadmin",
    "minecraft.stop"
  ];
  for (const name of required) {
    assert(tools.includes(name), `missing tool ${name}`);
  }
  const config = loadConfig();
  assert(existsSync(path.join(config.repoRoot, "gradlew.bat")), "repo root has gradlew.bat");
  const context: ToolContext = {
    config,
    tools: new Map<string, ToolDefinition>(),
    webAdmin: {
      playwright: undefined,
      browser: undefined,
      context: undefined,
      page: undefined,
      baseUrl: undefined,
      consoleErrors: [],
      pageErrors: [],
      failedRequests: [],
      badResponses: [],
      screenshots: []
    }
  };
  const initialize = await handleRequest({ jsonrpc: "2.0", id: 1, method: "initialize", params: {} }, context);
  assert(Boolean(initialize?.result), "initialize returned result");
  console.log(`tzz-test-mcp smoke ok: ${tools.length} tools`);
}

function assert(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

main().catch((error: unknown) => {
  const message = error instanceof Error ? error.stack ?? error.message : String(error);
  console.error(message);
  process.exit(1);
});
