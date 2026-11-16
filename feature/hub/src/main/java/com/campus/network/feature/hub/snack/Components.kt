package com.campus.network.feature.hub.snack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Vector bag illustration
@Composable
fun Bag(p: Product, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(100f / 130f)) {
        val s = size.width / 100f
        fun o(x: Float, y: Float) = Offset(x * s, y * s)
        val outline = Path().apply {
            moveTo(9 * s, 6 * s)
            lineTo(91 * s, 6 * s)
            lineTo(96 * s, 20 * s)
            lineTo(92 * s, 122 * s)
            lineTo(8 * s, 122 * s)
            lineTo(4 * s, 20 * s)
            close()
        }
        drawPath(outline, p.top ?: p.bag)
        val lower = Path().apply {
            moveTo(6 * s, 38 * s)
            lineTo(94 * s, 38 * s)
            lineTo(92 * s, 122 * s)
            lineTo(8 * s, 122 * s)
            close()
        }
        drawPath(lower, p.bag)
        drawRect(Color.Black.copy(alpha = .15f), o(9f, 6f), Size(82 * s, 8 * s))
        val fs = minOf(18f, 150f / p.lb.length) * s
        val layout = tm.measure(
            p.lb,
            TextStyle(
                fontFamily = SquadaOne,
                fontWeight = FontWeight.Normal,
                fontSize = (fs / density).sp,
                color = p.tc
            )
        )
        drawText(
            layout,
            topLeft = Offset(50 * s - layout.size.width / 2f, 62 * s - layout.firstBaseline)
        )
        drawCircle(p.ac, 19 * s, o(50f, 96f))
        drawCircle(Color.White.copy(alpha = .5f), 19 * s, o(50f, 96f), style = Stroke(3 * s))
    }
}

// Basket Icon
@Composable
fun Basket(size: Dp, tint: Color) = Canvas(Modifier.size(size)) {
    val s = this.size.width / 24f
    val st = Stroke(2 * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(
        Path().apply {
            moveTo(4 * s, 10 * s)
            lineTo(20 * s, 10 * s)
            lineTo(18.5f * s, 19 * s)
            lineTo(5.5f * s, 19 * s)
            close()
        },
        tint,
        style = st
    )
    drawLine(tint, Offset(8 * s, 10 * s), Offset(11 * s, 5 * s), 2 * s, StrokeCap.Round)
    drawLine(tint, Offset(16 * s, 10 * s), Offset(13 * s, 5 * s), 2 * s, StrokeCap.Round)
}

// Tab Icons (15dp, stroke 2)
@Composable
fun TabIcon(cat: Cat, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(15.dp)) {
        val s = size.width / 24f
        val st = Stroke(2 * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (cat) {
            Cat.Choco -> {
                val path = Path().apply {
                    moveTo(12 * s, 3 * s)
                    lineTo(19 * s, 7 * s)
                    lineTo(19 * s, 17 * s)
                    lineTo(12 * s, 21 * s)
                    lineTo(5 * s, 17 * s)
                    lineTo(5 * s, 7 * s)
                    close()
                }
                drawPath(path, tint, style = st)
            }
            Cat.Chips -> {
                val path = Path().apply {
                    moveTo(5 * s, 14 * s)
                    cubicTo(5 * s, 9 * s, 10 * s, 5 * s, 19 * s, 6 * s)
                    cubicTo(18 * s, 15 * s, 14 * s, 20 * s, 9 * s, 19 * s)
                    close()
                }
                drawPath(path, tint, style = st)
            }
            Cat.Candy -> {
                drawLine(tint, Offset(7 * s, 7 * s), Offset(17 * s, 17 * s), 2 * s, StrokeCap.Round)
                drawLine(tint, Offset(5 * s, 9 * s), Offset(9 * s, 5 * s), 2 * s, StrokeCap.Round)
                drawLine(tint, Offset(15 * s, 19 * s), Offset(19 * s, 15 * s), 2 * s, StrokeCap.Round)
            }
        }
    }
}

// Round Button base
@Composable
fun RoundBtn(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(10.dp, CircleShape, ambientColor = Color(0x33503C8C), spotColor = Color(0x33503C8C))
            .clip(CircleShape)
            .background(Pale)
            .pressable(onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun BackBtn(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundBtn(onClick = onClick, modifier = modifier) {
        Text("‹", style = ts(24, FontWeight.Normal, Ink))
    }
}

@Composable
fun MenuBtn(onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    RoundBtn(onClick = onClick, modifier = modifier) {
        Canvas(Modifier.size(12.dp, 8.dp)) {
            val strokeWidth = 2.dp.toPx()
            drawLine(Ink, Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), strokeWidth, StrokeCap.Round)
            drawLine(Ink, Offset(0f, 7.dp.toPx()), Offset(size.width, 7.dp.toPx()), strokeWidth, StrokeCap.Round)
        }
    }
}

// Animations
fun Modifier.riseIn(index: Int) = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        a.animateTo(1f, tween(550, easing = Ease))
    }
    graphicsLayer {
        alpha = a.value
        translationY = (1 - a.value) * 30.dp.toPx()
        val s = .94f + .06f * a.value
        scaleX = s
        scaleY = s
    }
}

