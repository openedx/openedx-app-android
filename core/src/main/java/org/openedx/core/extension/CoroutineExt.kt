package org.openedx.core.extension

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
inline fun <T> channelFlowWithAwait(
    @BuilderInference crossinline block: suspend ProducerScope<T>.() -> Unit
) = channelFlow {
    block(this)
    awaitClose()
}
