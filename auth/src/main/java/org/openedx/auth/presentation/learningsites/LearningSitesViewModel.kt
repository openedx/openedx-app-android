package org.openedx.auth.presentation.learningsites

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.auth.presentation.AuthRouter
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceSessionManager
import org.openedx.core.instance.InstanceStore
import java.util.Locale

data class LearningSitesUIState(
    /** The selected site, when it has a session. */
    val currentSite: Instance? = null,
    /** Other sites the learner is signed in to. */
    val otherSignedInSites: List<Instance> = emptyList(),
    /** Sites without a session, filtered by [searchQuery]. */
    val addableSites: List<Instance> = emptyList(),
    val searchQuery: String = "",
)

/**
 * The Learning Sites screen: the first screen of a multi-site build, and where a signed-in
 * learner switches sites or signs out of one ([isSwitching]).
 */
class LearningSitesViewModel(
    val isSwitching: Boolean,
    private val instanceStore: InstanceStore,
    private val preferences: CorePreferences,
    private val sessionManager: InstanceSessionManager,
    private val router: AuthRouter,
    private val config: Config,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearningSitesUIState())
    val uiState: StateFlow<LearningSitesUIState> = _uiState.asStateFlow()

    private var catalog: List<Instance> = emptyList()
    private var signedInKeys: Set<String> = emptySet()

    /** Set while a selection navigates away, so a second tap can't start another one. */
    private var isNavigating = false

    init {
        refresh()
    }

    /** Re-reads sessions: signing in or out elsewhere changes which list a site belongs to. */
    fun refresh() {
        isNavigating = false
        viewModelScope.launch {
            val sites = instanceStore.instancesConfig.instances
                .sortedBy { it.name.lowercase(Locale.getDefault()) }
            signedInKeys = withContext(dispatcher) {
                sites.filter { preferences.hasSession(it.key) }.map { it.key }.toSet()
            }
            catalog = sites
            publish()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        publish()
    }

    /**
     * A site with a session opens straight to its dashboard; any other site opens its sign-in.
     * When switching, the previous site's screens are closed before the switch, so nothing
     * still running on them can act on the new site.
     */
    fun select(fragmentManager: FragmentManager, instance: Instance) {
        if (isNavigating) return
        isNavigating = true
        val isSignedIn = instance.key in signedInKeys
        if (isSwitching) {
            router.clearBackStack(fragmentManager)
            fragmentManager.executePendingTransactions()
        }
        sessionManager.switchActiveInstance(instance)
        when {
            isSignedIn -> router.navigateToMain(fragmentManager, null, null)
            else -> {
                if (isSwitching) router.navigateToLearningSites(fragmentManager, isSwitching = false)
                if (config.isPreLoginExperienceEnabled()) {
                    router.navigateToLogistration(fragmentManager, null)
                } else {
                    router.navigateToSignIn(fragmentManager, null, null)
                }
            }
        }
    }

    /**
     * Signing out of the selected site, while switching, ends that session everywhere and
     * starts over on a fresh Learning Sites screen. Any other site just leaves the list of
     * signed-in sites.
     */
    fun logOut(fragmentManager: FragmentManager, instance: Instance) {
        if (isNavigating) return
        viewModelScope.launch {
            val wasCurrent = instance.key == _uiState.value.currentSite?.key
            sessionManager.logout(instance)
            if (wasCurrent && isSwitching) {
                isNavigating = true
                router.navigateToLearningSites(fragmentManager, isSwitching = false)
            } else {
                refresh()
            }
        }
    }

    private fun publish() {
        val current = instanceStore.currentInstance?.takeIf { it.key in signedInKeys }
        val query = _uiState.value.searchQuery.trim()
        _uiState.update { state ->
            state.copy(
                currentSite = current,
                otherSignedInSites = catalog.filter { it.key in signedInKeys && it.key != current?.key },
                addableSites = catalog
                    .filter { it.key !in signedInKeys }
                    .filter { query.isEmpty() || it.matches(query) },
            )
        }
    }

    private fun Instance.matches(query: String): Boolean {
        return instanceName.contains(query, ignoreCase = true) ||
            name.contains(query, ignoreCase = true) ||
            apiHostUrl.contains(query, ignoreCase = true)
    }
}
