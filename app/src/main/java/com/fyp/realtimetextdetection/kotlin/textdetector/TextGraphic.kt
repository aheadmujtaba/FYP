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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.multidex.BuildConfig
import com.fyp.realtimetextdetection.GraphicOverlay
import com.google.mlkit.vision.text.Text
import java.util.Arrays
import kotlin.math.max
import kotlin.math.min

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
/*
class TextGraphic
constructor(
  overlay: GraphicOverlay?,
  private val text: Text,
  private val shouldGroupTextInBlocks: Boolean,
  private val showLanguageTag: Boolean,
  private val showConfidence: Boolean
) : GraphicOverlay.Graphic(overlay) {

  private val rectPaint: Paint = Paint()
  private val textPaint: Paint
  private val labelPaint: Paint

  init {
    rectPaint.color = MARKER_COLOR
    rectPaint.style = Paint.Style.FILL
    rectPaint.strokeWidth = STROKE_WIDTH
    textPaint = Paint()
    textPaint.color = TEXT_COLOR
    textPaint.textSize = TEXT_SIZE
    labelPaint = Paint()
    labelPaint.color = MARKER_COLOR
    labelPaint.style = Paint.Style.FILL
    // Redraw the overlay, as this graphic has been added.
    postInvalidate()
  }

  */
/** Draws the text block annotations for position, size, and raw value on the supplied canvas. *//*

  override fun draw(canvas: Canvas) {
    Log.d(TAG, "Text is: " + text.text)
    for (textBlock in text.textBlocks) { // Renders the text at the bottom of the box.
      Log.d(TAG, "TextBlock text is: " + textBlock.text)
      Log.d(TAG, "TextBlock boundingbox is: " + textBlock.boundingBox)
      Log.d(TAG, "TextBlock cornerpoint is: " + Arrays.toString(textBlock.cornerPoints))
      if (shouldGroupTextInBlocks) {
        drawText(
          getFormattedText(textBlock.text, textBlock.recognizedLanguage, confidence = null),
          RectF(textBlock.boundingBox),
          TEXT_SIZE * textBlock.lines.size + 2 * STROKE_WIDTH,
          canvas
        )
      } else {
        for (line in textBlock.lines) {
          Log.d(TAG, "Line text is: " + line.text)
          Log.d(TAG, "Line boundingbox is: " + line.boundingBox)
          Log.d(TAG, "Line cornerpoint is: " + Arrays.toString(line.cornerPoints))
          Log.d(TAG, "Line confidence is: " + line.confidence)
          Log.d(TAG, "Line angle is: " + line.angle)
          // Draws the bounding box around the TextBlock.
          val rect = RectF(line.boundingBox)
          drawText(
            getFormattedText(line.text, line.recognizedLanguage, line.confidence),
            rect,
            TEXT_SIZE + 2 * STROKE_WIDTH,
            canvas
          )
          for (element in line.elements) {
            Log.d(TAG, "Element text is: " + element.text)
            Log.d(TAG, "Element boundingbox is: " + element.boundingBox)
            Log.d(TAG, "Element cornerpoint is: " + Arrays.toString(element.cornerPoints))
            Log.d(TAG, "Element language is: " + element.recognizedLanguage)
            Log.d(TAG, "Element confidence is: " + element.confidence)
            Log.d(TAG, "Element angle is: " + element.angle)
            for (symbol in element.symbols) {
            Log.d(TAG, "Symbol text is: " + symbol.text)
            Log.d(TAG, "Symbol boundingbox is: " + symbol.boundingBox)
            Log.d(TAG, "Symbol cornerpoint is: " + Arrays.toString(symbol.cornerPoints))
            Log.d(TAG, "Symbol confidence is: " + symbol.confidence)
            Log.d(TAG, "Symbol angle is: " + symbol.angle)
          }
          }
        }
      }
    }
  }

  private fun getFormattedText(text: String, languageTag: String, confidence: Float?): String {
    val res =
      if (showLanguageTag) String.format(TEXT_WITH_LANGUAGE_TAG_FORMAT, languageTag, text) else text
    return if (showConfidence && confidence != null) String.format("%s (%.2f)", res, confidence)
    else res
  }

  private fun drawText(text: String, rect: RectF, textHeight: Float, canvas: Canvas) {
    // If the image is flipped, the left will be translated to right, and the right to left.

    // Translate coordinates from image space to overlay space
    val left = translateX(rect.left)
    val top = translateY(rect.top)
    val right = translateX(rect.right)
    val bottom = translateY(rect.bottom)

    val boxRect = RectF(left, top, right, bottom)
    canvas.drawRect(boxRect, rectPaint)

    if (text.isBlank()) return

    // --- 1️⃣ Scale text size according to bounding box height
    val boxHeight = boxRect.height()
    val boxWidth = boxRect.width()

    // set text size relative to box height
    val baseSize = boxHeight * 0.6f
    textPaint.textSize = baseSize.coerceAtMost(64f).coerceAtLeast(16f)

    // --- 2️⃣ If text is too wide, reduce text size
    val textWidth = textPaint.measureText(text)
    if (textWidth > boxWidth) {
      val adjustFactor = boxWidth / textWidth
      textPaint.textSize *= adjustFactor
    }

    // --- 3️⃣ Center the text horizontally and vertically
    val fm = textPaint.fontMetrics
    val textX = boxRect.left + (boxWidth - textPaint.measureText(text)) / 2
    val textY = boxRect.top + (boxHeight - (fm.bottom - fm.top)) / 2 - fm.top

    // Draw text
    canvas.drawText(text, textX, textY, textPaint)
  */
