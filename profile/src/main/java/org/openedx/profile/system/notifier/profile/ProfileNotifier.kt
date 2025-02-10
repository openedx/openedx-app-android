package org.openedx.profile.system.notifier.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.openedx.profile.system.notifier.account.AccountDeactivated
import org.openedx.profile.system.notifier.account.AccountUpdated

class ProfileNotifier {

    private val channel = MutableSharedFlow<ProfileEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<ProfileEvent> = channel.asSharedFlow()

    suspend fun send(event: AccountUpdated) = channel.emit(event)
    suspend fun send(event: AccountDeactivated) = channel.emit(event)
}
