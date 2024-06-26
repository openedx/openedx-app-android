package org.openedx.core.system.notifier.app

sealed class AppUpgradeEvent: AppEvent {
    object UpgradeRequiredEvent : AppUpgradeEvent()
    class UpgradeRecommendedEvent(val newVersionName: String) : AppUpgradeEvent()
}
