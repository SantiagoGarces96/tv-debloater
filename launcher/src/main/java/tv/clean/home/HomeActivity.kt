package tv.clean.home

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppEntry(val label: String, val packageName: String, val icon: ImageBitmap?)

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = LauncherPrefs(this)
        setContent {
            val apps = remember { loadApps() }
            var vpnOn by remember { mutableStateOf(false) }
            var config by remember { mutableStateOf(prefs.load()) }
            var showSettings by remember { mutableStateOf(false) }
            val consent = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    startVpn()
                    vpnOn = true
                }
            }
            Surface(Modifier.fillMaxSize(), color = Color(0xFF0B0C0F)) {
                if (showSettings) {
                    SettingsScreen(
                        config = config,
                        onChange = { config = it; prefs.save(it) },
                        onClose = { showSettings = false },
                    )
                } else {
                    Home(
                        apps = apps,
                        config = config,
                        onLaunch = ::launch,
                        vpnOn = vpnOn,
                        onToggleVpn = {
                            if (vpnOn) {
                                stopVpn()
                                vpnOn = false
                            } else {
                                val prep = VpnService.prepare(this)
                                if (prep != null) consent.launch(prep) else { startVpn(); vpnOn = true }
                            }
                        },
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }

    private fun launch(pkg: String) {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) startActivity(intent)
    }

    private fun startVpn() {
        startService(Intent(this, DnsBlockVpnService::class.java).setAction(DnsBlockVpnService.ACTION_START))
    }

    private fun stopVpn() {
        startService(Intent(this, DnsBlockVpnService::class.java).setAction(DnsBlockVpnService.ACTION_STOP))
    }

    private fun loadApps(): List<AppEntry> {
        val pm = packageManager
        val self = packageName
        fun query(category: String) = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(category), 0,
        )
        val resolved = (query(Intent.CATEGORY_LEANBACK_LAUNCHER) + query(Intent.CATEGORY_LAUNCHER))
        val seen = HashSet<String>()
        val out = ArrayList<AppEntry>()
        for (ri in resolved) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == self || !seen.add(pkg)) continue
            val label = ri.loadLabel(pm).toString()
            val icon: ImageBitmap? = runCatching {
                val d: Drawable = ri.activityInfo.loadBanner(pm) ?: ri.loadIcon(pm)
                d.toBitmap(160, 90).asImageBitmap()
            }.getOrNull()
            out.add(AppEntry(label, pkg, icon))
        }
        return out.sortedBy { it.label.lowercase() }
    }
}

@Composable
private fun Home(
    apps: List<AppEntry>,
    config: LauncherConfig,
    onLaunch: (String) -> Unit,
    vpnOn: Boolean,
    onToggleVpn: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val accent = Color(config.accent)
    var focusedLabel by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 40.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Clock()
            Spacer(Modifier.weight(1f))
            AdBlockToggle(vpnOn, accent, onToggleVpn)
            Spacer(Modifier.padding(start = 10.dp))
            PillButton("Ajustes", accent, onOpenSettings)
        }
        Text(
            focusedLabel ?: "Tus apps",
            color = if (focusedLabel != null) Color.White else Color(0xFF9BA1AC),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 22.dp, bottom = 24.dp),
        )
        val columns = if (config.columns > 0) GridCells.Fixed(config.columns) else GridCells.Adaptive(210.dp)
        LazyVerticalGrid(
            columns = columns,
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppTile(
                    app = app,
                    accent = accent,
                    onClick = { onLaunch(app.packageName) },
                    onFocused = { focusedLabel = app.label },
                )
            }
        }
    }
}

@Composable
private fun PillButton(text: String, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .border(if (focused) 3.dp else 1.dp, if (focused) accent else Color(0xFF23272F), RoundedCornerShape(24.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(text, color = if (focused) accent else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsScreen(
    config: LauncherConfig,
    onChange: (LauncherConfig) -> Unit,
    onClose: () -> Unit,
) {
    val accent = Color(config.accent)
    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text("Personalizar", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Text("Color de acento", color = Color(0xFF9BA1AC), fontSize = 15.sp, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LauncherPrefs.ACCENTS.forEach { (name, color) ->
                SwatchButton(name, Color(color), selected = config.accent == color) {
                    onChange(config.copy(accent = color))
                }
            }
        }

        Text("Columnas", color = Color(0xFF9BA1AC), fontSize = 15.sp, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LauncherPrefs.COLUMN_OPTIONS.forEach { (label, value) ->
                ChoiceButton(label, accent, selected = config.columns == value) {
                    onChange(config.copy(columns = value))
                }
            }
        }

        Spacer(Modifier.weight(1f))
        PillButton("Volver", accent, onClose)
    }
}

@Composable
private fun SwatchButton(name: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color)
                .border(
                    if (focused || selected) 3.dp else 0.dp,
                    Color.White,
                    RoundedCornerShape(14.dp),
                )
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        )
        Text(
            name,
            color = if (selected) Color.White else Color(0xFF9BA1AC),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ChoiceButton(label: String, accent: Color, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val on = selected || focused
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(if (on) 3.dp else 1.dp, if (on) accent else Color(0xFF23272F), RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(label, color = if (selected) accent else Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            kotlinx.coroutines.delay(10_000)
        }
    }
    val time = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
    Text(time, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun AdBlockToggle(on: Boolean, accent: Color, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .border(
                if (focused) 3.dp else 1.dp,
                if (focused) accent else Color(0xFF23272F),
                RoundedCornerShape(24.dp),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            if (on) "● Anuncios bloqueados" else "○ Bloquear anuncios",
            color = if (on) accent else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AppTile(app: AppEntry, accent: Color, onClick: () -> Unit, onFocused: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, spring(), label = "scale")
    val elevation by animateDpAsState(if (focused) 18.dp else 0.dp, label = "elev")
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { if (it.isFocused) onFocused() }
            .shadow(elevation, shape, ambientColor = accent, spotColor = accent)
            .clip(shape)
            .border(if (focused) 3.dp else 1.dp, if (focused) accent else Color(0xFF23272F), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val img = app.icon
        if (img != null) {
            androidx.compose.foundation.Image(
                bitmap = img,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape),
            )
        } else {
            Text(
                app.label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}
