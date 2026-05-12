export type Json =
  | null
  | boolean
  | number
  | string
  | Json[]
  | { [key: string]: Json };

export type JsonObject = { [key: string]: Json };

export type ToolContent = {
  type: "text";
  text: string;
};

export type ToolCallResult = {
  content: ToolContent[];
  structuredContent?: JsonObject;
  isError?: boolean;
};

export type ToolHandler = (args: JsonObject, context: ToolContext) => Promise<ToolCallResult>;

export type ToolDefinition = {
  name: string;
  description: string;
  inputSchema: JsonObject;
  handler: ToolHandler;
  readOnlyHint?: boolean;
};

export type TzzTestMcpConfig = {
  repoRoot: string;
  webAdminUrl: string;
  reportsDir: string;
  screenshotsDir: string;
  allowedHosts: string[];
  gradleTimeoutSeconds: number;
  playwrightHeadless: boolean;
};

export type WebAdminState = {
  playwright: unknown | undefined;
  browser: unknown | undefined;
  context: unknown | undefined;
  page: unknown | undefined;
  baseUrl: string | undefined;
  consoleErrors: string[];
  pageErrors: string[];
  failedRequests: JsonObject[];
  badResponses: JsonObject[];
  screenshots: string[];
};

export type ToolContext = {
  config: TzzTestMcpConfig;
  tools: Map<string, ToolDefinition>;
  webAdmin: WebAdminState;
};
