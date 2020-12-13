package com.duzhaokun123.galleryview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryPageAdapter(private val context: Context) : RecyclerView.Adapter<GalleryPageAdapter.GalleryPageViewHolder>() {
    companion object {
        const val INVALID_INDEX = -1
    }

    var galleryViewListener: GalleryView.Listener? = null

    var provider: GalleryProvider? = null

    var pageTextColor = Color.GRAY

    var scaleMode = GalleryView.SCALE_FIT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryPageViewHolder {
        return GalleryPageViewHolder(LayoutInflater.from(context).inflate(R.layout.view_gallery_page, parent, false))
    }

    override fun onBindViewHolder(holder: GalleryPageViewHolder, position: Int) {
        holder.page = position + 1
        provider?.addListener(holder.providerListener)
    }

    override fun onViewRecycled(holder: GalleryPageViewHolder) {
        provider?.removeListener(holder.providerListener)
    }

    override fun onViewAttachedToWindow(holder: GalleryPageViewHolder) {
        provider?.let {
            holder.state = it.stateOf(holder.index)
            holder.error = it.errorOf(holder.index)
            holder.progress = it.progressOf(holder.index)
            if (it.stateOf(holder.index) == GalleryProvider.PageState.WAIT) {
                it.request(holder.index)
            }
            try {
                holder.content = it.get(holder.index)
            } catch (e: GalleryProvider.CannotGetException) {
            }
        }
    }

    override fun getItemCount() = provider?.size ?: 0

    inner class GalleryPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPage = itemView.findViewById<TextView>(R.id.tv_page)!!.apply {
            setTextColor(pageTextColor)
        }
        private val pb = itemView.findViewById<ProgressBar>(R.id.pb)!!
        private val tvError = itemView.findViewById<TextView>(R.id.tv_error)!!.apply {
            setOnClickListener { galleryViewListener?.onTapErrorText(index) }
        }
        private val pv = itemView.findViewById<PhotoView>(R.id.pv)!!.apply {
            setOnLongClickListener {
                galleryViewListener?.onLongPressPage(index)
                true
            }
            scaleType = scaleMode.toImageViewScaleType()
        }

        var page = 0
            set(value) {
                field = value
                onSetPage()
            }
        val index
            get() = page - 1

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

        val providerListener = object : GalleryProvider.Listener {
            override fun onPageStateChange(index: Int, state: GalleryProvider.PageState) {
                if (this@GalleryPageViewHolder.index == index)
                    this@GalleryPageViewHolder.state = state
            }

            override fun onPageProgressChange(index: Int, progress: Int) {
                if (this@GalleryPageViewHolder.index == index)
                    this@GalleryPageViewHolder.progress = progress
            }

            override fun onPageError(index: Int, error: String?) {
                if (this@GalleryPageViewHolder.index == index)
                    this@GalleryPageViewHolder.error = error
            }

            override fun onPageReady(index: Int, bitmap: Bitmap?) {
                if (this@GalleryPageViewHolder.index == index)
                    this@GalleryPageViewHolder.content = bitmap
            }
        }

        private fun onSetPage() {
            tvPage.let {
                GlobalScope.launch(Dispatchers.Main) {
                    it.text = page.toString()
                }
            }
        }

        private fun onSetState() {
            GlobalScope.launch(Dispatchers.Main) {
                when (state) {
                    GalleryProvider.PageState.WAIT -> {
                        tvPage.visibility = View.VISIBLE
                        pb.visibility = View.VISIBLE
                        pb.isIndeterminate = true
                        tvError.visibility = View.GONE
                        pv.visibility = View.GONE
                    }
                    GalleryProvider.PageState.LOADING -> {
                        tvPage.visibility = View.VISIBLE
                        pb.visibility = View.VISIBLE
                        pb.isIndeterminate = false
                        tvError.visibility = View.GONE
                        pv.visibility = View.GONE
                    }
                    GalleryProvider.PageState.READY -> {
                        tvPage.visibility = View.GONE
                        pb.visibility = View.GONE
                        tvError.visibility = View.GONE
                        pv.visibility = View.VISIBLE
                    }
                    GalleryProvider.PageState.ERROR -> {
                        tvPage.visibility = View.VISIBLE
                        pb.visibility = View.GONE
                        tvError.visibility = View.VISIBLE
                        pv.visibility = View.GONE
                    }
                }
            }
        }

        private fun onSetError() {
            tvError.let {
                GlobalScope.launch(Dispatchers.Main) {
                    it.text = error
                }
            }
        }

        private fun onSetProgress() {
            pb.let {
                GlobalScope.launch(Dispatchers.Main) {
                    it.setProgress(progress, true)
                }
            }
        }

        private fun onSetContent() {
            pv.let {
                GlobalScope.launch(Dispatchers.Main) {
                    it.setImageBitmap(content)
                }
            }
        }
    }

}