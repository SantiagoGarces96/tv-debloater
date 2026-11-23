package tv.clean.debloater.phone

/** Tipos de apps sobrantes en el celular. */
enum class PhoneKind(val label: String) {
    ADS("Publicidad y rastreo"),
    STORE("Tiendas y promociones"),
    DUPLICATE("Duplicadas"),
    UNUSED("Poco útiles"),
}

data class PhoneApp(
    val pkg: String,
    val name: String,
    val why: String,
    val kind: PhoneKind,
    /** Se puede quitar sin perder nada importante. */
    val recommended: Boolean,
)

/** Lista cerrada y revisada a mano: la app nunca toca un paquete que no esté aquí. */
object PhoneCatalog {
    val apps: List<PhoneApp> = listOf(
        PhoneApp("com.miui.msa.global", "MSA (anuncios del sistema)", "Muestra publicidad en apps de Xiaomi.", PhoneKind.ADS, true),
        PhoneApp("com.miui.analytics", "Analytics de Xiaomi", "Envía estadísticas de uso.", PhoneKind.ADS, true),
        PhoneApp("com.miui.daemon", "MIUI Daemon", "Recoge datos de uso en segundo plano.", PhoneKind.ADS, true),
        PhoneApp("com.miui.bugreport", "Informe de errores", "Envía informes a Xiaomi.", PhoneKind.ADS, true),
        PhoneApp("com.miui.misightservice", "MiSight", "Telemetría del sistema.", PhoneKind.ADS, true),
        PhoneApp("com.miui.thirdappassistant", "Asistente de apps de terceros", "Vigila el comportamiento de otras apps.", PhoneKind.ADS, true),
        PhoneApp("com.miui.miservice", "Servicios y comentarios", "Encuestas y soporte de Xiaomi.", PhoneKind.ADS, true),
        PhoneApp("com.xiaomi.mipicks", "GetApps", "Tienda de Xiaomi con anuncios.", PhoneKind.STORE, true),
        PhoneApp("com.xiaomi.glgm", "Game Center", "Promociones de juegos.", PhoneKind.STORE, true),
        PhoneApp("com.mi.global.shop", "Mi Store", "Tienda online de Xiaomi.", PhoneKind.STORE, true),
        PhoneApp("com.mi.global.bbs", "Mi Community", "Foro de Xiaomi.", PhoneKind.STORE, true),
        PhoneApp("com.miui.android.fashiongallery", "Carrusel de fondos (Glance)", "Fondos de bloqueo con publicidad.", PhoneKind.STORE, true),
        PhoneApp("com.miui.yellowpage", "Páginas amarillas", "Directorio de empresas con anuncios.", PhoneKind.STORE, true),
        PhoneApp("com.xiaomi.barrage", "Comentarios flotantes", "Mensajes flotantes en vídeos.", PhoneKind.UNUSED, true),
        PhoneApp("com.mi.globalbrowser", "Mi Browser", "Navegador con publicidad (ya tiene Chrome).", PhoneKind.DUPLICATE, true),
        PhoneApp("com.miui.player", "Mi Música", "Reproductor con anuncios.", PhoneKind.DUPLICATE, true),
        PhoneApp("com.miui.videoplayer", "Mi Video", "Reproductor con anuncios.", PhoneKind.DUPLICATE, true),
        PhoneApp("com.xiaomi.payment", "Mi Pay", "Pagos de Xiaomi.", PhoneKind.UNUSED, true),
        PhoneApp("com.mi.globalminusscreen", "App Vault", "Lo usa la pantalla de inicio de Xiaomi: mejor dejarlo.", PhoneKind.UNUSED, false),
        PhoneApp("com.mi.appfinder", "Buscador del inicio de Xiaomi", "Lo usa la pantalla de inicio de Xiaomi: mejor dejarlo.", PhoneKind.UNUSED, false),
        PhoneApp("com.xiaomi.mtb", "Herramienta de pruebas", "Herramienta de fábrica.", PhoneKind.UNUSED, true),
        PhoneApp("com.debug.loggerui", "Registrador de depuración", "Herramienta de fábrica que consume memoria.", PhoneKind.UNUSED, true),
        PhoneApp("com.xiaomi.aiasst.vision", "Subtítulos con IA", "Asistente de subtítulos de Xiaomi.", PhoneKind.UNUSED, true),
        PhoneApp("com.miui.fm", "Radio FM", "Radio FM (necesita auriculares).", PhoneKind.UNUSED, false),
        PhoneApp("com.samsung.android.app.spage", "Samsung Free", "Noticias y promociones a la izquierda del inicio.", PhoneKind.STORE, true),
        PhoneApp("com.samsung.android.tvplus", "Samsung TV Plus", "Canales de TV con anuncios.", PhoneKind.STORE, true),
        PhoneApp("com.samsung.android.app.tips", "Consejos de Samsung", "Avisos de consejos.", PhoneKind.UNUSED, true),
        PhoneApp("com.samsung.android.ardrawing", "Dibujo AR", "Efectos de realidad aumentada.", PhoneKind.UNUSED, true),
        PhoneApp("com.samsung.android.aremoji", "Emoji AR", "Avatares de realidad aumentada.", PhoneKind.UNUSED, true),
        PhoneApp("com.samsung.android.kidsinstaller", "Samsung Kids", "Modo infantil.", PhoneKind.UNUSED, false),
        PhoneApp("com.samsung.android.bixby.agent", "Bixby", "Asistente de voz de Samsung.", PhoneKind.DUPLICATE, false),
        PhoneApp("com.samsung.android.visionintelligence", "Bixby Vision", "Reconocimiento de imágenes de Samsung.", PhoneKind.DUPLICATE, false),
        PhoneApp("com.samsung.android.game.gamehome", "Game Launcher", "Lanzador de juegos de Samsung.", PhoneKind.UNUSED, false),
        PhoneApp("com.sec.android.app.sbrowser", "Samsung Internet", "Segundo navegador.", PhoneKind.DUPLICATE, false),
        PhoneApp("com.microsoft.skydrive", "OneDrive", "Nube de Microsoft preinstalada.", PhoneKind.DUPLICATE, false),
        PhoneApp("com.motorola.ccc.notification", "Notificaciones de Motorola", "Avisos promocionales.", PhoneKind.ADS, true),
        PhoneApp("com.motorola.demo", "Modo demostración", "Modo tienda.", PhoneKind.UNUSED, true),
        PhoneApp("com.motorola.help", "Ayuda de Motorola", "Ayuda en línea.", PhoneKind.UNUSED, true),
        PhoneApp("com.facebook.appmanager", "Facebook App Manager", "Servicio oculto de Facebook.", PhoneKind.ADS, true),
        PhoneApp("com.facebook.services", "Facebook Services", "Servicio oculto de Facebook.", PhoneKind.ADS, true),
        PhoneApp("com.facebook.system", "Facebook App Installer", "Servicio oculto de Facebook.", PhoneKind.ADS, true),
        PhoneApp("com.amazon.appmanager", "Amazon App Manager", "Instala promociones de Amazon.", PhoneKind.STORE, true),
        PhoneApp("com.netflix.partner.activation", "Activación de Netflix", "Servicio de promoción de Netflix.", PhoneKind.STORE, true),
        PhoneApp("com.microsoftsdk.crossdeviceservicebroker", "Vincular con Windows", "Servicio en segundo plano.", PhoneKind.UNUSED, true),
        PhoneApp("com.google.android.videos", "Google TV", "Películas de Google.", PhoneKind.DUPLICATE, true),
        PhoneApp("com.google.android.apps.subscriptions.red", "Google One", "Promoción de almacenamiento.", PhoneKind.STORE, true),
        PhoneApp("com.google.android.apps.tachyon", "Google Meet", "Videollamadas (ya tiene WhatsApp).", PhoneKind.DUPLICATE, true),
        PhoneApp("com.google.android.feedback", "Comentarios de Google", "Envío de comentarios.", PhoneKind.ADS, true),
        PhoneApp("com.google.android.printservice.recommendation", "Recomendación de impresoras", "Servicio de impresión.", PhoneKind.UNUSED, true),
        PhoneApp("com.google.android.apps.magazines", "Google Noticias", "Noticias de Google.", PhoneKind.UNUSED, false),
        PhoneApp("com.google.android.apps.podcasts", "Google Podcasts", "Podcasts de Google.", PhoneKind.UNUSED, false),
        PhoneApp("com.google.android.apps.youtube.music", "YouTube Music", "Música de YouTube.", PhoneKind.DUPLICATE, false),
        PhoneApp("com.google.android.projection.gearhead", "Android Auto", "Solo si no lo usa en el coche.", PhoneKind.UNUSED, false),
        PhoneApp("com.android.dreams.basic", "Salvapantallas básico", "Salvapantallas al cargar.", PhoneKind.UNUSED, true),
        PhoneApp("com.android.bookmarkprovider", "Favoritos antiguos", "Proveedor de favoritos en desuso.", PhoneKind.UNUSED, true),
    )

    private val packages: Set<String> = apps.map { it.pkg }.toSet()

    fun isAllowed(pkg: String): Boolean = pkg in packages

    private val command = Regex(
        "(pm uninstall -k --user 0|cmd package install-existing --user 0) ([A-Za-z][A-Za-z0-9_.]*)",
    )

    /** Única orden que acepta el servicio con permisos: quitar o devolver una app de la lista. */
    fun isAllowedCommand(cmd: String): Boolean {
        val m = command.matchEntire(cmd) ?: return false
        return isAllowed(m.groupValues[2])
    }

    fun removeCommand(pkg: String) = "pm uninstall -k --user 0 $pkg"

    fun restoreCommand(pkg: String) = "cmd package install-existing --user 0 $pkg"
}
