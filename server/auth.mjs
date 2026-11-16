import { createRemoteJWKSet, jwtVerify } from "jose";

// Claves públicas con las que Firebase firma los tokens de sesión (no son secretas).
const FIREBASE_JWKS_URL = new URL(
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
);

/**
 * Crea una función que valida un ID token de Firebase Authentication.
 * Solo necesita el ID del proyecto Firebase (no una clave privada).
 * `keyResolver` se puede reemplazar en las pruebas.
 */
export function createFirebaseVerifier({ projectId, keyResolver } = {}) {
  if (!projectId) throw new Error("FIREBASE_PROJECT_ID es obligatorio.");
  const keys = keyResolver || createRemoteJWKSet(FIREBASE_JWKS_URL);

  return async function verifyFirebaseToken(token) {
    const { payload } = await jwtVerify(token, keys, {
      algorithms: ["RS256"],
      issuer: `https://securetoken.google.com/${projectId}`,
      audience: projectId,
      clockTolerance: 5
    });
    if (typeof payload.sub !== "string" || !payload.sub) {
      throw new Error("Token sin identificador de usuario.");
    }
    const now = Math.floor(Date.now() / 1000);
    if (typeof payload.auth_time === "number" && payload.auth_time > now + 5) {
      throw new Error("Token con fecha de autenticación futura.");
    }
    return {
      uid: payload.sub,
      email: typeof payload.email === "string" ? payload.email : undefined
    };
  };
}

/** Extrae el token de la cabecera `Authorization: Bearer <token>`. */
export function bearerToken(header) {
  if (typeof header !== "string") return null;
  const match = /^Bearer\s+([A-Za-z0-9._~+/=-]+)$/.exec(header.trim());
  return match ? match[1] : null;
}
