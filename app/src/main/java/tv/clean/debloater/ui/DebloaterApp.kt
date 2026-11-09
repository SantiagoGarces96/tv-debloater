package tv.clean.debloater.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.clean.debloater.data.AppGroup
import tv.clean.debloater.data.AppRow
import tv.clean.debloater.vm.Action
import tv.clean.debloater.vm.DebloaterViewModel
import tv.clean.debloater.vm.Stage
import tv.clean.debloater.vm.UiState

@Composable
fun DebloaterApp(state: UiState, vm: DebloaterViewModel, modifier: Modifier = Modifier) {
    when (state.stage) {
        Stage.Platform -> PlatformScreen(vm, modifier)
        Stage.Connect -> ConnectScreen(state, vm, modifier)
        Stage.Packages -> PackagesScreen(state, vm, modifier)
        Stage.Working -> WorkingScreen(state, vm, modifier)
        Stage.LgInfo -> LgInfoScreen(vm, modifier)
    }
}

@Composable
private fun PlatformScreen(vm: DebloaterViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Clean TV", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("¿Qué tipo de televisor o decodificador tienes?", style = MaterialTheme.typography.bodyMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📺 Android TV / Google TV / Decodificador", fontWeight = FontWeight.Bold)
                Text(
                    "Quita bloatware, telemetría y anuncios por ADB; bloqueo de anuncios por DNS y launcher limpio.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = vm::chooseAndroid, modifier = Modifier.fillMaxWidth()) {
                    Text("Continuar con Android")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📺 LG (webOS)", fontWeight = FontWeight.Bold)
                Text(
                    "Las LG no son Android ni usan ADB. Se limpian con un launcher propio vía Modo Desarrollador.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = vm::chooseLg, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver opción para LG")
                }
            }
        }
    }
}

@Composable
private fun LgInfoScreen(vm: DebloaterViewModel, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    val project = "https://github.com/Razdva122/clean-your-tv-lg-webos"
    val installer = "https://github.com/Razdva122/clean-your-tv-lg-webos/releases/latest"
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("LG (webOS)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Por qué es distinto", fontWeight = FontWeight.SemiBold)
                Text(
                    "LG webOS no es Android: no hay ADB ni se pueden desinstalar servicios del sistema como aquí. " +
                        "Lo que sí se puede es poner un launcher limpio (sin filas patrocinadas) usando el " +
                        "Modo Desarrollador de LG.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cómo hacerlo", fontWeight = FontWeight.SemiBold)
                Text(
                    "Hay una app Android dedicada (open source, MIT) que lo instala desde el teléfono: " +
                        "activas Modo Desarrollador en la TV, metes la clave de 6 caracteres y la app instala el launcher.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Button(onClick = { uriHandler.openUri(installer) }, modifier = Modifier.fillMaxWidth()) {
            Text("Descargar la app para LG (webOS)")
        }
        OutlinedButton(onClick = { uriHandler.openUri(project) }, modifier = Modifier.fillMaxWidth()) {
            Text("Ver el proyecto en GitHub")
        }
        TextButton(onClick = vm::backToPlatform, modifier = Modifier.fillMaxWidth()) {
            Text("Volver")
        }
    }
}

