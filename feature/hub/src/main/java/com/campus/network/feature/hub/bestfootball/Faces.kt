package com.campus.network.feature.hub.bestfootball

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class FaceConfig(
    val bg: Color,
    val skin: Color,
    val hair: Color,
    val shirt: Color = Color.White,
    val style: String = "short", // "short", "long", "bun", "afro", "curly"
    val beard: Boolean = false,
    val glasses: Boolean = false,
    val collar: Color? = null
)

val FACES: Map<String, FaceConfig> = mapOf(
    "me" to FaceConfig(
        bg = Color(0xFF2A2A2E), skin = Color(0xFFE2AE8C), hair = Color(0xFF3A2418),
        shirt = Color(0xFFF2F2F2), style = "curly"
    ),
    "star" to FaceConfig(
        bg = Color(0xFFFFFFFF), skin = Color(0xFFE8BFA4), hair = Color(0xFF3A2A22),
        shirt = Color(0xFFF4C7DD), style = "short", beard = true, collar = Color(0xFFE9A9C8)
    ),
    "d1" to FaceConfig(
        bg = Color(0xFFEFE9E2), skin = Color(0xFF6B4630), hair = Color(0xFF1D130D),
        shirt = Color(0xFFF5F5F5), style = "short"
    ),
    "d2" to FaceConfig(
        bg = Color(0xFFF3E3DC), skin = Color(0xFFF0C9AE), hair = Color(0xFF6A3F2A),
        shirt = Color(0xFFF6E6EC), style = "long"
    ),
    "d3" to FaceConfig(
        bg = Color(0xFFE6EEF4), skin = Color(0xFFEFC8B0), hair = Color(0xFF4A3226),
        shirt = Color(0xFFD8E6F2), style = "bun"
    ),
    "d4" to FaceConfig(
        bg = Color(0xFFE9F0E2), skin = Color(0xFFE2B08F), hair = Color(0xFFC79A4A),
        shirt = Color(0xFFF3EFE0), style = "long"
    ),
    "d5" to FaceConfig(
        bg = Color(0xFFEFE7EE), skin = Color(0xFF8A5A3C), hair = Color(0xFF17100C),
        shirt = Color(0xFFECE7F4), style = "afro"
    ),
    "d6" to FaceConfig(
        bg = Color(0xFFE9ECF2), skin = Color(0xFFECC4A6), hair = Color(0xFF2B2420),
        shirt = Color(0xFFE7ECF5), style = "short"
    ),
    "s1" to FaceConfig(
        bg = Color(0xFFE3DED8), skin = Color(0xFFF0C7AD), hair = Color(0xFFD8B36A),
        shirt = Color(0xFFF1F1F1), style = "long", glasses = true
    ),
    "s2" to FaceConfig(
        bg = Color(0xFFEFE8DF), skin = Color(0xFFF0C6A8), hair = Color(0xFF2B2118),
        shirt = Color(0xFF222222), style = "bun"
    ),
    "s3" to FaceConfig(
        bg = Color(0xFFFFF748), skin = Color(0xFFEFC3A5), hair = Color(0xFFB8863A),
        shirt = Color(0xFFFF9F1C), style = "long"
    ),
    "c1" to FaceConfig(
        bg = Color(0xFF3A2A2A), skin = Color(0xFFE9C2A6), hair = Color(0xFF150F0D),
        shirt = Color(0xFFD9D3CC), style = "long"
    ),
    "c2" to FaceConfig(
        bg = Color(0xFFD9654A), skin = Color(0xFF7A4F36), hair = Color(0xFF120C09),
        shirt = Color(0xFFD8D8D8), style = "short"
    ),
    "c3" to FaceConfig(
        bg = Color(0xFFD7CFC9), skin = Color(0xFFF0CFB8), hair = Color(0xFFE6C98F),
        shirt = Color(0xFFF2F2F2), style = "bun"
    ),
    "c4" to FaceConfig(
        bg = Color(0xFFCFE6E0), skin = Color(0xFFD9A98A), hair = Color(0xFF2A1D16),
        shirt = Color(0xFF35506B), style = "short"
    ),
    "c5" to FaceConfig(
        bg = Color(0xFFF1D9D0), skin = Color(0xFFF2C8AD), hair = Color(0xFF7B3B25),
        shirt = Color(0xFFFFFFFF), style = "long"
    )
)

