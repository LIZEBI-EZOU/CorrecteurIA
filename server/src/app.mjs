import express from "express";
import helmet from "helmet";
import cors from "cors";
import rateLimit from "express-rate-limit";
import crypto from "node:crypto";

const app = express();
app.disable("x-powered-by");
app.set("trust proxy", 1);

const MAX_TEXT_CHARS = Number(process.env.MAX_TEXT_CHARS || 12000);
const OPENAI_MODEL = process.env.OPENAI_MODEL || "gpt-5.6-luna";
const LANGUAGE_TOOL_URL = process.env.LANGUAGETOOL_URL || "https://api.languagetool.org/v2/check";

const metrics = {
  startedAt: Date.now(),
  requests: 0,
  rewrites: 0,
  corrections: 0,
  failures: 0,
  openAiFailures: 0,
  languageToolFailures: 0
};

app.use(helmet());
app.use(cors({
  origin(origin, callback) {
    const allowed = (process.env.ALLOWED_ORIGINS || "").split(",").map(v => v.trim()).filter(Boolean);
    if (!origin || allowed.length === 0 || allowed.includes(origin)) return callback(null, true);
    return callback(new Error("Origin non autorisée"));
  }
}));
app.use(express.json({ limit: "32kb" }));

app.use((req, res, next) => {
  req.requestId = crypto.randomUUID();
  metrics.requests += 1;
  res.setHeader("X-Request-ID", req.requestId);
  const started = Date.now();
  res.on("finish", () => {
    console.log(JSON.stringify({
      event: "request",
      requestId: req.requestId,
      method: req.method,
      path: req.path,
      status: res.statusCode,
      durationMs: Date.now() - started
    }));
  });
  next();
});

const rewriteLimiter = rateLimit({
  windowMs: 60_000,
  limit: Number(process.env.REWRITE_RATE_LIMIT || 20),
  standardHeaders: "draft-8",
  legacyHeaders: false,
  message: { error: "Trop de requêtes de reformulation. Réessayez dans une minute." }
});

const correctLimiter = rateLimit({
  windowMs: 60_000,
  limit: Number(process.env.CORRECT_RATE_LIMIT || 40),
  standardHeaders: "draft-8",
  legacyHeaders: false,
  message: { error: "Trop de requêtes de correction. Réessayez dans une minute." }
});

function validText(text) {
  return typeof text === "string" && text.trim().length > 0 && text.length <= MAX_TEXT_CHARS;
}

function errorResponse(res, status, code, message, requestId) {
  return res.status(status).json({ error: code, message, requestId });
}

