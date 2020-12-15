package com.duzhaokun123.galleryview

import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

fun PhotoView.setImageBitmap(bm: Bitmap, @GalleryView.ScaleMode scaleMode: Int) {
    val tag = "PhotoView"
    this.scaleType = ImageView.ScaleType.CENTER
    this.setImageBitmap(bm)
    GlobalScope.launch(Dispatchers.Main) {
        delay(80)
        val scale =
                when (scaleMode) {
                    GalleryView.SCALE_FIT ->  {
                        val xScale = this@setImageBitmap.measuredWidth / bm.width.toFloat()
                        val yScale = this@setImageBitmap.measuredHeight / bm.height.toFloat()
                        min(xScale, yScale)
                    }
//                    GalleryView.SCALE_FIXED -> 1F
                    GalleryView.SCALE_ORIGIN -> 1F
                    GalleryView.SCALE_FIT_WIDTH -> this@setImageBitmap.measuredWidth / bm.width.toFloat()
                    GalleryView.SCALE_FIT_HEIGHT -> this@setImageBitmap.measuredHeight / bm.height.toFloat()
                    else -> {
                        Log.w(tag, "setImageBitmap: unknown scaleMode $scaleMode")
                        1F
                    }
                }
        Log.d(tag, "setImageBitmap: scale $scale")
        try {
            this@setImageBitmap.scale = scale
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}