fun Modifier.pressable(onClick: () -> Unit) = composed {
    val src = remember { MutableInteractionSource() }
    val down by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (down) .97f else 1f, tween(200), label = "pressScale")
    graphicsLayer {
        scaleX = sc
        scaleY = sc
    }.clickable(src, indication = null, onClick = onClick)
}

// Product Card
@Composable
fun ProductCard(
    p: Product,
    index: Int,
    large: Boolean,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radius = if (large) 36.dp else 26.dp
    Box(
        modifier = modifier
            .riseIn(index)
            .pressable(onOpen)
            .then(if (large) Modifier.size(300.dp) else Modifier.fillMaxWidth().height(178.dp))
            .clip(RoundedCornerShape(radius))
            .background(p.bg)
    ) {
        Column(
            modifier = Modifier
                .padding(if (large) 22.dp else 14.dp)
                .fillMaxWidth(if (large) .5f else .58f)
        ) {
            Text(
                text = p.name,
                style = if (large) ts(28, FontWeight.Normal, lh = 1.05f, ls = -.5f) else ts(16, FontWeight.Normal, lh = 1.15f)
            )
            Text(
                text = p.label,
                style = ts(if (large) 13 else 11, FontWeight.Normal, Color(0xFF777777)),
                modifier = Modifier
                    .padding(top = if (large) 12.dp else 8.dp)
                    .background(Color.White.copy(.65f), RoundedCornerShape(if (large) 16.dp else 12.dp))
                    .padding(horizontal = if (large) 14.dp else 10.dp, vertical = if (large) 6.dp else 4.dp)
            )
        }

        Bag(
            p = p,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = if (large) 6.dp else 8.dp, y = if (large) 20.dp else 12.dp)
                .fillMaxWidth(.62f)
                .graphicsLayer { rotationZ = 9f }
                .shadow(10.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black.copy(.18f), spotColor = Color.Black.copy(.18f))
        )

        val price = "$ " + "%05.2f".format(p.price.toFloat())
        if (large) {
            Text(
                text = price,
                style = ts(17, FontWeight.Normal, Ink),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 22.dp, bottom = 20.dp)
                    .graphicsLayer { rotationZ = -6f }
                    .background(Color.White.copy(.55f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        } else {
            Text(
                text = price,
                style = ts(13, FontWeight.Normal, Ink),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 12.dp)
            )
        }

        val b = if (large) 52.dp else 32.dp
        val pad = if (large) 20.dp else 12.dp
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = pad, bottom = pad)
                .size(b)
                .clip(CircleShape)
                .background(Ink)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onAdd
                ),
            contentAlignment = Alignment.Center
        ) {
            Basket(if (large) 20.dp else 16.dp, Color.White)
        }
    }
}

// Make Payment slider
@Composable
fun PaySlider(onDone: () -> Unit, modifier: Modifier = Modifier) {
    var done by remember { mutableStateOf(false) }
    val x = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Track)
    ) {
        val max = with(LocalDensity.current) { (maxWidth - 64.dp).toPx() }
        Text(
            text = if (done) "Payment Successful" else "Make Payment",
            style = ts(16, FontWeight.Normal, Ink),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 34.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .offset { IntOffset(x.value.roundToInt(), 0) }
                .size(52.dp)
                .clip(CircleShape)
                .background(Yellow)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (x.value < -max * .85f) {
                                    x.animateTo(-max, tween(300))
                                    done = true
                                    delay(1800)
                                    onDone()
                                } else {
                                    x.animateTo(0f, tween(300))
                                }
                            }
                        },
                        onHorizontalDrag = { c, d ->
                            c.consume()
                            scope.launch {
                                x.snapTo((x.value + d).coerceIn(-max, 0f))
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (done) "✓" else "›",
                style = ts(22, FontWeight.Normal, Ink)
            )
        }
    }
}

// Floating Cart Bar (on Grid & Home)
@Composable
fun CartBar(
    cart: List<CartLine>,
    addEvents: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = cart.sumOf { it.q }
    val bumpAnim = remember { Animatable(0f) }

    LaunchedEffect(addEvents) {
        if (addEvents > 0) {
            bumpAnim.animateTo(-8f, tween(160, easing = Ease))
            bumpAnim.animateTo(0f, tween(240, easing = Ease))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .graphicsLayer { translationY = bumpAnim.value.dp.toPx() }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Ink)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = 22.dp, end = 22.dp, bottom = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Count badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Yellow),
                contentAlignment = Alignment.Center
            ) {
                Text(totalCount.toString(), style = ts(13, FontWeight.Normal, Ink))
            }

            // Labels
            Column {
                Text("Cart", style = ts(15, FontWeight.Normal, Color.White))
                Text("$totalCount item${if (totalCount > 1) "s" else ""}", style = ts(11, FontWeight.Normal, Grey))
            }

            Spacer(Modifier.weight(1f))

            // Overlapping bag thumbnails (last 4 items)
            Row(
                modifier = Modifier.padding(start = 10.dp),
                horizontalArrangement = Arrangement.spacedBy((-10).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val lastItems = cart.takeLast(4)
                for (item in lastItems) {
                    val prod = Products.find { it.id == item.id }
                    if (prod != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, Ink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Bag(prod, Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
