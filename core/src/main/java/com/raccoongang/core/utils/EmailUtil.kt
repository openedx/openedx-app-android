package com.raccoongang.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.raccoongang.core.R

object EmailUtil {

    fun showFeedbackScreen(
        context: Context,
        subject: String,
        appVersion: String
    ) {
        val NEW_LINE = "\n"
        val to = context.getString(R.string.feedback_email_address)
        val body = StringBuilder()
        with(body) {
            append("${context.getString(R.string.core_android_os_version)} ${Build.VERSION.RELEASE}")
            append(NEW_LINE)
            append("${context.getString(R.string.core_app_version)} $appVersion")
            append(NEW_LINE)
            append("${context.getString(R.string.core_android_device_model)} ${Build.MODEL}")
            append(NEW_LINE)
            append(NEW_LINE)
            append(context.getString(R.string.core_insert_feedback))
        }
        sendEmailIntent(context, to, subject, body.toString())
    }

    fun sendEmailIntent(
        context: Context?,
        to: String,
        subject: String,
        email: String
    ) {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, email)
        emailIntent.type = "plain/text"
        try {
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.let {
                // add flag to make sure this call works from non-activity context
                val targetIntent = Intent.createChooser(
                    emailIntent,
                    it.getString(R.string.core_email_chooser_header)
                )
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.startActivity(targetIntent)
            }
        } catch (ex: ActivityNotFoundException) {
            //There is no activity which can perform the intended share Intent
            context?.let {
                Toast.makeText(
                    it, it.getString(R.string.core_email_client_not_present),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}