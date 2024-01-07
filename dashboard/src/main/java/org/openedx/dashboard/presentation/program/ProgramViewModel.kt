package org.openedx.dashboard.presentation.program

import androidx.fragment.app.FragmentManager
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.dashboard.presentation.dashboard.DashboardRouter

class ProgramViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val router: DashboardRouter,
) : BaseViewModel() {

    val uriScheme: String get() = config.getUriScheme()

    val programConfig get() = config.getProgramConfig()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    fun infoCardClicked(fragmentManager: FragmentManager, pathId: String) {
        if (pathId.isNotEmpty()) {
            router.navigateToProgramInfo(
                fm = fragmentManager,
                pathId = pathId
            )
        }
    }
}
