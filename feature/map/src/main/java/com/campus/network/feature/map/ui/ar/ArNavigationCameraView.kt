package com.campus.network.feature.map.ui.ar

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.UTurnRight
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.campus.network.feature.map.domain.ActiveNavigationSession
import com.campus.network.feature.map.domain.DeviceOrientationState
import com.campus.network.feature.map.domain.GeoCoordinate
import com.campus.network.feature.map.domain.GeoSpatialMath
import com.campus.network.feature.map.domain.GeospatialAnchorMode
import com.campus.network.feature.map.domain.ManeuverDirection
import com.campus.network.feature.map.domain.TiltSwitchPolicy
import com.campus.network.feature.map.ui.mapbox.MapboxMiniPreviewMap
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.Scene
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val ArElectricBlue = Color(0xFF0A66FF)
private val ArCyanGlow = Color(0xFF00D4FF)

/**
 * Full-screen ARCore + SceneView 3D Navigation Camera overlay paired with a
 * glassmorphic Compose HUD, curved bottom Mapbox mini-map, VPS Geospatial anchoring,
 * and automatic fused GPS + compass heading fallback.
 */
@Composable
fun ArNavigationCameraView(
    userLocation: GeoCoordinate?,
    orientationState: DeviceOrientationState,
    activeNavigation: ActiveNavigationSession?,
    geospatialMode: GeospatialAnchorMode,
    tiltSwitchPolicy: TiltSwitchPolicy,
    onGeospatialModeChanged: (GeospatialAnchorMode) -> Unit,
    onSwitchTo2DMap: () -> Unit,
    onToggleTiltPolicy: () -> Unit,
    onStopNavigation: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenDestinationSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val isArSupported = remember(context) {
        runCatching {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            availability == ArCoreApk.Availability.SUPPORTED_INSTALLED
        }.getOrDefault(false)
    }

    LaunchedEffect(hasCameraPermission, isArSupported) {
        when {
            !hasCameraPermission -> onGeospatialModeChanged(GeospatialAnchorMode.AR_UNAVAILABLE)
            isArSupported -> onGeospatialModeChanged(GeospatialAnchorMode.INITIALIZING)
            else -> onGeospatialModeChanged(GeospatialAnchorMode.GPS_COMPASS_FALLBACK)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B16))
    ) {
        if (!hasCameraPermission) {
            ArUnavailableBackdrop(
                hasCameraPermission = false,
                isArSupported = isArSupported,
                onRequestCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                onReturnTo2DMap = onSwitchTo2DMap,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isArSupported) {
            ArScene3dLayer(
                userLocation = userLocation,
                orientationState = orientationState,
                activeNavigation = activeNavigation,
                onGeospatialModeChanged = onGeospatialModeChanged,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            SensorArCamera3dLayer(
                userLocation = userLocation,
                orientationState = orientationState,
                activeNavigation = activeNavigation,
                onGeospatialModeChanged = onGeospatialModeChanged,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Floating AR HUD inspired directly by clean modern AR street guidance
        ArNavigationHudOverlay(
            userLocation = userLocation,
            orientationState = orientationState,
            activeNavigation = activeNavigation,
            geospatialMode = geospatialMode,
            tiltSwitchPolicy = tiltSwitchPolicy,
            onSwitchTo2DMap = onSwitchTo2DMap,
            onToggleTiltPolicy = onToggleTiltPolicy,
            onStopNavigation = onStopNavigation,
            onOpenMessages = onOpenMessages,
            onOpenDestinationSearch = onOpenDestinationSearch,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ArScene3dLayer(
    userLocation: GeoCoordinate?,
    orientationState: DeviceOrientationState,
    activeNavigation: ActiveNavigationSession?,
    onGeospatialModeChanged: (GeospatialAnchorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val childNodes = rememberNodes()

    val latestNav by rememberUpdatedState(activeNavigation)
    val latestUserLoc by rememberUpdatedState(userLocation)
    val latestOrientation by rememberUpdatedState(orientationState)
    val latestOnModeChange by rememberUpdatedState(onGeospatialModeChanged)

    var lastSyncElapsedMs by remember { mutableLongStateOf(0L) }
    var lastWaypointSignature by remember { mutableStateOf("") }
    var lastAnchorWasVps by remember { mutableStateOf(false) }

    val managedNodes = remember { mutableListOf<Node>() }

    fun clearManagedNodes() {
        managedNodes.forEach { node ->
            runCatching { childNodes.remove(node) }
            runCatching { node.destroy() }
        }
        managedNodes.clear()
        lastWaypointSignature = ""
    }

    DisposableEffect(Unit) {
        onDispose {
            clearManagedNodes()
        }
    }

    LaunchedEffect(activeNavigation?.destination) {
        if (activeNavigation == null) {
            clearManagedNodes()
        } else {
            lastWaypointSignature = ""
        }
    }

    ARScene(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        childNodes = childNodes,
        sessionConfiguration = { session, config ->
            if (session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
                config.geospatialMode = Config.GeospatialMode.ENABLED
            }
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            config.focusMode = Config.FocusMode.AUTO
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }
        },
        onSessionUpdated = { session, frame ->
            val nowMs = SystemClock.elapsedRealtime()
            if (nowMs - lastSyncElapsedMs < 350L) return@ARScene
            lastSyncElapsedMs = nowMs

            val nav = latestNav
            if (nav == null) {
                if (managedNodes.isNotEmpty()) clearManagedNodes()
                return@ARScene
            }

            if (frame.camera.trackingState != TrackingState.TRACKING) {
                latestOnModeChange(GeospatialAnchorMode.INITIALIZING)
                return@ARScene
            }

            val earth = session.earth
            val geoPose = earth?.cameraGeospatialPose
            val useVps = earth != null &&
                earth.trackingState == TrackingState.TRACKING &&
                geoPose != null &&
                geoPose.horizontalAccuracy in 0.01..15.0

            latestOnModeChange(
                if (useVps) GeospatialAnchorMode.VPS_GEOSPATIAL
                else GeospatialAnchorMode.GPS_COMPASS_FALLBACK
            )

            val waypoints = nav.sampledArWaypoints.take(8)
            val dest = nav.destination
            val signature = "${waypoints.size}:${waypoints.firstOrNull()?.latitude}:${dest.latitude}:${dest.longitude}"

            if (useVps && earth != null && geoPose != null) {
                if (lastAnchorWasVps && signature == lastWaypointSignature && managedNodes.isNotEmpty()) {
                    return@ARScene
                }
                clearManagedNodes()
                lastAnchorWasVps = true
                lastWaypointSignature = signature

                val sidewalkAlt = geoPose.altitude - 1.45
                for (i in waypoints.indices) {
                    val wp = waypoints[i]
                    val nextPt = waypoints.getOrNull(i + 1) ?: dest
                    val bearing = GeoSpatialMath.bearingDegrees(wp, nextPt)
                    runCatching {
                        val anchor = earth.createAnchor(wp.latitude, wp.longitude, sidewalkAlt, 0f, 0f, 0f, 1f)
                        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
                        modelLoader.createModelInstance("models/path_chevron.glb")?.let { inst ->
                            anchorNode.addChildNode(
                                ModelNode(
                                    modelInstance = inst,
                                    autoAnimate = true
                                ).apply {
                                    rotation = Rotation(x = 0f, y = (-bearing).toFloat(), z = 0f)
                                }
                            )
                            childNodes.add(anchorNode)
                            managedNodes.add(anchorNode)
                        }
                    }
                }

                // Arrival Ring + Floating Destination Pin at route endpoint
                runCatching {
                    val destAnchor = earth.createAnchor(dest.latitude, dest.longitude, sidewalkAlt, 0f, 0f, 0f, 1f)
                    val destAnchorNode = AnchorNode(engine = engine, anchor = destAnchor)
                    modelLoader.createModelInstance("models/arrival_ring.glb")?.let { ringInst ->
                        destAnchorNode.addChildNode(ModelNode(modelInstance = ringInst, autoAnimate = true))
                    }
                    modelLoader.createModelInstance("models/destination_pin.glb")?.let { pinInst ->
                        destAnchorNode.addChildNode(ModelNode(modelInstance = pinInst, autoAnimate = true))
                    }
                    childNodes.add(destAnchorNode)
                    managedNodes.add(destAnchorNode)
                }
                return@ARScene
            }

            // Fallback: Fused GPS + Compass AR projection around tracked camera pose & detected ground plane
            val origin = nav.snappedLocation ?: latestUserLoc ?: return@ARScene
            val camPose = frame.camera.displayOrientedPose
            val camPos = camPose.translation
            val zAxis = camPose.zAxis
            val fwdX = -zAxis[0]
            val fwdZ = -zAxis[2]
            val fLen = sqrt((fwdX * fwdX + fwdZ * fwdZ).toDouble()).coerceAtLeast(1e-4)
            val camClockwiseDeg = Math.toDegrees(atan2(fwdX / fLen, -fwdZ / fLen))
            val azimuthDeg = latestOrientation.azimuthDegrees.toDouble()

            val detectedGroundY = session.getAllTrackables(Plane::class.java)
                .filter { it.trackingState == TrackingState.TRACKING && it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                .minOfOrNull { it.centerPose.ty() }
                ?: (camPos[1] - 1.38f)

            clearManagedNodes()
            lastAnchorWasVps = false
            lastWaypointSignature = signature

            for (i in waypoints.indices) {
                val wp = waypoints[i]
                val nextPt = waypoints.getOrNull(i + 1) ?: dest
                val distFromUser = GeoSpatialMath.haversineMeters(origin, wp)
                if (distFromUser > 35.0) continue

                val (enuX, _, enuZ) = GeoSpatialMath.geoToLocalEnuMeters(origin, wp)
                val (localX, localZ) = GeoSpatialMath.enuToArLocalWithCompass(
                    eastMeters = enuX,
                    northMeters = -enuZ,
                    cameraForwardX = fwdX,
                    cameraForwardZ = fwdZ,
                    cameraAzimuthDeg = azimuthDeg.toFloat()
                )
                val bearing = GeoSpatialMath.bearingDegrees(wp, nextPt)
                val yawDeg = -(camClockwiseDeg - azimuthDeg + bearing).toFloat()

                modelLoader.createModelInstance("models/path_chevron.glb")?.let { inst ->
                    val node = ModelNode(modelInstance = inst, autoAnimate = true).apply {
                        position = Position(x = camPos[0] + localX, y = detectedGroundY, z = camPos[2] + localZ)
                        rotation = Rotation(x = 0f, y = yawDeg, z = 0f)
                    }
                    childNodes.add(node)
                    managedNodes.add(node)
                }
            }

            // Place Destination Pin & Arrival Ring (clamped to <= 42m for crisp outdoor visibility)
            val (dEnuX, _, dEnuZ) = GeoSpatialMath.geoToLocalEnuMeters(origin, dest)
            val rawDist = sqrt((dEnuX * dEnuX + dEnuZ * dEnuZ).toDouble()).coerceAtLeast(0.5)
            val visualScale = if (rawDist > 42.0) (42.0 / rawDist).toFloat() else 1.0f
            val (dLocalX, dLocalZ) = GeoSpatialMath.enuToArLocalWithCompass(
                eastMeters = dEnuX * visualScale,
                northMeters = -dEnuZ * visualScale,
                cameraForwardX = fwdX,
                cameraForwardZ = fwdZ,
                cameraAzimuthDeg = azimuthDeg.toFloat()
            )

            modelLoader.createModelInstance("models/arrival_ring.glb")?.let { ringInst ->
                val ringNode = ModelNode(modelInstance = ringInst, autoAnimate = true).apply {
                    position = Position(x = camPos[0] + dLocalX, y = detectedGroundY, z = camPos[2] + dLocalZ)
                }
                childNodes.add(ringNode)
                managedNodes.add(ringNode)
            }
            modelLoader.createModelInstance("models/destination_pin.glb")?.let { pinInst ->
                val pinNode = ModelNode(modelInstance = pinInst, autoAnimate = true).apply {
                    position = Position(x = camPos[0] + dLocalX, y = detectedGroundY, z = camPos[2] + dLocalZ)
                }
                childNodes.add(pinNode)
                managedNodes.add(pinNode)
            }
        }
    )
}

/**
 * Non-ARCore live rear camera + transparent Filament 3D Scene fallback for devices without
 * Google Play Services for AR (e.g., Android Go / TECNO BF6).
 * Projects animated 3D `.glb` chevrons, destination pin, and arrival ring over the live camera
 * feed using accelerometer/magnetometer orientation + GPS route waypoints.
 */
@Composable
private fun SensorArCamera3dLayer(
    userLocation: GeoCoordinate?,
    orientationState: DeviceOrientationState,
    activeNavigation: ActiveNavigationSession?,
    onGeospatialModeChanged: (GeospatialAnchorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val childNodes = rememberNodes()

    val latestNav by rememberUpdatedState(activeNavigation)
    val latestUserLoc by rememberUpdatedState(userLocation)
    val latestOrientation by rememberUpdatedState(orientationState)

    var manualYawOffsetDeg by remember { mutableFloatStateOf(0f) }
    var lastSyncElapsedMs by remember { mutableLongStateOf(0L) }
    var lastRebuildSignature by remember { mutableStateOf("") }

    val worldGroupNode = remember(engine) { Node(engine = engine) }
    val managedNodes = remember { mutableListOf<Node>() }

    fun clearManagedNodes() {
        managedNodes.forEach { node ->
            runCatching { worldGroupNode.removeChildNode(node) }
            runCatching { node.destroy() }
        }
        managedNodes.clear()
        lastRebuildSignature = ""
    }

    LaunchedEffect(Unit) {
        onGeospatialModeChanged(GeospatialAnchorMode.GPS_COMPASS_FALLBACK)
        if (!childNodes.contains(worldGroupNode)) {
            childNodes.add(worldGroupNode)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            clearManagedNodes()
            runCatching { childNodes.remove(worldGroupNode) }
            runCatching { worldGroupNode.destroy() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    manualYawOffsetDeg = ((manualYawOffsetDeg - dragAmount * 0.25f) % 360f + 360f) % 360f
                }
            }
    ) {
        RearCameraPreviewLayer(modifier = Modifier.fillMaxSize())

        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            isOpaque = false,
            cameraManipulator = null,
            childNodes = childNodes,
            onFrame = {
                val orient = latestOrientation
                val nav = latestNav
                // Tilt the 3D ground plane to match phone pitch (78 deg upright = horizon level)
                val pitchTiltDeg = (orient.pitchDegrees.coerceIn(25f, 95f) - 78f) * 0.55f
                val rollTiltDeg = (-orient.rollDegrees).coerceIn(-25f, 25f) * 0.35f
                worldGroupNode.rotation = Rotation(x = pitchTiltDeg, y = 0f, z = rollTiltDeg)

                val nowMs = SystemClock.elapsedRealtime()
                if (nowMs - lastSyncElapsedMs < 250L) return@Scene
                lastSyncElapsedMs = nowMs

                val origin = nav?.snappedLocation ?: latestUserLoc
                val routeFirstTarget = nav?.sampledArWaypoints?.getOrNull(1) ?: nav?.destination
                val defaultForwardBearing = if (origin != null && routeFirstTarget != null) {
                    GeoSpatialMath.bearingDegrees(origin, routeFirstTarget).toFloat()
                } else {
                    0f
                }
                val baseHeading = if (orient.azimuthDegrees != 0f) {
                    orient.azimuthDegrees
                } else {
                    defaultForwardBearing
                }
                val effectiveHeadingDeg = ((baseHeading + manualYawOffsetDeg) % 360f + 360f) % 360f

                val groundY = -1.35f

                if (nav == null) {
                    val freeSig = "freeroam:${manualYawOffsetDeg.toInt() / 3}"
                    if (freeSig == lastRebuildSignature && managedNodes.isNotEmpty()) return@Scene
                    clearManagedNodes()
                    lastRebuildSignature = freeSig

                    // Free-roam 3D walkway chevrons ahead of user so AR View is always visually alive
                    val previewDistances = listOf(3.2f, 6.0f, 8.8f)
                    for (dist in previewDistances) {
                        val inst = modelLoader.createModelInstance("models/path_chevron.glb")
                        val node = ModelNode(modelInstance = inst, autoAnimate = true).apply {
                            position = Position(x = 0f, y = groundY, z = -dist)
                            rotation = Rotation(x = 0f, y = -manualYawOffsetDeg, z = 0f)
                        }
                        worldGroupNode.addChildNode(node)
                        managedNodes.add(node)
                    }
                    return@Scene
                }

                val waypoints = nav.sampledArWaypoints.take(8)
                val dest = nav.destination
                val effOrigin = origin ?: waypoints.firstOrNull() ?: dest
                val sig = "${waypoints.size}:${effOrigin.latitude}:${dest.latitude}:${dest.longitude}:${effectiveHeadingDeg.toInt() / 2}"
                if (sig == lastRebuildSignature && managedNodes.isNotEmpty()) return@Scene

                clearManagedNodes()
                lastRebuildSignature = sig

                for (i in waypoints.indices) {
                    val wp = waypoints[i]
                    val nextPt = waypoints.getOrNull(i + 1) ?: dest
                    val distFromUser = GeoSpatialMath.haversineMeters(effOrigin, wp)
                    if (distFromUser > 35.0) continue

                    val (enuX, _, enuZ) = GeoSpatialMath.geoToLocalEnuMeters(effOrigin, wp)
                    val (localX, localZ) = GeoSpatialMath.enuToArLocalWithCompass(
                        eastMeters = enuX,
                        northMeters = -enuZ,
                        cameraForwardX = 0f,
                        cameraForwardZ = -1f,
                        cameraAzimuthDeg = effectiveHeadingDeg
                    )
                    val resolvedZ = if (distFromUser < 0.8) -2.6f else localZ
                    val bearing = GeoSpatialMath.bearingDegrees(wp, nextPt).toFloat()
                    val yawDeg = -(bearing - effectiveHeadingDeg)

                    val inst = modelLoader.createModelInstance("models/path_chevron.glb")
                    val node = ModelNode(modelInstance = inst, autoAnimate = true).apply {
                        position = Position(x = localX, y = groundY, z = resolvedZ)
                        rotation = Rotation(x = 0f, y = yawDeg, z = 0f)
                    }
                    worldGroupNode.addChildNode(node)
                    managedNodes.add(node)
                }

                val (dEnuX, _, dEnuZ) = GeoSpatialMath.geoToLocalEnuMeters(effOrigin, dest)
                val rawDist = sqrt((dEnuX * dEnuX + dEnuZ * dEnuZ).toDouble()).coerceAtLeast(0.5)
                val clampedDist = rawDist.coerceIn(4.0, 28.0)
                val scale = (clampedDist / rawDist).toFloat()
                val (dLocalX, dLocalZ) = GeoSpatialMath.enuToArLocalWithCompass(
                    eastMeters = dEnuX * scale,
                    northMeters = -dEnuZ * scale,
                    cameraForwardX = 0f,
                    cameraForwardZ = -1f,
                    cameraAzimuthDeg = effectiveHeadingDeg
                )

                val ringInst = modelLoader.createModelInstance("models/arrival_ring.glb")
                val ringNode = ModelNode(modelInstance = ringInst, autoAnimate = true).apply {
                    position = Position(x = dLocalX, y = groundY, z = dLocalZ)
                }
                worldGroupNode.addChildNode(ringNode)
                managedNodes.add(ringNode)

                val pinInst = modelLoader.createModelInstance("models/destination_pin.glb")
                val pinNode = ModelNode(modelInstance = pinInst, autoAnimate = true).apply {
                    position = Position(x = dLocalX, y = groundY, z = dLocalZ)
                }
                worldGroupNode.addChildNode(pinNode)
                managedNodes.add(pinNode)
            }
        )
    }
}

@Composable
private fun RearCameraPreviewLayer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            runCatching { boundProvider?.unbindAll() }
            boundProvider = null
        }
    }

    LaunchedEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val provider = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
            future.addListener(
                {
                    if (cont.isActive) {
                        runCatching { future.get() }
                            .onSuccess { cont.resume(it) }
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }
        boundProvider = provider
        val selector = when {
            provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
            provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> return@LaunchedEffect
        }
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview)
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun ArNavigationHudOverlay(
    userLocation: GeoCoordinate?,
    orientationState: DeviceOrientationState,
    activeNavigation: ActiveNavigationSession?,
    geospatialMode: GeospatialAnchorMode,
    tiltSwitchPolicy: TiltSwitchPolicy,
    onSwitchTo2DMap: () -> Unit,
    onToggleTiltPolicy: () -> Unit,
    onStopNavigation: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenDestinationSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasArrived = activeNavigation != null &&
        (activeNavigation.hasArrived || activeNavigation.distanceRemainingMeters <= 5.5)

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Full-screen Emerald Green Wash + Radial White Celebration Streaks on Arrival
        AnimatedVisibility(
            visible = hasArrived,
            enter = fadeIn(tween(350)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            HyperArrivalGreenCelebrationOverlay(
                destinationName = activeNavigation?.destinationName.orEmpty(),
                onCardClick = onStopNavigation
            )
        }

        // 2. Top Smooth Dark Vignette Gradient (when navigating before arrival)
        if (!hasArrived) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xD9050811),
                                Color(0x8C050811),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // 3. Top-Left Borderless Hyper AR Typography Header ("Navigating to <dest>" + Two-Tone Maneuver)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            if (activeNavigation != null) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color.White.copy(alpha = 0.72f),
                                fontWeight = FontWeight.Normal
                            )
                        ) {
                            append("Navigating to ")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(activeNavigation.destinationName)
                        }
                    },
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                val parsed = remember(
                    activeNavigation.currentStep,
                    activeNavigation.distanceRemainingMeters,
                    hasArrived
                ) {
                    formatHyperArManeuver(activeNavigation, hasArrived)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (parsed.isArrivalOrFinalApproach) {
                        CheckeredFlagVectorIcon(
                            tint = if (parsed.isImmediateNow) Color.White else Color(0xFFB4BCD0),
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        Icon(
                            imageVector = iconForManeuver(parsed.direction),
                            contentDescription = parsed.direction.name,
                            tint = if (parsed.isImmediateNow) Color.White else Color(0xFFB4BCD0),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(parsed.boldPrefix)
                            }
                            if (parsed.secondaryAction.isNotEmpty()) {
                                withStyle(
                                    SpanStyle(
                                        color = if (parsed.isImmediateNow) Color.White else Color(0xFF949EB2),
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(parsed.secondaryAction)
                                }
                            }
                        },
                        fontSize = 27.sp,
                        lineHeight = 32.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                // Idle AR state when opened before picking a destination: tap header to search
                Column(
                    modifier = Modifier.clickable(onClick = onOpenDestinationSearch)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.72f))) {
                                append("Navigating to ")
                            }
                            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                append("Select destination")
                            }
                        },
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tap to search place",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Bottom Floating Corner Rounded-Square Icon Buttons (100% matching Hyper AR video)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // Bottom-Left Stacked Pair: List/Search button + Viewfinder/AR Toggle button
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HyperSquareIconButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Search or change destination",
                    onClick = onOpenDestinationSearch
                )
                HyperSquareIconButton(
                    icon = Icons.Default.CropFree,
                    contentDescription = "Toggle AR tilt lock",
                    onClick = onToggleTiltPolicy
                )
            }

            // Bottom-Right Single Button: Switch to / recenter 2D Map
            HyperSquareIconButton(
                icon = Icons.Default.NearMe,
                contentDescription = "Switch to 2D Map",
                onClick = onSwitchTo2DMap,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

internal data class HyperManeuverDisplay(
    val boldPrefix: String,
    val secondaryAction: String,
    val direction: ManeuverDirection,
    val isImmediateNow: Boolean,
    val isArrivalOrFinalApproach: Boolean
)

internal fun formatHyperArManeuver(
    session: ActiveNavigationSession,
    hasArrived: Boolean
): HyperManeuverDisplay {
    if (hasArrived || session.hasArrived || session.distanceRemainingMeters <= 5.5) {
        return HyperManeuverDisplay(
            boldPrefix = "Now you've arrived",
            secondaryAction = "",
            direction = ManeuverDirection.ARRIVE,
            isImmediateNow = true,
            isArrivalOrFinalApproach = true
        )
    }

    val step = session.currentStep
    val direction = step?.direction ?: ManeuverDirection.STRAIGHT
    val totalDist = session.distanceRemainingMeters.coerceAtLeast(1.0)
    val stepDist = (step?.distanceMeters ?: totalDist).coerceAtLeast(1.0)

    // When within 22m of final destination or on the arrival step, show checkered flag + "In Xm you've arrived"
    if (direction == ManeuverDirection.ARRIVE || totalDist <= 22.0) {
        val meters = totalDist.toInt().coerceAtLeast(1)
        return HyperManeuverDisplay(
            boldPrefix = "In ${meters}m",
            secondaryAction = " you've\narrived",
            direction = ManeuverDirection.ARRIVE,
            isImmediateNow = false,
            isArrivalOrFinalApproach = true
        )
    }

    val actionPhrase = when (direction) {
        ManeuverDirection.LEFT -> "turn left"
        ManeuverDirection.SLIGHT_LEFT -> "turn slightly left"
        ManeuverDirection.SHARP_LEFT -> "turn sharp left"
        ManeuverDirection.RIGHT -> "turn right"
        ManeuverDirection.SLIGHT_RIGHT -> "turn slightly right"
        ManeuverDirection.SHARP_RIGHT -> "turn sharp right"
        ManeuverDirection.UTURN -> "turn around right"
        ManeuverDirection.ARRIVE -> "you've arrived"
        ManeuverDirection.STRAIGHT -> {
            val raw = step?.instruction?.trim().orEmpty()
            when {
                raw.contains("left", ignoreCase = true) -> "turn left"
                raw.contains("right", ignoreCase = true) -> "turn right"
                raw.contains("around", ignoreCase = true) || raw.contains("u-turn", ignoreCase = true) -> "turn around right"
                else -> "continue straight"
            }
        }
    }

    return if (stepDist <= 4.5) {
        HyperManeuverDisplay(
            boldPrefix = "Now $actionPhrase",
            secondaryAction = "",
            direction = direction,
            isImmediateNow = true,
            isArrivalOrFinalApproach = false
        )
    } else {
        val distLabel = GeoSpatialMath.formatDistanceShort(stepDist).replace(" ", "")
        HyperManeuverDisplay(
            boldPrefix = "In $distLabel",
            secondaryAction = " $actionPhrase",
            direction = direction,
            isImmediateNow = false,
            isArrivalOrFinalApproach = false
        )
    }
}

@Composable
internal fun HyperSquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = Color(0xE6E8EEF8),
        shadowElevation = 4.dp,
        modifier = modifier
            .size(46.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFF1D63DC),
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
internal fun CheckeredFlagVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)

        // Flag pole on the left
        drawLine(
            color = tint,
            start = Offset(w * 0.16f, h * 0.14f),
            end = Offset(w * 0.16f, h * 0.88f),
            strokeWidth = w * 0.085f,
            cap = StrokeCap.Round
        )

        // Outer flag border
        val flagLeft = w * 0.18f
        val flagTop = h * 0.16f
        val flagWidth = w * 0.66f
        val flagHeight = h * 0.48f
        drawRect(
            color = tint,
            topLeft = Offset(flagLeft, flagTop),
            size = Size(flagWidth, flagHeight),
            style = stroke
        )

        // 4x2 checkered squares inside the flag
        val cols = 4
        val rows = 2
        val cellW = flagWidth / cols
        val cellH = flagHeight / rows
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if ((r + c) % 2 == 0) {
                    drawRect(
                        color = tint,
                        topLeft = Offset(flagLeft + c * cellW, flagTop + r * cellH),
                        size = Size(cellW, cellH)
                    )
                }
            }
        }
    }
}

