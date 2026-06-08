package io.github.dot166.nexus

import android.content.Intent
import android.os.Bundle
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spa.framework.util.appendSpaParams
import io.github.dot166.jlib.app.jActivity


class PreferenceActivity: jActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, SpaEnvironmentFactory.instance.browseActivityClass)
        intent.appendSpaParams(destination = HomePageProvider.createSettingsPage().buildRoute())
        startActivity(intent)
        finish()
    }
}