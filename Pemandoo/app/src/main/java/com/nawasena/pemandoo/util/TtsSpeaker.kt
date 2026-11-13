package com.nawasena.pemandoo.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Small wrapper around [TextToSpeech] with three improvements over the original
 * code: it waits for engine initialization, it handles long descriptions by
 * splitting them into utterances, and it reports the real speaking state.
 */
class TtsSpeaker(
    private val context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var initialized = false
    private var pendingText: String? = null
    private var lastRequestedText: String? = null
    private var lastUtteranceId: String? = null
    private var batchCounter = 0
    private var fallbackAttempted = false

    var isSpeaking: Boolean = false
        private set

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            initialized = false
            mainHandler.post { onError("Text-to-Speech tidak tersedia di perangkat ini") }
            return
        }

        val tts = engine ?: return
        applyLanguage(tts, AppPreferences.getTtsLanguage(context))
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                setSpeaking(true)
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == lastUtteranceId) {
                    setSpeaking(false)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                handleSynthesisError()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                handleSynthesisError()
            }
        })

        initialized = true
        fallbackAttempted = false
        pendingText?.let { text ->
            pendingText = null
            speak(text)
        }
    }

    fun setLanguage(languageTag: String) {
        AppPreferences.setTtsLanguage(context, languageTag)
        fallbackAttempted = false
        if (initialized) {
            engine?.let { applyLanguage(it, languageTag) }
        }
    }

    fun speak(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            setSpeaking(false)
            return
        }

        lastRequestedText = cleanText
        fallbackAttempted = false
        speakInternal(cleanText)
    }

    private fun speakInternal(cleanText: String) {
        if (!initialized) {
            pendingText = cleanText
            return
        }

        val tts = engine ?: return
        tts.stop()

        val maxLength = (TextToSpeech.getMaxSpeechInputLength() - 80).coerceAtLeast(500)
        val chunks = cleanText.chunked(maxLength)
        batchCounter++
        val batch = batchCounter
        lastUtteranceId = "pemandoo-$batch-${chunks.lastIndex}"

        chunks.forEachIndexed { index, chunk ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val result = tts.speak(chunk, queueMode, null, "pemandoo-$batch-$index")
            if (result == TextToSpeech.ERROR) {
                handleSynthesisError()
                return
            }
        }
        setSpeaking(true)
    }

    fun stop() {
        pendingText = null
        engine?.stop()
        setSpeaking(false)
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        initialized = false
        isSpeaking = false
    }

    private fun applyLanguage(tts: TextToSpeech, languageTag: String) {
        val requested = Locale.forLanguageTag(languageTag)
        val availability = tts.isLanguageAvailable(requested)
        if (availability == TextToSpeech.LANG_AVAILABLE ||
            availability == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            availability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        ) {
            tts.language = requested
            return
        }

        // Tidak semua perangkat memiliki voice Bahasa Indonesia. Gunakan
        // bahasa perangkat agar narasi tetap bisa diputar.
        val fallback = Locale.getDefault()
        val fallbackAvailability = tts.isLanguageAvailable(fallback)
        tts.language = if (fallbackAvailability >= TextToSpeech.LANG_AVAILABLE) {
            fallback
        } else {
            Locale.US
        }
    }

    /**
     * Some engines report the requested language as available but then need a
     * network voice download. Retry once with English (usually installed
     * on-device) so the tour does not end in silence when offline.
     */
    private fun handleSynthesisError() {
        mainHandler.post {
            val tts = engine
            val text = lastRequestedText
            if (!fallbackAttempted && tts != null && !text.isNullOrBlank()) {
                fallbackAttempted = true
                tts.language = Locale.US
                speakInternal(text)
                return@post
            }
            setSpeaking(false)
            onError("Gagal memutar tur audio")
        }
    }

    private fun setSpeaking(value: Boolean) {
        if (isSpeaking == value) return
        isSpeaking = value
        mainHandler.post { onSpeakingChanged(value) }
    }
}
