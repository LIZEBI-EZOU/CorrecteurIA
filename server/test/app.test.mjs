import test from "node:test";
import assert from "node:assert/strict";
import { app } from "../src/app.mjs";

const server = app.listen(0, "127.0.0.1");
const port = server.address().port;
const base = `http://127.0.0.1:${port}`;

test("health endpoint", async () => {
  const response = await fetch(`${base}/health`);
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.status, "ok");
  assert.equal(body.service, "CorrecteurIA Server");
});

test("unknown route returns JSON 404", async () => {
  const response = await fetch(`${base}/unknown`);
  assert.equal(response.status, 404);
  const body = await response.json();
  assert.equal(body.error, "NOT_FOUND");
});

test("rewrite validates text before using OpenAI", async () => {
  const response = await fetch(`${base}/v1/rewrite`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text: "" })
  });
  assert.equal(response.status, 400);
  const body = await response.json();
  assert.equal(body.error, "INVALID_TEXT");
});

test.after(() => server.close());
