package com.yanzubritskiy.mltest.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.Target
import com.yanzubritskiy.mltest.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ImagesListAdapter(private val requestManager: RequestManager) :
    RecyclerView.Adapter<ImagesListAdapter.ImageViewHolder>() {
    val images = mutableListOf<Bitmap>()
    private var imageClickListener: ImageClickListener? = null
    private var imageLongClickListener: ImageLongClickListener? = null

    fun setImageClickListener(imageClickListener: ImageClickListener) {
        this.imageClickListener = imageClickListener
    }

    fun setImageLongClickListener(imageLongClickListener: ImageLongClickListener) {
        this.imageLongClickListener = imageLongClickListener
    }


    interface ImagesLoadedCallBack {
        fun onImagesLoaded(count: Int)
    }

    interface ImageClickListener {
        fun onClick(image: Bitmap, position: Int)
    }

    interface ImageLongClickListener {
        fun onLongClick(image: Bitmap, position: Int)
    }

    fun setImages(images: List<Bitmap>?) {
        this.images.clear()
        images?.let { this.images.addAll(0, it) }
        notifyDataSetChanged()
    }

    fun setImageUris(uris: List<Uri>?, callBack: ImagesLoadedCallBack? = null) {
        images.clear()
        notifyDataSetChanged()
        GlobalScope.launch(Dispatchers.Main) {
            uris?.forEachIndexed { index, uri ->
                withContext(Dispatchers.IO) {
                    val bitmap = requestManager.asBitmap()
                        .load(uri)
                        .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)// Width and height
                        .get()
                    images.add(bitmap)
                }
                if (index == uris.size - 1) {
                    callBack?.onImagesLoaded(images.size)
                }
            } ?: run {
                callBack?.onImagesLoaded(0)

            }
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(parent.createImageView())
    }

    override fun getItemCount() = images.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], imageClickListener, imageLongClickListener)
    }

    private fun View.createImageView(): ImageView {
        val imageView = ImageView(context)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.topMargin = resources.getDimension(R.dimen.activity_vertical_margin).toInt()
        imageView.layoutParams = layoutParams
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_XY
        return imageView
    }

    fun replace(position: Int, image: Bitmap) {
        images.removeAt(position)
        images.add(position, image)
        notifyDataSetChanged()
    }

    class ImageViewHolder(private val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {

        fun bind(
            image: Bitmap,
            imageClickListener: ImageClickListener?,
            imageLongClickListener: ImageLongClickListener?
        ) {
            imageView.setImageBitmap(image)
            imageClickListener?.let { clickListener ->
                imageView.setOnClickListener { clickListener.onClick(image, adapterPosition) }
            }
            imageLongClickListener?.let { listener ->
                imageView.setOnLongClickListener {
                    val vibe = it.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibe.vibrate(50)
                    listener.onLongClick(image, adapterPosition)
                    return@setOnLongClickListener true
                }
            }
        }
    }
}