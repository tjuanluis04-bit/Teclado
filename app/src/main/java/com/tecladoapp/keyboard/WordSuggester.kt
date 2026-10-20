package com.tecladoapp.keyboard

/**
 * Sugeridor de palabras y corrector ortográfico simple basado en un diccionario
 * embebido de palabras frecuentes en español + distancia de edición (Levenshtein)
 * para proponer correcciones cuando la palabra escrita no existe en el diccionario.
 */
object WordSuggester {

    // Diccionario compacto de palabras muy frecuentes en español.
    private val dictionary: Set<String> = setOf(
        "el","la","de","que","y","a","en","un","ser","se","no","haber","por","con","su",
        "para","como","estar","tener","le","lo","lo","todo","pero","más","hacer","o","poder",
        "decir","este","ir","otro","ese","si","me","ya","ver","porque","dar","cuando","muy",
        "sin","vez","mucho","saber","qué","sobre","mi","alguno","mismo","yo","también","hasta",
        "año","dos","querer","entre","así","primero","desde","grande","eso","ni","nos","llegar",
        "pasar","tiempo","ella","siempre","día","tanto","ella","hola","gracias","por favor",
        "buenos","días","tardes","noches","adiós","nombre","casa","trabajo","teléfono","correo",
        "mensaje","teclado","aplicación","favorito","reciente","categoría","buscar","copiar",
        "pegar","emoji","tema","color","tamaño","ajustar","fuente","letra","número","hoy",
        "mañana","ayer","semana","mes","gente","persona","amigo","familia","cosa","lugar",
        "vida","mundo","mano","parte","ojo","palabra","caso","punto","forma","problema",
        "estoy","estás","está","estamos","están","soy","eres","es","somos","son","fue","era","ha","he","hay","había","tengo","tiene","tienes","tenemos","quiero","quieres","puedo","puedes","puede","podemos","voy","vas","va","vamos","van","hago","haces","hace","dime","dijo","dice","sé","sabes","sabe","creo","pienso","necesito","necesitas","favor","claro","bueno","buena","bien","mal","mejor","peor","nuevo","nueva","viejo","pequeño","largo","corto","alto","bajo","rápido","lento","fácil","difícil","importante","posible","necesario","diferente","igual","cerca","lejos","aquí","allí","ahí","donde","dónde","cuándo","cómo","quién","cuál","cuánto","entonces","después","antes","luego","ahora","pronto","tarde","temprano","nunca","jamás","algo","nada","alguien","nadie","cada","varios","muchos","pocos","todos","todas","ambos","mismo","misma","solo","sola","tan","más","menos","casi","apenas","aunque","mientras","durante","según","hacia","contra","bajo","tras","sino","pues","porque","para","pero","además","también","tampoco","incluso","quizás","quizá","vale","listo","perfecto","excelente","genial","increíble","maravilloso","feliz","triste","enojado","cansado","hambre","sed","comida","cena","desayuno","almuerzo","agua","café","cerveza","pan","carne","pollo","pescado","fruta","verdura","dinero","banco","tienda","mercado","precio","pago","pagar","comprar","vender","enviar","recibir","llamar","llamada","escribir","leer","hablar","escuchar","mirar","pensar","sentir","vivir","morir","nacer","crecer","jugar","correr","caminar","viajar","volver","salir","entrar","subir","bajar","abrir","cerrar","empezar","terminar","seguir","esperar","buscar","encontrar","perder","ganar","ayudar","cambiar","usar","llevar","traer","poner","quitar","tomar","dejar","parecer","conocer","entender","recordar","olvidar","aprender","enseñar","estudiar","escuela","universidad","clase","maestro","profesor","alumno","libro","carta","foto","video","música","canción","película","juego","fiesta","regalo","cumpleaños","navidad","vacaciones","viaje","ciudad","país","calle","carro","coche","auto","bus","avión","tren","moto","bicicleta","hospital","médico","doctor","salud","enfermo","medicina","dolor","cabeza","cuerpo","corazón","cara","boca","nariz","pie","madre","padre","mamá","papá","hijo","hija","hermano","hermana","abuelo","abuela","tío","tía","primo","esposo","esposa","novio","novia","niño","niña","hombre","mujer","señor","señora","jefe","empresa","oficina","proyecto","reunión","cliente","producto","servicio","cuenta","contraseña","internet","computadora","celular","pantalla","batería","archivo","documento","imagen","pregunta","respuesta","razón","motivo","idea","plan","meta","sueño","amor","cariño","paz","guerra","verdad","mentira","miedo","esperanza","suerte","tiempo","momento","rato","minuto","hora","horas","lunes","martes","miércoles","jueves","viernes","sábado","domingo","enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre","tengo","estoy","necesito","quisiera","podrías","podría","saludos","atentamente","cordialmente","disculpa","perdón","lo siento","de nada","con gusto","mucho gusto","hasta luego","nos vemos","buen día","feliz día"
    )

    /**
     * Sugerencias por prefijo: primero las palabras que el usuario más usa (userFreq),
     * luego el diccionario. Con prefijo vacío devuelve las más usadas.
     */
    fun suggestionsFor(prefix: String, userFreq: Map<String, Int> = emptyMap(), limit: Int = 3): List<String> {
        val lower = prefix.lowercase()
        val fromUser = userFreq.entries
            .filter { it.key.startsWith(lower) && it.key != lower && (lower.isNotEmpty() || it.value >= 2) }
            .sortedByDescending { it.value }
            .map { it.key }
        val fromDict = if (lower.isEmpty()) emptyList() else dictionary
            .filter { it.startsWith(lower) && it != lower }
            .sortedBy { it.length }
        return (fromUser + fromDict).distinct().take(limit)
    }

    fun knows(word: String, userFreq: Map<String, Int>): Boolean =
        dictionary.contains(word.lowercase()) || userFreq.containsKey(word.lowercase())

    /** Devuelve true si la palabra se considera correctamente escrita. */
    fun isSpelledCorrectly(word: String, userFreq: Map<String, Int> = emptyMap()): Boolean {
        if (word.isBlank()) return true
        val lower = word.lowercase()
        if (lower.length <= 2) return true
        return dictionary.contains(lower) || (userFreq[lower] ?: 0) >= 2
    }

    fun spellingCorrections(word: String, limit: Int = 3): List<String> {
        val lower = word.lowercase()
        return dictionary
            .map { it to levenshtein(lower, it) }
            .filter { it.second <= 2 }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }
}
