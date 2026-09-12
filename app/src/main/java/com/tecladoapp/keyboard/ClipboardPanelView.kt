package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ClipboardPanelView(context: Context, private val prefs: Prefs) : LinearLayout(context) {

    interface Listener {
        fun onPaste(text: String)
        fun onClose()
    }

    var listener: Listener? = null

    private enum class Tab { RECIENTES, CATEGORIAS }
    private var currentTab = Tab.RECIENTES
    private var openCategory: ClipCategory? = null // null = mostrando lista de categorías

    private val bgColor = Color.parseColor("#161616")
    private val textColor = Color.parseColor("#EAEAEA")
    private val hintColor = Color.parseColor("#888888")
    private val activeTabColor = Color.parseColor("#33445577")

    private val tabRecientes = TextView(context)
    private val tabCategorias = TextView(context)
    private val contentContainer = FrameLayout(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(bgColor)

        val topBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 4)
        }
        val title = TextView(context).apply {
            text = "Portapapeles"; textSize = 15f
            setTextColor(textColor)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor("#CCCCCC"))
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setOnClickListener { listener?.onClose() }
        }
        topBar.addView(title)
        topBar.addView(closeBtn)
        addView(topBar)

        val tabBar = LinearLayout(context).apply { orientation = HORIZONTAL }
        tabRecientes.apply {
            text = "Recientes"; gravity = Gravity.CENTER; setPadding(0, 16, 0, 16)
            setTextColor(textColor)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentTab = Tab.RECIENTES; openCategory = null; render() }
        }
        tabCategorias.apply {
            text = "Categorías"; gravity = Gravity.CENTER; setPadding(0, 16, 0, 16)
            setTextColor(textColor)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentTab = Tab.CATEGORIAS; openCategory = null; render() }
        }
        tabBar.addView(tabRecientes)
        tabBar.addView(tabCategorias)
        addView(tabBar)

        contentContainer.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        addView(contentContainer)

        render()
    }

    private fun render() {
        tabRecientes.setBackgroundColor(if (currentTab == Tab.RECIENTES) activeTabColor else Color.TRANSPARENT)
        tabCategorias.setBackgroundColor(if (currentTab == Tab.CATEGORIAS) activeTabColor else Color.TRANSPARENT)
        contentContainer.removeAllViews()
        when (currentTab) {
            Tab.RECIENTES -> contentContainer.addView(buildRecientesView())
            Tab.CATEGORIAS -> {
                val cat = openCategory
                contentContainer.addView(if (cat == null) buildCategoriasListView() else buildCategoryItemsView(cat))
            }
        }
    }

    // ---------- Pestaña Recientes ----------
    private fun buildRecientesView(): RecyclerView {
        val recycler = RecyclerView(context)
        recycler.layoutManager = LinearLayoutManager(context)
        val items = prefs.getRecentTabItems()
        val adapter = ClipAdapter(
            items = items,
            onTap = { listener?.onPaste(it.text) },
            onCategorize = { item -> showCategoryPicker(item) },
            onDelete = { item -> prefs.deleteClip(item.id); render() },
            onEdit = null,
            showEditInsteadOfCategorize = false
        )
        recycler.adapter = adapter
        return recycler
    }

    private fun showCategoryPicker(item: ClipItem) {
        val categories = prefs.getCategories()
        val popup = PopupContainer(context)
        val list = ListView(context)
        val names = categories.map { it.name }.toMutableList()
        names.add("+ Nueva categoría")
        val listAdapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, names)
        list.adapter = listAdapter
        list.setOnItemClickListener { _, _, pos, _ ->
            if (pos == categories.size) {
                promptNewCategoryName { name ->
                    val cat = prefs.addCategory(name)
                    prefs.setClipCategory(item.id, cat.id)
                    render()
                }
            } else {
                prefs.setClipCategory(item.id, categories[pos].id)
                render()
            }
            popup.dismiss()
        }
        popup.show(list, "Categorizar en…", this)
    }

    private fun promptNewCategoryName(onCreated: (String) -> Unit) {
        val input = EditText(context).apply {
            setTextColor(textColor)
            setHintTextColor(hintColor)
            hint = "Nombre de la categoría"
        }
        val popup = PopupContainer(context)
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(24, 24, 24, 24)
        }
        container.addView(input)
        val btn = Button(context).apply {
            text = "Crear"
            setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) { onCreated(name); popup.dismiss() }
            }
        }
        container.addView(btn)
        popup.show(container, "Nueva categoría", this)
    }

    // ---------- Pestaña Categorías: lista ----------
    private fun buildCategoriasListView(): LinearLayout {
        val root = LinearLayout(context).apply { orientation = VERTICAL }

        val searchBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
        }
        val lupa = ImageView(context).apply {
            setImageResource(R.drawable.ic_search)
            setColorFilter(hintColor)
            layoutParams = LinearLayout.LayoutParams(44, 44).apply { marginEnd = 12 }
        }
        val search = EditText(context).apply {
            hint = "Buscar categoría…"
            setTextColor(textColor)
            setHintTextColor(hintColor)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        searchBar.addView(lupa)
        searchBar.addView(search)
        root.addView(searchBar)

        val listContainer = LinearLayout(context).apply { orientation = VERTICAL }
        val scroller = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        scroller.addView(listContainer)
        root.addView(scroller)

        fun populate(filter: String) {
            listContainer.removeAllViews()
            val cats = prefs.getCategories().filter { it.name.contains(filter, ignoreCase = true) }
            cats.forEach { cat ->
                val row = TextView(context).apply {
                    text = "  ${cat.name}"
                    textSize = 15f
                    setTextColor(textColor)
                    setPadding(24, 28, 24, 28)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0)
                    compoundDrawablePadding = 20
                    compoundDrawableTintList = android.content.res.ColorStateList.valueOf(hintColor)
                    setOnClickListener { openCategory = cat; render() }
                }
                listContainer.addView(row)
            }
            val addRow = TextView(context).apply {
                text = "  Crear categoría"
                textSize = 15f
                setPadding(24, 28, 24, 28)
                setTextColor(Color.parseColor("#6C8CFF"))
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add, 0, 0, 0)
                compoundDrawablePadding = 20
                compoundDrawableTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#6C8CFF"))
                setOnClickListener { promptNewCategoryName { render() } }
            }
            listContainer.addView(addRow)
        }
        populate("")
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { populate(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })

        return root
    }

    // ---------- Pestaña Categorías: dentro de una categoría (reordenar + editar) ----------
    private fun buildCategoryItemsView(category: ClipCategory): LinearLayout {
        val root = LinearLayout(context).apply { orientation = VERTICAL }

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
        }
        val backIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(Color.parseColor("#CCCCCC"))
            layoutParams = LinearLayout.LayoutParams(44, 44).apply { marginEnd = 12 }
            setOnClickListener { openCategory = null; render() }
        }
        val back = TextView(context).apply {
            text = category.name; textSize = 15f
            setTextColor(textColor)
            setOnClickListener { openCategory = null; render() }
        }
        header.addView(backIcon)
        header.addView(back)
        root.addView(header)

        val recycler = RecyclerView(context)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(recycler)

        val items = prefs.getClipsForCategory(category.id).toMutableList()
        val adapter = ClipAdapter(
            items = items,
            onTap = { listener?.onPaste(it.text) },
            onCategorize = null,
            onDelete = { item -> prefs.deleteClip(item.id); render() },
            onEdit = { item -> promptEditText(item) },
            showEditInsteadOfCategorize = true
        )
        recycler.adapter = adapter

        // Arrastrar para reordenar dentro de la categoría
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = vh.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from < 0 || to < 0) return false
                val item = items.removeAt(from)
                items.add(to, item)
                adapter.notifyItemMoved(from, to)
                prefs.reorderClipsInCategory(category.id, items.map { it.id })
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
        })
        touchHelper.attachToRecyclerView(recycler)

        return root
    }

    private fun promptEditText(item: ClipItem) {
        val input = EditText(context).apply {
            setText(item.text)
            setTextColor(textColor)
            setHintTextColor(hintColor)
        }
        val popup = PopupContainer(context)
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(24, 24, 24, 24)
        }
        container.addView(input)
        val btn = Button(context).apply {
            text = "Guardar"
            setOnClickListener {
                prefs.updateClipText(item.id, input.text.toString())
                popup.dismiss()
                render()
            }
        }
        container.addView(btn)
        popup.show(container, "Editar", this)
    }

    // ---------- Adapter compartido ----------
    private inner class ClipAdapter(
        val items: List<ClipItem>,
        val onTap: (ClipItem) -> Unit,
        val onCategorize: ((ClipItem) -> Unit)?,
        val onDelete: (ClipItem) -> Unit,
        val onEdit: ((ClipItem) -> Unit)?,
        val showEditInsteadOfCategorize: Boolean
    ) : RecyclerView.Adapter<ClipAdapter.VH>() {

        inner class VH(val row: LinearLayout, val text: TextView, val actions: LinearLayout) : RecyclerView.ViewHolder(row)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val row = LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(20, 20, 20, 20)
            }
            val text = TextView(context).apply { textSize = 14f; maxLines = 3; setTextColor(textColor) }
            val actions = LinearLayout(context).apply {
                orientation = HORIZONTAL
                visibility = ViewGroup.GONE
            }
            row.addView(text)
            row.addView(actions)
            return VH(row, text, actions)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.text.text = item.text
            holder.actions.removeAllViews()

            val secondIcon = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 12, 32, 0)
                val img = ImageView(context).apply {
                    setImageResource(if (showEditInsteadOfCategorize) R.drawable.ic_edit else R.drawable.ic_tag)
                    setColorFilter(Color.parseColor("#6C8CFF"))
                    layoutParams = LinearLayout.LayoutParams(36, 36).apply { marginEnd = 8 }
                }
                val label = TextView(context).apply {
                    text = if (showEditInsteadOfCategorize) "Editar" else "Categorizar"
                    setTextColor(Color.parseColor("#6C8CFF"))
                }
                addView(img)
                addView(label)
                setOnClickListener {
                    if (showEditInsteadOfCategorize) onEdit?.invoke(item) else onCategorize?.invoke(item)
                    holder.actions.visibility = ViewGroup.GONE
                }
            }
            val deleteIcon = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 12, 0, 0)
                val img = ImageView(context).apply {
                    setImageResource(R.drawable.ic_delete)
                    setColorFilter(Color.parseColor("#FF6B6B"))
                    layoutParams = LinearLayout.LayoutParams(36, 36).apply { marginEnd = 8 }
                }
                val label = TextView(context).apply {
                    text = "Borrar"
                    setTextColor(Color.parseColor("#FF6B6B"))
                }
                addView(img)
                addView(label)
                setOnClickListener { onDelete(item) }
            }
            holder.actions.addView(secondIcon)
            holder.actions.addView(deleteIcon)

            holder.row.setOnClickListener {
                if (holder.actions.visibility == ViewGroup.VISIBLE) onTap(item) else onTap(item)
            }
            holder.row.setOnLongClickListener {
                holder.actions.visibility = if (holder.actions.visibility == ViewGroup.VISIBLE) ViewGroup.GONE else ViewGroup.VISIBLE
                true
            }
        }

        override fun getItemCount() = items.size
    }
}
