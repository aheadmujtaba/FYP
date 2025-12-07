package com.fyp.realtimetextdetection.kotlin

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fyp.realtimetextdetection.GraphicOverlay
import com.fyp.realtimetextdetection.R
import com.fyp.realtimetextdetection.kotlin.textdetector.TextRecognitionProcessor
import com.google.android.gms.tasks.Task
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

class ImagePreviewActivity : AppCompatActivity() {

    private var imageView: ImageView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var progressBar: ProgressBar? = null
    private var textRecognizer: TextRecognizer? = null
    private var imageProcessor: TextRecognitionProcessor? = null
    private var currentBitmap: Bitmap? = null

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

        imageView = findViewById(R.id.image_view)
        graphicOverlay = findViewById(R.id.graphic_overlay)
        progressBar = findViewById(R.id.progress_bar)

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

        try {
            // Load bitmap from URI
            val inputStream = contentResolver.openInputStream(imageUri)
            currentBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (currentBitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Display image
            imageView?.setImageBitmap(currentBitmap)

            // Update graphic overlay dimensions
            graphicOverlay?.setImageSourceInfo(
                currentBitmap!!.width,
                currentBitmap!!.height,
                false
            )

            // Process image for text recognition
            processImage()

        } catch (e: IOException) {
            Log.e(TAG, "Error loading image", e)
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            showProgress(false)
        }
    }

    private fun processImage() {
        currentBitmap?.let { bitmap ->
            val image = InputImage.fromBitmap(bitmap, 0)

            textRecognizer?.process(image)
                ?.addOnSuccessListener { text ->
                    showProgress(false)
                    displayTextResults(text)
                }
                ?.addOnFailureListener { e ->
                    showProgress(false)
                    Log.e(TAG, "Text recognition failed", e)
                    Toast.makeText(
                        this,
                        "Text recognition failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun displayTextResults(text: Text) {
        graphicOverlay?.clear()

        if (text.textBlocks.isEmpty()) {
            Toast.makeText(this, "No text detected in image", Toast.LENGTH_SHORT).show()
            return
        }

        // Use the image processor to handle translation and display
        imageProcessor?.onSuccess(text, graphicOverlay!!)

        Toast.makeText(
            this,
            "Detected ${text.textBlocks.size} text block(s)",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showProgress(show: Boolean) {
        progressBar?.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        textRecognizer?.close()
        imageProcessor?.stop()
        currentBitmap?.recycle()
        currentBitmap = null
    }

    override fun onResume() {
        super.onResume()
        // Refresh if preferences changed
        imageProcessor?.onPreferencesChanged()
        currentBitmap?.let {
            val image = InputImage.fromBitmap(it, 0)
            textRecognizer?.process(image)
                ?.addOnSuccessListener { text ->
                    displayTextResults(text)
                }
        }
    }
}