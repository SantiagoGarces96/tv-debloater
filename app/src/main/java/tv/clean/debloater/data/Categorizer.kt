package tv.clean.debloater.data

/** Grupos en los que se clasifica cada app para mostrarlas ordenadas. */
enum class AppGroup(val label: String) {
    ADS("📣 Anuncios"),
    TRACKING("🔎 Te espían"),
    BRAND("🏷 Extras de marca"),
    APPS("📦 Apps de sobra"),
    OTHER("Otras"),
}

object Categorizer {

    /** Cuenta cuántas apps hay en cada grupo. */
    fun summarize(rows: List<AppRow>): Map<AppGroup, Int> =
        rows.groupingBy { it.group }.eachCount()


    private val adsName = listOf(
        "advert", "adservice", ".ads", "ads.", "bidding", "sponsored",
        "promo", "hybridadid", "blackbird", "dtb", "kso",
    )
    private val adsText = listOf("anuncio", "advert", "sponsored", "patrocinad", "bidding", "ad sync", "advertising")

    private val brandName = listOf(
        "partner", "preload", "preinstall", "oobe", "demo", "factorytest",
        "factory", "agentservice", "upgrade", "fhstbconfig", "stbcheck",
    )
    private val brandText = listOf("partner", "fabricante", "demo", "preinstal", "operador", "tienda")

    fun of(
        pkg: String,
        reason: String?,
        description: String?,
        uad: UadEntry?,
        isTelemetry: Boolean,
    ): AppGroup {
        val p = pkg.lowercase()
        val t = ((reason ?: "") + " " + (description ?: "")).lowercase()

        if (adsName.any { p.contains(it) } || adsText.any { t.contains(it) }) return AppGroup.ADS
        if (isTelemetry) return AppGroup.TRACKING
        if (uad?.list.equals("Oem", true) ||
            brandName.any { p.contains(it) } ||
            brandText.any { t.contains(it) }
        ) {
            return AppGroup.BRAND
        }
        if (uad?.list.equals("Google", true) || uad?.list.equals("Misc", true)) return AppGroup.APPS
        return AppGroup.OTHER
    }
}
