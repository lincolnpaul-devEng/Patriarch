package com.campus.network.feature.hub.snack

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// SCREEN 1: GRID ("Chips Collections")
// -------------------------------------------------------------
@Composable
fun GridScreen(vm: SnackVm, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    val gridProducts = remember { Products.filter { it.id != "dark" } }
    val leftCol = remember { gridProducts.filterIndexed { i, _ -> i % 2 == 0 } }
    val rightCol = remember { gridProducts.filterIndexed { i, _ -> i % 2 == 1 } }

    HomescreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 14.dp)
                .padding(horizontal = 22.dp)
                .verticalScroll(scrollState)
                .padding(bottom = 110.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(text = "Chips", style = ts(30, FontWeight.Normal, Ink, lh = 1.05f, ls = -.5f))
                    Text(text = "Collections", style = ts(26, FontWeight.Normal, Ink.copy(alpha = 0.85f), lh = 1.05f, ls = -.5f))
                }
                BackBtn(onClick = { vm.go(SnackScreen.Home) })
            }

            // Meta row (245 items + filter icon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "245 Items", style = ts(12, FontWeight.Normal, Color(0xFF333333)))
                Spacer(Modifier.width(8.dp))
                Canvas(Modifier.size(14.dp)) {
                    val sc = size.width / 24f
                    drawLine(Color(0xFF333333), Offset(4 * sc, 8 * sc), Offset(20 * sc, 8 * sc), 2 * sc, StrokeCap.Round)
                    drawLine(Color(0xFF333333), Offset(4 * sc, 16 * sc), Offset(20 * sc, 16 * sc), 2 * sc, StrokeCap.Round)
                    drawCircle(Color.White, 2.5f * sc, Offset(9 * sc, 8 * sc))
                    drawCircle(Color(0xFF333333), 2.5f * sc, Offset(9 * sc, 8 * sc), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f * sc))
                    drawCircle(Color.White, 2.5f * sc, Offset(15 * sc, 16 * sc))
                    drawCircle(Color(0xFF333333), 2.5f * sc, Offset(15 * sc, 16 * sc), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f * sc))
                }
            }

            // Two staggered columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    leftCol.forEachIndexed { idx, p ->
                        val originalIndex = idx * 2
                        ProductCard(
                            p = p,
                            index = originalIndex,
                            large = false,
                            onOpen = {
                                vm.current = p.id
                                vm.qty = 1
                                vm.go(SnackScreen.Detail)
                            },
                            onAdd = { vm.add(p.id, 1) }
                        )
                    }
                }

                // Right column (pushed down 34dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rightCol.forEachIndexed { idx, p ->
                        val originalIndex = idx * 2 + 1
                        ProductCard(
                            p = p,
                            index = originalIndex,
                            large = false,
                            onOpen = {
                                vm.current = p.id
                                vm.qty = 1
                                vm.go(SnackScreen.Detail)
                            },
                            onAdd = { vm.add(p.id, 1) }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2: HOME ("Order From The Best Of Snacks")
// -------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: SnackVm, modifier: Modifier = Modifier) {
    val categories = listOf<Cat?>(null, Cat.Choco, Cat.Chips, Cat.Candy)
    val catNames = mapOf(null to "All", Cat.Choco to "Choco", Cat.Chips to "Chips", Cat.Candy to "Candy")
    val filteredProducts = remember(vm.tab) {
        if (vm.tab == null) Products else Products.filter { it.cat == vm.tab }
    }
    val listState = rememberLazyListState()

    HomescreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 14.dp)
                .padding(bottom = 110.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(text = "Order From The", style = ts(26, FontWeight.Normal, Ink, lh = 1.1f, ls = -.5f))
                    Row {
                        Text(text = "Best Of ", style = ts(26, FontWeight.Normal, Ink, lh = 1.1f, ls = -.5f))
                        Text(text = "Snacks", style = ts(28, FontWeight.Normal, Ink, lh = 1.1f, ls = -.5f))
                    }
                }
                MenuBtn()
            }

            // Category Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 26.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { c ->
                    val isSelected = vm.tab == c
                    val bg by animateColorAsState(if (isSelected) Ink else TabBg, tween(300), label = "tabBg")
                    val textColor by animateColorAsState(if (isSelected) Color.White else Color(0xFF444444), tween(300), label = "tabText")
                    val iconTint = if (isSelected) Yellow else Color(0xFF444444)

                    Row(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(bg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { vm.tab = c }
                            )
                            .animateContentSize(tween(300))
                            .padding(horizontal = if (isSelected) 18.dp else 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (c != null) {
                            TabIcon(cat = c, tint = iconTint)
                            if (isSelected) {
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                        if (isSelected || c == null) {
                            Text(text = catNames[c] ?: "", style = ts(13, FontWeight.Normal, textColor))
                        }
                    }
                }
            }

            // Section Row ("Choco Collections ->")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = catNames[vm.tab] ?: "All", style = ts(20, FontWeight.Normal, Ink))
                    Spacer(Modifier.width(4.dp))
                    Text(text = " Collections", style = ts(20, FontWeight.Normal, Ink.copy(alpha = 0.8f)))
                }
                Text(
                    text = "→",
                    style = ts(24, FontWeight.Normal, Ink),
                    modifier = Modifier.pressable { vm.go(SnackScreen.Grid) }
                )
            }

            // Carousel with Large Cards
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            ) {
                itemsIndexed(
                    items = filteredProducts,
                    key = { _, p -> "${p.id}_${vm.tab}" }
                ) { index, product ->
                    ProductCard(
                        p = product,
                        index = index,
                        large = true,
                        onOpen = {
                            vm.current = product.id
                            vm.qty = 1
                            vm.go(SnackScreen.Detail)
                        },
                        onAdd = { vm.add(product.id, 1) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 3: DETAIL
// -------------------------------------------------------------
@Composable
fun DetailScreen(vm: SnackVm, modifier: Modifier = Modifier) {
    val product = remember(vm.current) { Products.find { it.id == vm.current } ?: Products.first() }
    val nameWords = remember(product.name) { product.name.split(" ") }
    val firstWord = nameWords.firstOrNull() ?: ""
    val otherWords = nameWords.drop(1).joinToString(" ")

    // Infinite gentle float animation
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val floatRot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatRot"
    )

    // Quantity pop scale animation
    var qtyScale by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(vm.qty) {
        qtyScale = 1.2f
        delay(160)
        qtyScale = 1f
    }
    val animatedQtyScale by animateFloatAsState(qtyScale, tween(160), label = "qtyScale")

    HomescreenBackground(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 14.dp, start = 22.dp, end = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(text = firstWord, style = ts(28, FontWeight.Normal, Ink, lh = 1.1f))
                        Text(text = otherWords, style = ts(30, FontWeight.Normal, Ink, lh = 1.1f))
                        Text(text = product.brand, style = ts(13, FontWeight.Normal, Grey), modifier = Modifier.padding(top = 6.dp))
                    }
                    BackBtn(onClick = { vm.go(vm.prev) })
                }

                // Centered Floating Product Bag Hero
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(210.dp)
                        .align(Alignment.CenterHorizontally)
                        .graphicsLayer {
                            translationY = floatY.dp.toPx()
                            rotationZ = floatRot
                        }
                        .shadow(20.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(0.2f), spotColor = Color.Black.copy(0.2f))
                        .clip(RoundedCornerShape(22.dp))
                        .background(product.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Bag(product, Modifier.fillMaxSize().padding(14.dp))
                }

                // Quantity Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 34.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundBtn(
                        onClick = { vm.qty++ },
                        size = 52.dp
                    ) {
                        Text("+", style = ts(26, FontWeight.Normal, Ink))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = vm.qty.toString(),
                            style = ts(38, FontWeight.Normal, Ink),
                            modifier = Modifier.graphicsLayer {
                                scaleX = animatedQtyScale
                                scaleY = animatedQtyScale
                            }
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Yellow)
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$ %.2f".format((product.price * vm.qty).toFloat()),
                                style = ts(16, FontWeight.Normal, Ink)
                            )
                        }
                    }

                    RoundBtn(
                        onClick = { if (vm.qty > 1) vm.qty-- },
                        size = 52.dp
                    ) {
                        Text("−", style = ts(26, FontWeight.Normal, Ink))
                    }
                }
            }

            // Fixed "Add To Cart" button at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(24.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x1A503C8C), spotColor = Color(0x1A503C8C))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            vm.add(product.id, vm.qty)
                            vm.go(SnackScreen.Cart)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add To Cart",
                    style = ts(16, FontWeight.Normal, Ink)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Basket(20.dp, Ink)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 4: CART
