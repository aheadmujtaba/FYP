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
package com.fyp.realtimetextdetection.preference

import android.content.Context
import android.content.SharedPreferences
import android.os.Build.VERSION_CODES
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.camera.core.CameraSelector
import com.fyp.realtimetextdetection.CameraSource
import com.fyp.realtimetextdetection.CameraSource.SizePair
import com.fyp.realtimetextdetection.R
import com.google.android.gms.common.images.Size
import com.google.common.base.Preconditions
import com.google.mlkit.nl.translate.TranslateLanguage

/** Utility class to retrieve shared preferences.  */
object PreferenceUtils {
    private const val POSE_DETECTOR_PERFORMANCE_MODE_FAST = 1

    @JvmStatic
    fun saveString(context: Context, @StringRes prefKeyId: Int, value: String?) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(context.getString(prefKeyId), value)
            .apply()
    }

    @JvmStatic
    fun getCameraPreviewSizePair(context: Context, cameraId: Int): SizePair? {
        Preconditions.checkArgument(
            cameraId == CameraSource.CAMERA_FACING_BACK
                    || cameraId == CameraSource.CAMERA_FACING_FRONT
        )
        val previewSizePrefKey: String?
        val pictureSizePrefKey: String?
        if (cameraId == CameraSource.CAMERA_FACING_BACK) {
            previewSizePrefKey = context.getString(R.string.pref_key_rear_camera_preview_size)
            pictureSizePrefKey = context.getString(R.string.pref_key_rear_camera_picture_size)
        } else {
            previewSizePrefKey = context.getString(R.string.pref_key_front_camera_preview_size)
            pictureSizePrefKey = context.getString(R.string.pref_key_front_camera_picture_size)
        }

        try {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return SizePair(
                Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)!!),
                Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null)!!)
            )
        } catch (e: Exception) {
            return null
        }
    }

    @RequiresApi(VERSION_CODES.LOLLIPOP)
    fun getCameraXTargetResolution(context: Context, lensfacing: Int): android.util.Size? {
        Preconditions.checkArgument(
            lensfacing == CameraSelector.LENS_FACING_BACK
                    || lensfacing == CameraSelector.LENS_FACING_FRONT
        )
        val prefKey =
            if (lensfacing == CameraSelector.LENS_FACING_BACK)
                context.getString(R.string.pref_key_camerax_rear_camera_target_resolution)
            else
                context.getString(R.string.pref_key_camerax_front_camera_target_resolution)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        try {
            return android.util.Size.parseSize(sharedPreferences.getString(prefKey, null))
        } catch (e: Exception) {
            return null
        }
    }

    fun shouldHideDetectionInfo(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(R.string.pref_key_info_hide)
        return sharedPreferences.getBoolean(prefKey, false)
    }


    fun shouldEnableAutoZoom(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(R.string.pref_key_enable_auto_zoom)
        return sharedPreferences.getBoolean(prefKey, true)
    }

    fun shouldGroupRecognizedTextInBlocks(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(R.string.pref_key_group_recognized_text_in_blocks)
        return sharedPreferences.getBoolean(prefKey, false)
    }

    fun showLanguageTag(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(R.string.pref_key_show_language_tag)
        return sharedPreferences.getBoolean(prefKey, false)
    }

    fun shouldShowTextConfidence(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(R.string.pref_key_show_text_confidence)
        return sharedPreferences.getBoolean(prefKey, false)
    }


    /**
     * Mode type preference is backed by [android.preference.ListPreference] which only support
     * storing its entry value as string type, so we need to retrieve as string and then convert to
     * integer.
     */
    private fun getModeTypePreferenceValue(
        context: Context, @StringRes prefKeyResId: Int, defaultValue: Int
    ): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(prefKeyResId)
        return sharedPreferences.getString(prefKey, defaultValue.toString())!!.toInt()
    }

    @JvmStatic
    fun isCameraLiveViewportEnabled(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(R.string.pref_key_camera_live_viewport)
        return sharedPreferences.getBoolean(prefKey, false)
    }

    // Translation preference keys
    private const val KEY_TRANSLATION_ENABLED = "translation_enabled"
    private const val KEY_SOURCE_LANGUAGE = "source_language"
    private const val KEY_TARGET_LANGUAGE = "target_language"
    private const val KEY_SHOW_BOTH_TEXTS = "show_both_texts"
    private const val KEY_TRANSLATION_WIFI_ONLY = "translation_wifi_only"

    // Your existing methods
    // ...

    // Translation methods
    fun isTranslationEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_TRANSLATION_ENABLED, false)
    }

    fun setTranslationEnabled(context: Context, enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_TRANSLATION_ENABLED, enabled)
            .apply()
    }

    fun getSourceLanguage(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_SOURCE_LANGUAGE, TranslateLanguage.ENGLISH) ?: TranslateLanguage.ENGLISH
    }

    fun setSourceLanguage(context: Context, language: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(KEY_SOURCE_LANGUAGE, language)
            .apply()
    }

    fun getTargetLanguage(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_TARGET_LANGUAGE, TranslateLanguage.SPANISH) ?: TranslateLanguage.SPANISH
    }

    fun setTargetLanguage(context: Context, language: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(KEY_TARGET_LANGUAGE, language)
            .apply()
    }

    fun shouldShowBothTexts(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_SHOW_BOTH_TEXTS, false)
    }

    fun isTranslationWifiOnly(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_TRANSLATION_WIFI_ONLY, true)
    }
}
