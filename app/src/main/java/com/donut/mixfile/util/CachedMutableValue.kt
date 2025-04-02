package com.donut.mixfile.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.alibaba.fastjson2.into
import com.alibaba.fastjson2.toJSONString
import com.donut.mixfile.appScope
import com.donut.mixfile.kv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun <T> constructCachedMutableValue(
    value: T,
    key: String,
    setVal: (value: T) -> Unit,
    getVal: () -> T,
) =
    object : CachedMutableValue<T>(value, key) {
        override fun readCachedValue(): T {
            return getVal()
        }

        override fun writeCachedValue(value: T) {
            setVal(value)
        }
    }


fun cachedMutableOf(value: String, key: String) =
    constructCachedMutableValue(
        value,
        key,
        { kv.encode(key, it) },
        { kv.decodeString(key, value)!! })

fun cachedMutableOf(value: Boolean, key: String) =
    constructCachedMutableValue(value, key, { kv.encode(key, it) }, { kv.decodeBool(key, value) })

fun cachedMutableOf(value: Long, key: String) =
    constructCachedMutableValue(value, key, { kv.encode(key, it) }, { kv.decodeLong(key, value) })

fun cachedMutableOf(value: Set<String>, key: String) =
    constructCachedMutableValue(
        value,
        key,
        { kv.encode(key, it) },
        { kv.decodeStringSet(key, value)!! },
    )


inline fun <reified T, reified C : Iterable<T>> cachedMutableOf(value: C, key: String) =
    constructCachedMutableValue(
        value,
        key,
        {
            kv.encode(key, it.toJSONString())
        },
        getter@{
            var result = value
            catchError {
                val json: C = kv.decodeString(key).into()
                result = json
            }
            return@getter result
        }
    )


abstract class CachedMutableValue<T>(
    value: T,
    private val key: String,
) {
    var value by mutableStateOf(value)
    private var saveJob: Job? = null
    private var loaded = false
    abstract fun readCachedValue(): T

    abstract fun writeCachedValue(value: T)

    operator fun getValue(thisRef: Any?, property: Any?): T {
        if (!loaded) {
            value = readCachedValue()
        }
        loaded = true
        return value
    }

    operator fun setValue(thisRef: Any?, property: Any?, value: T) {
        this.value = value
        synchronized(key) {
            saveJob?.cancel()
            saveJob = appScope.launch(Dispatchers.IO) {
                catchError {
                    writeCachedValue(value)
                }
            }
        }
    }
}