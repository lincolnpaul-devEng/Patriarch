package com.campus.network.feature.hub.golive

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Full-bleed live camera preview. Rebinds only when [useFront] flips.
 * If the requested lens doesn't exist the other one is used; if the device has
 * no usable camera at all, [onUnavailable] is invoked.
 */
@Composable
internal fun GoLiveCameraPreview(
    useFront: Boolean,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(useFront) {
        val future = ProcessCameraProvider.getInstance(context)
        val provider = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
            future.addListener(
                { cont.resume(future.get()) },
                ContextCompat.getMainExecutor(context)
            )
        }
        val preferred = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val other = if (useFront) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        val selector = when {
            provider.hasCamera(preferred) -> preferred
            provider.hasCamera(other) -> other
            else -> {
                onUnavailable()
                return@LaunchedEffect
            }
        }
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview)
        }.onFailure { onUnavailable() }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
