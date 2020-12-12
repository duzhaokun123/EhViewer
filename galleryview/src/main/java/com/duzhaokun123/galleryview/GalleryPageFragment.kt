package com.duzhaokun123.galleryview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryPageFragment @JvmOverloads constructor(val galleryView: GalleryView? = null) : Fragment(R.layout.fragement_gallery_page) {
    private var tvPage: TextView? = null
    private var pb: ProgressBar? = null
    private var tvError: TextView? = null
    private var pv: PhotoView? = null

    var galleryViewListener: GalleryView.Listener? = null
    var onStopListener: (() -> Unit)? = null

    var isUsing = false
        private set

    var page = 0
        set(value) {
            field = value
            onSetPage()
        }

    var state = GalleryProvider.PageState.WAIT
        set(value) {
            field = value
            onSetState()
        }

    var error: String? = null
        set(value) {
            field = value
            onSetError()
        }

    var progress = 0
        set(value) {
            field = value
            onSetProgress()
        }

    var content: Bitmap? = null
        set(value) {
            field = value
            onSetContent()
        }

    var pageTextColor: Int = Color.GRAY
        set(value) {
            field = value
            onSetPageTextColor()
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isUsing = true
    }

    override fun onStop() {
        super.onStop()
        isUsing = false
        onStopListener?.invoke()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return super.onCreateView(inflater, container, savedInstanceState)!!.apply {
            tvPage = findViewById(R.id.tv_page)
            pb = findViewById(R.id.pb)
            tvError = findViewById(R.id.tv_error)
            pv = findViewById(R.id.pv)

            onSetState()
            onSetPage()
            onSetError()
            onSetProgress()
            onSetContent()
            onSetPageTextColor()

            tvError!!.setOnClickListener { galleryViewListener?.onTapErrorText(page - 1) }
            pv!!.setOnLongClickListener {
                galleryViewListener?.onLongPressPage(page - 1)
                true
            }
            pv!!.setOnViewTapListener { view, x, y ->
                val x0 = 0F
                val x1 = view.width / 3F
                val x2 = 2 * x1
                val x3 = view.width.toFloat()
                val y0 = 0F
                val y1 = view.height / 2F
                val y2 = view.height.toFloat()
                when(x) {
                    in x0..x1 -> galleryView?.pageLeft()
                    in x1..x2 -> {
                        when(y) {
                            in y0..y1 -> galleryViewListener?.onTapMenuArea()
                            in y1..y2 -> galleryViewListener?.onTapSliderArea()
                        }
                    }
                    in x2..x3 -> galleryView?.pageRight()
                }
            }
        }
    }

    private fun onSetPage() {
        tvPage?.let {
            GlobalScope.launch(Dispatchers.Main) {
                it.text = page.toString()
            }
        }
    }

    private fun onSetState() {
        GlobalScope.launch(Dispatchers.Main) {
            when (state) {
                GalleryProvider.PageState.WAIT -> {
                    tvPage?.visibility = View.VISIBLE
                    pb?.visibility = View.VISIBLE
                    pb?.isIndeterminate = true
                    tvError?.visibility = View.GONE
                    pv?.visibility = View.GONE
                }
                GalleryProvider.PageState.LOADING -> {
                    tvPage?.visibility = View.VISIBLE
                    pb?.visibility = View.VISIBLE
                    pb?.isIndeterminate = false
                    tvError?.visibility = View.GONE
                    pv?.visibility = View.GONE
                }
                GalleryProvider.PageState.READY -> {
                    tvPage?.visibility = View.GONE
                    pb?.visibility = View.GONE
                    tvError?.visibility = View.GONE
                    pv?.visibility = View.VISIBLE
                }
                GalleryProvider.PageState.ERROR -> {
                    tvPage?.visibility = View.VISIBLE
                    pb?.visibility = View.GONE
                    tvError?.visibility = View.VISIBLE
                    pv?.visibility = View.GONE
                }
            }
        }
    }

    private fun onSetError() {
        tvError?.let {
            GlobalScope.launch(Dispatchers.Main) {
                it.text = error
            }
        }
    }

    private fun onSetProgress() {
        pb?.let {
            GlobalScope.launch(Dispatchers.Main) {
                it.setProgress(progress, true)
            }
        }
    }

    private fun onSetContent() {
        pv?.let {
            GlobalScope.launch(Dispatchers.Main) {
                it.setImageBitmap(content)
            }
        }
    }

    private fun onSetPageTextColor() {
        tvPage?.let {
            GlobalScope.launch(Dispatchers.Main) {
                it.setTextColor(pageTextColor)
            }
        }
    }
}