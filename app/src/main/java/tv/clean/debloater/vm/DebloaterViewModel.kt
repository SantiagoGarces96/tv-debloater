package tv.clean.debloater.vm

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import tv.clean.debloater.adb.AdbManager
import tv.clean.debloater.adb.AdbResult
import tv.clean.debloater.adb.DeviceInfo
import tv.clean.debloater.adb.TvDiscovery
import tv.clean.debloater.tizen.SamsungClient
import tv.clean.debloater.webos.WebosClient
import tv.clean.debloater.data.AppRow
import tv.clean.debloater.data.BackupRecord
import tv.clean.debloater.data.BackupStore
import tv.clean.debloater.data.AppGroup
import tv.clean.debloater.data.AutoRulesDefaults
import tv.clean.debloater.data.CatalogApp
import tv.clean.debloater.net.ApkDownloader
import tv.clean.debloater.data.Categorizer
import tv.clean.debloater.data.DebloatProfile
import tv.clean.debloater.data.DebloatRepository
import tv.clean.debloater.data.Prefs
import tv.clean.debloater.data.Protected
import tv.clean.debloater.data.Telemetry
import tv.clean.debloater.data.UadEntry

enum class Stage { Platform, Connect, Packages, Working, LgInfo, SamsungInfo, Downloads, Automation }

/** APK ya descargado y verificado, esperando que el usuario confirme la instalación. */
data class PendingApk(
    val label: String,
    val path: String,
    val host: String,
    val mb: Long,
    val fromCode: Boolean,
)

enum class Action { Disable, Uninstall }

/** Confirmación pendiente antes de aplicar cambios. */
data class Confirm(val count: Int, val uninstall: Boolean)

data class UiState(
    val stage: Stage = Stage.Platform,
    val hostInput: String = "",
    val busy: Boolean = false,
    val device: DeviceInfo? = null,
    val error: String? = null,
    val profiles: List<DebloatProfile> = emptyList(),
    val selectedProfileId: String? = null,
    val rows: List<AppRow> = emptyList(),
    val selected: Set<String> = emptySet(),
    val log: List<String> = emptyList(),
    val workDone: Boolean = false,
    val hasBackup: Boolean = false,
    val action: Action = Action.Disable,
    val confirm: Confirm? = null,
    val adBlockHost: String? = null,
    val discovering: Boolean = false,
    val found: List<String> = emptyList(),
    // LG webOS
    val lgIp: String = "",
    val lgPassphrase: String = "",
    val lgBusy: Boolean = false,
    val lgDetail: String? = null,
    val lgKeyReady: Boolean = false,
    val lgLog: List<String> = emptyList(),
    // Samsung Tizen
    val samsungIp: String = "",
    val samsungBusy: Boolean = false,
    val samsungDetail: String? = null,
    // Descargar e instalar
    val dlCatalog: List<CatalogApp> = emptyList(),
    val dlInput: String = "",
    val dlBusy: Boolean = false,
    val dlLog: List<String> = emptyList(),
    val dlInstalled: Set<String> = emptySet(),
    val dlPending: PendingApk? = null,
    // Cerrar avisos automáticamente
    val autoRules: String = "",
    val autoEnabled: Boolean? = null,
    val autoBusy: Boolean = false,
    val autoLog: List<String> = emptyList(),
)

class DebloaterViewModel(app: Application) : AndroidViewModel(app) {

    private val adb = AdbManager(app)
    private val repo = DebloatRepository(app)
    private val backups = BackupStore(app)
    private val prefs = Prefs(app)

    private val _state = MutableStateFlow(UiState(hostInput = prefs.lastHost))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var installed: List<String> = emptyList()
    private var catalog: Map<String, String?> = emptyMap()
    private var uad: Map<String, UadEntry> = emptyMap()
    private var lgKey: String? = null

    fun chooseAndroid() = _state.update { it.copy(stage = Stage.Connect, error = null) }

    fun chooseLg() = _state.update { it.copy(stage = Stage.LgInfo) }

    fun chooseSamsung() = _state.update { it.copy(stage = Stage.SamsungInfo) }

    fun samsungOnIp(value: String) = _state.update { it.copy(samsungIp = value, samsungDetail = null) }

