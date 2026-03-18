package com.pocket4cut.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CaptureEngine(
    private val context: Context,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    suspend fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        val provider = getCameraProvider()
        cameraProvider = provider

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val selector = CameraSelector.DEFAULT_FRONT_CAMERA

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)

        imageCapture = capture
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        imageCapture = null
    }

    suspend fun takePictureToFile(targetFile: File): File {
        val capture = imageCapture ?: error("Camera is not bound")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(targetFile).build()
        val executor: Executor = ContextCompat.getMainExecutor(context)

        return suspendCoroutine { cont ->
            capture.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        cont.resume(targetFile)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resumeWithException(exception)
                    }
                },
            )
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider {
        val existing = cameraProvider
        if (existing != null) return existing

        val future = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        return suspendCoroutine { cont ->
            future.addListener(
                {
                    try {
                        cont.resume(future.get())
                    } catch (t: Throwable) {
                        cont.resumeWithException(t)
                    }
                },
                executor,
            )
        }
    }
}

