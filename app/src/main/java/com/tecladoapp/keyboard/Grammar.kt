package com.tecladoapp.keyboard

import java.text.Normalizer

/**
 * Clasificador APROXIMADO de palabras en español: sustantivos, adjetivos, verbos y adverbios.
 * Se basa en listas de palabras funcionales, terminaciones y la palabra anterior; no es un
 * analizador lingüístico completo, así que puede equivocarse en palabras ambiguas.
 */
object Grammar {

    data class Result(
        val nouns: List<String>, val adjectives: List<String>,
        val verbs: List<String>, val adverbs: List<String>
    )

    private fun set(s: String) = s.split(' ', '\n').filter { it.isNotBlank() }.toSet()

    private val determiners = set("el la los las un una unos unas este esta estos estas ese esa esos esas aquel aquella aquellos aquellas mi mis tu tus su sus nuestro nuestra nuestros nuestras vuestro vuestra vuestros vuestras algún alguna algunos algunas ningún ninguna cada todo toda todos todas otro otra otros otras mucho mucha muchos muchas poco poca pocos pocas varios varias demás cierto cierta ciertos ciertas")

    private val stop = set("""
        a ante bajo con contra de del desde durante en entre hacia hasta mediante para por según sin sobre tras al
        y e o u ni que pero sino aunque porque pues si como cuando donde cual cuales quien quienes cuyo cuya cuyos cuyas
        yo tú vos él ella usted nosotros nosotras ellos ellas ustedes vosotros vosotras me te se lo le les nos os mí ti sí conmigo contigo consigo
        esto eso aquello algo nada alguien nadie qué quién cuál cuánto cuánta cuántos cuántas dónde cómo cuándo
        uno dos tres cuatro cinco seis siete ocho nueve diez cien mil
        el la los las un una unos unas este esta estos estas ese esa esos esas aquel aquella aquellos aquellas
        mi mis tu tus su sus nuestro nuestra nuestros nuestras
        sea sean ser
    """.trimIndent())

    private val adverbs = set("""
        muy mucho poco bien mal ahora antes después luego ya aún aun todavía siempre nunca jamás hoy ayer anoche mañana
        aquí ahí allí allá acá cerca lejos dentro fuera arriba abajo delante detrás encima debajo adelante atrás
        así también tampoco solo sólo más menos casi apenas bastante demasiado tan tanto tanta pronto tarde temprano
        quizá quizás acaso entonces además incluso mientras tanto despacio rápido bajo alto claro mejor peor
        no sí tal vez enseguida inmediatamente nuevamente
    """.trimIndent())

    private val knownAdjectives = set("""
        grande pequeño pequeña grandes pequeños pequeñas bueno buena buenos buenas malo mala malos malas nuevo nueva nuevos nuevas
        viejo vieja viejos viejas largo larga largos largas corto corta cortos cortas alto alta altos altas bajo baja bajos bajas
        mismo misma mismos mismas joven jóvenes fácil fáciles difícil difíciles importante importantes mejor mejores peor peores
        rojo roja rojos rojas azul azules verde verdes amarillo amarilla blanco blanca negro negra gris grises rosa morado morada
        feliz felices triste tristes fuerte fuertes débil débiles rápido rápida rápidos rápidas lento lenta hermoso hermosa
        bonito bonita feo fea nuevo cierto propio propia propios propias posible posibles necesario necesaria necesarios necesarias
        único única únicos únicas siguiente anterior último última últimos últimas primero primera primeros primeras tercero
        general generales social sociales público pública públicos públicas privado privada humano humana humanos humanas
        libre libres lleno llena vacío vacía caliente calientes frío fría fríos frías cálido cálida claro clara oscuro oscura
        sencillo sencilla simple simples complejo compleja diferente diferentes igual iguales similar similares
        bueno mayor mayores menor menores real reales verdadero verdadera falso falsa seguro segura seguros seguras
        cansado cansada cansados cansadas contento contenta enfermo enferma sano sana rico rica pobre pobres
    """.trimIndent())

    /** Sustantivos que parecen verbos (-ar/-er/-ir) o participios. */
    private val nounExceptions = set("""
        lugar lugares mujer mujeres taller talleres hogar hogares mar mares par pares bar bares altar azúcar cáncer líder líderes
        carácter placer placeres deber deberes poder poderes ser seres amanecer atardecer anochecer cantar mármol collar collares
        ruido sonido partido vestido pedido sentido marido tejido nacido olvido comido bebida comida salida llegada vida herida
        sandwich mujer cuaderno año años día días mes meses vez veces
    """.trimIndent())

