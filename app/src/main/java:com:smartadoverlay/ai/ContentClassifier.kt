package com.smartadoverlay.ai

import android.content.Context
import android.util.Log

/**
 * Takes detected objects and classifies content into categories for ad targeting
 */
class ContentClassifier(private val context: Context) {
    
    private val TAG = "ContentClassifier"
    
    // Category keywords mapping
    private val categoryKeywords = mapOf(
        "sports" to listOf(
            "ball", "player", "field", "stadium", "game", "match", "team", 
            "soccer", "football", "basketball", "tennis", "baseball", "sport",
            "athlete", "competition", "race", "win", "lose", "score", "goal"
        ),
        "entertainment" to listOf(
            "movie", "film", "actor", "actress", "drama", "comedy", "show", 
            "theater", "cinema", "series", "television", "tv", "stream", 
            "episode", "scene", "performance", "stage", "audience", "applause"
        ),
        "cooking" to listOf(
            "food", "kitchen", "chef", "recipe", "meal", "dish", "cook", 
            "bake", "ingredient", "spice", "taste", "flavor", "cuisine",
            "restaurant", "dinner", "lunch", "breakfast", "vegetable", "meat"
        ),
        "music" to listOf(
            "instrument", "concert", "musician", "band", "song", "singer", 
            "vocalist", "melody", "rhythm", "beat", "lyrics", "guitar", 
            "piano", "drum", "performance", "stage", "album", "record", "note"
        )
    )
    
    /**
     * Classifies content based on detected objects and labels
     * Returns the most likely content category
     */
    fun classifyContent(detectedItems: List<DetectedContent>): String {
        // If no items detected, return default
        if (detectedItems.isEmpty()) {
            return "entertainment"
        }
        
        // Calculate scores for each category
        val categoryScores = mutableMapOf<String, Float>()
        
        for (category in categoryKeywords.keys) {
            categoryScores[category] = 0f
        }
        
        // Process each detected item
        for (item in detectedItems) {
            val label = item.label.lowercase()
            val confidence = item.confidence
            
            // Check each category for keyword matches
            for ((category, keywords) in categoryKeywords) {
                for (keyword in keywords) {
                    if (label.contains(keyword) || keyword.contains(label)) {
                        // Add weighted score based on confidence
                        categoryScores[category] = categoryScores[category]!! + confidence
                        break  // Move to next category once matched
                    }
                }
            }
        }
        
        // Find category with highest score
        var highestScore = 0f
        var bestCategory = "entertainment"  // Default
        
        for ((category, score) in categoryScores) {
            Log.d(TAG, "Category: $category, Score: $score")
            if (score > highestScore) {
                highestScore = score
                bestCategory = category
            }
        }
        
        Log.d(TAG, "Selected category: $bestCategory with score $highestScore")
        return bestCategory
    }
    
    /**
     * Returns a list of all supported categories
     */
    fun getSupportedCategories(): List<String> {
        return categoryKeywords.keys.toList()
    }
}"