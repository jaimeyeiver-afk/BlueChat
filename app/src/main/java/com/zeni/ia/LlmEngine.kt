package com.zeni.ia

import android.content.Context
import android.net.Uri
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Motor de IA que corre 100% dentro del teléfono con un modelo .task / .bin (MediaPipe). */
class LlmEngine(private val context: Context) {

    private var llm: LlmInference? = null
    private val modelFile get() = File(context.filesDir, "model.task")

    fun hasModel(): Boolean = modelFile.exists() && modelFile.length() > 1_000_000

    /** Copia el modelo elegido por el usuario al almacenamiento interno de la app. */
    suspend fun importModel(uri: Uri, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val total = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        val tmp = File(context.filesDir, "model.tmp")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            tmp.outputStream().use { out -> copy(input, out, total, onProgress) }
        }
        finalize(tmp)
    }

    /** Descarga el modelo UNA sola vez. Después ya no hace falta internet. */
    suspend fun downloadModel(url: String, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true
        if (conn.responseCode !in 200..299) error("Error ${conn.responseCode} al descargar")
        val tmp = File(context.filesDir, "model.tmp")
        conn.inputStream.use { input ->
            tmp.outputStream().use { out -> copy(input, out, conn.contentLengthLong, onProgress) }
        }
        finalize(tmp)
    }

    private fun copy(
        input: java.io.InputStream,
        out: java.io.OutputStream,
        total: Long,
        onProgress: (Float) -> Unit
    ) {
        val buf = ByteArray(1 shl 16)
        var done = 0L
        var lastPct = -1
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            done += n
            if (total > 0) {
                val pct = (done * 100 / total).toInt()
                if (pct != lastPct) { lastPct = pct; onProgress(done.toFloat() / total) }
            }
        }
    }

    private fun finalize(tmp: File) {
        llm?.close(); llm = null
        if (modelFile.exists()) modelFile.delete()
        if (!tmp.renameTo(modelFile)) error("No se pudo guardar el modelo")
    }

    suspend fun load() = withContext(Dispatchers.IO) {
        if (llm != null) return@withContext
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(1024)
            .build()
        llm = LlmInference.createFromOptions(context, options)
    }

    suspend fun generate(history: List<Msg>): String = withContext(Dispatchers.Default) {
        val engine = llm ?: error("El modelo no está cargado")
        engine.generateResponse(buildPrompt(history)).trim()
    }

    fun deleteModel() {
        llm?.close(); llm = null
        modelFile.delete()
    }

    /** Formato de chat estilo Gemma (el más común para modelos .task). */
    private fun buildPrompt(history: List<Msg>): String {
        val sb = StringBuilder()
        // Solo los últimos mensajes para no pasarse del contexto del modelo.
        var budget = 3000
        val picked = ArrayList<Msg>()
        for (m in history.asReversed()) {
            if (budget - m.text.length < 0 && picked.isNotEmpty()) break
            budget -= m.text.length
            picked.add(0, m)
        }
        picked.forEach { m ->
            val r = if (m.role == "user") "user" else "model"
            sb.append("<start_of_turn>").append(r).append('\n')
                .append(m.text).append("<end_of_turn>\n")
        }
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }
}
