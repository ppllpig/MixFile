package com.donut.mixfile.util

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.donut.mixfile.kv
import com.google.gson.reflect.TypeToken

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

fun cachedMutableOf(value: Parcelable, key: String) =
    constructCachedMutableValue(
        value,
        key,
        { kv.encode(key, it) },
        getter@{
            kv.decodeParcelable(key, value.javaClass)
        })

inline fun <reified T> cachedMutableOf(value: List<T>, key: String) =
    constructCachedMutableValue(
        value,
        key,
        { kv.encode(key, it.toJsonString()) },
        getter@{
            var result = listOf<T>()
            val type = object : TypeToken<List<T>>() {}.type
            catchError {
                val json: List<T> = GSON.fromJson(kv.decodeString(key), type)
                result = json
            }
            return@getter result
        }
    )

inline fun <reified T> cachedMutableOfSet(value: Set<T>, key: String) =
    constructCachedMutableValue(
        value,
        key,
        { kv.encode(key, it.toJsonString()) },
        getter@{
            var result = setOf<T>()
            val type = object : TypeToken<Set<T>>() {}.type
            catchError {
                val json: Set<T> = GSON.fromJson(kv.decodeString(key), type)
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
        writeCachedValue(value)
    }
}