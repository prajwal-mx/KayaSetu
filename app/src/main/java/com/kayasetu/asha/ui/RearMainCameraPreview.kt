package com.kayasetu.asha.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs
import kotlin.math.max

/**
 * Selects strictly the Primary 1x Rear Main Camera (LENS_FACING_BACK),
 * explicitly rejecting front cameras, depth sensors, macro lenses, and ultrawide lenses.
 */
fun selectPrimaryRearMainCameraId(cameraManager: CameraManager): String? {
    return try {
        val ids = cameraManager.cameraIdList
        if (ids.isEmpty()) return null

        var bestId: String? = null
        var bestScore = -1.0

        for (id in ids) {
            val chars = cameraManager.getCameraCharacteristics(id)
            val facing = chars.get(CameraCharacteristics.LENS_FACING)
            if (facing != CameraCharacteristics.LENS_FACING_BACK) continue

            val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            val isBackwardCompatible = capabilities?.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
            ) ?: true
            if (!isBackwardCompatible) continue

            // Physical sensor size (mm^2) — Main camera always has the largest physical sensor;
            // macro (2MP) and ultrawide sensors are significantly smaller.
            val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val sensorArea = if (sensorSize != null) {
                (sensorSize.width * sensorSize.height).toDouble()
            } else {
                15.0
            }

            // Focal length check — Standard 1x main lens is typically ~3.5mm to 7.0mm;
            // Ultrawide and macro lenses typically have very short focal lengths (< 2.3mm).
            val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            val primaryFocal = focalLengths?.firstOrNull() ?: 4.5f
            val focalBonus = if (primaryFocal in 3.2f..7.5f) 50.0 else -40.0

            // Android CDD guarantees camera ID "0" (if back-facing) is the primary rear main camera
            val idZeroBonus = if (id == "0") 200.0 else 0.0

            val totalScore = idZeroBonus + sensorArea * 2.0 + focalBonus
            if (totalScore > bestScore) {
                bestScore = totalScore
                bestId = id
            }
        }
        bestId ?: ids.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (_: Exception) {
        null
    }
}

class RearMainCameraController(private val context: Context) {
    var textureView: TextureView? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size = Size(1280, 960)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun captureBitmap(): Bitmap? {
        val tv = textureView ?: return null
        return if (tv.isAvailable) {
            try {
                tv.bitmap
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun startRearMainCamera(onError: () -> Unit = {}) {
        val tv = textureView ?: return
        if (!tv.isAvailable) return

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: run {
            onError()
            return
        }

        val rearMainId = selectPrimaryRearMainCameraId(cameraManager) ?: run {
            onError()
            return
        }

        try {
            val chars = cameraManager.getCameraCharacteristics(rearMainId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map?.getOutputSizes(SurfaceTexture::class.java)
            if (!outputSizes.isNullOrEmpty()) {
                // Choose a high-clarity 4:3 or 1:1 preview size around 1280p–1920p from the main sensor
                previewSize = outputSizes
                    .filter { it.width in 720..2560 && it.height in 720..2560 }
                    .minByOrNull { abs((it.width.toDouble() / it.height.toDouble()) - 1.333) }
                    ?: outputSizes.first()
            }

            closeCamera()

            cameraManager.openCamera(
                rearMainId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        createPreviewSession(camera, chars)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cameraDevice = null
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        cameraDevice = null
                        onError()
                    }
                },
                mainHandler
            )
        } catch (_: Exception) {
            onError()
        }
    }

    private fun createPreviewSession(camera: CameraDevice, chars: CameraCharacteristics) {
        val tv = textureView ?: return
        val surfaceTexture = tv.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        configureCenterCropTransform(tv, previewSize.width, previewSize.height)

        val previewSurface = Surface(surfaceTexture)
        try {
            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)

                // Continuous picture autofocus on the main rear sensor
                val afModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                if (afModes != null && afModes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                } else if (afModes != null && afModes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO)) {
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                }

                // Auto exposure with flash strictly off to prevent skin specular glare
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)

                // Lock zoom ratio strictly to 1.0f on Android 11+ so the HAL never auto-switches
                // to a secondary macro or ultrawide lens (< 1.0x)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val zoomRange = chars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                    val targetZoom = if (zoomRange != null) {
                        1.0f.coerceIn(zoomRange.lower, zoomRange.upper)
                    } else {
                        1.0f
                    }
                    set(CaptureRequest.CONTROL_ZOOM_RATIO, targetZoom)
                }
            }

            @Suppress("DEPRECATION")
            camera.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        try {
                            session.setRepeatingRequest(builder.build(), null, mainHandler)
                        } catch (_: Exception) {
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                mainHandler
            )
        } catch (_: Exception) {
        }
    }

    fun configureCenterCropTransform(tv: TextureView, bufW: Int, bufH: Int) {
        val viewW = tv.width.toFloat()
        val viewH = tv.height.toFloat()
        if (viewW <= 0f || viewH <= 0f || bufW <= 0 || bufH <= 0) return

        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewW, viewH)
        val bufRect = RectF(0f, 0f, bufH.toFloat(), bufW.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        bufRect.offset(centerX - bufRect.centerX(), centerY - bufRect.centerY())
        matrix.setRectToRect(viewRect, bufRect, Matrix.ScaleToFit.FILL)
        val scale = max(viewH / bufW.toFloat(), viewW / bufH.toFloat())
        matrix.postScale(scale, scale, centerX, centerY)
        tv.setTransform(matrix)
    }

    fun closeCamera() {
        try {
            captureSession?.close()
        } catch (_: Exception) {
        }
        captureSession = null
        try {
            cameraDevice?.close()
        } catch (_: Exception) {
        }
        cameraDevice = null
    }
}

@Composable
fun RearMainCameraViewfinder(
    controller: RearMainCameraController,
    onHardwareUnavailable: () -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(controller) {
        onDispose {
            controller.closeCamera()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                controller.textureView = this
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        controller.startRearMainCamera(onError = onHardwareUnavailable)
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        controller.configureCenterCropTransform(this@apply, width, height)
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        controller.closeCamera()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        }
    )
}

@Composable
fun rememberRearMainCameraController(): RearMainCameraController {
    val context = LocalContext.current
    return remember { RearMainCameraController(context) }
}
