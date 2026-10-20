package org.openedx.core.instance

/**
 * Which instance a request belongs to, as an OkHttp tag.
 *
 * Requests built through the app's API client are tagged with the selected instance when they
 * start. A caller that must stay on one site across suspensions (sign-in, sign-out, push
 * registration) passes the tag itself through a Retrofit `@Tag` parameter: the request then goes
 * to that site's host with that site's token, whichever site is selected by the time it is sent.
 * A `null` key means no instance: the app-level host and the legacy session.
 */
data class RequestInstance(val instanceKey: String?)
