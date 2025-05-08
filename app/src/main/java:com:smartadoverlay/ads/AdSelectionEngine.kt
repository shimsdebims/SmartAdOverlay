package com.smartadoverlay.ads

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.smartadoverlay.R
import java.util.Random

/**
 * Selects appropriate advertisements based on content classification.
 */
class AdSelectionEngine(private val context: Context) {
    
    private val TAG = "AdSelectionEngine"
    
    // Maps category to list of ad resources
    private val categoryAds = mapOf(
        "sports" to listOf(R.drawable.ad_sports),
        "entertainment" to listOf(R.drawable.ad_entertainment),
        "cooking" to listOf(R.drawable.ad_cooking),
        "music" to listOf(R.drawable.ad_music)
    )
    
    // Keep track of recently shown ads to avoid repetition
    private val recentlyShownAds = mutableListOf<Int>()
    private val random = Random()
    
    /**
     * Selects an appropriate ad based on the content category.
     * Returns ad content with metadata.
     */
    fun selectAd(category: String): AdContent {
        Log.d(TAG, "Selecting ad for category: $category")
        
        // Get ads for this category or default to entertainment
        val availableAds = categoryAds[category] ?: categoryAds["entertainment"]!!
        
        // Filter out recently shown ads if possible
        val filteredAds = if (availableAds.size > recentlyShownAds.size) {
            availableAds.filter { it !in recentlyShownAds }
        } else {
            availableAds
        }
        
        // Select a random ad from the filtered list
        val selectedAdResource = if (filteredAds.isNotEmpty()) {
            filteredAds[random.nextInt(filteredAds.size)]
        } else {
            availableAds[random.nextInt(availableAds.size)]
        }
        
        // Update recently shown ads
        recentlyShownAds.add(selectedAdResource)
        if (recentlyShownAds.size > 5) {
            recentlyShownAds.removeAt(0)
        }
        
        // Load the ad bitmap
        val adBitmap = loadAdBitmap(selectedAdResource)
        
        // Create ad metadata
        val adMeta = AdMetadata(
            category = category,
            placementPreference = getPlacementPreference(category),
            displayDuration = getDisplayDuration(category),
            transparency = getTransparency(category)
        )
        
        return AdContent(
            bitmap = adBitmap,
            resourceId = selectedAdResource,
            metadata = adMeta
        )
    }
    
    /**
     * Loads an ad bitmap from resources
     */
    private fun loadAdBitmap(resourceId: Int): Bitmap {
        return try {
            BitmapFactory.decodeResource(context.resources, resourceId)
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Failed to load ad resource: $resourceId")
            // Return a simple colored bitmap as fallback
            Bitmap.createBitmap(300, 150, Bitmap.Config.ARGB_8888)
        }
    }
    
    /**
     * Determines preferred placement for different categories
     */
    private fun getPlacementPreference(category: String): PlacementPreference {
        return when (category) {
            "sports" -> PlacementPreference.BOTTOM_RIGHT  // Bottom right for sports
            "entertainment" -> PlacementPreference.TOP_RIGHT  // Top right for entertainment
            "cooking" -> PlacementPreference.BOTTOM_LEFT  // Bottom left for cooking
            "music" -> PlacementPreference.TOP_LEFT  // Top left for music
            else -> PlacementPreference.BOTTOM_RIGHT  // Default
        }
    }
    
    /**
     * Determines display duration for different categories
     */
    private fun getDisplayDuration(category: String): Long {
        return when (category) {
            "sports" -> 15000L  // 15 seconds for sports
            "entertainment" -> 20000L  // 20 seconds for entertainment
            "cooking" -> 25000L  // 25 seconds for cooking
            "music" -> 18000L  // 18 seconds for music
            else -> 15000L  // Default 15 seconds
        }
    }
    
    /**
     * Determines transparency level for different categories
     */
    private fun getTransparency(category: String): Float {
        return when (category) {
            "sports" -> 0.15f  // More transparent for fast-moving content
            "entertainment" -> 0.2f
            "cooking" -> 0.25f
            "music" -> 0.15f
            else -> 0.2f  // Default 20% transparency
        }
    }
    
    // For testing purposes only
    fun getTestAd(): AdContent {
        return selectAd("entertainment")
    }
}

/**
 * Enum representing preferred placement positions for ads
 */
enum class PlacementPreference {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}