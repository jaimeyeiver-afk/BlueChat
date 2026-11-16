# Zeni IA

Asistente de IA tipo ChatGPT que corre **100% en tu teléfono, sin internet**.
Diseño oscuro morado, chats guardados en el dispositivo, privado.

## Cómo conseguir el APK
1. Ve a la pestaña **Actions** → "Build Zeni IA APK" → último run → descarga `ZeniIA-apk`.
2. O entra a **Releases** → `latest` → descarga `ZeniIA.apk`.

## Modelo de IA (solo la primera vez)
La app necesita un modelo local en formato MediaPipe `.task` (por ejemplo Gemma 3 1B o Qwen 2.5 0.5B/1.5B en formato LiteRT / MediaPipe).
Al abrir la app: **Elegir modelo del teléfono** (archivo ya descargado) o **pegar un enlace** y descargarlo una sola vez.
Después todo funciona sin conexión.

## Tecnología
Kotlin · Jetpack Compose · MediaPipe LLM Inference (`tasks-genai`).