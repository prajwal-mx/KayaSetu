package com.kayasetu.asha.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Instant, 100% Offline Bilingual (English + Kannada) Voice Speaker for KayaSetu.
 *
 * How it works:
 * 1. Primary path (Instant Offline Bundled Neural Voice Clips in `assets/voice/`):
 *    - All 26 English (`en-IN`) and Kannada (`kn`) voice prompts, questions, and triage outcomes
 *      are pre-recorded neural voice clips bundled inside the APK (`app/src/main/assets/voice/`).
 *    - Every bundled clip has a 420ms hardware-DAC wake-up lead-in so phone speakers/Bluetooth
 *      amplifiers open before the first syllable begins — eliminating both the 2–3s TTS engine
 *      synthesis delay and any clipped first letters in English or Kannada.
 * 2. Secondary fallback path (Offline Android TextToSpeech with 350ms silent warm-up utterance):
 *    - Used only if a custom/unmapped string is passed.
 */
class KayaSetuVoiceSpeaker(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var isInitializing = false
    private var activeLangTag: String = ""
    private var pendingRequest: Pair<String, Boolean>? = null

    fun speak(rawText: String, isKn: Boolean) {
        if (rawText.isBlank()) return
        requestMediaAudioFocus()

        // 1. Try playing the pre-bundled offline neural voice recording from assets/voice/*.mp3
        val assetClipName = resolveBundledVoiceAsset(rawText, isKn)
        if (assetClipName != null && playBundledAsset(assetClipName)) {
            return
        }

        // 2. Fallback to local TextToSpeech if an unmapped custom string is requested
        val engine = tts
        if (engine == null || !isReady) {
            pendingRequest = rawText to isKn
            if (!isInitializing) {
                initEngine(preferGoogleEngine = true)
            }
            return
        }
        speakInternalTts(engine, rawText, isKn)
    }

    private fun playBundledAsset(assetFileName: String): Boolean {
        return try {
            stopCurrentAudio()
            val afd = appContext.assets.openFd("voice/$assetFileName")
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setVolume(1.0f, 1.0f)
            mp.setOnCompletionListener { player ->
                try {
                    player.release()
                } catch (_: Exception) {
                }
                if (mediaPlayer === player) {
                    mediaPlayer = null
                }
            }
            mp.setOnErrorListener { player, _, _ ->
                try {
                    player.release()
                } catch (_: Exception) {
                }
                if (mediaPlayer === player) {
                    mediaPlayer = null
                }
                true
            }
            mp.prepare()
            mp.start()
            mediaPlayer = mp
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun stopCurrentAudio() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (_: Exception) {
        }
        mediaPlayer = null

        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    private fun isGoogleTtsInstalled(): Boolean {
        return try {
            appContext.packageManager.getPackageInfo("com.google.android.tts", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun initEngine(preferGoogleEngine: Boolean) {
        if (isInitializing) return
        isInitializing = true
        isReady = false
        activeLangTag = ""

        val useGoogle = preferGoogleEngine && isGoogleTtsInstalled()
        val listener = TextToSpeech.OnInitListener { status ->
            mainHandler.post {
                isInitializing = false
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts
                    if (engine != null) {
                        configureEngineDefaults(engine)
                        isReady = true
                        pendingRequest?.let { (text, isKn) ->
                            pendingRequest = null
                            speakInternalTts(engine, text, isKn)
                        }
                    }
                } else if (useGoogle) {
                    try {
                        tts?.shutdown()
                    } catch (_: Exception) {
                    }
                    tts = null
                    initEngine(preferGoogleEngine = false)
                }
            }
        }

        tts = try {
            if (useGoogle) {
                TextToSpeech(appContext, listener, "com.google.android.tts")
            } else {
                TextToSpeech(appContext, listener)
            }
        } catch (_: Exception) {
            isInitializing = false
            null
        }
    }

    private fun configureEngineDefaults(engine: TextToSpeech) {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            engine.setAudioAttributes(attrs)
            engine.setSpeechRate(1.10f)
            engine.setPitch(1.0f)
        } catch (_: Exception) {
        }
    }

    private fun requestMediaAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun hasInstalledKannadaVoice(engine: TextToSpeech): Boolean {
        return try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val knVoice = voices.firstOrNull { v ->
                    v.locale.language.equals("kn", ignoreCase = true) &&
                        (v.features == null || !v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
                }
                if (knVoice != null) return true
            }
            val knIn = Locale.forLanguageTag("kn-IN")
            if (engine.isLanguageAvailable(knIn) >= TextToSpeech.LANG_AVAILABLE) return true

            val kn = Locale.forLanguageTag("kn")
            engine.isLanguageAvailable(kn) >= TextToSpeech.LANG_AVAILABLE
        } catch (_: Exception) {
            false
        }
    }

    private fun selectBestEnglishLocale(engine: TextToSpeech): Boolean {
        val candidates = listOf(
            Locale.forLanguageTag("en-IN"),
            Locale.US,
            Locale.UK,
            Locale.ENGLISH
        )
        for (loc in candidates) {
            try {
                val avail = engine.isLanguageAvailable(loc)
                if (avail >= TextToSpeech.LANG_AVAILABLE) {
                    val tag = loc.toLanguageTag()
                    if (activeLangTag != tag) {
                        val res = engine.setLanguage(loc)
                        if (res >= TextToSpeech.LANG_AVAILABLE) {
                            activeLangTag = tag
                            return true
                        }
                    } else {
                        return true
                    }
                }
            } catch (_: Exception) {
            }
        }
        return try {
            val defLoc = engine.defaultVoice?.locale ?: Locale.getDefault()
            engine.setLanguage(defLoc) >= TextToSpeech.LANG_AVAILABLE
        } catch (_: Exception) {
            false
        }
    }

    private fun selectBestKannadaLocale(engine: TextToSpeech): Boolean {
        if (!hasInstalledKannadaVoice(engine)) return false
        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val installedKnVoice = voices.firstOrNull { v ->
                    v.locale.language.equals("kn", ignoreCase = true) &&
                        (v.features == null || !v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
                }
                if (installedKnVoice != null) {
                    if (activeLangTag != "voice:${installedKnVoice.name}") {
                        val vRes = engine.setVoice(installedKnVoice)
                        if (vRes == TextToSpeech.SUCCESS) {
                            activeLangTag = "voice:${installedKnVoice.name}"
                            return true
                        }
                    } else {
                        return true
                    }
                }
            }
        } catch (_: Exception) {
        }

        val candidates = listOf(
            Locale.forLanguageTag("kn-IN"),
            Locale.forLanguageTag("kn")
        )
        for (loc in candidates) {
            try {
                val avail = engine.isLanguageAvailable(loc)
                if (avail >= TextToSpeech.LANG_AVAILABLE) {
                    val tag = loc.toLanguageTag()
                    if (activeLangTag != tag) {
                        val res = engine.setLanguage(loc)
                        if (res >= TextToSpeech.LANG_AVAILABLE) {
                            activeLangTag = tag
                            return true
                        }
                    } else {
                        return true
                    }
                }
            } catch (_: Exception) {
            }
        }
        return false
    }

    private fun speakInternalTts(engine: TextToSpeech, rawText: String, isKn: Boolean) {
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putString(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS, "true")
        }

        if (isKn) {
            val knSupported = selectBestKannadaLocale(engine)
            if (knSupported) {
                val cleanedKn = cleanTextForNaturalSpeech(rawText, isKn = true)
                val phoneticFallback = toPhoneticKannadaSpeech(rawText)
                val uttId = "kayasetu_kn_${System.currentTimeMillis()}"

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {}
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId == uttId) {
                            mainHandler.post {
                                speakPhoneticKannadaFallback(engine, phoneticFallback, params)
                            }
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (utteranceId == uttId) {
                            mainHandler.post {
                                speakPhoneticKannadaFallback(engine, phoneticFallback, params)
                            }
                        }
                    }
                })

                engine.setSpeechRate(1.05f)
                engine.setPitch(1.0f)
                // Warm up hardware DAC for 320ms before speaking so first syllable isn't clipped
                engine.playSilentUtterance(320L, TextToSpeech.QUEUE_FLUSH, "warmup_$uttId")
                val result = engine.speak(cleanedKn, TextToSpeech.QUEUE_ADD, params, uttId)
                if (result == TextToSpeech.ERROR) {
                    speakPhoneticKannadaFallback(engine, phoneticFallback, params)
                }
            } else {
                val phoneticKn = toPhoneticKannadaSpeech(rawText)
                speakPhoneticKannadaFallback(engine, phoneticKn, params)
            }
        } else {
            selectBestEnglishLocale(engine)
            engine.setSpeechRate(1.10f)
            engine.setPitch(1.0f)
            val cleanedEn = cleanTextForNaturalSpeech(rawText, isKn = false)
            val uttId = "kayasetu_en_${System.currentTimeMillis()}"
            engine.playSilentUtterance(320L, TextToSpeech.QUEUE_FLUSH, "warmup_$uttId")
            val result = engine.speak(cleanedEn, TextToSpeech.QUEUE_ADD, params, uttId)
            if (result == TextToSpeech.ERROR) {
                pendingRequest = rawText to false
                initEngine(preferGoogleEngine = true)
            }
        }
    }

    private fun speakPhoneticKannadaFallback(
        engine: TextToSpeech,
        phoneticText: String,
        params: Bundle
    ) {
        selectBestEnglishLocale(engine)
        engine.setSpeechRate(1.05f)
        engine.setPitch(1.0f)
        val uttId = "kayasetu_kn_phonetic_${System.currentTimeMillis()}"
        engine.playSilentUtterance(320L, TextToSpeech.QUEUE_FLUSH, "warmup_$uttId")
        val res = engine.speak(phoneticText, TextToSpeech.QUEUE_ADD, params, uttId)
        if (res == TextToSpeech.ERROR) {
            pendingRequest = phoneticText to false
            initEngine(preferGoogleEngine = true)
        }
    }

    fun shutdown() {
        stopCurrentAudio()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        isReady = false
    }

    companion object {
        /**
         * Resolves any app prompt, question, or triage result to its pre-recorded
         * offline neural voice clip in `assets/voice/`.
         */
        fun resolveBundledVoiceAsset(rawText: String, isKn: Boolean): String? {
            val t = rawText.trim()
            if (isKn) {
                return when {
                    t.contains("ಚರ್ಮದ ಆರೋಗ್ಯ ತಪಾಸಣೆ ಪ್ರಾರಂಭಿಸಲು") -> "home_kn.mp3"
                    t.contains("ಚರ್ಮದ ಗಾಯವನ್ನು ವೃತ್ತದ ಒಳಗೆ ಇರಿಸಿ") -> "photo_kn.mp3"
                    t.contains("ಚರ್ಮದ ಗಾಯ ದೇಹದ ಯಾವ ಭಾಗದಲ್ಲಿದೆ") -> "details_kn.mp3"
                    t.contains("ಗಾಯದಿಂದ ರಕ್ತ ಅಥವಾ ಕೀವು ಬರುತ್ತಿದೆಯೇ") -> "q1_kn.mp3"
                    t.contains("ಇದು ವೇಗವಾಗಿ ಬೆಳೆಯುತ್ತಿದೆಯೇ") -> "q2_kn.mp3"
                    t.contains("ಇದು ನೋವಿನಿಂದ ಕೂಡಿದೆಯೇ ಅಥವಾ ತೆರೆದ ಹುಣ್ಣಾಗಿದೆಯೇ") -> "q3_kn.mp3"
                    t.contains("ಒಂದು ತಿಂಗಳಿಗಿಂತ ಹೆಚ್ಚು ಕಾಲ ವಾಸಿಯಾಗದೆ ಉಳಿದಿದೆಯೇ") -> "q4_kn.mp3"
                    t.contains("ರೋಗಿಗೆ ಸಕ್ಕರೆ ಕಾಯಿಲೆ ಅಥವಾ ಮಧುಮೇಹ ಇದೆಯೇ") -> "q5_kn.mp3"
                    t.contains("ಜ್ವರ ಅಥವಾ ಗಂಟುಗಳ ಊತ ಇದೆಯೇ") -> "q6_kn.mp3"
                    t.contains("ತುಂಬಾ ತುರಿಕೆ ಇದೆಯೇ") -> "q7_kn.mp3"
                    t.contains("ತಕ್ಷಣ ತಜ್ಞ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ") -> "result_red_kn.mp3"
                    t.contains("ಪಿಎಚ್‌ಸಿ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ") ||
                        t.contains("ಪ್ರಾಥಮಿಕ ಆರೋಗ್ಯ ಕೇಂದ್ರದ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ") -> "result_amber_kn.mp3"
                    t.contains("ಸಾಮಾನ್ಯ ಆರೈಕೆ ಸಾಕು") -> "result_green_kn.mp3"
                    else -> null
                }
            } else {
                return when {
                    t.contains("Tap Start Skin Check below", ignoreCase = true) -> "home_en.mp3"
                    t.contains("Keep the skin spot inside the circle", ignoreCase = true) -> "photo_en.mp3"
                    t.contains("Select where the skin spot is located", ignoreCase = true) -> "details_en.mp3"
                    t.contains("Is it bleeding or oozing fluid", ignoreCase = true) -> "q1_en.mp3"
                    t.contains("Has it grown fast or changed shape", ignoreCase = true) -> "q2_en.mp3"
                    t.contains("Is it painful or an open sore", ignoreCase = true) -> "q3_en.mp3"
                    t.contains("Has it stayed for more than one month", ignoreCase = true) -> "q4_en.mp3"
                    t.contains("Does the patient have diabetes", ignoreCase = true) -> "q5_en.mp3"
                    t.contains("Is there fever or swelling", ignoreCase = true) -> "q6_en.mp3"
                    t.contains("Is it very itchy", ignoreCase = true) -> "q7_en.mp3"
                    t.contains("Visit Hospital Specialist", ignoreCase = true) -> "result_red_en.mp3"
                    t.contains("See PHC Doctor Soon", ignoreCase = true) ||
                        t.contains("Primary Health Centre doctor", ignoreCase = true) -> "result_amber_en.mp3"
                    t.contains("Normal Care", ignoreCase = true) ||
                        t.contains("Keep the skin clean and dry", ignoreCase = true) -> "result_green_en.mp3"
                    else -> null
                }
            }
        }

        fun cleanTextForNaturalSpeech(rawText: String, isKn: Boolean): String {
            var cleaned = rawText
                .replace(Regex("\\([^)]*\\)"), " ")
                .replace(Regex("\\[[^]]*]"), " ")
                .replace("/", if (isKn) " ಅಥವಾ " else " or ")
                .replace("—", ". ")
                .replace("–", " to ")
                .replace("·", ". ")
                .replace(">", if (isKn) "ಕ್ಕಿಂತ ಹೆಚ್ಚು " else "more than ")
                .replace("<", if (isKn) "ಕ್ಕಿಂತ ಕಡಿಮೆ " else "under ")

            if (isKn) {
                cleaned = cleaned
                    .replace("PHC", "ಪ್ರಾಥಮಿಕ ಆರೋಗ್ಯ ಕೇಂದ್ರ")
                    .replace("1", "ಒಂದು")
                    .replace("2", "ಎರಡು")
                    .replace("4", "ನಾಲ್ಕು")
                    .replace("7", "ಏಳು")
                    .replace("48", "ನಲವತ್ತೆಂಟು")
            } else {
                cleaned = cleaned
                    .replace(Regex("\\bPHC\\b"), "Primary Health Centre")
                    .replace(Regex("\\bhrs\\b", RegexOption.IGNORE_CASE), "hours")
                    .replace(Regex("\\byrs\\b", RegexOption.IGNORE_CASE), "years")
            }

            return cleaned.replace(Regex("\\s+"), " ").trim()
        }

        fun toPhoneticKannadaSpeech(rawText: String): String {
            val trimmed = rawText.trim()

            when {
                trimmed.contains("ಚರ್ಮದ ಆರೋಗ್ಯ ತಪಾಸಣೆ ಪ್ರಾರಂಭಿಸಲು") ->
                    return "Charmada aarogya thapaasane praarambhisalu kelagina button otthiri. Photo thegeyiri, matthu eelu sarala prashnegalige uttharisiri."
                trimmed.contains("ಚರ್ಮದ ಗಾಯವನ್ನು ವೃತ್ತದ ಒಳಗೆ ಇರಿಸಿ") ->
                    return "Charmada gaayavannu vrutthada olage irisi, matthu spashtavaada photo thegeyalu camera button otthiri."
                trimmed.contains("ಚರ್ಮದ ಗಾಯ ದೇಹದ ಯಾವ ಭಾಗದಲ್ಲಿದೆ") ->
                    return "Charmada gaaya deehada yaava bhaagadallide, matthu rogiya vayassina gumpu yaavudu endu aayke maadi."
                trimmed.contains("ಗಾಯದಿಂದ ರಕ್ತ ಅಥವಾ ಕೀವು ಬರುತ್ತಿದೆಯೇ") ->
                    return "Gaayadinda raktha athava keevu barutthideye? Thaanaagiye raktha athava drava sorutthideye endu gamanisi."
                trimmed.contains("ಇದು ವೇಗವಾಗಿ ಬೆಳೆಯುತ್ತಿದೆಯೇ") ->
                    return "Idu vegavaagi beleyutthideye? Gaayada anchugalu haradutthiveye athava banna badalaagutthideye endu keli."
                trimmed.contains("ಇದು ನೋವಿನಿಂದ ಕೂಡಿದೆಯೇ ಅಥವಾ ತೆರೆದ ಹುಣ್ಣಾಗಿದೆಯೇ") ->
                    return "Idu novininda koodideye athava thereda hunnaagideye? Muttidare novu ideye endu keli."
                trimmed.contains("ಒಂದು ತಿಂಗಳಿಗಿಂತ ಹೆಚ್ಚು ಕಾಲ ವಾಸಿಯಾಗದೆ ಉಳಿದಿದೆಯೇ") ->
                    return "Ee gaayavu ondu thingaligintha hecchu kaala vaasiyaagade ulidideye? Naalku vaaragaligintha hecchu kaala ideye endu keli."
                trimmed.contains("ರೋಗಿಗೆ ಸಕ್ಕರೆ ಕಾಯಿಲೆ ಅಥವಾ ಮಧುಮೇಹ ಇದೆಯೇ") ->
                    return "Rogige sakkare kaayile athava madhumeha ideye?"
                trimmed.contains("ಜ್ವರ ಅಥವಾ ಗಂಟುಗಳ ಊತ ಇದೆಯೇ") ->
                    return "Rogige jvara athava gantugala ootha ideye? Mai bisi athava hatthiradalli ootha ideye endu gamanisi."
                trimmed.contains("ತುಂಬಾ ತುರಿಕೆ ಇದೆಯೇ") ->
                    return "Charmadalli thumbaa thurike ideye? Hulukaddiyanthe melmai thurike ideye endu keli."
                trimmed.contains("ತಕ್ಷಣ ತಜ್ಞ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ") ->
                    return "Thakshana thajna vaidyarge thorisi. Ee charmada gaayakke thakshana vaidya-keeya pareeksheya agathyavide. Dayavittu hatthirada jillaa aaspathreya charmaroga thajnarge kaluhisi."
                trimmed.contains("ಪಿಎಚ್‌ಸಿ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ") || trimmed.contains("ಪ್ರಾಥಮಿಕ ಆರೋಗ್ಯ ಕೇಂದ್ರದ ವೈದ್ಯರಿಗೆ ತೋರಿಸಿ") ->
                    return "P H C vaidyarge thorisi. Idannu eradu dinagala olagaagi praathamika aarogya kendrada vaidyarge thorisi pareekshisi."
                trimmed.contains("ಸಾಮಾನ್ಯ ಆರೈಕೆ ಸಾಕು") ->
                    return "Saamaanya aaraike saaku. Charmavannu svacchavaagi matthu onagidanthe irisi. Eelu dinagalalli vaasiyaagadiddare matthe pareekshisi."
            }

            return transliterateKannadaUnicode(trimmed)
        }

        private fun transliterateKannadaUnicode(text: String): String {
            val independentVowels = mapOf(
                'ಅ' to "a", 'ಆ' to "aa", 'ಇ' to "i", 'ಈ' to "ee",
                'ಉ' to "u", 'ಊ' to "oo", 'ಋ' to "ru", 'ಎ' to "e",
                'ಏ' to "ae", 'ಐ' to "ai", 'ಒ' to "o", 'ಓ' to "oh", 'ಔ' to "au"
            )
            val consonants = mapOf(
                'ಕ' to "k", 'ಖ' to "kh", 'ಗ' to "g", 'ಘ' to "gh", 'ಙ' to "ng",
                'ಚ' to "ch", 'ಛ' to "chh", 'ಜ' to "j", 'ಝ' to "jh", 'ಞ' to "ny",
                'ಟ' to "t", 'ಠ' to "th", 'ಡ' to "d", 'ಢ' to "dh", 'ಣ' to "n",
                'ತ' to "th", 'ಥ' to "thh", 'ದ' to "d", 'ಧ' to "dh", 'ನ' to "n",
                'ಪ' to "p", 'ಫ' to "ph", 'ಬ' to "b", 'ಭ' to "bh", 'ಮ' to "m",
                'ಯ' to "y", 'ರ' to "r", 'ಲ' to "l", 'ವ' to "v", 'ಶ' to "sh",
                'ಷ' to "sh", 'ಸ' to "s", 'ಹ' to "h", 'ಳ' to "l"
            )
            val vowelSigns = mapOf(
                'ಾ' to "aa", 'ಿ' to "i", 'ೀ' to "ee", 'ು' to "u", 'ೂ' to "oo",
                'ೃ' to "ru", 'ೆ' to "e", 'ೇ' to "ae", 'ೈ' to "ai",
                'ೊ' to "o", 'ೋ' to "oh", 'ೌ' to "au"
            )
            val virama = '್'

            val sb = StringBuilder()
            var i = 0
            while (i < text.length) {
                val ch = text[i]
                when {
                    independentVowels.containsKey(ch) -> {
                        sb.append(independentVowels[ch])
                    }
                    consonants.containsKey(ch) -> {
                        val base = consonants[ch] ?: ""
                        val next = if (i + 1 < text.length) text[i + 1] else '\u0000'
                        if (next == virama) {
                            sb.append(base)
                            i++
                        } else if (vowelSigns.containsKey(next)) {
                            sb.append(base).append(vowelSigns[next])
                            i++
                        } else {
                            sb.append(base).append("a")
                        }
                    }
                    ch == 'ಂ' -> sb.append("m")
                    ch == 'ಃ' -> sb.append("h")
                    ch == '\u200C' || ch == '\u200D' -> Unit
                    else -> sb.append(ch)
                }
                i++
            }
            return sb.toString().replace(Regex("\\s+"), " ").trim()
        }
    }
}
