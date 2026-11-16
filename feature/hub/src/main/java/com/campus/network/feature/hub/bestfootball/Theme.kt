package com.campus.network.feature.hub.bestfootball

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.feature.hub.R

// Exact Colors (§1)
val Purple = Color(0xFFA969FE)   // screen background
val Red    = Color(0xFFFF4D32)   // bottom panel / stats screen
val Yellow = Color(0xFFFFF874)   // 72% bar
val Ink    = Color(0xFF0B0612)   // all dark text, needle, home bar
val Mint   = Color(0xFFB5F7D9)
val Green  = Color(0xFFDDFDC2)
val Peach  = Color(0xFFFBE6B8)
val Lilac  = Color(0xFFDCBDFF)   // card corner fold
val White  = Color(0xFFFFFFFF)
val DateTxt = Color(0xFF8551BF)  // "08 SEP" (alpha .95)
val Grey = Color(0xFFCFCFD4)     // "10", "28%"

// Fonts (§2)
val SquadaOne = FontFamily(Font(R.font.squada_one, FontWeight.Normal))
val Inter = FontFamily.SansSerif

// Faux-bold display text with double drawing (§2)
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

// Design canvas container (§0): 393 × 852 dp scaled uniformly to screen
@Composable
fun FittedScreen(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val s = minOf(maxWidth / 393.dp, maxHeight / 852.dp) // uniform scale
        val density = LocalDensity.current.density
        CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1f)) {
            Box(
                Modifier.align(Alignment.Center)
                    .requiredSize(393.dp, 852.dp)
                    .graphicsLayer {
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .clip(RoundedCornerShape(52.dp))
            ) {
                content()
            }
        }
    }
}

// Exact Springs (§4.5)
val dialSpec  = spring<Float>(dampingRatio = 0.740f, stiffness = 165f)   // pos
val tiltSpec  = spring<Float>(dampingRatio = 0.408f, stiffness = 150f)   // needle tilt
val redSpec   = spring<Float>(dampingRatio = 0.776f, stiffness = 120f)   // red panel
val cardSpec  = spring<Float>(dampingRatio = 0.587f, stiffness = 210f)   // cards, stat cards, 72% bar
val homeSpec  = spring<Float>(dampingRatio = 0.685f, stiffness = 90f)    // home layer fade
