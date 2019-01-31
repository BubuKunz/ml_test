package com.yanzubritskiy.mltest.ui.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.util.SparseArray
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.yanzubritskiy.mltest.getAssetFiles
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.asDeferred
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class FaceRecognitionViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableImages = MutableLiveData<List<Uri>>()
    private val mutableError = MutableLiveData<String>()
    private val mutableResultImages = MutableLiveData<List<Bitmap>>()
    private val mutableIsProcessing = MutableLiveData<Boolean>()
    private val images: List<Uri>
    private var detector: FirebaseVisionFaceDetector? = null
    private var job: Job? = null

    val errorEvent: LiveData<String> = mutableError

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

    companion object {
        private const val TAG = "FaceRecognitionResult"
    }

    val map = HashMap<Int, MutableList<Bitmap>>()

    private fun processFaceRecognitionResult(
        faces: MutableList<FirebaseVisionFace>,
        index: Int,
        bitmap: Bitmap
    ): Boolean {
        Log.d(TAG, "image №$index:")
        faces.forEach {
            Log.d(TAG, "headEulerAngleY = " + it.headEulerAngleY)
            Log.d(TAG, "headEulerAngleZ = " + it.headEulerAngleZ)
            Log.d(TAG, "leftEyeOpenProbability = " + it.leftEyeOpenProbability)
            Log.d(TAG, "rightEyeOpenProbability = " + it.rightEyeOpenProbability)
            Log.d(TAG, "smilingProbability = " + it.smilingProbability)
            Log.d(TAG, "trackingId = " + it.trackingId)
            map[it.trackingId]?.run {
                add(bitmap)
            } ?: run {
                val images = mutableListOf<Bitmap>().apply { add(bitmap) }
                map.put(it.trackingId, images)
            }
        }
        return faces.size > 0
    }


    private fun looksLikeHandle(text: String) =
        text.toLowerCase().contains("itomy")


    private fun handleSuccessResult(
        faces: MutableList<FirebaseVisionFace>,
        resultList: ArrayList<Bitmap>,
        bitmap: Bitmap,
        counter: Int,
        images: List<Bitmap>
    ) {
        if (processFaceRecognitionResult(faces, counter, bitmap)) {
            resultList.add(bitmap)
        }
        handleAnyResult(counter, images, resultList)
    }

    private fun handleAnyResult(
        counter: Int,
        images: List<Bitmap>,
        resultList: ArrayList<Bitmap>
    ) {
        if (counter == images.size) {
            mutableIsProcessing.postValue(false)
            mutableResultImages.postValue(resultList)
            var sameFacesCounter = 0
            for ((trackingId, sameFaceImages) in map) {
                if (sameFaceImages.size > 1) {
                    sameFacesCounter++
                    Log.d(
                        TAG, "trackingId = $trackingId, imagesSize = " +
                                sameFaceImages.size + "sameFacesCounter = $sameFacesCounter"
                    )
                }
            }

        }
    }

    @Throws(IOException::class)
    private fun getImagesFromAssets(context: Context): List<Uri> {
        val result = ArrayList<Uri>()
        val files = context.getAssetFiles("photos")
        files?.run {
            result.addAll(this)
        }
        return result
    }

    fun reset() {
        detector?.close()
        job?.cancel()
        mutableImages.postValue(images)
    }

    fun processImages(images: MutableList<Bitmap>) {
        detector?.close()
        mutableIsProcessing.postValue(true)
        val resultList = ArrayList<Bitmap>()
        var counter = 0
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setMinFaceSize(0.4f)
            .enableTracking()
            .build()
        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(options)
        this.detector = detector
        job = GlobalScope.launch {
            withContext(Dispatchers.Main) {
                images.forEach {
                    val image = FirebaseVisionImage.fromBitmap(it)
                    val detectorTask = detector.detectInImage(image)
                    try {
                        val recognizedFaces = detectorTask.asDeferred().await()
                        counter++
                        handleSuccessResult(recognizedFaces, resultList, it, counter, images)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        counter++
                        handleAnyResult(counter, images, resultList)
                    } finally {
                        detector.close()
                    }
                }
            }
        }
    }

    /**
     * image should contain only one face
     */
    fun findSameFace(photo: Bitmap, images: MutableList<Bitmap>) {
        detector?.close()
        mutableIsProcessing.postValue(true)
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .enableTracking()
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setMinFaceSize(0.4f)
            .build()
        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(options)
        this.detector = detector
        job = GlobalScope.launch {
            withContext(Dispatchers.Main) {
                val image = FirebaseVisionImage.fromBitmap(photo)
                val detectorTask = detector.detectInImage(image)

                try {
                    val recognizedFaces = detectorTask.asDeferred().await()
                    if (recognizedFaces.size < 1) {
                        throw IllegalArgumentException("No face found")
                    }
                    val trackId = recognizedFaces[0].trackingId
                    val sameImages = getSameFaceImages(trackId, images, detector)
                    mutableIsProcessing.postValue(false)
                    mutableResultImages.postValue(sameImages)
                } catch (e: Exception) {
                    e.printStackTrace()
                    mutableError.postValue("Error face recognizing")
                } finally {
                    detector.close()
                }
            }
        }
    }

    private suspend fun getSameFaceImages(
        faceId: Int,
        images: List<Bitmap>,
        detector: FirebaseVisionFaceDetector
    ): List<Bitmap> {
        val resultImages = GlobalScope.async(Dispatchers.IO) {
            val result = ArrayList<Bitmap>()

            images.forEach {
                val image = FirebaseVisionImage.fromBitmap(it)
                val detectorTask = detector.detectInImage(image)
                try {
                    val recognizedFaces = detectorTask.asDeferred().await()
                    if (containsSameFace(faceId, recognizedFaces)) {
                        result.add(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()

                } finally {
                    detector.close()
                }
            }
            return@async result
        }
        return resultImages.await()
    }

    private fun containsSameFace(faceId: Int, faces: List<FirebaseVisionFace>): Boolean {
        faces.forEach {
            if (it.trackingId == faceId) return true
        }
        return false
    }
}