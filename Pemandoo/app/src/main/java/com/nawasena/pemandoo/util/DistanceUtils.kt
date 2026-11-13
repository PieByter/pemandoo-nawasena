package com.nawasena.pemandoo.util

import java.util.Locale

object DistanceUtils {

    fun formatDistance(meters: Float?): String {
        if (meters == null || meters.isNaN()) return "Lokasi belum tersedia"
        return if (meters < 1000f) {
            String.format(Locale.getDefault(), "%.0f m dari lokasimu", meters)
        } else {
            String.format(Locale.getDefault(), "%.1f km dari lokasimu", meters / 1000f)
        }
    }

    fun formatDistanceShort(meters: Float?): String {
        if (meters == null || meters.isNaN()) return "—"
        return if (meters < 1000f) {
            String.format(Locale.getDefault(), "%.0f m", meters)
        } else {
            String.format(Locale.getDefault(), "%.1f km", meters / 1000f)
        }
    }
}
