import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { generateKeyPair, SignJWT } from "jose";
import { bearerToken, createFirebaseVerifier } from "../auth.mjs";
import { createZeniServer } from "../index.mjs";

// Proyecto y claves ficticios: solo existen dentro de estas pruebas.
const PROJECT = "proyecto-de-prueba";
let good, other;

before(async () => {
  good = await generateKeyPair("RS256");
  other = await generateKeyPair("RS256");
});

function sign({ key = good.privateKey, aud = PROJECT, iss = `https://securetoken.google.com/${PROJECT}`,
  sub = "usuario-1", exp = "1h", alg = "RS256" } = {}) {
  return new SignJWT({ email: "alguien@example.com", auth_time: Math.floor(Date.now() / 1000) - 10 })
    .setProtectedHeader({ alg, kid: "k1" })
    .setSubject(sub).setAudience(aud).setIssuer(iss)
    .setIssuedAt().setExpirationTime(exp)
    .sign(key);
}

const verifier = () => createFirebaseVerifier({ projectId: PROJECT, keyResolver: async () => good.publicKey });

test("acepta un token válido", async () => {
  const user = await verifier()(await sign());
  assert.equal(user.uid, "usuario-1");
});

test("rechaza token de otro proyecto, emisor, firma o vencido", async () => {
  const verify = verifier();
  await assert.rejects(verify(await sign({ aud: "otro" })));
  await assert.rejects(verify(await sign({ iss: "https://securetoken.google.com/otro" })));
  await assert.rejects(verify(await sign({ key: other.privateKey })));
  await assert.rejects(verify(await sign({ exp: Math.floor(Date.now() / 1000) - 60 })));
  await assert.rejects(verify("no.es.un-token"));
});

test("bearerToken lee solo el formato correcto", () => {
  assert.equal(bearerToken("Bearer abc.def.ghi"), "abc.def.ghi");
  assert.equal(bearerToken("Basic abc"), null);
  assert.equal(bearerToken(undefined), null);
});

// ---- Servidor completo con Gemini simulada ----
let app, base, geminiCalls;
const fakeFetch = async (_url, options) => {
  geminiCalls.push(options.headers["x-goog-api-key"]);
  return { ok: true, json: async () => ({ candidates: [{ content: { parts: [{ text: "Hola desde la prueba" }] } }] }) };
};

before(async () => {
  geminiCalls = [];
  app = createZeniServer({ apiKey: "clave-falsa", projectId: PROJECT, verifyToken: verifier(), fetchImpl: fakeFetch });
  await new Promise((resolve) => app.listen(0, "127.0.0.1", resolve));
  base = `http://127.0.0.1:${app.address().port}`;
});
after(() => app.close());

const chat = (headers = {}) => fetch(`${base}/chat`, {
  method: "POST",
  headers: { "content-type": "application/json", ...headers },
  body: JSON.stringify({ messages: [{ role: "user", text: "Hola" }] })
});

test("/health es público", async () => {
  assert.equal((await fetch(`${base}/health`)).status, 200);
});

test("/chat sin sesión → 401 y no llama a Gemini", async () => {
  assert.equal((await chat()).status, 401);
  assert.equal((await chat({ authorization: "Bearer token-falso" })).status, 401);
  assert.equal(geminiCalls.length, 0);
});

test("/chat con sesión válida → 200 con respuesta", async () => {
  const res = await chat({ authorization: `Bearer ${await sign()}` });
  assert.equal(res.status, 200);
  assert.equal((await res.json()).answer, "Hola desde la prueba");
  assert.equal(geminiCalls.length, 1);
});

test("sin FIREBASE_PROJECT_ID el servidor no queda abierto → 503", async () => {
  const closed = createZeniServer({ apiKey: "clave-falsa", projectId: "", fetchImpl: fakeFetch });
  await new Promise((resolve) => closed.listen(0, "127.0.0.1", resolve));
  const res = await fetch(`http://127.0.0.1:${closed.address().port}/chat`, {
    method: "POST", headers: { "content-type": "application/json" },
    body: JSON.stringify({ messages: [{ role: "user", text: "Hola" }] })
  });
  closed.close();
  assert.equal(res.status, 503);
});
