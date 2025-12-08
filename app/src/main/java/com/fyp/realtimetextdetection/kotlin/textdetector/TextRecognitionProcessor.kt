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


class TextRecognitionProcessor(
  private val context: Context,
  textRecognizerOptions: TextRecognizerOptionsInterface
) : VisionProcessorBase<Text>(context) {

  private val textRecognizer: TextRecognizer = TextRecognition.getClient(textRecognizerOptions)

  // Translation components
  private var translator: Translator? = null
  private var isTranslationEnabled: Boolean = true
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

  /**
   * Public method to get translation for a specific text
   * Used by accessibility features (TTS)
   */
  fun getTranslationForText(originalText: String): String? {
    if (!isTranslationEnabled || originalText.isBlank()) {
      return null
    }
    return translationCache.get(originalText)
  }

  /**
   * Check if translation is enabled
   */
  fun isTranslationActive(): Boolean = isTranslationEnabled

  /**
   * Get all cached translations
   */
  fun getAllTranslations(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val snapshot = translationCache.snapshot()
    for ((key, value) in snapshot) {
      map[key] = value
    }
    return map
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

          if (BuildConfig.DEBUG) {
            Log.d(TAG, "Translation completed: '$originalText' -> '$translated'")
          }
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