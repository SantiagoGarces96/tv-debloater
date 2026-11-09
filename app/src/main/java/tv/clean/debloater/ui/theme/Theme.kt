package tv.clean.debloater.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF7BE07B)
private val AccentDark = Color(0xFF15181E)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = AccentDark,
    secondary = Color(0xFFCFFFCF),
    background = Color(0xFF0E1013),
    surface = Color(0xFF15181E),
    surfaceVariant = Color(0xFF1E222B),
    error = Color(0xFFFF6B6B),
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
