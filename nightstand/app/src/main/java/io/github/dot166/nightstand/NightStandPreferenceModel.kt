package io.github.dot166.nightstand

import android.R
import dev.patrickgold.jetpref.datastore.annotations.Preferences
import dev.patrickgold.jetpref.datastore.jetprefDataStoreOf
import dev.patrickgold.jetpref.datastore.model.PreferenceMigrationEntry
import dev.patrickgold.jetpref.datastore.model.PreferenceModel
import io.github.dot166.jlib.app.JLibPreferenceModel

val nightStandPreferenceStore = jetprefDataStoreOf(NightStandPreferenceModel::class)

@Preferences
abstract class NightStandPreferenceModel : PreferenceModel() {
    companion object {
        const val NAME = "NS-prefs"
    }

    val internal = Internal()
    inner class Internal {
    }
    val clock = Clock()
    inner class Clock {
        val screensaverClockColor = enum("screensaver_clock_color", ClockColour.PINK)
        val screensaverShowAmPm = boolean("screensaver_show_ampm", true)
        val screensaverBoldText = boolean("screensaver_bold_text", true)
    }
    val nightMode = NightMode()
    inner class NightMode {
        val screensaverNightMode = boolean("screensaver_night_mode", false)
        val screensaverNightModeDnd = boolean("screensaver_clock_night_mode_dnd", false)
        val screensaverClockNightModeColor = enum("screensaver_clock_night_mode_color", ClockColour.PINK)
        val screensaverNightModeBrightness = int("screensaver_clock_night_mode_brightness", 40)
    }

    override fun migrate(entry: PreferenceMigrationEntry): PreferenceMigrationEntry {
        return when (entry.key) {

            // Default: keep entry
            else -> entry.keepAsIs()
        }
    }
}
