package com.donut.mixfile.util

import com.donut.mixfile.cacheKv
import com.tencent.mmkv.MMKV

object CacheUtil {

    fun get(key: String): ByteArray? {
        return cacheKv.getBytes(key, null)
    }

    fun put(key: String, value: ByteArray, duration: Int = MMKV.ExpireInMinute) {
        cacheKv.putBytes(key, value, duration)
    }


}