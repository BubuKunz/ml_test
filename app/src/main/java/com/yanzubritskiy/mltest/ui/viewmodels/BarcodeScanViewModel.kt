package com.yanzubritskiy.mltest.ui.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.SparseArray
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.yanzubritskiy.mltest.getAssetFiles
import java.io.IOException


class BarcodeScanViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableImages = MutableLiveData<List<Uri>>()
    private val mutableResult = MutableLiveData<String>()
    private val mutableIsProcessing = MutableLiveData<Boolean>()
    private var uris = emptyList<Uri>()
    private val imageNames = SparseArray<String>()


    private fun postImages(images: List<Uri>) {
        images?.forEachIndexed { i, url ->
            imageNames.append(i, getFileName(url))
        }
        mutableImages.postValue(images)
    }


    private fun loadImages() =
        getImagesFromAssets(getApplication())


    fun getFileName(uri: Uri): String {
        var result = uri.path
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result.substring(cut + 1)
        }
        return result
    }

    val images: LiveData<List<Uri>> = mutableImages

    fun getResult(): LiveData<String> {
        return mutableResult
    }

    @Throws(IOException::class)
    private fun getImagesFromAssets(context: Context): List<Uri> {
        val result = ArrayList<Uri>()
        val files = context.getAssetFiles("barcodes")
        files?.run {
            result.addAll(this)
        }
        return result
    }

    fun scanBarcode(bitmap: Bitmap?, position: Int) {
        bitmap?.run {
            val image = FirebaseVisionImage.fromBitmap(this)
            val detector = FirebaseVision.getInstance()
                .visionBarcodeDetector

            val result = detector.detectInImage(image)
                .addOnSuccessListener { barcodes ->
                    mutableResult.postValue(processBarcodesResult(barcodes, position))
                }
                .addOnFailureListener {
                    // Task failed with an exception
                    // ...
                }

        }
    }


    private fun processBarcodesResult(barcodes: List<FirebaseVisionBarcode>, position: Int): String {
        val result = StringBuilder()
        try {
            result.append("File: ${imageNames[position]}\n")
        } catch (e: IndexOutOfBoundsException) {
            //ignore
        }
        result.append("Barcodes found: ${barcodes.size}")
        for (barcode in barcodes) {
            processBarcode(barcode)?.run {
                result.append("\n")
                result.append(this)
            }
        }
        return result.toString()
    }

    private fun processBarcode(barcode: FirebaseVisionBarcode): String? {
        val bounds = barcode.boundingBox
        val corners = barcode.cornerPoints
        val rawValue = barcode.rawValue
        val valueType = barcode.valueType
        // See API reference for complete list of supported types
        return when (valueType) {
            FirebaseVisionBarcode.TYPE_WIFI -> {
                val ssid = barcode.displayValue
                val password = barcode.wifi!!.password
                val type = barcode.wifi!!.encryptionType
                "TYPE_WIFI: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_URL -> {
                val title = barcode.url!!.title
                val url = barcode.url!!.url
                "TYPE_URL: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_CALENDAR_EVENT -> {
                "TYPE_CALENDAR_EVENT: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_CONTACT_INFO -> {
                "TYPE_CONTACT_INFO: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_SMS -> {
                "TYPE_SMS: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_DRIVER_LICENSE -> {
                "TYPE_DRIVER_LICENSE: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_GEO -> {
                "TYPE_GEO: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_ISBN -> {
                "TYPE_ISBN: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_PHONE -> {
                "TYPE_PHONE: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_TEXT -> {
                "TYPE_TEXT: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_EMAIL -> {
                "TYPE_EMAIL: " +barcode.displayValue
            }
            FirebaseVisionBarcode.TYPE_UNKNOWN -> {
                "TYPE_UNKNOWN: " +barcode.displayValue
            }
            else -> barcode.displayValue
        }
    }

    fun start() {
        uris = loadImages()
        postImages(uris)
    }
}