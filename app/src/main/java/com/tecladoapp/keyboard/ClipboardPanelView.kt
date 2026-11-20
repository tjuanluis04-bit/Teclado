package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ClipboardPanelView(context: Context, private val prefs: Prefs, private val inlineProvider: InlineKeyboardProvider) : LinearLayout(context) {

    private var searchAttached = false
    private fun releaseSearchKeyboard() {
        if (searchAttached) { inlineProvider.detachInlineKeyboard(); searchAttached = false }
    }

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
            text = "Portapapeles"; textSize = 18f
            setTextColor(textColor)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor("#CCCCCC"))
            layoutParams = LinearLayout.LayoutParams(72, 72)
            setOnClickListener { listener?.onClose() }
        }
        topBar.addView(title)
        topBar.addView(closeBtn)
        addView(topBar)

        val tabBar = LinearLayout(context).apply { orientation = HORIZONTAL }
        tabRecientes.apply {
            text = "Recientes"; gravity = Gravity.CENTER; setPadding(0, 24, 0, 24)
            setTextColor(textColor)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentTab = Tab.RECIENTES; openCategory = null; render() }
        }
        tabCategorias.apply {
            text = "Categorías"; gravity = Gravity.CENTER; setPadding(0, 24, 0, 24)
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

    private fun addDivider(rv: RecyclerView) {
        val d = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#3C3C3C")); setSize(1, (resources.displayMetrics.density * 1.5f).toInt().coerceAtLeast(2))
        }
        rv.addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(context, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL).apply { setDrawable(d) })
    }

    private fun smallIcon(res: Int, tint: String, onClick: () -> Unit): ImageView {
        val sz = (resources.displayMetrics.density * 40f).toInt()
        return ImageView(context).apply {
            setImageResource(res); setColorFilter(Color.parseColor(tint))
            val pad = (resources.displayMetrics.density * 8f).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(sz, sz)
            setOnClickListener { onClick() }
        }
    }

    private fun confirmDeleteCategory(cat: ClipCategory) {
        val popup = PopupContainer(context)
        val box = LinearLayout(context).apply { orientation = VERTICAL; setPadding(8, 8, 8, 8) }
        box.addView(TextView(context).apply {
            text = "Los textos de «${cat.name}» no se borran: pasan a Recientes."
            textSize = 16f; setTextColor(Color.parseColor("#CCCCCC")); setPadding(0, 0, 0, 20)
        })
        box.addView(Button(context).apply {
            text = "Eliminar categoría"; textSize = 17f
            setOnClickListener {
                popup.dismiss()
                // los textos escritos a mano pasan a ser recientes normales
                prefs.deleteCategory(cat.id)
                render()
            }
        })
        popup.show(box, "¿Eliminar categoría?", this)
    }

    private fun render() {
        releaseSearchKeyboard()
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
        addDivider(recycler)
        return recycler
    }

    private fun showCategoryPicker(item: ClipItem) {
        val categories = prefs.getCategories()
        val popup = PopupContainer(context)
        val list = ListView(context)
        val names = categories.map { it.name }.toMutableList()
        names.add("+ Nueva categoría")
        val listAdapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, names) {
            override fun getView(position: Int, convertView: android.view.View?, parent: ViewGroup): android.view.View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(Color.WHITE)
                v.textSize = 19f
                v.setPadding(32, 28, 32, 28)
                return v
            }
        }
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

    private fun promptNewCategoryName(
        title: String = "Nueva categoría", initial: String = "", buttonLabel: String = "Crear",
        hint: String = "Nombre de la categoría", onCreated: (String) -> Unit
    ) {
        releaseSearchKeyboard()
        val field = InlineTextField(context, hint)
        if (initial.isNotEmpty()) field.setTextValue(initial)
        val popup = PopupContainer(context)
        val container = LinearLayout(context).apply { orientation = VERTICAL; setPadding(24, 24, 24, 24) }
        container.addView(field)
        container.addView(field.keyboardSlot)
        val btn = Button(context).apply {
            text = buttonLabel
            textSize = 18f
            setOnClickListener {
                val name = field.text().trim()
                if (name.isNotEmpty()) { popup.dismiss(); onCreated(name) }
            }
        }
        container.addView(btn, 0)
        field.onEnter = { btn.performClick() }
        popup.show(container, title, this, onDismiss = { inlineProvider.detachInlineKeyboard() })
        field.setActive(true)
        inlineProvider.attachInlineKeyboard(field.keyboardSlot, field, extraHeightPx = 0)
    }

    // ---------- Pestaña Categorías: lista ----------
    private fun buildCategoriasListView(): LinearLayout {
        val root = LinearLayout(context).apply { orientation = VERTICAL }

        val searchBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
        }
        val search = InlineTextField(context, "Buscar categoría…")
        val doneBtn = TextView(context).apply {
            text = "Listo"; textSize = 16f
            setTextColor(Color.parseColor("#6C8CFF"))
            setPadding(24, 12, 24, 12)
            visibility = android.view.View.GONE
            setOnClickListener { releaseSearchKeyboard(); search.setActive(false); visibility = android.view.View.GONE }
        }
        search.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        search.onTap = {
            if (!searchAttached) {
                searchAttached = true
                search.setActive(true)
                doneBtn.visibility = android.view.View.VISIBLE
                inlineProvider.attachInlineKeyboard(search.keyboardSlot, search, extraHeightPx = (resources.displayMetrics.density * 250f).toInt())
            }
        }
        search.onEnter = { doneBtn.performClick() }
        searchBar.addView(search)
        searchBar.addView(doneBtn)
        root.addView(searchBar)
        root.addView(search.keyboardSlot)

        // Lista de categorías: mantener presionada una y arrastrar arriba/abajo para reordenar
        var filterText = ""
        val shown = mutableListOf<ClipCategory>()
        val catRecycler = RecyclerView(context)
        catRecycler.layoutManager = LinearLayoutManager(context)
        catRecycler.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)

        val catAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = shown.size + 1 // +1: fila "Crear categoría"
            override fun getItemViewType(position: Int) = if (position < shown.size) 0 else 1
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val row = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(28, 12, 12, 12)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                val tv = TextView(context).apply {
                    textSize = 19f
                    setPadding(0, 20, 0, 20)
                    compoundDrawablePadding = 24
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(tv)
                return object : RecyclerView.ViewHolder(row) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val row = holder.itemView as LinearLayout
                val tv = row.getChildAt(0) as TextView
                while (row.childCount > 1) row.removeViewAt(1)
                if (position < shown.size) {
                    val cat = shown[position]
                    tv.text = cat.name
                    tv.setTextColor(textColor)
                    tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0)
                    tv.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(hintColor)
                    tv.setOnClickListener { openCategory = cat; render() }
                    row.addView(smallIcon(R.drawable.ic_edit, "#6C8CFF") {
                        promptNewCategoryName("Renombrar categoría", cat.name, "Guardar") { n -> prefs.renameCategory(cat.id, n); render() }
                    })
                    row.addView(smallIcon(R.drawable.ic_delete, "#FF6B6B") { confirmDeleteCategory(cat) })
                } else {
                    tv.text = "Crear categoría"
                    tv.setTextColor(Color.parseColor("#6C8CFF"))
                    tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add, 0, 0, 0)
                    tv.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#6C8CFF"))
                    tv.setOnClickListener { promptNewCategoryName { name -> prefs.addCategory(name); render() } }
                }
            }
        }
        catRecycler.adapter = catAdapter
        addDivider(catRecycler)

        fun reloadCats() {
            shown.clear()
            shown.addAll(prefs.getCategories().filter { it.name.contains(filterText, ignoreCase = true) })
            catAdapter.notifyDataSetChanged()
        }
        reloadCats()

        val catTouch = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun isLongPressDragEnabled() = filterText.isEmpty()
            override fun getMovementFlags(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int =
                if (vh.bindingAdapterPosition >= shown.size) 0 else super.getMovementFlags(rv, vh)
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = vh.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from < 0 || to < 0 || from >= shown.size || to >= shown.size) return false
                val item = shown.removeAt(from)
                shown.add(to, item)
                catAdapter.notifyItemMoved(from, to)
                return true
            }
            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                if (filterText.isEmpty()) prefs.saveCategories(shown.toList())
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
        })
        catTouch.attachToRecyclerView(catRecycler)
        root.addView(catRecycler)

        search.onTextChanged = { filterText = it; reloadCats() }

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
            layoutParams = LinearLayout.LayoutParams(64, 64).apply { marginEnd = 12 }
            setOnClickListener { openCategory = null; render() }
        }
        val back = TextView(context).apply {
            text = category.name; textSize = 18f
            setTextColor(textColor)
            setOnClickListener { openCategory = null; render() }
        }
        back.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val addText = TextView(context).apply {
            text = "＋ Añadir texto"; textSize = 16f
            setTextColor(Color.parseColor("#6C8CFF"))
            setPadding(24, 12, 24, 12)
            setOnClickListener {
                promptNewCategoryName("Añadir texto a «${category.name}»", "", "Guardar", "Escribe el texto…") { t ->
                    prefs.addClipToCategory(t, category.id); render()
                }
            }
        }
        header.addView(backIcon)
        header.addView(back)
        header.addView(addText)
        root.addView(header)

        val recycler = RecyclerView(context)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(recycler)

        var touchHelperRef: ItemTouchHelper? = null
        val items = prefs.getClipsForCategory(category.id).toMutableList()
        val adapter = ClipAdapter(
            items = items,
            onTap = { listener?.onPaste(it.text) },
            onCategorize = null,
            onDelete = { item -> prefs.deleteClip(item.id); render() },
            onEdit = { item -> promptEditText(item) },
            showEditInsteadOfCategorize = true,
            alwaysShowActions = true,
            onRemoveFromCategory = { item -> prefs.setClipCategory(item.id, null); render() },
            onStartDrag = { vh -> touchHelperRef?.startDrag(vh) }
        )
        recycler.adapter = adapter
        addDivider(recycler)

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
            override fun isLongPressDragEnabled() = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
        })
        touchHelperRef = touchHelper
        touchHelper.attachToRecyclerView(recycler)

        return root
    }

    private fun promptEditText(item: ClipItem) {
        val field = InlineTextField(context, "Texto")
        field.setTextValue(item.text)
        val popup = PopupContainer(context)
        val container = LinearLayout(context).apply { orientation = VERTICAL; setPadding(24, 24, 24, 24) }
        container.addView(field)
        container.addView(field.keyboardSlot)
        val btn = Button(context).apply {
            text = "Guardar"
            textSize = 18f
            setOnClickListener {
                prefs.updateClipText(item.id, field.text())
                popup.dismiss()
                render()
            }
        }
        container.addView(btn, 0)
        popup.show(container, "Editar", this, onDismiss = { inlineProvider.detachInlineKeyboard() })
        field.setActive(true)
        inlineProvider.attachInlineKeyboard(field.keyboardSlot, field, extraHeightPx = 0)
    }

    // ---------- Adapter compartido ----------
    private inner class ClipAdapter(
        val items: List<ClipItem>,
        val onTap: (ClipItem) -> Unit,
        val onCategorize: ((ClipItem) -> Unit)?,
        val onDelete: (ClipItem) -> Unit,
        val onEdit: ((ClipItem) -> Unit)?,
        val showEditInsteadOfCategorize: Boolean,
        val alwaysShowActions: Boolean = false,
        val onRemoveFromCategory: ((ClipItem) -> Unit)? = null,
        val onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
    ) : RecyclerView.Adapter<ClipAdapter.VH>() {

        inner class VH(val row: LinearLayout, val text: TextView, val actions: LinearLayout) : RecyclerView.ViewHolder(row)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(20, 14, 12, 14)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val text = TextView(context).apply {
                textSize = 17f; maxLines = 3; setTextColor(textColor)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            // acciones a la derecha, compactas, en cuadrícula de 2 x 2
            val actions = LinearLayout(context).apply {
                orientation = VERTICAL
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
            val d = context.resources.displayMetrics.density
            fun btn(res: Int, tint: String, onClick: () -> Unit) = smallIcon(res, tint, onClick)

            val first = LinearLayout(context).apply { orientation = HORIZONTAL }
            val second = LinearLayout(context).apply { orientation = HORIZONTAL }

            // Fila 1: editar / categorizar  +  quitar de la categoría
            first.addView(btn(if (showEditInsteadOfCategorize) R.drawable.ic_edit else R.drawable.ic_tag, "#6C8CFF") {
                if (showEditInsteadOfCategorize) onEdit?.invoke(item) else onCategorize?.invoke(item)
                if (!alwaysShowActions) holder.actions.visibility = ViewGroup.GONE
            })
            if (onRemoveFromCategory != null) {
                first.addView(btn(R.drawable.ic_close, "#FFB74D") { onRemoveFromCategory.invoke(item) })
            }
            // Fila 2: mover  +  borrar
            if (onStartDrag != null) {
                second.addView(TextView(context).apply {
                    text = "≡"; textSize = 26f; gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#CCCCCC"))
                    layoutParams = LinearLayout.LayoutParams((40 * d).toInt(), (40 * d).toInt())
                    setOnTouchListener { _, ev ->
                        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) onStartDrag.invoke(holder)
                        false
                    }
                })
            }
            second.addView(btn(R.drawable.ic_delete, "#FF6B6B") { onDelete(item) })
            holder.actions.addView(first)
            holder.actions.addView(second)
            if (alwaysShowActions) holder.actions.visibility = ViewGroup.VISIBLE

            holder.row.setOnClickListener { onTap(item) }
            if (!alwaysShowActions) {
                holder.row.setOnLongClickListener {
                    holder.actions.visibility = if (holder.actions.visibility == ViewGroup.VISIBLE) ViewGroup.GONE else ViewGroup.VISIBLE
                    true
                }
            } else {
                holder.row.setOnLongClickListener(null)
            }
        }

        override fun getItemCount() = items.size
    }
}
