package org.openedx.core.system.notifier

sealed class AppUpgradeEventUIState {
    object UpgradeRequiredScreen : AppUpgradeEventUIState()
    object UpgradeRecommendedDialog : AppUpgradeEventUIState()
    object UpgradeRecommendedBox : AppUpgradeEventUIState()
}
