# Servidor seguro de Zeni IA

Este servidor mantiene la clave de Gemini fuera de la APK. La app Android debe llamar a este servidor; nunca debe llamar a Gemini con una clave incluida dentro de la aplicación.

## Requisitos
- Node.js 20 o posterior.
- Una clave de Gemini configurada por el propietario del servicio en el panel de variables secretas del hosting.

## Variables del servidor
- `GEMINI_API_KEY`: clave privada, solo en el servidor.
- `GEMINI_MODEL`: modelo de Gemini (por defecto `gemini-3.8-flash`).
- `PORT`: puerto (por defecto 8080).
- `ALLOWED_ORIGIN`: opcional para limitar solicitudes desde una web.

## Ejecutar localmente
1. Copia `.env.example` a `.env`.
2. Coloca la clave en `.env` solo en tu equipo/servidor; nunca la subas a GitHub.
3. Ejecuta `npm start`.
4. Comprueba `GET /health`.
5. Envía un `POST /chat` con JSON: `{"messages":[{"role":"user","text":"Hola"}]}`.

El endpoint usa Google Search grounding para preguntas que necesiten información actual. La respuesta es JSON con el campo `answer`.

## Seguridad
- Límite básico de solicitudes por dirección IP.
- Límite de tamaño y cantidad de mensajes.
- La clave no se devuelve al cliente ni se registra.
- Para publicar el servicio, usa HTTPS y configura límites/cuotas en el proveedor.
- El límite en memoria es básico; en un despliegue con varias instancias debe sustituirse por un limitador compartido.
