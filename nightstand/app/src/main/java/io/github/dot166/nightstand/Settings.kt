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

import com.android.settingslib.datastore.SharedPreferencesStorage

/**
 * This class encapsulates the storage of application preferences in [SharedPreferencesStorage].
 */
internal class Settings(val prefs: SharedPreferencesStorage, val defaultColour: String) {
    /**
     * @return a value indicating what color the digital clock is in the screensaver in Night Mode
     */
    fun getClockNightModeColor(): String {
        return getClockColor(KEY_NIGHT_MODE_COLOR)
    }

    /**
     * @return a value indicating what color to use for the digital clock display on the screensaver
     */
    fun getScreensaverClockColor(): String {
        return getClockColor(KEY_CLOCK_COLOR)
    }

    /**
     * @return `true` if the screen saver should be dimmed for lower contrast at night
     */
    fun getScreensaverNightModeOn(): Boolean {
        return prefs.getBoolean(KEY_NIGHT_MODE) ?: false
    }

    /**
     * @return `true` if the screen saver should be dimmed for lower contrast at night
     */
    fun getScreensaverNightModeDndOn(): Boolean {
        return prefs.getBoolean(KEY_NIGHT_MODE_DND) ?: false
    }

    /**
     * @return `int` the screen saver brightness level at night
     */
    fun getScreensaverNightModeBrightness(): Int {
        return prefs.getInt(KEY_NIGHT_MODE_BRIGHTNESS) ?: 40
    }

    /**
     * @return `true` if the screen saver should show AM/PM in 12 hour mode
     */
    fun getScreensaverShowAmPmOn(): Boolean {
        return prefs.getBoolean(KEY_SHOW_AMPM) ?: true
    }

    /**
     * @return `true` if the screen saver should show the clock in bold
     */
    fun getScreensaverBoldTextOn(): Boolean {
        return prefs.getBoolean(KEY_BOLD_TEXT) ?: true
    }

    private fun getClockColor(key: String): String {
        val clockColor: String = prefs.getString(key) ?: defaultColour
        return clockColor.uppercase()
    }

    fun setClockNightModeColor(value: String) {
        setClockColor(KEY_NIGHT_MODE_COLOR, value)
    }

    fun setScreensaverClockColor(value: String) {
        setClockColor(KEY_CLOCK_COLOR, value)
    }

    fun setScreensaverNightModeOn(value: Boolean) {
        prefs.setBoolean(KEY_NIGHT_MODE, value)
    }

    fun setScreensaverNightModeDndOn(value: Boolean) {
        prefs.setBoolean(KEY_NIGHT_MODE_DND, value)
    }

    fun setScreensaverNightModeBrightness(value: Int) {
        prefs.setInt(KEY_NIGHT_MODE_BRIGHTNESS, value)
    }

    fun setScreensaverShowAmPmOn(value: Boolean) {
        prefs.setBoolean(KEY_SHOW_AMPM, value)
    }

    fun setScreensaverBoldTextOn(value: Boolean) {
        prefs.setBoolean(KEY_BOLD_TEXT, value)
    }

    private fun setClockColor(key: String, value: String) {
        prefs.setString(key, value)
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