@Composable
private fun HyperArrivalGreenCelebrationOverlay(
    destinationName: String,
    onCardClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArrivalRays")
    val rayProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RayBurstProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xCC0D8A47),
                        Color(0xB8189E54),
                        Color(0xCC0F7F42)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Radial white speed/celebration streaks bursting around the center card
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.52f
            val anglesDeg = listOf(22f, 52f, 128f, 155f, 205f, 238f, 305f, 338f)
            anglesDeg.forEachIndexed { idx, deg ->
                val phase = (rayProgress + idx * 0.125f) % 1f
                val rad = Math.toRadians(deg.toDouble())
                val innerR = size.minDimension * (0.28f + phase * 0.18f)
                val outerR = innerR + size.minDimension * 0.22f
                val alpha = (1f - phase * 0.7f).coerceIn(0.2f, 0.95f)
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(
                        x = cx + (cos(rad) * innerR).toFloat(),
                        y = cy + (sin(rad) * innerR).toFloat()
                    ),
                    end = Offset(
                        x = cx + (cos(rad) * outerR).toFloat(),
                        y = cy + (sin(rad) * outerR).toFloat()
                    ),
                    strokeWidth = 7f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Rounded White Destination Card (100% matching uploaded_media_3 / frame_17)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFFEFF4FF),
            shadowElevation = 14.dp,
            modifier = Modifier
                .size(210.dp)
                .clickable(onClick = onCardClick)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IsometricDestinationBoxIllustration(
                    modifier = Modifier.size(120.dp)
                )
                if (destinationName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = destinationName,
                        color = Color(0xFF1E293B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun IsometricDestinationBoxIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Isometric open parcel/destination box matching the Hyper AR card illustration
        val leftFace = Path().apply {
            moveTo(w * 0.22f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.60f)
            lineTo(w * 0.50f, h * 0.88f)
            lineTo(w * 0.22f, h * 0.72f)
            close()
        }
        drawPath(leftFace, color = Color(0xFFC69C72))

        val rightFace = Path().apply {
            moveTo(w * 0.50f, h * 0.60f)
            lineTo(w * 0.78f, h * 0.46f)
            lineTo(w * 0.78f, h * 0.72f)
            lineTo(w * 0.50f, h * 0.88f)
            close()
        }
        drawPath(rightFace, color = Color(0xFF8D623F))

        val insideBack = Path().apply {
            moveTo(w * 0.22f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.34f)
            lineTo(w * 0.78f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.60f)
            close()
        }
        drawPath(insideBack, color = Color(0xFF5D3A1E))

        // Left & right open flaps
        val topLeftFlap = Path().apply {
            moveTo(w * 0.22f, h * 0.46f)
            lineTo(w * 0.14f, h * 0.32f)
            lineTo(w * 0.42f, h * 0.20f)
            lineTo(w * 0.50f, h * 0.34f)
            close()
        }
        drawPath(topLeftFlap, color = Color(0xFFD9B38C))

        val topRightFlap = Path().apply {
            moveTo(w * 0.50f, h * 0.34f)
            lineTo(w * 0.58f, h * 0.20f)
            lineTo(w * 0.86f, h * 0.32f)
            lineTo(w * 0.78f, h * 0.46f)
            close()
        }
        drawPath(topRightFlap, color = Color(0xFFC89D74))

        // Small blue brand badge on the left box face
        drawCircle(
            color = Color(0xFF2563EB),
            radius = w * 0.045f,
            center = Offset(w * 0.34f, h * 0.68f)
        )
    }
}

