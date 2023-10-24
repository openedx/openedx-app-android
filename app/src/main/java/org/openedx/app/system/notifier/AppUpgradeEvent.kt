package org.openedx.app.system.notifier

sealed class AppUpgradeEvent: AppEvent {
    object UpgradeRequiredEvent : AppUpgradeEvent()
    object UpgradeRecommendedEvent : AppUpgradeEvent()
}