    fun samsungCheck() {
        _state.update { it.copy(samsungBusy = true, samsungDetail = null) }
        viewModelScope.launch {
            val status = SamsungClient.check(_state.value.samsungIp)
            _state.update { it.copy(samsungBusy = false, samsungDetail = status.detail) }
        }
    }

    fun backToPlatform() = _state.update { it.copy(stage = Stage.Platform, error = null, found = emptyList()) }

    fun lgOnIp(value: String) = _state.update { it.copy(lgIp = value, lgDetail = null) }

    fun lgOnPassphrase(value: String) = _state.update { it.copy(lgPassphrase = value, lgDetail = null) }

    fun lgCheck() {
        _state.update { it.copy(lgBusy = true, lgDetail = null, lgKeyReady = false) }
        viewModelScope.launch {
            val status = WebosClient.check(_state.value.lgIp, _state.value.lgPassphrase)
            lgKey = status.devKeyPem
            _state.update {
                it.copy(
                    lgBusy = false,
                    lgDetail = status.detail,
                    lgKeyReady = status.devKeyPem != null,
                )
            }
        }
    }

    fun lgInstall(uri: Uri) {
        val key = lgKey
        if (key == null) {
            _state.update { it.copy(lgDetail = "Primero comprueba la conexión para obtener la clave.") }
            return
        }
        _state.update { it.copy(lgBusy = true, lgLog = emptyList()) }
        viewModelScope.launch {
            val file = copyUriToCache(uri, "launcher.ipk")
            if (file == null) {
                appendLgLog("No se pudo leer el IPK seleccionado.")
                _state.update { it.copy(lgBusy = false) }
                return@launch
            }
            WebosClient.installIpk(_state.value.lgIp, key, file) { line -> appendLgLog(line) }
            runCatching { file.delete() }
            _state.update { it.copy(lgBusy = false) }
        }
    }

    private fun appendLgLog(line: String) = _state.update { it.copy(lgLog = it.lgLog + line) }

    fun onHostChange(value: String) = _state.update { it.copy(hostInput = value, error = null) }