/*
    val x0 = translateX(rect.left)
    val x1 = translateX(rect.right)
    rect.left = min(x0, x1)
    rect.right = max(x0, x1)
    rect.top = translateY(rect.top)
    rect.bottom = translateY(rect.bottom)
    canvas.drawRect(rect, rectPaint)
    val textWidth = textPaint.measureText(text)
    canvas.drawRect(
      rect.left - STROKE_WIDTH,
      rect.top - textHeight,
      rect.left + textWidth + 2 * STROKE_WIDTH,
      rect.top,
      labelPaint
    )
    canvas.drawRect(
      rect.left,
      rect.top ,
      rect.left ,
      rect.top,
      labelPaint
    )
    // Renders the text at the bottom of the box.
    canvas.drawText(text, rect.left, rect.bottom - STROKE_WIDTH, textPaint)*//*

  }

  companion object {
    private const val TAG = "TextGraphic"
    private const val TEXT_WITH_LANGUAGE_TAG_FORMAT = "%s:%s"
    private const val TEXT_COLOR = Color.BLACK
    private const val MARKER_COLOR = Color.WHITE
    private const val TEXT_SIZE = 54.0f
    private const val STROKE_WIDTH = 4.0f
  }
}
*/

/*
class TextGraphic(
  overlay: GraphicOverlay?,
  private val text: Text,
  private val shouldGroupTextInBlocks: Boolean,
  private val showLanguageTag: Boolean,
  private val showConfidence: Boolean
) : GraphicOverlay.Graphic(overlay) {

  private val rectPaint: Paint = Paint()
  private val textPaint: Paint
  private val labelPaint: Paint

  // Reusable objects to avoid allocations in draw()
  private val boxRect = RectF()
  private val fontMetrics = Paint.FontMetrics()
  private val textBounds = Rect()

  init {
    rectPaint.color = MARKER_COLOR
    rectPaint.style = Paint.Style.FILL
    rectPaint.strokeWidth = STROKE_WIDTH
    textPaint = Paint()
    textPaint.color = TEXT_COLOR
    textPaint.textSize = TEXT_SIZE
    labelPaint = Paint()
    labelPaint.color = MARKER_COLOR
    labelPaint.style = Paint.Style.FILL
    postInvalidate()
  }

  override fun draw(canvas: Canvas) {
    // Remove or conditionally compile logging for production
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "Text is: ${text.text}")
    }

    for (textBlock in text.textBlocks) {
      if (BuildConfig.DEBUG) {
        Log.d(TAG, "TextBlock text is: ${textBlock.text}")
      }

      if (shouldGroupTextInBlocks) {
        drawText(
          getFormattedText(textBlock.text, textBlock.recognizedLanguage, confidence = null),
          textBlock.boundingBox,
          canvas
        )
      } else {
        for (line in textBlock.lines) {
          if (BuildConfig.DEBUG) {
            Log.d(TAG, "Line text is: ${line.text}")
          }

          drawText(
            getFormattedText(line.text, line.recognizedLanguage, line.confidence),
            line.boundingBox,
            canvas
          )

          // Only log elements if really needed - this is expensive
          if (BuildConfig.DEBUG && Log.isLoggable(TAG, Log.VERBOSE)) {
            for (element in line.elements) {
              Log.v(TAG, "Element text is: ${element.text}")
            }
          }
        }
      }
    }
  }

  private fun getFormattedText(text: String, languageTag: String, confidence: Float?): String {
    val res = if (showLanguageTag) "$languageTag:$text" else text
    return if (showConfidence && confidence != null) {
      String.format("%s (%.2f)", res, confidence)
    } else {
      res
    }
  }

  private fun drawText(text: String, rect: Rect?, canvas: Canvas) {
    if (rect == null || text.isBlank()) return

    // Translate coordinates from image space to overlay space
    val left = translateX(rect.left.toFloat())
    val top = translateY(rect.top.toFloat())
    val right = translateX(rect.right.toFloat())
    val bottom = translateY(rect.bottom.toFloat())

    // Reuse boxRect instead of creating new RectF
    boxRect.set(left, top, right, bottom)
    canvas.drawRect(boxRect, rectPaint)

    // Get box dimensions
    val boxHeight = boxRect.height()
    val boxWidth = boxRect.width()

    // Scale text size according to bounding box height
    val baseSize = boxHeight * 0.6f
    textPaint.textSize = baseSize.coerceIn(16f, 64f)

    // If text is too wide, reduce text size
    val textWidth = textPaint.measureText(text)
    if (textWidth > boxWidth) {
      textPaint.textSize *= (boxWidth / textWidth)
    }

    // Center the text horizontally and vertically
    textPaint.getFontMetrics(fontMetrics)
    val textX = boxRect.left + (boxWidth - textPaint.measureText(text)) / 2
    val textY = boxRect.top + (boxHeight - (fontMetrics.bottom - fontMetrics.top)) / 2 - fontMetrics.top

    canvas.drawText(text, textX, textY, textPaint)
  }

  companion object {
    private const val TAG = "TextGraphic"
    private const val TEXT_COLOR = Color.BLACK
    private const val MARKER_COLOR = Color.WHITE
    private const val TEXT_SIZE = 54.0f
    private const val STROKE_WIDTH = 4.0f
  }
}
*/
/*
class TextGraphic(

  overlay: GraphicOverlay?,
  private val text: Text,
  private val shouldGroupTextInBlocks: Boolean,
  private val showLanguageTag: Boolean,
  private val showConfidence: Boolean
) : GraphicOverlay.Graphic(overlay) {

  private val rectPaint: Paint = Paint().apply {
    color = MARKER_COLOR
    style = Paint.Style.FILL
    strokeWidth = STROKE_WIDTH
    isAntiAlias = false // Disable for speed if quality acceptable
  }

  private val textPaint: Paint = Paint().apply {
    color = TEXT_COLOR
    textSize = TEXT_SIZE
    isAntiAlias = true // Keep for text readability
    isSubpixelText = true
  }

  // Reusable objects - NEVER allocate in draw()
  private val boxRect = RectF()
  private val fontMetrics = Paint.FontMetrics()
  private val clipBounds = Rect()

  // Cache for formatted text to avoid string operations
  private val formattedTextCache = HashMap<String, String>(32)

  // Pre-computed display items to avoid calculations in draw()
  private val displayItems = mutableListOf<DisplayItem>()
  private var needsRebuild = true

  private data class DisplayItem(
    val text: String,
    val rect: Rect,
    var scaledTextSize: Float = TEXT_SIZE
  )

  init {
    postInvalidate()
  }

  // Rebuild display list once, not every frame
  private fun rebuildDisplayList() {
    if (!needsRebuild) return

    displayItems.clear()

    for (textBlock in text.textBlocks) {
      if (shouldGroupTextInBlocks) {
        textBlock.boundingBox?.let { bbox ->
          displayItems.add(
            DisplayItem(
              getFormattedTextCached(
                textBlock.text,
                textBlock.recognizedLanguage,
                null
              ),
              bbox
            )
          )
        }
      } else {
        for (line in textBlock.lines) {
          line.boundingBox?.let { bbox ->
            displayItems.add(
              DisplayItem(
                getFormattedTextCached(
                  line.text,
                  line.recognizedLanguage,
                  line.confidence
                ),
                bbox
              )
            )
          }
        }
      }
    }

    needsRebuild = false
  }

  override fun draw(canvas: Canvas) {
    rebuildDisplayList()

    // Get canvas clip bounds for frustum culling
    canvas.getClipBounds(clipBounds)

    // Fast path: render only visible items
    for (item in displayItems) {
      // Skip if text is empty
      if (item.text.isEmpty()) continue

      // Translate coordinates
      val left = translateX(item.rect.left.toFloat())
      val top = translateY(item.rect.top.toFloat())
      val right = translateX(item.rect.right.toFloat())
      val bottom = translateY(item.rect.bottom.toFloat())

      boxRect.set(left, top, right, bottom)

      // Frustum culling - skip if completely outside viewport
      if (!RectF.intersects(boxRect, clipBounds.toRectF())) {
        continue
      }

      // Quick reject test - hardware accelerated
      if (canvas.quickReject(boxRect, Canvas.EdgeType.BW)) {
        continue
      }

      // Draw background box
      canvas.drawRect(boxRect, rectPaint)

      // Calculate and cache text size (only once per item)
      if (item.scaledTextSize == TEXT_SIZE) {
        val boxHeight = boxRect.height()
        val boxWidth = boxRect.width()

        var size = (boxHeight * 0.6f).coerceIn(16f, 64f)
        textPaint.textSize = size

        // Adjust if too wide
        val textWidth = textPaint.measureText(item.text)
        if (textWidth > boxWidth) {
          size *= (boxWidth / textWidth * 0.95f) // 95% to add padding
        }

        item.scaledTextSize = size
      }

      textPaint.textSize = item.scaledTextSize

      // Center text
      val boxHeight = boxRect.height()
      val boxWidth = boxRect.width()
      textPaint.getFontMetrics(fontMetrics)

      val textX = boxRect.left + (boxWidth - textPaint.measureText(item.text)) * 0.5f
      val textY = boxRect.top + (boxHeight - (fontMetrics.bottom - fontMetrics.top)) * 0.5f - fontMetrics.top

      // Single draw call
      canvas.drawText(item.text, textX, textY, textPaint)
    }
  }

  private fun getFormattedTextCached(text: String, languageTag: String, confidence: Float?): String {
    // Create cache key
    val key = if (showConfidence && confidence != null) {
      "$languageTag:$text:$confidence"
    } else {
      "$languageTag:$text"
    }

    return formattedTextCache.getOrPut(key) {
      val res = if (showLanguageTag) "$languageTag:$text" else text
      if (showConfidence && confidence != null) {
        String.format("%s (%.2f)", res, confidence)
      } else {
        res
      }
    }
  }

  // Helper to convert Rect to RectF for intersection test
  private fun Rect.toRectF(): RectF {
    return RectF(this.left.toFloat(), this.top.toFloat(), this.right.toFloat(), this.bottom.toFloat())
  }

  // Call this if the text data changes
  fun invalidateDisplayList() {
    needsRebuild = true
    formattedTextCache.clear()
    displayItems.forEach { it.scaledTextSize = TEXT_SIZE }
  }

  companion object {
    private const val TAG = "TextGraphic"
    private const val TEXT_COLOR = Color.BLACK
    private const val MARKER_COLOR = Color.WHITE
    private const val TEXT_SIZE = 54.0f
    private const val STROKE_WIDTH = 4.0f
  }
}
*/
/*
class TextGraphic(
  overlay: GraphicOverlay?,
  private var text: Text,
  val shouldGroupTextInBlocks: Boolean,
  val showLanguageTag: Boolean,
  val showConfidence: Boolean
) : GraphicOverlay.Graphic(overlay) {

  private val rectPaint: Paint = Paint().apply {
    color = MARKER_COLOR
    style = Paint.Style.FILL
    strokeWidth = STROKE_WIDTH
    isAntiAlias = false // Disable for speed
  }

  private val textPaint: Paint = Paint().apply {
    color = TEXT_COLOR
    textSize = TEXT_SIZE
    isAntiAlias = true
    isSubpixelText = true
  }

  // Reusable objects
  private val boxRect = RectF()
  private val fontMetrics = Paint.FontMetrics()
  private val clipBounds = Rect()

  // Display list cache
  private val displayItems = mutableListOf<DisplayItem>()
  private val formattedTextCache = HashMap<String, String>(32)
  private var needsRebuild = true

  private data class DisplayItem(
    val text: String,
    val rect: Rect,
    var scaledTextSize: Float = TEXT_SIZE
  )

  init {
    postInvalidate()
  }

  // Allow updating text without recreating graphic
  fun updateText(newText: Text) {
    this.text = newText
    invalidateDisplayList()
    postInvalidate()
  }

  fun invalidateDisplayList() {
    needsRebuild = true
    formattedTextCache.clear()
    displayItems.forEach { it.scaledTextSize = TEXT_SIZE }
  }

  private fun rebuildDisplayList() {
    if (!needsRebuild) return

    displayItems.clear()

    for (textBlock in text.textBlocks) {
      if (shouldGroupTextInBlocks) {
        textBlock.boundingBox?.let { bbox ->
          displayItems.add(
            DisplayItem(
              getFormattedTextCached(textBlock.text, textBlock.recognizedLanguage, null),
              bbox
            )
          )
        }
      } else {
        for (line in textBlock.lines) {
          line.boundingBox?.let { bbox ->
            displayItems.add(
              DisplayItem(
                getFormattedTextCached(line.text, line.recognizedLanguage, line.confidence),
                bbox
              )
            )
          }
        }
      }
    }

    needsRebuild = false
  }

  override fun draw(canvas: Canvas) {
    rebuildDisplayList()
    canvas.getClipBounds(clipBounds)

    for (item in displayItems) {
      if (item.text.isEmpty()) continue

      val left = translateX(item.rect.left.toFloat())
      val top = translateY(item.rect.top.toFloat())
      val right = translateX(item.rect.right.toFloat())
      val bottom = translateY(item.rect.bottom.toFloat())

      boxRect.set(left, top, right, bottom)

      // Frustum culling
      if (!RectF.intersects(boxRect, RectF(clipBounds))) continue
      if (canvas.quickReject(boxRect, Canvas.EdgeType.BW)) continue

      canvas.drawRect(boxRect, rectPaint)

      // Cache text size calculations
      if (item.scaledTextSize == TEXT_SIZE) {
        val boxHeight = boxRect.height()
        val boxWidth = boxRect.width()

        var size = (boxHeight * 0.6f).coerceIn(16f, 64f)
        textPaint.textSize = size

        val textWidth = textPaint.measureText(item.text)
        if (textWidth > boxWidth) {
          size *= (boxWidth / textWidth * 0.95f)
        }

        item.scaledTextSize = size
      }

      textPaint.textSize = item.scaledTextSize

      val boxHeight = boxRect.height()
      val boxWidth = boxRect.width()
      textPaint.getFontMetrics(fontMetrics)

      val textX = boxRect.left + (boxWidth - textPaint.measureText(item.text)) * 0.5f
      val textY = boxRect.top + (boxHeight - (fontMetrics.bottom - fontMetrics.top)) * 0.5f - fontMetrics.top

      canvas.drawText(item.text, textX, textY, textPaint)
    }
  }

  private fun getFormattedTextCached(text: String, languageTag: String, confidence: Float?): String {
    val key = if (showConfidence && confidence != null) {
      "$languageTag:$text:$confidence"
    } else {
      "$languageTag:$text"
    }

    return formattedTextCache.getOrPut(key) {
      val res = if (showLanguageTag) "$languageTag:$text" else text
      if (showConfidence && confidence != null) {
        String.format("%s (%.2f)", res, confidence)
      } else {
        res
      }
    }
  }

  companion object {
    private const val TEXT_COLOR = Color.BLACK
    private const val MARKER_COLOR = Color.WHITE
    private const val TEXT_SIZE = 54.0f
    private const val STROKE_WIDTH = 4.0f
  }
}*/
/*
class TextGraphic(
  overlay: GraphicOverlay?,
  private var text: Text,
  val shouldGroupTextInBlocks: Boolean,
  val showLanguageTag: Boolean,
  val showConfidence: Boolean,
  private var translationMap: Map<String, String>? = null
) : GraphicOverlay.Graphic(overlay) {

  private val rectPaint: Paint = Paint().apply {
    color = MARKER_COLOR
    style = Paint.Style.FILL
    strokeWidth = STROKE_WIDTH
    isAntiAlias = false
  }

  private val textPaint: Paint = Paint().apply {
    color = TEXT_COLOR
    textSize = TEXT_SIZE
    isAntiAlias = true
    isSubpixelText = true
  }

  // Reusable objects
  private val boxRect = RectF()
  private val fontMetrics = Paint.FontMetrics()
  private val clipBounds = Rect()

  // Display list cache
  private val displayItems = mutableListOf<DisplayItem>()
  private val formattedTextCache = HashMap<String, String>(32)
  private var needsRebuild = true

  private data class DisplayItem(
    val originalText: String,
    val displayText: String,
    val rect: Rect,
    var scaledTextSize: Float = TEXT_SIZE
  )

  init {
    postInvalidate()
  }

  fun updateText(newText: Text, newTranslationMap: Map<String, String>? = null) {
    this.text = newText
    this.translationMap = newTranslationMap
    invalidateDisplayList()
    postInvalidate()
  }

  fun invalidateDisplayList() {
    needsRebuild = true
    formattedTextCache.clear()
    displayItems.forEach { it.scaledTextSize = TEXT_SIZE }
  }

  private fun rebuildDisplayList() {
    if (!needsRebuild) return

    displayItems.clear()

    for (textBlock in text.textBlocks) {
      if (shouldGroupTextInBlocks) {
        textBlock.boundingBox?.let { bbox ->
          val originalText = textBlock.text
          val translatedText = translationMap?.get(originalText) ?: originalText
          val displayText = getFormattedTextCached(
            translatedText,
            textBlock.recognizedLanguage,
            null
          )

          displayItems.add(
            DisplayItem(
              originalText,
              displayText,
              bbox
            )
          )
        }
      } else {
        for (line in textBlock.lines) {
          line.boundingBox?.let { bbox ->
            val originalText = line.text
            val translatedText = translationMap?.get(originalText) ?: originalText
            val displayText = getFormattedTextCached(
              translatedText,
              line.recognizedLanguage,
              line.confidence
            )

            displayItems.add(
              DisplayItem(
                originalText,
                displayText,
                bbox
              )
            )
          }
        }
      }
    }

    needsRebuild = false
  }

  override fun draw(canvas: Canvas) {
    rebuildDisplayList()
    canvas.getClipBounds(clipBounds)

    for (item in displayItems) {
      if (item.displayText.isEmpty()) continue

      val left = translateX(item.rect.left.toFloat())
      val top = translateY(item.rect.top.toFloat())
      val right = translateX(item.rect.right.toFloat())
      val bottom = translateY(item.rect.bottom.toFloat())

      boxRect.set(left, top, right, bottom)

      // Frustum culling
      if (!RectF.intersects(boxRect, RectF(clipBounds))) continue
      if (canvas.quickReject(boxRect, Canvas.EdgeType.BW)) continue

      canvas.drawRect(boxRect, rectPaint)

      // Cache text size calculations
      if (item.scaledTextSize == TEXT_SIZE) {
        val boxHeight = boxRect.height()
        val boxWidth = boxRect.width()

        var size = (boxHeight * 0.6f).coerceIn(16f, 64f)
        textPaint.textSize = size

        val textWidth = textPaint.measureText(item.displayText)
        if (textWidth > boxWidth) {
          size *= (boxWidth / textWidth * 0.95f)
        }

        item.scaledTextSize = size
      }

      textPaint.textSize = item.scaledTextSize

      val boxHeight = boxRect.height()
      val boxWidth = boxRect.width()
      textPaint.getFontMetrics(fontMetrics)

      val textX = boxRect.left + (boxWidth - textPaint.measureText(item.displayText)) * 0.5f
      val textY = boxRect.top + (boxHeight - (fontMetrics.bottom - fontMetrics.top)) * 0.5f - fontMetrics.top

      canvas.drawText(item.displayText, textX, textY, textPaint)
    }
  }

  private fun getFormattedTextCached(text: String, languageTag: String, confidence: Float?): String {
    val key = if (showConfidence && confidence != null) {
      "$languageTag:$text:$confidence"
    } else {
      "$languageTag:$text"
    }

    return formattedTextCache.getOrPut(key) {
      val res = if (showLanguageTag) "$languageTag:$text" else text
      if (showConfidence && confidence != null) {
        String.format("%s (%.2f)", res, confidence)
      } else {
        res
      }
    }
  }

  companion object {
    private const val TEXT_COLOR = Color.BLACK
    private const val MARKER_COLOR = Color.WHITE
    private const val TEXT_SIZE = 54.0f
    private const val STROKE_WIDTH = 4.0f
  }
}
*/
class TextGraphic(
  overlay: GraphicOverlay?,
  private var text: Text,
  val shouldGroupTextInBlocks: Boolean,
  val showLanguageTag: Boolean,
  val showConfidence: Boolean,
  private var translationMap: Map<String, String>? = null
) : GraphicOverlay.Graphic(overlay) {

  private val rectPaint: Paint = Paint().apply {
    color = MARKER_COLOR
    style = Paint.Style.FILL
    strokeWidth = STROKE_WIDTH
    isAntiAlias = false
  }

  private val textPaint: Paint = Paint().apply {
    color = TEXT_COLOR
    textSize = TEXT_SIZE
    isAntiAlias = true
    isSubpixelText = true
  }

  // Reusable objects
  private val boxRect = RectF()
  private val fontMetrics = Paint.FontMetrics()
  private val clipBounds = Rect()

  // Display list cache
  private val displayItems = mutableListOf<DisplayItem>()
  private val formattedTextCache = HashMap<String, String>(32)
  private var needsRebuild = true

  private data class DisplayItem(
    val originalText: String,
    val displayText: String,
    val rect: Rect,
    var scaledTextSize: Float = TEXT_SIZE
  )

  init {
    postInvalidate()
  }

  fun updateText(newText: Text, newTranslationMap: Map<String, String>? = null, forceUpdate: Boolean = false) {
    // Detect if anything actually changed
    val textChanged = this.text !== newText
    val translationsChanged = newTranslationMap?.let { new ->
      translationMap?.let { old ->
        new.size != old.size || new.keys.any { old[it] != new[it] }
      } ?: true
    } ?: (translationMap != null) // Translation map went from something to null

    this.text = newText
    this.translationMap = newTranslationMap

    // Always invalidate and redraw if forced, text changed, or translations changed
    if (forceUpdate || textChanged || translationsChanged || needsRebuild) {
      invalidateDisplayList()
      postInvalidate()
    }
  }

  fun invalidateDisplayList() {
    needsRebuild = true
    // Don't clear caches - keep them for stability
    displayItems.forEach { it.scaledTextSize = TEXT_SIZE }
  }

  private fun rebuildDisplayList() {
    if (!needsRebuild) return

    displayItems.clear()

    for (textBlock in text.textBlocks) {
      if (shouldGroupTextInBlocks) {
        textBlock.boundingBox?.let { bbox ->
          val originalText = textBlock.text
          val translatedText = translationMap?.get(originalText) ?: originalText
          val displayText = getFormattedTextCached(
            translatedText,
            textBlock.recognizedLanguage,
            null
          )

          displayItems.add(
            DisplayItem(
              originalText,
              displayText,
              bbox
            )
          )
        }
      } else {
        for (line in textBlock.lines) {
          line.boundingBox?.let { bbox ->
            val originalText = line.text
            val translatedText = translationMap?.get(originalText) ?: originalText
            val displayText = getFormattedTextCached(
              translatedText,
              line.recognizedLanguage,
              line.confidence
            )

            displayItems.add(
              DisplayItem(
                originalText,
                displayText,
                bbox
              )
            )
          }
        }
      }
    }

    needsRebuild = false
  }

  override fun draw(canvas: Canvas) {
    rebuildDisplayList()
    canvas.getClipBounds(clipBounds)

    for (item in displayItems) {
      if (item.displayText.isEmpty()) continue

      val left = translateX(item.rect.left.toFloat())
      val top = translateY(item.rect.top.toFloat())
      val right = translateX(item.rect.right.toFloat())
      val bottom = translateY(item.rect.bottom.toFloat())

      boxRect.set(left, top, right, bottom)

      // Frustum culling
      if (!RectF.intersects(boxRect, RectF(clipBounds))) continue
      if (canvas.quickReject(boxRect, Canvas.EdgeType.BW)) continue

      canvas.drawRect(boxRect, rectPaint)

      // Cache text size calculations
      if (item.scaledTextSize == TEXT_SIZE) {
        val boxHeight = boxRect.height()
        val boxWidth = boxRect.width()

        var size = (boxHeight * 0.6f).coerceIn(16f, 64f)
        textPaint.textSize = size

        val textWidth = textPaint.measureText(item.displayText)
        if (textWidth > boxWidth) {
          size *= (boxWidth / textWidth * 0.95f)
        }

        item.scaledTextSize = size
      }

      textPaint.textSize = item.scaledTextSize

      val boxHeight = boxRect.height()
      val boxWidth = boxRect.width()
      textPaint.getFontMetrics(fontMetrics)

      val textX = boxRect.left + (boxWidth - textPaint.measureText(item.displayText)) * 0.5f
      val textY = boxRect.top + (boxHeight - (fontMetrics.bottom - fontMetrics.top)) * 0.5f - fontMetrics.top

      canvas.drawText(item.displayText, textX, textY, textPaint)
    }
  }

  private fun getFormattedTextCached(text: String, languageTag: String, confidence: Float?): String {
    val key = if (showConfidence && confidence != null) {
      "$languageTag:$text:$confidence"
    } else {
      "$languageTag:$text"
    }

    return formattedTextCache.getOrPut(key) {
      val res = if (showLanguageTag) "$languageTag:$text" else text
      if (showConfidence && confidence != null) {
        String.format("%s (%.2f)", res, confidence)
      } else {
        res
      }
    }
  }

  companion object {
    private const val TEXT_COLOR = Color.BLACK
    private const val MARKER_COLOR = Color.WHITE
    private const val TEXT_SIZE = 54.0f
    private const val STROKE_WIDTH = 4.0f
  }
}