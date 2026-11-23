package tv.clean.debloater.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tv.clean.debloater.phone.PhoneCleaner
import tv.clean.debloater.phone.PhoneKind
import tv.clean.debloater.phone.PhoneRow
import tv.clean.debloater.phone.ShizukuState
import tv.clean.debloater.vm.PhoneUi
import tv.clean.debloater.vm.PhoneViewModel

private fun Context.open(intent: Intent): Boolean =
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

private fun Context.openShizuku() {
    val app = packageManager.getLaunchIntentForPackage(PhoneCleaner.SHIZUKU_PKG)
    if (app != null && open(app)) return
    val id = PhoneCleaner.SHIZUKU_PKG
    if (!open(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")))) {
        open(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")))
    }
}

/** Limpieza de ESTE celular. */
@Composable
fun PhoneScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: PhoneViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler { onBack() }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    val installed = state.rows.filter { it.installed }
    val removed = state.rows.filter { !it.installed }
    val count = installed.count { it.app.pkg in state.selected }
    val ready = state.shizuku == ShizukuState.READY && !state.busy

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Este celular", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(state.model, style = MaterialTheme.typography.bodyMedium)
            }
            item { ShizukuCard(state, vm) }
            state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (!state.loading && installed.isEmpty()) {
                item { Text("No encontré apps basura de la lista en este celular.") }
            }
            PhoneKind.entries.forEach { kind ->
                val rows = installed.filter { it.app.kind == kind }
                if (rows.isEmpty()) return@forEach
                item(key = "k-${kind.name}") {
                    Text("${kind.label} (${rows.size})", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                }
                items(rows, key = { it.app.pkg }) { r ->
                    AppRowItem(r, r.app.pkg in state.selected) { vm.toggle(r.app.pkg) }
                }
            }
            if (removed.isNotEmpty()) {
                item(key = "removed") {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Quitadas (${removed.size})", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = vm::restoreAll, enabled = ready) { Text("Devolver todas") }
                    }
                }
                items(removed, key = { it.app.pkg }) { r ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.app.name, Modifier.weight(1f))
                        TextButton(onClick = { vm.restore(r.app.pkg) }, enabled = ready) { Text("Devolver") }
                    }
                }
            }
            item { TextButton(onClick = onBack) { Text("← Volver") } }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(
            onClick = vm::askRemove,
            enabled = ready && count > 0,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text(if (count == 0) "Elige qué quitar" else "Quitar $count apps") }
    }

    if (state.confirm) {
        AlertDialog(
            onDismissRequest = vm::cancel,
            title = { Text("¿Quitar $count apps?") },
            text = { Text("Se quitan solo para ti y las puedes devolver desde esta misma pantalla. No se borra nada del sistema.") },
            confirmButton = { TextButton(onClick = vm::remove) { Text("Quitar") } },
            dismissButton = { TextButton(onClick = vm::cancel) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun AppRowItem(row: PhoneRow, checked: Boolean, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(Modifier.padding(end = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column {
                Text(row.app.name, fontWeight = FontWeight.SemiBold)
                Text(
                    row.app.why + if (row.app.recommended) "" else " · Opcional",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ShizukuCard(state: PhoneUi, vm: PhoneViewModel) {
    val ctx = LocalContext.current
    if (state.shizuku == ShizukuState.READY) {
        Text("✓ Shizuku conectado: listo para limpiar.", color = MaterialTheme.colorScheme.primary)
        return
    }
    val xiaomi = if (state.xiaomi) {
        "\n\nEn Xiaomi activa también «Depuración USB (ajustes de seguridad)» en Opciones de desarrollador."
    } else {
        ""
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val (title, text, action) = when (state.shizuku) {
                ShizukuState.MISSING -> Triple(
                    "Falta Shizuku",
                    "Es una app gratuita que permite quitar apps del sistema sin PC y sin root. Instálala desde Play Store.",
                    "Instalar Shizuku",
                )
                ShizukuState.STOPPED -> Triple(
                    "Inicia Shizuku",
                    "Ábrelo y sigue «Iniciar mediante depuración inalámbrica». Hay que hacerlo después de cada reinicio.$xiaomi",
                    "Abrir Shizuku",
                )
                else -> Triple("Falta un permiso", "Shizuku está activo. Permite que SG Alfa lo use.$xiaomi", "Permitir")
            }
            Text(title, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = { if (state.shizuku == ShizukuState.DENIED) vm.requestPermission() else ctx.openShizuku() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(action) }
            if (state.shizuku != ShizukuState.DENIED) {
                OutlinedButton(
                    onClick = { ctx.open(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Opciones de desarrollador") }
            }
        }
    }
}
