# Configurar el servidor OpenAI de Zeni IA

El cambio a OpenAI no requiere eliminar el proyecto Firebase. Firebase Authentication sigue validando qué usuario puede usar el servidor; Firebase AI Logic y Gemini dejan de participar en las respuestas online.

## Qué se conserva
- La app Android `com.zeni.ia`.
- El inicio de sesión con Google/Firebase Authentication.
- El formato `POST /chat` que ya utiliza la APK.
- El historial local de conversaciones.

## Qué cambia
- El servidor sustituye Gemini por la API Responses de OpenAI.
- La clave `OPENAI_API_KEY` se guarda solo como secreto en el hosting.
- El servidor puede usar búsqueda web mediante la herramienta integrada `web_search`.

## Configuración que requiere al adulto responsable
La API de OpenAI tiene facturación separada de una suscripción ChatGPT. Un adulto responsable debe gestionar la cuenta API, revisar las condiciones y fijar límites de gasto antes de habilitar el servicio.

Variables del hosting:
- `OPENAI_API_KEY`: clave secreta, nunca en GitHub ni en el APK.
- `FIREBASE_PROJECT_ID`: ID del proyecto Firebase que ya utiliza la app.
- `OPENAI_MODEL`: opcional; por defecto `gpt-6-astra`.
- `PORT`: normalmente lo asigna el hosting.

Publica la carpeta `server/` en un hosting Node.js 20+ con HTTPS. Después, configura la variable de repositorio `ZENI_SERVER_URL` con la dirección HTTPS del servidor y compila la APK usando el workflow existente.

## Comprobación
- `GET https://TU-SERVIDOR/health` debe responder `{"ok":true,"service":"zeni-ia-openai-api"}`.
- Abre Zeni IA, inicia sesión y envía un mensaje sencillo.
- Si indica que el servidor no está configurado, revisa las variables del hosting.
- No compartas claves API, tokens de Firebase ni archivos privados.

## Nota sobre Firebase
No borres Firebase Authentication ni el proyecto Firebase: el servidor usa el ID del proyecto para verificar los tokens de sesión. Desactivar el proveedor de inicio de sesión impediría que la APK se autenticara.
