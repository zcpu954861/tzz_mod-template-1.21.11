import { existsSync, mkdirSync, realpathSync, statSync } from "node:fs";
import path from "node:path";
import type { JsonObject, TzzTestMcpConfig } from "./types.js";

export function ensureDirectory(dir: string): string {
  mkdirSync(dir, { recursive: true });
  return dir;
}

export function safeName(input: unknown, fallback = "artifact"): string {
  const raw = String(input ?? "").trim() || fallback;
  const normalized = raw
    .replace(/[<>:"/\\|?*\u0000-\u001f]/g, "-")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 80);
  return normalized || fallback;
}

export function timestamp(): string {
  return new Date().toISOString().replace(/[:.]/g, "-");
}

export function redactSecrets(input: string): string {
  return input
    .replace(/(authorization\s*[:=]\s*)([^\s,;]+)/gi, "$1[redacted]")
    .replace(/(cookie\s*[:=]\s*)([^\n]+)/gi, "$1[redacted]")
    .replace(/(set-cookie\s*[:=]\s*)([^\n]+)/gi, "$1[redacted]")
    .replace(/(x-tzz-webadmin-csrf\s*[:=]\s*)([^\s,;]+)/gi, "$1[redacted]")
    .replace(/(x-tzz-testbridge-token\s*[:=]\s*)([^\s,;]+)/gi, "$1[redacted]")
    .replace(/(TZZ_TESTBRIDGE_TOKEN\s*[:=]\s*)([^\s,;]+)/gi, "$1[redacted]")
    .replace(/(testBridgeToken\s*[:=]\s*)([^\s,;]+)/gi, "$1[redacted]")
    .replace(/(tzz_webadmin_session\s*[:=]\s*)([^\s,;]+)/gi, "$1[redacted]")
    .replace(/((?:password|passwd|pwd|token|secret|csrf|sessionId|session)\s*[:=]\s*)([^\s,;]+)/gi, "$1[redacted]");
}

export function tailText(input: string, maxChars: number): { text: string; truncated: boolean } {
  const redacted = redactSecrets(input);
  if (redacted.length <= maxChars) {
    return { text: redacted, truncated: false };
  }
  return {
    text: redacted.slice(redacted.length - maxChars),
    truncated: true
  };
}

export function assertInside(baseDir: string, candidate: string): string {
  const resolvedBase = path.resolve(baseDir);
  const resolvedCandidate = path.resolve(candidate);
  const normalizedBase = resolvedBase.endsWith(path.sep) ? resolvedBase : resolvedBase + path.sep;
  if (resolvedCandidate !== resolvedBase && !resolvedCandidate.startsWith(normalizedBase)) {
    throw new Error(`Path is outside allowed directory: ${resolvedCandidate}`);
  }
  return resolvedCandidate;
}

export function resolveInside(baseDir: string, relativePath: string): string {
  if (!relativePath || path.isAbsolute(relativePath) || relativePath.includes("\0")) {
    throw new Error("Path must be a relative path inside the allowed directory");
  }
  return assertInside(baseDir, path.resolve(baseDir, relativePath));
}

export function ensureReportPath(config: TzzTestMcpConfig, name: unknown, extension: ".md" | ".png" | ".log"): string {
  const base = extension === ".png"
    ? resolveRepoOutputDir(config.repoRoot, config.screenshotsDir)
    : resolveRepoOutputDir(config.repoRoot, config.reportsDir);
  ensureDirectory(base);
  const file = `${timestamp()}-${safeName(name)}${extension}`;
  return assertInside(base, path.join(base, file));
}

export function sessionLogPath(config: TzzTestMcpConfig): string {
  const dir = ensureDirectory(resolveRepoOutputDir(config.repoRoot, config.reportsDir));
  return assertInside(dir, path.join(dir, "session.log"));
}

export function ensureAllowedUrl(rawUrl: string, config: TzzTestMcpConfig): URL {
  const url = new URL(rawUrl);
  if (url.protocol !== "http:" && url.protocol !== "https:") {
    throw new Error(`URL protocol is not allowed: ${url.protocol}`);
  }
  const host = normalizeHost(url.hostname);
  if (!isLoopbackHost(host)) {
    throw new Error(`URL host must be localhost or loopback: ${url.hostname}`);
  }
  const allowed = config.allowedHosts.map(normalizeHost);
  if (!allowed.includes(host)) {
    throw new Error(`URL host is not allowed: ${url.hostname}`);
  }
  return url;
}

export function ensureHashRoute(input: unknown): string {
  const route = String(input ?? "").trim();
  if (!route.startsWith("#/") || route.includes("\0") || route.toLowerCase().startsWith("javascript:")) {
    throw new Error("hashRoute must start with #/ and stay inside the WebAdmin app");
  }
  return route;
}

export function resolveAllowedLog(config: TzzTestMcpConfig, args: JsonObject): string {
  const repo = path.resolve(config.repoRoot);
  const logName = typeof args.logName === "string" ? args.logName : "";
  const named: Record<string, string> = {
    run_latest: path.join(repo, "run", "logs", "latest.log"),
    mcp_session: sessionLogPath(config),
    gradle_test_report: path.join(repo, "build", "reports", "tests", "test", "index.html")
  };
  if (logName) {
    const selected = named[logName];
    if (!selected) {
      throw new Error(`Unknown logName: ${logName}`);
    }
    return selected;
  }

  const allowedPath = typeof args.allowedPath === "string" ? args.allowedPath.trim() : "";
  if (!allowedPath) {
    throw new Error("logs.tail requires logName or allowedPath");
  }
  if (path.isAbsolute(allowedPath) || allowedPath.includes("\0")) {
    throw new Error("allowedPath must be relative to repo root");
  }
  const candidate = path.resolve(repo, allowedPath);
  const allowedDirs = [
    path.join(repo, "run", "logs"),
    path.join(repo, "build", "reports"),
    resolveRepoOutputDir(repo, config.reportsDir)
  ];
  if (!allowedDirs.some((allowedDir) => isInsideOrEqual(allowedDir, candidate))) {
    throw new Error("allowedPath is not in an approved log/report directory");
  }
  return candidate;
}

export function fileMeta(file: string): JsonObject {
  if (!existsSync(file)) {
    return { exists: false, path: file };
  }
  const stat = statSync(file);
  return {
    exists: true,
    path: file,
    sizeBytes: stat.size,
    modifiedTime: stat.mtime.toISOString()
  };
}

export function validateWorkspaceRoot(repoRoot: string): string {
  const resolved = realpathSync(path.resolve(repoRoot));
  const required = ["gradlew.bat", "build.gradle", ".git"];
  for (const entry of required) {
    if (!existsSync(path.join(resolved, entry))) {
      throw new Error(`Invalid repo root; missing ${entry}`);
    }
  }
  return resolved;
}

export function resolveRepoOutputDir(repoRoot: string, relativeDir: string): string {
  if (!relativeDir || path.isAbsolute(relativeDir) || relativeDir.includes("\0")) {
    throw new Error("Output directory must be a relative repo path");
  }
  return assertInside(repoRoot, path.resolve(repoRoot, relativeDir));
}

function normalizeHost(host: string): string {
  return host.replace(/^\[|\]$/g, "").toLowerCase();
}

function isLoopbackHost(host: string): boolean {
  return host === "localhost" || host === "127.0.0.1" || host === "::1" || host === "0:0:0:0:0:0:0:1";
}

function isInsideOrEqual(baseDir: string, candidate: string): boolean {
  const resolvedBase = path.resolve(baseDir);
  const resolvedCandidate = path.resolve(candidate);
  const normalizedBase = resolvedBase.endsWith(path.sep) ? resolvedBase : resolvedBase + path.sep;
  return resolvedCandidate === resolvedBase || resolvedCandidate.startsWith(normalizedBase);
}
