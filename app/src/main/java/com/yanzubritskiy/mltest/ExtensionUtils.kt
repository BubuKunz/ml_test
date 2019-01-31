package com.yanzubritskiy.mltest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri


private const val ASSETS_PATH = "file:///android_asset/"

fun Context.getAssetFiles(folderName: String): List<Uri>? {
    val assetManager = assets
    val files = assetManager.list(folderName)
    return files?.map {
        Uri.parse("$ASSETS_PATH$folderName/$it")
    }
}



fun Drawable.drawableToBitmap(): Bitmap? {
    var bitmap: Bitmap? = null

    if (this is BitmapDrawable) {
        if (bitmap != null) {
            return bitmap
        }
    }

    bitmap = if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
        Bitmap.createBitmap(
            1,
            1,
            Bitmap.Config.ARGB_8888
        ) // Single color bitmap will be created of 1x1 pixel
    } else {
        Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    }

    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}