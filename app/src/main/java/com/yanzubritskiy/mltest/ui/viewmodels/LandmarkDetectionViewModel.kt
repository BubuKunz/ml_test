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
import com.google.firebase.ml.vision.cloud.landmark.FirebaseVisionCloudLandmark
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.yanzubritskiy.mltest.getAssetFiles
import java.io.IOException


class LandmarkDetectionViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableImages = MutableLiveData<List<Uri>>()
    private val mutableResult = MutableLiveData<Pair<Int, Bitmap>>()
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

    fun getResult(): LiveData<Pair<Int, Bitmap>> {
        return mutableResult
    }

    @Throws(IOException::class)
    private fun getImagesFromAssets(context: Context): List<Uri> {
        val result = ArrayList<Uri>()
        val files = context.getAssetFiles("photos_landmark")
        files?.run {
            result.addAll(this)
        }
        return result
    }

    fun recognizeLandmarks(bitmap: Bitmap, position: Int) {
        val options = FirebaseVisionCloudDetectorOptions.Builder()
            .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
            .setMaxResults(15)
            .build()

        val detector = FirebaseVision.getInstance()
            .getVisionCloudLandmarkDetector(options)
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.detectInImage(image)
            .addOnSuccessListener {
                if (it.isNullOrEmpty()) mutableError.postValue("No landmark recognized")
                else processLandmarks(it, position, bitmap)
            }
            .addOnFailureListener {
                mutableError.postValue("Error detecting: " + it.message)
            }
    }

    private fun processLandmarks(
        landmarks: List<FirebaseVisionCloudLandmark>,
        position: Int,
        image: Bitmap
    ) {
        val builder = StringBuilder()
        var resultBitmap: Bitmap? = null
        landmarks.forEach { landmark ->
            val bounds = landmark.boundingBox
            val landmarkName = landmark.landmark
            val entityId = landmark.entityId
            val confidence = landmark.confidence
            builder.append("\n")
            builder.append(landmarkName)
            builder.append("(")
            // Multiple locations are possible, e.g., the location of the depicted
            // landmark and the location the picture was taken.

            resultBitmap = if (resultBitmap == null) {
                drawTextToBitmap(image, 78, landmarkName, bounds)
            } else {
                drawTextToBitmap(resultBitmap!!, 78, landmarkName, bounds)
            }

            for (loc in landmark.locations) {
                val latitude = loc.latitude
                val longitude = loc.longitude
                builder.append("LatLng=$latitude: $longitude")
            }
            builder.append(") rect=( $bounds)")
        }
        resultBitmap?.let {
            mutableResult.postValue(Pair(position, it))
        }
//        mutableResult.postValue(builder.toString())
    }

    fun reset() {
        mutableImages.postValue(images)
    }

    private fun drawTextToBitmap(sourceImage: Bitmap, textSize: Int = 78, text1: String, rect: Rect?): Bitmap {
        var bitmap = sourceImage
        val point = Point(rect?.centerX() ?: 0, rect?.centerY() ?: 0)
        var bitmapConfig = bitmap.config;
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bitmap = bitmap.copy(bitmapConfig, true)

        val canvas = Canvas(bitmap)
        // new antialised Paint
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        // text size in pixels
        val scale = (sourceImage.width / 1080f)
        paint.textSize = (textSize * scale)
        //custom fonts
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        // draw text to the Canvas center
        val bounds = Rect()
        //draw the first text
        paint.getTextBounds(text1, 0, text1.length, bounds)
        var x = point.x -( bounds.width() / 2f)
        var y = point.y +( bounds.height() / 2f)
        canvas.drawText(text1, x, y, paint)

        val strokePaint = Paint()
        strokePaint.color = Color.WHITE
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 10f
        canvas.drawRect(rect, strokePaint)

        return bitmap
    }
}