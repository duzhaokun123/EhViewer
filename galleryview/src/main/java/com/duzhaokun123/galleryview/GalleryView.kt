package com.duzhaokun123.galleryview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GalleryView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), GalleryProvider.Listener {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_gallary_view, this)
    }

    companion object {
        const val LAYOUT_MODE_L2R = 0
        const val LAYOUT_MODE_R2L = 1
        const val LAYOUT_MODE_T2B = 2

        const val SCALE_ORIGIN = 0
        const val SCALE_FIT_WIDTH = 1
        const val SCALE_FIT_HEIGHT = 2
        const val SCALE_FIT = 3
        const val SCALE_FIXED = 4

        @GalleryLayoutMode
        @JvmStatic
        fun sanitizeLayoutMode(layoutMode: Int): Int {
            return if (layoutMode != LAYOUT_MODE_L2R
                    && layoutMode != LAYOUT_MODE_R2L
                    && layoutMode != LAYOUT_MODE_T2B) {
                LAYOUT_MODE_L2R
            } else {
                layoutMode
            }
        }

        @ScaleMode
        @JvmStatic
        fun sanitizeScaleMode(scaleMode: Int): Int {
            return if (scaleMode != SCALE_ORIGIN
                    && scaleMode != SCALE_FIT_WIDTH
                    && scaleMode != SCALE_FIT_HEIGHT
                    && scaleMode != SCALE_FIT
                    && scaleMode != SCALE_FIXED) {
                SCALE_FIT
            } else {
                scaleMode
            }
        }
    }

    @IntDef(LAYOUT_MODE_R2L, LAYOUT_MODE_L2R, LAYOUT_MODE_T2B)
    @Retention(AnnotationRetention.SOURCE)
    annotation class GalleryLayoutMode

    @IntDef(SCALE_ORIGIN, SCALE_FIT_WIDTH, SCALE_FIT_HEIGHT, SCALE_FIT, SCALE_FIXED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ScaleMode

    private var needSetDoubleItemLater = true

    private val pb = findViewById<ProgressBar>(R.id.pb)!!
    private val tvError = findViewById<TextView>(R.id.tv_error)!!
    private val vp2 = findViewById<ViewPager2>(R.id.vp2)!!.apply {
        if (isInEditMode.not()) {
            adapter = GalleryPageAdapter(context)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    listener?.onUpdateCurrentIndex(position)
                }
            })
        }
    }

    private val vp2DefaultLM = (vp2.getChildAt(0) as RecyclerView).layoutManager

    @GalleryLayoutMode
    var galleryLayoutMode = LAYOUT_MODE_R2L
        set(value) {
            if (field != value) {
                field = value
                onSetGalleryLayoutMode()
            }
        }

    @ScaleMode
    var scaleMode = SCALE_FIT
        set(value) {
            if (field != value) {
                field = value
                (vp2.adapter as GalleryPageAdapter).scaleMode = value
            }
        }

    var defaultErrorString = "error"
    var pageTextColor = Color.GRAY

    var listener: Listener? = null
        set(value) {
            field = value
            (vp2.adapter as GalleryPageAdapter).galleryViewListener = value
        }

    var provider: GalleryProvider? = null
        set(value) {
            field?.removeListener(this)
            field?.stop()
            value?.addListener(this)
            value?.start()
            field = value

            value?.let { onStateChange(it.state) }
            (vp2.adapter as GalleryPageAdapter).provider = provider
        }

    var doubleItems = false
        set(value) {
            field = value
            if (needSetDoubleItemLater)
                GlobalScope.launch(Dispatchers.Main) {
                    delay(80)
                    onSetDoubleItem()
                }
            else
                onSetDoubleItem()
        }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        needSetDoubleItemLater = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        onSetGalleryLayoutMode()
    }

    override fun onStateChange(state: GalleryProvider.State, error: String?) {
        GlobalScope.launch(Dispatchers.Main) {
            when (state) {
                GalleryProvider.State.WAIT -> {
                    pb.visibility = VISIBLE
                    tvError.visibility = GONE
                    vp2.visibility = GONE
                }
                GalleryProvider.State.ERROR -> {
                    pb.visibility = GONE
                    tvError.visibility = VISIBLE
                    vp2.visibility = GONE
                    tvError.text = error
                }
                GalleryProvider.State.READY -> {
                    pb.visibility = GONE
                    tvError.visibility = GONE
                    vp2.visibility = VISIBLE
                }
            }
            listener?.onStateChange(state)
        }
    }

    fun pageLeft() {
        when (galleryLayoutMode) {
            LAYOUT_MODE_R2L -> vp2.setCurrentItem(vp2.currentItem + 1, true)
            LAYOUT_MODE_L2R -> vp2.setCurrentItem(vp2.currentItem - 1, true)
        }
    }

    fun pageRight() {
        when (galleryLayoutMode) {
            LAYOUT_MODE_R2L -> vp2.setCurrentItem(vp2.currentItem - 1, true)
            LAYOUT_MODE_L2R -> vp2.setCurrentItem(vp2.currentItem + 1, true)
        }
    }

    fun setCurrentPage(page: Int) {
        vp2.setCurrentItem(page, true)
    }

    private fun onSetGalleryLayoutMode() {
        (vp2.getChildAt(0) as RecyclerView).layoutManager = vp2DefaultLM
        when (galleryLayoutMode) {
            LAYOUT_MODE_L2R -> {
                vp2.orientation = ViewPager2.ORIENTATION_HORIZONTAL
                ViewCompat.setLayoutDirection(vp2, ViewCompat.LAYOUT_DIRECTION_LTR)
            }
            LAYOUT_MODE_R2L -> {
                vp2.orientation = ViewPager2.ORIENTATION_HORIZONTAL
                ViewCompat.setLayoutDirection(vp2, ViewCompat.LAYOUT_DIRECTION_RTL)
            }
            LAYOUT_MODE_T2B -> vp2.orientation = ViewPager2.ORIENTATION_VERTICAL
        }
    }

    private fun onSetDoubleItem() {
        vp2.apply {
            offscreenPageLimit = 1
            (getChildAt(0) as RecyclerView).apply {
                if (doubleItems) {
                    clipToPadding = false
                    val padding = vp2.width / 2
                    when (galleryLayoutMode) {
                        LAYOUT_MODE_L2R -> setPadding(0, 0, padding, 0)
                        LAYOUT_MODE_R2L -> setPadding(padding, 0, 0, 0)
                        LAYOUT_MODE_T2B -> {
                            setPadding(0, 0, 0, 0)
                            layoutManager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
                        }
                    }
                } else {
                    setPadding(0, 0, 0, 0)
                    clipToPadding = true
                }
            }
        }
    }

    interface Listener {
        fun onStateChange(state: GalleryProvider.State)

        fun onUpdateCurrentIndex(index: Int)
        fun onTapErrorText(index: Int)
        fun onLongPressPage(index: Int)

        fun onTapSliderArea()
        fun onTapMenuArea()
    }
}