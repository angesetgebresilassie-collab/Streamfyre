package com.island.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import kotlin.math.abs

/** Detects any app recording audio (other apps' recordings are reported anonymised by Android). */
class RecordingWatcher(ctx: Context) {
    private val am = ctx.getSystemService(AudioManager::class.java)

    private val callback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
            IslandState.recording.value = configs.isNotEmpty()
        }
    }

    fun start() {
        am.registerAudioRecordingCallback(callback, Handler(Looper.getMainLooper()))
        IslandState.recording.value = am.activeRecordingConfigurations.isNotEmpty()
    }

    fun stop() {
        am.unregisterAudioRecordingCallback(callback)
        IslandState.recording.value = false
    }
}

/** Tracks the active media session (title, artist, artwork). Needs notification access. */
class MediaWatcher(private val ctx: Context) {
    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(ctx, IslandNotificationListener::class.java)
    private var controllers: List<MediaController> = emptyList()

    // Artwork cache so repeated metadata callbacks don't re-scale/re-decode.
    private var lastSrc: Bitmap? = null
    private var lastUri: String? = null
    private var lastArt: Bitmap? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list -> attach(list ?: emptyList()) }

    fun start() {
        stop()
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, component)
            attach(msm.getActiveSessions(component))
        } catch (_: SecurityException) {
            // Notification access not granted yet; retried when the listener connects.
            IslandState.music.value = null
        }
    }

    fun stop() {
        try {
            msm.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: Exception) {
        }
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = emptyList()
    }

    private fun attach(list: List<MediaController>) {
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = list
        controllers.forEach { it.registerCallback(controllerCallback) }
        publish()
    }

    private fun publish() {
        val c = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()
        if (c == null) {
            IslandState.music.value = null
            return
        }
        val md = c.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
        if (title.isNullOrBlank()) {
            IslandState.music.value = null
            return
        }
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        IslandState.music.value = MusicInfo(
            title = title,
            artist = artist,
            playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            pkg = c.packageName,
            art = artworkFor(md),
        )
    }

    private fun artworkFor(md: MediaMetadata?): Bitmap? {
        if (md == null) return null

        val src = md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (src != null) {
            if (src === lastSrc) return lastArt
            lastSrc = src
            lastUri = null
            lastArt = scaled(src)
            return lastArt
        }

        val uri = md.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
        if (uri != null) {
            if (uri == lastUri) return lastArt
            lastUri = uri
            lastSrc = null
            lastArt = loadLocalUri(uri)
            return lastArt
        }
        return null
    }

    private fun scaled(b: Bitmap): Bitmap {
        val max = 256
        val longest = maxOf(b.width, b.height)
        if (longest <= max) return b
        val s = max.toFloat() / longest
        return Bitmap.createScaledBitmap(
            b,
            (b.width * s).toInt().coerceAtLeast(1),
            (b.height * s).toInt().coerceAtLeast(1),
            true,
        )
    }

    /** Only content:// and file:// art is loaded here (no network access from this process). */
    private fun loadLocalUri(uri: String): Bitmap? {
        return try {
            val u = Uri.parse(uri)
            if (u.scheme != "content" && u.scheme != "file") {
                null
            } else {
                ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it) }?.let { scaled(it) }
            }
        } catch (_: Exception) {
            null
        }
    }
}

/** Live charging power: |current| x voltage, smoothed. Normalises the unit quirks across devices. */
class ChargeMonitor(private val ctx: Context) {
    private val bm = ctx.getSystemService(BatteryManager::class.java)
    private var ema = 0f
    private var primed = false

    fun sample(): ChargeInfo? {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        if (status != BatteryManager.BATTERY_STATUS_CHARGING) {
            primed = false
            return null
        }
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val percent = if (level >= 0) level * 100 / scale else 0

        val rawV = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).toFloat()
        val volts = when {
            rawV > 100_000f -> rawV / 1_000_000f // microvolts
            rawV > 100f -> rawV / 1_000f // millivolts (normal)
            else -> rawV // already volts
        }

        val rawA = abs(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)).toFloat()
        val amps = if (rawA > 20_000f) rawA / 1_000_000f else rawA / 1_000f // uA vs mA

        val watts = amps * volts
        ema = if (primed) ema * 0.7f + watts * 0.3f else watts
        primed = true

        return ChargeInfo(
            watts = ema,
            percent = percent,
            wireless = plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS,
        )
    }
}
