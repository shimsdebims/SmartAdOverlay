package com.smartadoverlay.ads

import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import com.smartadoverlay.ai.ObjectWithClassification

/**
 * Handles the strategy for placing ads on screen based on content analysis
 */
class AdPlacementStrategy {
    
    private val TAG = "AdPlacementStrategy"
    
    // Screen dimensions
    private var screenWidth = 1280
    private var screenHeight = 720
    
    // Margin from screen edges
    private val screenMargin = 20
    
    /**
     * Updates the screen dimensions
     */
    fun updateScreenDimensions(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }
    
    /**
     * Calculates the optimal position for an ad based on:
     * 1. Content analysis (to avoid covering important elements)
     * 2. Ad's preferred placement
     * 3. Ad dimensions
     * 
     * Returns the coordinates for the top-left corner of the ad
     */
    fun calculateAdPosition(
        adContent: AdContent,
        detectedObjects: List<ObjectWithClassification> = emptyList()
    ): Point {
        val adWidth = adContent.bitmap.width
        val adHeight = adContent.bitmap.height
        
        // Start with preferred position based on category
        val preferredPosition = when (adContent.metadata.placementPreference) {
            PlacementPreference.TOP_LEFT -> Point(screenMargin, screenMargin)
            PlacementPreference.TOP_RIGHT -> Point(screenWidth - adWidth - screenMargin, screenMargin)
            PlacementPreference.BOTTOM_LEFT -> Point(screenMargin, screenHeight - adHeight - screenMargin)
            PlacementPreference.BOTTOM_RIGHT -> Point(screenWidth - adWidth - screenMargin, screenHeight - adHeight - screenMargin)
            PlacementPreference.CENTER -> Point((screenWidth - adWidth) / 2, (screenHeight - adHeight) / 2)
        }
        
        // If no objects detected, just use preferred position
        if (detectedObjects.isEmpty()) {
            return preferredPosition
        }
        
        // Check for collisions with important content
        val adRect = Rect(
            preferredPosition.x,
            preferredPosition.y,
            preferredPosition.x + adWidth,
            preferredPosition.y + adHeight
        )
        
        // If no collision, use preferred position
        if (!hasSignificantCollision(adRect, detectedObjects)) {
            return preferredPosition
        }
        
        // Try alternative positions if preferred position has collision
        Log.d(TAG, "Preferred position has collision, finding alternative")
        
        // Generate alternative positions to try
        val alternativePositions = listOf(
            Point(screenMargin, screenMargin),  // Top left
            Point(screenWidth - adWidth - screenMargin, screenMargin),  // Top right
            Point(screenMargin, screenHeight - adHeight - screenMargin),  // Bottom left
            Point(screenWidth - adWidth - screenMargin, screenHeight - adHeight - screenMargin),  // Bottom right
            Point((screenWidth - adWidth) / 2, screenHeight - adHeight - screenMargin)  // Bottom center
        )
        
        // Find best alternative with least collision
        var bestPosition = preferredPosition
        var leastCollisionScore = Double.MAX_VALUE
        
        for (position in alternativePositions) {
            val testRect = Rect(
                position.x,
                position.y,
                position.x + adWidth,
                position.y + adHeight
            )
            
            val collisionScore = calculateCollisionScore(testRect, detectedObjects)
            
            if (collisionScore < leastCollisionScore) {
                leastCollisionScore = collisionScore
                bestPosition = position
            }
        }
        
        Log.d(TAG, "Selected position: $bestPosition with collision score $leastCollisionScore")
        return bestPosition
    }
    
    /**
     * Checks if the ad rectangle significantly collides with important objects
     */
    private fun hasSignificantCollision(
        adRect: Rect,
        detectedObjects: List<ObjectWithClassification>
    ): Boolean {
        // Check for collisions with important objects
        for (obj in detectedObjects) {
            if (Rect.intersects(adRect, obj.boundingBox)) {
                // Calculate overlap area
                val overlapRect = Rect()
                overlapRect.setIntersect(adRect, obj.boundingBox)
                val overlapArea = overlapRect.width() * overlapRect.height()
                
                // Calculate object area
                val objectArea = obj.boundingBox.width() * obj.boundingBox.height()
                
                // If overlap is significant (>30% of object), consider it a collision
                if (overlapArea > objectArea * 0.3) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Calculates a collision score for a potential ad position
     * Lower score is better (less collision)
     */
    private fun calculateCollisionScore(
        adRect: Rect,
        detectedObjects: List<ObjectWithClassification>
    ): Double {
        var score = 0.0
        
        for (obj in detectedObjects) {
            if (Rect.intersects(adRect, obj.boundingBox)) {
                // Calculate overlap area
                val overlapRect = Rect()
                overlapRect.setIntersect(adRect, obj.boundingBox)
                val overlapArea = overlapRect.width() * overlapRect.height()
                
                // Calculate object area
                val objectArea = obj.boundingBox.width() * obj.boundingBox.height()
                
                // Calculate object importance based on labels
                val importance = calculateObjectImportance(obj)
                
                // Add to score: overlap ratio * importance
                score += (overlapArea.toDouble() / objectArea) * importance
            }
        }
        
        return score
    }
    
    /**
     * Calculates the importance of an object based on its labels
     * Higher value = more important to avoid covering
     */
    private fun calculateObjectImportance(obj: ObjectWithClassification): Double {
        // Check for important objects like faces, text, etc.
        val importantLabels = mapOf(
            "face" to 3.0,
            "person" to 2.5,
            "text" to 2.0,
            "logo" to 1.5,
            "product" to 1.2
        )
        
        var highestImportance = 1.0  // Default importance
        
        for (label in obj.labels) {
            for ((key, importance) in importantLabels) {
                if (label.text.contains(key, ignoreCase = true)) {
                    if (importance > highestImportance) {
                        highestImportance = importance
                    }
                }
            }
        }
        
        return highestImportance
    }
}