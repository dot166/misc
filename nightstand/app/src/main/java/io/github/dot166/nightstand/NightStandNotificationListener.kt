package io.github.dot166.nightstand

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NightStandNotificationListener : NotificationListenerService() {

    private var mNotificationManager: NotificationManager? = null

    init {
        sNotificationListenerInstance = this
    }

    override fun onCreate() {
        super.onCreate()
        mNotificationManager = NotificationManager.getInstance()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "onListenerConnected")
        mIsConnected = true
        onNotificationFullRefresh()
    }

    private fun onNotificationFullRefresh() {
        mNotificationManager!!.onNotificationFullRefresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "onListenerDisconnected")
        mIsConnected = false
        onNotificationFullRefresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) {
            mNotificationManager!!.onNotificationPosted(sbn)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null) {
            mNotificationManager!!.onNotificationRemoved(sbn)
        }
    }

    companion object {
        const val TAG: String = "NightStandNotificationListener"

        private var sNotificationListenerInstance: NightStandNotificationListener? = null

        private var mIsConnected = false

        fun getInstanceIfConnected(): NightStandNotificationListener? {
            return if (mIsConnected) sNotificationListenerInstance else null
        }
    }
}
