package com.raccoongang.core.extension

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> genericType() = object: TypeToken<T>() {}.type

inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)

