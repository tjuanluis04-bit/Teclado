package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EmojiPanelView(context: Context, private val prefs: Prefs) : LinearLayout(context) {

    interface Listener {
        fun onEmojiSelected(emoji: String)
        fun onClose()
        fun onEnterSearchMode(keyboardSlot: FrameLayout)
        fun onExitSearchMode()
        fun onDeleteLastEmoji()
    }

    var listener: Listener? = null

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
    private lateinit var adapter: EmojiAdapter
    private val tabBar = LinearLayout(context)
    private val tabRecientes = TextView(context)
    private val tabFavoritos = TextView(context)
    private val categoryScroll = HorizontalScrollView(context)
    private val categoryButtons = mutableListOf<ImageView>()
    private val keyboardSlot = FrameLayout(context)

    init {
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
            layoutParams = LinearLayout.LayoutParams(px(22f), px(22f)).apply { marginEnd = 12 }
        }
        searchQueryText.apply {
            text = "Buscar emoji…"
            setTextColor(Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        exitSearchIcon.apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(px(22f), px(22f)).apply { marginEnd = 16 }
            visibility = View.GONE
            setOnClickListener { exitSearch() }
        }
        val deleteBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_backspace)
            setColorFilter(Color.parseColor("#CCCCCC"))
            layoutParams = LinearLayout.LayoutParams(px(24f), px(24f)).apply { marginEnd = 16 }
            setOnClickListener { listener?.onDeleteLastEmoji() }
        }
        val closeBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor("#CCCCCC"))
            layoutParams = LinearLayout.LayoutParams(px(22f), px(22f))
            setOnClickListener { listener?.onClose() }
        }
        searchBar.addView(lupa)
        searchBar.addView(searchQueryText)
        searchBar.addView(exitSearchIcon)
        searchBar.addView(deleteBtn)
        searchBar.addView(closeBtn)
        searchBar.setOnClickListener { if (!searchMode) enterSearch() }
        addView(searchBar)

        // Pestañas Recientes / Favoritos
        tabRecientes.apply {
            text = "Recientes"; gravity = Gravity.CENTER; setPadding(0, 16, 0, 16)
            setTextColor(Color.parseColor("#EAEAEA"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentTab = Tab.RECIENTES; refreshContent() }
        }
        tabFavoritos.apply {
            text = "  Favoritos"; gravity = Gravity.CENTER; setPadding(0, 16, 0, 16)
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

        recycler.layoutManager = GridLayoutManager(context, 8)
        recycler.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        addView(recycler)

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
                layoutParams = LinearLayout.LayoutParams(px(44f), px(44f)).apply {
                    setMargins(px(4f), px(6f), px(4f), px(6f))
                }
                setPadding(px(8f), px(8f), px(8f), px(8f))
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
        listener?.onEnterSearchMode(keyboardSlot)
        refreshContent()
    }

    private fun exitSearch() {
        searchMode = false
        searchQuery.clear()
        currentTab = Tab.RECIENTES
        applySearchModeUi()
        listener?.onExitSearchMode()
        refreshContent()
    }

    private fun applySearchModeUi() {
        tabBar.visibility = if (searchMode) View.GONE else View.VISIBLE
        categoryScroll.visibility = if (searchMode) View.GONE else View.VISIBLE
        exitSearchIcon.visibility = if (searchMode) View.VISIBLE else View.GONE
        keyboardSlot.visibility = if (searchMode) View.VISIBLE else View.GONE
        recycler.layoutParams = if (searchMode) {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(110f))
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

        val list: List<String> = when (currentTab) {
            Tab.RECIENTES -> prefs.getRecentEmojis()
            Tab.FAVORITOS -> prefs.getFavoriteEmojis()
            Tab.BUSQUEDA -> EmojiDataFull.search(searchQuery.toString()).map { it.emoji }
            Tab.CATEGORIA -> EmojiDataFull.categories[currentCategoryIndex].items.map { it.emoji }
        }
        adapter.submit(list)
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
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val star = ImageView(parent.context).apply {
                setImageResource(R.drawable.ic_star)
                setColorFilter(Color.parseColor("#FFC107"))
                layoutParams = FrameLayout.LayoutParams(20, 20, Gravity.TOP or Gravity.END)
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
