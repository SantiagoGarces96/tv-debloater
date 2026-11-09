package tv.clean.debloater.data

/**
 * Heurística para marcar paquetes que suelen recopilar datos / mostrar anuncios / rastrear.
 * No es un antivirus: es una pista para priorizar. El malware de firmware NO se detecta aquí.
 */
object Telemetry {

    private val nameHints = listOf(
        "analytic", "telemetr", "tracker", "tracking", "advert", "adservice",
        ".ads", "ads.", ".ad.", "crashlytics", "metrics", "metric",
        "dataservice", "datacollection", "appsflyer", "agentservice",
        "partner", "firebase.analytics", "perfcollection", "wirelessmetrics",
        "acr", "blackbird", "hybridadid", "dtb", "oobe.analytics",
    )

    private val textHints = listOf(
        "telemetr", "rastre", "anuncio", "advert", "track", "analytic",
        "data collection", "recopil", "patrocinad", "sponsored", "uploads",
        "metric", "ad sync", "advertising", "bidding", "personaliz",
    )

    private val labelHints = listOf("telemetry", "ads", "tracker", "analytics")

    fun isTelemetry(
        pkg: String,
        reason: String?,
        description: String?,
        labels: List<String>,
    ): Boolean {
        val p = pkg.lowercase()
        if (nameHints.any { p.contains(it) }) return true
        if (labels.any { l -> labelHints.any { l.contains(it, ignoreCase = true) } }) return true
        val text = ((reason ?: "") + " " + (description ?: "")).lowercase()
        return textHints.any { text.contains(it) }
    }
}
