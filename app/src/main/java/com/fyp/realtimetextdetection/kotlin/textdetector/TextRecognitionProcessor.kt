/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fyp.realtimetextdetection.kotlin.textdetector

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.fyp.realtimetextdetection.BuildConfig
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.fyp.realtimetextdetection.GraphicOverlay
import com.fyp.realtimetextdetection.kotlin.VisionProcessorBase
import com.fyp.realtimetextdetection.preference.PreferenceUtils
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import java.util.concurrent.ConcurrentHashMap

/** Processor for the text detector demo. */
/*
class TextRecognitionProcessor(
  private val context: Context,
  textRecognizerOptions: TextRecognizerOptionsInterface
) : VisionProcessorBase<Text>(context) {
  private val textRecognizer: TextRecognizer = TextRecognition.getClient(textRecognizerOptions)
  private val shouldGroupRecognizedTextInBlocks: Boolean =
    PreferenceUtils.shouldGroupRecognizedTextInBlocks(context)
  private val showLanguageTag: Boolean = PreferenceUtils.showLanguageTag(context)
  private val showConfidence: Boolean = PreferenceUtils.shouldShowTextConfidence(context)

  override fun stop() {
    super.stop()
    textRecognizer.close()
  }

  override fun detectInImage(image: InputImage): Task<Text> {
    return textRecognizer.process(image)
  }

  override fun onSuccess(text: Text, graphicOverlay: GraphicOverlay) {
    Log.d(TAG, "On-device Text detection successful")
    logExtrasForTesting(text)
    graphicOverlay.add(
      TextGraphic(
        graphicOverlay,
        text,
        shouldGroupRecognizedTextInBlocks,
        showLanguageTag,
        showConfidence
      )
    )
  }

  override fun onFailure(e: Exception) {
    Log.w(TAG, "Text detection failed.$e")
  }

  companion object {
    private const val TAG = "TextRecProcessor"
    private fun logExtrasForTesting(text: Text?) {
      if (text != null) {
        Log.v(MANUAL_TESTING_LOG, "Detected text has : " + text.textBlocks.size + " blocks")
        for (i in text.textBlocks.indices) {
          val lines = text.textBlocks[i].lines
          Log.v(
            MANUAL_TESTING_LOG,
            String.format("Detected text block %d has %d lines", i, lines.size)
          )
          for (j in lines.indices) {
            val elements = lines[j].elements
            Log.v(
              MANUAL_TESTING_LOG,
              String.format("Detected text line %d has %d elements", j, elements.size)
            )
            for (k in elements.indices) {
              val element = elements[k]
              Log.v(
                MANUAL_TESTING_LOG,
                String.format("Detected text element %d says: %s", k, element.text)
              )
              Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                  "Detected text element %d has a bounding box: %s",
                  k,
                  element.boundingBox!!.flattenToString()
                )
              )
              Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                  "Expected corner point size is 4, get %d",
                  element.cornerPoints!!.size
                )
              )
              for (point in element.cornerPoints!!) {
                Log.v(
                  MANUAL_TESTING_LOG,
                  String.format(
                    "Corner point for element %d is located at: x - %d, y = %d",
                    k,
                    point.x,
                    point.y
                  )
                )
              }
            }
          }
        }
      }
    }
  }
}
*/


