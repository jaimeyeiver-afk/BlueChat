# Servidor seguro de Zeni IA (OpenAI)

La app Android conserva su contrato actual: envía `POST /chat` con el token de sesión de Firebase y una lista de mensajes. Este servidor verifica la sesión y llama a la API Responses de OpenAI. La clave de OpenAI vive exclusivamente en el entorno del servidor.

## Requisitos
- Node.js 20 o posterior.
- Un hosting compatible con Node.js y HTTPS.
- Una cuenta OpenAI API administrada por un adulto responsable, con facturación y límites de gasto revisados.
- El ID del proyecto Firebase existente para verificar los tokens. No se necesita una clave privada de Firebase.

## Variables del servidor
- `OPENAI_API_KEY`: secreto de OpenAI, solo en el panel del hosting.
- `FIREBASE_PROJECT_ID`: ID del proyecto Firebase que emite los tokens de inicio de sesión.
- `OPENAI_MODEL`: modelo API habilitado para la cuenta; por defecto `gpt-6-astra`.
- `PORT`: puerto del servidor (por defecto 8080).
- `ALLOWED_ORIGIN`: opcional para limitar orígenes web.

Si falta la clave de OpenAI o el ID de Firebase, `POST /chat` responde 503. La API nunca se deja abierta sin autenticación.

## Ejecutar
1. Instala Node.js 20+ y ejecuta `npm install`.
2. Configura las variables en el entorno local o en el panel del hosting. No guardes secretos en Git.
3. Ejecuta `npm start`.
4. Comprueba `GET /health`.
5. `POST /chat` requiere `Authorization: Bearer <Firebase ID token>` y JSON como `{"messages":[{"role":"user","text":"Hola"}]}`.

## Seguridad
- Verifica firma, emisor, audiencia y caducidad del token Firebase.
- Límite básico por IP y usuario; máximo 20 mensajes por petición y 8.000 caracteres por mensaje.
- Cuerpo máximo de 64 KiB y tiempo máximo para llamadas externas.
- La clave de OpenAI no se incluye en el APK ni en GitHub.
- Publica solo mediante HTTPS y configura límites de gasto/uso en la cuenta API.
- El limitador en memoria es básico; para varias instancias debe sustituirse por uno compartido.
- Antes de distribuir: añadir Firebase App Check / Play Integrity y separar el historial por cuenta.

## Pruebas
Ejecuta `npm test` para las pruebas automatizadas del servidor.
