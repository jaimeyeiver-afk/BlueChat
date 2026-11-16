package com.zeni.ia

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

enum class ModelState { None, Busy, Loading, Ready }

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val engine = LlmEngine(app)

    val conversations = mutableStateListOf<Conv>()
    var currentId by mutableStateOf<Long?>(null)
        private set
    val messages = mutableStateListOf<Msg>()

    var modelState by mutableStateOf(ModelState.None)
        private set
    var progress by mutableStateOf(0f)
        private set
    var status by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var generating by mutableStateOf(false)
        private set

    init {
        conversations.addAll(store.load())
        if (engine.hasModel()) loadModel()
    }

    fun clearError() { error = null }

    // ---------- Modelo ----------
    fun importModel(uri: Uri) = runModelJob("Copiando modelo al teléfono…") {
        engine.importModel(uri) { progress = it }
    }

    fun downloadModel(url: String) = runModelJob("Descargando modelo (solo esta vez)…") {
        engine.downloadModel(url.trim()) { progress = it }
    }

    private fun runModelJob(label: String, block: suspend () -> Unit) {
        modelState = ModelState.Busy
        status = label
        progress = 0f
        error = null
        viewModelScope.launch {
            try {
                block()
                loadSuspend()
            } catch (e: Throwable) {
                error = e.message ?: "Error desconocido"
                modelState = if (engine.hasModel()) ModelState.Loading else ModelState.None
                if (engine.hasModel()) loadModel()
            }
        }
    }

    private fun loadModel() {
        viewModelScope.launch { loadSuspend() }
    }

    private suspend fun loadSuspend() {
        modelState = ModelState.Loading
        status = "Cargando Zeni IA…"
        try {
            engine.load()
            modelState = ModelState.Ready
        } catch (e: Throwable) {
            error = "No se pudo cargar el modelo: ${e.message}"
            engine.deleteModel()
            modelState = ModelState.None
        }
    }

    fun removeModel() {
        engine.deleteModel()
        modelState = ModelState.None
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
        if (t.isEmpty() || generating || modelState != ModelState.Ready) return
        messages.add(Msg("user", t))
        persist()
        generating = true
        val snapshot = messages.toList()
        viewModelScope.launch {
            val updates = Channel<String>(Channel.CONFLATED)
            var responseStarted = false
            val renderer = launch {
                for (partial in updates) {
                    if (!responseStarted) {
                        messages.add(Msg("assistant", partial))
                        responseStarted = true
                    } else if (messages.lastOrNull()?.role == "assistant") {
                        messages[messages.lastIndex] = Msg("assistant", partial)
                    }
                }
            }
            try {
                val answer = engine.generate(snapshot) { partial ->
                    updates.trySend(partial)
                }.ifBlank { "…" }
                updates.close()
                renderer.join()
                if (responseStarted && messages.lastOrNull()?.role == "assistant") {
                    messages[messages.lastIndex] = Msg("assistant", answer)
                } else {
                    messages.add(Msg("assistant", answer))
                }
            } catch (e: Throwable) {
                updates.close()
                renderer.join()
                messages.add(Msg("assistant", "⚠️ Error: ${e.cause?.message ?: e.message}"))
            } finally {
                generating = false
                persist()
            }
        }
    }
}
