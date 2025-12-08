package com.fyp.realtimetextdetection.kotlin

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fyp.realtimetextdetection.GraphicOverlay
import com.fyp.realtimetextdetection.R
import com.fyp.realtimetextdetection.kotlin.textdetector.TextRecognitionProcessor
import com.fyp.realtimetextdetection.preference.PreferenceUtils
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import java.util.Locale

class ImagePreviewActivity : AppCompatActivity() {

    private var imageView: ImageView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var progressBar: ProgressBar? = null
    private var statusText: TextView? = null
    private var btnReadText: MaterialButton? = null
    private var btnStopSpeaking: MaterialButton? = null
    private var translationStatusCard: MaterialCardView? = null
    private var translationStatusText: TextView? = null

    private var textRecognizer: TextRecognizer? = null
    private var imageProcessor: TextRecognitionProcessor? = null
    private var currentBitmap: Bitmap? = null

    // Accessibility components
    private var textToSpeech: TextToSpeech? = null
    private var vibrator: Vibrator? = null
    private var isTTSReady = false

    // Translation status
    private var lastRecognizedText: Text? = null
    private var translationCompleteCount = 0
    private var translationTotalCount = 0

    companion object {
        private const val TAG = "ImagePreviewActivity"
        private const val EXTRA_IMAGE_URI = "extra_image_uri"
        private const val EXTRA_RECOGNITION_MODE = "extra_recognition_mode"

        const val RECOGNITION_LATIN = "latin"
        const val RECOGNITION_CHINESE = "chinese"
        const val RECOGNITION_DEVANAGARI = "devanagari"
        const val RECOGNITION_JAPANESE = "japanese"
        const val RECOGNITION_KOREAN = "korean"

        fun createIntent(
            context: Context,
            imageUri: Uri,
            recognitionMode: String = RECOGNITION_LATIN
        ): Intent {
            return Intent(context, ImagePreviewActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URI, imageUri.toString())
                putExtra(EXTRA_RECOGNITION_MODE, recognitionMode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        initializeViews()
        initializeAccessibilityFeatures()
        setupListeners()

        // Get image URI from intent
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val recognitionMode = intent.getStringExtra(EXTRA_RECOGNITION_MODE) ?: RECOGNITION_LATIN

        if (imageUriString == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)

        // Initialize text recognizer based on mode
        initializeTextRecognizer(recognitionMode)

        // Load and process image
        loadAndProcessImage(imageUri)
    }

    private fun initializeViews() {
        imageView = findViewById(R.id.image_view)
        graphicOverlay = findViewById(R.id.graphic_overlay)
        progressBar = findViewById(R.id.progress_bar)
        statusText = findViewById(R.id.status_text)
        btnReadText = findViewById(R.id.btn_read_text)
        btnStopSpeaking = findViewById(R.id.btn_stop_speaking)
        translationStatusCard = findViewById(R.id.translation_status)
        translationStatusText = findViewById(R.id.translation_status_text)
    }

    private fun initializeAccessibilityFeatures() {
        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTTSReady = true
                // Set language based on target translation language
                val targetLang = PreferenceUtils.getTargetLanguage(this)
                val locale = getLocaleForLanguageCode(targetLang)
                val result = textToSpeech?.setLanguage(locale)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS language not supported, falling back to default")
                    textToSpeech?.setLanguage(Locale.getDefault())
                }

                btnReadText?.isEnabled = true
                updateStatusText("Ready")
            } else {
                Log.e(TAG, "TTS initialization failed")
                btnReadText?.isEnabled = false
                updateStatusText("Voice unavailable")
            }
        }

        // Initialize Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun setupListeners() {
        // Read text button
        btnReadText?.setOnClickListener {
            provideHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            readAllTranslatedText()
        }

        // Stop speaking button
        btnStopSpeaking?.setOnClickListener {
            provideHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            textToSpeech?.stop()
            updateStatusText("Stopped")
        }

