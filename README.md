# Zeni IA

Asistente de IA tipo ChatGPT con interfaz oscura morada y conversaciones guardadas en el teléfono.

## Estado del proyecto
- La rama `fix/streaming-replies` conserva la versión anterior.
- La rama `zeni-online-gemini` conserva el servidor anterior con Gemini.
- La rama `zeni-openai-server` cambia el servidor online a OpenAI Responses API.
- La app Android sigue usando Google/Firebase Authentication para iniciar sesión y envía un token al servidor en cada mensaje.
- El servidor Node.js valida ese token y mantiene la clave OpenAI fuera del APK.
- La app recibe la misma ruta `POST /chat`; no hace falta cambiar el contrato Android para sustituir el proveedor de IA.
- La URL HTTPS del servidor se configura al compilar mediante `ZENI_SERVER_URL`.

## Servidor OpenAI
Consulta [server/README.md](server/README.md) y [docs/CONFIGURAR_OPENAI_Y_SERVIDOR.md](docs/CONFIGURAR_OPENAI_Y_SERVIDOR.md). La clave `OPENAI_API_KEY` debe existir solo como secreto del hosting, nunca en la APK ni en GitHub.

## APK
El workflow Android existente puede compilar la aplicación cuando se configure la URL HTTPS del servidor y la configuración Firebase requerida para el inicio de sesión.

## Tecnología
Kotlin · Jetpack Compose · MediaPipe LLM Inference (modo local) · Node.js · OpenAI Responses API.
