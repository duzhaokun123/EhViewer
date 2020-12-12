package com.duzhaokun123.galleryview

import android.graphics.Bitmap
import com.hippo.unifile.UniFile

abstract class GalleryProvider {
    companion object {
        // With dot
        val SUPPORT_IMAGE_EXTENSIONS = arrayOf(
                ".jpg",  // Joint Photographic Experts Group
                ".jpeg",
                ".png",  // Portable Network Graphics
                ".gif")
    }

    enum class State {
        WAIT, ERROR, READY
    }

    enum class PageState {
        WAIT, LOADING, READY, ERROR
    }

    data class PageInfo(
            val page: Int,
            val state: PageState,
            val error: String? = null,
            val content: Bitmap? = null,
            val progress: Int = 0
    )

    class CannotGetException(message: String? = null, cause: Exception? = null) : Exception(message, cause)

    /**
     * K: index
     */
    private val pageInfos = mutableMapOf<Int, PageInfo>()

    private val listeners = mutableSetOf<Listener>()

    var state = State.WAIT

    open var startPage = -1

    abstract val error: String?

    abstract val size: Int

    abstract fun start()

    abstract fun stop()

    abstract fun request(index: Int)

    open fun forceRequest(index: Int) = request(index)

    abstract fun cancelRequest(index: Int)

    open fun reload(index: Int) = forceRequest(index)

    open fun getImageFilename(index: Int): String = index.toString()

    open fun getImageFilenameWithExtension(index: Int) = index.toString()

    /**
     * @return saved?
     */
    abstract fun save(index: Int, file: UniFile): Boolean

    /**
     * @return saved file, null not saved
     */
    abstract fun save(index: Int, dir: UniFile, fileName: String): UniFile?

    open fun putStartPage(index: Int) {
        startPage = index
    }

    @Throws(CannotGetException::class)
    fun get(index: Int): Bitmap {
        val a = pageInfos[index] ?: throw CannotGetException("$index not ready yet")
        when (a.state) {
            PageState.WAIT, PageState.LOADING -> throw CannotGetException("$index not ready yet")
            PageState.READY -> return a.content
                    ?: throw CannotGetException("$index ready, but content is null")
            PageState.ERROR -> throw CannotGetException("$index error: ${a.error}")
        }
    }

    fun stateOf(index: Int) = pageInfos[index]?.state ?: PageState.WAIT

    fun errorOf(index: Int) = pageInfos[index]?.error

    fun progressOf(index: Int) = pageInfos[index]?.progress ?: 0

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    @JvmOverloads
    protected fun notifyStateChange(state: State, error: String? = null) {
        this.state = state
        listeners.forEach { it.onStateChange(state, error) }
    }

    protected fun notifyPageWait(index: Int) {
        pageInfos[index] = PageInfo(index + 1, PageState.WAIT)
        listeners.forEach { it.onPageStateChange(index, PageState.WAIT) }
    }

    protected fun notifyPageFailed(index: Int, error: String? = null) {
        pageInfos[index] = PageInfo(index + 1, PageState.ERROR, error = error)
        listeners.forEach {
            it.onPageError(index, error)
            it.onPageStateChange(index, PageState.ERROR)
        }
    }

    protected fun notifyPagePercent(index: Int, percent: Float) {
        val stateChanged = stateOf(index) != PageState.LOADING
        val progress = (100 * percent).toInt()
        pageInfos[index] = PageInfo(index + 1, PageState.LOADING, progress = progress)
        listeners.forEach {
            if (stateChanged)
                it.onPageStateChange(index, PageState.LOADING)
            it.onPageProgressChange(index, progress)
        }
    }

    @JvmOverloads
    protected fun notifyPageSucceed(index: Int, content: Bitmap? = null) {
        pageInfos[index] = PageInfo(index + 1, PageState.READY, content = content)
        listeners.forEach {
            it.onPageReady(index, content)
            it.onPageStateChange(index, PageState.READY)
        }
    }

    interface Listener {
        fun onStateChange(state: State, error: String? = null)

        fun onPageStateChange(index: Int, state: PageState)

        fun onPageReady(index: Int, bitmap: Bitmap?)

        fun onPageError(index: Int, error: String? = null)

        fun onPageProgressChange(index: Int, progress: Int)
    }
}