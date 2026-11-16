package com.zeni.ia

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Msg(val role: String, val text: String) // role: "user" | "assistant"

data class Conv(val id: Long, val title: String, val messages: List<Msg>)

/** Guarda las conversaciones en el teléfono (sin internet, sin nube). */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("zeni_store", Context.MODE_PRIVATE)

    fun load(): List<Conv> {
        val raw = prefs.getString("convs", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ms = o.getJSONArray("m")
                Conv(
                    o.getLong("id"),
                    o.getString("t"),
                    (0 until ms.length()).map { j ->
                        val m = ms.getJSONObject(j)
                        Msg(m.getString("r"), m.getString("x"))
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(convs: List<Conv>) {
        val arr = JSONArray()
        convs.forEach { c ->
            val ms = JSONArray()
            c.messages.forEach { ms.put(JSONObject().put("r", it.role).put("x", it.text)) }
            arr.put(JSONObject().put("id", c.id).put("t", c.title).put("m", ms))
        }
        prefs.edit().putString("convs", arr.toString()).apply()
    }
}
