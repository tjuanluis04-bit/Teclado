package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
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

    private enum class Tab { RECIENTES, FAVORITOS, BUSQUEDA }
    private var currentTab = Tab.RECIENTES

    private val searchInput = EditText(context)
    private val recycler = RecyclerView(context)
    private lateinit var adapter: EmojiAdapter
    private val tabRecientes = TextView(context)
    private val tabFavoritos = TextView(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#F5F5F5"))

        // Barra superior: lupa de búsqueda + cerrar
        val searchBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 8)
        }
        val lupa = TextView(context).apply { text = "🔍"; textSize = 16f; setPadding(0, 0, 12, 0) }
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
        val closeBtn = TextView(context).apply {
            text = "✕"; textSize = 16f; setPadding(16, 0, 0, 0)
            setOnClickListener { listener?.onClose() }
        }
        searchBar.addView(lupa)
        searchBar.addView(searchInput)
        searchBar.addView(closeBtn)
        addView(searchBar)

        // Pestañas
        val tabBar = LinearLayout(context).apply { orientation = HORIZONTAL }
        tabRecientes.apply {
            text = "Recientes"; gravity = Gravity.CENTER; setPadding(0, 16, 0, 16)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentTab = Tab.RECIENTES; refreshContent() }
        }
        tabFavoritos.apply {
            text = "Favoritos ⭐"; gravity = Gravity.CENTER; setPadding(0, 16, 0, 16)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentTab = Tab.FAVORITOS; refreshContent() }
        }
        tabBar.addView(tabRecientes)
        tabBar.addView(tabFavoritos)
        addView(tabBar)

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

        val list: List<String> = when (currentTab) {
            Tab.RECIENTES -> prefs.getRecentEmojis()
            Tab.FAVORITOS -> prefs.getFavoriteEmojis()
            Tab.BUSQUEDA -> EmojiData.search(searchInput.text.toString()).map { it.emoji }
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

        class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val emoji = items[position]
            holder.tv.text = emoji
            holder.tv.setOnClickListener { onTap(emoji) }
            holder.tv.setOnLongClickListener { onLongPress(emoji); true }
        }

        override fun getItemCount() = items.size
    }
}
