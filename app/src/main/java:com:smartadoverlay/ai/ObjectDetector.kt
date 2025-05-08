package com.smartadoverlay.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles object detection in images using ML Kit
 */
class ObjectDetector(private val context: Context) {
    
    private val TAG = "ObjectDetector"
    
    // Configure object detector
    private val detector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        
        ObjectDetection.getClient(options)
    }
    
    /**
     * Detects objects in the provided image
     * Returns a list of detected objects with their classifications
     */
    suspend fun detectObjects(image: Bitmap): List<ObjectWithClassification> = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<List<ObjectWithClassification>>()
        
        try {
            val inputImage = InputImage.fromBitmap(image, 0)
            
            detector.process(inputImage)
                .addOnSuccessListener { detectedObjects ->
                    val results = mutableListOf<ObjectWithClassification>()
                    
                    for (detectedObject in detectedObjects) {
                        val boundingBox = detectedObject.boundingBox
                        val labels = detectedObject.labels.map { label ->
                            ObjectLabel(label.text, label.confidence)
                        }
                        
                        results.add(
                            ObjectWithClassification(
                                boundingBox = boundingBox,
                                labels = labels
                            )
                        )
                    }
                    
                    deferred.complete(results)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Object detection failed: ${e.message}")
                    deferred.complete(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during object detection: ${e.message}")
            deferred.complete(emptyList())
        }
        
        return@withContext deferred.await()
    }
    
    /**
     * Provides mock object detection results for testing
     */
    fun getMockDetectionResults(): List<ObjectWithClassification> {
        val mockObjects = listOf(
            ObjectWithClassification(
                boundingBox = Rect(100, 100, 300, 300),
                labels = listOf(
                    ObjectLabel("person", 0.92f),
                    ObjectLabel("player", 0.87f)
                )
            ),
            ObjectWithClassification(
                boundingBox = Rect(400, 200, 500, 250),
                labels = listOf(
                    ObjectLabel("ball", 0.95f)
                )
            ),
            ObjectWithClassification(
                boundingBox = Rect(50, 400, 600, 700),
                labels = listOf(
                    ObjectLabel("field", 0.89f),
                    ObjectLabel("grass", 0.85f)
                )
            )
        )
        
        return mockObjects
    }
}

/**
 * Represents a detected object with its bounding box and classification labels
 */
data class ObjectWithClassification(
    val boundingBox: Rect,
    val labels: List<ObjectLabel>
)

/**
 * Represents a classification label with text and confidence score
 */
data class ObjectLabel(
    val text: String,
    val confidence: Float
)