@Composable
private fun ConnectScreen(state: UiState, vm: DebloaterViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Clean TV", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Conecta con tu Android/Google TV por ADB sobre Wi‑Fi y quita el bloatware.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Antes de conectar", fontWeight = FontWeight.SemiBold)
                Text(
                    "1. En la TV: Ajustes → Acerca de → pulsa 7 veces en Build.\n" +
                        "2. Opciones de desarrollador → Depuración USB y Depuración por red.\n" +
                        "3. Teléfono y TV en la misma red Wi‑Fi.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        OutlinedButton(
            onClick = vm::scanNetwork,
            enabled = !state.busy && !state.discovering,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.discovering) {
                CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                Text("  Buscando TVs en tu Wi‑Fi…")
            } else {
                Text("Buscar mi TV en la red")
            }
        }

        state.found.forEach { ip ->
            Button(
                onClick = { vm.pickFound(ip) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Conectar a $ip")
            }
        }

        Text("…o escribe la IP a mano:", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = state.hostInput,
            onValueChange = vm::onHostChange,
            label = { Text("IP de la TV (ej. 192.168.0.42)") },
            singleLine = true,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = vm::connect,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.busy) {
                CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.height(0.dp))
                Text("  Conectando…")
            } else {
                Text("Conectar")
            }
        }
        Text(
            "Al conectar, acepta \"¿Permitir depuración?\" en la pantalla de la TV.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        TextButton(onClick = vm::backToPlatform, modifier = Modifier.fillMaxWidth()) {
            Text("← Cambiar plataforma")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackagesScreen(state: UiState, vm: DebloaterViewModel, modifier: Modifier) {
    val device = state.device
    val pickApk = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) vm.installLauncher(uri) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    device?.model ?: "TV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Android ${device?.androidVersion ?: "?"} · ${state.rows.size} apps",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = vm::disconnect) { Text("Desconectar") }
        }

        Spacer(Modifier.height(8.dp))

        Card(Modifier.fillMaxWidth()) {
            Text(
                "⚠️ El malware de firmware (cajas baratas tipo BadBox) no se quita por ADB; " +
                    "requiere reflashear o reemplazar el equipo. Aquí solo se desactiva/desinstala software normal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(10.dp),
            )
        }
        Spacer(Modifier.height(8.dp))

        // Selector de perfil con búsqueda (hay cientos de modelos)
        var expanded by remember { mutableStateOf(false) }
        var query by remember { mutableStateOf("") }
        val selectedName = state.profiles.firstOrNull { it.id == state.selectedProfileId }?.name
            ?: "Común (recomendado)"
        val filtered = remember(query, state.profiles) {
            if (query.isBlank()) state.profiles
            else state.profiles.filter {
                it.name.contains(query, true) || it.brand.contains(query, true) || it.id.contains(query, true)
            }
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Modelo / perfil de debloat") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Buscar tu modelo…") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                )
                DropdownMenuItem(
                    text = { Text("Común (multimarca, recomendado)") },
                    onClick = { vm.selectProfile(null); expanded = false },
                )
                filtered.take(40).forEach { p ->
                    DropdownMenuItem(
                        text = { Text("${p.name}  ·  ${p.brand}") },
                        onClick = { vm.selectProfile(p.id); expanded = false },
                    )
                }
                if (filtered.size > 40) {
                    DropdownMenuItem(
                        enabled = false,
                        text = { Text("…y ${filtered.size - 40} más, afina la búsqueda") },
                        onClick = {},
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(onClick = vm::selectRecommended, label = { Text("Recomendados") })
            AssistChip(onClick = vm::selectTelemetry, label = { Text("Telemetría") })
            AssistChip(onClick = vm::clearSelection, label = { Text("Ninguno") })
        }

        // Método: desactivar (reversible) o desinstalar (usuario 0)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.action == Action.Disable,
                onClick = { vm.setAction(Action.Disable) },
                label = { Text("Desactivar (reversible)") },
            )
            FilterChip(
                selected = state.action == Action.Uninstall,
                onClick = { vm.setAction(Action.Uninstall) },
                label = { Text("Desinstalar (user 0)") },
            )
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        val grouped = remember(state.rows) { state.rows.groupBy { it.group } }
        LazyColumn(Modifier.weight(1f)) {
            AppGroup.entries.forEach { group ->
                val rows = grouped[group].orEmpty()
                if (rows.isNotEmpty()) {
                    item(key = "header_${group.name}") {
                        Text(
                            "${group.label} · ${rows.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(rows, key = { it.packageName }) { row ->
                        AppRowItem(
                            row = row,
                            checked = row.packageName in state.selected,
                            onToggle = { vm.toggle(row.packageName) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val actionLabel = if (state.action == Action.Disable) {
            "Desactivar ${state.selected.size}"
        } else {
            "Desinstalar ${state.selected.size} (user 0)"
        }
        Button(
            onClick = vm::runAction,
            enabled = state.selected.isNotEmpty() && !state.busy,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(actionLabel)
        }
        if (state.hasBackup) {
            OutlinedButton(onClick = vm::restoreLatest, modifier = Modifier.fillMaxWidth()) {
                Text("Restaurar último respaldo")
            }
        }

        val adOn = state.adBlockHost != null
        OutlinedButton(
            onClick = vm::toggleAdBlock,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (adOn) "Bloqueo de anuncios: ACTIVO — desactivar"
                else "Activar bloqueo de anuncios (DNS AdGuard)",
            )
        }

        homeSwitchNote(device)?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        OutlinedButton(
            onClick = { pickApk.launch(arrayOf("*/*")) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Instalar launcher (Projectivy) y fijar como inicio…")
        }
    }
}

/** Nota de riesgo al cambiar la pantalla de inicio, según el fabricante/modelo. */
private fun homeSwitchNote(device: tv.clean.debloater.adb.DeviceInfo?): String? {
    val m = ((device?.manufacturer ?: "") + " " + (device?.model ?: "")).lowercase()
    return when {
        m.contains("amazon") || m.contains("fire") ->
            "⚠️ Fire TV no permite launcher de terceros como inicio: aquí solo sirve el debloat."
        m.contains("chromecast") || m.contains("google tv") || m.contains("streamer") ->
            "⚠️ En Google TV/Chromecast el cambio de inicio puede no sobrevivir al reinicio."
        else -> null
    }
}

@Composable
private fun AppRowItem(row: AppRow, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(
                row.packageName,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Badges: telemetría + nivel de riesgo
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (row.isTelemetry) Badge("🔎 datos", Color(0xFFFF6B6B))
                row.risk?.let { Badge(riskLabel(it), riskColor(it)) }
            }
            val text = row.description ?: row.reason
            text?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun riskLabel(removal: String): String = when (removal.lowercase()) {
    "recommended" -> "seguro"
    "advanced" -> "avanzado"
    "expert" -> "experto"
    "unsafe" -> "no seguro"
    else -> removal.lowercase()
}

private fun riskColor(removal: String): Color = when (removal.lowercase()) {
    "recommended" -> Color(0xFF7BE07B)
    "advanced" -> Color(0xFFE0C65B)
    "expert" -> Color(0xFFE08A3C)
    "unsafe" -> Color(0xFFFF6B6B)
    else -> Color(0xFF9AA0A6)
}

@Composable
private fun WorkingScreen(state: UiState, vm: DebloaterViewModel, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.busy) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
            Text(
                if (state.busy) "  Trabajando…" else "Terminado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(state.log) { line ->
                Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
        if (state.workDone) {
            Button(onClick = vm::backToPackages, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        }
    }
}
