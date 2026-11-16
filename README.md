# Zeni IA (copia 32 bits, con internet)

Asistente de IA tipo ChatGPT que usa **Gemini por internet** mediante **Firebase AI Logic** (Gemini Developer API).
Diseño oscuro morado, chats guardados en el dispositivo. Sin servidor propio y sin claves privadas de Gemini dentro de la app.

## Cómo conseguir el APK
1. Pestaña **Actions** → "Build Zeni IA APK (copia 32 bits, con internet)" → último run exitoso.
2. Descarga `ZeniIA-internet` para la versión release.
3. `ZeniIA-internet-debug` es solo para pruebas y usa el proveedor debug de App Check.

## Qué necesita funcionar
- En Firebase Console, proyecto `zeni-ia`: **Build → AI Logic → Gemini Developer API** activada.
- `app/google-services.json` del proyecto, con el paquete `com.zeni.ia`.
- Internet en el teléfono y Google Play Services.
- App Check registrado para la app Android. La versión debug usa Debug Provider y su token debe registrarse de forma privada en Firebase Console. La versión release usa Play Integrity.

## App Check
- **Debug:** al abrir la APK de pruebas, App Check genera un token local. Regístralo en Firebase Console → Security → App Check → Apps → tu app → Manage debug tokens. Mantén ese token privado; nunca lo subas a GitHub ni lo incluyas en una versión release.
- **Release:** usa Play Integrity. Configura Play Integrity en Firebase App Check y ajusta la política de reconocimiento según cómo distribuyas la APK (Google Play o instalación externa). La huella SHA-256 del certificado de firma debe corresponder al APK distribuido.

## Si el chat da error
- "Firebase AI Logic no está activado…" → activar AI Logic en la consola.
- "Firebase App Check token is invalid" → comprobar que el proveedor correcto esté configurado y que el token debug esté registrado (debug), o que Play Integrity y la huella de firma estén configurados (release).
- "El modelo no está disponible…" → revisar `MODEL_NAME` en `GeminiEngine.kt`.

## Tecnología
Kotlin · Jetpack Compose · Firebase AI Logic (`firebase-ai`) · Gemini · Firebase App Check.
Sin librerías nativas, así que funciona en ARM de 32 y de 64 bits (por ejemplo Galaxy J4 Core).
