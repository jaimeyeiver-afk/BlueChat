# Zeni IA

Asistente de IA tipo ChatGPT con interfaz oscura morada y conversaciones guardadas en el teléfono.

## Estado del proyecto
- La rama `fix/streaming-replies` conserva la versión anterior.
- El trabajo de conexión online se desarrolla por separado en `zeni-online-gemini`.
- El servidor seguro está en `server/`. Mantiene la clave de Gemini fuera de la APK y ofrece `POST /chat`, que ahora exige sesión de Firebase.
- La app Android inicia sesión con Google (Firebase Authentication) y envía el token de sesión al servidor en cada mensaje.
- La dirección del servidor se fija al compilar (`ZENI_SERVER_URL`); el usuario no escribe ninguna dirección.
- Pendiente (requiere al propietario): crear el proyecto Firebase, publicar el servidor y cargar `GOOGLE_SERVICES_JSON` y `ZENI_SERVER_URL` en GitHub. Guía paso a paso: [docs/CONFIGURAR_FIREBASE_Y_SERVIDOR.md](docs/CONFIGURAR_FIREBASE_Y_SERVIDOR.md).

## Servidor online
Consulta [server/README.md](server/README.md). La clave de Gemini debe configurarse como secreto en el hosting, nunca en un archivo de la app ni en GitHub. El servidor usa Google Search grounding para consultas que necesitan información actual.

## APK actual (modo local)
1. Ve a **Actions** → "Build Zeni IA APK" → último run → descarga `ZeniIA-apk`.
2. O entra a **Releases** → `latest` → descarga `ZeniIA.apk`.

## Modelo local
La app actual usa un modelo MediaPipe `.task`, que se guarda en el teléfono y permite conversar sin conexión.

## Tecnología
Kotlin · Jetpack Compose · MediaPipe LLM Inference · Node.js API para Gemini.
