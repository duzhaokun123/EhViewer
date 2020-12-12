package com.duzhaokun123.galleryview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), GalleryProvider.Listener {

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_gallary_view, this)
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
    }

    @IntDef(LAYOUT_MODE_R2L, LAYOUT_MODE_L2R, LAYOUT_MODE_T2B)
    @Retention(AnnotationRetention.SOURCE)
    annotation class GalleryLayoutMode

    @IntDef(SCALE_ORIGIN, SCALE_FIT_WIDTH, SCALE_FIT_HEIGHT, SCALE_FIT, SCALE_FIXED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ScaleMode

    private var pb: ProgressBar? = null
    private var tvError: TextView? = null
    private var vp2: ViewPager2? = null

    private val pageFragments = mutableSetOf<GalleryPageFragment>()

    @GalleryLayoutMode
    var galleryLayoutMode = LAYOUT_MODE_R2L

    @ScaleMode
    var scaleMode = SCALE_FIT

    var defaultErrorString = "error"
    var pageTextColor = Color.GRAY

    var listener: Listener? = null
        set(value) {
            field = value
            pageFragments.forEach { it.galleryViewListener = value }
        }

    var provider: GalleryProvider? = null
        set(value) {
            field?.removeListener(this)
            field?.stop()
            value?.addListener(this)
            value?.start()
            field = value

            value?.let { onStateChange(it.state) }
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pb = findViewById(R.id.pb)
        tvError = findViewById(R.id.tv_error)
        vp2 = findViewById(R.id.vp2)

        if (isInEditMode.not()) {
            vp2!!.adapter = ViewPager2Adapter(context as FragmentActivity)
            when (galleryLayoutMode) {
                LAYOUT_MODE_L2R -> {
                    vp2!!.orientation = ViewPager2.ORIENTATION_HORIZONTAL
                    ViewCompat.setLayoutDirection(vp2!!, ViewCompat.LAYOUT_DIRECTION_LTR)
                }
                LAYOUT_MODE_R2L -> {
                    vp2!!.orientation = ViewPager2.ORIENTATION_HORIZONTAL
                    ViewCompat.setLayoutDirection(vp2!!, ViewCompat.LAYOUT_DIRECTION_RTL)
                }
                LAYOUT_MODE_T2B -> vp2!!.orientation = ViewPager2.ORIENTATION_VERTICAL
            }

            vp2!!.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    provider?.let {
                        if (it.stateOf(position) == GalleryProvider.PageState.WAIT) {
                            it.request(position)
                        }
                    }
                    listener?.onUpdateCurrentIndex(position)
                }
            })

            provider?.let { onStateChange(it.state) }
        }
    }

    override fun onStateChange(state: GalleryProvider.State, error: String?) {
        GlobalScope.launch(Dispatchers.Main) {
            when (state) {
                GalleryProvider.State.WAIT -> {
                    pb?.visibility = VISIBLE
                    tvError?.visibility = GONE
                    vp2?.visibility = GONE
                }
                GalleryProvider.State.ERROR -> {
                    pb?.visibility = GONE
                    tvError?.visibility = VISIBLE
                    vp2?.visibility = GONE
                    tvError?.text = error
                }
                GalleryProvider.State.READY -> {
                    pb?.visibility = GONE
                    tvError?.visibility = GONE
                    vp2?.visibility = VISIBLE
                }
            }
            listener?.onStateChange(state)
        }
    }

    override fun onPageStateChange(index: Int, state: GalleryProvider.PageState) {
        pageFragments.forEach {
            if (it.isUsing && it.page == index + 1) {
                it.state = state
                return
            }
        }
    }

    override fun onPageReady(index: Int, bitmap: Bitmap?) {
        pageFragments.forEach {
            if (it.isUsing && it.page == index + 1) {
                bitmap?.let { bitmap ->
                    it.content = bitmap
                }
                return
            }
        }
    }

    override fun onPageError(index: Int, error: String?) {
        pageFragments.forEach {
            if (it.isUsing && it.page == index + 1) {
                it.error = error
                return
            }
        }
    }

    override fun onPageProgressChange(index: Int, progress: Int) {
        pageFragments.forEach {
            if (it.isUsing && it.page == index + 1) {
                it.progress = progress
                return
            }
        }
    }

    fun pageLeft() {
        when (galleryLayoutMode) {
            LAYOUT_MODE_R2L -> vp2?.setCurrentItem(vp2!!.currentItem + 1, true)
            LAYOUT_MODE_L2R -> vp2?.setCurrentItem(vp2!!.currentItem - 1, true)
        }
    }

    fun pageRight() {
        when (galleryLayoutMode) {
            LAYOUT_MODE_R2L -> vp2?.setCurrentItem(vp2!!.currentItem - 1, true)
            LAYOUT_MODE_L2R -> vp2?.setCurrentItem(vp2!!.currentItem + 1, true)
        }
    }

    fun setCurrentPage(page: Int) {
        vp2?.setCurrentItem(page, true)
    }

    private fun onSetPageFragmentInfo(index: Int, pageFragment: GalleryPageFragment) {
        pageFragment.page = index + 1
        pageFragment.pageTextColor = pageTextColor
        try {
            pageFragment.content = provider?.get(index)
        } catch (e: GalleryProvider.CannotGetException) {
            pageFragment.content = null
        }
        pageFragment.error = provider?.errorOf(index) ?: defaultErrorString
        pageFragment.progress = provider?.progressOf(index) ?: 0
        pageFragment.state = provider?.stateOf(index) ?: GalleryProvider.PageState.WAIT
        pageFragment.galleryViewListener = listener
        pageFragment.onStopListener = { provider?.cancelRequest(index) }
    }

    inner class ViewPager2Adapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount() = provider?.size ?: 0

        override fun createFragment(position: Int): Fragment {
            pageFragments.forEach {
                if (it.isUsing.not()) {
                    onSetPageFragmentInfo(position, it)
                    return it
                }
            }
            return GalleryPageFragment(this@GalleryView).also {
                onSetPageFragmentInfo(position, it)
                pageFragments.add(it)
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