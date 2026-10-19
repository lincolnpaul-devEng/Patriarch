package com.campus.network.feature.hub.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campus.network.core.model.CampusSecurityPolicy
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MarketplaceAccessControl
import com.campus.network.core.model.MarketplaceRolePerspective
import com.campus.network.core.ui.components.ShimmerAsyncImage
import kotlinx.coroutines.launch

private val BrandBluePrimary = Color(0xFF014784)
private val BrandBlueAction = Color(0xFF0284C7)
private val BrandBlueSoft = Color(0xFFE0F2FE)
private val TextDarkPrimary = Color(0xFF0F172A)
private val TextSlateSecondary = Color(0xFF475569)
private val TextMuted = Color(0xFF64748B)
private val DividerLight = Color(0xFFE2E8F0)
private val SectionSeparatorBg = Color(0xFFF1F5F9)
private val EmeraldSafe = Color(0xFF059669)
private val EmeraldSoft = Color(0xFFECFDF5)
private val RoseRareAccent = Color(0xFFE11D48)

data class ProductBasketConfig(
    val item: MarketItem,
    val handoverOptionTitle: String,
    val handoverFee: Double,
    val packageOptionTitle: String,
    val packageExtraPrice: Double,
    val quantity: Int,
    val buyerNote: String,
    val isOrderConfirmedInEscrow: Boolean = false,
    val orderReference: String = "ORD-${(1000..9999).random()}"
) {
    val unitPriceWithOptions: Double
        get() = item.price + handoverFee + packageExtraPrice

    val totalAmount: Double
        get() = unitPriceWithOptions * quantity
}

private data class HandoverOption(
    val rankBadge: String,
    val title: String,
    val subtitle: String,
    val extraFee: Double
)

private data class PackageSelectOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val extraPrice: Double
)

