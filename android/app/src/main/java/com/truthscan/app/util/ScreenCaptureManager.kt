package com.truthscan.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import kotlin.math.min

class ScreenCaptureManager(
    context: Context,
    private val mediaProjection: MediaProjection
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics()
    private var virtualDisplay: android.media.projection.VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    init {
        val display = windowManager.defaultDisplay
        @Suppress("DEPRECATION")
        display.getMetrics(displayMetrics)
    }

    fun captureScreen(): Bitmap {
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "TruthScan",
            screenWidth,
            screenHeight,
            density,
            android.media.projection.VirtualDisplay.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            null
        )

        val countDownLatch = CountDownLatch(1)
        var bitmap: Bitmap? = null

        imageReader!!.setOnImageAvailableListener({ reader ->
            try {
                val image = reader.acquireLatestImage()
                if (image != null) {
                    bitmap = imageToBitmap(image)
                    image.close()
                    countDownLatch.countDown()
                }
            } catch (e: Exception) {
                countDownLatch.countDown()
            }
        }, null)

        try {
            countDownLatch.await()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        cleanup()
        return bitmap ?: throw Exception("Failed to capture screen")
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val width = image.width
        val height = image.height
        val rowPadding = planes[0].rowPadding
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        buffer.rewind()
        val pixelBuffer = IntArray(width * height)
        for (i in 0 until height) {
            val offset = i * (width * pixelStride + rowPadding)
            buffer.position(offset)
            buffer.asIntBuffer().get(pixelBuffer, i * width, width)
        }
        bitmap.setPixels(pixelBuffer, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val scaledBitmap = scaleBitmap(bitmap, 1080, 1920)
        val stream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val imageBytes = stream.toByteArray()
        return android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scale = min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun release() {
        cleanup()
    }

    private fun cleanup() {
        virtualDisplay?.release()
        imageReader?.close()
        virtualDisplay = null
        imageReader = null
    }
}
