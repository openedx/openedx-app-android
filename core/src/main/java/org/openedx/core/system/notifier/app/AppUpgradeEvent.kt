package org.openedx.core.system.notifier.app

sealed class AppUpgradeEvent : AppEvent {
    data object UpgradeRequiredEvent : AppUpgradeEvent()
    class UpgradeRecommendedEvent(val newVersionName: String) : AppUpgradeEvent()
}