        // Touch interaction on overlay
        var lastTapTime = 0L
        graphicOverlay?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime < 300) {
                        // Double tap detected
                        provideHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        readAllTranslatedText()
                        lastTapTime = 0
                    } else {
                        lastTapTime = currentTime
                    }
                    true
                }
                else -> false
            }
        }

        graphicOverlay?.setOnLongClickListener {
            provideHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            announceDetectionStatus()
            true
        }
    }

    private fun getLocaleForLanguageCode(languageCode: String): Locale {
        return when (languageCode) {
            TranslateLanguage.ENGLISH -> Locale.ENGLISH
            TranslateLanguage.SPANISH -> Locale("es")
            TranslateLanguage.FRENCH -> Locale.FRENCH
            TranslateLanguage.GERMAN -> Locale.GERMAN
            TranslateLanguage.ITALIAN -> Locale.ITALIAN
            TranslateLanguage.CHINESE -> Locale.CHINESE
            TranslateLanguage.JAPANESE -> Locale.JAPANESE
            TranslateLanguage.KOREAN -> Locale.KOREAN
            TranslateLanguage.ARABIC -> Locale("ar")
            TranslateLanguage.HINDI -> Locale("hi")
            TranslateLanguage.URDU -> Locale("ur")
            else -> Locale.getDefault()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun initializeTextRecognizer(mode: String) {
        val options = when (mode) {
            RECOGNITION_CHINESE -> ChineseTextRecognizerOptions.Builder().build()
            RECOGNITION_DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
            RECOGNITION_JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
            RECOGNITION_KOREAN -> KoreanTextRecognizerOptions.Builder().build()
            else -> TextRecognizerOptions.Builder().build()
        }

        textRecognizer = TextRecognition.getClient(options)
        imageProcessor = TextRecognitionProcessor(this, options)
    }

    private fun loadAndProcessImage(imageUri: Uri) {
        showProgress(true)
        updateStatusText("Loading image...")

        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            bitmap = rotateImageIfRequired(bitmap, imageUri)
            currentBitmap = bitmap
            imageView?.setImageBitmap(currentBitmap)

            imageView?.post {
                updateGraphicOverlay()
            }

            processImage()

        } catch (e: IOException) {
            Log.e(TAG, "Error loading image", e)
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            showProgress(false)
            updateStatusText("Error loading image")
        }
    }

    private fun updateGraphicOverlay() {
        val bitmap = currentBitmap ?: return
        val imageView = imageView ?: return
        val graphicOverlay = graphicOverlay ?: return

        // Get the actual drawable dimensions in the ImageView
        val drawable = imageView.drawable ?: return

        val imageViewWidth = imageView.width
        val imageViewHeight = imageView.height
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // Calculate the scale factor based on ImageView's scaleType (usually CENTER_CROP or FIT_CENTER)
        val scaleX = imageViewWidth.toFloat() / bitmapWidth.toFloat()
        val scaleY = imageViewHeight.toFloat() / bitmapHeight.toFloat()

        // For FIT_CENTER (default), use the smaller scale to fit the entire image
        val scale = Math.min(scaleX, scaleY)

        val scaledWidth = (bitmapWidth * scale).toInt()
        val scaledHeight = (bitmapHeight * scale).toInt()

        // Set the graphic overlay to match the actual displayed image size
        graphicOverlay.setImageSourceInfo(
            bitmapWidth,
            bitmapHeight,
            false
        )

        // Adjust overlay size to match scaled image
        val layoutParams = graphicOverlay.layoutParams
        layoutParams.width = scaledWidth
        layoutParams.height = scaledHeight
        graphicOverlay.layoutParams = layoutParams
    }

    private fun rotateImageIfRequired(bitmap: Bitmap, imageUri: Uri): Bitmap {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val exif = androidx.exifinterface.media.ExifInterface(inputStream!!)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )

            return when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 ->
                    rotateBitmap(bitmap, 90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 ->
                    rotateBitmap(bitmap, 180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 ->
                    rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EXIF data", e)
            return bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        bitmap.recycle()
        return rotatedBitmap
    }

    private fun processImage() {
        currentBitmap?.let { bitmap ->
            updateStatusText("Detecting text...")
            val image = InputImage.fromBitmap(bitmap, 0)

            textRecognizer?.process(image)
                ?.addOnSuccessListener { text ->
                    showProgress(false)
                    displayTextResults(text)

                    if (text.textBlocks.isNotEmpty()) {
                        provideHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                        // Show translation status if enabled
                        if (imageProcessor?.isTranslationActive() == true) {
                            showTranslationStatus(true)
                            updateStatusText("Translating...")

                            // Check translation completion after a delay
                            Handler(Looper.getMainLooper()).postDelayed({
                                checkTranslationStatus()
                            }, 1500)
                        } else {
                            updateStatusText("Text detected")
                        }

                        // Auto-read if accessibility is enabled
                        if (isAccessibilityEnabled()) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                readAllTranslatedText()
                            }, 800)
                        }
                    }
                }
                ?.addOnFailureListener { e ->
                    showProgress(false)
                    Log.e(TAG, "Text recognition failed", e)
                    Toast.makeText(
                        this,
                        "Text recognition failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateStatusText("Recognition failed")
                    provideHapticFeedback(HapticFeedbackConstants.REJECT)
                }
        }
    }

    private fun checkTranslationStatus() {
        // Hide translation status indicator
        showTranslationStatus(false)
        updateStatusText("Ready to read")
    }

    private fun displayTextResults(text: Text) {
        graphicOverlay?.clear()
        lastRecognizedText = text

        if (text.textBlocks.isEmpty()) {
            Toast.makeText(this, "No text detected in image", Toast.LENGTH_SHORT).show()
            speakText(getString(R.string.no_text_detected))
            updateStatusText("No text detected")
            return
        }

        // Use the image processor to handle translation and display
        imageProcessor?.onSuccess(text, graphicOverlay!!)
        graphicOverlay?.clear()
        Handler(Looper.getMainLooper()).postDelayed({
            imageProcessor?.onSuccess(text, graphicOverlay!!)
        }, 1500)

        val blockCount = text.textBlocks.size
        Toast.makeText(
            this,
            "Detected $blockCount text block(s)",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun readAllTranslatedText() {
        val text = lastRecognizedText

        if (text == null || text.textBlocks.isEmpty()) {
            speakText(getString(R.string.no_text_detected))
            updateStatusText("No text to read")
            return
        }

        if (!isTTSReady) {
            Toast.makeText(this, getString(R.string.tts_not_ready), Toast.LENGTH_SHORT).show()
            return
        }

        updateStatusText("Reading text...")
        textToSpeech?.stop()

        val translatedTexts = mutableListOf<String>()
        val isTranslationEnabled = imageProcessor?.isTranslationActive() ?: false

        if (PreferenceUtils.shouldGroupRecognizedTextInBlocks(this)) {
            for (textBlock in text.textBlocks) {
                val displayText = if (isTranslationEnabled) {
                    imageProcessor?.getTranslationForText(textBlock.text) ?: textBlock.text
                } else {
                    textBlock.text
                }
                if (displayText.isNotBlank()) {
                    translatedTexts.add(displayText)
                }
            }
        } else {
            for (textBlock in text.textBlocks) {
                for (line in textBlock.lines) {
                    val displayText = if (isTranslationEnabled) {
                        imageProcessor?.getTranslationForText(line.text) ?: line.text
                    } else {
                        line.text
                    }
                    if (displayText.isNotBlank()) {
                        translatedTexts.add(displayText)
                    }
                }
            }
        }

        if (translatedTexts.isEmpty()) {
            speakText(getString(R.string.no_translation_available))
            updateStatusText("No text available")
            return
        }

        // Speak each text with pauses
        translatedTexts.forEachIndexed { index, translatedText ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val utteranceId = "translated_$index"
                textToSpeech?.speak(
                    translatedText,
                    if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                    null,
                    utteranceId
                )
            } else {
                @Suppress("DEPRECATION")
                textToSpeech?.speak(
                    translatedText,
                    if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                    null
                )
            }
        }

        // Set completion listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    runOnUiThread {
                        updateStatusText("Reading: ${translatedTexts.size} items")
                    }
                }

                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        updateStatusText("Reading complete")
                    }
                }

                override fun onError(utteranceId: String?) {
                    runOnUiThread {
                        updateStatusText("Reading error")
                    }
                }
            })
        }

        provideHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun announceDetectionStatus() {
        val text = lastRecognizedText
        if (text == null || text.textBlocks.isEmpty()) {
            speakText(getString(R.string.no_text_detected_status))
            return
        }

        val blockCount = text.textBlocks.size
        val totalLines = text.textBlocks.sumOf { it.lines.size }

        val message = getString(R.string.image_status, blockCount, totalLines)
        speakText(message)
        updateStatusText("$blockCount blocks, $totalLines lines")
    }

    private fun speakText(text: String) {
        if (!isTTSReady) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "status")
        } else {
            @Suppress("DEPRECATION")
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    private fun provideHapticFeedback(feedbackConstant: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (feedbackConstant) {
                HapticFeedbackConstants.LONG_PRESS ->
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                HapticFeedbackConstants.VIRTUAL_KEY ->
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                HapticFeedbackConstants.REJECT ->
                    VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
                else ->
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.isEnabled && am.isTouchExplorationEnabled
    }

    private fun updateStatusText(status: String) {
        statusText?.text = status
    }

    private fun showProgress(show: Boolean) {
        progressBar?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showTranslationStatus(show: Boolean) {
        translationStatusCard?.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        textRecognizer?.close()
        imageProcessor?.stop()
        currentBitmap?.recycle()
        currentBitmap = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    override fun onResume() {
        super.onResume()
        imageProcessor?.onPreferencesChanged()
        currentBitmap?.let {
            val image = InputImage.fromBitmap(it, 0)
            textRecognizer?.process(image)
                ?.addOnSuccessListener { text ->
                    displayTextResults(text)
                }
        }
    }

    override fun onPause() {
        super.onPause()
        textToSpeech?.stop()
    }
}