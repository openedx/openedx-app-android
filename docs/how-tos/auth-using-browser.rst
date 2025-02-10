How to use Browser-based Login and Registration
===============================================

Introduction
------------

If your Open edX instance is set up with a custom authentication system that requires logging in
via the browser, you can use the ``BROWSER_LOGIN`` and ``BROWSER_REGISTRATION`` flags to redirect
login and registration to the browser.

The ``BROWSER_LOGIN`` flag is used to redirect login to the browser. In this case clicking on the
login button will open the authorization flow in an Android custom browser tab and redirect back to
the application.

The ``BROWSER_REGISTRATION`` flag is used to redirect registration to the browser. In this case
clicking on the registration button will open the registration page in a regular browser tab. Once
registered, the user will as of writing this document **not** be automatically redirected to the
application.

Usage
-----

In order to use the ``BROWSER_LOGIN`` feature, you need to set up an OAuth2 provider via
``<LMS>/admin/oauth2_provider/application/`` that has a redirect URL with the following format

    ``<application id>://oauth2Callback``

Here application ID is the ID for the Android application and defaults to ``"org.openedx.app"``. This
URI scheme is handled by the application and will be used by the app to get the OAuth2 token for
using the APIs.

Note that normally the Django OAuth Toolkit doesn't allow custom schemes like the above as redirect
URIs, so you will need to explicitly allow the by adding this URI scheme to
``ALLOWED_REDIRECT_URI_SCHEMES`` in the Django OAuth Toolkit settings in ``OAUTH2_PROVIDER``. You
can add the following line to your django settings python file:

.. code-block:: python

    OAUTH2_PROVIDER["ALLOWED_REDIRECT_URI_SCHEMES"] = ["https", "org.openedx.app"]

Replace ``"org.openedx.app"`` with the correct id for your application. You must list all allowed
schemes here, including ``"https"`` and ``"http"``.

The authentication will then redirect to the browser in a custom tab that redirects back to the app.

..note::

    If a user logs out from the application, they might still be logged in, in the browser.