package com.yanzubritskiy.mltest.kotlin

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.vision.text.TextRecognizer
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.document.FirebaseVisionCloudDocumentRecognizerOptions
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText
import com.yanzubritskiy.mltest.R
import kotlinx.android.synthetic.main.activity_test.*
import java.io.IOException
import java.util.*

class TestActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        floatingActionButton.setOnClickListener {
            UploadPhotoDialogFragment.getInstance().show(supportFragmentManager, UploadPhotoDialogFragment.TAG)
        }
    }

    fun setImageUri(imgUri: Uri) {
        imageView.setImageURI(imgUri)
//        val bitmap: Bitmap = (imageView.drawable as BitmapDrawable).bitmap
//        val image = FirebaseVisionImage.fromBitmap(bitmap)
//        val metadata = FirebaseVisionImageMetadata.Builder()
//            .setWidth(480)   // 480x360 is typically sufficient for
//            .setHeight(360)  // image recognition
//            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
//            .build()
        val image: FirebaseVisionImage
        try {
            image = FirebaseVisionImage.fromFilePath(this, imgUri)
            val options = FirebaseVisionCloudDocumentRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("uk"))
                .build()
            val detector = FirebaseVision.getInstance()
                .getCloudDocumentTextRecognizer(options)
            val result: Task<FirebaseVisionDocumentText> = detector.processImage(image)
                .addOnSuccessListener { firebaseVisionText ->
                    // Task completed successfully
                    for (block in firebaseVisionText.blocks) {

                        val blockText = block.text
                        val blockConfidence = block.confidence
                        val blockLanguages = block.recognizedLanguages
                        val blockFrame = block.boundingBox
                        for (line in block.paragraphs) {
                            val lineText = line.text
                            val lineConfidence = line.confidence
                            val lineLanguages = line.recognizedLanguages
//                            val lineCornerPoints = line.cornerPoints
                            val lineFrame = line.boundingBox
                            for (element in line.words) {
                                val elementText = element.text
                                val elementConfidence = element.confidence
                                val elementLanguages = element.recognizedLanguages
//                                val elementCornerPoints = element.cornerPoints
                                val elementFrame = element.boundingBox
                            }
                        }
                    }
                    val resultText = firebaseVisionText.text
                    resultTextView.text = resultText
                    // ...
                }
                .addOnFailureListener {
                    resultTextView.text = it.message

                    // Task failed with an exception
                    // ...
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}