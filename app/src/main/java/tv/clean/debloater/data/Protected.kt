package tv.clean.debloater.data

/**
 * Paquetes ESENCIALES que nunca se deben desactivar ni borrar: romperían la TV
 * (Ajustes, interfaz del sistema, launcher, teclado, proveedores, servicios de Google, radios…).
 * La app los oculta de la lista y nunca los selecciona.
 */
object Protected {

    // Coincidencia exacta.
    private val exact = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.settings.intelligence",
        "com.android.tv.settings",
        "com.android.phone",
        "com.android.shell",
        "com.android.keychain",
        "com.android.certinstaller",
        "com.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.android.vending",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.google.android.webview",
        "com.android.systemui.tv",
    )

    // Coincidencia por fragmento del nombre.
    private val contains = listOf(
        "settings",
        "systemui",
        "launcher",
        "inputmethod",
        ".ime",
        "packageinstaller",
        "permissioncontroller",
        "providers.",
        "keychain",
        "certinstaller",
        "com.google.android.gms",
        "com.google.android.gsf",
        "telephony",
        "telecom",
        "com.android.phone",
        "bluetooth",
        ".wifi",
        "wifi.",
        ".nfc",
        "webview",
        "networkstack",
        "captiveportal",
        "com.android.se",
        "vpndialogs",
        "cameraprovider",
        "setupwizard",
        "com.android.cts",
        "com.android.server",
        // Almacenamiento y archivos (esenciales para el deco/TV)
        "storage",
        "externalstorage",
        "documentsui",
        "mediaprovider",
        "media.module",
        "downloads",
        "mtp",
        "sharedstoragebackup",
        // Otros esenciales del sistema
        "com.android.location",
        "fused",
        "com.google.android.ext",
        "dynsystem",
        "emergency",
        "com.android.intentresolver",
        "ondevicepersonalization",
        "com.android.devicelockcontroller",
        // Hardware del deco: gráficos, red, mando, HDMI y audio (p. ej. ARRIS / Broadcom)
        "gfxdriver",
        "wifi",
        "tethering",
        "remoteservice",
        ".rcu",
        "hdmi",
        "dolby",
        "audio.service",
        "inputdevices",
        "managedprovisioning",
        "modulemetadata",
        "propertyservice",
        "chmframework",
        ".overlay",
    )

    fun isProtected(pkg: String): Boolean {
        val p = pkg.lowercase()
        if (p in exact) return true
        return contains.any { p.contains(it) }
    }
}
