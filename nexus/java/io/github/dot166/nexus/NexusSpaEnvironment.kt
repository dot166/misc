package io.github.dot166.nexus

import android.content.Context
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spa.framework.common.createSettingsPage
import io.github.dot166.jlib.app.DefaultHomePageProvider
import io.github.dot166.jlib.app.JLibSpaEnvironment
import io.github.dot166.nexus.setup.Page1
import io.github.dot166.nexus.setup.SetupWizardProvider

class NexusSpaEnvironment(context: Context) : JLibSpaEnvironment(context) {
    override val pageProviderRepository = lazy {
        SettingsPageProviderRepository(
            allPageProviders =
                listOf(
                    SetupWizardProvider,
                    Page1,
                    HomePageProvider,
                ),
            rootPages = listOf(SetupWizardProvider.createSettingsPage(), HomePageProvider.createSettingsPage()),
        )
    }
}