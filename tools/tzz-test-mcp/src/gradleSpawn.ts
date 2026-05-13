import { existsSync } from "node:fs";
import path from "node:path";

export type GradleSpawnCommand = {
  command: string;
  args: string[];
  wrapperPath: string;
};

export function resolveGradleExecutable(repoRoot: string): string {
  const wrapperName = process.platform === "win32" ? "gradlew.bat" : "gradlew";
  const wrapperPath = path.join(repoRoot, wrapperName);
  if (!existsSync(wrapperPath)) {
    throw new Error(`未找到 ${wrapperName}：${wrapperPath}`);
  }
  return wrapperPath;
}

export function buildGradleSpawnCommand(repoRoot: string, gradleArgs: readonly string[]): GradleSpawnCommand {
  const wrapperPath = resolveGradleExecutable(repoRoot);
  if (process.platform === "win32") {
    return {
      command: "cmd.exe",
      args: ["/d", "/c", "call", wrapperPath, ...gradleArgs],
      wrapperPath
    };
  }
  return {
    command: wrapperPath,
    args: [...gradleArgs],
    wrapperPath
  };
}
