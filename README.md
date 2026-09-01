# Teclado App

Teclado personalizado (IME) para Android con acceso rápido a emojis y portapapeles,
sugerencias de palabras, corrección ortográfica básica, temas/fuentes personalizables,
tamaño ajustable y gesto de "recorrer palabras" deslizando la tecla Enter.

## Cómo compilarlo

1. Sube esta carpeta a un repositorio de GitHub (rama `main`).
2. El workflow `.github/workflows/build.yml` compila automáticamente el APK debug
   en cada push, usando GitHub Actions (Gradle, sin necesidad de Android Studio).
3. Descarga el APK desde la pestaña **Actions → build → Artifacts** una vez termine.
4. También puedes lanzarlo manualmente desde Actions con "Run workflow" (workflow_dispatch).

## Instalación y activación en el celular

1. Instala el APK.
2. Abre la app "Teclado App" → botón **Activar teclado en Ajustes del sistema**.
3. Actívalo en la lista de teclados del sistema.
4. Vuelve a la app y pulsa **Elegir este teclado** (o cambia de teclado desde la
   barra de notificaciones) para seleccionarlo como predeterminado.

## Qué incluye esta primera versión

- Fila de números 0-9 sobre las letras, teclado QWERTY completo, símbolos.
- Barra superior con accesos directos a Emojis 🙂, Portapapeles 📋 y Ajustes ⚙️.
- **Emojis**: pestañas Recientes / Favoritos + lupa de búsqueda por palabra clave;
  mantener presionado un emoji lo marca/desmarca como favorito.
- **Portapapeles**: pestañas Recientes (últimos 10, el más viejo se descarta al
  llegar uno nuevo salvo que esté categorizado) y Categorías (lista con lupa).
  Mantener presionado un elemento reciente muestra "Categorizar" y "Borrar".
  Dentro de una categoría se puede arrastrar para reordenar y aparece "Editar".
- Pegado directo del último texto copiado desde cualquiera de las dos pestañas.
- Sugerencias de palabras (diccionario embebido en español) y corrección
  ortográfica básica (distancia de edición) mostradas en una barra sobre el teclado.
- Tamaño del teclado ajustable con un control deslizante en Ajustes.
- 10 temas de color/textura y 8 tipografías seleccionables en Ajustes.
- Gesto en la tecla Enter: deslizar el dedo izquierda/derecha mueve el cursor
  palabra por palabra; si sigues deslizando tras el primer salto, extiende la
  selección para abarcar más palabras (como un trackpad).
- Icono adaptativo maximalista: el diseño ocupa el 60% central del icono con
  20% de margen en los cuatro lados (`drawable/ic_launcher_foreground.xml`).

## Próximos pasos sugeridos

- Ampliar el diccionario de sugerencias/ortografía (o conectar con el
  `SpellCheckerService` del sistema para cobertura completa).
- Añadir más categorías de emojis y un set más amplio.
- Pulir animaciones de teclas y vista previa al presionar (popup de tecla).
- Guardar/editar el nombre de categorías desde un icono de edición dedicado.
