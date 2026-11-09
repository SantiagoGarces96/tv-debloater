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
import tv.clean.debloater.webos.WebosClient
import tv.clean.debloater.data.AppRow
import tv.clean.debloater.data.BackupRecord
import tv.clean.debloater.data.BackupStore
import tv.clean.debloater.data.Categorizer
import tv.clean.debloater.data.DebloatProfile
import tv.clean.debloater.data.DebloatRepository
import tv.clean.debloater.data.Telemetry
import tv.clean.debloater.data.UadEntry

enum class Stage { Platform, Connect, Packages, Working, LgInfo }

enum class Action { Disable, Uninstall }

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
)

class DebloaterViewModel(app: Application) : AndroidViewModel(app) {

    private val adb = AdbManager(app)
    private val repo = DebloatRepository(app)
    private val backups = BackupStore(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var installed: List<String> = emptyList()
    private var catalog: Map<String, String?> = emptyMap()
    private var uad: Map<String, UadEntry> = emptyMap()
    private var lgKey: String? = null

    fun chooseAndroid() = _state.update { it.copy(stage = Stage.Connect, error = null) }

    fun chooseLg() = _state.update { it.copy(stage = Stage.LgInfo) }

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
        val rows = installed.map { pkg ->
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

    /** Fase 2: instala un APK de launcher en la TV y lo fija como pantalla de inicio. */
    fun installLauncher(uri: Uri) {
        _state.update { it.copy(stage = Stage.Working, busy = true, log = emptyList(), workDone = false) }
        viewModelScope.launch {
            appendLog("Preparando el APK…")
            val file = copyUriToCache(uri)
            if (file == null) {
                appendLog("No se pudo leer el APK seleccionado.")
                _state.update { it.copy(busy = false, workDone = true) }
                return@launch
            }
            appendLog("Instalando en la TV (${file.length() / (1024 * 1024)} MB)…")
            when (val r = adb.installApk(file)) {
                is AdbResult.Ok -> {
                    appendLog("✓ APK instalado.")
                    appendLog("Fijando Projectivy como pantalla de inicio…")
                    when (val h = adb.setDefaultHome(PROJECTIVY_HOME)) {
                        is AdbResult.Ok -> appendLog("✓ Projectivy es ahora el inicio.")
                        is AdbResult.Err -> appendLog(
                            "APK instalado, pero no pude fijar el inicio: ${h.message}\n" +
                                "Ábrelo una vez en la TV y elígelo como inicio desde Ajustes.",
                        )
                    }
                }
                is AdbResult.Err -> appendLog("✗ Falló la instalación: ${r.message}")
            }
            runCatching { file.delete() }
            _state.update { it.copy(busy = false, workDone = true) }
        }
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
        const val PROJECTIVY_PKG = "com.spocky.projengmenu"
        const val PROJECTIVY_HOME = "com.spocky.projengmenu/com.spocky.projengmenu.ui.home.MainActivity"
        const val ADGUARD_DNS = "dns.adguard-dns.com"
    }
}
