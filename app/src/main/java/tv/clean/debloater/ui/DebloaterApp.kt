package tv.clean.debloater.ui

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
import androidx.compose.material3.ExposedDropdownMenu
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.clean.debloater.data.AppRow
import tv.clean.debloater.vm.DebloaterViewModel
import tv.clean.debloater.vm.Stage
import tv.clean.debloater.vm.UiState

@Composable
fun DebloaterApp(state: UiState, vm: DebloaterViewModel, modifier: Modifier = Modifier) {
    when (state.stage) {
        Stage.Connect -> ConnectScreen(state, vm, modifier)
        Stage.Packages -> PackagesScreen(state, vm, modifier)
        Stage.Working -> WorkingScreen(state, vm, modifier)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackagesScreen(state: UiState, vm: DebloaterViewModel, modifier: Modifier) {
    val device = state.device
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

        // Selector de perfil
        var expanded by remember { mutableStateOf(false) }
        val selectedName = state.profiles.firstOrNull { it.id == state.selectedProfileId }?.name
            ?: "Todas las listas (recomendado)"
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Perfil de debloat") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Todas las listas (recomendado)") },
                    onClick = { vm.selectProfile(null); expanded = false },
                )
                state.profiles.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.name) },
                        onClick = { vm.selectProfile(p.id); expanded = false },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(onClick = vm::selectRecommended, label = { Text("Recomendados") })
            AssistChip(onClick = vm::clearSelection, label = { Text("Ninguno") })
            Spacer(Modifier.weight(1f))
            Text("${state.selected.size} sel.", style = MaterialTheme.typography.bodySmall)
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(state.rows, key = { it.packageName }) { row ->
                AppRowItem(
                    row = row,
                    checked = row.packageName in state.selected,
                    onToggle = { vm.toggle(row.packageName) },
                )
                HorizontalDivider()
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = vm::runUninstall,
            enabled = state.selected.isNotEmpty() && !state.busy,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Desinstalar ${state.selected.size} (usuario 0)")
        }
        if (state.hasBackup) {
            OutlinedButton(onClick = vm::restoreLatest, modifier = Modifier.fillMaxWidth()) {
                Text("Restaurar último respaldo")
            }
        }
    }
}

@Composable
private fun AppRowItem(row: AppRow, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
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
            row.reason?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (row.inAnyList) {
            AssistChip(onClick = {}, enabled = false, label = { Text("bloat") })
        }
    }
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