/*
class TextRecognitionProcessor(
  private val context: Context,
  textRecognizerOptions: TextRecognizerOptionsInterface
) : VisionProcessorBase<Text>(context) {

  private val textRecognizer: TextRecognizer = TextRecognition.getClient(textRecognizerOptions)

  // Cache preferences to avoid repeated SharedPreferences lookups
  private var shouldGroupRecognizedTextInBlocks: Boolean = false
  private var showLanguageTag: Boolean = false
  private var showConfidence: Boolean = false

  // Reuse TextGraphic instance instead of creating new one each frame
  private var currentTextGraphic: TextGraphic? = null

  // Track if preferences have changed
  private var preferencesVersion = 0

  init {
    updatePreferences()
  }

  private fun updatePreferences() {
    shouldGroupRecognizedTextInBlocks = PreferenceUtils.shouldGroupRecognizedTextInBlocks(context)
    showLanguageTag = PreferenceUtils.showLanguageTag(context)
    showConfidence = PreferenceUtils.shouldShowTextConfidence(context)
  }

  override fun stop() {
    super.stop()
    textRecognizer.close()
    currentTextGraphic = null
  }

  override fun detectInImage(image: InputImage): Task<Text> {
    return textRecognizer.process(image)
  }

  override fun onSuccess(text: Text, graphicOverlay: GraphicOverlay) {
    // Remove all logging in production - MASSIVE performance boost
    if (BuildConfig.DEBUG && Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "On-device Text detection successful")
    }

    // Only log for manual testing if explicitly enabled
    if (BuildConfig.DEBUG && Log.isLoggable(MANUAL_TESTING_LOG, Log.VERBOSE)) {
      logExtrasForTesting(text)
    }

    // Reuse existing TextGraphic if preferences haven't changed
    val graphic = currentTextGraphic
    if (graphic != null &&
      graphic.shouldGroupTextInBlocks == shouldGroupRecognizedTextInBlocks &&
      graphic.showLanguageTag == showLanguageTag &&
      graphic.showConfidence == showConfidence) {
      // Update with new text data and invalidate display list
      graphic.updateText(text)
      graphicOverlay.add(graphic)
    } else {
      // Create new graphic only when preferences change
      currentTextGraphic = TextGraphic(
        graphicOverlay,
        text,
        shouldGroupRecognizedTextInBlocks,
        showLanguageTag,
        showConfidence
      )
      graphicOverlay.add(currentTextGraphic!!)
    }
  }

  override fun onFailure(e: Exception) {
    if (BuildConfig.DEBUG) {
      Log.w(TAG, "Text detection failed.$e")
    }
  }

  // Call this if user changes preferences
  fun onPreferencesChanged() {
    updatePreferences()
    preferencesVersion++
    currentTextGraphic = null // Force recreation with new preferences
  }

  companion object {
    private const val TAG = "TextRecProcessor"

    // Optimized logging - only when needed
    private fun logExtrasForTesting(text: Text?) {
      if (text == null) return

      // Use StringBuilder for efficient string concatenation
      val sb = StringBuilder()
      sb.append("Detected text has : ").append(text.textBlocks.size).append(" blocks")
      Log.v(MANUAL_TESTING_LOG, sb.toString())

      // Early exit if no blocks
      if (text.textBlocks.isEmpty()) return

      for (i in text.textBlocks.indices) {
        val lines = text.textBlocks[i].lines
        Log.v(MANUAL_TESTING_LOG, "Detected text block $i has ${lines.size} lines")

        for (j in lines.indices) {
          val elements = lines[j].elements
          Log.v(MANUAL_TESTING_LOG, "Detected text line $j has ${elements.size} elements")

          for (k in elements.indices) {
            val element = elements[k]
            Log.v(MANUAL_TESTING_LOG, "Detected text element $k says: ${element.text}")

            element.boundingBox?.let { bbox ->
              Log.v(MANUAL_TESTING_LOG, "Detected text element $k has a bounding box: ${bbox.flattenToString()}")
            }

            element.cornerPoints?.let { points ->
              Log.v(MANUAL_TESTING_LOG, "Expected corner point size is 4, get ${points.size}")
              for (point in points) {
                Log.v(MANUAL_TESTING_LOG, "Corner point for element $k is located at: x=${point.x}, y=${point.y}")
              }
            }
          }
        }
      }
    }
  }
}
*/
/*

class TextRecognitionProcessor(
  private val context: Context,
  textRecognizerOptions: TextRecognizerOptionsInterface
) : VisionProcessorBase<Text>(context) {

  private val textRecognizer: TextRecognizer = TextRecognition.getClient(textRecognizerOptions)

  // Translation components
  private var translator: Translator? = null
  private val translatorOptions: TranslatorOptions
  private var isTranslationEnabled: Boolean = false
  private var targetLanguage: String = TranslateLanguage.ENGLISH

  // Translation cache to avoid re-translating same text
  private val translationCache = LruCache<String, String>(100)

  // Pending translations tracker
  private val pendingTranslations = ConcurrentHashMap<String, Boolean>()

  // Cache preferences
  private var shouldGroupRecognizedTextInBlocks: Boolean = false
  private var showLanguageTag: Boolean = false
  private var showConfidence: Boolean = false

  // Reuse TextGraphic instance
  private var currentTextGraphic: TextGraphic? = null

  init {
    updatePreferences()

    // Initialize translator with default options (can be changed later)
    translatorOptions = TranslatorOptions.Builder()
      .setSourceLanguage(TranslateLanguage.ENGLISH) // Auto-detect in practice
      .setTargetLanguage(targetLanguage)
      .build()

    isTranslationEnabled = PreferenceUtils.isTranslationEnabled(context)
    if (isTranslationEnabled) {
      initializeTranslator()
    }
  }

  private fun updatePreferences() {
    shouldGroupRecognizedTextInBlocks = PreferenceUtils.shouldGroupRecognizedTextInBlocks(context)
    showLanguageTag = PreferenceUtils.showLanguageTag(context)
    showConfidence = PreferenceUtils.shouldShowTextConfidence(context)
  }

  private fun initializeTranslator() {
    val sourceLanguage = PreferenceUtils.getSourceLanguage(context) ?: TranslateLanguage.ENGLISH
    targetLanguage = PreferenceUtils.getTargetLanguage(context) ?: TranslateLanguage.ENGLISH

    val options = TranslatorOptions.Builder()
      .setSourceLanguage(sourceLanguage)
      .setTargetLanguage(targetLanguage)
      .build()

    translator?.close()
    translator = Translation.getClient(options)

    // Download model if needed (async, non-blocking)
    val conditions = DownloadConditions.Builder()
      .requireWifi()
      .build()

    translator?.downloadModelIfNeeded(conditions)
      ?.addOnSuccessListener {
        if (BuildConfig.DEBUG) {
          Log.d(TAG, "Translation model downloaded successfully")
        }
      }
      ?.addOnFailureListener { e ->
        if (BuildConfig.DEBUG) {
          Log.e(TAG, "Failed to download translation model", e)
        }
      }
  }

  override fun stop() {
    super.stop()
    textRecognizer.close()
    translator?.close()
    currentTextGraphic = null
    translationCache.evictAll()
    pendingTranslations.clear()
  }

  override fun detectInImage(image: InputImage): Task<Text> {
    return textRecognizer.process(image)
  }

  override fun onSuccess(text: Text, graphicOverlay: GraphicOverlay) {
    if (BuildConfig.DEBUG && Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "On-device Text detection successful")
    }

    if (BuildConfig.DEBUG && Log.isLoggable(MANUAL_TESTING_LOG, Log.VERBOSE)) {
      logExtrasForTesting(text)
    }

    // Check if translation is enabled
    if (isTranslationEnabled && translator != null) {
      // Translate text asynchronously
      translateAndDisplay(text, graphicOverlay)
    } else {
      // Display original text without translation
      displayText(text, graphicOverlay, null)
    }
  }

  private fun translateAndDisplay(text: Text, graphicOverlay: GraphicOverlay) {
    val translationMap = mutableMapOf<String, String>()
    val translationTasks = mutableListOf<Task<String>>()

    // Collect all text items to translate
    val textsToTranslate = mutableListOf<Pair<String, String>>() // (original, key)

    if (shouldGroupRecognizedTextInBlocks) {
      for (textBlock in text.textBlocks) {
        val originalText = textBlock.text
        if (originalText.isNotBlank()) {
          textsToTranslate.add(originalText to "block_${textBlock.text.hashCode()}")
        }
      }
    } else {
      for (textBlock in text.textBlocks) {
        for (line in textBlock.lines) {
          val originalText = line.text
          if (originalText.isNotBlank()) {
            textsToTranslate.add(originalText to "line_${line.text.hashCode()}")
          }
        }
      }
    }

    // Translate each text item
    for ((originalText, key) in textsToTranslate) {
      // Check cache first
      val cached = translationCache.get(originalText)
      if (cached != null) {
        translationMap[originalText] = cached
        continue
      }

      // Check if already translating
      if (pendingTranslations.containsKey(originalText)) {
        continue
      }

      // Mark as pending
      pendingTranslations[originalText] = true

      // Translate
      val task = translator!!.translate(originalText)
        .addOnSuccessListener { translated ->
          translationCache.put(originalText, translated)
          translationMap[originalText] = translated
          pendingTranslations.remove(originalText)
        }
        .addOnFailureListener { e ->
          if (BuildConfig.DEBUG) {
            Log.e(TAG, "Translation failed for: $originalText", e)
          }
          pendingTranslations.remove(originalText)
          // Use original text as fallback
          translationMap[originalText] = originalText
        }

      translationTasks.add(task)
    }

    // Wait for all translations to complete (with timeout)
    if (translationTasks.isNotEmpty()) {
      Tasks.whenAllComplete(translationTasks)
        .addOnCompleteListener {
          // Display with translations
          displayText(text, graphicOverlay, translationMap)
        }
    } else {
      // All were cached, display immediately
      displayText(text, graphicOverlay, translationMap)
    }
  }

  private fun displayText(
    text: Text,
    graphicOverlay: GraphicOverlay,
    translationMap: Map<String, String>?
  ) {
    // Reuse or create TextGraphic
    val graphic = currentTextGraphic
    if (graphic != null &&
      graphic.shouldGroupTextInBlocks == shouldGroupRecognizedTextInBlocks &&
      graphic.showLanguageTag == showLanguageTag &&
      graphic.showConfidence == showConfidence) {
      graphic.updateText(text, translationMap)
      graphicOverlay.add(graphic)
    } else {
      currentTextGraphic = TextGraphic(
        graphicOverlay,
        text,
        shouldGroupRecognizedTextInBlocks,
        showLanguageTag,
        showConfidence,
        translationMap
      )
      graphicOverlay.add(currentTextGraphic!!)
    }
  }

  override fun onFailure(e: Exception) {
    if (BuildConfig.DEBUG) {
      Log.w(TAG, "Text detection failed.$e")
    }
  }

  fun onPreferencesChanged() {
    updatePreferences()
    currentTextGraphic = null

    // Update translation settings
    val newTranslationEnabled = PreferenceUtils.isTranslationEnabled(context)
    val newTargetLanguage = PreferenceUtils.getTargetLanguage(context) ?: TranslateLanguage.ENGLISH

    if (newTranslationEnabled != isTranslationEnabled || newTargetLanguage != targetLanguage) {
      isTranslationEnabled = newTranslationEnabled
      targetLanguage = newTargetLanguage
      translationCache.evictAll()

      if (isTranslationEnabled) {
        initializeTranslator()
      } else {
        translator?.close()
        translator = null
      }
    }
  }

  companion object {
    private const val TAG = "TextRecProcessor"

    private fun logExtrasForTesting(text: Text?) {
      if (text == null) return

      val sb = StringBuilder()
      sb.append("Detected text has : ").append(text.textBlocks.size).append(" blocks")
      Log.v(MANUAL_TESTING_LOG, sb.toString())

      if (text.textBlocks.isEmpty()) return

      for (i in text.textBlocks.indices) {
        val lines = text.textBlocks[i].lines
        Log.v(MANUAL_TESTING_LOG, "Detected text block $i has ${lines.size} lines")

        for (j in lines.indices) {
          val elements = lines[j].elements
          Log.v(MANUAL_TESTING_LOG, "Detected text line $j has ${elements.size} elements")

          for (k in elements.indices) {
            val element = elements[k]
            Log.v(MANUAL_TESTING_LOG, "Detected text element $k says: ${element.text}")

            element.boundingBox?.let { bbox ->
              Log.v(MANUAL_TESTING_LOG, "Detected text element $k has a bounding box: ${bbox.flattenToString()}")
            }

            element.cornerPoints?.let { points ->
              Log.v(MANUAL_TESTING_LOG, "Expected corner point size is 4, get ${points.size}")
              for (point in points) {
                Log.v(MANUAL_TESTING_LOG, "Corner point for element $k is located at: x=${point.x}, y=${point.y}")
              }
            }
          }
        }
      }
    }
  }
}*/

