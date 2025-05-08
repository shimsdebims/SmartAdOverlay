package com.smartadoverlay.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: Activity) {
    
    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
        
        private const val PERMISSION_REQUEST_CODE = 200
    }
    
    fun checkAndRequestPermissions(): Boolean {
        val missingPermissions = getMissingPermissions()
        return if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions,
                PERMISSION_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }
    
    fun getMissingPermissions(): Array<String> {
        return REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
    
    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            return grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        }
        return false
    }
}