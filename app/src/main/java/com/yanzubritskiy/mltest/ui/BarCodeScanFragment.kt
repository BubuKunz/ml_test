package com.yanzubritskiy.mltest.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.media.SoundPool
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
import com.yanzubritskiy.mltest.ui.viewmodels.BarcodeScanViewModel
import kotlinx.android.synthetic.main.fragment_bar_code_scanning.*

class BarCodeScanFragment : Fragment() {
    private var progressDialog: ProgressDialog? = null

    private val imagesAdapter: ImagesListAdapter by lazy {
        ImagesListAdapter(Glide.with(this))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_bar_code_scanning, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProviders.of(this).get(BarcodeScanViewModel::class.java)
        viewModel.start()
        imagesRecyclerView.adapter = imagesAdapter
        imagesAdapter.setImageClickListener(object : ImagesListAdapter.ImageClickListener {
            override fun onClick(image: Bitmap, position: Int) {
                viewModel.scanBarcode(image, position)
            }
        })
        viewModel.images.observe(this,
            Observer<List<Uri>> { images ->
                showProcessingDialog()
                imagesAdapter.setImageUris(images, object : ImagesListAdapter.ImagesLoadedCallBack {
                    override fun onImagesLoaded(count: Int) {
                        progressDialog?.dismiss()
                    }
                })
            })
        viewModel.getResult().observe(this,
            Observer {
                activity?.run {
                    AlertDialog.Builder(this)
                        .setMessage(it).show()
                }
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

    companion object {
        fun getInstance(): BarCodeScanFragment {
            return BarCodeScanFragment()
        }
    }
}