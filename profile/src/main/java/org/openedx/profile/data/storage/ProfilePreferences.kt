package org.openedx.profile.data.storage

import org.openedx.profile.data.model.Account

interface ProfilePreferences {
    var profile: Account?
}
