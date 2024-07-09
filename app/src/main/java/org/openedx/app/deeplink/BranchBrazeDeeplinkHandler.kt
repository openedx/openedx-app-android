package org.openedx.app.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.braze.ui.BrazeDeeplinkHandler
import com.braze.ui.actions.UriAction
import org.openedx.app.AppActivity

internal class BranchBrazeDeeplinkHandler : BrazeDeeplinkHandler() {
    override fun gotoUri(context: Context, uriAction: UriAction) {
        val deeplink = uriAction.uri.toString()

        if (deeplink.contains("app.link")) {
            val intent = Intent(context, AppActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(deeplink)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("branch_force_new_session", true)
            }
            context.startActivity(intent)
        } else {
            super.gotoUri(context, uriAction)
        }
    }
}