// -------------------------------------------------------------
@Composable
fun CartScreen(vm: SnackVm, modifier: Modifier = Modifier) {
    val subtotal = remember(vm.cart.toList()) {
        vm.cart.sumOf { line ->
            val p = Products.find { it.id == line.id }
            (p?.price ?: 0) * line.q
        }
    }
    val totalCount = vm.cart.sumOf { it.q }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 14.dp)
        ) {
            // Cart Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Cart", style = ts(32, FontWeight.Normal, Ink))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Yellow)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { vm.go(SnackScreen.Home) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = totalCount.toString(), style = ts(14, FontWeight.Normal, Ink))
                }
            }

            Spacer(Modifier.height(18.dp))

            // Dark Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .background(Panel)
                    .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
            ) {
                // Top Handle Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(34.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF444444))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 22.dp)
                ) {
                    // Scrollable Rows
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        vm.cart.forEachIndexed { i, line ->
                            val prod = Products.find { it.id == line.id }
                            if (prod != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .riseIn(i),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Bag(prod, Modifier.size(24.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = prod.name, style = ts(15, FontWeight.Normal, Color.White))
                                        Text(text = prod.brand, style = ts(11, FontWeight.Normal, Color(0xFF888888)), modifier = Modifier.padding(top = 2.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "$%02d.00".format(prod.price * line.q),
                                            style = ts(13, FontWeight.Normal, Ink)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Yellow Total Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 100.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Yellow)
                            .padding(18.dp)
                    ) {
                        Column {
                            // Delivery Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Delivery Amount", style = ts(12, FontWeight.Normal, Color(0xFF666655)))
                                Text(text = "$04.00", style = ts(13, FontWeight.Normal, Ink))
                            }

                            // 1dp Divider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.Black.copy(0.15f))
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(text = "Total Amount", style = ts(12, FontWeight.Normal, Color(0xFF666655)))
                            Text(
                                text = "USD %.2f".format((subtotal + 4).toFloat()),
                                style = ts(28, FontWeight.Normal, Ink)
                            )
                        }

                        // Overlapping Mini Bags in bottom-right corner
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy((-14).dp)
                        ) {
                            val miniItems = vm.cart.takeLast(3)
                            for (item in miniItems) {
                                val p = Products.find { it.id == item.id }
                                if (p != null) {
                                    Bag(
                                        p = p,
                                        modifier = Modifier
                                            .width(34.dp)
                                            .graphicsLayer { rotationZ = 10f }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Make Payment Slider (bottom 30dp)
        PaySlider(
            onDone = {
                vm.cart.clear()
                vm.go(SnackScreen.Home)
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp)
        )
    }
}

// -------------------------------------------------------------
// ROOT COMPOSABLE: SNACK ORDERING SCREEN
// -------------------------------------------------------------
@Composable
fun SnackOrderingScreen(
    vm: SnackVm = viewModel(),
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedContent(
            targetState = vm.screen,
            transitionSpec = {
                (fadeIn(tween(350)) + scaleIn(tween(350), initialScale = .97f)) togetherWith fadeOut(tween(350))
            },
            label = "SnackScreenFlow"
        ) { targetScreen ->
            when (targetScreen) {
                SnackScreen.Grid -> GridScreen(vm)
                SnackScreen.Home -> HomeScreen(vm)
                SnackScreen.Detail -> DetailScreen(vm)
                SnackScreen.Cart -> CartScreen(vm)
            }
        }

        // Floating Cart Bar on Grid & Home
        val showBar = (vm.screen == SnackScreen.Grid || vm.screen == SnackScreen.Home) && vm.cart.isNotEmpty()
        AnimatedVisibility(
            visible = showBar,
            enter = slideInVertically(tween(450, easing = Ease)) { it },
            exit = slideOutVertically(tween(450, easing = Ease)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CartBar(
                cart = vm.cart,
                addEvents = vm.addEvents,
                onClick = { vm.go(SnackScreen.Cart) }
            )
        }
    }

    BackHandler {
        when (vm.screen) {
            SnackScreen.Detail -> vm.go(vm.prev)
            SnackScreen.Cart -> vm.go(SnackScreen.Home)
            SnackScreen.Home -> vm.go(SnackScreen.Grid)
            SnackScreen.Grid -> {
                if (onNavigateBack != null) onNavigateBack()
            }
        }
    }
}
