package com.truthscan.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.truthscan.app.service.FloatingService
import com.truthscan.app.util.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjectionResultCode: Int = -1
    private var mediaProjectionResultData: Intent? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updateStatus("Notification permission: ${if (isGranted) "✓" else "✗"}")
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            mediaProjectionResultCode = result.resultCode
            mediaProjectionResultData = result.data
            startFloatingService()
        } else {
            updateStatus("Screenshot permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        startButton.setOnClickListener { checkPermissionsAndStart() }

        updateStatus("Ready to start\nPlease check permissions below")
        checkPermissionStatus()
    }

    private fun checkPermissionStatus() {
        val hasWindowPermission = PermissionHelper.hasSystemAlertWindowPermission(this)
        updateStatus("Window permission: ${if (hasWindowPermission) "✓" else "✗ (open Settings)"}")
    }

    private fun checkPermissionsAndStart() {
        // 1. Check floating window permission
        if (!PermissionHelper.hasSystemAlertWindowPermission(this)) {
            AlertDialog.Builder(this)
                .setTitle("Floating Window Permission")
                .setMessage("TruthScan needs permission to display a floating icon.\n\nGo to: Settings > Apps > Special app access > Display over other apps")
                .setPositiveButton("Go to Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        // 2. Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 3. Request screen capture permission
        val projectionIntent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(projectionIntent)
    }

    private fun startFloatingService() {
        if (mediaProjectionResultCode == -1 || mediaProjectionResultData == null) {
            updateStatus("Screenshot permission not granted")
            return
        }

        val intent = Intent(this, FloatingService::class.java).apply {
            putExtra("resultCode", mediaProjectionResultCode)
            putExtra("resultData", mediaProjectionResultData)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        updateStatus("Floating service started ✓\nYou can close this app now")
        startButton.isEnabled = false
    }

    private fun updateStatus(text: String) {
        statusText.text = text
    }
}
