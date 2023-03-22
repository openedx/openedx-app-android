package com.raccoongang.core.domain.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class LanguageProficiency(
    @SerializedName("code")
    val code: String
) : Parcelable
