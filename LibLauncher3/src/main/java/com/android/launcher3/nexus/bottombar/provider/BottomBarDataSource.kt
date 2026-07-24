package com.android.launcher3.nexus.bottombar.provider

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import java.util.Locale
import androidx.core.content.edit
import com.android.launcher3.nexus.bottombar.LocaleUtils
import com.android.launcher3.nexus.bottombar.util.getLocalizedResources
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile
    private var currentTargets: List<SmartspaceTarget> = emptyList()
    protected abstract fun internalTargets(locale: Locale): Flow<List<SmartspaceTarget>>
    open val disabledTargets: List<SmartspaceTarget> = emptyList()
    var locale: Locale = Locale.getDefault()
    init {
        scope.launch {
            internalTargets(locale).collect { targets ->
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
    final override fun getTargets(localeString: String): List<SmartspaceTarget> {
        locale = LocaleUtils.toLocale(localeString)
        return currentTargets
    }
    final override fun getEnabled() = sharedPreferences.getBoolean(enabledPreferenceKey, isAvailable)
    final override fun setEnabled(bool: Boolean) = sharedPreferences.edit { putBoolean(enabledPreferenceKey, bool) }
    final override fun getName(localeString: String): String {
        locale = LocaleUtils.toLocale(localeString)
        return getLocalizedResources(locale, context).getString(providerName)
    }
    final override fun isAvailableFunction() = isAvailable
    final override fun getDisabledTargetsFunction() = disabledTargets
    override fun requiresSetup() = false
    override fun startSetup() {}
    override fun forceRefresh() {}
    companion object {
        const val ACTION_BOTTOM_BAR_TARGETS_UPDATED =
            "com.android.launcher3.nexus.bottombar.TARGETS_UPDATED"
    }
}