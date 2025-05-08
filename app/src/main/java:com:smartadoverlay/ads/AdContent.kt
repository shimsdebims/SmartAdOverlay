package com.smartadoverlay.ads

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents an advertisement with its bitmap and metadata
 */
data class AdContent(
    val bitmap: Bitmap,
    val resourceId: Int,
    val metadata: AdMetadata
)

/**
 * Contains metadata about an advertisement
 */
@Parcelize
data class AdMetadata(
    val category: String,
    val placementPreference: PlacementPreference,
    val displayDuration: Long = 15000,  // Default 15 seconds
    val transparency: Float = 0.2f      // Default 20% transparency
) : Parcelable