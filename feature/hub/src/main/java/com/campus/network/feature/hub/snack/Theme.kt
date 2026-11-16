package com.campus.network.feature.hub.snack

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.feature.hub.R

// Homescreen Theme Colors
val Purple = Color(0xFFA969FE)   // homescreen background
val Red    = Color(0xFFFF4D32)   // bottom panel / accents
val Yellow = Color(0xFFFFF874)   // homescreen yellow accent
val Ink    = Color(0xFF0B0612)   // all dark text
val Mint   = Color(0xFFB5F7D9)
val Green  = Color(0xFFDDFDC2)
val Peach  = Color(0xFFFBE6B8)
val Lilac  = Color(0xFFDCBDFF)
val White  = Color(0xFFFFFFFF)
val Grey   = Color(0xFFCFCFD4)

val Panel  = Color(0xFF17181A)   // cart panel
val Track  = Color(0xFFF4F3F8)   // slider track
val TabBg  = Color(0xE6EBE7F8)   // inactive tab (90% alpha)
val Pale   = Color(0xB3FFFFFF)   // white 70%

// Homescreen Font Theme (Squada One)
val SquadaOne = FontFamily(Font(R.font.squada_one, FontWeight.Normal))
val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_sans_light, FontWeight.Light),
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

// Primary typography using Homescreen font
fun ts(
    size: Int,
    w: FontWeight = FontWeight.Normal,
    color: Color = Ink,
    lh: Float = 1.05f,
    ls: Float = 0f
) = TextStyle(
    fontFamily = SquadaOne,
    fontSize = size.sp,
    fontWeight = w,
    color = color,
    lineHeight = (size * lh).sp,
    letterSpacing = ls.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
)

// Homescreen Faux-bold Display Text
@Composable
fun DText(
    text: String,
    size: Float,
    color: Color,
    strokeEm: Float = 0.036f,
    lineHeight: Float = size,
    letterSpacing: Float = 0f,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { (size * strokeEm).dp.toPx() }
    val st = TextStyle(
        fontFamily = SquadaOne,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp,
        textAlign = align,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    )
    Box(modifier) {
        Text(text = text, style = st.copy(color = color))
        Text(
            text = text,
            style = st.copy(
                color = color,
                drawStyle = Stroke(width = strokeWidthPx, join = StrokeJoin.Round)
            )
        )
    }
}

// Homescreen Background Design: Purple canvas + fading dot matrix pattern
@Composable
fun HomescreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Purple)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color(70, 20, 140, (255 * 0.16f).toInt())
            val step = 14f.dp.toPx()
            val rows = (size.height / step).toInt()
            val cols = (size.width / step).toInt()
            val fadeStart = 324f.dp.toPx()
            val fadeEnd = 596f.dp.toPx()

            for (r in 0..rows) {
                val cy = 4f.dp.toPx() + r * step
                val alpha = when {
                    cy <= fadeStart -> 1f
                    cy >= fadeEnd -> 0f
                    else -> 1f - (cy - fadeStart) / (fadeEnd - fadeStart)
                }
                if (alpha <= 0.01f) continue
                for (c in 0..cols) {
                    val cx = 7f.dp.toPx() + c * step
                    drawCircle(
                        color = dotColor.copy(alpha = dotColor.alpha * alpha),
                        radius = 1.7f.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }
        content()
    }
}

val Ease = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
