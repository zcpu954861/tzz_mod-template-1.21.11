import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { validateWorkspaceRoot } from "./safety.js";
import type { TzzTestMcpConfig } from "./types.js";

type PartialConfig = Partial<TzzTestMcpConfig>;

export function loadConfig(): TzzTestMcpConfig {
  const packageRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
  const defaultRepoRoot = path.resolve(packageRoot, "..", "..");
  const fileConfig = loadFileConfig();
  const repoRoot = validateWorkspaceRoot(
    process.env.TZZ_REPO_ROOT
      ?? fileConfig.repoRoot
      ?? defaultRepoRoot
  );

  const reportsDir = process.env.TZZ_MCP_REPORTS_DIR ?? fileConfig.reportsDir ?? "reports/mcp";
  const screenshotsDir = process.env.TZZ_MCP_SCREENSHOTS_DIR ?? fileConfig.screenshotsDir ?? "reports/mcp/screenshots";
  const webAdminUrl = process.env.TZZ_WEBADMIN_URL ?? fileConfig.webAdminUrl ?? "http://127.0.0.1:18080/";
  const allowedHosts = parseHosts(process.env.TZZ_MCP_ALLOWED_HOSTS) ?? fileConfig.allowedHosts ?? ["127.0.0.1", "localhost", "::1"];
  const gradleTimeoutSeconds = parsePositiveInt(
    process.env.TZZ_MCP_GRADLE_TIMEOUT_SECONDS,
    fileConfig.gradleTimeoutSeconds ?? 900
  );
  const playwrightHeadless = parseBoolean(
    process.env.TZZ_MCP_PLAYWRIGHT_HEADLESS,
    fileConfig.playwrightHeadless ?? true
  );

  return {
    repoRoot,
    webAdminUrl,
    reportsDir,
    screenshotsDir,
    allowedHosts,
    gradleTimeoutSeconds,
    playwrightHeadless
  };
}

function loadFileConfig(): PartialConfig {
  const configPath = process.env.TZZ_TEST_MCP_CONFIG;
  if (!configPath) {
    return {};
  }
  const resolved = path.resolve(configPath);
  if (!existsSync(resolved)) {
    throw new Error(`TZZ_TEST_MCP_CONFIG does not exist: ${resolved}`);
  }
  const parsed = JSON.parse(readFileSync(resolved, "utf8")) as PartialConfig;
  return parsed && typeof parsed === "object" ? parsed : {};
}

function parseHosts(value: string | undefined): string[] | undefined {
  if (!value) {
    return undefined;
  }
  return value.split(",").map((entry) => entry.trim()).filter(Boolean);
}

function parsePositiveInt(value: string | undefined, fallback: number): number {
  if (!value) {
    return fallback;
  }
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function parseBoolean(value: string | undefined, fallback: boolean): boolean {
  if (!value) {
    return fallback;
  }
  return /^(1|true|yes|on)$/i.test(value);
}
