package org.openedx.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import org.openedx.core.R

object EmailUtil {

    fun showFeedbackScreen(
        context: Context,
        feedbackEmailAddress: String,
        subject: String = context.getString(R.string.core_email_subject),
        feedback: String = "",
        appVersion: String
    ) {
        val NEW_LINE = "\n"
        val body = StringBuilder()
        with(body) {
            append(feedback)
            append(NEW_LINE)
            append(NEW_LINE)
            append("${context.getString(R.string.core_android_os_version)} ${Build.VERSION.RELEASE}")
            append(NEW_LINE)
            append("${context.getString(R.string.core_app_version)} $appVersion")
            append(NEW_LINE)
            append("${context.getString(R.string.core_android_device_model)} ${Build.MODEL}")
        }
        sendEmailIntent(context, feedbackEmailAddress, subject, body.toString())
    }

    fun sendEmailIntent(
        context: Context?,
        to: String,
        subject: String,
        email: String
    ) {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, email)
        }
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
        } catch (e: ActivityNotFoundException) {
            // There is no activity which can perform the intended share Intent
            e.printStackTrace()
            context?.let {
                Toast.makeText(
                    it,
                    it.getString(R.string.core_email_client_not_present),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
