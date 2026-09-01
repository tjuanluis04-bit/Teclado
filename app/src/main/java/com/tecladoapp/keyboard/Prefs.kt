package com.tecladoapp.keyboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("teclado_app_prefs", Context.MODE_PRIVATE)

    // ---------- Ajustes generales ----------
    var themeId: String
        get() = sp.getString("theme_id", "clasico_azul") ?: "clasico_azul"
        set(v) = sp.edit().putString("theme_id", v).apply()

    var fontId: String
        get() = sp.getString("font_id", "sistema") ?: "sistema"
        set(v) = sp.edit().putString("font_id", v).apply()

    /** Altura del teclado, 0.8 - 1.4 (multiplicador sobre la altura base) */
    var keyboardHeightScale: Float
        get() = sp.getFloat("kb_height_scale", 1.0f)
        set(v) = sp.edit().putFloat("kb_height_scale", v).apply()

    var spellCheckEnabled: Boolean
        get() = sp.getBoolean("spell_check_enabled", true)
        set(v) = sp.edit().putBoolean("spell_check_enabled", v).apply()

    var wordSuggestionsEnabled: Boolean
        get() = sp.getBoolean("word_suggestions_enabled", true)
        set(v) = sp.edit().putBoolean("word_suggestions_enabled", v).apply()

    // ---------- Portapapeles ----------
    companion object {
        const val MAX_RECENT_CLIPS = 10
    }

    fun getRecentClips(): MutableList<ClipItem> {
        val raw = sp.getString("clips_recent", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<ClipItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                ClipItem(
                    id = o.getString("id"),
                    text = o.getString("text"),
                    timestamp = o.getLong("ts"),
                    categoryId = if (o.has("cat") && !o.isNull("cat")) o.getString("cat") else null
                )
            )
        }
        return list
    }

    private fun saveRecentClips(list: List<ClipItem>) {
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("text", it.text)
            o.put("ts", it.timestamp)
            o.put("cat", it.categoryId)
            arr.put(o)
        }
        sp.edit().putString("clips_recent", arr.toString()).apply()
    }

    /** Añade un nuevo texto copiado. Si supera MAX_RECENT_CLIPS, elimina el más viejo
     *  que NO esté categorizado (los categorizados persisten en su categoría). */
    fun addNewClip(text: String) {
        if (text.isBlank()) return
        val list = getRecentClips()
        if (list.isNotEmpty() && list.first().text == text) return // evitar duplicado inmediato

        val newItem = ClipItem(id = UUID.randomUUID().toString(), text = text, timestamp = System.currentTimeMillis())
        list.add(0, newItem)

        // Contamos cuántos "slots" de recientes (sin categorizar) hay
        val uncategorized = list.filter { it.categoryId == null }
        if (uncategorized.size > MAX_RECENT_CLIPS) {
            // eliminar el más viejo sin categoría
            val oldest = uncategorized.minByOrNull { it.timestamp }
            if (oldest != null) list.remove(oldest)
        }
        saveRecentClips(list)
    }

    fun updateClipText(id: String, newText: String) {
        val list = getRecentClips()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(text = newText)
            saveRecentClips(list)
        }
    }

    fun deleteClip(id: String) {
        val list = getRecentClips().filterNot { it.id == id }
        saveRecentClips(list)
    }

    fun setClipCategory(id: String, categoryId: String?) {
        val list = getRecentClips()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(categoryId = categoryId)
            saveRecentClips(list)
        }
    }

    fun reorderClipsInCategory(categoryId: String, orderedIds: List<String>) {
        val list = getRecentClips()
        val map = list.associateBy { it.id }.toMutableMap()
        // Reconstruir preservando orden: primero los reordenados de la categoría, luego el resto
        val reordered = orderedIds.mapNotNull { map[it] }
        val others = list.filterNot { it.id in orderedIds }
        saveRecentClips(reordered + others)
    }

    /** Devuelve las tarjetas "recientes" mostradas en la pestaña Recientes:
     *  las últimas MAX_RECENT_CLIPS, incluyan o no categoría (una vez categorizadas
     *  siguen mostrándose ahí también si siguen entre las últimas 10; si no, solo aparecen
     *  en su categoría). */
    fun getRecentTabItems(): List<ClipItem> {
        return getRecentClips().sortedByDescending { it.timestamp }.take(MAX_RECENT_CLIPS)
    }

    fun getClipsForCategory(categoryId: String): List<ClipItem> {
        return getRecentClips().filter { it.categoryId == categoryId }
    }

    // ---------- Categorías ----------
    fun getCategories(): MutableList<ClipCategory> {
        val raw = sp.getString("clip_categories", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<ClipCategory>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(ClipCategory(o.getString("id"), o.getString("name")))
        }
        return list
    }

    fun saveCategories(list: List<ClipCategory>) {
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("name", it.name)
            arr.put(o)
        }
        sp.edit().putString("clip_categories", arr.toString()).apply()
    }

    fun addCategory(name: String): ClipCategory {
        val list = getCategories()
        val cat = ClipCategory(UUID.randomUUID().toString(), name)
        list.add(cat)
        saveCategories(list)
        return cat
    }

    fun renameCategory(id: String, newName: String) {
        val list = getCategories()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx].name = newName
            saveCategories(list)
        }
    }

    fun deleteCategory(id: String) {
        saveCategories(getCategories().filterNot { it.id == id })
        // los clips de esa categoría vuelven a quedar sin categoría
        val clips = getRecentClips().map { if (it.categoryId == id) it.copy(categoryId = null) else it }
        saveRecentClips(clips)
    }

    // ---------- Emojis ----------
    fun getRecentEmojis(): MutableList<String> {
        val raw = sp.getString("emoji_recent", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return MutableList(arr.length()) { arr.getString(it) }
    }

    fun addRecentEmoji(emoji: String) {
        val list = getRecentEmojis()
        list.remove(emoji)
        list.add(0, emoji)
        while (list.size > 30) list.removeAt(list.size - 1)
        sp.edit().putString("emoji_recent", JSONArray(list).toString()).apply()
    }

    fun getFavoriteEmojis(): MutableList<String> {
        val raw = sp.getString("emoji_favorites", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return MutableList(arr.length()) { arr.getString(it) }
    }

    fun toggleFavoriteEmoji(emoji: String) {
        val list = getFavoriteEmojis()
        if (list.contains(emoji)) list.remove(emoji) else list.add(0, emoji)
        sp.edit().putString("emoji_favorites", JSONArray(list).toString()).apply()
    }

    fun isFavoriteEmoji(emoji: String) = getFavoriteEmojis().contains(emoji)
}
