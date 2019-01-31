package com.yanzubritskiy.mltest.kotlin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yanzubritskiy.mltest.R
import kotlinx.android.synthetic.main.fragment_upload_photo_dialog.view.*
import java.io.File
import java.io.IOException


class UploadPhotoDialogFragment : BottomSheetDialogFragment() {
    private var photoUri: Uri? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_upload_photo_dialog, container, false)
        view.cameraLayout.setOnClickListener { onMakePhotoClick() }
        view.collectionsLayout.setOnClickListener { onUploadPhotoFromGalleryClick() }
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(IMAGE_URI_KEY, photoUri)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        photoUri = savedInstanceState?.getParcelable(IMAGE_URI_KEY)
    }

    private fun onMakePhotoClick() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            val packageManager = activity?.packageManager ?: return@also
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val nonNullActivity = activity ?: return@also
                val photoFile: File? = try {
                    createImageFile(PHOTO_FILE_NAME, nonNullActivity)
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e(TAG, ex.toString())
                    //TODO 29.11.2018 show some error to user
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also { file ->
                    photoUri = FileProvider.getUriForFile(
                        nonNullActivity,
                        "com.yanzubritskiy.mltest.fileprovider",
                        file
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    private fun onUploadPhotoFromGalleryClick() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also {
            startActivityForResult(it, REQUEST_UPLOAD_PHOTO_FROM_GALLERY)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            REQUEST_UPLOAD_PHOTO_FROM_GALLERY -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let {
                        (activity as TestActivity).setImageUri(it)
                    }
                }
                dismiss()
            }
            REQUEST_TAKE_PHOTO -> {
                if (resultCode == Activity.RESULT_OK) {
                    photoUri?.let {
                        (activity as TestActivity).setImageUri(it)
                    }
                }
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "UploadPhotoDialogFrag"

        private const val REQUEST_UPLOAD_PHOTO_FROM_GALLERY = 0
        private const val REQUEST_TAKE_PHOTO = 1
        private const val PHOTO_FILE_NAME = "user_pic"
        private const val IMAGE_URI_KEY = "image_key"

        fun getInstance(): UploadPhotoDialogFragment {
            return UploadPhotoDialogFragment()
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(name: String, context: Context): File {
        // Create an image file name
        return File.createTempFile(
            name,
            ".jpg",
            context.cacheDir
        )
    }

    private fun getTempImageFileUri(name: String): Uri? {
        activity?.run {

            val photoFile: File? = try {
                createImageFile(name, this)
            } catch (ex: IOException) {
                null
            }
            // Continue only if the File was successfully created
            photoFile?.also { file ->
                return FileProvider.getUriForFile(
                    this,
                    "com.client.marvel.fileprovider",
                    file
                )
            }
        }
        return null
    }
}