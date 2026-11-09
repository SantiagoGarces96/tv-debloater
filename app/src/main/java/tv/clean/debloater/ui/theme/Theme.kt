package tv.clean.debloater.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF34D399)
private val OnAccent = Color(0xFF06281D)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = Color(0xFF9BA1AC),
    background = Color(0xFF0B0C0F),
    surface = Color(0xFF14161B),
    surfaceVariant = Color(0xFF1B1E25),
    error = Color(0xFFF87171),
    onSurface = Color(0xFFF4F5F7),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F8A1F),
    secondary = Color(0xFF2E7D32),
)

@Composable
fun TvDebloaterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