    private val irregularVerbs = set("""
        es son soy eres somos era eran eras éramos fue fueron fui fuiste fuimos sea sean será serán sería serían
        está estás están estoy estamos estaba estaban estuvo estuvieron esté estén estará estarán
        hay haya ha han he has hemos había habían habías habíamos hubo hubieron habrá habrán
        tiene tienen tengo tienes tenemos tenía tenían tuvo tuvieron tenga tengan tendrá tendrán
        hace hacen hago haces hacemos hacía hacían hizo hicieron haga hagan hará harán
        puede pueden puedo puedes podemos podía podían pudo pudieron pueda puedan podrá podrán
        debe deben debo debes debemos debía debían debió deberá
        quiere quieren quiero quieres queremos quería querían quiso quisieron
        va van voy vas vamos iba iban fui fue fueron vaya vayan irá irán
        dice dicen digo dices decimos decía decían dijo dijeron diga digan dirá dirán
        sabe saben sé sabes sabemos sabía sabían supo supieron sepa sepan sabrá
        viene vienen vengo vienes venimos venía venían vino vinieron venga vengan vendrá vendrán
        ve ven veo ves vemos veía veían vio vieron vea vean verá verán
        da dan doy das damos daba daban dio dieron dé den dará darán
        pone ponen pongo pones ponemos ponía ponían puso pusieron ponga pongan pondrá
        sale salen salgo sales salimos salía salían salió salieron salga salgan saldrá
        sigue siguen sigo sigues seguimos seguía seguían siguió siguieron siga sigan
        parece parecen parezco parecía parecían pareció parecieron parezca
        queda quedan quedo quedas quedamos quedaba quedaban quedó quedaron
        llega llegan llego llegas llegamos llegaba llegaban llegó llegaron
        deja dejan dejo dejas dejamos dejaba dejaban dejó dejaron
        lleva llevan llevo llevas llevamos llevaba llevaban llevó llevaron
        trae traen traigo traes traemos traía traían trajo trajeron
        cree creen creo crees creemos creía creían creyó creyeron
        piensa piensan pienso piensas pensamos pensaba pensaban pensó pensaron
        vive viven vivo vives vivimos vivía vivían vivió vivieron
        come comen comes comemos comía comían comió comieron
        habla hablan hablo hablas hablamos hablaba hablaban habló hablaron
        necesita necesitan necesito necesitas necesitamos necesitaba necesitaban necesitó necesitaron
        encuentra encuentran encuentro encuentras encontramos encontraba encontró encontraron
        busca buscan busco buscas buscamos buscaba buscó buscaron
        escribe escriben escribo escribes escribimos escribía escribió escribieron
        lee leen leo lees leemos leía leían leyó leyeron
        mira miran miro miras miramos miraba miró miraron
        usa usan uso usas usamos usaba usaban usó usaron
        conoce conocen conozco conoces conocemos conocía conoció
        toma toman tomo tomas tomamos tomaba tomó tomaron
        pasa pasan paso pasas pasamos pasaba pasó pasaron
        gusta gustan gusto gustas gustaba gustó
        importa importan importaba importó
        existe existen existía existió
        permite permiten permitía permitió
        sirve sirven servía sirvió
        ayuda ayudan ayudo ayudas ayudamos ayudaba ayudó
        trabaja trabajan trabajo trabajas trabajamos trabajaba trabajó trabajaron
        empieza empiezan empiezo empezamos empezaba empezó empezaron
        termina terminan termino terminas terminamos terminaba terminó terminaron
        puedo
    """.trimIndent())

    private val auxiliaries = set("he has ha hemos han había habías habíamos habían haya hayan hubiera hubo habré habrá habrán es son era eran fue fueron sea sean está están estaba estaban estuvo")

    private val subjects = set("yo tú vos él ella usted nosotros nosotras ellos ellas ustedes vosotros se me te nos lo no ya también siempre nunca y que")

    private val nounLikeEndings = Regex("(ción|sión|dad|tad|tud|umbre|ncia|nza|ismo|ista|aje|miento|ura|eza|ez|or|ión|ía|ería)$")
    private val badIaNouns = Regex("(logía|grafía|nomía|ería|ría|tría|sofía|energía|alegría|melodía|poesía|categoría|policía|cortesía|compañía|simpatía|fantasía|garantía|travesía|anarquía|monarquía|teoría|biología|día|días|tía|tías|mía|mías|guía|guías|vía|vías|pía|cobardía|valentía|cercanía|lejanía|sabiduría|geografía|ciudadanía|sociología|psicología|filosofía|democracia|fantasías)$")

