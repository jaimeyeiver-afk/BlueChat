# Zeni IA (copia 32 bits, con internet)

Asistente de IA tipo ChatGPT que usa **Gemini por internet** mediante **Firebase AI Logic** (Gemini Developer API).
Diseño oscuro morado, chats guardados en el dispositivo. Sin servidor propio y sin claves dentro de la app.

## Cómo conseguir el APK
1. Pestaña **Actions** → "Build Zeni IA APK (copia 32 bits, con internet)" → último run → descarga `ZeniIA-internet`.

## Qué necesita funcionar
- En la consola de Firebase (proyecto `zeni-ia`): **Build → AI Logic → Gemini Developer API** activada.
- `app/google-services.json` del proyecto, con el paquete `com.zeni.ia`.
- Internet en el teléfono y Google Play Services.

## Si el chat da error
- "Firebase AI Logic no está activado…" → activar AI Logic en la consola.
- "El modelo no está disponible…" → cambiar `MODEL_NAME` en `GeminiEngine.kt`.

## Tecnología
Kotlin · Jetpack Compose · Firebase AI Logic (`firebase-ai`) · Gemini.
Sin librerías nativas, así que funciona en ARM de 32 y de 64 bits (por ejemplo Galaxy J4 Core).
