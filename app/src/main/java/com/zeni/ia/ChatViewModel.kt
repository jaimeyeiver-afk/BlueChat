package com.zeni.ia

import android.app.Activity
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
    private val auth = AuthManager(app)

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

    // ---------- Servidor y sesión ----------
    /** Dirección del servidor, fijada al compilar la app (el usuario no escribe nada). */
    private val serverUrl = BuildConfig.ZENI_SERVER_URL
    val serverConfigured: Boolean = serverUrl.startsWith("https://") && serverUrl.length > 12
    val firebaseConfigured: Boolean = auth.isConfigured

    var signedIn by mutableStateOf(false)
        private set
    var userLabel by mutableStateOf<String?>(null)
        private set
    var signingIn by mutableStateOf(false)
        private set

    val canChat: Boolean
        get() = serverConfigured && firebaseConfigured && signedIn

    private val stopObservingAuth: () -> Unit

    init {
        conversations.addAll(store.load())
        stopObservingAuth = auth.observe { user ->
            signedIn = user != null
            userLabel = user?.email ?: user?.displayName
        }
        viewModelScope.launch { auth.resumePendingBrowserSignIn() }
    }

    override fun onCleared() {
        stopObservingAuth()
    }

    fun clearError() { error = null }

    fun signIn(activity: Activity) {
        if (signingIn) return
        signingIn = true
        error = null
        viewModelScope.launch {
            try {
                auth.signIn(activity)
            } catch (_: SignInCancelled) {
                // El usuario cerró el selector de cuentas; no es un error.
            } catch (e: Throwable) {
                error = "No se pudo iniciar sesión: ${e.message ?: "error desconocido"}"
            } finally {
                signingIn = false
            }
        }
    }

    fun signOut() {
        newChat()
        viewModelScope.launch { auth.signOut() }
    }

    /** Pide el token de sesión y llama al servidor; si el servidor lo rechaza, renueva el token una vez. */
    private suspend fun askServer(history: List<Msg>): String = try {
        OnlineClient.generate(serverUrl, auth.idToken(), history)
    } catch (_: SessionRejectedException) {
        OnlineClient.generate(serverUrl, auth.idToken(forceRefresh = true), history)
    }

    fun useLocalMode() {
        if (modelState != ModelState.Ready && engine.hasModel()) loadModel()
    }

    // ---------- Modelo local ----------
    fun importModel(uri: Uri) {
        useLocalMode()
        runModelJob("Copiando modelo al teléfono…") {
            engine.importModel(uri) { progress = it }
        }
    }

    fun downloadModel(url: String) {
        useLocalMode()
        runModelJob("Descargando modelo (solo esta vez)…") {
            engine.downloadModel(url.trim()) { progress = it }
        }
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
        if (t.isEmpty() || generating || !canChat) return
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
                val answer = askServer(snapshot).ifBlank { "…" }
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
