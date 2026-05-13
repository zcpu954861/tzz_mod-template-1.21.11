import { appendFileSync, writeFileSync } from "node:fs";
import type { ToolDefinition, TzzTestMcpConfig, WebAdminState } from "../types.js";
import { fail, ok } from "../results.js";
import { ensureReportPath, redactSecrets, sessionLogPath } from "../safety.js";

export function reportWriteTool(): ToolDefinition {
  return {
    name: "report.write",
    description: "Write a Markdown test report under reports/mcp.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        name: {
          type: "string"
        },
        title: {
          type: "string"
        },
        markdown: {
          type: "string"
        },
        includeScreenshots: {
          type: "boolean"
        }
      },
      required: ["name", "markdown"]
    },
    async handler(args, context) {
      const name = typeof args.name === "string" ? args.name : "";
      const markdown = typeof args.markdown === "string" ? args.markdown : "";
      if (!name || !markdown) {
        return fail("VALIDATION_ERROR", "report.write requires name and markdown.");
      }
      const title = typeof args.title === "string" && args.title.trim() ? args.title.trim() : name;
      const includeScreenshots = args.includeScreenshots !== false;
      const file = ensureReportPath(context.config, name, ".md");
      const body = buildReport(title, markdown, includeScreenshots ? context.webAdmin.screenshots : []);
      writeFileSync(file, body, "utf8");
      return ok("Report written.", {
        path: file,
        screenshotsIncluded: includeScreenshots,
        screenshotCount: includeScreenshots ? context.webAdmin.screenshots.length : 0
      });
    }
  };
}

export function appendSessionAudit(config: TzzTestMcpConfig, toolName: string, okValue: boolean, message: string): void {
  const line = JSON.stringify({
    time: new Date().toISOString(),
    tool: toolName,
    ok: okValue,
    message: redactSecrets(message)
  });
  appendFileSync(sessionLogPath(config), `${line}\n`, "utf8");
}

function buildReport(title: string, markdown: string, screenshots: string[]): string {
  const screenshotSection = screenshots.length === 0
    ? ""
    : [
        "",
        "## Screenshots",
        "",
        ...screenshots.map((screenshot) => `- ${screenshot}`),
        ""
      ].join("\n");
  return [
    `# ${redactSecrets(title)}`,
    "",
    `Generated: ${new Date().toISOString()}`,
    "",
    redactSecrets(markdown).trim(),
    screenshotSection
  ].join("\n");
}
