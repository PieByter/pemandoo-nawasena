package com.nawasena.pemandoo.util

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "pemandoo_prefs"
    private const val KEY_TTS_LANGUAGE = "tts_language"
    private const val KEY_LAST_LANDMARK = "last_landmark_id"
    private const val KEY_BACKGROUND_PROMPTED = "background_location_prompted"

    const val LANGUAGE_INDONESIAN = "id-ID"
    const val LANGUAGE_ENGLISH = "en-US"

    fun getTtsLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TTS_LANGUAGE, LANGUAGE_INDONESIAN)
            ?: LANGUAGE_INDONESIAN
    }

    fun setTtsLanguage(context: Context, languageTag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TTS_LANGUAGE, languageTag)
            .apply()
    }

    fun languageDisplayName(languageTag: String): String = when (languageTag) {
        LANGUAGE_ENGLISH -> "English"
        else -> "Bahasa Indonesia"
    }

    fun getLastLandmarkId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_LANDMARK, null)
    }

    fun setLastLandmarkId(context: Context, landmarkId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_LANDMARK, landmarkId)
            .apply()
    }

    fun wasBackgroundLocationPrompted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BACKGROUND_PROMPTED, false)
    }

    fun markBackgroundLocationPrompted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BACKGROUND_PROMPTED, true)
            .apply()
    }
}
