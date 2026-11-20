package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Conectores del español por categoría: un toque inserta el conector (con un espacio detrás). */
class ConnectorsPanelView(context: Context) : LinearLayout(context) {

    interface Listener {
        fun onInsert(text: String)
        fun onClose()
    }
    var listener: Listener? = null

    private val data: List<Pair<String, List<String>>> = listOf(
        "Adición" to listOf("además", "asimismo", "también", "incluso", "igualmente", "de igual modo", "por otra parte", "aparte de eso", "es más", "del mismo modo", "y", "ni"),
        "Contraste" to listOf("pero", "sin embargo", "no obstante", "en cambio", "por el contrario", "sino", "ahora bien", "al contrario", "mientras que", "a diferencia de", "aun así"),
        "Concesión" to listOf("aunque", "si bien", "a pesar de", "a pesar de que", "por más que", "aun cuando", "pese a", "así y todo"),
        "Causa" to listOf("porque", "ya que", "puesto que", "debido a", "dado que", "a causa de", "gracias a", "visto que", "en vista de que", "por culpa de"),
        "Consecuencia" to listOf("por lo tanto", "por consiguiente", "así que", "de modo que", "en consecuencia", "por eso", "por ende", "de ahí que", "por esta razón", "de manera que", "entonces"),
        "Finalidad" to listOf("para que", "con el fin de", "a fin de", "con el propósito de", "con la intención de", "con miras a", "para"),
        "Condición" to listOf("si", "siempre que", "siempre y cuando", "a menos que", "con tal de que", "en caso de que", "a condición de que", "salvo que", "excepto si", "de lo contrario"),
        "Orden" to listOf("en primer lugar", "en segundo lugar", "primero", "segundo", "luego", "después", "a continuación", "finalmente", "por último", "para empezar", "antes de todo", "por otro lado"),
        "Tiempo" to listOf("cuando", "mientras", "apenas", "desde que", "hasta que", "antes de que", "una vez que", "al mismo tiempo", "mientras tanto", "en ese momento", "de pronto", "al final"),
        "Ejemplo" to listOf("por ejemplo", "como", "tal como", "a saber", "en particular", "es decir", "por caso", "así", "como muestra", "verbigracia"),
        "Énfasis" to listOf("sobre todo", "especialmente", "en efecto", "de hecho", "cabe destacar", "vale decir", "es importante señalar", "en realidad", "ciertamente", "lo más importante"),
        "Comparación" to listOf("así como", "igual que", "al igual que", "tanto como", "más que", "menos que", "de la misma manera", "en comparación con", "semejante a"),
        "Conclusión" to listOf("en conclusión", "en resumen", "para concluir", "en definitiva", "en síntesis", "en suma", "para terminar", "en pocas palabras", "en fin", "como resultado"),
        "Reformulación" to listOf("o sea", "es decir", "dicho de otro modo", "en otras palabras", "esto es", "mejor dicho", "para ser más precisos"),
        "Opinión" to listOf("en mi opinión", "a mi parecer", "desde mi punto de vista", "según", "a mi juicio", "personalmente", "creo que", "me parece que")
    )

    private var current = 0
    private val chips = LinearLayout(context)
    private val list = LinearLayout(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#161616"))
        addView(PanelUi.topBar(context, "Conectores") { listener?.onClose() })

        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(context, 50f))
        }
        chips.orientation = HORIZONTAL
        chips.setPadding(PanelUi.dp(context, 6f), PanelUi.dp(context, 4f), PanelUi.dp(context, 6f), 0)
        scroll.addView(chips)
        addView(scroll)

        list.orientation = VERTICAL
        list.setPadding(PanelUi.dp(context, 8f), PanelUi.dp(context, 4f), PanelUi.dp(context, 8f), PanelUi.dp(context, 8f))
        val sv = ScrollView(context).apply { layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f); addView(list) }
        addView(sv)
        render()
    }

    private fun render() {
        chips.removeAllViews()
        data.forEachIndexed { i, (name, _) -> chips.addView(PanelUi.chip(context, name, i == current) { current = i; render() }) }
        list.removeAllViews()
        // dos columnas
        val items = data[current].second
        var i = 0
        while (i < items.size) {
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            for (k in 0..1) {
                val w = items.getOrNull(i + k)
                row.addView(TextView(context).apply {
                    text = w ?: ""
                    textSize = 17f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                    setPadding(PanelUi.dp(context, 8f), PanelUi.dp(context, 12f), PanelUi.dp(context, 8f), PanelUi.dp(context, 12f))
                    if (w != null) {
                        background = GradientDrawable().apply { setColor(Color.parseColor("#262626")); cornerRadius = PanelUi.dp(context, 10f).toFloat() }
                        setOnClickListener { listener?.onInsert("$w ") }
                    }
                    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(PanelUi.dp(context, 3f), PanelUi.dp(context, 3f), PanelUi.dp(context, 3f), PanelUi.dp(context, 3f))
                    }
                })
            }
            list.addView(row)
            i += 2
        }
    }
}
