package io.github.dot166.nightstand

import android.content.Context
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spa.framework.common.createSettingsPage
import io.github.dot166.jlib.app.JLibSpaEnvironment

class NightStandSpaEnvironment(context: Context): JLibSpaEnvironment(context) {
    override val pageProviderRepository = lazy {
        SettingsPageProviderRepository(
            allPageProviders =
                listOf(
                    HomePageProvider,
                ),
            rootPages = listOf(HomePageProvider.createSettingsPage()),
        )
    }
}
