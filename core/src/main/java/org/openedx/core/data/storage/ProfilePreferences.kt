package org.openedx.core.data.storage

import org.openedx.core.domain.model.Account

interface ProfilePreferences {
    var profile: Account?
}