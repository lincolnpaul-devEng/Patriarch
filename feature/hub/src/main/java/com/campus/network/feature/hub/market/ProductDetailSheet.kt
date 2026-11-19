package com.campus.network.feature.hub.market

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.ui.components.HomescreenInk
import com.campus.network.core.ui.components.HomescreenOrange
import com.campus.network.core.ui.components.drawerDotGrid
import com.campus.network.feature.hub.HubViewModel
import com.campus.network.feature.hub.bestfootball.DText
import com.campus.network.feature.hub.bestfootball.SquadaOne
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

private val BuySuccessGreen = Color(0xFF00A651)
private val BuyErrorRed = Color(0xFFB3261E)

private fun normalizeBuyerPhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when {
        digits.startsWith("254") -> digits
        digits.startsWith("0") && digits.length == 10 -> "254${digits.drop(1)}"
        else -> digits
    }
}

private fun isValidBuyerPhone(normalized: String) = Regex("^254[17]\\d{8}$").matches(normalized)

/**
 * Full product detail sheet: real listing data, real pickup terms, real Safe Trade CTA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    item: MarketItem,
    isPaymentProviderReady: Boolean,
    paymentProviderName: String,
    onDismiss: () -> Unit,
    onStartBuy: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HomescreenOrange,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .drawerDotGrid()
                .padding(horizontal = 18.dp)
                .padding(bottom = 30.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DText(text = "ITEM DETAILS", size = 24f, color = HomescreenInk, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = HomescreenInk, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                if (item.imageUrl != null) {
                    AsyncImage(item.imageUrl, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                        Text("NO PHOTO", fontFamily = SquadaOne, color = HomescreenInk.copy(alpha = 0.5f), fontSize = 15.sp)
                    }
                }
                StatusChip(
                    label = item.status.replace('_', ' '),
                    bg = when (item.status) {
                        "ACTIVE" -> BuySuccessGreen
                        "RESERVED" -> HomescreenInk
                        else -> HomescreenInk
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(item.title, color = HomescreenInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "KES ${item.price.toLong()}",
                fontFamily = SquadaOne,
                color = HomescreenInk,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailChip(item.category)
                DetailChip(item.condition)
                DetailChip(item.pickupTiming)
            }
            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                SectionHeader("ABOUT")
                Text(item.description, color = HomescreenInk.copy(alpha = 0.85f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            SectionHeader("SAFE PICKUP")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = BuySuccessGreen, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        item.pickupSpotName.ifBlank { "Seller chosen point" },
                        fontFamily = SquadaOne, color = HomescreenInk, fontSize = 15.sp
                    )
                    Text(
                        listOf(
                            item.pickupSecurityLevel.ifBlank { "SELLER CHOSEN POINT" },
                            item.pickupTiming
                        ).filter { it.isNotBlank() }.joinToString("  •  "),
                        color = HomescreenInk.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionHeader("PAYMENT")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = HomescreenInk, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "$paymentProviderName — funds stay locked until you scan the seller's release code",
                    color = HomescreenInk.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStartBuy,
                enabled = item.status == "ACTIVE" && isPaymentProviderReady,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = HomescreenInk,
                    disabledContainerColor = Color.White.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    when {
                        item.status != "ACTIVE" -> item.status.replace('_', ' ') + " — NOT AVAILABLE"
                        !isPaymentProviderReady -> "SAFE TRADE NOT DEPLOYED ON THIS BUILD"
                        else -> "BUY WITH SAFE TRADE"
                    },
                    fontFamily = SquadaOne,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Buyer Safe Trade flow, stage-driven:
 * CONFIRM (quantity/note/phone → STK push) → AWAITING (M-Pesa confirmation) →
 * RELEASE (scan seller QR / manual code) → DONE. Refund escape hatch while FUNDS_HELD.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeTradeBuyerSheet(
    item: MarketItem,
    profile: StudentProfile?,
    viewModel: HubViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var stage by remember { mutableStateOf(BuyerStage.CONFIRM) }
    var quantity by remember { mutableStateOf(1) }
    var buyerNote by remember { mutableStateOf("") }
    var payoutPhone by remember { mutableStateOf(profile?.phoneNumber.orEmpty()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }

    val basket = remember(item) {
        ProductBasketConfig(
            item = item,
            handoverOptionTitle = item.pickupSpotName.ifBlank { "Safe pickup" },
            handoverFee = 0.0,
            packageOptionTitle = "",
            packageExtraPrice = 0.0,
            quantity = quantity,
            buyerNote = buyerNote
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HomescreenOrange,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .drawerDotGrid()
                .padding(horizontal = 18.dp)
                .padding(bottom = 30.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    DText(text = "SAFE TRADE", size = 24f, color = HomescreenInk)
                    Text(
                        when (stage) {
                            BuyerStage.CONFIRM -> "Lock ${item.currency} ${item.price.toLong()} in escrow"
                            BuyerStage.AWAITING -> "Waiting for M-Pesa confirmation"
                            BuyerStage.RELEASE -> "Meet, then scan the seller's code"
                            BuyerStage.DONE -> "Handover complete"
                        },
                        color = HomescreenInk.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = HomescreenInk, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(14.dp))

            when (stage) {
                BuyerStage.CONFIRM -> {
                    WhiteBuyCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("How many?", fontFamily = SquadaOne, color = HomescreenInk, fontSize = 14.sp)
                                Text("KES ${item.price.toLong()} each", color = HomescreenInk.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier.size(34.dp).clip(CircleShape).background(HomescreenInk)
                            ) { Text("−", color = Color.White, fontWeight = FontWeight.Bold) }
                            Text(
                                quantity.toString(),
                                fontFamily = SquadaOne, color = HomescreenInk, fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { if (quantity < 20) quantity++ },
                                modifier = Modifier.size(34.dp).clip(CircleShape).background(HomescreenInk)
                            ) { Text("+", color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.height(10.dp))
                        BuyTextField(value = buyerNote, onValueChange = { buyerNote = it }, label = "NOTE FOR SELLER (OPTIONAL)")
                        Spacer(Modifier.height(10.dp))
                        BuyTextField(
                            value = payoutPhone,
                            onValueChange = { payoutPhone = it },
                            label = "YOUR M-PESA NUMBER",
                            supportingText = "You will get an STK prompt on this number."
                        )
                    }
                    errorMessage?.let { BuyError(it) }
                    Spacer(Modifier.height(12.dp))
                    BuyPrimaryButton(
                        label = "PAY KES ${(item.price * quantity).toLong()} INTO ESCROW",
                        enabled = !isWorking
                    ) {
                        val normalized = normalizeBuyerPhone(payoutPhone)
                        if (!isValidBuyerPhone(normalized)) {
                            errorMessage = "Enter a valid M-Pesa number (07XX… or 01XX…)"
                        } else {
                            errorMessage = null
                            isWorking = true
                            scope.launch {
                                val result = viewModel.lockEscrowFunds(
                                    basket = basket.copy(quantity = quantity, buyerNote = buyerNote),
                                    paymentMethodId = "MPESA_STK",
                                    buyerPhoneE164 = normalized
                                )
                                isWorking = false
                                when (result) {
                                    is com.campus.network.core.model.PaymentResult.Failure ->
                                        errorMessage = result.message
                                    is com.campus.network.core.model.PaymentResult.Success -> {
                                        if (result.value.escrowStatus == "FUNDS_HELD") {
                                            stage = BuyerStage.RELEASE
                                        } else {
                                            stage = BuyerStage.AWAITING
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                BuyerStage.AWAITING -> {
                    WhiteBuyCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = HomescreenInk, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Enter your M-Pesa PIN on your phone. We lock the money the moment you confirm.",
                                color = HomescreenInk, fontSize = 12.sp
                            )
                        }
                    }
                    errorMessage?.let { BuyError(it) }
                    Spacer(Modifier.height(12.dp))
                    BuyPrimaryButton(label = "I HAVE PAID — CHECK STATUS", enabled = !isWorking) {
                        isWorking = true
                        scope.launch {
                            val order = viewModel.refreshEscrowOrder(item.id)
                            isWorking = false
                            when (order?.status?.name) {
                                "FUNDS_HELD" -> { stage = BuyerStage.RELEASE; errorMessage = null }
                                "RELEASED_TO_SELLER" -> stage = BuyerStage.DONE
                                else -> errorMessage = "No M-Pesa confirmation yet. Complete the prompt and try again."
                            }
                        }
                    }
                }

                BuyerStage.RELEASE -> ReleaseStageContent(
                    item = item,
                    viewModel = viewModel,
                    errorMessage = errorMessage,
                    statusMessage = statusMessage,
                    onStatus = { statusMessage = it },
                    onReleased = { stage = BuyerStage.DONE },
                    onError = { errorMessage = it }
                )

                BuyerStage.DONE -> {
                    WhiteBuyCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = BuySuccessGreen, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("RELEASED TO SELLER", fontFamily = SquadaOne, color = HomescreenInk, fontSize = 17.sp)
                            Text(
                                "Funds moved to the seller's M-Pesa. Mark this listing done on your end.",
                                color = HomescreenInk.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    BuyPrimaryButton(label = "CLOSE", enabled = true) { onDismiss() }
                }
            }
        }
    }
}

@Composable
private fun ReleaseStageContent(
    item: MarketItem,
    viewModel: HubViewModel,
    errorMessage: String?,
    statusMessage: String?,
    onStatus: (String?) -> Unit,
    onReleased: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var scannerTab by remember { mutableStateOf(0) }
    var manualCode by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) scannerTab = 1
    }

    fun release(code: String) {
        val clean = code.trim().uppercase()
            .removePrefix("SAFE_TRADE_RELEASE:")
        if (clean.isBlank()) {
            onError("Enter or scan the seller's release code")
            return
        }
        if (isVerifying) return
        isVerifying = true
        onStatus(null)
        scope.launch {
            val result = viewModel.releaseEscrowFunds(item.id, clean)
            isVerifying = false
            when (result) {
                is com.campus.network.core.model.PaymentResult.Failure ->
                    onError(result.message)
                is com.campus.network.core.model.PaymentResult.Success -> onReleased()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HomescreenInk)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("SCAN SELLER QR", "ENTER CODE").forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (scannerTab == index) HomescreenOrange else Color.Transparent)
                    .clickable { scannerTab = index }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontFamily = SquadaOne, color = if (scannerTab == index) HomescreenInk else Color.White, fontSize = 13.sp)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    if (scannerTab == 0) {
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val executor = ContextCompat.getMainExecutor(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            val analysisExecutor = Executors.newSingleThreadExecutor()
                            val multiFormatReader = MultiFormatReader()
                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy: ImageProxy ->
                                val buffer = imageProxy.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                val width = imageProxy.width
                                val height = imageProxy.height
                                try {
                                    val source = PlanarYUVLuminanceSource(
                                        bytes, width, height, 0, 0, width, height, false
                                    )
                                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                                    val qrText = multiFormatReader.decodeWithState(binaryBitmap).text
                                    if (!qrText.isNullOrBlank()) {
                                        ContextCompat.getMainExecutor(ctx).execute { release(qrText) }
                                    }
                                } catch (_: Exception) {
                                    // Frame did not contain a readable QR code
                                } finally {
                                    multiFormatReader.reset()
                                    imageProxy.close()
                                }
                            }
                            try {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                                activeCamera = cam
                            } catch (_: Exception) {
                                onError("Unable to start camera scanner")
                            }
                        }, executor)
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .border(2.5.dp, BuySuccessGreen, RoundedCornerShape(16.dp))
                )
                IconButton(
                    onClick = {
                        activeCamera?.let { cam ->
                            if (cam.cameraInfo.hasFlashUnit()) {
                                val next = !isTorchEnabled
                                cam.cameraControl.enableTorch(next)
                                isTorchEnabled = next
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isTorchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    "Point at the seller's Safe Trade QR",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else {
            WhiteBuyCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = HomescreenInk, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Camera permission needed to scan", color = HomescreenInk, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    BuySecondaryButton("ALLOW CAMERA") { permissionLauncher.launch(Manifest.permission.CAMERA) }
                }
            }
        }
    } else {
        WhiteBuyCard {
            BuyTextField(
                value = manualCode,
                onValueChange = { manualCode = it.uppercase() },
                label = "RELEASE CODE",
                supportingText = "Shown under the seller's QR — e.g. CP-123456"
            )
        }
        Spacer(Modifier.height(10.dp))
    }
    statusMessage?.let { BuyNote(it) }
    errorMessage?.takeIf { it.isNotBlank() }?.let { BuyError(it) }
    Spacer(Modifier.height(10.dp))
    BuyPrimaryButton(
        label = if (isVerifying) "RELEASING…" else "RELEASE KES ${item.price.toLong()} TO SELLER",
        enabled = !isVerifying
    ) {
        release(manualCode)
    }
    BuySecondaryButton("CANCEL ORDER & REFUND ME") {
        isVerifying = true
        scope.launch {
            val order = viewModel.refreshEscrowOrder(item.id)
            if (order == null) {
                isVerifying = false
                onError("No funded order found to cancel")
                return@launch
            }
            val result = viewModel.refundEscrowOrder(order)
            isVerifying = false
            when (result) {
                is com.campus.network.core.model.PaymentResult.Failure -> onError(result.message)
                is com.campus.network.core.model.PaymentResult.Success -> onReleased()
            }
        }
    }
}

@Composable
private fun WhiteBuyCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun BuyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String? = null
) {
    Column {
        Text(label, fontFamily = SquadaOne, color = HomescreenInk, fontSize = 12.sp, letterSpacing = 1.sp)
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HomescreenInk,
                unfocusedBorderColor = HomescreenInk.copy(alpha = 0.25f),
                cursorColor = HomescreenInk,
                focusedTextColor = HomescreenInk,
                unfocusedTextColor = HomescreenInk
            ),
            modifier = Modifier.fillMaxWidth()
        )
        supportingText?.let {
            Text(it, color = HomescreenInk.copy(alpha = 0.55f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun BuyPrimaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = HomescreenInk,
            disabledContainerColor = Color.White.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(label, fontFamily = SquadaOne, fontSize = 14.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun BuySecondaryButton(label: String, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = HomescreenInk),
        modifier = Modifier.fillMaxWidth().height(44.dp)
    ) {
        Text(label, fontFamily = SquadaOne, fontSize = 12.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun BuyNote(text: String) {
    Text(
        text,
        color = HomescreenInk.copy(alpha = 0.8f),
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
    )
}

@Composable
private fun BuyError(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BuyErrorRed)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun DetailChip(label: String) {
    if (label.isBlank()) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, fontFamily = SquadaOne, color = HomescreenInk, fontSize = 11.sp)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontFamily = SquadaOne,
        color = HomescreenInk,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun StatusChip(label: String, bg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, fontFamily = SquadaOne, color = Color.White, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

private enum class BuyerStage { CONFIRM, AWAITING, RELEASE, DONE }
