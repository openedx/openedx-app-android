package org.openedx.core.system.notifier

sealed class AppUpgradeEvent {
    object UpgradeRequiredEvent : AppUpgradeEvent()
    object UpgradeRecommendedEvent : AppUpgradeEvent()
}
