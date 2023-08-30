package org.openedx.profile.data.storage

import org.openedx.profile.domain.model.Account

interface ProfilePreferences {
    var profile: Account?
}