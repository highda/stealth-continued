package com.cosmos.unreddit.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Reports content hidden because "Show NSFW content" is off, so the user can be told about it
 * instead of getting a shorter list (or none) without explanation.
 */
object NsfwNotifier {

    private val _hidden = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val hidden: SharedFlow<Unit> = _hidden.asSharedFlow()

    /** Keeps [item] unless it is NSFW and NSFW content is off; reports what it drops. */
    fun keep(showNsfw: Boolean, isNsfw: Boolean): Boolean {
        if (showNsfw || !isNsfw) return true
        _hidden.tryEmit(Unit)
        return false
    }
}
