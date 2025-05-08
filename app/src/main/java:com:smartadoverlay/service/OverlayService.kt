package com.smartadoverlay.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartadoverlay.R
import com.smartadoverlay.ads.AdContent
import com.smartadoverlay.ads.AdSelectionEngine
import com.smartadoverlay.util.ImageUtils

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var adImageView: ImageView
    private lateinit var adSelectionEngine: AdSelectionEngine
    private var currentAd: AdContent? = null

    override fun onCreate() {
        super.onCreate()
        setupOverlay()
        setupAdEngine()
        registerReceiver()
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        adImageView = overlayView.findViewById(R.id.ad_view)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        windowManager.addView(overlayView, params)
    }

    private fun setupAdEngine() {
        adSelectionEngine = AdSelectionEngine(this)
    }

    private fun registerReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context, intent: Intent) {
                    val category = intent.getStringExtra("category") ?: "entertainment"
                    showAdForCategory(category)
                }
            },
            android.content.IntentFilter("com.smartadoverlay.CONTENT_CLASSIFIED")
        )
    }

    private fun showAdForCategory(category: String) {
        val adContent = adSelectionEngine.selectAd(category)
        currentAd = adContent

        // Apply transparency and display
        val transparentAd = ImageUtils.adjustTransparency(
            adContent.bitmap,
            adContent.metadata.transparency
        )
        adImageView.setImageBitmap(transparentAd)
        adImageView.visibility = View.VISIBLE

        // Schedule ad removal after duration
        adImageView.postDelayed({
            adImageView.visibility = View.INVISIBLE
        }, adContent.metadata.displayDuration)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}