/**
 * Full-Screen Product Details Page replicating Reference Design 1 (Airbnb-inspired listing view)
 * with progressive transitions to:
 * 1. Product Options & Add-to-Basket Drawer Sheet (Reference Design 2 - Uber Eats style)
 * 2. Basket & SafeTrade Escrow Checkout Sheet
 * 3. Post-Purchase Order Confirmation with Direct Seller Messaging & QR Scanner Release
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MarketProductDetailsFullScreen(
    item: MarketItem,
    isSaved: Boolean,
    basketConfig: ProductBasketConfig?,
    onToggleSave: () -> Unit,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenOptionsSheet: () -> Unit,
    onOpenCheckoutSheet: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onMessageSeller: (sellerName: String, initialOrderMessage: String?) -> Unit,
    onOpenMapLocation: (Double, Double, String) -> Unit,
    currentUserName: String = "Student",
    onSecurityNotice: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val galleryUrls = remember(item.id, item.imageUrl, item.imageUrls) {
        val urls = item.imageUrls.filter { it.isNotBlank() }
        if (urls.isNotEmpty()) {
            urls
        } else if (!item.imageUrl.isNullOrBlank()) {
            listOf(item.imageUrl)
        } else {
            emptyList()
        }
    }

    val pagerState = rememberPagerState(pageCount = { galleryUrls.size.coerceAtLeast(1) })
    val pagerScope = androidx.compose.runtime.rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val reviewCount = remember(item.id) { 94 + (item.id.hashCode().let { if (it < 0) -it else it } % 180) }

    val isOrderPurchased = basketConfig?.isOrderConfirmedInEscrow == true
    val isInBasket = basketConfig != null && !basketConfig.isOrderConfirmedInEscrow

    val naturalOwnerMatch = remember(item.sellerName, currentUserName) {
        val u = currentUserName.trim().lowercase()
        val s = item.sellerName.trim().lowercase()
        u.isNotEmpty() && (s == u || s.startsWith(u.substringBefore(" ")) || u.startsWith(s.substringBefore(" ")))
    }
    var forceSellerPerspective by remember(item.id, naturalOwnerMatch) {
        mutableStateOf(naturalOwnerMatch)
    }
    var isSellerReleaseCodeRevealed by remember(item.id) { mutableStateOf(false) }
    var isSellerQrModalOpen by remember(item.id) { mutableStateOf(false) }

    val accessControl = remember(
        item,
        currentUserName,
        isOrderPurchased,
        forceSellerPerspective,
        isSellerReleaseCodeRevealed
    ) {
        CampusSecurityPolicy.evaluateMarketplacePermissions(
            item = item,
            currentUserName = currentUserName,
            isOrderPurchased = isOrderPurchased,
            forceSellerPerspective = forceSellerPerspective,
            isSellerCodeRevealed = isSellerReleaseCodeRevealed
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 110.dp)
        ) {
            // 1. Edge-to-Edge Swipeable Hero Photo Gallery with Floating Controls & Counter Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(Color(0xFF0F172A))
            ) {
                if (galleryUrls.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        ShimmerAsyncImage(
                            model = galleryUrls[page],
                            contentDescription = "${item.title} photo ${page + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = item.title,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                // Subtle top scrim for floating icon legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Top Navigation Overlay Row: Circular Back on left, Share + Heart on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onBackClick,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.94f),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextDarkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onShareClick,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.94f),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share listing",
                                    tint = TextDarkPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onToggleSave,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.94f),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Save listing",
                                    tint = if (isSaved) RoseRareAccent else TextDarkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom-Right Photo Counter Pill ("1 / 5") matching Reference Image 1
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC1E293B),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 32.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${galleryUrls.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // 2. Overlapping Rounded White Sheet Body (matches Reference Image 1 card sheet)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-20).dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                ) {
                    // Interactive Multi-Photo Thumbnail Strip (Jump to any uploaded photo)
                    if (galleryUrls.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            itemsIndexed(galleryUrls) { idx, photoUrl ->
                                val isCurrentPage = pagerState.currentPage == idx
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isCurrentPage) 2.dp else 1.dp,
                                            color = if (isCurrentPage) BrandBluePrimary else DividerLight,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            pagerScope.launch {
                                                pagerState.animateScrollToPage(idx)
                                            }
                                        }
                                ) {
                                    ShimmerAsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Photo thumbnail ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    MarketplaceRoleSecurityCard(
                        accessControl = accessControl,
                        item = item,
                        onTogglePerspective = { isSeller ->
                            forceSellerPerspective = isSeller
                            if (!isSeller) isSellerReleaseCodeRevealed = false
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (accessControl.canViewSellerSecretQrTicket) {
                        SellerSafeTradeQrTicketCard(
                            item = item,
                            maskedOrVisibleCode = accessControl.maskedOrVisibleReleaseCode,
                            isCodeRevealed = isSellerReleaseCodeRevealed,
                            onToggleRevealCode = { isSellerReleaseCodeRevealed = !isSellerReleaseCodeRevealed },
                            onOpenFullscreenQr = { isSellerQrModalOpen = true },
                            onSharePickupTicket = {
                                RealQrCodeUtils.sharePickupCode(
                                    context = context,
                                    code = item.qrReleaseCode,
                                    itemTitle = item.title,
                                    priceKes = item.price,
                                    spotName = item.pickupSpotName
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // Active Order Banner if already reserved in SafeTrade Escrow (buyer only)
                    if (!accessControl.isSellerOwner && isOrderPurchased && basketConfig != null) {
                        ActiveEscrowOrderBanner(
                            config = basketConfig,
                            onMessageSeller = {
                                onMessageSeller(
                                    item.sellerName,
                                    "Hi ${item.sellerName.substringBefore(" ")}, I have reserved '${item.title}' (${basketConfig.orderReference}) in SafeTrade Escrow for ${item.currency} ${"%,.0f".format(basketConfig.totalAmount)}. When can we meet at ${item.pickupSpotName}?"
                                )
                            },
                            onScanQrClick = onOpenQrScanner
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Product Title
                    Text(
                        text = item.title,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        lineHeight = 31.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Location Subtitle: exact hall/dorm only after escrow lock (or for the owner)
                    Text(
                        text = "Available at ${item.pickupSpotName}, " + if (accessControl.canViewExactPickupCoordinates) {
                            item.campusHallLocation
                        } else {
                            "exact bay revealed after escrow"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDarkPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Specs / Attributes Line ("Condition • Category • Pickup Timing • SafeTrade")
                    Text(
                        text = "${item.condition} • ${item.category} • ${item.pickupTiming} • SafeTrade Escrow",
                        fontSize = 13.5.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Three-Column Outlined "Campus favorite" Badge Card (exact match to Reference Image 1)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, DividerLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Left Column: Rating + 5 mini stars
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = String.format("%.2f", item.sellerRating),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDarkPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                                    repeat(5) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = TextDarkPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(DividerLight)
                            )

                            // Center Column: Laurel Wreath + "Campus favorite"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1.35f)
                            ) {
                                LaurelWreathBranch(isLeft = true, modifier = Modifier.size(width = 16.dp, height = 32.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Campus\nfavorite",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary,
                                    lineHeight = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                LaurelWreathBranch(isLeft = false, modifier = Modifier.size(width = 16.dp, height = 32.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(DividerLight)
                            )

                            // Right Column: Review count + underlined "Reviews"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$reviewCount",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDarkPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Verified Trades",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDarkPrimary,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                    HorizontalDivider(color = DividerLight, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Rare Find Callout Row (Diamond/Gem Icon + Text, matching Reference Image 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        RareFindGemIcon(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "This is a rare campus find",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${item.sellerName.substringBefore(" ")}'s listings in ${item.category} are usually reserved within a few hours.",
                                fontSize = 13.5.sp,
                                color = TextMuted,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = DividerLight, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    // 5. Seller / Host Profile Row with Direct "Message" Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(52.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = BrandBluePrimary,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val initials = item.sellerName
                                        .split(" ")
                                        .filter { it.isNotBlank() }
                                        .take(2)
                                        .joinToString("") { it.first().uppercase() }
                                    Text(
                                        text = initials.ifEmpty { "S" },
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (item.isCampusVerifiedSeller) {
                                Surface(
                                    shape = CircleShape,
                                    color = RoseRareAccent,
                                    border = BorderStroke(2.dp, Color.White),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified Student Seller",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Listed by ${item.sellerName}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Verified Student Seller • 99% response rate",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }

                        if (accessControl.isSellerOwner) {
                            OutlinedButton(
                                onClick = { isSellerQrModalOpen = true },
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.2.dp, BrandBluePrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.QrCode2, null, tint = BrandBluePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Seller QR", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandBluePrimary)
                            }
                        } else OutlinedButton(
                            onClick = {
                                onMessageSeller(
                                    item.sellerName,
                                    "Hi ${item.sellerName.substringBefore(" ")}, I'm interested in your listing '${item.title}' (${item.currency} ${"%,.0f".format(item.price)}) at ${item.pickupSpotName}. Is it still available?"
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.2.dp, TextDarkPrimary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = TextDarkPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Message",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDarkPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = DividerLight, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    // 6. Highlight Callouts (SafeTrade Escrow & Campus Pickup Spot)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Protected by ${item.payoutMethod}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (accessControl.isSellerOwner) {
                                    "Buyer funds are locked in SafePay Escrow before meetup. Present your Seller QR Ticket only after handing over the item."
                                } else {
                                    "Your payment stays locked in escrow until you inspect the item in person and scan the seller's release QR code (${accessControl.maskedOrVisibleReleaseCode})."
                                },
                                fontSize = 13.5.sp,
                                color = TextMuted,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = TextDarkPrimary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.pickupSpotName} (${item.pickupSecurityLevel})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = accessControl.pickupLocationSecurityNotice,
                                fontSize = 13.5.sp,
                                color = TextMuted,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (accessControl.canViewExactPickupCoordinates) {
                                Text(
                                    text = "View verified handoff pin on Campus Map →",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSafe,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        onOpenMapLocation(item.pickupLatitude, item.pickupLongitude, item.pickupSpotName)
                                    }
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSecurityNotice?.invoke(
                                                "Exact GPS handoff coordinates are locked until SafePay Escrow is reserved."
                                            )
                                            onOpenOptionsSheet()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Exact GPS pin locked. Tap to reserve in SafePay Escrow",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = DividerLight, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    // 7. About this item description
                    Text(
                        text = "About this item",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.description.ifBlank {
                            "Well-maintained ${item.title} in ${item.condition.lowercase()} condition. Ideal for coursework, studio projects, and daily campus use. Stored carefully at ${item.campusHallLocation} and available for full inspection at ${item.pickupSpotName} before escrow release."
                        },
                        fontSize = 14.5.sp,
                        color = TextSlateSecondary,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // 8. Sticky Bottom Reservation / Checkout Bar (Exact match to Reference Image 1 bottom bar)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = DividerLight, thickness = 0.8.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Price underlined + SafeTrade Escrow pill
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            val displayAmount = basketConfig?.totalAmount ?: item.price
                            Text(
                                text = "${item.currency} ${"%,.0f".format(displayAmount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDarkPrimary,
                                textDecoration = TextDecoration.Underline
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (basketConfig != null) "for ${basketConfig.quantity} item(s)" else "total",
                                fontSize = 12.5.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Pill badge ("SafeTrade Escrow" or "Free cancellation")
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SectionSeparatorBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isOrderPurchased) EmeraldSafe else TextDarkPrimary,
                                    modifier = Modifier.size(15.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        accessControl.isSellerOwner -> "Seller QR Ticket Ready"
                                        isOrderPurchased -> "Escrow Locked • Pin Unlocked"
                                        else -> "SafePay Escrow Protected"
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                    }

                    // Right side: Progressive CTA button
                    when {
                        accessControl.isSellerOwner -> {
                            Button(
                                onClick = { isSellerQrModalOpen = true },
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Show Seller QR",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        isOrderPurchased -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        onMessageSeller(
                                            item.sellerName,
                                            "Hi ${item.sellerName.substringBefore(" ")}, I'm heading to ${item.pickupSpotName} for '${item.title}' (${basketConfig?.orderReference})."
                                        )
                                    },
                                    shape = RoundedCornerShape(26.dp),
                                    border = BorderStroke(1.2.dp, BrandBluePrimary),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = BrandBluePrimary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Chat",
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBluePrimary
                                    )
                                }

                                Button(
                                    onClick = onOpenQrScanner,
                                    shape = RoundedCornerShape(26.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Scan QR",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        isInBasket -> {
                            Button(
                                onClick = onOpenCheckoutSheet,
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Checkout (${basketConfig?.quantity ?: 1})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        else -> {
                            Button(
                                onClick = onOpenOptionsSheet,
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 0.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text(
                                    text = "Reserve / Buy",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isSellerQrModalOpen && accessControl.canViewSellerSecretQrTicket) {
            SellerQrTicketModalSheet(
                item = item,
                isCodeRevealed = isSellerReleaseCodeRevealed,
                onToggleRevealCode = { isSellerReleaseCodeRevealed = !isSellerReleaseCodeRevealed },
                onDismiss = { isSellerQrModalOpen = false },
                onShareTicket = {
                    RealQrCodeUtils.sharePickupCode(
                        context = context,
                        code = item.qrReleaseCode,
                        itemTitle = item.title,
                        priceKes = item.price,
                        spotName = item.pickupSpotName
                    )
                }
            )
        }
    }
}

/** Role banner: shows who the viewer is (buyer/seller) and lets a listing owner preview the buyer view. */
@Composable
private fun MarketplaceRoleSecurityCard(
    accessControl: MarketplaceAccessControl,
    item: MarketItem,
    onTogglePerspective: (isSeller: Boolean) -> Unit
) {
    val isSeller = accessControl.isSellerOwner
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSeller) BrandBlueSoft else SectionSeparatorBg,
        border = BorderStroke(1.dp, if (isSeller) BrandBlueAction.copy(alpha = 0.4f) else DividerLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSeller) Icons.Default.Verified else Icons.Default.Lock,
                contentDescription = null,
                tint = BrandBluePrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accessControl.rolePerspective.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Text(
                    text = if (isSeller) "You can see your release QR; buyers cannot." else "Release code is hidden: ${accessControl.maskedOrVisibleReleaseCode}",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }
            Text(
                text = if (isSeller) "Preview as buyer" else "Seller view",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBluePrimary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onTogglePerspective(!isSeller) }
            )
        }
    }
}

