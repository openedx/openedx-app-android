@file:Suppress("NOTHING_TO_INLINE")

package org.openedx.core.extension

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import com.google.gson.Gson
import java.io.Serializable

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    SDK_INT >= 33 -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
    SDK_INT >= 33 -> getParcelableArrayList(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}

inline fun <T> objectToString(value: T): String = Gson().toJson(value)

inline fun <reified T> stringToObject(value: String): T? {
    return try {
        Gson().fromJson(value, genericType<T>())
    } catch (e: Exception) {
        null
    }
}
