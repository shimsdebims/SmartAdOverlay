package com.smartadoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartadoverlay.service.FrameCaptureService
import com.smartadoverlay.service.OverlayService
import com.smartadoverlay.util.PermissionHelper

class MainActivity : AppCompatActivity() {
    
    private val REQUEST_OVERLAY_PERMISSION = 100
    private val permissionHelper = PermissionHelper(this)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            checkPermissionsAndStartServices()
        }
        
        findViewById<Button>(R.id.btnStopService).setOnClickListener {
            stopServices()
        }
    }
    
    private fun checkPermissionsAndStartServices() {
        if (!Settings.canDrawOverlays(this)) {
            // Request overlay permission
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        } else if (permissionHelper.checkAndRequestPermissions()) {
            startServices()
        }
    }
    
    private fun startServices() {
        // Start Frame Capture Service
        val frameCaptureIntent = Intent(this, FrameCaptureService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(frameCaptureIntent)
        } else {
            startService(frameCaptureIntent)
        }
        
        // Start Overlay Service
        val overlayIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(overlayIntent)
        } else {
            startService(overlayIntent)
        }
        
        Toast.makeText(this, "Smart Ad Overlay services started", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopServices() {
        // Stop Frame Capture Service
        stopService(Intent(this, FrameCaptureService::class.java))
        
        // Stop Overlay Service
        stopService(Intent(this, OverlayService::class.java))
        
        Toast.makeText(this, "Smart Ad Overlay services stopped", Toast.LENGTH_SHORT).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this) && permissionHelper.checkAndRequestPermissions()) {
                startServices()
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionHelper.handlePermissionsResult(requestCode, permissions, grantResults)) {
            if (Settings.canDrawOverlays(this)) {
                startServices()
            }
        } else {
            Toast.makeText(this, "Required permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
}
val hdmiIntent = packageManager.getLaunchIntentForPackage("com.vendor.hdmiin")
startActivity(hdmiIntent)