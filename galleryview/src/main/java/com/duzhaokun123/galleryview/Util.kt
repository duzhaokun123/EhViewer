package com.duzhaokun123.galleryview

import android.util.Log
import android.widget.ImageView

fun Int.toImageViewScaleType(): ImageView.ScaleType {
    return when (this) {
        GalleryView.SCALE_ORIGIN -> ImageView.ScaleType.CENTER
//        GalleryView.SCALE_FIT_WIDTH ->
//        GalleryView.SCALE_FIT_HEIGHT ->
        GalleryView.SCALE_FIT -> ImageView.ScaleType.FIT_CENTER
        GalleryView.SCALE_FIXED -> ImageView.ScaleType.CENTER_INSIDE
        else -> {
            Log.w("ScaleMode", "cannot solve $this, use FIT_CENTER as default")
            ImageView.ScaleType.FIT_CENTER
        }
    }
}