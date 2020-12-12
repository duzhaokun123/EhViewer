package com.duzhaokun123.galleryview

import java.util.*
import kotlin.random.Random

object ObjectCache {
    private val map = WeakHashMap<String, Any?>()

    fun put(obj: Any?): String {
        val key = "${System.currentTimeMillis()}${Random.nextInt()}"
        map[key] = obj
        return key
    }

    fun get(key: String?): Any? {
        return map.remove(key)
    }
}