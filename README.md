# Zeni IA

Asistente de IA tipo ChatGPT con interfaz oscura morada y conversaciones guardadas en el teléfono.

## Estado del proyecto
- La rama `fix/streaming-replies` conserva la versión anterior.
- El trabajo de conexión online se desarrolla por separado en `zeni-online-gemini`.
- El servidor seguro inicial está en `server/`. Mantiene la clave de Gemini fuera de la APK y ofrece el endpoint `POST /chat`.
- La conexión Android todavía debe integrarse y probarse; esta rama aún no contiene una APK online lista.

## Servidor online
Consulta [server/README.md](server/README.md). La clave de Gemini debe configurarse como secreto en el hosting, nunca en un archivo de la app ni en GitHub. El servidor usa Google Search grounding para consultas que necesitan información actual.

## APK actual (modo local)
1. Ve a **Actions** → "Build Zeni IA APK" → último run → descarga `ZeniIA-apk`.
2. O entra a **Releases** → `latest` → descarga `ZeniIA.apk`.

## Modelo local
La app actual usa un modelo MediaPipe `.task`, que se guarda en el teléfono y permite conversar sin conexión.

## Tecnología
Kotlin · Jetpack Compose · MediaPipe LLM Inference · Node.js API para Gemini.
