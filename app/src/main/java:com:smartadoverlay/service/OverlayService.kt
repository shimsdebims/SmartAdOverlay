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

    override fun onCreate() {
        super.onCreate()
        setupOverlay()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val category = intent.getStringExtra("category") ?: "entertainment"
                    showAd(category)
                }
            },
            IntentFilter("CONTENT_CLASSIFIED")
        )
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        windowManager.addView(overlayView, params)
    }

    private fun showAd(category: String) {
        val adContent = AdSelectionEngine(this).selectAd(category)
        overlayView.findViewById<ImageView>(R.id.ad_view).apply {
            setImageBitmap(ImageUtils.adjustTransparency(
                adContent.bitmap, 
                adContent.metadata.transparency
            ))
            visibility = View.VISIBLE
            postDelayed({ visibility = View.INVISIBLE }, 
                adContent.metadata.displayDuration)
        }
    }
}