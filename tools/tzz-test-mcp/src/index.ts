#!/usr/bin/env node
import { runServer } from "./server.js";

runServer().catch((error: unknown) => {
  const message = error instanceof Error ? error.stack ?? error.message : String(error);
  process.stderr.write(`tzz-test-mcp failed: ${message}\n`);
  process.exitCode = 1;
});
