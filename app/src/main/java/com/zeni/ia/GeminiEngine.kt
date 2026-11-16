package com.zeni.ia

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/** Motor de IA en línea: Gemini (Developer API) a través de Firebase AI Logic. No usa ninguna clave dentro de la app. */
class GeminiEngine(private val context: Context) {

    companion object {
        /** Modelo de Gemini. Si Google lo retira, solo hay que cambiar este nombre. */
        const val MODEL_NAME = "gemini-3.5-flash"

        private const val SYSTEM_PROMPT =
            "Eres Zeni IA, un asistente amable, claro y útil. Responde siempre en el mismo idioma del usuario."

        /** Máximo de mensajes anteriores que se envían como contexto. */
        private const val MAX_MESSAGES = 20
    }

    private val model: GenerativeModel by lazy {
        check(FirebaseApp.getApps(context).isNotEmpty()) {
            "Firebase no está configurado (falta google-services.json)."
        }
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content { text(SYSTEM_PROMPT) }
        )
    }

    /** Devuelve la respuesta de Gemini en trozos de texto, a medida que llegan. */
    fun stream(history: List<Msg>): Flow<String> {
        val contents = toContents(history)
        return model.generateContentStream(*contents.toTypedArray()).mapNotNull { it.text }
    }

    /**
     * Gemini exige que la conversación empiece con un mensaje del usuario y que los turnos se alternen.
     * Aquí se quitan los avisos de error guardados y se unen mensajes seguidos del mismo autor.
     */
    private fun toContents(history: List<Msg>): List<Content> {
        val clean = history
            .filterNot { it.role != "user" && it.text.startsWith("⚠️") }
            .filter { it.text.isNotBlank() }
            .takeLast(MAX_MESSAGES)

        val merged = ArrayList<Msg>()
        for (m in clean) {
            val last = merged.lastOrNull()
            if (last != null && last.role == m.role) {
                merged[merged.lastIndex] = Msg(m.role, last.text + "\n\n" + m.text)
            } else {
                merged.add(m)
            }
        }
        while (merged.isNotEmpty() && merged.first().role != "user") merged.removeAt(0)

        return merged.map { m ->
            content(role = if (m.role == "user") "user" else "model") { text(m.text) }
        }
    }
}
