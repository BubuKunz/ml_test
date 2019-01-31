package com.yanzubritskiy.mltest.ui

import android.app.ProgressDialog
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
import com.yanzubritskiy.mltest.ui.viewmodels.TextRecognitionViewModel
import kotlinx.android.synthetic.main.fragment_text_recogntion.*
import kotlinx.android.synthetic.main.fragment_text_recogntion.view.*

class TextRecognitionFragment : Fragment() {
    private var progressDialog: ProgressDialog? = null
    private val imagesAdapter: ImagesListAdapter by lazy {
        ImagesListAdapter(Glide.with(this@TextRecognitionFragment))
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_text_recogntion, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProviders.of(this).get(TextRecognitionViewModel::class.java)
        imagesRecyclerView.adapter = imagesAdapter
        viewModel.getImages().observe(this,
            Observer<List<Uri>> { images ->
                buttonsState(false)
                imagesAdapter.setImageUris(images, object : ImagesListAdapter.ImagesLoadedCallBack {
                    override fun onImagesLoaded(count: Int) {
                        buttonsState(true)
                    }
                })
            })
        viewModel.getResultImages().observe(this,
            Observer {
                imagesAdapter.setImages(it)
            })
        view.resetButton.setOnClickListener {
            viewModel.reset()
        }
        view.onDeviceButton.setOnClickListener {
            view.resetButton.isEnabled = true
            buttonsState(false)
            viewModel.runTextRecognition(imagesAdapter.images, searchEditText.text.toString())
        }
        view.onCloudButton.setOnClickListener {
            view.resetButton.isEnabled = true
            buttonsState(false)
            viewModel.runCloudTextRecognition(imagesAdapter.images, searchEditText.text.toString())
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

    private fun buttonsState(state: Boolean) {
        onDeviceButton.isEnabled = state
        onCloudButton.isEnabled = state
    }

    companion object {
        fun getInstance(): TextRecognitionFragment {
            return TextRecognitionFragment()
        }
    }
}