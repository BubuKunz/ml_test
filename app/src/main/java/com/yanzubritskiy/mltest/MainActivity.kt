package com.yanzubritskiy.mltest

import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.yanzubritskiy.mltest.ui.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity: AppCompatActivity() {

    private val samplesMap = mapOf(
        "Text recognition" to TextRecognitionFragment.getInstance(),
        "Face detection" to FaceDetectionFragment.getInstance(),
        "Barcode scan" to BarCodeScanFragment.getInstance(),
        "Image labeling" to ImageLabelingFragment.getInstance(),
        "Landmark Recognition" to LandmarkRecognizingFragment.getInstance()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        for ((key, value) in samplesMap) {
            val button = Button(this)
            button.text = key
            button.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            button.setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.containerLayout, value)
                    .addToBackStack(value::class.java.simpleName)
                    .commit()
            }
            contentLayout.addView(button)
        }
    }
}