/** Seller-only ticket card. The code stays masked until the seller explicitly reveals it. */
@Composable
private fun SellerSafeTradeQrTicketCard(
    item: MarketItem,
    maskedOrVisibleCode: String,
    isCodeRevealed: Boolean,
    onToggleRevealCode: () -> Unit,
    onOpenFullscreenQr: () -> Unit,
    onSharePickupTicket: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EmeraldSoft,
        border = BorderStroke(1.2.dp, EmeraldSafe.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode2, null, tint = EmeraldSafe, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Seller SafeTrade Release Ticket",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF065F46)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Show this to the buyer in person at ${item.pickupSpotName} only after they have inspected the item.",
                fontSize = 12.5.sp,
                color = Color(0xFF047857),
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = maskedOrVisibleCode,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = BrandBluePrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleRevealCode) {
                    Icon(
                        imageVector = if (isCodeRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isCodeRevealed) "Hide code" else "Reveal code",
                        tint = TextDarkPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenFullscreenQr,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                    modifier = Modifier.weight(1f)
                ) { Text("Show QR", fontSize = 12.5.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = onSharePickupTicket,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Share Ticket", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = EmeraldSafe) }
            }
        }
    }
}

/** Seller-only full QR sheet. Only reachable when the viewer is the listing owner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SellerQrTicketModalSheet(
    item: MarketItem,
    isCodeRevealed: Boolean,
    onToggleRevealCode: () -> Unit,
    onDismiss: () -> Unit,
    onShareTicket: () -> Unit
) {
    val qrBitmap = remember(item.id, item.qrReleaseCode) {
        RealQrCodeUtils.generateQrBitmap(item.qrReleaseCode, 640).asImageBitmap()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Seller Release QR", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.title} • ${item.pickupSpotName}",
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                bitmap = qrBitmap,
                contentDescription = "Release QR code",
                modifier = Modifier
                    .size(240.dp)
                    .border(1.dp, DividerLight, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CampusSecurityPolicy.maskReleaseCode(item.qrReleaseCode, true, isCodeRevealed),
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = BrandBluePrimary
                )
                IconButton(onClick = onToggleRevealCode) {
                    Icon(
                        imageVector = if (isCodeRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isCodeRevealed) "Hide code" else "Reveal code"
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onShareTicket,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) { Text("Share Pickup Ticket", fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * Stage 2: Product Options & Add-to-Basket Modal Sheet
 * Replicates Reference Image 2 (Uber Eats-style customization & Add to Basket sheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductOptionsBasketDrawerSheet(
    item: MarketItem?,
    isOpen: Boolean,
    initialConfig: ProductBasketConfig? = null,
    onDismiss: () -> Unit,
    onShareClick: () -> Unit,
    onAddToBasket: (ProductBasketConfig) -> Unit
) {
    if (!isOpen || item == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val handoverOptions = remember(item.id) {
        listOf(
            HandoverOption(
                rankBadge = "#1, Chosen by 100+ students",
                title = "Meet at ${item.pickupSpotName}",
                subtitle = "${item.pickupSecurityLevel} • Free SafeTrade pickup",
                extraFee = 0.0
            ),
            HandoverOption(
                rankBadge = "#2, Express Hall Runner",
                title = "Deliver to ${item.campusHallLocation} Entrance",
                subtitle = "Verified peer courier brings item in 25 mins • +${item.currency} 150",
                extraFee = 150.0
            )
        )
    }

    val packageOptions = remember(item.id) {
        listOf(
            PackageSelectOption(
                id = "standard",
                title = "${item.title} (${item.condition})",
                subtitle = "Popular • Includes 15-min live testing before QR release",
                extraPrice = 0.0
            ),
            PackageSelectOption(
                id = "bundle",
                title = "${item.title} + Accessory / Study Pack",
                subtitle = "+${item.currency} 300.00 • Includes protective pouch or revision notes",
                extraPrice = 300.0
            ),
            PackageSelectOption(
                id = "priority_hold",
                title = "${item.title} + 48-Hour Priority Escrow Hold",
                subtitle = "+${item.currency} 100.00 • Reserves item exclusively while you arrange pickup",
                extraPrice = 100.0
            )
        )
    }

    var selectedHandoverIndex by remember(item.id) { mutableIntStateOf(0) }
    var selectedPackageId by remember(item.id) { mutableStateOf(packageOptions.first().id) }
    var quantity by remember(item.id) { mutableIntStateOf(initialConfig?.quantity ?: 1) }
    var buyerNote by remember(item.id) { mutableStateOf(initialConfig?.buyerNote ?: "") }

    val selectedHandover = handoverOptions[selectedHandoverIndex]
    val selectedPackage = packageOptions.first { it.id == selectedPackageId }
    val unitTotal = item.price + selectedHandover.extraFee + selectedPackage.extraPrice
    val basketTotal = unitTotal * quantity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp)
            ) {
                // 1. Hero Product Image with Floating Circular X Close & Share Buttons (Reference Image 2)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(225.dp)
                        .background(Color(0xFF0F172A))
                ) {
                    if (!item.imageUrl.isNullOrBlank()) {
                        ShimmerAsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = item.title,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.95f),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close options sheet",
                                    tint = TextDarkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onShareClick,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.95f),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share item",
                                    tint = TextDarkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Title, Specs Subtitle, Price & "#1 most liked" Badge (Reference Image 2)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.condition} • ${item.category} • Listed by ${item.sellerName}",
                        fontSize = 13.5.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${item.currency} ${"%,.2f".format(item.price)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // "#1 most liked" pill badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SectionSeparatorBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = null,
                                tint = TextSlateSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "#1 most liked in ${item.category}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSlateSecondary
                            )
                        }
                    }
                }

                // 3. "Most popular" Horizontal Scrollable Cards Section (Reference Image 2)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Most popular",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(handoverOptions) { idx, option ->
                            val isSelected = selectedHandoverIndex == idx
                            Surface(
                                onClick = { selectedHandoverIndex = idx },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(
                                    width = if (isSelected) 1.8.dp else 1.dp,
                                    color = if (isSelected) BrandBluePrimary else DividerLight
                                ),
                                modifier = Modifier.width(292.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.rankBadge,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBlueAction
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = option.title,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDarkPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = option.subtitle,
                                            fontSize = 12.5.sp,
                                            color = TextMuted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedHandoverIndex = idx },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = BrandBluePrimary,
                                            unselectedColor = Color(0xFFCBD5E1)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Thick gray section divider matching Uber Eats sheet style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(SectionSeparatorBg)
                )

                // 4. "Select Package / Inspection" Section with "Required" Pill Badge (Reference Image 2)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Select Option",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Choose 1",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SectionSeparatorBg
                        ) {
                            Text(
                                text = "Required",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSlateSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    packageOptions.forEachIndexed { index, option ->
                        val isSelected = selectedPackageId == option.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPackageId = option.id }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.subtitle,
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPackageId = option.id },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BrandBluePrimary,
                                    unselectedColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                        if (index < packageOptions.lastIndex) {
                            HorizontalDivider(color = DividerLight, thickness = 0.7.dp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(SectionSeparatorBg)
                )

                // 5. Meetup Note & Quantity Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = "Pickup note for ${item.sellerName.substringBefore(" ")} (Optional)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = buyerNote,
                        onValueChange = { buyerNote = it },
                        placeholder = {
                            Text(
                                text = "E.g. I can meet at ${item.pickupSpotName} around 2:00 PM after lecture...",
                                fontSize = 13.5.sp,
                                color = TextMuted
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBluePrimary,
                            unfocusedBorderColor = DividerLight,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Quantity Stepper Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quantity",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                onClick = { if (quantity > 1) quantity-- },
                                shape = CircleShape,
                                color = SectionSeparatorBg,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease quantity",
                                        tint = if (quantity > 1) TextDarkPrimary else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = "$quantity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )

                            Surface(
                                onClick = { if (quantity < 5) quantity++ },
                                shape = CircleShape,
                                color = SectionSeparatorBg,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase quantity",
                                        tint = TextDarkPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Sticky Bottom Full-Width CTA: "Add 1 to basket • KES X,XXX.XX" (Exact match to Reference Image 2)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = DividerLight, thickness = 0.8.dp)
                    Button(
                        onClick = {
                            onAddToBasket(
                                ProductBasketConfig(
                                    item = item,
                                    handoverOptionTitle = selectedHandover.title,
                                    handoverFee = selectedHandover.extraFee,
                                    packageOptionTitle = selectedPackage.title,
                                    packageExtraPrice = selectedPackage.extraPrice,
                                    quantity = quantity,
                                    buyerNote = buyerNote.trim()
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDarkPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Add $quantity to basket • ${item.currency} ${"%,.2f".format(basketTotal)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stage 3: Progressive Basket Checkout & SafeTrade Escrow Confirmation Drawer Sheet.
 * Only after locking payment in SafeTrade Escrow does the user unlock the QR Scanner release flow
 * and direct Order Chat with the seller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasketEscrowCheckoutSheet(
    config: ProductBasketConfig?,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onConfirmEscrowPurchase: (ProductBasketConfig) -> Unit,
    onMessageSeller: (sellerName: String, initialOrderMessage: String) -> Unit,
    onOpenQrScanner: () -> Unit
) {
    if (!isOpen || config == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val item = config.item
    var selectedPaymentMethod by remember { mutableStateOf("M-Pesa Express (STK Push)") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            if (!config.isOrderConfirmedInEscrow) {
                // STEP A: BASKET REVIEW & SAFETRADE ESCROW LOCK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BrandBlueSoft,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Your SafeTrade Basket",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDarkPrimary
                            )
                            Text(
                                text = "Seller: ${item.sellerName} • ${item.pickupSpotName}",
                                fontSize = 12.5.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Item Summary Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, DividerLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!item.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = item.title,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${config.quantity}x ${config.packageOptionTitle}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = config.handoverOptionTitle,
                                    fontSize = 12.5.sp,
                                    color = BrandBluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (config.buyerNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Note: \"${config.buyerNote}\"",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = "${item.currency} ${"%,.0f".format(config.totalAmount)}",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDarkPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DividerLight, thickness = 0.7.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Item Subtotal (${config.quantity}x)", fontSize = 13.sp, color = TextMuted)
                            Text("${item.currency} ${"%,.0f".format((item.price + config.packageExtraPrice) * config.quantity)}", fontSize = 13.sp, color = TextDarkPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Handover / Delivery", fontSize = 13.sp, color = TextMuted)
                            Text(
                                text = if (config.handoverFee == 0.0) "FREE" else "${item.currency} ${"%,.0f".format(config.handoverFee)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (config.handoverFee == 0.0) EmeraldSafe else TextDarkPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SafeTrade Escrow Protection", fontSize = 13.sp, color = TextMuted)
                            Text("INCLUDED", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = EmeraldSafe)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Payment Method Selection
                Text(
                    text = "Escrow Funding Method",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val paymentMethods = listOf(
                    "M-Pesa Express (STK Push)" to "Instant mobile money lock • Zero fee",
                    "Campus SafePay Wallet" to "Available balance • Instant settlement",
                    "Student Debit / Virtual Card" to "Visa / Mastercard protected"
                )

                paymentMethods.forEach { (method, desc) ->
                    val selected = selectedPaymentMethod == method
                    Surface(
                        onClick = { selectedPaymentMethod = method },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) BrandBlueSoft.copy(alpha = 0.4f) else Color.White,
                        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) BrandBluePrimary else DividerLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(method, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                                Text(desc, fontSize = 12.sp, color = TextMuted)
                            }
                            RadioButton(
                                selected = selected,
                                onClick = { selectedPaymentMethod = method },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandBluePrimary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Escrow How-It-Works Info Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldSoft,
                    border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldSafe,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Your ${item.currency} ${"%,.0f".format(config.totalAmount)} is held securely in Campus SafePay Escrow. ${item.sellerName} only receives payout after you meet, inspect the item, and scan their QR code.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF065F46),
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onConfirmEscrowPurchase(config.copy(isOrderConfirmedInEscrow = true))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirm & Lock Escrow • ${item.currency} ${"%,.0f".format(config.totalAmount)}",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // STEP B: POST-PURCHASE ORDER CONFIRMED -> MESSAGE SELLER OR SCAN QR AT HANDOVER
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldSoft,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSafe,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BrandBlueSoft
                    ) {
                        Text(
                            text = "ESCROW ORDER ${config.orderReference}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandBluePrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Reserved in SafeTrade Escrow!",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${item.currency} ${"%,.0f".format(config.totalAmount)} is locked safely. Message ${item.sellerName} to coordinate your handover at ${item.pickupSpotName}.",
                        fontSize = 13.5.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Handover Checklist Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, DividerLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Next Steps to Complete Trade",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "1. Message ${item.sellerName} to confirm meetup time at ${item.pickupSpotName}.",
                                fontSize = 13.sp,
                                color = TextSlateSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "2. Inspect '${item.title}' in person when you meet.",
                                fontSize = 13.sp,
                                color = TextSlateSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "3. Tap 'Scan Seller QR / Release Code' below to release funds (code ${CampusSecurityPolicy.maskReleaseCode(item.qrReleaseCode, false)}).",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldSafe
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary CTA 1: Message Seller Directly (Opens Chat Thread with Order Context)
                    Button(
                        onClick = {
                            val noteSuffix = if (config.buyerNote.isNotBlank()) " Note: ${config.buyerNote}" else ""
                            onMessageSeller(
                                item.sellerName,
                                "Hi ${item.sellerName.substringBefore(" ")}, I just locked ${item.currency} ${"%,.0f".format(config.totalAmount)} in SafeTrade Escrow for '${config.packageOptionTitle}' (${config.orderReference}). Handover option: ${config.handoverOptionTitle}.$noteSuffix When can we meet?"
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Message Seller (${item.sellerName})",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary CTA 2: Scan Seller QR Code at Pickup Spot (Opens SafeTradeEscrowBuyerSheet)
                    OutlinedButton(
                        onClick = onOpenQrScanner,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, EmeraldSafe),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = EmeraldSafe,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "At Pickup Spot? Scan Seller QR Code",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSafe
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ActiveEscrowOrderBanner(
    config: ProductBasketConfig,
    onMessageSeller: () -> Unit,
    onScanQrClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EmeraldSoft,
        border = BorderStroke(1.2.dp, EmeraldSafe.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = EmeraldSafe,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Order ${config.orderReference} Reserved in Escrow",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF065F46)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Meet ${config.item.sellerName} at ${config.item.pickupSpotName}. Scan their QR code only after inspecting the item.",
                fontSize = 12.5.sp,
                color = Color(0xFF047857),
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onMessageSeller,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Message Seller", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onScanQrClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Seller QR", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Custom Canvas Laurel Wreath Branch matching Reference Image 1's "Guest favorite" / "Campus favorite" emblem.
 */
