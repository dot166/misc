package io.github.dot166.nexus

import android.os.Bundle
import com.android.settingslib.preference.PreferenceFragment
import io.github.dot166.jlib.app.jConfigActivity


class PreferenceActivity: jConfigActivity() {
    override fun preferenceFragment(): PreferenceFragment {
        return NexusPreferenceFragment()
    }

    class NexusPreferenceFragment : PreferenceFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            setPreferencesFromResource(R.xml.config, rootKey)
        }
    }
}