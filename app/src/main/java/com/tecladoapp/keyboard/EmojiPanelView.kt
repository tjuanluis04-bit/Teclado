package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EmojiPanelView(context: Context, private val prefs: Prefs, private val inlineProvider: InlineKeyboardProvider) : LinearLayout(context), InlineInputHost {

    interface Listener {
        fun onEmojiSelected(emoji: String)
        fun onClose()
        fun onDeleteLastEmoji()
        fun onEmojiSpace()
        fun onEmojiEnter()
    }

    var listener: Listener? = null

    private companion object {
        val glyphPaint = android.graphics.Paint()
        val glyphCache = HashMap<String, Boolean>()
        /** true si la tipografía del teléfono sabe dibujar ese emoji (evita cuadritos rotos como 🫪). */
        fun supported(e: String): Boolean = glyphCache.getOrPut(e) { glyphPaint.hasGlyph(e) }

        private val toneCache = HashMap<String, String>()
        private val toneMods = intArrayOf(0, 0x1F3FB, 0x1F3FC, 0x1F3FD, 0x1F3FE, 0x1F3FF)

        /** Aplica el tono de piel (1..5) si el teléfono lo soporta; si no, deja el emoji base. */
        fun withTone(e: String, tone: Int): String = toneCache.getOrPut("$e#$tone") {
            val first = e.codePointAt(0)
            val firstLen = Character.charCount(first)
            var rest = e.substring(firstLen)
            if (rest.startsWith("\uFE0F")) rest = rest.substring(1)
            val cand = String(Character.toChars(first)) + String(Character.toChars(toneMods[tone])) + rest
            if (cand != e && glyphPaint.hasGlyph(cand)) cand else e
        }

        private val flagCache = HashMap<String, Long>()
        private val droppedTerritories = setOf("EA", "BV", "SJ", "UM", "CP", "HM")
        private fun flagCode(e: String): String? {
            val cps = e.codePoints().toArray()
            if (cps.size != 2 || cps.any { it < 0x1F1E6 || it > 0x1F1FF }) return null
            return String(charArrayOf((cps[0] - 0x1F1E6 + 'A'.code).toChar(), (cps[1] - 0x1F1E6 + 'A'.code).toChar()))
        }
        private fun flagHash(e: String): Long = flagCache.getOrPut(e) {
            val bmp = android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(bmp)
            val p = android.graphics.Paint().apply { textSize = 26f }
            c.drawText(e, 0f, 26f, p)
            val px = IntArray(32 * 32); bmp.getPixels(px, 0, 32, 0, 0, 32, 32)
            bmp.recycle()
            var h = 1125899906842597L
            for (v in px) h = 31 * h + v
            h
        }
        /** Quita banderas repetidas (territorios que se dibujan igual que su país). */
        fun dedupeFlags(list: List<String>): List<String> {
            val seen = HashSet<Long>()
            val res = ArrayList<String>()
            for (e in list) {
                val code = flagCode(e)
                if (code != null) {
                    if (code in droppedTerritories) continue
                    if (!seen.add(flagHash(e))) continue
                }
                res.add(e)
            }
            return res
        }
    }

    private enum class Tab { RECIENTES, FAVORITOS, BUSQUEDA, CATEGORIA }
    private var currentTab = Tab.RECIENTES
    private var currentCategoryIndex = 0
    private var searchMode = false
    private val searchQuery = StringBuilder()

    private val density = context.resources.displayMetrics.density
    private fun px(dp: Float) = (dp * density).toInt()

    private val searchQueryText = TextView(context)
    private val exitSearchIcon = ImageView(context)
    private val recycler = RecyclerView(context)
    private val recyclerHolder = FrameLayout(context)
    private lateinit var adapter: EmojiAdapter
    private val tabBar = LinearLayout(context)
    private val tabRecientes = TextView(context)
    private val tabFavoritos = TextView(context)
    private val categoryScroll = HorizontalScrollView(context)
    private val categoryButtons = mutableListOf<ImageView>()
    private val keyboardSlot = FrameLayout(context)
    private val toneRow = LinearLayout(context)
    private val toneDots = mutableListOf<View>()

    init {
        EmojiDataFull.loadSpanish(context)
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#161616"))

        // Barra superior: lupa/buscador + borrador (para borrar emoji ya insertados) + cerrar
        val searchBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 8)
        }
        val lupa = ImageView(context).apply {
            setImageResource(R.drawable.ic_search)
            setColorFilter(Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(px(30f), px(30f)).apply { marginEnd = 12 }
        }
        searchQueryText.apply {
            text = "Buscar emoji…"
            textSize = 18f
            setTextColor(Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        exitSearchIcon.apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(px(32f), px(32f)).apply { marginEnd = 16 }
            visibility = View.GONE
            setOnClickListener { exitSearch() }
        }
        val closeBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor("#CCCCCC"))
            layoutParams = LinearLayout.LayoutParams(px(32f), px(32f))
            setOnClickListener { listener?.onClose() }
        }
        searchBar.addView(lupa)
        searchBar.addView(searchQueryText)
        searchBar.addView(exitSearchIcon)
        searchBar.addView(closeBtn)
        searchBar.setOnClickListener { if (!searchMode) enterSearch() }
        addView(searchBar)

        // Pestañas Recientes / Favoritos
        tabRecientes.apply {
            text = "Recientes"; textSize = 17f; gravity = Gravity.CENTER; setPadding(0, 22, 0, 22)
            setTextColor(Color.parseColor("#EAEAEA"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentTab = Tab.RECIENTES; refreshContent() }
        }
        tabFavoritos.apply {
            text = "  Favoritos"; textSize = 17f; gravity = Gravity.CENTER; setPadding(0, 22, 0, 22)
            setTextColor(Color.parseColor("#EAEAEA"))
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_star, 0, 0, 0)
            compoundDrawableTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#EAEAEA"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentTab = Tab.FAVORITOS; refreshContent() }
        }
        tabBar.orientation = HORIZONTAL
        tabBar.addView(tabRecientes)
        tabBar.addView(tabFavoritos)
        addView(tabBar)

        recycler.layoutManager = GridLayoutManager(context, 7)
        recycler.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        recyclerHolder.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        recyclerHolder.addView(recycler)
        // Botones flotantes: espacio, salto de línea y borrador (no activan el teclado)
        fun floatBtn(icon: Int?, label: String?, onClick: () -> Unit, w: Float): View {
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#DD2A2A2A")); cornerRadius = px(14f).toFloat()
            }
            return if (icon != null) ImageView(context).apply {
                setImageResource(icon); setColorFilter(Color.WHITE)
                setPadding(px(12f), px(12f), px(12f), px(12f)); background = bg
                layoutParams = LinearLayout.LayoutParams(px(w), px(48f)).apply { marginStart = px(6f) }
                setOnClickListener { onClick() }
            } else TextView(context).apply {
                text = label; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER
                background = bg
                layoutParams = LinearLayout.LayoutParams(px(w), px(48f)).apply { marginStart = px(6f) }
                setOnClickListener { onClick() }
            }
        }
        val floatRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, px(10f), px(10f)) }
        }
        floatRow.addView(floatBtn(null, "␣", { listener?.onEmojiSpace() }, 72f))
        floatRow.addView(floatBtn(null, "↵", { listener?.onEmojiEnter() }, 52f))
        floatRow.addView(floatBtn(R.drawable.ic_backspace, null, { listener?.onDeleteLastEmoji() }, 56f))
        recyclerHolder.addView(floatRow)

        // Fila de tonos de piel (solo en la categoría Personas)
        toneRow.orientation = HORIZONTAL
        toneRow.gravity = Gravity.CENTER
        toneRow.visibility = View.GONE
        val toneColors = listOf("#FFCC33", "#F7DCC4", "#E8BE98", "#C69163", "#8E5B3A", "#4A2E1B")
        toneColors.forEachIndexed { i, hex ->
            val dot = View(context).apply {
                background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(Color.parseColor(hex)) }
                layoutParams = LinearLayout.LayoutParams(px(34f), px(34f)).apply { setMargins(px(8f), px(4f), px(8f), px(4f)) }
                setOnClickListener { prefs.skinTone = i; refreshContent() }
            }
            toneDots.add(dot); toneRow.addView(dot)
        }
        addView(toneRow, indexOfChild(tabBar) + 1)
        addView(recyclerHolder)

        // Fila de categorías con iconos propios (no emojis), scroll horizontal, abajo
        categoryScroll.isHorizontalScrollBarEnabled = false
        categoryScroll.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val categoryRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        val categoryIcons = listOf(
            R.drawable.ic_cat_smileys, R.drawable.ic_cat_people, R.drawable.ic_cat_animals,
            R.drawable.ic_cat_food, R.drawable.ic_cat_activities, R.drawable.ic_cat_travel,
            R.drawable.ic_cat_objects, R.drawable.ic_cat_symbols, R.drawable.ic_cat_flags
        )
        EmojiDataFull.categories.forEachIndexed { index, _ ->
            val btn = ImageView(context).apply {
                setImageResource(categoryIcons.getOrElse(index) { R.drawable.ic_cat_objects })
                setColorFilter(Color.parseColor("#999999"))
                layoutParams = LinearLayout.LayoutParams(px(56f), px(56f)).apply {
                    setMargins(px(4f), px(6f), px(4f), px(6f))
                }
                setPadding(px(11f), px(11f), px(11f), px(11f))
                setOnClickListener {
                    currentTab = Tab.CATEGORIA
                    currentCategoryIndex = index
                    refreshContent()
                }
            }
            categoryButtons.add(btn)
            categoryRow.addView(btn)
        }
        categoryScroll.addView(categoryRow)
        addView(categoryScroll)

        // Espacio donde se monta el teclado QWERTY real mientras se busca
        keyboardSlot.visibility = View.GONE
        addView(keyboardSlot)

        adapter = EmojiAdapter(
            onTap = { emoji ->
                prefs.addRecentEmoji(emoji)
                listener?.onEmojiSelected(emoji)
            },
            onLongPress = { emoji ->
                prefs.toggleFavoriteEmoji(emoji)
                refreshContent()
            },
            isFavorite = { prefs.isFavoriteEmoji(it) }
        )
        recycler.adapter = adapter

        refreshContent()
    }

    private fun enterSearch() {
        searchMode = true
        searchQuery.clear()
        currentTab = Tab.BUSQUEDA
        applySearchModeUi()
        inlineProvider.attachInlineKeyboard(keyboardSlot, this, extraHeightPx = px(190f))
        refreshContent()
    }

    private fun exitSearch() {
        searchMode = false
        searchQuery.clear()
        currentTab = Tab.RECIENTES
        applySearchModeUi()
        inlineProvider.detachInlineKeyboard()
        refreshContent()
    }

    private fun applySearchModeUi() {
        tabBar.visibility = if (searchMode) View.GONE else View.VISIBLE
        categoryScroll.visibility = if (searchMode) View.GONE else View.VISIBLE
        exitSearchIcon.visibility = if (searchMode) View.VISIBLE else View.GONE
        keyboardSlot.visibility = if (searchMode) View.VISIBLE else View.GONE
        recyclerHolder.layoutParams = if (searchMode) {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(130f))
        } else {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        updateSearchQueryText()
    }

    private fun updateSearchQueryText() {
        if (searchMode) {
            searchQueryText.text = if (searchQuery.isEmpty()) "Escribe para buscar…" else searchQuery.toString()
            searchQueryText.setTextColor(Color.parseColor("#EAEAEA"))
        } else {
            searchQueryText.text = "Buscar emoji…"
            searchQueryText.setTextColor(Color.parseColor("#999999"))
        }
    }

    /** Llamado por el servicio del teclado mientras el teclado real está montado en keyboardSlot. */
    fun appendSearchChar(c: Char) {
        searchQuery.append(c)
        updateSearchQueryText()
        refreshContent()
    }

    fun removeSearchChar() {
        if (searchQuery.isNotEmpty()) searchQuery.deleteCharAt(searchQuery.length - 1)
        updateSearchQueryText()
        refreshContent()
    }

    override fun onInlineChar(c: Char) { appendSearchChar(c) }
    override fun onInlineBackspace() { removeSearchChar() }
    override fun onInlineEnter() { closeSearchFromKeyboard() }

    fun isInSearchMode(): Boolean = searchMode

    fun closeSearchFromKeyboard() {
        exitSearch()
    }

    private fun refreshContent() {
        tabRecientes.setBackgroundColor(if (currentTab == Tab.RECIENTES) Color.parseColor("#33445577") else Color.TRANSPARENT)
        tabFavoritos.setBackgroundColor(if (currentTab == Tab.FAVORITOS) Color.parseColor("#33445577") else Color.TRANSPARENT)
        categoryButtons.forEachIndexed { i, btn ->
            val active = currentTab == Tab.CATEGORIA && i == currentCategoryIndex
            btn.setColorFilter(if (active) Color.parseColor("#6C8CFF") else Color.parseColor("#999999"))
        }

        val showTones = currentTab == Tab.CATEGORIA && currentCategoryIndex == 1
        toneRow.visibility = if (showTones) View.VISIBLE else View.GONE
        toneDots.forEachIndexed { i, d ->
            d.alpha = if (i == prefs.skinTone) 1f else 0.45f
            d.scaleX = if (i == prefs.skinTone) 1.2f else 1f; d.scaleY = d.scaleX
        }
        val list: List<String> = when (currentTab) {
            Tab.RECIENTES -> prefs.getRecentEmojis()
            Tab.FAVORITOS -> prefs.getFavoriteEmojis()
            Tab.BUSQUEDA -> EmojiDataFull.search(searchQuery.toString()).map { it.emoji }
            Tab.CATEGORIA -> EmojiDataFull.categories[currentCategoryIndex].items.map { it.emoji }
        }
        var out = list.filter { supported(it) }
        if (currentTab == Tab.CATEGORIA && currentCategoryIndex == 8) out = dedupeFlags(out)
        if (showTones && prefs.skinTone > 0) out = out.map { withTone(it, prefs.skinTone) }
        adapter.submit(out)
    }

    private class EmojiAdapter(
        val onTap: (String) -> Unit,
        val onLongPress: (String) -> Unit,
        val isFavorite: (String) -> Boolean
    ) : RecyclerView.Adapter<EmojiAdapter.VH>() {

        private var items: List<String> = emptyList()
        fun submit(list: List<String>) { items = list; notifyDataSetChanged() }

        class VH(val frame: FrameLayout, val tv: TextView, val star: ImageView) : RecyclerView.ViewHolder(frame)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val frame = FrameLayout(parent.context)
            val tv = TextView(parent.context).apply {
                textSize = 30f
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val star = ImageView(parent.context).apply {
                setImageResource(R.drawable.ic_star)
                setColorFilter(Color.parseColor("#FFC107"))
                layoutParams = FrameLayout.LayoutParams(28, 28, Gravity.TOP or Gravity.END)
                visibility = View.GONE
            }
            frame.addView(tv)
            frame.addView(star)
            frame.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            return VH(frame, tv, star)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val emoji = items[position]
            holder.tv.text = emoji
            holder.star.visibility = if (isFavorite(emoji)) View.VISIBLE else View.GONE
            holder.tv.setOnClickListener { onTap(emoji) }
            holder.tv.setOnLongClickListener { onLongPress(emoji); true }
        }

        override fun getItemCount() = items.size
    }
}
