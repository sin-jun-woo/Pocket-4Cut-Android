package com.pocket4cut.camera

import android.content.Context
import android.util.Rational
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CaptureEngine(
    private val context: Context,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    suspend fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    ) {
        val provider = getCameraProvider()
        cameraProvider = provider

        awaitLaidOut(previewView)
        val rotation = previewView.display.rotation

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
            .build()

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        provider.unbindAll()
        val viewPort = ViewPort.Builder(Rational(previewView.width, previewView.height), rotation)
            .setScaleType(ViewPort.FILL_CENTER)
            .build()
        val group = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(preview)
            .addUseCase(capture)
            .build()
        camera = provider.bindToLifecycle(lifecycleOwner, selector, group)

        imageCapture = capture
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        imageCapture = null
        camera = null
    }

    fun zoomRatioRange(): Pair<Float, Float>? {
        val state = camera?.cameraInfo?.zoomState?.value ?: return null
        return state.minZoomRatio to state.maxZoomRatio
    }

    fun currentZoomRatio(): Float? =
        camera?.cameraInfo?.zoomState?.value?.zoomRatio

    fun setZoomRatio(ratio: Float) {
        val cam = camera ?: return
        val state = cam.cameraInfo.zoomState.value ?: return
        val clamped = ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        val executor = ContextCompat.getMainExecutor(context)
        cam.cameraControl.setZoomRatio(clamped).addListener({}, executor)
    }

    suspend fun takePictureToFile(targetFile: File): File {
        val capture = imageCapture ?: error("Camera is not bound")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(targetFile).build()
        val executor: Executor = ContextCompat.getMainExecutor(context)

        return suspendCancellableCoroutine { cont ->
            capture.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        if (cont.isActive) cont.resume(targetFile)
                        else targetFile.delete()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (cont.isActive) cont.resumeWithException(exception)
                        else targetFile.delete()
                    }
                },
            )
            cont.invokeOnCancellation { targetFile.delete() }
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider {
        val existing = cameraProvider
        if (existing != null) return existing

        val future = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        return suspendCancellableCoroutine { cont ->
            future.addListener(
                {
                    try {
                        val provider = future.get()
                        if (cont.isActive) cont.resume(provider)
                    } catch (t: Throwable) {
                        if (cont.isActive) cont.resumeWithException(t)
                    }
                },
                executor,
            )
        }
    }

    private suspend fun awaitLaidOut(previewView: PreviewView) {
        if (previewView.width > 0 && previewView.height > 0) return
        suspendCancellableCoroutine { cont ->
            val listener = object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View, left: Int, top: Int, right: Int, bottom: Int,
                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int,
                ) {
                    if (right > left && bottom > top && cont.isActive) {
                        previewView.removeOnLayoutChangeListener(this)
                        cont.resume(Unit)
                    }
                }
            }
            previewView.addOnLayoutChangeListener(listener)
            cont.invokeOnCancellation { previewView.post { previewView.removeOnLayoutChangeListener(listener) } }
            if (previewView.width > 0 && previewView.height > 0 && cont.isActive) {
                previewView.removeOnLayoutChangeListener(listener)
                cont.resume(Unit)
            }
        }
    }
}

