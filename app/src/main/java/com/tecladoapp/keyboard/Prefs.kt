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

    /** Altura del teclado, 0.9 - 1.6 (multiplicador). Por defecto: versión grande. */
    var keyboardHeightScale: Float
        get() = sp.getFloat("kb_height_scale_v2", 1.2f)
        set(v) = sp.edit().putFloat("kb_height_scale_v2", v).apply()

    var spellCheckEnabled: Boolean
        get() = sp.getBoolean("spell_check_enabled", true)
        set(v) = sp.edit().putBoolean("spell_check_enabled", v).apply()

    var wordSuggestionsEnabled: Boolean
        get() = sp.getBoolean("word_suggestions_enabled", true)
        set(v) = sp.edit().putBoolean("word_suggestions_enabled", v).apply()

    var skinTone: Int
        get() = sp.getInt("skin_tone", 0)
        set(v) = sp.edit().putInt("skin_tone", v).apply()

    // ---------- Portapapeles ----------
    companion object {
        const val MAX_RECENT_CLIPS = 20
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
                    categoryId = if (o.has("cat") && !o.isNull("cat")) o.getString("cat") else null,
                    manual = o.optBoolean("man", false)
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
            o.put("man", it.manual)
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
        val uncategorized = list.filter { it.categoryId == null && !it.manual }
        if (uncategorized.size > MAX_RECENT_CLIPS) {
            // eliminar el más viejo sin categoría
            val oldest = uncategorized.minByOrNull { it.timestamp }
            if (oldest != null) list.remove(oldest)
        }
        saveRecentClips(list)
    }

    /** Texto escrito a mano dentro de una categoría: se queda ahí y no aparece en Recientes. */
    fun addClipToCategory(text: String, categoryId: String) {
        if (text.isBlank()) return
        val list = getRecentClips()
        list.add(ClipItem(UUID.randomUUID().toString(), text, System.currentTimeMillis(), categoryId, manual = true))
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
            list[idx] = list[idx].copy(categoryId = categoryId, manual = if (categoryId == null) false else list[idx].manual)
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
        return getRecentClips().filter { !it.manual }.sortedByDescending { it.timestamp }.take(MAX_RECENT_CLIPS)
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
        val clips = getRecentClips().map { if (it.categoryId == id) it.copy(categoryId = null, manual = false) else it }
        saveRecentClips(clips)
    }

    /** Último texto copiado que aún no se ha ofrecido/pegado desde la barra de sugerencias. */
    var pendingClip: String?
        get() = sp.getString("pending_clip", null)
        set(v) { if (v == null) sp.edit().remove("pending_clip").apply() else sp.edit().putString("pending_clip", v).apply() }

    // ---------- Términos / definiciones / analogías ----------
    fun getTerms(): MutableList<TermEntry> {
        val arr = JSONArray(sp.getString("terms", "[]") ?: "[]")
        return MutableList(arr.length()) {
            val o = arr.getJSONObject(it)
            TermEntry(o.getString("id"), o.optString("cat"), o.optString("term"), o.optString("def"), o.optString("ana"))
        }
    }
    fun saveTerms(list: List<TermEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("cat", it.category).put("term", it.term).put("def", it.definition).put("ana", it.analogy))
        }
        sp.edit().putString("terms", arr.toString()).apply()
    }
    fun addTerm(e: TermEntry) { val l = getTerms(); l.add(0, e); saveTerms(l) }
    fun deleteTerm(id: String) = saveTerms(getTerms().filterNot { it.id == id })

    // ---------- Palabras usadas (predicción) ----------
    fun getWordFreq(): MutableMap<String, Int> {
        val o = JSONObject(sp.getString("word_freq", "{}") ?: "{}")
        val m = mutableMapOf<String, Int>()
        o.keys().forEach { m[it] = o.getInt(it) }
        return m
    }
    fun saveWordFreq(m: Map<String, Int>) {
        val o = JSONObject()
        // conservar solo las 1500 más usadas
        m.entries.sortedByDescending { it.value }.take(1500).forEach { o.put(it.key, it.value) }
        sp.edit().putString("word_freq", o.toString()).apply()
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
