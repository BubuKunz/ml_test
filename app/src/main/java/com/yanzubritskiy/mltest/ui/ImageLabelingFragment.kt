package com.yanzubritskiy.mltest.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.yanzubritskiy.mltest.R
import com.yanzubritskiy.mltest.ui.viewmodels.ImageLabelingViewModel
import kotlinx.android.synthetic.main.fragment_image_labeling.*

class ImageLabelingFragment : Fragment() {
    private var progressDialog: ProgressDialog? = null
    private val imagesAdapter: ImagesListAdapter by lazy {
        ImagesListAdapter(Glide.with(this))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_image_labeling, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = ViewModelProviders.of(this).get(ImageLabelingViewModel::class.java)
        imagesRecyclerView.adapter = imagesAdapter
        imagesAdapter.run {
            setImageClickListener(object : ImagesListAdapter.ImageClickListener {
                override fun onClick(image: Bitmap, position: Int) {
                    viewModel.recognizeLabelsOnDevise(image)
                }

            })
            setImageLongClickListener(object : ImagesListAdapter.ImageLongClickListener {
                override fun onLongClick(image: Bitmap, position: Int) {
                    viewModel.recognizeLabelsOnCloud(image)
                }

            })
        }

        viewModel.getImages().observe(this,
            Observer<List<Uri>> { images ->
                imagesAdapter.setImageUris(images)
            })

        viewModel.error.observe(this,
            Observer {
                activity?.run {
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                }
            })

        viewModel.isProcessing.observe(this,
            Observer {
                progressDialog?.dismiss()
                if (it) showProcessingDialog()
            })

        viewModel.getResult().observe(this,
            Observer {
                showAlertDialog(it)
            })
    }

    private fun showAlertDialog(it: String) {
        activity?.run {
            val dialog = AlertDialog.Builder(context)
                .setMessage(it)
                .create()
            dialog.show()
        }
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

    companion object {
        fun getInstance(): ImageLabelingFragment {
            return ImageLabelingFragment()
        }
    }
}