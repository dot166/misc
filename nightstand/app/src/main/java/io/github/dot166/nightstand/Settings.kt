/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.dot166.nightstand

import android.content.Context
import android.content.SharedPreferences

/**
 * This class encapsulates the storage of application preferences in [SharedPreferences].
 */
internal class Settings {
    /**
     * @return a value indicating what color the digital clock is in the screensaver in Night Mode
     */
    fun getClockNightModeColor(context: Context, prefs: SharedPreferences): String {
        return getClockColor(context, prefs, SettingsActivity.KEY_NIGHT_MODE_COLOR)
    }

    /**
     * @return a value indicating what color to use for the digital clock display on the screensaver
     */
    fun getScreensaverClockColor(context: Context, prefs: SharedPreferences): String {
        return getClockColor(context, prefs, SettingsActivity.KEY_CLOCK_COLOR)
    }

    /**
     * @return `true` if the screen saver should be dimmed for lower contrast at night
     */
    fun getScreensaverNightModeOn(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(SettingsActivity.KEY_NIGHT_MODE, false)
    }

    /**
     * @return `true` if the screen saver should be dimmed for lower contrast at night
     */
    fun getScreensaverNightModeDndOn(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(SettingsActivity.KEY_NIGHT_MODE_DND, false)
    }

    /**
     * @return `int` the screen saver brightness level at night
     */
    fun getScreensaverNightModeBrightness(prefs: SharedPreferences): Int {
        return prefs.getInt(SettingsActivity.KEY_NIGHT_MODE_BRIGHTNESS, 40)
    }

    /**
     * @return `true` if the screen saver should show AM/PM in 12 hour mode
     */
    fun getScreensaverShowAmPmOn(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(SettingsActivity.KEY_SHOW_AMPM, true)
    }

    /**
     * @return `true` if the screen saver should show the clock in bold
     */
    fun getScreensaverBoldTextOn(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(SettingsActivity.KEY_BOLD_TEXT, false)
    }

    private fun getClockColor(context: Context, prefs: SharedPreferences, key: String?): String {
        val defaultColor = context.getString(R.string.default_clock_color)
        val clockColor: String = prefs.getString(key, defaultColor)!!
        // Use hardcoded locale to perform toUpperCase, because in some languages toUpperCase adds
        // accent to character, which breaks the enum conversion.
        return clockColor.uppercase()
    }
}
