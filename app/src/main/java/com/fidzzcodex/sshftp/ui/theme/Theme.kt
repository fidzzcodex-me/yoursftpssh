package com.fidzzcodex.sshftp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Colors ───────────────────────────────────────────────────────────────────
object NeoColors {
    val Black     = Color(0xFF1A1A1A)
    val White     = Color(0xFFFFFFFF)
    val Red       = Color(0xFFFF3366)
    val Blue      = Color(0xFF3366FF)
    val Yellow    = Color(0xFFFFCC00)
    val LightGray = Color(0xFFF5F5F5)
    val DarkGray  = Color(0xFF2D2D2D)
    val Green     = Color(0xFF00CC66)
    val Orange    = Color(0xFFFF6600)
}

data class NeoColorScheme(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val error: Color,
    val success: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val shadow: Color,
    val terminalBg: Color,
    val terminalText: Color,
)

val LightColorScheme = NeoColorScheme(
    background    = NeoColors.White,
    surface       = NeoColors.LightGray,
    primary       = NeoColors.Blue,
    secondary     = NeoColors.Yellow,
    accent        = NeoColors.Red,
    error         = NeoColors.Red,
    success       = NeoColors.Green,
    text          = NeoColors.Black,
    textSecondary = NeoColors.DarkGray,
    border        = NeoColors.Black,
    shadow        = NeoColors.Black,
    terminalBg    = NeoColors.Black,
    terminalText  = NeoColors.Green,
)

// ─── Typography ───────────────────────────────────────────────────────────────
val InterFamily = FontFamily.Default  // replaced with Inter at runtime via downloadable fonts
val MonoFamily  = FontFamily.Monospace

data class NeoTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val mono: TextStyle,
    val monoSmall: TextStyle,
)

val AppTypography = NeoTypography(
    h1        = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Black,  fontSize = 28.sp, letterSpacing = (-0.5).sp),
    h2        = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
    h3        = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold,   fontSize = 18.sp),
    body      = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    label     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold,   fontSize = 11.sp, letterSpacing = 1.sp),
    mono      = TextStyle(fontFamily = MonoFamily,  fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    monoSmall = TextStyle(fontFamily = MonoFamily,  fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
)

// ─── Dimensions ───────────────────────────────────────────────────────────────
data class NeoDimensions(
    val borderWidth: Dp,
    val shadowOffset: Dp,
    val borderRadius: Dp,
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,
    val buttonHeight: Dp,
    val iconSize: Dp,
)

val AppDimensions = NeoDimensions(
    borderWidth   = 3.dp,
    shadowOffset  = 5.dp,
    borderRadius  = 0.dp,
    paddingSmall  = 8.dp,
    paddingMedium = 16.dp,
    paddingLarge  = 24.dp,
    buttonHeight  = 52.dp,
    iconSize      = 24.dp,
)

// ─── CompositionLocals ────────────────────────────────────────────────────────
val LocalNeoColors     = staticCompositionLocalOf { LightColorScheme }
val LocalNeoTypography = staticCompositionLocalOf { AppTypography }
val LocalNeoDimensions = staticCompositionLocalOf { AppDimensions }

// ─── Theme entry point ────────────────────────────────────────────────────────
object SSHFTPTheme {
    val colors: NeoColorScheme
        @Composable get() = LocalNeoColors.current
    val typography: NeoTypography
        @Composable get() = LocalNeoTypography.current
    val dimensions: NeoDimensions
        @Composable get() = LocalNeoDimensions.current
}

@Composable
fun SSHFTPTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNeoColors     provides LightColorScheme,
        LocalNeoTypography provides AppTypography,
        LocalNeoDimensions provides AppDimensions,
        content            = content,
    )
}
