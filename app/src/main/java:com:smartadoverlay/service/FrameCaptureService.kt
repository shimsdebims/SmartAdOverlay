package com.smartadoverlay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.smartadoverlay.R
import com.smartadoverlay.ai.ContentAnalyzer
import com.smartadoverlay.ai.ContentClassifier
import com.smartadoverlay.util.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class FrameCaptureService : Service() {
    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startCapture()
        return START_STICKY
    }

    private fun startCapture() {
        val metrics = resources.displayMetrics
        imageReader = ImageReader(metrics.widthPixels, metrics.heightPixels, 
            PixelFormat.RGBA_8888, 2)

        // Use MediaProjection to capture HDMIin app's output
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "HDMIin_Capture",
                imageReader.width, imageReader.height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface, null, null
            )
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                processImage(image)
                image.close()
            }, Handler(Looper.getMainLooper()))
        }
    }

    private fun processImage(image: Image) {
        val bitmap = ImageUtils.imageToBitmap(image) // Convert to Bitmap
        contentAnalyzer.analyzeFrame(bitmap) { objects ->
            val category = contentClassifier.classifyContent(objects)
            sendBroadcast(Intent("CONTENT_CLASSIFIED").apply {
                putExtra("category", category)
            })
        }
    }
}
    
    // This attempts to access HDMIin app's frames directly
    // The exact implementation will depend on your box's capabilities
    private fun setupHDMIFrameCapture() {
        try {
            // Initialize frame grabber
            imageReader = ImageReader.newInstance(
                displayWidth,
                displayHeight,
                PixelFormat.RGBA_8888,
                2
            )
            
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    processImage(image)
                    image.close()
                }
            }, Handler(Looper.getMainLooper()))
            
            // Note: This is a simplified version - in reality, you'd need to:
            // 1. Find the HDMIin app's surface
            // 2. Request permission to capture its output
            // 3. Set up a VirtualDisplay or similar to mirror its content
            
            Log.d(TAG, "HDMIin frame capture setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set up HDMIin capture: ${e.message}")
            throw e
        }
    }
    
    // Fallback method if direct access to HDMIin app isn't working
    private fun startFallbackFrameCapture() {
        serviceScope.launch {
            // In a real implementation, this would use MediaProjection API
            // or accessibility services to capture the screen
            Log.d(TAG, "Using fallback capture method")
            
            while (true) {
                // For demo purposes, we'll simulate frame capture
                val dummyFrame = Bitmap.createBitmap(displayWidth, displayHeight, Bitmap.Config.ARGB_8888)
                processFrame(dummyFrame)
                delay(2000) // Capture every 2 seconds
            }
        }
    }
    
    private fun captureCurrentFrame(): Bitmap? {
        // This is where you'd implement the actual frame grabbing
        // from the HDMIin app
        
        // For development purposes, this returns a dummy bitmap
        // You'll need to replace this with actual capture code
        return Bitmap.createBitmap(displayWidth, displayHeight, Bitmap.Config.ARGB_8888)
    }
    
    private fun processImage(image: Image) {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * displayWidth
        
        // Create bitmap from image buffer
        val bitmap = Bitmap.createBitmap(
            displayWidth + rowPadding / pixelStride,
            displayHeight,
            Bitmap.Config.ARGB_8888
        )
        
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        
        // Process the bitmap
        processFrame(bitmap)
    }
    
    private fun processFrame(frame: Bitmap) {
        // Send frame for content analysis
        contentAnalyzer.analyzeFrame(frame) { objects ->
            // Once analyzed, classify the content
            val category = contentClassifier.classifyContent(objects)
            
            // Broadcast the result to the overlay service
            val intent = Intent("com.smartadoverlay.CONTENT_CLASSIFIED")
            intent.putExtra("category", category)
            sendBroadcast(intent)
            
            Log.d(TAG, "Frame processed, category: $category")
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Frame Capture Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Ad Overlay")
            .setContentText("Analyzing content for intelligent ad placement")
            .setSmallIcon(R.drawable.ad_sports) // Replace with your icon
            .build()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        captureJob?.cancel()
        imageReader?.close()
        super.onDestroy()
    }
}