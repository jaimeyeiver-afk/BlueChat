package com.zeni.ia

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** El servidor rechazó el token de sesión (venció o no es válido). */
class SessionRejectedException(message: String) : Exception(message)

object OnlineClient {
    suspend fun generate(serverUrl: String, idToken: String, messages: List<Msg>): String = withContext(Dispatchers.IO) {
        val base = serverUrl.trim().trimEnd('/')
        require(base.startsWith("https://")) { "La dirección del servidor debe comenzar con https://" }
        val payload = JSONObject().put("messages", JSONArray().apply {
            messages.takeLast(20).forEach { message ->
                put(JSONObject().put("role", message.role).put("text", message.text))
            }
        })
        val connection = (URL("$base/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 50_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $idToken")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val result = if (raw.isNotBlank()) JSONObject(raw) else JSONObject()
            if (status == 401) {
                throw SessionRejectedException(result.optString("error").ifBlank {
                    "Tu sesión no es válida o venció. Inicia sesión de nuevo."
                })
            }
            if (status !in 200..299) {
                throw IllegalStateException(result.optString("error").ifBlank {
                    "El servidor respondió con el código $status."
                })
            }
            result.optString("answer").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("El servidor no devolvió una respuesta.")
        } finally {
            connection.disconnect()
        }
    }
}
