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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tv.clean.debloater.vm.DebloaterViewModel
import tv.clean.debloater.vm.UiState

@Composable
fun DownloadsScreen(state: UiState, vm: DebloaterViewModel, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Descargar e instalar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = vm::backToPackages) { Text("Volver") }
        }
        Text(
            "Pega la URL de un APK o un código de AFTVnews y lo instalo en tu tele.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        OutlinedTextField(
            value = state.dlInput,
            onValueChange = vm::onDlInput,
            label = { Text("URL o código") },
            singleLine = true,
            enabled = !state.dlBusy,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        Button(
            onClick = vm::downloadFromInput,
            enabled = !state.dlBusy && state.dlInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            if (state.dlBusy) {
                CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                Text("  Trabajando…")
            } else {
                Text("Descargar")
            }
        }

        if (state.dlLog.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(10.dp)) {
                    state.dlLog.takeLast(8).forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Text(
            "Catálogo",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        )
        LazyColumn(Modifier.weight(1f)) {
            items(state.dlCatalog, key = { it.name }) { app ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(app.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        if (app.description.isNotBlank()) {
                            Text(
                                app.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        if (app.pkg.isNotEmpty() && app.pkg in state.dlInstalled) {
                            Text(
                                "✓ Ya instalada",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    OutlinedButton(onClick = { vm.installFromCatalog(app) }, enabled = !state.dlBusy) {
                        Text(if (app.type == "playstore") "Play Store" else "Instalar")
                    }
                }
                HorizontalDivider()
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "Instala solo apps de fuentes en las que confíes. Los enlaces y códigos que pegues son responsabilidad tuya.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }

    state.dlPending?.let { p ->
        AlertDialog(
            onDismissRequest = vm::cancelPending,
            title = { Text("¿Instalar en la tele?") },
            text = {
                Text(
                    "${p.label} · ${p.mb} MB\nViene de: ${p.host}" +
                        (if (p.fromCode) "\n(resuelto desde un código de AFTVnews)" else "") +
                        "\n\nInstálalo solo si confías en ese origen.",
                )
            },
            confirmButton = { TextButton(onClick = vm::confirmInstallPending) { Text("Instalar") } },
            dismissButton = { TextButton(onClick = vm::cancelPending) { Text("Cancelar") } },
        )
    }
}
