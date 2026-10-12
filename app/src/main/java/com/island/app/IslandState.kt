package com.island.app

import kotlinx.coroutines.flow.MutableStateFlow

data class MusicInfo(
    val title: String,
    val artist: String,
    val playing: Boolean,
    val pkg: String?,
)

data class ChargeInfo(
    val watts: Float,
    val percent: Int,
    val wireless: Boolean,
)

/** Single source of truth the overlay UI observes. */
object IslandState {
    val music = MutableStateFlow<MusicInfo?>(null)
    val recording = MutableStateFlow(false)
    val charging = MutableStateFlow<ChargeInfo?>(null)

    /** Bumped whenever notification access connects, so media sessions can be (re)attached. */
    val listenerTick = MutableStateFlow(0)
}
