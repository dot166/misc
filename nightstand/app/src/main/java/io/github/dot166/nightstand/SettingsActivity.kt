package io.github.dot166.nightstand

import android.os.Bundle
import android.text.format.DateFormat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.widget.SliderPreference
import io.github.dot166.jlib.app.jConfigActivity

class SettingsActivity : jConfigActivity() {
    override fun preferenceFragment(): PreferenceFragment {
        return PrefsFragment()
    }


    class PrefsFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            preferenceManager.setStorageDeviceProtected()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.screensaver_settings)
        }

        override fun onResume() {
            super.onResume()
            refresh()
        }

        override fun onPreferenceChange(pref: Preference, newValue: Any?): Boolean {
            when (pref.key) {
                KEY_NIGHT_MODE_COLOR, KEY_CLOCK_COLOR -> {
                    val clockColorPref = pref as ListPreference
                    val clockColorindex = clockColorPref.findIndexOfValue(newValue as String?)
                    clockColorPref.setSummary(clockColorPref.entries[clockColorindex])
                }

                KEY_NIGHT_MODE_BRIGHTNESS -> {
                    val clockBrightness = pref as SliderPreference
                    val progress = resources.getString(
                        R.string.clock_brightness_percentage, newValue.toString()
                    )
                    clockBrightness.setSummary(progress)
                }
            }
            return true
        }

        private fun refresh() {
            val clockColorPref = findPreference<ListPreference?>(KEY_CLOCK_COLOR)
            val nightModeColorPref = findPreference<ListPreference?>(KEY_NIGHT_MODE_COLOR)
            val nightModePref = findPreference<SwitchPreferenceCompat?>(KEY_NIGHT_MODE)
            val nightModeDndPref = findPreference<SwitchPreferenceCompat?>(KEY_NIGHT_MODE_DND)
            val showAmPmPref = findPreference<SwitchPreferenceCompat?>(KEY_SHOW_AMPM)
            val boldTextPref = findPreference<SwitchPreferenceCompat?>(KEY_BOLD_TEXT)
            val nightModeBrightness = findPreference<SliderPreference?>(KEY_NIGHT_MODE_BRIGHTNESS)
            if (clockColorPref != null) {
                val indexColor = clockColorPref.findIndexOfValue(Utils.screensaverClockColor)
                clockColorPref.setValueIndex(indexColor)
                clockColorPref.setSummary(clockColorPref.entries[indexColor])
                clockColorPref.onPreferenceChangeListener = this
            }
            if (nightModeColorPref != null) {
                val indexColor =
                    nightModeColorPref.findIndexOfValue(Utils.screensaverClockNightModeColor)
                nightModeColorPref.setValueIndex(indexColor)
                nightModeColorPref.setSummary(nightModeColorPref.entries[indexColor])
                nightModeColorPref.onPreferenceChangeListener = this
            }
            nightModePref?.setChecked(Utils.screensaverNightModeOn)
            nightModeDndPref?.setChecked(Utils.screensaverNightModeDndOn)
            if (showAmPmPref != null) {
                showAmPmPref.setChecked(Utils.screensaverShowAmPmOn)
                showAmPmPref.isEnabled = !DateFormat.is24HourFormat(context)
            }
            boldTextPref?.setChecked(Utils.screensaverBoldTextOn)
            if (nightModeBrightness != null) {
                val percentage = Utils.screensaverNightModeBrightness
                nightModeBrightness.value = percentage
                val progress = resources.getString(
                    R.string.clock_brightness_percentage, percentage.toString()
                )
                nightModeBrightness.setSummary(progress)
                nightModeBrightness.onPreferenceChangeListener = this
                nightModeBrightness.updatesContinuously = true
            }
        }
    }

    companion object {
        const val KEY_CLOCK_COLOR: String = "screensaver_clock_color"
        const val KEY_NIGHT_MODE: String = "screensaver_night_mode"
        const val KEY_NIGHT_MODE_COLOR: String = "screensaver_clock_night_mode_color"
        const val KEY_NIGHT_MODE_DND: String = "screensaver_clock_night_mode_dnd"
        const val KEY_NIGHT_MODE_BRIGHTNESS: String = "screensaver_clock_night_mode_brightness"
        const val KEY_SHOW_AMPM: String = "screensaver_show_ampm"
        const val KEY_BOLD_TEXT: String = "screensaver_bold_text"
    }
}
