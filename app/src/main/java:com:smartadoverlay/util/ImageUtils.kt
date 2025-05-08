package com.smartadoverlay.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import java.nio.ByteBuffer

object ImageUtils {
    
    fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        // Create bitmap
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }
    
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = scaleWidth.coerceAtMost(scaleHeight)
        
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }
    
    fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }
    
    fun adjustTransparency(bitmap: Bitmap, alpha: Float): Bitmap {
        val adjustedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(adjustedBitmap.width * adjustedBitmap.height)
        adjustedBitmap.getPixels(pixels, 0, adjustedBitmap.width, 0, 0, 
            adjustedBitmap.width, adjustedBitmap.height)
        
        val alphaValue = (alpha * 255).toInt()
        
        for (i in pixels.indices) {
            pixels[i] = (alphaValue shl 24) or (pixels[i] and 0x00FFFFFF)
        }
        
        adjustedBitmap.setPixels(pixels, 0, adjustedBitmap.width, 0, 0, 
            adjustedBitmap.width, adjustedBitmap.height)
        return adjustedBitmap
    }
}