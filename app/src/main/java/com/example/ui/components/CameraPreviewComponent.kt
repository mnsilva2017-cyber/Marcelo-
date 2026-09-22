package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenLight

/**
 * Componente CameraX para exibição do preview em tempo real e captura de fotos.
 * Suporta alternância de câmera (frontal/traseira), modos de flash e tratamento de erros.
 */
@Composable
fun CameraPreviewComponent(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    initialLensFacing: Int = CameraSelector.LENS_FACING_FRONT
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    var lensFacing by remember { mutableIntStateOf(initialLensFacing) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var bindError by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()
    }

    // Initialize CameraProvider
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: Exception) {
                Log.e("CameraPreviewComponent", "Falha ao inicializar CameraProvider", e)
                bindError = "Não foi possível inicializar a câmera: ${e.localizedMessage}"
                onError(bindError!!)
            }
        }, mainExecutor)
    }

    // Cleanup camera provider bindings on dispose
    DisposableEffect(cameraProvider) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraPreviewComponent", "Erro ao desvincular câmera", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .border(BorderStroke(2.dp, WhatsAppGreen.copy(alpha = 0.5f)), RoundedCornerShape(24.dp))
            .testTag("camera_preview_container"),
        contentAlignment = Alignment.Center
    ) {
        if (bindError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Aviso da Câmera",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bindError ?: "Erro desconhecido",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        } else {
            // Live CameraX Preview
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camerax_preview_view"),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { previewView ->
                    val provider = cameraProvider ?: return@AndroidView
                    try {
                        provider.unbindAll()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreviewComponent", "Falha ao vincular câmera", e)
                        // Fallback: If requested lens is unavailable (e.g. front camera on some hardware), try other
                        try {
                            val fallbackLens = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                            val fallbackSelector = CameraSelector.Builder()
                                .requireLensFacing(fallbackLens)
                                .build()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                fallbackSelector,
                                preview,
                                imageCapture
                            )
                            lensFacing = fallbackLens
                        } catch (e2: Exception) {
                            bindError = "Câmera não disponível no dispositivo: ${e2.localizedMessage}"
                            onError(bindError!!)
                        }
                    }
                }
            )

            // Viewfinder decorative corner brackets
            ViewfinderOverlay()

            // Top Camera Bar: Lens Switch & Flash controls
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash button
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    IconButton(
                        onClick = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                                else -> ImageCapture.FLASH_MODE_AUTO
                            }
                            imageCapture.flashMode = flashMode
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_toggle_flash")
                    ) {
                        Icon(
                            imageVector = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                                else -> Icons.Default.FlashAuto
                            },
                            contentDescription = "Flash",
                            tint = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> Color.Gray
                                else -> Color(0xFFFBBF24)
                            }
                        )
                    }
                }

                // Switch Camera (Front / Back)
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_switch_camera")
                    ) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            contentDescription = "Alternar Câmera",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom Shutter Action Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                CaptureShutterButton(
                    isCapturing = isCapturing,
                    onClick = {
                        if (isCapturing) return@CaptureShutterButton
                        isCapturing = true

                        imageCapture.takePicture(
                            mainExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    try {
                                        val rotation = imageProxy.imageInfo.rotationDegrees
                                        val bitmap = imageProxy.toBitmap()
                                        val matrix = Matrix().apply {
                                            postRotate(rotation.toFloat())
                                            // Espelhar selfie frontal para efeito natural de espelho
                                            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                                postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                                            }
                                        }
                                        val rotated = Bitmap.createBitmap(
                                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                                        )
                                        isCapturing = false
                                        onImageCaptured(rotated)
                                    } catch (e: Exception) {
                                        Log.e("CameraPreviewComponent", "Erro ao processar imagem", e)
                                        isCapturing = false
                                        onError("Erro ao processar imagem capturada: ${e.localizedMessage}")
                                    } finally {
                                        imageProxy.close()
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraPreviewComponent", "Erro na captura da imagem", exception)
                                    isCapturing = false
                                    onError("Falha ao capturar imagem: ${exception.localizedMessage}")
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

/**
 * Botão disparador (Shutter) com design de anel moderno e feedback visual.
 */
@Composable
private fun CaptureShutterButton(
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.25f))
            .clickable(enabled = !isCapturing, onClick = onClick)
            .testTag("btn_camera_shutter"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (isCapturing) Color.Gray else Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = WhatsAppGreen,
                    strokeWidth = 3.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Disparar Câmera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Grade sutil de enquadramento facial no visor
 */
@Composable
private fun ViewfinderOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                RoundedCornerShape(16.dp)
            )
    )
}
