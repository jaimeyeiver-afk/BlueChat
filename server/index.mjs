import { createServer } from "node:http";
import { pathToFileURL } from "node:url";
import { bearerToken, createFirebaseVerifier } from "./auth.mjs";

const MAX_BODY = 64 * 1024;
const WINDOW_MS = 60_000;
const MAX_REQUESTS_PER_WINDOW = 20;

function send(res, status, body) {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff"
  });
  res.end(JSON.stringify(body));
}

async function readJson(req) {
  let raw = "";
  for await (const chunk of req) {
    raw += chunk;
    if (Buffer.byteLength(raw, "utf8") > MAX_BODY) {
      throw new Error("BODY_TOO_LARGE");
    }
  }
  return JSON.parse(raw);
}

/**
 * Crea el servidor HTTP de Zeni IA.
 * `verifyToken` y `fetchImpl` se pueden inyectar para pruebas.
 */
export function createZeniServer({
  apiKey = process.env.GEMINI_API_KEY,
  model = process.env.GEMINI_MODEL || "gemini-3.8-flash",
  allowedOrigin = process.env.ALLOWED_ORIGIN || "",
  projectId = process.env.FIREBASE_PROJECT_ID,
  verifyToken,
  fetchImpl = fetch
} = {}) {
  const verify = verifyToken || (projectId ? createFirebaseVerifier({ projectId }) : null);
  const requests = new Map();

  function allowedByRateLimit(key) {
    const now = Date.now();
    const recent = (requests.get(key) || []).filter((time) => now - time < WINDOW_MS);
    if (recent.length >= MAX_REQUESTS_PER_WINDOW) {
      requests.set(key, recent);
      return false;
    }
    recent.push(now);
    requests.set(key, recent);
    return true;
  }

  return createServer(async (req, res) => {
    const origin = req.headers.origin;
    if (allowedOrigin && origin && origin !== allowedOrigin) {
      return send(res, 403, { error: "Origen no permitido." });
    }
    if (allowedOrigin && origin) {
      res.setHeader("access-control-allow-origin", allowedOrigin);
      res.setHeader("vary", "Origin");
    }
    if (req.method === "OPTIONS") {
      res.writeHead(204, {
        "access-control-allow-methods": "POST, GET, OPTIONS",
        "access-control-allow-headers": "content-type, authorization"
      });
      return res.end();
    }

    const path = new URL(req.url || "/", "http://localhost").pathname;
    if (req.method === "GET" && path === "/health") {
      return send(res, 200, { ok: true, service: "zeni-ia-api" });
    }
    if (req.method !== "POST" || path !== "/chat") {
      return send(res, 404, { error: "Ruta no encontrada." });
    }
    // Sin clave de Gemini o sin proyecto Firebase el servidor NO atiende chats (nunca queda abierto).
    if (!apiKey || !verify) {
      return send(res, 503, { error: "El servidor todavía no está configurado." });
    }
    const ip = req.headers["x-forwarded-for"]?.split(",")[0]?.trim() || req.socket.remoteAddress || "unknown";
    if (!allowedByRateLimit(`ip:${ip}`)) {
      return send(res, 429, { error: "Demasiadas solicitudes. Espera un minuto." });
    }

    // Inicio de sesión: cada petición debe traer el token de Firebase del usuario.
    const token = bearerToken(req.headers.authorization);
    if (!token) {
      return send(res, 401, { error: "Inicia sesión para usar Zeni IA." });
    }
    let user;
    try {
      user = await verify(token);
    } catch {
      return send(res, 401, { error: "Tu sesión no es válida o venció. Inicia sesión de nuevo." });
    }
    if (!allowedByRateLimit(`uid:${user.uid}`)) {
      return send(res, 429, { error: "Demasiadas solicitudes. Espera un minuto." });
    }

    let body;
    try {
      body = await readJson(req);
    } catch (error) {
      return send(res, error.message === "BODY_TOO_LARGE" ? 413 : 400, {
        error: error.message === "BODY_TOO_LARGE" ? "El mensaje es demasiado grande." : "JSON no válido."
      });
    }

    const messages = body?.messages;
    if (!Array.isArray(messages) || messages.length === 0 || messages.length > 20) {
      return send(res, 400, { error: "Envía entre 1 y 20 mensajes." });
    }
    const contents = [];
    for (const message of messages) {
      if (!message || !["user", "assistant"].includes(message.role) ||
          typeof message.text !== "string" || !message.text.trim() ||
          message.text.length > 8000) {
        return send(res, 400, { error: "Formato de mensaje no válido." });
      }
      contents.push({
        role: message.role === "assistant" ? "model" : "user",
        parts: [{ text: message.text }]
      });
    }
    if (contents[contents.length - 1].role !== "user") {
      return send(res, 400, { error: "El último mensaje debe ser del usuario." });
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 45_000);
    try {
      const response = await fetchImpl(
        `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
        {
          method: "POST",
          headers: {
            "content-type": "application/json",
            "x-goog-api-key": apiKey
          },
          body: JSON.stringify({
            system_instruction: {
              parts: [{ text: "Eres Zeni IA, un asistente útil. Responde en el idioma del usuario y explica de forma clara." }]
            },
            contents,
            tools: [{ google_search: {} }],
            generationConfig: { temperature: 0.7, maxOutputTokens: 2048 }
          }),
          signal: controller.signal
        }
      );
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        console.error("Gemini API respondió con estado", response.status);
        return send(res, 502, { error: "No se pudo obtener respuesta de la IA. Inténtalo más tarde." });
      }
      const answer = data?.candidates?.[0]?.content?.parts
        ?.map((part) => part.text || "")
        .join("")
        .trim();
      if (!answer) {
        return send(res, 502, { error: "La IA no devolvió texto. Inténtalo de nuevo." });
      }
      return send(res, 200, { answer });
    } catch (error) {
      const timedOut = error.name === "AbortError";
      return send(res, timedOut ? 504 : 502, {
        error: timedOut ? "La respuesta tardó demasiado." : "No se pudo conectar con el servicio de IA."
      });
    } finally {
      clearTimeout(timeout);
    }
  });
}

// Arranca solo cuando se ejecuta con `node index.mjs` (no al importarlo en las pruebas).
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const port = Number(process.env.PORT || 8080);
  if (!process.env.FIREBASE_PROJECT_ID) {
    console.warn("Aviso: falta FIREBASE_PROJECT_ID; /chat responderá 503 hasta configurarlo.");
  }
  createZeniServer().listen(port, "0.0.0.0", () => {
    console.log(`Zeni IA API escuchando en el puerto ${port}`);
  });
}