@Composable
private fun LaurelWreathBranch(
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeColor = Color(0xFF0F172A)
        val path = Path().apply {
            if (isLeft) {
                moveTo(w * 0.85f, h * 0.92f)
                cubicTo(
                    w * 0.15f, h * 0.72f,
                    w * 0.15f, h * 0.28f,
                    w * 0.80f, h * 0.08f
                )
            } else {
                moveTo(w * 0.15f, h * 0.92f)
                cubicTo(
                    w * 0.85f, h * 0.72f,
                    w * 0.85f, h * 0.28f,
                    w * 0.20f, h * 0.08f
                )
            }
        }
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )

        // Small olive leaves along the stem
        val leafOffsets = listOf(0.25f, 0.50f, 0.75f)
        leafOffsets.forEach { t ->
            val y = h * t
            val xBase = if (isLeft) w * 0.35f else w * 0.65f
            val xTip = if (isLeft) w * 0.08f else w * 0.92f
            drawLine(
                color = strokeColor,
                start = Offset(xBase, y),
                end = Offset(xTip, y - h * 0.08f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Custom Canvas Faceted Gem Icon matching Reference Image 1's "This is a rare find" diamond icon.
 */
@Composable
private fun RareFindGemIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gemColor = RoseRareAccent

        val outer = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.15f)
            lineTo(w * 0.95f, h * 0.42f)
            lineTo(w * 0.50f, h * 0.90f)
            lineTo(w * 0.05f, h * 0.42f)
            close()
        }
        drawPath(
            path = outer,
            color = gemColor,
            style = Stroke(width = 4.2f, cap = StrokeCap.Round)
        )

        // Horizontal crown facet line
        drawLine(
            color = gemColor,
            start = Offset(w * 0.07f, h * 0.42f),
            end = Offset(w * 0.93f, h * 0.42f),
            strokeWidth = 3.5f
        )

        // Inner facets
        drawLine(
            color = gemColor,
            start = Offset(w * 0.25f, h * 0.15f),
            end = Offset(w * 0.36f, h * 0.42f),
            strokeWidth = 3f
        )
        drawLine(
            color = gemColor,
            start = Offset(w * 0.75f, h * 0.15f),
            end = Offset(w * 0.64f, h * 0.42f),
            strokeWidth = 3f
        )
        drawLine(
            color = gemColor,
            start = Offset(w * 0.36f, h * 0.42f),
            end = Offset(w * 0.50f, h * 0.90f),
            strokeWidth = 3f
        )
        drawLine(
            color = gemColor,
            start = Offset(w * 0.64f, h * 0.42f),
            end = Offset(w * 0.50f, h * 0.90f),
            strokeWidth = 3f
        )
    }
}
