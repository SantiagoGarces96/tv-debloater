package tv.clean.debloater.vm

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import tv.clean.debloater.phone.PhoneCleaner
import tv.clean.debloater.phone.PhoneRow
import tv.clean.debloater.phone.ShizukuState

data class PhoneUi(
    val model: String = "",
    val xiaomi: Boolean = false,
    val shizuku: ShizukuState = ShizukuState.MISSING,
    val rows: List<PhoneRow> = emptyList(),
    val selected: Set<String> = emptySet(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val confirm: Boolean = false,
    val message: String? = null,
)

class PhoneViewModel(app: Application) : AndroidViewModel(app) {

    private val cleaner = PhoneCleaner(app)
    private val _state = MutableStateFlow(
        PhoneUi(
            model = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            xiaomi = Regex("xiaomi|redmi|poco", RegexOption.IGNORE_CASE).containsMatchIn(Build.MANUFACTURER + Build.BRAND),
        ),
    )
    val state: StateFlow<PhoneUi> = _state.asStateFlow()
    private var picked = false

    private val onPermission = Shizuku.OnRequestPermissionResultListener { code, _ ->
        if (code == PhoneCleaner.REQUEST) refresh()
    }

    init {
        runCatching { Shizuku.addRequestPermissionResultListener(onPermission) }
    }

    fun refresh() {
        viewModelScope.launch {
            val rows = cleaner.scan()
            _state.update {
                it.copy(
                    shizuku = cleaner.state(),
                    rows = rows,
                    loading = false,
                    selected = if (picked) {
                        it.selected
                    } else {
                        rows.filter { r -> r.installed && r.app.recommended }.map { r -> r.app.pkg }.toSet()
                    },
                )
            }
            picked = true
        }
    }

    fun toggle(pkg: String) = _state.update {
        it.copy(selected = if (pkg in it.selected) it.selected - pkg else it.selected + pkg)
    }

    fun requestPermission() = cleaner.requestPermission()

    fun askRemove() = _state.update { it.copy(confirm = true) }

    fun cancel() = _state.update { it.copy(confirm = false) }

    fun remove() {
        val pkgs = _state.value.rows.filter { it.installed && it.app.pkg in _state.value.selected }.map { it.app.pkg }
        run(pkgs, true)
    }

    fun restore(pkg: String) = run(listOf(pkg), false)

    fun restoreAll() = run(_state.value.rows.filter { !it.installed }.map { it.app.pkg }, false)

    private fun run(pkgs: List<String>, remove: Boolean) {
        if (pkgs.isEmpty() || _state.value.busy) return
        _state.update { it.copy(confirm = false, busy = true, message = null) }
        viewModelScope.launch {
            val ok = cleaner.apply(pkgs, remove) { _, _ -> }
            val verb = if (remove) "quitadas" else "devueltas"
            val text = when {
                ok < 0 -> "No pude conectar con Shizuku. Ábrelo, pulsa «Iniciar» y vuelve aquí."
                ok == pkgs.size -> "Listo: $ok apps $verb."
                else -> "$ok apps $verb; ${pkgs.size - ok} no se pudieron cambiar."
            }
            _state.update { it.copy(busy = false, message = text, selected = it.selected - pkgs.toSet()) }
            refresh()
        }
    }

    override fun onCleared() {
        runCatching { Shizuku.removeRequestPermissionResultListener(onPermission) }
        cleaner.close()
    }
}
