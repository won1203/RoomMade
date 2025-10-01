package com.example.roommade.ui.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.roommade.vm.FloorPlanViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun CaptureScreen(
    onConfirmDraft: () -> Unit,
    onCancel: () -> Unit,
    vm: FloorPlanViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var hasPermission by rememberSaveable { mutableStateOf(false) }
    RequestCameraPermission { granted -> hasPermission = granted }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect

        val provider = context.getCameraProvider()
        val pv = PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        previewView = pv

        val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
        val capture = ImageCapture.Builder()
            .setTargetResolution(Size(1280, 720))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
        imageCapture = capture
    }

    Surface {
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { previewView ?: PreviewView(context) }, modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = onCancel) { Text("뒤로") }
                    Button(onClick = {
                        val capture = imageCapture ?: return@Button
                        takeInMemory(capture, context) { bitmap, error ->
                            if (bitmap != null) {
                                vm.autoDetectFrom(bitmap)
                                scope.launch { snackbarHostState.showSnackbar("사진 분석 완료: 유령 후보 준비됨") }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "촬영 실패: ${error?.localizedMessage ?: "알 수 없음"}"
                                    )
                                }
                            }
                        }
                    }) { Text("촬영") }
                    Button(onClick = onConfirmDraft) { Text("초안 확정") }
                }
            }

            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun RequestCameraPermission(onResult: (Boolean) -> Unit) {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = onResult
    )
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.CAMERA) }
}

private fun takeInMemory(
    imageCapture: ImageCapture,
    context: Context,
    onResult: (Bitmap?, Throwable?) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bmp = image.toBitmapNV21()
                    onResult(bmp, null)
                } catch (t: Throwable) {
                    onResult(null, t)
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onResult(null, exception)
            }
        }
    )
}

/**
 * 기기 호환성이 좋은 변환:
 * ImageProxy(YUV_420_888) → NV21 바이트 배열 → JPEG로 압축 → Bitmap 디코드 → 회전 보정
 */
private fun ImageProxy.toBitmapNV21(): Bitmap {
    // 1) YUV_420_888 → NV21
    val nv21 = yuv420888ToNv21(this)

    // 2) NV21 → JPEG
    val yuvImage = android.graphics.YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
    val jpegBytes = out.toByteArray()

    // 3) JPEG → Bitmap
    val bmp = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

    // 4) 회전 보정
    val matrix = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val yBuffer = image.planes[0].buffer // Y
    val uBuffer = image.planes[1].buffer // U
    val vBuffer = image.planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // Y
    yBuffer.get(nv21, 0, ySize)

    // VU (NV21 형식: V 다음 U)
    val chromaRowStride = image.planes[1].rowStride
    val chromaPixelStride = image.planes[1].pixelStride
    var offset = ySize
    val width = image.width
    val height = image.height

    val vRowStride = image.planes[2].rowStride
    val vPixelStride = image.planes[2].pixelStride

    val uRowStride = image.planes[1].rowStride
    val uPixelStride = image.planes[1].pixelStride

    // 크로마 샘플링(2x2) 고려해서 NV21로 합치기
    var row = 0
    while (row < height / 2) {
        var col = 0
        while (col < width / 2) {
            val vuIndexV = row * vRowStride + col * vPixelStride
            val vuIndexU = row * uRowStride + col * uPixelStride
            nv21[offset++] = vBuffer.get(vuIndexV)
            nv21[offset++] = uBuffer.get(vuIndexU)
            col++
        }
        row++
    }
    return nv21
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    val provider = future.get()
                    if (continuation.isActive) continuation.resume(provider)
                } catch (t: Throwable) {
                    if (continuation.isActive) continuation.resumeWithException(t)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
        continuation.invokeOnCancellation { future.cancel(true) }
    }
