package com.android.launcher3.nexus.bottombar.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

fun getLocalizedResources(desiredLocale: Locale, context: Context): Resources {
    var conf: Configuration = context.resources.configuration
    conf = Configuration(conf)
    conf.setLocale(desiredLocale)
    val localizedContext = context.createConfigurationContext(conf)
    return localizedContext.resources
}