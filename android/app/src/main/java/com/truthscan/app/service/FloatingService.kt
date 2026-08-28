package com.truthscan.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.truthscan.app.R
import com.truthscan.app.api.GeminiRepository
import com.truthscan.app.ui.ResultOverlayView
import com.truthscan.app.util.ScreenCaptureManager
import com.truthscan.app.util.UsageTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var screenCapture: ScreenCaptureManager
    private lateinit var geminiRepo: GeminiRepository
    private lateinit var usageTracker: UsageTracker

    private var floatingIcon: ImageButton? = null
    private var resultOverlay: ResultOverlayView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        geminiRepo = GeminiRepository()
        usageTracker = UsageTracker(this)
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: return START_STICKY
        val resultData = intent.getParcelableExtra("resultData") as? Intent ?: return START_STICKY

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)!!
        screenCapture = ScreenCaptureManager(this, mediaProjection)

        createFloatingIcon()
        return START_STICKY
    }

    private fun startForeground() {
        val channelId = "truthscan_floating"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TruthScan Floating Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TruthScan")
            .setContentText("Floating service is running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(false)
            .build()

        startForeground(1, notification)
    }

    private fun createFloatingIcon() {
        val params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = 80
            height = 80
            x = 100
            y = 100
            gravity = Gravity.TOP or Gravity.START
        }

        floatingIcon = ImageButton(this).apply {
            setBackgroundColor(Color.parseColor("#FF6B35"))
            setImageResource(android.R.drawable.ic_menu_search)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            setOnClickListener { onFloatingIconClick() }
            setOnTouchListener(FloatingIconTouchListener(params))
        }

        windowManager.addView(floatingIcon, params)
    }

    private fun onFloatingIconClick() {
        if (!usageTracker.canUseToday()) {
            Toast.makeText(this, "Daily limit reached (5 checks)", Toast.LENGTH_SHORT).show()
            return
        }

        floatingIcon?.isEnabled = false
        Toast.makeText(this, "Capturing screenshot...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                val bitmap = screenCapture.captureScreen()
                analyzeScreenshot(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this@FloatingService, "Capture failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                floatingIcon?.isEnabled = true
            }
        }
    }

    private fun analyzeScreenshot(bitmap: Bitmap) {
        scope.launch {
            try {
                val base64 = screenCapture.bitmapToBase64(bitmap)

                geminiRepo.analyzeImage(base64, "image/jpeg") { result ->
                    if (result.isSuccess) {
                        usageTracker.incrementUsageCount()
                        showResultOverlay(result.getOrNull()!!)
                    } else {
                        Toast.makeText(this@FloatingService, "Analysis failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@FloatingService, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showResultOverlay(result: AnalysisResult) {
        resultOverlay?.let { windowManager.removeView(it) }

        val params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = 320
            height = 480
            x = 50
            y = 200
            gravity = Gravity.TOP or Gravity.START
        }

        resultOverlay = ResultOverlayView(this, result) { removeResultOverlay() }
        windowManager.addView(resultOverlay, params)

        handler.postDelayed({ removeResultOverlay() }, 8000)
    }

    private fun removeResultOverlay() {
        resultOverlay?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Already removed
            }
            resultOverlay = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        floatingIcon?.let { windowManager.removeView(it) }
        resultOverlay?.let { windowManager.removeView(it) }
        screenCapture.release()
        mediaProjection.stop()
    }

    // Dragging logic for floating icon
    private inner class FloatingIconTouchListener(val params: WindowManager.LayoutParams) : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    if (kotlin.math.abs(deltaX) > 10 || kotlin.math.abs(deltaY) > 10) {
                        isDragging = true
                        params.x = (initialX + deltaX).toInt()
                        params.y = (initialY + deltaY).toInt()
                        windowManager.updateViewLayout(floatingIcon, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    return !isDragging
                }
            }
            return false
        }
    }
}

data class AnalysisResult(
    val credibility: Int,
    val verdict: String,
    val reason: List<String>,
    val suggestions: List<String>
)
