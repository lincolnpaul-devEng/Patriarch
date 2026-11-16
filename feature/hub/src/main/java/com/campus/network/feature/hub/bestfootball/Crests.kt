package com.campus.network.feature.hub.bestfootball

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

fun DrawScope.drawCrest(club: String, sizePx: Float) {
    val sc = sizePx / 48f
    when (club) {
        "red" -> {
            drawCircle(Color.White, radius = 23f * sc, center = Offset(24f * sc, 24f * sc))
            drawCircle(Color(0xFFD6262C), radius = 20f * sc, center = Offset(24f * sc, 24f * sc))
            drawRect(
                Color(0xFF111111),
                topLeft = Offset(21f * sc, 6f * sc),
                size = Size(6f * sc, 36f * sc)
            )
            drawRect(
                Color(0xFF111111).copy(alpha = 0.85f),
                topLeft = Offset(6f * sc, 21f * sc),
                size = Size(36f * sc, 6f * sc)
            )
        }
        "mersey" -> {
            val shield = Path().apply {
                moveTo(8f * sc, 7f * sc)
                lineTo(40f * sc, 7f * sc)
                lineTo(40f * sc, 25f * sc)
                cubicTo(40f * sc, 34f * sc, 33f * sc, 39f * sc, 24f * sc, 42f * sc)
                cubicTo(15f * sc, 39f * sc, 8f * sc, 34f * sc, 8f * sc, 25f * sc)
                close()
            }
            drawPath(shield, color = Color(0xFF1F5FD1))

            val star = Path().apply {
                moveTo(24f * sc, 12f * sc)
                lineTo(26.8f * sc, 18f * sc)
                lineTo(33.2f * sc, 18.8f * sc)
                lineTo(28.5f * sc, 23.2f * sc)
                lineTo(29.7f * sc, 29.6f * sc)
                lineTo(24f * sc, 26.4f * sc)
                lineTo(18.3f * sc, 29.6f * sc)
                lineTo(19.5f * sc, 23.2f * sc)
                lineTo(14.8f * sc, 18.8f * sc)
                lineTo(21.2f * sc, 18f * sc)
                close()
            }
            drawPath(star, color = Color.White)
        }
        "kings" -> {
            drawCircle(Color(0xFF1B2F86), radius = 23f * sc, center = Offset(24f * sc, 24f * sc))
            drawCircle(Color.White, radius = 18f * sc, center = Offset(24f * sc, 24f * sc))
            val diamond1 = Path().apply {
                moveTo(24f * sc, 11f * sc)
                lineTo(33f * sc, 24f * sc)
                lineTo(24f * sc, 37f * sc)
                lineTo(15f * sc, 24f * sc)
                close()
            }
            drawPath(diamond1, color = Color(0xFF2C5BD6))
            val diamond2 = Path().apply {
                moveTo(24f * sc, 17f * sc)
                lineTo(28.6f * sc, 24f * sc)
                lineTo(24f * sc, 31f * sc)
                lineTo(19.4f * sc, 24f * sc)
                close()
            }
            drawPath(diamond2, color = Color.White)
            drawCircle(Color(0xFF1B2F86), radius = 1.4f * sc, center = Offset(24f * sc, 8f * sc))
            drawCircle(Color(0xFF1B2F86), radius = 1.4f * sc, center = Offset(24f * sc, 40f * sc))
        }
        "hornets" -> {
            drawCircle(Color.White, radius = 23f * sc, center = Offset(24f * sc, 24f * sc))
            drawCircle(Color(0xFF111111), radius = 21f * sc, center = Offset(24f * sc, 24f * sc))
            drawCircle(Color(0xFFFFE51A), radius = 18f * sc, center = Offset(24f * sc, 24f * sc))
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = android.graphics.Color.parseColor("#111111")
                    textSize = 17f * sc
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                drawText("HRN", 24f * sc, 28f * sc, paint)
                val paint2 = Paint().apply {
                    color = android.graphics.Color.parseColor("#111111")
                    textSize = 7.5f * sc
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                drawText("09", 24f * sc, 38f * sc, paint2)
            }
        }
        "azure" -> {
            drawCircle(Color(0xFF2F9BE0), radius = 23f * sc, center = Offset(24f * sc, 24f * sc))
            drawCircle(Color.White, radius = 18f * sc, center = Offset(24f * sc, 24f * sc))
            val hexOutline = Path().apply {
                moveTo(24f * sc, 11f * sc)
                lineTo(34.5f * sc, 17f * sc)
                lineTo(34.5f * sc, 31f * sc)
                lineTo(24f * sc, 37f * sc)
                lineTo(13.5f * sc, 31f * sc)
                lineTo(13.5f * sc, 17f * sc)
                close()
            }
            drawPath(hexOutline, color = Color(0xFF2F9BE0), style = Stroke(width = 3f * sc))
            val hexInner = Path().apply {
                moveTo(24f * sc, 17f * sc)
                lineTo(29.3f * sc, 20f * sc)
                lineTo(29.3f * sc, 26f * sc)
                lineTo(24f * sc, 29f * sc)
                lineTo(18.7f * sc, 26f * sc)
                lineTo(18.7f * sc, 20f * sc)
                close()
            }
            drawPath(hexInner, color = Color(0xFF2F9BE0))
        }
        "fox" -> {
            drawCircle(Color(0xFF1A43C4), radius = 23f * sc, center = Offset(24f * sc, 24f * sc))
            drawCircle(Color(0xFFF2C230), radius = 18f * sc, center = Offset(24f * sc, 24f * sc))
            val foxHead = Path().apply {
                moveTo(12f * sc, 14f * sc)
                lineTo(20f * sc, 21f * sc)
                lineTo(28f * sc, 21f * sc)
                lineTo(36f * sc, 14f * sc)
                lineTo(33f * sc, 27f * sc)
                lineTo(24f * sc, 36f * sc)
                lineTo(15f * sc, 27f * sc)
                close()
            }
            drawPath(foxHead, color = Color(0xFF1A43C4))
        }
        else -> { // "atlas"
            drawCircle(Color.White, radius = 23f * sc, center = Offset(24f * sc, 24f * sc))
            drawCircle(Color(0xFF169C4E), radius = 20f * sc, center = Offset(24f * sc, 24f * sc))
            drawCircle(Color.White, radius = 10f * sc, center = Offset(24f * sc, 24f * sc), style = Stroke(width = 2.4f * sc))
            drawLine(Color.White, start = Offset(4f * sc, 24f * sc), end = Offset(44f * sc, 24f * sc), strokeWidth = 2.4f * sc)
            drawLine(Color.White, start = Offset(24f * sc, 4f * sc), end = Offset(24f * sc, 44f * sc), strokeWidth = 2.4f * sc)
        }
    }
}

@Composable
fun CrestImage(
    club: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawCrest(club, size.width)
    }
}
