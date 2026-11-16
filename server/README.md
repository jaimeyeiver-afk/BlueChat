# Servidor seguro de Zeni IA

Este servidor mantiene la clave de Gemini fuera de la APK y solo atiende a usuarios que iniciaron sesión con Google (Firebase Authentication). La app Android debe llamar a este servidor; nunca debe llamar a Gemini con una clave incluida dentro de la aplicación.

## Requisitos
- Node.js 20 o posterior.
- Una clave de Gemini configurada por el propietario del servicio en el panel de variables secretas del hosting.
- Un proyecto de Firebase con Authentication y el proveedor Google activados.

## Variables del servidor
- `GEMINI_API_KEY`: clave privada, solo en el servidor.
- `FIREBASE_PROJECT_ID`: ID del proyecto Firebase. Con él el servidor valida los tokens de sesión (no necesita ninguna clave privada de Firebase).
- `GEMINI_MODEL`: modelo de Gemini (por defecto `gemini-3.8-flash`).
- `PORT`: puerto (por defecto 8080).
- `ALLOWED_ORIGIN`: opcional para limitar solicitudes desde una web.

Si falta `GEMINI_API_KEY` o `FIREBASE_PROJECT_ID`, `POST /chat` responde 503: el servidor nunca queda abierto sin autenticación.

## Ejecutar localmente
1. `npm install`
2. Copia `.env.example` a `.env`, rellénalo solo en tu equipo/servidor y nunca lo subas a GitHub.
3. Carga las variables y ejecuta `npm start` (por ejemplo `node --env-file=.env index.mjs`).
4. Comprueba `GET /health`.
5. `POST /chat` exige la cabecera `Authorization: Bearer <ID token de Firebase>` y un JSON como `{"messages":[{"role":"user","text":"Hola"}]}`.

## Pruebas
`npm test` ejecuta pruebas con claves y tokens ficticios (no usan red ni credenciales reales).

## Respuestas de /chat
- `200` `{ "answer": "..." }`
- `401` sin sesión, o token inválido/vencido
- `429` demasiadas solicitudes (por IP y por usuario)
- `503` servidor sin configurar

El endpoint usa Google Search grounding para preguntas que necesiten información actual.

## Seguridad
- Cada petición se valida contra Firebase (firma RS256, emisor, audiencia y caducidad del token).
- Límite básico de solicitudes por dirección IP y por usuario.
- Límite de tamaño y cantidad de mensajes.
- La clave no se devuelve al cliente ni se registra.
- Para publicar el servicio, usa HTTPS y configura límites/cuotas en el proveedor.
- El límite en memoria es básico; en un despliegue con varias instancias debe sustituirse por un limitador compartido.
- Mejora futura recomendada: Firebase App Check / Play Integrity para que solo la app oficial pueda usar el servidor.
