package io.github.dot166.nightstand

import android.content.Context
import android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationManager private constructor() {

    private val scope = MainScope()
    private val notificationsMap = mutableMapOf<String, StatusBarNotification>()

    val notifications: StateFlow<List<StatusBarNotification>>
        field = MutableStateFlow(emptyList<StatusBarNotification>())

    fun onNotificationPosted(sbn: StatusBarNotification) {
        notificationsMap[sbn.key] = sbn
        onChange()
    }

    fun onNotificationRemoved(sbn: StatusBarNotification) {
        notificationsMap.remove(sbn.key)
        onChange()
    }

    fun onNotificationFullRefresh() {
        scope.launch(Dispatchers.IO) {
            val tmpMap = runCatching {
                NightStandNotificationListener.getInstanceIfConnected()?.activeNotifications?.associateBy { it.key }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                notificationsMap.clear()
                if (tmpMap != null) {
                    notificationsMap.putAll(tmpMap)
                }
                onChange()
            }
        }
    }

    private fun onChange() {
        notifications.value = notificationsMap.values.toList()
    }

    companion object {
        @JvmField
        var INSTANCE: NotificationManager? = null

        @JvmStatic
        fun getInstance(): NotificationManager {
            if (INSTANCE != null) {
                return INSTANCE!!
            } else {
                INSTANCE = NotificationManager()
                return INSTANCE!!
            }
        }
    }
}

private const val PERM_SUBSTITUTE_APP_NAME = "android.permission.SUBSTITUTE_NOTIFICATION_APP_NAME"
private const val EXTRA_SUBSTITUTE_APP_NAME = "android.substName"

fun StatusBarNotification.getAppName(context: Context): CharSequence {
    val subName = notification.extras.getString(EXTRA_SUBSTITUTE_APP_NAME)
    if (subName != null) {
        if (context.checkPackagePermission(packageName, PERM_SUBSTITUTE_APP_NAME)) {
            return subName
        }
    }
    return context.getAppName(packageName)
}

infix fun Int.hasFlag(flag: Int): Boolean {
    return (this and flag) == flag
}

fun Context.checkPackagePermission(packageName: String, permissionName: String): Boolean {
    try {
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        info.requestedPermissions?.forEachIndexed { index, s ->
            if (s == permissionName) {
                return info.requestedPermissionsFlags?.get(index)?.hasFlag(REQUESTED_PERMISSION_GRANTED)!!
            }
        }
    } catch (_: PackageManager.NameNotFoundException) {
    }
    return false
}

fun Context.getAppName(name: String): CharSequence {
    try {
        return packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(name, PackageManager.GET_META_DATA),
        )
    } catch (_: PackageManager.NameNotFoundException) {
    }
    return name
}