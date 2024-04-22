package org.openedx.discovery.presentation

import androidx.fragment.app.Fragment

class DiscoveryNavigator(
    private val isDiscoveryTypeWebView: Boolean,
) {
    fun getDiscoveryFragment(): Fragment {
        return if (isDiscoveryTypeWebView) {
            WebViewDiscoveryFragment.newInstance()
        } else {
            NativeDiscoveryFragment.newInstance()
        }
    }
}
