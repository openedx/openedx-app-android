package org.openedx.discussion.data.model.request

import com.google.gson.annotations.SerializedName

data class ReportBody(
    @SerializedName("abuse_flagged")
    val abuseFlagged: Boolean
)
