package io.github.dot166.nexus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.android.settingslib.datastore.SharedPreferencesStorage
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import io.github.dot166.jlib.app.DefaultSharedPrefsManager

class FeedProvider(val context: Context) {
    val prefs: SharedPreferencesStorage = DefaultSharedPrefsManager.getSharedPreferencesStorage(context)
    val pm: PackageManager = context.packageManager
    val providers: List<Pair<String, String>>
    init {
        val intent = Intent("io.github.dot166.nexus.WINDOW_OVERLAY_PROVIDER")
        val activities: MutableList<ResolveInfo> = pm.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val components = mutableListOf(Pair("io.github.dot166.nexus/io.github.dot166.nexus.DefaultStub", context.getString(R.string.default_stub)))

        for (info in activities) {
            if (info.activityInfo.packageName == context.packageName) {
                continue // stub is already there
            }
            val label = info.loadLabel(pm).toString()
            val componentName = info.activityInfo.name
            val packageName = info.activityInfo.packageName
            components.add(Pair("$packageName/$componentName", label))
        }
        providers = components
    }
    fun getSavedFeed(): String {
        if (!prefs.contains("feed_provider")) {
            prefs.setString("feed_provider", "io.github.dot166.nexus/io.github.dot166.nexus.DefaultStub")
            return "io.github.dot166.nexus/io.github.dot166.nexus.DefaultStub"
        }
        return prefs.getString("feed_provider")?: "io.github.dot166.nexus/io.github.dot166.nexus.DefaultStub"
    }
    fun setSavedFeed(provider: String) {
        prefs.setString("feed_provider", provider)
    }
    fun getProvidersAsListOptions(): MutableList<ListPreferenceOption> {
        val options = mutableListOf<ListPreferenceOption>()
        for (i in 0 until providers.size) {
            val provider = providers[i]
            options.add(ListPreferenceOption(i, provider.second, provider.first))
        }
        return options
    }
    fun getProvider(i: Int): String {
        if (i >= providers.size) {
            return "io.github.dot166.nexus/io.github.dot166.nexus.DefaultStub"
        }
        val provider = providers[i]
        return provider.first
    }
    fun getIndexOfProvider(s: String): Int {
        for (i in 0 until providers.size) {
            val provider = providers[i]
            if (provider.first == s) {
                return i
            }
        }
        return 0
    }
}