package com.yanzubritskiy.mltest.ui.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions.LATEST_MODEL
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionLabel
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions
import com.yanzubritskiy.mltest.getAssetFiles
import java.io.IOException


class ImageLabelingViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableImages = MutableLiveData<List<Uri>>()
    private val mutableResult = MutableLiveData<String>()
    private val mutableError = MutableLiveData<String>()
    private val mutableIsProcessing = MutableLiveData<Boolean>()
    private val images: List<Uri>

    init {
        images = getImagesFromAssets(application)
        mutableImages.postValue(images)
    }

    val isProcessing: LiveData<Boolean> = mutableIsProcessing
    val error: LiveData<String> = mutableError

    fun getImages(): LiveData<List<Uri>> {
        return mutableImages
    }

    fun getResult(): LiveData<String> {
        return mutableResult
    }

    @Throws(IOException::class)
    private fun getImagesFromAssets(context: Context): List<Uri> {
        val result = ArrayList<Uri>()
        val files = context.getAssetFiles("photos_labeling")
        files?.run {
            result.addAll(this)
        }
        val files2 = context.getAssetFiles("photos")
        files2?.run {
            result.addAll(this)
        }
        return result
    }

    fun recognizeLabelsOnDevise(bitmap: Bitmap) {
        val options = FirebaseVisionLabelDetectorOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()

        val detector = FirebaseVision.getInstance()
            .getVisionLabelDetector(options)
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.detectInImage(image)
            .addOnSuccessListener {
                if (it.isNullOrEmpty()) mutableError.postValue("No labels recognized")
                else processLabels(it)
            }
            .addOnFailureListener {
                mutableError.postValue("Error detecting: " + it.message)
            }
    }

    private fun processLabels(
        labels: MutableList<FirebaseVisionLabel>
    ) {
        val builder = StringBuilder()
        labels.forEachIndexed { i, label ->
            val text = label.label
            val entityId = label.entityId
            val confidence = label.confidence
            builder.append(text)
            if (i < (labels.size - 1)) builder.append(", ")
        }
        mutableResult.postValue(builder.toString())
    }

    fun recognizeLabelsOnCloud(bitmap: Bitmap) {
        val options = FirebaseVisionCloudDetectorOptions.Builder()
            .setMaxResults(15)
            .setModelType(LATEST_MODEL)
            .build()

        val detector = FirebaseVision.getInstance()
            .getVisionCloudLabelDetector(options)

        val image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.detectInImage(image)
            .addOnSuccessListener {
                if (it.isNullOrEmpty()) mutableError.postValue("No labels recognized")
                else processCloudLabels(it)
            }
            .addOnFailureListener {
                mutableError.postValue("Error detecting: " + it.message)
            }
    }

    private fun processCloudLabels(labels: List<FirebaseVisionCloudLabel>) {
        val builder = StringBuilder()
        labels.forEachIndexed { i, label ->
            val text = label.label
            val entityId = label.entityId
            val confidence = label.confidence
            builder.append(text)
            if (i < (labels.size - 1)) builder.append(", ")
        }
        mutableResult.postValue(builder.toString())    }
}