async function fetchWithTimeout(url, options = {}, timeoutMs = 35_000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

function extractOpenAiText(json) {
  if (typeof json?.output_text === "string" && json.output_text.trim()) {
    return json.output_text.trim();
  }
  for (const item of json?.output || []) {
    for (const part of item?.content || []) {
      if (typeof part?.text === "string" && part.text.trim()) return part.text.trim();
    }
  }
  return null;
}

function rewriteInstructions(style, intensity) {
  return [
    "Tu es le moteur de reformulation français de CorrecteurIA.",
    "Reformule uniquement le texte fourni.",
    "Conserve strictement le sens, les faits, les nombres, les noms propres, les citations et les informations importantes.",
    "N'ajoute aucune information et ne supprime aucune information.",
    "Retourne uniquement le texte final, sans commentaire, sans préambule et sans mention d'IA.",
    "Langue: français.",
    `Style demandé: ${style}.`,
    `Intensité demandée: ${intensity}.`
  ].join("\n");
}

app.get("/", (_req, res) => {
  res.json({
    status: "ok",
    service: "CorrecteurIA Server",
    message: "API CorrecteurIA opérationnelle",
    endpoints: ["/health", "/ready", "/v1/correct", "/v1/rewrite"]
  });
});

app.get("/health", (_req, res) => {
  res.json({
    status: "ok",
    service: "CorrecteurIA Server",
    version: "1.0.0",
    model: OPENAI_MODEL,
    uptimeSeconds: Math.floor((Date.now() - metrics.startedAt) / 1000),
    time: new Date().toISOString()
  });
});

app.get("/ready", (_req, res) => {
  const configured = Boolean(process.env.OPENAI_API_KEY);
  if (!configured) return res.status(503).json({ status: "not_ready", reason: "OPENAI_API_KEY manquante" });
  res.json({ status: "ready", model: OPENAI_MODEL });
});

app.get("/metrics", (req, res) => {
  const token = process.env.METRICS_TOKEN;
  if (!token || req.get("X-Metrics-Token") !== token) return res.status(404).end();
  res.json({
    ...metrics,
    uptimeSeconds: Math.floor((Date.now() - metrics.startedAt) / 1000)
  });
});

app.post("/v1/rewrite", rewriteLimiter, async (req, res) => {
  const { text, style = "Standard", intensity = "Équilibré" } = req.body || {};
  if (!validText(text)) {
    return errorResponse(res, 400, "INVALID_TEXT", `Le texte doit contenir entre 1 et ${MAX_TEXT_CHARS} caractères.`, req.requestId);
  }
  if (!process.env.OPENAI_API_KEY) {
    return errorResponse(res, 503, "AI_NOT_CONFIGURED", "Le serveur IA n'est pas encore configuré.", req.requestId);
  }

  metrics.rewrites += 1;
  try {
    const response = await fetchWithTimeout("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: OPENAI_MODEL,
        instructions: rewriteInstructions(String(style).slice(0, 80), String(intensity).slice(0, 40)),
        input: text,
        max_output_tokens: 1600,
        store: false
      })
    });

    const json = await response.json().catch(() => ({}));
    if (!response.ok) {
      metrics.openAiFailures += 1;
      console.error(JSON.stringify({
        event: "openai_error",
        requestId: req.requestId,
        status: response.status,
        error: json?.error?.code || json?.error?.type || "unknown"
      }));
      const upstreamCode = json?.error?.code || json?.error?.type || "unknown";
      if (response.status === 429 && upstreamCode === "credit_balance_exhausted") {
        return errorResponse(res, 503, "AI_CREDIT_REQUIRED", "Le service IA distant nécessite un crédit API disponible.", req.requestId);
      }
      return errorResponse(res, 502, "AI_UPSTREAM_ERROR", "Le service IA distant a refusé ou interrompu la requête.", req.requestId);
    }

    const output = extractOpenAiText(json);
    if (!output) {
      metrics.openAiFailures += 1;
      return errorResponse(res, 502, "AI_EMPTY", "Le service IA n'a renvoyé aucun texte.", req.requestId);
    }

    return res.json({
      output,
      mode: "IA en ligne",
      model: OPENAI_MODEL,
      requestId: req.requestId
    });
  } catch (error) {
    metrics.openAiFailures += 1;
    console.error(JSON.stringify({ event: "openai_exception", requestId: req.requestId, error: String(error) }));
    return errorResponse(res, 504, "AI_TIMEOUT", "Le service IA est temporairement indisponible.", req.requestId);
  }
});

app.post("/v1/correct", correctLimiter, async (req, res) => {
  const { text } = req.body || {};
  if (!validText(text)) {
    return errorResponse(res, 400, "INVALID_TEXT", `Le texte doit contenir entre 1 et ${MAX_TEXT_CHARS} caractères.`, req.requestId);
  }

  metrics.corrections += 1;
  try {
    const form = new URLSearchParams({
      text,
      language: "fr",
      enabledOnly: "false"
    });
    const response = await fetchWithTimeout(LANGUAGE_TOOL_URL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: form.toString()
    }, 20_000);

    const json = await response.json().catch(() => ({}));
    if (!response.ok) {
      metrics.languageToolFailures += 1;
      return errorResponse(res, 502, "CORRECTOR_UPSTREAM_ERROR", "Le service de correction en ligne est indisponible.", req.requestId);
    }

    let result = text;
    const matches = Array.isArray(json.matches) ? json.matches : [];
    const replacements = [];
    for (const match of matches) {
      const replacement = match?.replacements?.[0]?.value;
      if (typeof replacement === "string" && Number.isInteger(match.offset) && Number.isInteger(match.length)) {
        replacements.push({ offset: match.offset, length: match.length, value: replacement });
      }
    }
    replacements.sort((a, b) => b.offset - a.offset);
    for (const item of replacements) {
      result = result.slice(0, item.offset) + item.value + result.slice(item.offset + item.length);
    }

    return res.json({
      output: result,
      matches: matches.length,
      mode: "Correction en ligne",
      requestId: req.requestId
    });
  } catch (error) {
    metrics.languageToolFailures += 1;
    console.error(JSON.stringify({ event: "languagetool_exception", requestId: req.requestId, error: String(error) }));
    return errorResponse(res, 504, "CORRECTOR_TIMEOUT", "Le service de correction est temporairement indisponible.", req.requestId);
  }
});

app.use((req, res) => {
  errorResponse(res, 404, "NOT_FOUND", "Route inconnue.", req.requestId);
});

app.use((error, req, res, _next) => {
  metrics.failures += 1;
  console.error(JSON.stringify({ event: "server_error", requestId: req.requestId, error: String(error) }));
  if (res.headersSent) return;
  errorResponse(res, 500, "SERVER_ERROR", "Erreur interne.", req.requestId);
});

export { app };
