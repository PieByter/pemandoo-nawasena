package com.nawasena.pemandoo.data

import android.location.Location
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A tourist landmark that can be shown on the map and played as an audio tour.
 *
 * Landmarks are stored locally so the app keeps working even when there is no
 * internet connection. [isUserCreated] distinguishes the bundled landmarks from
 * places added by the user from the "Tambah" screen.
 */
@Entity(tableName = "landmarks")
data class Landmark(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val photo: String = "",
    val overview: String = "",
    val architecturalFeatures: String = "",
    val interestingFacts: String = "",
    val historicalSignificance: String = "",
    val hours: String = "",
    val entryFee: String = "",
    val radiusMeters: Float = DEFAULT_RADIUS_METERS,
    val isUserCreated: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val hasCoordinates: Boolean
        get() = latitude in -90.0..90.0 && longitude in -180.0..180.0

    fun distanceTo(latitude: Double, longitude: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(this.latitude, this.longitude, latitude, longitude, result)
        return result[0]
    }

    fun distanceTo(location: Location?): Float? =
        location?.let { distanceTo(it.latitude, it.longitude) }

    /**
     * Text used by the text-to-speech engine. Sections are separated with blank
     * lines so the narration sounds natural.
     */
    fun narrationText(): String = buildString {
        append(overview.trim())
        if (architecturalFeatures.isNotBlank()) {
            append("\n\n")
            append(architecturalFeatures.trim())
        }
        if (interestingFacts.isNotBlank()) {
            append("\n\n")
            append(interestingFacts.trim())
        }
        if (historicalSignificance.isNotBlank()) {
            append("\n\n")
            append(historicalSignificance.trim())
        }
        if (hours.isNotBlank() || entryFee.isNotBlank()) {
            append("\n\nInformasi kunjungan. ")
            if (hours.isNotBlank()) append("Jam buka ${hours.trim()}. ")
            if (entryFee.isNotBlank()) append("Tiket masuk ${entryFee.trim()}.")
        }
    }.trim()

    companion object {
        const val DEFAULT_RADIUS_METERS = 120f
    }
}

/** A landmark paired with the distance from the user (in meters). */
data class NearbyLandmark(
    val landmark: Landmark,
    val distanceMeters: Float? = null
)
