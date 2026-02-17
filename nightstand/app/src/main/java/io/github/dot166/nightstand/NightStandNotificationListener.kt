package io.github.dot166.nightstand

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NightStandNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // optional
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // optional
    }
}
