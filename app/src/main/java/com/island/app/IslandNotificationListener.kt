package com.island.app

import android.service.notification.NotificationListenerService

/**
 * Never reads notification content. Its only job is to hold the notification-access grant,
 * which Android requires before apps can read active media sessions.
 */
class IslandNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        IslandState.listenerTick.value = IslandState.listenerTick.value + 1
    }
}
