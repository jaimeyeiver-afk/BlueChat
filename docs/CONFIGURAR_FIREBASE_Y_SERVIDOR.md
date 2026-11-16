# Configurar Firebase, inicio de sesión con Google y servidor

Lo que ya está hecho en el código (rama `zeni-online-gemini`) y lo que debe hacer el propietario.
**Nada de esto se puede hacer desde el código**: requiere tus cuentas de Google y GitHub. No pegues claves en el chat ni en archivos del repositorio.

## Cómo encaja todo

```
APK (J4 Core)  ──token de Firebase──▶  Servidor Zeni  ──clave privada──▶  Gemini
   │                                      (verifica el token)
   └─ Google Sign-In + Firebase Auth
```

- La **dirección del servidor** se fija al compilar (variable `ZENI_SERVER_URL`); el usuario no escribe nada.
- La **configuración de Firebase** entra al compilar (secreto `GOOGLE_SERVICES_JSON`).
- La **clave de Gemini** vive solo en el servidor.
- Si falta alguno de los dos primeros, la app compila igual y muestra "Zeni todavía no está conectada".

## Paso 1 — Crear el proyecto Firebase
1. Entra a <https://console.firebase.google.com> con tu cuenta de Google → **Agregar proyecto** (nombre: `Zeni IA`; Analytics es opcional).
2. En **Configuración del proyecto → General**, anota el **ID del proyecto** (lo necesitará el servidor).

## Paso 2 — Registrar la app Android
1. **Agregar app → Android**. Nombre de paquete exacto: `com.zeni.ia`.
2. Huellas de firma: en GitHub → **Actions** → último run de la rama → **Summary** aparece "Huella de firma de esta APK" con **SHA-1** y **SHA-256**. Añade ambas en **Configuración del proyecto → Tus apps → Android → Agregar huella digital**.
   - La APK de pruebas usa una clave de firma fija (`app/zeni-test.keystore`, pública a propósito) para que esa huella no cambie en cada compilación.
   - SHA-1 actual: `3A:97:7F:20:37:D0:27:11:B8:30:5E:96:29:E6:9E:6D:8A:BE:08:75`

## Paso 3 — Activar Google como método de acceso
1. **Authentication → Comenzar → Método de acceso → Google → Habilitar**, elige un correo de asistencia y guarda.
2. **Hazlo antes de descargar el archivo del paso 4**: así `google-services.json` incluye el cliente web de OAuth que necesita el login.

## Paso 4 — Descargar `google-services.json` y guardarlo en GitHub
1. **Configuración del proyecto → Tus apps → Android → Descargar google-services.json**.
2. En GitHub: repositorio → **Settings → Secrets and variables → Actions → New repository secret**.
   - Nombre: `GOOGLE_SERVICES_JSON`
   - Valor: el **contenido completo** del archivo.
3. El workflow comprueba que sea un JSON válido, que sea de `com.zeni.ia` y que traiga el cliente web; si no, falla con un mensaje claro.

## Paso 5 — Clave de Gemini y servidor
1. Crea una clave en <https://aistudio.google.com/apikey>. Guárdala **solo** en el panel de secretos de tu hosting.
2. Publica la carpeta `server/` en un hosting con Node.js 20+ y HTTPS. Elige el proveedor y avísame: puedo preparar los archivos de despliegue para ese proveedor concreto.
3. Variables del servidor:
   - `GEMINI_API_KEY` = tu clave (secreto)
   - `FIREBASE_PROJECT_ID` = el ID del paso 1
   - (opcional) `GEMINI_MODEL`; por defecto `gemini-3.8-flash`
4. Comprueba `https://TU-SERVIDOR/health` → debe responder `{"ok":true,...}`.

## Paso 6 — Decirle a la app dónde está el servidor
GitHub → **Settings → Secrets and variables → Actions → pestaña Variables → New repository variable**:
- Nombre: `ZENI_SERVER_URL`
- Valor: `https://TU-SERVIDOR` (sin `/` al final). No es un secreto.

## Paso 7 — Compilar e instalar
1. **Actions → Build Zeni IA APK → Run workflow** (rama `zeni-online-gemini`).
2. Descarga el artefacto `ZeniIA-J4-test`.
3. **Desinstala la versión anterior** del J4 Core antes de instalar: ahora la APK se firma con otra clave y Android no permite actualizar encima.
4. Abre la app → **Continuar con Google** → chatea.

## Si algo falla
| Síntoma | Causa probable |
|---|---|
| "Zeni todavía no está conectada" | Falta `GOOGLE_SERVICES_JSON` o `ZENI_SERVER_URL` al compilar (revisa los avisos amarillos del run) |
| El login se cierra o dice que no hay credenciales | SHA-1/SHA-256 no registrados en Firebase, o Google no habilitado antes de descargar el JSON |
| "Tu sesión no es válida" | `FIREBASE_PROJECT_ID` del servidor no coincide con el proyecto de la app |
| "El servidor todavía no está configurado" (503) | Falta `GEMINI_API_KEY` o `FIREBASE_PROJECT_ID` en el servidor |
| Error 502 al chatear | Revisa los registros del servidor (clave de Gemini, cuota o modelo) |

## Antes de compartir la app con otras personas
- Sustituir `app/zeni-test.keystore` por una clave de firma **privada** (guardada como secreto) y registrar su nueva huella en Firebase.
- Añadir Firebase App Check / Play Integrity para que solo la app oficial use el servidor.
- Las conversaciones se guardan en el teléfono sin separar por cuenta: en un teléfono compartido, otra cuenta vería el historial.
