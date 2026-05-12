import type { Json, JsonObject, ToolCallResult } from "./types.js";
import { redactSecrets } from "./safety.js";

export type ToolErrorCode =
  | "OK"
  | "VALIDATION_ERROR"
  | "COMMAND_FAILED"
  | "AUTH_FAILED"
  | "AUTH_REQUIRED"
  | "BUTTON_DISABLED"
  | "BUTTON_CLICK_FAILED"
  | "CONFIG_ERROR"
  | "LOGIN_FAILED"
  | "TIMEOUT"
  | "TESTBRIDGE_AUTH_FAILED"
  | "TESTBRIDGE_DISABLED"
  | "TESTBRIDGE_NOT_READY"
  | "GUI_NOT_OPEN"
  | "UNSUPPORTED_GUI"
  | "COMMAND_DENIED"
  | "WORKSPACE_INVALID"
  | "TOOL_UNAVAILABLE"
  | "NOT_FOUND"
  | "NOT_AUTHENTICATED"
  | "SELECTOR_NOT_FOUND"
  | "SUBMIT_NOT_TRIGGERED"
  | "SECURITY_DENIED"
  | "BROWSER_ERROR"
  | "IO_ERROR";

export function ok(message: string, data: JsonObject = {}): ToolCallResult {
  const structuredContent = sanitizeJsonObject({
    ok: true,
    code: "OK",
    message,
    ...data
  });
  return {
    content: [{ type: "text", text: message }],
    structuredContent
  };
}

export function fail(code: Exclude<ToolErrorCode, "OK">, message: string, data: JsonObject = {}): ToolCallResult {
  const structuredContent = sanitizeJsonObject({
    ok: false,
    code,
    message,
    ...data
  });
  return {
    isError: true,
    content: [{ type: "text", text: `${code}: ${message}` }],
    structuredContent
  };
}

export function sanitizeJsonObject(value: JsonObject): JsonObject {
  return sanitizeJson(value) as JsonObject;
}

function sanitizeJson(value: Json): Json {
  if (value === null || typeof value === "boolean" || typeof value === "number") {
    return value;
  }
  if (typeof value === "string") {
    return redactSecrets(value);
  }
  if (Array.isArray(value)) {
    return value.map((entry) => sanitizeJson(entry));
  }
  const sanitized: JsonObject = {};
  for (const [key, entry] of Object.entries(value)) {
    if (isSecretKey(key)) {
      sanitized[key] = "[redacted]";
    } else {
      sanitized[key] = sanitizeJson(entry);
    }
  }
  return sanitized;
}

function isSecretKey(key: string): boolean {
  if (/configured$/i.test(key)) {
    return false;
  }
  return /password|passwd|pwd|token|secret|cookie|csrf|authorization|session/i.test(key);
}