@Composable
private fun ArUnavailableBackdrop(
    hasCameraPermission: Boolean,
    isArSupported: Boolean,
    onRequestCamera: () -> Unit,
    onReturnTo2DMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF0B132B), Color(0xFF1C2541)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xE60F172A),
            border = BorderStroke(1.dp, ArCyanGlow.copy(alpha = 0.5f)),
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = ArCyanGlow,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (!hasCameraPermission) {
                        "Camera Permission Needed for AR"
                    } else if (!isArSupported) {
                        "ARCore Unavailable on This Device"
                    } else {
                        "Preparing AR Camera"
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (!hasCameraPermission) {
                        "Allow camera access to project live 3D navigation chevrons onto the walkway."
                    } else {
                        "Continue using the live Mapbox mini-map and turn-by-turn guidance below."
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = if (!hasCameraPermission) onRequestCamera else onReturnTo2DMap,
                    colors = ButtonDefaults.buttonColors(containerColor = ArElectricBlue)
                ) {
                    Text(
                        text = if (!hasCameraPermission) "Enable AR Camera" else "Open 2D Map",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

internal fun iconForManeuver(direction: ManeuverDirection): ImageVector {
    return when (direction) {
        ManeuverDirection.SLIGHT_LEFT -> Icons.Default.TurnSlightLeft
        ManeuverDirection.LEFT -> Icons.Default.TurnLeft
        ManeuverDirection.SHARP_LEFT -> Icons.Default.TurnSharpLeft
        ManeuverDirection.SLIGHT_RIGHT -> Icons.Default.TurnSlightRight
        ManeuverDirection.RIGHT -> Icons.Default.TurnRight
        ManeuverDirection.SHARP_RIGHT -> Icons.Default.TurnSharpRight
        ManeuverDirection.UTURN -> Icons.Default.UTurnRight
        ManeuverDirection.ARRIVE -> Icons.Default.Flag
        ManeuverDirection.STRAIGHT -> Icons.Default.ArrowUpward
    }
}

private fun compassCardinal(azimuthDeg: Float): String {
    val norm = ((azimuthDeg % 360f) + 360f) % 360f
    return when {
        norm < 22.5f || norm >= 337.5f -> "N"
        norm < 67.5f -> "NE"
        norm < 112.5f -> "E"
        norm < 157.5f -> "SE"
        norm < 202.5f -> "S"
        norm < 247.5f -> "SW"
        norm < 292.5f -> "W"
        else -> "NW"
    }
}
