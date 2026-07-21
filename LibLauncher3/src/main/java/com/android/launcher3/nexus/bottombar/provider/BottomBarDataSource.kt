package com.android.launcher3.nexus.bottombar.provider

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import java.util.Locale
import androidx.core.content.edit
import com.android.launcher3.nexus.bottombar.LocaleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BottomBarDataSource(
    val context: Context,
    val providerName: Int,
    val enabledPreferenceKey: String,
    val serviceComponentName: ComponentName,
) : IBottomBarProvider.Stub() {
    open val isAvailable: Boolean = true
    private val sharedPreferences = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    private var currentTargets: List<SmartspaceTarget> = emptyList()
    protected abstract val internalTargets: Flow<List<SmartspaceTarget>>
    open val disabledTargets: List<SmartspaceTarget> = emptyList()
    init {
        scope.launch {
            internalTargets.collect { targets ->
                currentTargets = targets
                context.sendBroadcast(
                    Intent(ACTION_BOTTOM_BAR_TARGETS_UPDATED).apply {
                        putExtra("targetProviderCN", serviceComponentName)
                        setPackage("com.android.launcher3")
                    }
                )
            }
        }
    }
    final override fun getTargets(): List<SmartspaceTarget> = currentTargets
    final override fun getEnabled() = sharedPreferences.getBoolean(enabledPreferenceKey, isAvailable)
    final override fun setEnabled(bool: Boolean) = sharedPreferences.edit { putBoolean(enabledPreferenceKey, bool) }
    final override fun getName(localeString: String) =
        getLocalizedResources(LocaleUtils.toLocale(localeString)).getString(providerName)
    final override fun isAvailable() = isAvailable
    final override fun getDisabledTargets() = disabledTargets
    override fun requiresSetup() = false
    override fun startSetup() {}
    private fun getLocalizedResources(desiredLocale: Locale): Resources {
        var conf: Configuration = context.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(desiredLocale)
        val localizedContext = context.createConfigurationContext(conf)
        return localizedContext.resources
    }
    companion object {
        const val ACTION_BOTTOM_BAR_TARGETS_UPDATED =
            "com.android.launcher3.nexus.bottombar.TARGETS_UPDATED"
    }
}