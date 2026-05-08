package io.github.dot166.nexus

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.google.android.gsa.overlay.controllers.OverlaysController
import com.google.android.gsa.overlay.controllers.OverlayController as GsaOverlayController
import android.app.NotificationChannel as AndroidNotificationChannel

class MinusOneService: LifecycleService() {

    private lateinit var overlaysController: OverlaysController

    override fun onCreate() {
        super.onCreate()
        startForeground(NotificationId.MINUS_ONE_SERVICE.ordinal, createNotification(this))
        overlaysController = OverlayController()
    }

    override fun onDestroy() {
        overlaysController.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return overlaysController.onBind(intent, null)
    }

    override fun onUnbind(intent: Intent): Boolean {
        overlaysController.onUnbind(intent)
        return false
    }

    private inner class OverlayController: ConfigurationOverlayController(this) {
        override fun getOverlay(uid: Int, context: Context): GsaOverlayController {
            return NexusOverlay(uid, context)
        }
    }

    private fun createNotification(context: Context): Notification {
        return context.createNotification(NotificationChannel.BACKGROUND_SERVICE) {
            val notificationIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannel.BACKGROUND_SERVICE.id)
            }
            it.setContentTitle(getString(R.string.notification_title_background_service))
            it.setContentText(getString(R.string.notification_content_background_service))
            it.setSmallIcon(R.drawable.outline_rss_feed_24)
            it.setOngoing(true)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.NATIVE_SERVICE.ordinal,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_title_background_service))
        }.also {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NotificationId.NATIVE_SERVICE.ordinal, it)
        }
    }
    fun Context.createNotification(
        channel: NotificationChannel,
        builder: (NotificationCompat.Builder) -> Unit
    ): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel =
            AndroidNotificationChannel(
                channel.id,
                getString(channel.titleRes),
                channel.importance
            ).apply {
                description = getString(channel.descRes)
                channel.options(this)
            }
        notificationManager.createNotificationChannel(notificationChannel)
        return NotificationCompat.Builder(this, channel.id).apply(builder).apply {
            val text = getContentText() ?: return@apply
            setStyle(NotificationCompat.BigTextStyle(this).bigText(text))
        }.build()
    }

    enum class NotificationChannel(
        val id: String,
        val importance: Int,
        val titleRes: Int,
        val descRes: Int,
        val options: AndroidNotificationChannel.() -> Unit = {}
    ) {
        BACKGROUND_SERVICE(
            "background_service",
            NotificationManager.IMPORTANCE_DEFAULT,
            R.string.notification_channel_background_service_title,
            R.string.notification_channel_background_service_subtitle,
            options = { setShowBadge(false) }
        )
    }

    enum class NotificationId {
        NATIVE_SERVICE,
        MINUS_ONE_SERVICE
    }

    fun NotificationCompat.Builder.getContentText(): CharSequence? {
        return this::class.java.getDeclaredField("mContentText").apply {
            isAccessible = true
        }.get(this) as CharSequence?
    }

}