    fun scanNetwork() {
        _state.update { it.copy(discovering = true, found = emptyList(), error = null) }
        viewModelScope.launch {
            val ips = TvDiscovery.scan()
            _state.update {
                it.copy(
                    discovering = false,
                    found = ips,
                    error = if (ips.isEmpty()) {
                        "No encontré TVs con depuración por red. Verifica que esté activada y en la misma Wi‑Fi."
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun pickFound(ip: String) {
        _state.update { it.copy(hostInput = ip) }
        connect()
    }

    fun connect() {
        val host = _state.value.hostInput.trim()
        if (host.isEmpty()) {
            _state.update { it.copy(error = "Escribe la IP de la TV.") }
            return
        }
        val (ip, port) = parseHostPort(host)
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = adb.connect(ip, port)) {
                is AdbResult.Ok -> {
                    prefs.lastHost = ip
                    val profiles = repo.profiles()
                    catalog = repo.mergedCatalog()
                    uad = repo.uadDatabase()
                    val backup = backups.latestForHost(ip)
                    val dns = adb.currentPrivateDns()
                    _state.update {
                        it.copy(
                            busy = false,
                            device = result.value,
                            profiles = profiles,
                            stage = Stage.Packages,
                            hasBackup = backup != null,
                            adBlockHost = dns,
                        )
                    }
                    loadPackages()
                }
                is AdbResult.Err -> _state.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    fun loadPackages() {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = adb.listPackages()) {
                is AdbResult.Ok -> {
                    installed = result.value
                    rebuildRows()
                    _state.update { it.copy(busy = false) }
                }
                is AdbResult.Err -> _state.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    fun selectProfile(id: String?) {
        _state.update { it.copy(selectedProfileId = id) }
        rebuildRows(preselect = true)
    }

    private fun rebuildRows(preselect: Boolean = true) {
        val profile = _state.value.profiles.firstOrNull { it.id == _state.value.selectedProfileId }
        val profileEntries = profile?.entries ?: emptyMap()
        val rows = installed.filterNot { Protected.isProtected(it) }.map { pkg ->
            val reason = profileEntries[pkg] ?: catalog[pkg]
            val u = uad[pkg]
            val description = u?.description?.takeIf { it.isNotBlank() }
            val telemetry = Telemetry.isTelemetry(pkg, reason, description, u?.labels ?: emptyList())
            AppRow(
                packageName = pkg,
                reason = reason,
                inSelectedProfile = pkg in profileEntries,
                inAnyList = pkg in catalog,
                description = description,
                risk = u?.removal?.takeIf { it.isNotBlank() },
                isTelemetry = telemetry,
                group = Categorizer.of(pkg, reason, description, u, telemetry),
            )
        }.sortedWith(
            compareByDescending<AppRow> { it.inSelectedProfile }
                .thenByDescending { it.isTelemetry }
                .thenByDescending { it.inAnyList }
                .thenBy { it.packageName },
        )
        val selected = if (preselect) {
            rows.filter { it.inSelectedProfile }.map { it.packageName }.toSet()
        } else {
            _state.value.selected
        }
        _state.update { it.copy(rows = rows, selected = selected) }
    }

    fun toggle(pkg: String) = _state.update {
        val s = it.selected.toMutableSet()
        if (!s.add(pkg)) s.remove(pkg)
        it.copy(selected = s)
    }

    fun selectRecommended() = _state.update {
        it.copy(selected = it.rows.filter { r -> r.inAnyList }.map { r -> r.packageName }.toSet())
    }

    fun selectTelemetry() = _state.update {
        it.copy(selected = it.rows.filter { r -> r.isTelemetry }.map { r -> r.packageName }.toSet())
    }

    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    /** Limpieza en un toque: desactiva (reversible) lo claramente molesto. */
    fun cleanNow() {
        val targets = _state.value.rows
            .filter {
                it.inSelectedProfile ||
                    it.group == AppGroup.ADS ||
                    it.group == AppGroup.TRACKING ||
                    it.group == AppGroup.BRAND
            }
            .map { it.packageName }
            .toSet()
        if (targets.isEmpty()) return
        _state.update { it.copy(selected = targets, action = Action.Disable, confirm = Confirm(targets.size, false)) }
    }

    /** Pide confirmar la acción manual (desactivar/desinstalar lo seleccionado). */
    fun requestApply() {
        val n = _state.value.selected.size
        if (n == 0) return
        _state.update { it.copy(confirm = Confirm(n, it.action == Action.Uninstall)) }
    }

    fun cancelConfirm() = _state.update { it.copy(confirm = null) }

    fun confirmProceed() {
        _state.update { it.copy(confirm = null) }
        runAction()
    }

    fun setAction(action: Action) = _state.update { it.copy(action = action) }

    fun runAction() {
        val targets = _state.value.selected.toList()
        if (targets.isEmpty()) return
        val action = _state.value.action
        _state.update { it.copy(stage = Stage.Working, busy = true, log = emptyList(), workDone = false) }
        viewModelScope.launch {
            val done = mutableListOf<String>()
            var i = 0
            for (pkg in targets) {
                i++
                val r = when (action) {
                    Action.Disable -> adb.disableForUser0(pkg)
                    Action.Uninstall -> adb.uninstallForUser0(pkg)
                }
                when (r) {
                    is AdbResult.Ok -> {
                        done += pkg
                        appendLog("[$i/${targets.size}] ✓ $pkg")
                    }
                    is AdbResult.Err -> appendLog("[$i/${targets.size}] ✗ $pkg — ${r.message}")
                }
            }
            if (done.isNotEmpty()) {
                val dev = _state.value.device
                backups.save(
                    BackupRecord(
                        host = adb.host ?: "",
                        model = dev?.model ?: "",
                        timestamp = System.currentTimeMillis(),
                        removed = done,
                        action = if (action == Action.Disable) "disable" else "uninstall",
                    ),
                )
                appendLog("Respaldo guardado (${done.size} paquetes).")
            }
            // Si se desinstalaron, salen del listado; si solo se desactivaron, siguen instalados.
            if (action == Action.Uninstall) {
                installed = installed.filterNot { it in done }
                rebuildRows(preselect = false)
            }
            _state.update { it.copy(busy = false, workDone = true, hasBackup = true) }
        }
    }

    fun toggleAdBlock() {
        val on = _state.value.adBlockHost != null
        _state.update { it.copy(stage = Stage.Working, busy = true, log = emptyList(), workDone = false) }
        viewModelScope.launch {
            if (on) {
                appendLog("Desactivando bloqueo de anuncios (DNS privado)…")
                when (val r = adb.clearPrivateDns()) {
                    is AdbResult.Ok -> {
                        appendLog("✓ Bloqueo de anuncios desactivado.")
                        _state.update { it.copy(adBlockHost = null) }
                    }
                    is AdbResult.Err -> appendLog("✗ ${r.message}")
                }
            } else {
                appendLog("Activando bloqueo de anuncios con $ADGUARD_DNS…")
                when (val r = adb.setPrivateDns(ADGUARD_DNS)) {
                    is AdbResult.Ok -> {
                        appendLog("✓ Anuncios y rastreadores bloqueados en toda la TV.")
                        _state.update { it.copy(adBlockHost = ADGUARD_DNS) }
                    }
                    is AdbResult.Err -> appendLog("✗ ${r.message}")
                }
            }
            _state.update { it.copy(busy = false, workDone = true) }
        }
    }

    fun restoreLatest() {
        _state.update { it.copy(stage = Stage.Working, busy = true, log = emptyList(), workDone = false) }
        viewModelScope.launch {
            val host = adb.host ?: ""
            val record = backups.latestForHost(host)
            if (record == null) {
                appendLog("No hay respaldo para $host.")
                _state.update { it.copy(busy = false, workDone = true) }
                return@launch
            }
            var i = 0
            for (pkg in record.removed) {
                i++
                val r = if (record.action == "disable") adb.enableForUser0(pkg)
                else adb.reinstallExisting(pkg)
                when (r) {
                    is AdbResult.Ok -> appendLog("[$i/${record.removed.size}] ✓ $pkg")
                    is AdbResult.Err -> appendLog("[$i/${record.removed.size}] ✗ $pkg — ${r.message}")
                }
            }
            appendLog("Restauración terminada.")
            _state.update { it.copy(busy = false, workDone = true) }
            loadPackages()
        }
    }

    /** Instala NUESTRO launcher (empaquetado en la app) y lo pone como pantalla de inicio. */
    fun installOwnLauncher() {
        _state.update { it.copy(stage = Stage.Working, busy = true, log = emptyList(), workDone = false) }
        viewModelScope.launch {
            appendLog("Preparando la pantalla de inicio…")
            val file = copyAssetToCache("launcher/clean-home.apk", "clean-home.apk")
            if (file == null) {
                appendLog("No encontré el launcher dentro de la app.")
                _state.update { it.copy(busy = false, workDone = true) }
                return@launch
            }
            appendLog("Instalando en la tele…")
            when (val r = adb.installApk(file)) {
                is AdbResult.Ok -> {
                    appendLog("✓ Instalada.")
                    appendLog("Poniéndola como pantalla de inicio…")
                    when (val h = adb.setDefaultHome(CLEAN_HOME_COMPONENT)) {
                        is AdbResult.Ok -> appendLog("✓ Lista: tu tele abre la nueva pantalla de inicio.")
                        is AdbResult.Err -> appendLog(
                            "Instalada, pero no pude fijarla: ${h.message}\n" +
                                "Ábrela una vez en la tele y elígela como inicio.",
                        )
                    }
                }
                is AdbResult.Err -> appendLog("✗ Falló la instalación: ${r.message}")
            }
            runCatching { file.delete() }
            _state.update { it.copy(busy = false, workDone = true) }
        }
    }

    private suspend fun copyAssetToCache(assetPath: String, name: String): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val ctx = getApplication<Application>()
                val file = File(ctx.cacheDir, name)
                ctx.assets.open(assetPath).use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file
            }.getOrNull()
        }

    private suspend fun copyUriToCache(uri: Uri, name: String = "launcher.apk"): File? = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = getApplication<Application>()
            val file = File(ctx.cacheDir, name)
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            file
        }.getOrNull()
    }

    fun backToPackages() = _state.update { it.copy(stage = Stage.Packages, log = emptyList()) }

    // ---------- Cerrar avisos automáticamente ----------

    /** Comprueba en la tele (no en la lista guardada) si la pantalla de inicio SG Alfa está instalada. */
    private suspend fun launcherInstalled(): Boolean {
        val list = adb.listPackages()
        if (list is AdbResult.Ok) installed = list.value
        return "tv.clean.home" in installed
    }

    private val needLauncherMsg =
        "Primero instala la pantalla de inicio SG Alfa (botón «Poner una pantalla de inicio limpia»): el servicio viene dentro de ella."

    fun openAutomation() {
        val saved = prefs.autoRules
        _state.update {
            it.copy(
                stage = Stage.Automation,
                autoRules = it.autoRules.ifBlank { saved.ifBlank { AutoRulesDefaults.TEXT } },
                autoLog = emptyList(),
            )
        }
        viewModelScope.launch {
            val enabled = adb.isAutoClickEnabled()
            _state.update { it.copy(autoEnabled = enabled) }
        }
    }

    fun onAutoRules(value: String) = _state.update { it.copy(autoRules = value) }

    fun resetAutoRules() = _state.update { it.copy(autoRules = AutoRulesDefaults.TEXT) }

    private fun appendAuto(line: String) = _state.update { it.copy(autoLog = it.autoLog + line) }

    fun toggleAutomation() {
        val enable = _state.value.autoEnabled != true
        viewModelScope.launch {
            _state.update { it.copy(autoBusy = true, autoLog = emptyList()) }
            if (enable && !launcherInstalled()) {
                appendAuto(needLauncherMsg)
                _state.update { it.copy(autoBusy = false) }
                return@launch
            }
            if (enable) {
                sendRules()
            }
            when (val r = adb.setAutoClickEnabled(enable)) {
                is AdbResult.Ok -> appendAuto(if (enable) "✓ Activado: la tele pulsará sola esos botones." else "✓ Desactivado.")
                is AdbResult.Err -> appendAuto("✗ ${r.message}")
            }
            _state.update { it.copy(autoBusy = false, autoEnabled = adb.isAutoClickEnabled()) }
        }
    }

    fun saveAutoRules() {
        viewModelScope.launch {
            _state.update { it.copy(autoBusy = true, autoLog = emptyList()) }
            if (!launcherInstalled()) {
                appendAuto(needLauncherMsg)
                _state.update { it.copy(autoBusy = false) }
                return@launch
            }
            sendRules()
            _state.update { it.copy(autoBusy = false) }
        }
    }

    private suspend fun sendRules() {
        val text = _state.value.autoRules
        prefs.autoRules = text
        when (val r = adb.pushAutoClickRules(text)) {
            is AdbResult.Ok -> appendAuto("✓ ${r.value} reglas guardadas en la tele.")
            is AdbResult.Err -> appendAuto("✗ No se pudieron guardar las reglas: ${r.message}")
        }
    }

    // ---------- Descargar e instalar ----------

    fun openDownloads() {
        _state.update { it.copy(stage = Stage.Downloads, dlInstalled = installed.toSet()) }
        if (_state.value.dlCatalog.isEmpty()) {
            viewModelScope.launch {
                val apps = repo.appCatalog()
                _state.update { it.copy(dlCatalog = apps) }
            }
        }
    }

    fun onDlInput(value: String) = _state.update { it.copy(dlInput = value) }

    private fun appendDl(line: String) = _state.update { it.copy(dlLog = it.dlLog + line) }

    /** Descarga desde lo escrito: una URL o un código de AFTVnews. */
    fun downloadFromInput() {
        val input = _state.value.dlInput
        val url = ApkDownloader.toUrl(input)
        if (url == null) {
            _state.update { it.copy(dlLog = listOf("Escribe una URL o un código.")) }
            return
        }
        val fromCode = ApkDownloader.isCode(input)
        val label = if (fromCode) "el código ${input.trim()}" else "el enlace"
        viewModelScope.launch {
            _state.update { it.copy(dlBusy = true, dlLog = emptyList()) }
            downloadAndAsk(label, url, fromCode)
        }
    }

    fun installFromCatalog(app: CatalogApp) {
        viewModelScope.launch {
            _state.update { it.copy(dlBusy = true, dlLog = emptyList()) }
            when (app.type) {
                "playstore" -> {
                    appendDl("Abriendo ${app.name} en la Play Store de la tele…")
                    when (val r = adb.openPlayStore(app.pkg)) {
                        is AdbResult.Ok -> appendDl("✓ Abierta en la tele: pulsa «Instalar» con el control.")
                        is AdbResult.Err -> appendDl("✗ ${r.message}")
                    }
                    _state.update { it.copy(dlBusy = false) }
                }
                "github" -> {
                    appendDl("Buscando la última versión oficial de ${app.name}…")
                    val url = ApkDownloader.latestGithubApk(app.repo, app.assetPattern)
                    if (url == null) {
                        appendDl("✗ No encontré un APK en la última versión publicada.")
                        _state.update { it.copy(dlBusy = false) }
                    } else {
                        downloadAndAsk(app.name, url, fromCode = false)
                    }
                }
                "url" -> downloadAndAsk(app.name, app.url, fromCode = false)
                "code" -> {
                    val url = ApkDownloader.toUrl(app.code)
                    if (url == null) _state.update { it.copy(dlBusy = false) }
                    else downloadAndAsk(app.name, url, fromCode = true)
                }
                else -> {
                    appendDl("Tipo de entrada desconocido: ${app.type}")
                    _state.update { it.copy(dlBusy = false) }
                }
            }
        }
    }

    private suspend fun downloadAndAsk(label: String, url: String, fromCode: Boolean) {
        val dest = File(getApplication<Application>().cacheDir, "download.apk")
        appendDl("Descargando $label…")
        when (val r = ApkDownloader.download(url, dest) { appendDl(it) }) {
            is ApkDownloader.Result.Ok -> {
                val host = runCatching { java.net.URL(r.finalUrl).host }.getOrDefault(r.finalUrl)
                appendDl("✓ APK verificado (${r.bytes / 1_048_576} MB), viene de $host.")
                _state.update {
                    it.copy(
                        dlBusy = false,
                        dlPending = PendingApk(label, dest.absolutePath, host, r.bytes / 1_048_576, fromCode),
                    )
                }
            }
            is ApkDownloader.Result.Err -> {
                appendDl("✗ ${r.message}")
                _state.update { it.copy(dlBusy = false) }
            }
        }
    }

    fun confirmInstallPending() {
        val p = _state.value.dlPending ?: return
        _state.update { it.copy(dlPending = null, dlBusy = true) }
        viewModelScope.launch {
            val file = File(p.path)
            appendDl("Instalando ${p.label} en la tele…")
            when (val r = adb.installApk(file)) {
                is AdbResult.Ok -> appendDl("✓ Instalada. Ya la tienes en la tele.")
                is AdbResult.Err -> appendDl("✗ Falló la instalación: ${r.message}")
            }
            runCatching { file.delete() }
            val list = adb.listPackages()
            if (list is AdbResult.Ok) {
                installed = list.value
                rebuildRows(preselect = false)
                _state.update { it.copy(dlInstalled = list.value.toSet()) }
            }
            _state.update { it.copy(dlBusy = false) }
        }
    }

    fun cancelPending() {
        _state.value.dlPending?.let { runCatching { File(it.path).delete() } }
        _state.update { it.copy(dlPending = null) }
        appendDl("Instalación cancelada.")
    }

    fun disconnect() {
        adb.close()
        installed = emptyList()
        catalog = emptyMap()
        _state.value = UiState()
    }

    private fun appendLog(line: String) = _state.update { it.copy(log = it.log + line) }

    private fun parseHostPort(input: String): Pair<String, Int> {
        val parts = input.split(":")
        return if (parts.size == 2) parts[0].trim() to (parts[1].trim().toIntOrNull() ?: 5555)
        else input.trim() to 5555
    }

    override fun onCleared() {
        adb.close()
        super.onCleared()
    }

    companion object {
        const val CLEAN_HOME_COMPONENT = "tv.clean.home/tv.clean.home.HomeActivity"
        const val ADGUARD_DNS = "dns.adguard-dns.com"
    }
}
