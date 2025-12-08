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

package com.fyp.realtimetextdetection.kotlin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fyp.realtimetextdetection.BitmapUtils
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.fyp.realtimetextdetection.CameraXViewModel
import com.fyp.realtimetextdetection.GraphicOverlay
import com.fyp.realtimetextdetection.R
import com.fyp.realtimetextdetection.VisionImageProcessor
import com.fyp.realtimetextdetection.kotlin.textdetector.TextRecognitionProcessor
import com.fyp.realtimetextdetection.preference.PreferenceUtils
import com.fyp.realtimetextdetection.preference.SettingsActivity
import com.fyp.realtimetextdetection.preference.SettingsActivity.LaunchSource
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraXLivePreviewActivity :
    AppCompatActivity(), OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    private var previewView: PreviewView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var selectedModel = OBJECT_DETECTION
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraSelector: CameraSelector? = null

    // Capture button and image capture use case
    private var captureButton: ImageView? = null
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        if (savedInstanceState != null) {
            selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, OBJECT_DETECTION)
        }
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        setContentView(R.layout.activity_vision_camerax_live_preview)
        previewView = findViewById(R.id.preview_view)
        if (previewView == null) {
            Log.d(TAG, "previewView is null")
        }
        graphicOverlay = findViewById(R.id.graphic_overlay)
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }
        val spinner = findViewById<Spinner>(R.id.spinner)
        val options: MutableList<String> = ArrayList()

        options.add(TEXT_RECOGNITION_LATIN)
        options.add(TEXT_RECOGNITION_CHINESE)
        options.add(TEXT_RECOGNITION_DEVANAGARI)
        options.add(TEXT_RECOGNITION_JAPANESE)
        options.add(TEXT_RECOGNITION_KOREAN)
        options.add(FACE_MESH_DETECTION)

        // Creating adapter for spinner
        val dataAdapter = ArrayAdapter(this, R.layout.spinner_style, options)
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // attaching data adapter to spinner
        spinner.adapter = dataAdapter
        spinner.onItemSelectedListener = this
        val facingSwitch = findViewById<ToggleButton>(R.id.facing_switch)
        facingSwitch.setOnCheckedChangeListener(this)
        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(CameraXViewModel::class.java)
            .processCameraProvider
            .observe(
                this,
                Observer { provider: ProcessCameraProvider? ->
                    cameraProvider = provider
                    bindAllCameraUseCases()
                },
            )

        val settingsButton = findViewById<ImageView>(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            intent.putExtra(SettingsActivity.EXTRA_LAUNCH_SOURCE, LaunchSource.CAMERAX_LIVE_PREVIEW)
            startActivity(intent)
        }

        // Setup capture button
        setupCaptureButton()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putString(STATE_SELECTED_MODEL, selectedModel)
    }

    @Synchronized
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        selectedModel = parent?.getItemAtPosition(pos).toString()
        Log.d(TAG, "Selected model: $selectedModel")
        bindAnalysisUseCase()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing.
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (cameraProvider == null) {
            return
        }
        val newLensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
        val newCameraSelector = CameraSelector.Builder().requireLensFacing(newLensFacing).build()
        try {
            if (cameraProvider!!.hasCamera(newCameraSelector)) {
                Log.d(TAG, "Set facing to " + newLensFacing)
                lensFacing = newLensFacing
                cameraSelector = newCameraSelector
                bindAllCameraUseCases()
                return
            }
        } catch (e: CameraInfoUnavailableException) {
            // Falls through
        }
        Toast.makeText(
            applicationContext,
            "This device does not have lens with facing: $newLensFacing",
            Toast.LENGTH_SHORT,
        )
            .show()
    }

    public override fun onResume() {
        super.onResume()
        bindAllCameraUseCases()
    }

    override fun onPause() {
        super.onPause()
        imageProcessor?.run { this.stop() }
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }

    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            cameraProvider!!.unbindAll()
            bindPreviewUseCase()
            bindAnalysisUseCase()
            bindImageCaptureUseCase()
        }
    }

    private fun bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return
        }
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        val builder = Preview.Builder()
        val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        previewUseCase = builder.build()
        previewUseCase!!.setSurfaceProvider(previewView!!.getSurfaceProvider())
        camera = cameraProvider!!.bindToLifecycle(this, cameraSelector!!, previewUseCase)
    }

    private fun bindImageCaptureUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (imageCapture != null) {
            cameraProvider!!.unbind(imageCapture)
        }

        val builder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

        val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }

        imageCapture = builder.build()

        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, imageCapture)
    }

    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        if (imageProcessor != null) {
            imageProcessor!!.stop()
        }
        imageProcessor =
            try {
                when (selectedModel) {
                    TEXT_RECOGNITION_LATIN -> {
                        Log.i(TAG, "Using on-device Text recognition Processor for Latin")
                        TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build())
                    }

                    TEXT_RECOGNITION_CHINESE -> {
                        Log.i(
                            TAG,
                            "Using on-device Text recognition Processor for Latin and Chinese"
                        )
                        TextRecognitionProcessor(
                            this,
                            ChineseTextRecognizerOptions.Builder().build()
                        )
                    }

                    TEXT_RECOGNITION_DEVANAGARI -> {
                        Log.i(
                            TAG,
                            "Using on-device Text recognition Processor for Latin and Devanagari"
                        )
                        TextRecognitionProcessor(
                            this,
                            DevanagariTextRecognizerOptions.Builder().build()
                        )
                    }

                    TEXT_RECOGNITION_JAPANESE -> {
                        Log.i(
                            TAG,
                            "Using on-device Text recognition Processor for Latin and Japanese"
                        )
                        TextRecognitionProcessor(
                            this,
                            JapaneseTextRecognizerOptions.Builder().build()
                        )
                    }

                    TEXT_RECOGNITION_KOREAN -> {
                        Log.i(
                            TAG,
                            "Using on-device Text recognition Processor for Latin and Korean"
                        )
                        TextRecognitionProcessor(
                            this,
                            KoreanTextRecognizerOptions.Builder().build()
                        )
                    }

                    else -> throw IllegalStateException("Invalid model name")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Can not create image processor: $selectedModel", e)
                Toast.makeText(
                    applicationContext,
                    "Can not create image processor: " + e.localizedMessage,
                    Toast.LENGTH_LONG,
                )
                    .show()
                return
            }

        val builder = ImageAnalysis.Builder()
        val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        analysisUseCase = builder.build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
            ContextCompat.getMainExecutor(this),
        ) { imageProxy: ImageProxy ->
            if (needUpdateGraphicOverlayImageSourceInfo) {
                val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                if (rotationDegrees == 0 || rotationDegrees == 180) {
                    graphicOverlay!!.setImageSourceInfo(
                        imageProxy.width,
                        imageProxy.height,
                        isImageFlipped
                    )
                } else {
                    graphicOverlay!!.setImageSourceInfo(
                        imageProxy.height,
                        imageProxy.width,
                        isImageFlipped
                    )
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            }

            try {
                imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
            } catch (e: MlKitException) {
                Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, analysisUseCase)
    }

    // CAPTURE METHODS

    private fun setupCaptureButton() {
        captureButton = findViewById(R.id.capture_button)
        captureButton?.setOnClickListener {
            captureAndPreviewImage()
        }
    }

    private fun captureAndPreviewImage() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "MLKIT_${timeStamp}.jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(storageDir, imageFileName)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(imageFile)
                    val recognitionMode = when (selectedModel) {
                        TEXT_RECOGNITION_CHINESE -> ImagePreviewActivity.RECOGNITION_CHINESE
                        TEXT_RECOGNITION_DEVANAGARI -> ImagePreviewActivity.RECOGNITION_DEVANAGARI
                        TEXT_RECOGNITION_JAPANESE -> ImagePreviewActivity.RECOGNITION_JAPANESE
                        TEXT_RECOGNITION_KOREAN -> ImagePreviewActivity.RECOGNITION_KOREAN
                        else -> ImagePreviewActivity.RECOGNITION_LATIN
                    }

                    val intent = ImagePreviewActivity.createIntent(this@CameraXLivePreviewActivity, savedUri, recognitionMode)
                    startActivity(intent)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exception)
                    Toast.makeText(
                        this@CameraXLivePreviewActivity,
                        "Capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
    companion object {
        private const val TAG = "CameraXLivePreview"
        private const val OBJECT_DETECTION = "Object Detection"
        private const val TEXT_RECOGNITION_LATIN = "Text Recognition Latin"
        private const val TEXT_RECOGNITION_CHINESE = "Text Recognition Chinese"
        private const val TEXT_RECOGNITION_DEVANAGARI = "Text Recognition Devanagari"
        private const val TEXT_RECOGNITION_JAPANESE = "Text Recognition Japanese"
        private const val TEXT_RECOGNITION_KOREAN = "Text Recognition Korean"
        private const val FACE_MESH_DETECTION = "Face Mesh Detection (Beta)"

        private const val STATE_SELECTED_MODEL = "selected_model"
    }
}