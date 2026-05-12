import type { ToolDefinition } from "../types.js";
import { gradleRunTool } from "./gradle.js";
import { healthTool } from "./health.js";
import { logsTailTool } from "./logs.js";
import { minecraftTools } from "./minecraft.js";
import { repoStatusTool } from "./repo.js";
import { reportWriteTool } from "./report.js";
import { webAdminTools } from "./webadmin.js";

export function createTools(): ToolDefinition[] {
  return [
    healthTool(),
    gradleRunTool(),
    logsTailTool(),
    repoStatusTool(),
    reportWriteTool(),
    ...minecraftTools(),
    ...webAdminTools()
  ];
}