fun DrawScope.drawFace(id: String, sizePx: Float) {
    val cfg = FACES[id] ?: return
    val sc = sizePx / 100f

    // 1. Background
    drawRect(color = cfg.bg, size = Size(sizePx, sizePx))

    // 2. Hair back
    if (cfg.style == "afro") {
        drawCircle(color = cfg.hair, radius = 29f * sc, center = Offset(50f * sc, 38f * sc))
    }
    if (cfg.style == "long") {
        val path = Path().apply {
            moveTo(28f * sc, 44f * sc)
            cubicTo(22f * sc, 66f * sc, 26f * sc, 86f * sc, 36f * sc, 92f * sc)
            lineTo(46f * sc, 62f * sc)
            close()
            moveTo(72f * sc, 44f * sc)
            cubicTo(78f * sc, 66f * sc, 74f * sc, 86f * sc, 64f * sc, 92f * sc)
            lineTo(54f * sc, 62f * sc)
            close()
        }
        drawPath(path, color = cfg.hair)
    }

    // 3. Shoulders
    val shoulderPath = Path().apply {
        moveTo(6f * sc, 104f * sc)
        cubicTo(8f * sc, 84f * sc, 28f * sc, 77f * sc, 50f * sc, 77f * sc)
        cubicTo(72f * sc, 77f * sc, 92f * sc, 84f * sc, 94f * sc, 104f * sc)
        close()
    }
    drawPath(shoulderPath, color = cfg.shirt)

    // Collar
    if (cfg.collar != null) {
        val collarPath = Path().apply {
            moveTo(38f * sc, 78f * sc)
            lineTo(50f * sc, 92f * sc)
            lineTo(62f * sc, 78f * sc)
            lineTo(56f * sc, 76f * sc)
            lineTo(50f * sc, 84f * sc)
            lineTo(44f * sc, 76f * sc)
            close()
        }
        drawPath(collarPath, color = cfg.collar)
    }

    // 4. Neck
    val neckRect = RoundRect(
        left = 42.5f * sc,
        top = 60f * sc,
        right = 57.5f * sc,
        bottom = 80f * sc,
        cornerRadius = CornerRadius(6f * sc, 6f * sc)
    )
    val neckPath = Path().apply { addRoundRect(neckRect) }
    drawPath(neckPath, color = cfg.skin)

    val neckShadePath = Path().apply {
        moveTo(42.5f * sc, 70f * sc)
        quadraticTo(50f * sc, 77f * sc, 57.5f * sc, 70f * sc)
        lineTo(57.5f * sc, 74f * sc)
        quadraticTo(50f * sc, 80f * sc, 42.5f * sc, 74f * sc)
        close()
    }
    drawPath(neckShadePath, color = Color(0x1F000000))

    // 5. Head
    val headRect = Rect(31f * sc, 25f * sc, 69f * sc, 71f * sc)
    val headPath = Path().apply { addOval(headRect) }
    drawPath(headPath, color = cfg.skin)

    // 6. Hair front
    if (cfg.style == "short" || cfg.style == "bun" || cfg.style == "long") {
        val hairFront = Path().apply {
            moveTo(30.5f * sc, 46f * sc)
            cubicTo(28f * sc, 24f * sc, 41f * sc, 21f * sc, 50f * sc, 21f * sc)
            cubicTo(59f * sc, 21f * sc, 72f * sc, 25f * sc, 69.5f * sc, 46f * sc)
            cubicTo(66f * sc, 36f * sc, 60f * sc, 32f * sc, 50f * sc, 32f * sc)
            cubicTo(40f * sc, 32f * sc, 34f * sc, 36f * sc, 30.5f * sc, 46f * sc)
            close()
        }
        drawPath(hairFront, color = cfg.hair)
    }

    if (cfg.style == "bun") {
        drawCircle(color = cfg.hair, radius = 9f * sc, center = Offset(50f * sc, 17f * sc))
    }

    if (cfg.style == "curly") {
        for (i in 0..8) {
            val a = PI * (1.05 + i * 0.113)
            val cx = (50f + 22f * cos(a).toFloat()) * sc
            val cy = (44f + 24f * sin(a).toFloat()) * sc
            drawCircle(color = cfg.hair, radius = 8f * sc, center = Offset(cx, cy))
        }
    }

    if (cfg.style == "afro") {
        val afroFringe = Path().apply {
            moveTo(31f * sc, 44f * sc)
            cubicTo(32f * sc, 32f * sc, 40f * sc, 30f * sc, 50f * sc, 30f * sc)
            cubicTo(60f * sc, 30f * sc, 68f * sc, 32f * sc, 69f * sc, 44f * sc)
            cubicTo(64f * sc, 38f * sc, 58f * sc, 36f * sc, 50f * sc, 36f * sc)
            cubicTo(42f * sc, 36f * sc, 36f * sc, 38f * sc, 31f * sc, 44f * sc)
            close()
        }
        drawPath(afroFringe, color = cfg.hair)
    }

    // 7. Beard
    if (cfg.beard) {
        val beardPath = Path().apply {
            moveTo(32f * sc, 52f * sc)
            cubicTo(33f * sc, 76f * sc, 67f * sc, 76f * sc, 68f * sc, 52f * sc)
            cubicTo(63f * sc, 64f * sc, 37f * sc, 64f * sc, 32f * sc, 52f * sc)
            close()
        }
        drawPath(beardPath, color = cfg.hair)
    }

    // 8. Eyes
    drawCircle(color = Color(0xFF1B1220), radius = 1.9f * sc, center = Offset(43f * sc, 49f * sc))
    drawCircle(color = Color(0xFF1B1220), radius = 1.9f * sc, center = Offset(57f * sc, 49f * sc))

    // 9. Mouth
    val mouthPath = Path().apply {
        moveTo(43.5f * sc, 59f * sc)
        quadraticTo(50f * sc, 64f * sc, 56.5f * sc, 59f * sc)
    }
    val mouthColor = if (cfg.beard) Color(0xFFF3D6CF) else Color(0xFF7A2F2F)
    drawPath(mouthPath, color = mouthColor, style = Stroke(width = 1.6f * sc, cap = StrokeCap.Round))

    // 10. Glasses
    if (cfg.glasses) {
        val glassesStroke = Stroke(width = 1.5f * sc)
        drawCircle(color = Color(0xFF1B1220), radius = 5.5f * sc, center = Offset(43f * sc, 49f * sc), style = glassesStroke)
        drawCircle(color = Color(0xFF1B1220), radius = 5.5f * sc, center = Offset(57f * sc, 49f * sc), style = glassesStroke)
        drawLine(color = Color(0xFF1B1220), start = Offset(48.5f * sc, 49f * sc), end = Offset(51.5f * sc, 49f * sc), strokeWidth = 1.5f * sc)
    }
}

@Composable
fun FaceImage(
    id: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawFace(id, size.width)
    }
}
