import readline from "node:readline";
import { loadConfig } from "./config.js";
import { fail } from "./results.js";
import { createTools } from "./tools/index.js";
import { appendSessionAudit } from "./tools/report.js";
import type { Json, JsonObject, ToolCallResult, ToolContext, ToolDefinition } from "./types.js";

type JsonRpcRequest = {
  jsonrpc?: string;
  id?: string | number | null;
  method?: string;
  params?: JsonObject;
};

type JsonRpcResponse = {
  jsonrpc: "2.0";
  id: string | number | null;
  result?: Json;
  error?: {
    code: number;
    message: string;
    data?: Json;
  };
};

export async function runServer(): Promise<void> {
  const config = loadConfig();
  const tools = new Map<string, ToolDefinition>();
  for (const tool of createTools()) {
    tools.set(tool.name, tool);
  }
  const context: ToolContext = {
    config,
    tools,
    webAdmin: {
      playwright: undefined,
      browser: undefined,
      context: undefined,
      page: undefined,
      baseUrl: undefined,
      viewportProfile: undefined,
      consoleErrors: [],
      pageErrors: [],
      failedRequests: [],
      badResponses: [],
      screenshots: []
    }
  };

  const rl = readline.createInterface({
    input: process.stdin,
    crlfDelay: Infinity
  });

  for await (const line of rl) {
    if (!line.trim()) {
      continue;
    }
    let request: JsonRpcRequest;
    try {
      request = JSON.parse(line) as JsonRpcRequest;
    } catch (error) {
      writeError(null, -32700, "Parse error", String(error));
      continue;
    }
    try {
      const response = await handleRequest(request, context);
      if (response) {
        writeResponse(response);
      }
    } catch (error) {
      const id = request.id ?? null;
      writeError(id, -32603, error instanceof Error ? error.message : String(error));
    }
  }
}

export async function handleRequest(request: JsonRpcRequest, context: ToolContext): Promise<JsonRpcResponse | undefined> {
  const id = request.id ?? null;
  const isNotification = request.id === undefined || request.id === null;
  const method = request.method ?? "";
  if (!method) {
    return errorResponse(id, -32600, "Invalid request");
  }

  if (method === "notifications/initialized") {
    return undefined;
  }
  if (method === "initialize") {
    return resultResponse(id, {
      protocolVersion: "2024-11-05",
      capabilities: {
        tools: {}
      },
      serverInfo: {
        name: "tzz-local-test-mcp",
        version: "0.1.0"
      }
    });
  }
  if (method === "ping") {
    return isNotification ? undefined : resultResponse(id, {});
  }
  if (method === "tools/list") {
    return resultResponse(id, {
      tools: [...context.tools.values()].map((tool) => ({
        name: tool.name,
        description: tool.description,
        inputSchema: tool.inputSchema,
        annotations: {
          readOnlyHint: tool.readOnlyHint === true
        }
      }))
    });
  }
  if (method === "tools/call") {
    const toolName = typeof request.params?.name === "string" ? request.params.name : "";
    const args = isJsonObject(request.params?.arguments) ? request.params.arguments : {};
    const tool = context.tools.get(toolName);
    if (!tool) {
      return resultResponse(id, fail("VALIDATION_ERROR", `Unknown tool: ${toolName}`) as unknown as Json);
    }
    const result = await tool.handler(args, context).catch((error: unknown) => fail("IO_ERROR", error instanceof Error ? error.message : String(error)));
    const ok = result.isError !== true;
    const message = result.content.map((entry) => entry.text).join("\n");
    appendSessionAudit(context.config, toolName, ok, message);
    return resultResponse(id, result as unknown as Json);
  }

  return errorResponse(id, -32601, `Method not found: ${method}`);
}

function resultResponse(id: string | number | null, result: Json): JsonRpcResponse {
  return {
    jsonrpc: "2.0",
    id,
    result
  };
}

function errorResponse(id: string | number | null, code: number, message: string, data?: Json): JsonRpcResponse {
  return {
    jsonrpc: "2.0",
    id,
    error: {
      code,
      message,
      ...(data === undefined ? {} : { data })
    }
  };
}

function writeResponse(response: JsonRpcResponse): void {
  process.stdout.write(`${JSON.stringify(response)}\n`);
}

function writeError(id: string | number | null, code: number, message: string, data?: Json): void {
  writeResponse(errorResponse(id, code, message, data));
}

function isJsonObject(value: unknown): value is JsonObject {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

export function toolListForSmoke(): string[] {
  return createTools().map((tool) => tool.name).sort();
}

export type { ToolCallResult };
