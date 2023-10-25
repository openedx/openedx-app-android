package org.openedx.core.system.notifier

sealed class AppUpgradeEvent {
    object UpgradeRequiredEvent : AppUpgradeEvent()
    class UpgradeRecommendedEvent(val newVersionName: String) : AppUpgradeEvent()
}