    private fun clean(w: String) = w.trim { !it.isLetter() }.lowercase()

    private fun isVerbForm(w: String, prev: String): Boolean {
        if (w in irregularVerbs) return true
        if (w in nounExceptions) return false
        val len = w.length
        if (len >= 5 && Regex("(ando|iendo|yendo)$").containsMatchIn(w)) return true
        if (len >= 4 && isInfinitive(w) && !nounLikeEndings.containsMatchIn(w)) return true
        if (len >= 5 && Regex("(aban|abas|ábamos|abais|aron|ieron|yeron|aste|iste|aré|arás|arán|aremos|eré|erás|erán|eremos|iré|irás|irán|iremos|arían|arías|erían|erías|irían|irías|ase|ases|asen|iese|ieses|iesen|iera|ieras|ieran)$").containsMatchIn(w)) return true
        if (len >= 5 && Regex("(aría|ería|iría)$").containsMatchIn(w) && !badIaNouns.containsMatchIn(w)) return true
        if (len >= 4 && Regex("(ía|ías|ían|íamos)$").containsMatchIn(w) && !badIaNouns.containsMatchIn(w)) return true
        if (len >= 4 && Regex("(ó|ió)$").containsMatchIn(w)) return true
        if (len >= 6 && Regex("(amos|emos)$").containsMatchIn(w)) return true
        if (len >= 5 && Regex("(ará|erá|irá)$").containsMatchIn(w)) return true
        // participio con auxiliar
        if (Regex("(ado|ido|ados|idas|ada|ida|ados|idos|to|cho|tos|chos|ta|cha)$").containsMatchIn(w) && len >= 5 && prev in auxiliaries) return true
        // tras un sujeto/«no»/«se»… una forma terminada en vocal o -n/-s suele ser verbo conjugado
        if (prev in subjects && len >= 3 && Regex("(a|e|o|as|es|an|en|amos|emos|imos|áis|éis)$").containsMatchIn(w) &&
            !nounLikeEndings.containsMatchIn(w) && w !in knownAdjectives) return true
        return false
    }

    private fun isAdjective(w: String): Boolean {
        if (w in knownAdjectives) return true
        if (w.length < 5) return false
        if (Regex("(oso|osa|osos|osas|ivo|iva|ivos|ivas|able|ables|ible|ibles|ico|ica|icos|icas|ante|antes|ente|entes|ario|aria|arios|arias|ero|era|eros|eras|udo|uda|ista|istas|ual|uales)$").containsMatchIn(w)
            && !nounLikeEndings.containsMatchIn(w.removeSuffix("s")) ) return true
        if (Regex("(ado|ada|ados|adas|ido|ida|idos|idas)$").containsMatchIn(w)) return true
        if (Regex("(al|ales|ar|ares|il|iles)$").containsMatchIn(w) && w.length >= 6 && !isInfinitive(w)) return true
        return false
    }

    private fun isInfinitive(w: String) = w.length >= 4 && Regex("(ar|er|ir)(se|me|te|lo|la|le|nos|los|las|les)?$").containsMatchIn(w) && w !in nounExceptions

    fun analyze(text: String): Result {
        val tokens = Regex("[\\p{L}]+(?:['’-][\\p{L}]+)*").findAll(text).map { it.value }.toList()
        val nouns = LinkedHashSet<String>(); val adjs = LinkedHashSet<String>()
        val verbs = LinkedHashSet<String>(); val advs = LinkedHashSet<String>()
        var prev = ""
        for (raw in tokens) {
            val w = clean(raw)
            if (w.isEmpty()) { continue }
            val afterDet = prev in determiners
            val afterAdvIntens = prev == "muy" || prev == "más" || prev == "menos" || prev == "tan"
            when {
                w in stop -> {}
                w.length == 1 -> {}
                w in adverbs && !(afterDet && w in knownAdjectives) -> advs.add(w)
                w.length > 6 && w.endsWith("mente") -> advs.add(w)
                w in irregularVerbs && !afterDet -> verbs.add(w)
                afterAdvIntens && w !in irregularVerbs && !isInfinitive(w) -> adjs.add(w)
                afterDet && w in knownAdjectives -> adjs.add(w)
                afterDet && !Regex("(ando|iendo|yendo)$").containsMatchIn(w) -> nouns.add(w)
                isVerbForm(w, prev) -> verbs.add(w)
                isAdjective(w) -> adjs.add(w)
                else -> nouns.add(w)
            }
            prev = w
        }
        return Result(nouns.toList(), adjs.toList(), verbs.toList(), advs.toList())
    }
}
