package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
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
    }

    var listener: Listener? = null

    private enum class Tab { RECIENTES, FAVORITOS, BUSQUEDA, CATEGORIA }
    private var currentTab = Tab.RECIENTES
    private var currentCategoryIndex = 0

    private val density = context.resources.displayMetrics.density
    private fun px(dp: Float) = (dp * density).toInt()

    private val searchInput = EditText(context)
    private val recycler = RecyclerView(context)
    private lateinit var adapter: EmojiAdapter
    private val tabRecientes = TextView(context)
    private val tabFavoritos = TextView(context)
    private val categoryButtons = mutableListOf<TextView>()

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#F5F5F5"))

        // Barra superior: lupa de búsqueda + cerrar
        val searchBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 8)
        }
        val lupa = android.widget.ImageView(context).apply {
            setImageResource(R.drawable.ic_search)
            setColorFilter(Color.parseColor("#888888"))
            layoutParams = LinearLayout.LayoutParams(48, 48).apply { marginEnd = 12 }
        }
        searchInput.apply {
            hint = "Buscar emoji…"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                    if (!s.isNullOrEmpty()) {
                        currentTab = Tab.BUSQUEDA
                        refreshContent()
                    } else {
                        currentTab = Tab.RECIENTES
                        refreshContent()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        val closeBtn = android.widget.ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor("#555555"))
            layoutParams = LinearLayout.LayoutParams(48, 48).apply { marginStart = 16 }
            setOnClickListener { listener?.onClose() }
        }
        searchBar.addView(lupa)
        searchBar.addView(searchInput)
        searchBar.addView(closeBtn)
        addView(searchBar)

        // Pestañas Recientes / Favoritos
        val tabBar = LinearLayout(context).apply { orientation = HORIZONTAL }
        tabRecientes.apply {
            text = "Recientes"; gravity = Gravity.CENTER; setPadding(0, 16, 0, 16)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentTab = Tab.RECIENTES; refreshContent() }
        }
        tabFavoritos.apply {
            text = "  Favoritos"; gravity = Gravity.CENTER; setPadding(0, 16, 0, 16)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_star, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentTab = Tab.FAVORITOS; refreshContent() }
        }
        tabBar.addView(tabRecientes)
        tabBar.addView(tabFavoritos)
        addView(tabBar)

        // Fila de categorías (siempre visible, deslizable horizontalmente)
        val categoryScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val categoryRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        EmojiData.categories.forEachIndexed { index, cat ->
            val btn = TextView(context).apply {
                text = cat.icon
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(px(14f), px(10f), px(14f), px(10f))
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

        recycler.layoutManager = GridLayoutManager(context, 8)
        recycler.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        addView(recycler)

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

    private fun refreshContent() {
        tabRecientes.setBackgroundColor(if (currentTab == Tab.RECIENTES) Color.parseColor("#E0E0FF") else Color.TRANSPARENT)
        tabFavoritos.setBackgroundColor(if (currentTab == Tab.FAVORITOS) Color.parseColor("#E0E0FF") else Color.TRANSPARENT)
        categoryButtons.forEachIndexed { i, btn ->
            val active = currentTab == Tab.CATEGORIA && i == currentCategoryIndex
            btn.setBackgroundColor(if (active) Color.parseColor("#E0E0FF") else Color.TRANSPARENT)
        }

        val list: List<String> = when (currentTab) {
            Tab.RECIENTES -> prefs.getRecentEmojis()
            Tab.FAVORITOS -> prefs.getFavoriteEmojis()
            Tab.BUSQUEDA -> EmojiData.search(searchInput.text.toString()).map { it.emoji }
            Tab.CATEGORIA -> EmojiData.categories[currentCategoryIndex].items.map { it.emoji }
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

        class VH(val frame: FrameLayout, val tv: TextView, val star: android.widget.ImageView) : RecyclerView.ViewHolder(frame)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val frame = FrameLayout(parent.context)
            val tv = TextView(parent.context).apply {
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val star = android.widget.ImageView(parent.context).apply {
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
