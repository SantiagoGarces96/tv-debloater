package tv.clean.debloater.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tv.clean.automation.Action
import tv.clean.automation.Macro
import tv.clean.automation.Macros
import tv.clean.automation.Trigger
import tv.clean.debloater.vm.DebloaterViewModel
import tv.clean.debloater.vm.UiState

@Composable
fun AutomationScreen(state: UiState, vm: DebloaterViewModel, modifier: Modifier) {
    val draft = state.autoDraft
    if (draft != null) {
        BackHandler(onBack = vm::cancelDraft)
        MacroEditor(
            initial = draft,
            isNew = state.autoDraftIsNew,
            error = state.autoDraftError,
            onSave = vm::saveDraft,
            onCancel = vm::cancelDraft,
            onDelete = { vm.deleteMacro(draft.id) },
            modifier = modifier,
        )
    } else {
        MacroList(state, vm, modifier)
    }
}

// ---------------- Lista ----------------

@Composable
private fun MacroList(state: UiState, vm: DebloaterViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Automatizaciones",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = vm::backToPackages) { Text("Volver") }
        }
        Text(
            "Como MacroDroid: CUANDO aparece algo en la tele, ENTONCES la tele hace algo sola. " +
                "Por ejemplo, pulsar «Continuar usando» o «Saltar anuncio». Necesita la pantalla de inicio SG Alfa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        // Interruptor general
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Servicio en la tele", fontWeight = FontWeight.SemiBold)
                    Text(
                        when (state.autoEnabled) {
                            true -> "● Activado"
                            false -> "○ Desactivado"
                            null -> "Comprobando…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.autoEnabled == true) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary,
                    )
                }
                if (state.autoBusy) {
                    CircularProgressIndicator(Modifier.height(22.dp).width(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Switch(
                    checked = state.autoEnabled == true,
                    onCheckedChange = { vm.toggleAutomation() },
                    enabled = !state.autoBusy && state.autoEnabled != null,
                )
            }
        }

        if (state.autoLog.isNotEmpty()) {
            state.autoLog.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Mis automatizaciones (${state.autoMacros.count { it.enabled }}/${state.autoMacros.size} encendidas)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }
        Button(onClick = vm::newMacro, modifier = Modifier.fillMaxWidth()) { Text("+ Crear automatización") }

        if (state.autoMacros.isEmpty()) {
            Text(
                "No tienes ninguna. Crea una o añade una plantilla de abajo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        state.autoMacros.forEach { m ->
            Card(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.editMacro(m) },
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            Macros.describe(m),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text("Toca para editar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Switch(checked = m.enabled, onCheckedChange = { vm.setMacroEnabled(m.id, it) })
                }
            }
        }

        val missing = Macros.templates().filter { t -> state.autoMacros.none { it.id == t.id } }
        HorizontalDivider()
        Text("Plantillas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Automatizaciones listas. «Añadir» la pone tal cual; «Copiar y cambiar» la abre en el editor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Macros.templates().forEach { t ->
            val added = t !in missing
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(t.name, fontWeight = FontWeight.SemiBold)
                    Text(Macros.describe(t), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (added) {
                            Text(
                                "✓ Ya está en tu lista",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        } else {
                            TextButton(onClick = { vm.addTemplate(t) }) { Text("Añadir") }
                        }
                        TextButton(onClick = { vm.newFromTemplate(t) }) { Text("Copiar y cambiar") }
                    }
                }
            }
        }

        Text(
            "Límites: solo funciona en apps que muestran sus botones de forma accesible, y no puede saltar " +
                "anuncios que no tienen botón para saltar. La tele no guarda ni envía lo que ve en pantalla.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

// ---------------- Editor ----------------

@Composable
private fun MacroEditor(
    initial: Macro,
    isNew: Boolean,
    error: String?,
    onSave: (Macro) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier,
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var trigger by remember(initial) { mutableStateOf(initial.trigger) }
    var text by remember(initial) { mutableStateOf(initial.text) }
    var context by remember(initial) { mutableStateOf(initial.context.joinToString(", ")) }
    var app by remember(initial) { mutableStateOf(initial.app) }
    var action by remember(initial) { mutableStateOf(initial.action) }
    var delay by remember(initial) { mutableStateOf(initial.delaySeconds) }
    var enabled by remember(initial) { mutableStateOf(initial.enabled) }
    var confirmDelete by remember(initial) { mutableStateOf(false) }

    // "Pulsar ese botón" no tiene sentido con un texto: pasamos a Atrás.
    if (trigger == Trigger.TEXT && action == Action.CLICK) action = Action.BACK

    val current = Macro(
        id = initial.id,
        name = name,
        enabled = enabled,
        trigger = trigger,
        text = text,
        context = context.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        action = action,
        app = app,
        delaySeconds = delay,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isNew) "Nueva automatización" else "Editar automatización",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            placeholder = { Text("Ej. Quitar «¿Sigues viendo?»") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Encendida", modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }

        // CUANDO
        Section("CUANDO", "Qué tiene que aparecer en la pantalla de la tele.", Color(0xFF2E7D32)) {
            Choice("Aparece un botón con este texto", trigger == Trigger.BUTTON) { trigger = Trigger.BUTTON }
            Choice("Aparece este texto en cualquier parte", trigger == Trigger.TEXT) { trigger = Trigger.TEXT }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(if (trigger == Trigger.BUTTON) "Texto del botón" else "Texto en pantalla") },
                placeholder = { Text(if (trigger == Trigger.BUTTON) "Ej. Continuar usando" else "Ej. Publicidad") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Hint(
                if (trigger == Trigger.BUTTON) "Escríbelo como sale en la tele. No importan mayúsculas ni signos."
                else "Al menos 3 letras. Se busca en todo lo que hay en pantalla.",
            )
        }

        // SOLO SI
        Section("SOLO SI (opcional)", "Condiciones para no pulsar donde no debe.", Color(0xFFEF6C00)) {
            OutlinedTextField(
                value = context,
                onValueChange = { context = it },
                label = { Text("También se ve alguno de estos textos") },
                placeholder = { Text("Ej. sigues viendo, deseas continuar") },
                modifier = Modifier.fillMaxWidth(),
            )
            Hint("Sepáralos con comas. Muy útil para botones cortos como «Sí» u «OK».")
            OutlinedTextField(
                value = app,
                onValueChange = { app = it },
                label = { Text("Solo en esta app (paquete)") },
                placeholder = { Text("Ej. com.google.android.youtube.tv") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Hint("Déjalo vacío para que funcione en todas las apps. El paquete lo ves en la lista de apps.")
        }

        // ENTONCES
        Section("ENTONCES", "Qué hace la tele.", Color(0xFF1565C0)) {
            if (trigger == Trigger.BUTTON) {
                Choice("Pulsar ese botón", action == Action.CLICK) { action = Action.CLICK }
            }
            Choice("Pulsar Atrás", action == Action.BACK) { action = Action.BACK }
            Choice("Ir a la pantalla de inicio", action == Action.HOME) { action = Action.HOME }
            Text(
                if (delay == 0) "Esperar antes: nada (al instante)" else "Esperar antes: $delay s",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = delay.toFloat(),
                onValueChange = { delay = it.toInt() },
                valueRange = 0f..30f,
                steps = 29,
            )
            Hint("Si esperas, la tele vuelve a mirar antes de actuar: si el aviso ya se fue, no hace nada.")
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Resumen", fontWeight = FontWeight.SemiBold)
                Text(
                    if (text.isBlank()) "Escribe qué tiene que aparecer." else Macros.describe(current),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (error != null) {
            Text("✗ $error", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }

        Button(onClick = { onSave(current) }, modifier = Modifier.fillMaxWidth()) { Text("Guardar") }
        if (!isNew) {
            if (confirmDelete) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("¿Eliminarla?", modifier = Modifier.weight(1f))
                    TextButton(onClick = { confirmDelete = false }) { Text("No") }
                    Button(onClick = onDelete) { Text("Sí, eliminar") }
                }
            } else {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, subtitle: String, color: Color, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            content()
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
}
