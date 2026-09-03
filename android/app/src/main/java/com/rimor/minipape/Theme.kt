package com.rimor.minipape

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Mont, in five weights. Black is the default, not an emphasis weight — it is what lets a
 * plain word act as a button without a box around it. SemiBold ships even though it is
 * rarely named: without it Compose collapses Medium onto Regular and headings stop reading
 * as headings.
 */
val MontFamily = FontFamily(
    Font(R.font.mont_thin, FontWeight.Thin),
    Font(R.font.mont_light, FontWeight.Light),
    Font(R.font.mont_regular, FontWeight.Normal),
    Font(R.font.mont_semibold, FontWeight.SemiBold),
    Font(R.font.mont_black, FontWeight.Black),
)

/** Black, white, and one accent at a time. */
object Mont {
    /** The Mont surface: black at 92%, with whatever is behind it faintly present. */
    val Surface = Color(0xEB000000)

    val Selected = Color(0xFFFFFFFF)
    val Primary = Color(0xEBFFFFFF)
    val Explanatory = Color(0x9EFFFFFF)
    val Dim = Color(0x94FFFFFF)
    val Disabled = Color(0x59FFFFFF)
    val Border = Color(0x57FFFFFF)

    /** The unfilled part of a slider or toggle track. On black, an empty control is a lost one. */
    val Track = Color(0x17FFFFFF)

    val Mustard = Color(0xFFD8A628)
    val Live = Color(0xFF2E9E5B)
    val Danger = Color(0xFFC0392B)

    /** A row is tall enough to hit without being separated from the ones beside it. */
    val RowPadding = 7

    val wordmark = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Thin, fontSize = 26.sp)
    val wordmarkBold = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Black, fontSize = 26.sp)
    val action = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Black, fontSize = 44.sp, letterSpacing = (-0.6).sp, lineHeight = 48.sp)
    val titleLarge = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.4).sp)
    val titleMedium = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.2).sp)
    val row = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Black, fontSize = 15.sp)
    val body = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Black, fontSize = 14.sp)
    val clock = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Black, fontSize = 13.sp)
    val bodySmall = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp)
    val caption = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.5.sp, lineHeight = 13.sp)
    val term = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Black, fontSize = 12.sp)
    val meaning = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp)
    val explanatory = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.5.sp, lineHeight = 16.sp)
    val toggle = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Black, fontSize = 10.sp)
    val mark = TextStyle(fontFamily = MontFamily, fontWeight = FontWeight.Black, fontSize = 9.sp)
}

private val MontColors = darkColorScheme(
    primary = Mont.Selected,
    onPrimary = Color.Black,
    secondary = Mont.Selected,
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Mont.Primary,
    surface = Color.Black,
    onSurface = Mont.Primary,
    surfaceVariant = Color.Black,
    onSurfaceVariant = Mont.Dim,
    outline = Mont.Border,
)

@Composable
fun MiniPapeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MontColors, content = content)
}
