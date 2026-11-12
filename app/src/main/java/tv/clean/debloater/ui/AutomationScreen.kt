package tv.clean.debloater.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.clean.debloater.vm.DebloaterViewModel
import tv.clean.debloater.vm.UiState

@Composable
fun AutomationScreen(state: UiState, vm: DebloaterViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Cerrar avisos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = vm::backToPackages) { Text("Volver") }
        }
        Text(
            "La tele pulsa sola botones como «Continuar usando», «Seguir viendo» o «Saltar anuncio». " +
                "Funciona con la pantalla de inicio SG Alfa instalada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        val status = when (state.autoEnabled) {
            true -> "● Activado en la tele"
            false -> "○ Desactivado"
            null -> "Comprobando…"
        }
        Text(
            status,
            fontWeight = FontWeight.SemiBold,
            color = if (state.autoEnabled == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        )
        Button(
            onClick = vm::toggleAutomation,
            enabled = !state.autoBusy && state.autoEnabled != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.autoBusy) {
                CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                Text("  Trabajando…")
            } else {
                Text(if (state.autoEnabled == true) "Desactivar" else "Activar")
            }
        }

        Text("Tus reglas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Una por línea: el texto del botón. Si pones «|» y otros textos, solo se pulsa cuando esos textos " +
                "también estén en pantalla. Ejemplo:  Sí | sigues viendo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        OutlinedTextField(
            value = state.autoRules,
            onValueChange = vm::onAutoRules,
            minLines = 8,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            enabled = !state.autoBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::saveAutoRules, enabled = !state.autoBusy) { Text("Guardar en la tele") }
            OutlinedButton(onClick = vm::resetAutoRules, enabled = !state.autoBusy) { Text("Reglas por defecto") }
        }

        if (state.autoLog.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    state.autoLog.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        Text(
            "Límites: solo funciona en apps que muestran sus botones de forma accesible, y no puede saltar " +
                "anuncios que no tienen botón para saltar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
