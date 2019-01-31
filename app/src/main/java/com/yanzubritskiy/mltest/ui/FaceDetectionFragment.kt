package com.yanzubritskiy.mltest.ui

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.yanzubritskiy.mltest.R
import com.yanzubritskiy.mltest.ui.viewmodels.FaceRecognitionViewModel
import kotlinx.android.synthetic.main.fragment_face_detection.*

class FaceDetectionFragment : Fragment() {
    private var progressDialog: ProgressDialog? = null

    private val imagesAdapter: ImagesListAdapter by lazy {
        ImagesListAdapter(Glide.with(this))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_face_detection, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProviders.of(this).get(FaceRecognitionViewModel::class.java)
        imagesRecyclerView.adapter = imagesAdapter
        imagesAdapter.setImageClickListener(object : ImagesListAdapter.ImageClickListener {
            override fun onClick(image: Bitmap, postion: Int) {
                setButtonsState(false)
                viewModel.findSameFace(image, imagesAdapter.images)            }

        })
        viewModel.getImages().observe(this,
            Observer<List<Uri>> { images ->
                imagesAdapter.setImageUris(images, object : ImagesListAdapter.ImagesLoadedCallBack {
                    override fun onImagesLoaded(count: Int) {
                        setButtonsState(true)
                    }
                })
            })
        viewModel.getResultImages().observe(this,
            Observer {
                resetButton.isEnabled = true
                imagesAdapter.setImages(it)
            })
        resetButton.setOnClickListener {
            viewModel.reset()
        }
        filterFacesButton.setOnClickListener {
            resetButton.isEnabled = true
            setButtonsState(false)
            viewModel.processImages(imagesAdapter.images)
        }


        viewModel.isProcessing.observe(this,
            Observer {
                progressDialog?.dismiss()
                if (it) showProcessingDialog()
            })
    }

    private fun showProcessingDialog() {
        progressDialog = ProgressDialog(activity)
        progressDialog?.run {
            isIndeterminate = true
            setTitle("Processing ")
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            isIndeterminate = true
            show()
        }
    }

    private fun setButtonsState(state: Boolean) {
        findSameFace.isEnabled = state
        filterFacesButton.isEnabled = state
    }
    companion object {
        fun getInstance(): FaceDetectionFragment {
            return FaceDetectionFragment()
        }
    }
}