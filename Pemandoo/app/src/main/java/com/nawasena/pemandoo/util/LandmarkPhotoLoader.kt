package com.nawasena.pemandoo.util

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.nawasena.pemandoo.R
import java.io.File

object LandmarkPhotoLoader {

    fun load(imageView: ImageView, photo: String?) {
        val model: Any? = when {
            photo.isNullOrBlank() -> null
            photo.startsWith("android.resource://") -> Uri.parse(photo)
            photo.startsWith("content://") -> Uri.parse(photo)
            photo.startsWith("file://") -> Uri.parse(photo)
            photo.startsWith("http://") || photo.startsWith("https://") -> photo
            File(photo).exists() -> File(photo)
            else -> null
        }

        Glide.with(imageView)
            .load(model ?: R.drawable.ic_image)
            .placeholder(R.drawable.ic_image)
            .error(R.drawable.ic_image)
            .centerCrop()
            .into(imageView)
    }
}
