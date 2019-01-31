package com.yanzubritskiy.mltest.ui.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions.SPARSE_MODEL
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.yanzubritskiy.mltest.getAssetFiles
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class TextRecognitionViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableImages = MutableLiveData<List<Uri>>()
    private val mutableResultImages = MutableLiveData<List<Bitmap>>()
    private val mutableIsProcessing = MutableLiveData<Boolean>()
    private val images: List<Uri>

    init {
        images = getImagesFromAssets(application)
        mutableImages.postValue(images)
    }


    fun getImages(): LiveData<List<Uri>> {
        return mutableImages
    }

    fun getResultImages(): LiveData<List<Bitmap>> {
        return mutableResultImages
    }

    val isProcessing: LiveData<Boolean> = mutableIsProcessing

    private fun processTextRecognitionResult(texts: FirebaseVisionText, searchString: String): Boolean {
        val blocks = texts.textBlocks
        if (blocks.size == 0) {
            return false
        }
        blocks.forEach { block ->
            block.lines.forEach { line ->
                line.elements.forEach { element ->
                    if (looksLikeHandle(element.text, searchString)) {
                        return true
                    }
                }
            }
        }
        return false
    }


    private fun looksLikeHandle(text: String, searchString: String) =
        text.toLowerCase().contains(searchString)


    fun runTextRecognition(images: List<Bitmap>, searchString: String) {
        val resultList = ArrayList<Bitmap>()
        var counter = 0
        mutableIsProcessing.postValue(true)
        images.forEach {
            it.let { bitmap ->
                val image = FirebaseVisionImage.fromBitmap(bitmap)
                val detector = FirebaseVision.getInstance()
                    .onDeviceTextRecognizer
                detector.processImage(image)
                    .addOnSuccessListener { texts ->
                        counter++
                        handleSuccessResult(texts, resultList, bitmap, counter, images, searchString)
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        e.printStackTrace()
                        counter++
                        handleAnyResult(counter, images, resultList)
                    }
            }
        }
    }

    private fun handleSuccessResult(
        texts: FirebaseVisionText,
        resultList: ArrayList<Bitmap>,
        bitmap: Bitmap,
        counter: Int,
        images: List<Bitmap>,
        searchString: String
    ) {
        if (processTextRecognitionResult(texts, searchString)) {
            resultList.add(bitmap)
        }
        handleAnyResult(counter, images, resultList)
    }


    fun runCloudTextRecognition(images: List<Bitmap>, searchString: String) {
        mutableIsProcessing.postValue(true)
        val resultList = ArrayList<Bitmap>()
        var counter = 0
        val options = FirebaseVisionCloudTextRecognizerOptions.Builder()
            .setLanguageHints(Arrays.asList("en", "ru", "uk"))
            .setModelType(SPARSE_MODEL)
            .enforceCertFingerprintMatch()
            .build()
        val detector = FirebaseVision.getInstance()
            .getCloudTextRecognizer(options)
        images.forEach{
            val image = FirebaseVisionImage.fromBitmap(it)
            detector.processImage(image)
                .addOnSuccessListener { texts ->
                    counter++
                    handleSuccessResult(texts, resultList, it, counter, images, searchString)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    counter++
                    handleAnyResult(counter, images, resultList)
                }
        }
    }

    private fun handleAnyResult(
        counter: Int,
        images: List<Bitmap>,
        resultList: ArrayList<Bitmap>
    ) {
        if (counter == images.size) {
            mutableIsProcessing.postValue(false)
            mutableResultImages.postValue(resultList)
        }
    }

    @Throws(IOException::class)
    private fun getImagesFromAssets(context: Context): List<Uri> {
        val result = ArrayList<Uri>()
        var files: List<Uri>? = context.getAssetFiles("photos")
        files?.run {
            result.addAll(this)
        }
        files = context.getAssetFiles("photos_text")
        files?.run {
            result.addAll(this)
        }
        return result
    }

    fun reset() {
        mutableImages.postValue(images)
    }
}