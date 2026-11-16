package com.zeni.ia

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val engine = GeminiEngine(app)

    val conversations = mutableStateListOf<Conv>()
    var currentId by mutableStateOf<Long?>(null)
        private set
    val messages = mutableStateListOf<Msg>()

    var generating by mutableStateOf(false)
        private set

    init {
        conversations.addAll(store.load())
    }

    // ---------- Conversaciones ----------
    fun newChat() {
        currentId = null
        messages.clear()
    }

    fun openChat(id: Long) {
        val c = conversations.firstOrNull { it.id == id } ?: return
        currentId = id
        messages.clear()
        messages.addAll(c.messages)
    }

    fun deleteChat(id: Long) {
        conversations.removeAll { it.id == id }
        if (currentId == id) newChat()
        store.save(conversations.toList())
    }

    private fun persist() {
        val id = currentId ?: System.currentTimeMillis().also { currentId = it }
        val title = messages.firstOrNull { it.role == "user" }?.text?.take(40) ?: "Nuevo chat"
        val conv = Conv(id, title, messages.toList())
        val i = conversations.indexOfFirst { it.id == id }
        if (i >= 0) conversations[i] = conv else conversations.add(0, conv)
        store.save(conversations.toList())
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || generating) return
        messages.add(Msg("user", t))
        persist()
        generating = true
        val snapshot = messages.toList()
        viewModelScope.launch {
            val answer = StringBuilder()
            var index = -1
            try {
                engine.stream(snapshot).collect { chunk ->
                    answer.append(chunk)
                    if (index < 0) {
                        messages.add(Msg("assistant", answer.toString()))
                        index = messages.lastIndex
                    } else {
                        messages[index] = Msg("assistant", answer.toString())
                    }
                }
                if (index < 0) messages.add(Msg("assistant", "…"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                messages.add(Msg("assistant", "⚠️ " + friendlyError(e)))
            } finally {
                generating = false
                persist()
            }
        }
    }

    /** Convierte los errores técnicos en un aviso que se entienda. */
    private fun friendlyError(e: Throwable): String {
        val raw = e.message ?: e.javaClass.simpleName
        val low = raw.lowercase()
        return when {
            e is IOException || "unable to resolve host" in low || "timeout" in low ->
                "No hay conexión a internet. Revisa tu WiFi o datos y vuelve a intentar."
            "google-services" in low || "firebase no está configurado" in low ->
                "Falta configurar Firebase en la app (google-services.json)."
            "not found" in low || "404" in low ->
                "El modelo de IA (${GeminiEngine.MODEL_NAME}) no está disponible. Hay que cambiar el nombre del modelo en la app."
            "quota" in low || "429" in low ->
                "Se alcanzó el límite de uso de la IA. Espera un momento y vuelve a intentar."
            "permission" in low || "403" in low || "not enabled" in low || "disabled" in low || "api key" in low ->
                "Firebase AI Logic no está activado o la clave no tiene permiso. Actívalo en la consola de Firebase (AI Logic → Gemini Developer API)."
            else -> "Error: $raw"
        }
    }
}
