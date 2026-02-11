/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2023-2025 The LineageOS Project
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

package io.github.dot166.nightstand;

import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.preference.PreferenceFragment;
import com.android.settingslib.widget.SliderPreference;

import io.github.dot166.jlib.app.jConfigActivity;

public final class SettingsActivity extends jConfigActivity {

    public static final String KEY_CLOCK_COLOR = "screensaver_clock_color";
    public static final String KEY_NIGHT_MODE = "screensaver_night_mode";
    public static final String KEY_NIGHT_MODE_COLOR = "screensaver_clock_night_mode_color";
    public static final String KEY_NIGHT_MODE_DND = "screensaver_clock_night_mode_dnd";
    public static final String KEY_NIGHT_MODE_BRIGHTNESS =
            "screensaver_clock_night_mode_brightness";
    public static final String KEY_SHOW_AMPM = "screensaver_show_ampm";
    public static final String KEY_BOLD_TEXT = "screensaver_bold_text";

    @NonNull
    @Override
    public PreferenceFragment preferenceFragment() {
        return new PrefsFragment();
    }


    public static class PrefsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setStorageDeviceProtected();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.screensaver_settings);
        }

        @Override
        public void onResume() {
            super.onResume();
            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_NIGHT_MODE_COLOR:
                case KEY_CLOCK_COLOR:
                    final ListPreference clockColorPref = (ListPreference) pref;
                    final int clockColorindex = clockColorPref.findIndexOfValue((String) newValue);
                    clockColorPref.setSummary(clockColorPref.getEntries()[clockColorindex]);
                    break;
                case KEY_NIGHT_MODE_BRIGHTNESS:
                    final SliderPreference clockBrightness = (SliderPreference) pref;
                    final String progress = getResources().getString(
                            R.string.clock_brightness_percentage, String.valueOf(newValue));
                    clockBrightness.setSummary(progress);
                    break;
            }
            return true;
        }

        private void refresh() {
            final ListPreference clockColorPref = findPreference(KEY_CLOCK_COLOR);
            final ListPreference nightModeColorPref = findPreference(KEY_NIGHT_MODE_COLOR);
            final SwitchPreferenceCompat nightModePref = findPreference(KEY_NIGHT_MODE);
            final SwitchPreferenceCompat nightModeDndPref = findPreference(KEY_NIGHT_MODE_DND);
            final SwitchPreferenceCompat showAmPmPref = findPreference(KEY_SHOW_AMPM);
            final SwitchPreferenceCompat boldTextPref = findPreference(KEY_BOLD_TEXT);
            final SliderPreference nightModeBrightness = findPreference(KEY_NIGHT_MODE_BRIGHTNESS);
            if (clockColorPref != null) {
                final int indexColor = clockColorPref.findIndexOfValue(Utils.getScreensaverClockColor());
                clockColorPref.setValueIndex(indexColor);
                clockColorPref.setSummary(clockColorPref.getEntries()[indexColor]);
                clockColorPref.setOnPreferenceChangeListener(this);
            }
            if (nightModeColorPref != null) {
                final int indexColor = nightModeColorPref.findIndexOfValue(Utils.getScreensaverClockNightModeColor());
                nightModeColorPref.setValueIndex(indexColor);
                nightModeColorPref.setSummary(nightModeColorPref.getEntries()[indexColor]);
                nightModeColorPref.setOnPreferenceChangeListener(this);
            }
            if (nightModePref != null) {
                nightModePref.setChecked(Utils.getScreensaverNightModeOn());
            }
            if (nightModeDndPref != null) {
                nightModeDndPref.setChecked(Utils.getScreensaverNightModeDndOn());
            }
            if (showAmPmPref != null) {
                showAmPmPref.setChecked(Utils.getScreensaverShowAmPmOn());
                showAmPmPref.setEnabled(!DateFormat.is24HourFormat(getContext()));
            }
            if (boldTextPref != null) {
                boldTextPref.setChecked(Utils.getScreensaverBoldTextOn());
            }
            if (nightModeBrightness != null) {
                final int percentage = Utils.getScreensaverNightModeBrightness();
                nightModeBrightness.setValue(percentage);
                final String progress = getResources().getString(
                        R.string.clock_brightness_percentage, String.valueOf(percentage));
                nightModeBrightness.setSummary(progress);
                nightModeBrightness.setOnPreferenceChangeListener(this);
                nightModeBrightness.setUpdatesContinuously(true);
            }
        }
    }
}
