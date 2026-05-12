import { existsSync, readFileSync } from "node:fs";
import type { ToolDefinition } from "../types.js";
import { fail, ok } from "../results.js";
import { fileMeta, resolveAllowedLog, tailText } from "../safety.js";

export function logsTailTool(): ToolDefinition {
  return {
    name: "logs.tail",
    description: "Read the tail of an allowlisted log or report file.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        logName: {
          type: "string",
          enum: ["run_latest", "mcp_session", "gradle_test_report"]
        },
        allowedPath: {
          type: "string"
        },
        lines: {
          type: "number",
          minimum: 1,
          maximum: 500
        }
      }
    },
    readOnlyHint: true,
    async handler(args, context) {
      let file: string;
      try {
        file = resolveAllowedLog(context.config, args);
      } catch (error) {
        return fail("SECURITY_DENIED", error instanceof Error ? error.message : "Log path is not allowed.");
      }
      const meta = fileMeta(file);
      if (!existsSync(file)) {
        return ok("Log file does not exist.", meta);
      }
      const lines = typeof args.lines === "number" ? Math.max(1, Math.min(500, Math.floor(args.lines))) : 80;
      const text = readFileSync(file, "utf8");
      const selected = text.split(/\r?\n/).slice(-lines).join("\n");
      const tail = tailText(selected, 32768);
      return ok("Log tail read.", {
        ...meta,
        lines,
        tail: tail.text,
        outputTruncated: tail.truncated
      });
    }
  };
}
