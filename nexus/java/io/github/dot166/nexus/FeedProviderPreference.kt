package io.github.dot166.nexus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.AttributeSet
import androidx.preference.ListPreference


class FeedProviderPreference @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0): ListPreference(ctx, attrs, defStyleAttr, defStyleRes) {
    init {
        val intent = Intent("io.github.dot166.nexus.WINDOW_OVERLAY_PROVIDER")
        val activities: MutableList<ResolveInfo> = ctx.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val labels = mutableListOf(ctx.getString(R.string.default_stub))
        val components = mutableListOf("io.github.dot166.nexus/io.github.dot166.nexus.DefaultStub")

        for (info in activities) {
            if (info.activityInfo.packageName == ctx.packageName) {
                continue // stub is already there
            }
            val label = info.loadLabel(ctx.packageManager).toString()
            labels.add(label)
            val componentName = info.activityInfo.name
            val packageName = info.activityInfo.packageName
            components.add("$packageName/$componentName")
        }
        entries = labels.toTypedArray()
        entryValues = components.toTypedArray()
    }
}