class TextRecognitionProcessor(
  private val context: Context,
  textRecognizerOptions: TextRecognizerOptionsInterface
) : VisionProcessorBase<Text>(context) {

  private val textRecognizer: TextRecognizer = TextRecognition.getClient(textRecognizerOptions)

  // Translation components
  private var translator: Translator? = null
  private var isTranslationEnabled: Boolean = false
  private var targetLanguage: String = TranslateLanguage.ENGLISH

  // Translation cache to avoid re-translating same text
  private val translationCache = LruCache<String, String>(200)

  // Pending translations tracker
  private val pendingTranslations = ConcurrentHashMap<String, Boolean>()

  // Cache preferences
  private var shouldGroupRecognizedTextInBlocks: Boolean = false
  private var showLanguageTag: Boolean = false
  private var showConfidence: Boolean = false

  // Single reusable TextGraphic instance - CRITICAL for no blinking
  private var currentTextGraphic: TextGraphic? = null

  init {
    updatePreferences()

    isTranslationEnabled = PreferenceUtils.isTranslationEnabled(context)
    if (isTranslationEnabled) {
      initializeTranslator()
    }
  }

  private fun updatePreferences() {
    shouldGroupRecognizedTextInBlocks = PreferenceUtils.shouldGroupRecognizedTextInBlocks(context)
    showLanguageTag = PreferenceUtils.showLanguageTag(context)
    showConfidence = PreferenceUtils.shouldShowTextConfidence(context)
  }

  private fun initializeTranslator() {
    val sourceLanguage = PreferenceUtils.getSourceLanguage(context)
    targetLanguage = PreferenceUtils.getTargetLanguage(context)

    val options = TranslatorOptions.Builder()
      .setSourceLanguage(sourceLanguage)
      .setTargetLanguage(targetLanguage)
      .build()

    translator?.close()
    translator = Translation.getClient(options)

    // Download model if needed (async, non-blocking)
    val conditions = DownloadConditions.Builder().apply {
      if (PreferenceUtils.isTranslationWifiOnly(context)) {
        requireWifi()
      }
    }.build()

    translator?.downloadModelIfNeeded(conditions)
      ?.addOnSuccessListener {
        if (BuildConfig.DEBUG) {
          Log.d(TAG, "Translation model ready")
        }
      }
      ?.addOnFailureListener { e ->
        if (BuildConfig.DEBUG) {
          Log.e(TAG, "Failed to download translation model", e)
        }
      }
  }

  override fun stop() {
    super.stop()
    textRecognizer.close()
    translator?.close()
    currentTextGraphic = null
    translationCache.evictAll()
    pendingTranslations.clear()
  }

  override fun detectInImage(image: InputImage): Task<Text> {
    return textRecognizer.process(image)
  }

  override fun onSuccess(text: Text, graphicOverlay: GraphicOverlay) {
    if (BuildConfig.DEBUG && Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "On-device Text detection successful")
    }

    if (BuildConfig.DEBUG && Log.isLoggable(MANUAL_TESTING_LOG, Log.VERBOSE)) {
      logExtrasForTesting(text)
    }

    // CRITICAL: Always display immediately with cached translations
    // Don't wait for new translations to complete
    displayTextImmediately(text, graphicOverlay)

    // Then translate missing items in background (won't cause redraw until next frame)
    if (isTranslationEnabled && translator != null) {
      translateInBackground(text)
    }
  }

  private fun displayTextImmediately(text: Text, graphicOverlay: GraphicOverlay) {
    // Build translation map from cache only (instant, no async)
    val translationMap = if (isTranslationEnabled) {
      buildTranslationMapFromCache(text)
    } else {
      null
    }

    // Reuse or create TextGraphic
    val graphic = currentTextGraphic
    if (graphic != null &&
      graphic.shouldGroupTextInBlocks == shouldGroupRecognizedTextInBlocks &&
      graphic.showLanguageTag == showLanguageTag &&
      graphic.showConfidence == showConfidence) {
      // Update existing graphic - no recreation, no blink
      // IMPORTANT: Always pass translation map even if null (for toggling off)
      graphic.updateText(text, translationMap, forceUpdate = true)
      graphicOverlay.add(graphic)
    } else {
      // Only create new when settings change
      currentTextGraphic = TextGraphic(
        graphicOverlay,
        text,
        shouldGroupRecognizedTextInBlocks,
        showLanguageTag,
        showConfidence,
        translationMap
      )
      graphicOverlay.add(currentTextGraphic!!)
    }
  }

  private fun buildTranslationMapFromCache(text: Text): Map<String, String> {
    val map = mutableMapOf<String, String>()

    if (shouldGroupRecognizedTextInBlocks) {
      for (textBlock in text.textBlocks) {
        val originalText = textBlock.text
        if (originalText.isNotBlank()) {
          translationCache.get(originalText)?.let {
            map[originalText] = it
          }
        }
      }
    } else {
      for (textBlock in text.textBlocks) {
        for (line in textBlock.lines) {
          val originalText = line.text
          if (originalText.isNotBlank()) {
            translationCache.get(originalText)?.let {
              map[originalText] = it
            }
          }
        }
      }
    }

    return map
  }

  private fun translateInBackground(text: Text) {
    // Collect texts that need translation
    val textsToTranslate = mutableListOf<String>()

    if (shouldGroupRecognizedTextInBlocks) {
      for (textBlock in text.textBlocks) {
        val originalText = textBlock.text
        if (originalText.isNotBlank() &&
          translationCache.get(originalText) == null &&
          !pendingTranslations.containsKey(originalText)) {
          textsToTranslate.add(originalText)
        }
      }
    } else {
      for (textBlock in text.textBlocks) {
        for (line in textBlock.lines) {
          val originalText = line.text
          if (originalText.isNotBlank() &&
            translationCache.get(originalText) == null &&
            !pendingTranslations.containsKey(originalText)) {
            textsToTranslate.add(originalText)
          }
        }
      }
    }

    // Translate each missing text in background
    for (originalText in textsToTranslate) {
      pendingTranslations[originalText] = true

      translator?.translate(originalText)
        ?.addOnSuccessListener { translated ->
          translationCache.put(originalText, translated)
          pendingTranslations.remove(originalText)
          // Translation cached for next frame - no immediate redraw
        }
        ?.addOnFailureListener { e ->
          if (BuildConfig.DEBUG) {
            Log.e(TAG, "Translation failed for: $originalText", e)
          }
          pendingTranslations.remove(originalText)
          // Cache original as fallback to prevent retrying
          translationCache.put(originalText, originalText)
        }
    }
  }

  override fun onFailure(e: Exception) {
    if (BuildConfig.DEBUG) {
      Log.w(TAG, "Text detection failed.$e")
    }
  }

  fun onPreferencesChanged() {
    updatePreferences()

    // Update translation settings
    val newTranslationEnabled = PreferenceUtils.isTranslationEnabled(context)
    val newTargetLanguage = PreferenceUtils.getTargetLanguage(context)

    if (newTranslationEnabled != isTranslationEnabled || newTargetLanguage != targetLanguage) {
      isTranslationEnabled = newTranslationEnabled
      targetLanguage = newTargetLanguage
      translationCache.evictAll()
      pendingTranslations.clear()

      if (isTranslationEnabled) {
        initializeTranslator()
      } else {
        translator?.close()
        translator = null
      }
    }

    // Force recreation of TextGraphic to apply new settings
    currentTextGraphic = null
  }

  companion object {
    private const val TAG = "TextRecProcessor"

    private fun logExtrasForTesting(text: Text?) {
      if (text == null) return

      val sb = StringBuilder()
      sb.append("Detected text has : ").append(text.textBlocks.size).append(" blocks")
      Log.v(MANUAL_TESTING_LOG, sb.toString())

      if (text.textBlocks.isEmpty()) return

      for (i in text.textBlocks.indices) {
        val lines = text.textBlocks[i].lines
        Log.v(MANUAL_TESTING_LOG, "Detected text block $i has ${lines.size} lines")

        for (j in lines.indices) {
          val elements = lines[j].elements
          Log.v(MANUAL_TESTING_LOG, "Detected text line $j has ${elements.size} elements")

          for (k in elements.indices) {
            val element = elements[k]
            Log.v(MANUAL_TESTING_LOG, "Detected text element $k says: ${element.text}")

            element.boundingBox?.let { bbox ->
              Log.v(MANUAL_TESTING_LOG, "Detected text element $k has a bounding box: ${bbox.flattenToString()}")
            }

            element.cornerPoints?.let { points ->
              Log.v(MANUAL_TESTING_LOG, "Expected corner point size is 4, get ${points.size}")
              for (point in points) {
                Log.v(MANUAL_TESTING_LOG, "Corner point for element $k is located at: x=${point.x}, y=${point.y}")
              }
            }
          }
        }
      }
    }
  }
}