package io.github.dot166.nightstand

import android.content.res.Configuration
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import io.github.dot166.jlib.app.CoreActivity
import io.github.dot166.jlib.app.jLibPreferenceStore
import io.github.dot166.jlib.compose.JLibAppTheme
import io.github.dot166.jlib.compose.JLibScreen
import io.github.dot166.jlib.compose.conditional
import io.github.dot166.jlib.compose.preference.ScrollableScreenColumn
import io.github.dot166.jlib.compose.preference.SegmentedListColumn

class PreferenceActivity : CoreActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalJetPrefDatastoreUi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val jLibPrefs by jLibPreferenceStore
        val prefs by nightStandPreferenceStore
        setContent {
            JLibAppTheme {
                Column(
                    modifier = Modifier
                        //.statusBarsPadding()
                        .navigationBarsPadding()
                        .conditional(LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            displayCutoutPadding()
                        }
                        .imePadding(),
                ) {
                    JLibScreen(
                        title = stringResource(R.string.app_name),
                        navigationIconVisible = true,
                    ) { (contentPadding, translucent) ->
                        ScrollableScreenColumn(contentPadding, translucent) {
                            SegmentedListColumn {
                                val count = 4
                                SwitchPreference(
                                    pref = jLibPrefs.theme.translucent,
                                    shapes = ListItemDefaults.segmentedShapes(0, count),
                                    icon = Icons.Default.Opacity,
                                    title = "Translucency",
                                )
                                ListPreference(
                                    listPref = prefs.clock.screensaverClockColor,
                                    shapes = ListItemDefaults.segmentedShapes(1, count),
                                    title = stringResource(R.string.clock_color_title),
                                    entries = ClockColour.toListPrefEntries(),
                                )
                                SwitchPreference(
                                    pref = prefs.clock.screensaverBoldText,
                                    shapes = ListItemDefaults.segmentedShapes(2, count),
                                    title = stringResource(R.string.bold_text_title),
                                )
                                SwitchPreference(
                                    pref = prefs.clock.screensaverShowAmPm,
                                    shapes = ListItemDefaults.segmentedShapes(3, count),
                                    title = stringResource(R.string.show_ampm_title),
                                    enabledIf = { !DateFormat.is24HourFormat(LocalContext.current) },
                                )
                            }
                            SegmentedListColumn(title = stringResource(R.string.night_mode_title)) {
                                val count = 4
                                SwitchPreference(
                                    pref = prefs.nightMode.screensaverNightMode,
                                    shapes = ListItemDefaults.segmentedShapes(0, count),
                                    title = stringResource(R.string.night_mode_enabled_title),
                                    summary = stringResource(R.string.night_mode_summary),
                                )
                                SwitchPreference(
                                    pref = prefs.nightMode.screensaverNightModeDnd,
                                    shapes = ListItemDefaults.segmentedShapes(1, count),
                                    title = stringResource(R.string.night_mode_dnd_title),
                                    summary = stringResource(R.string.night_mode_dnd_summary),
                                    enabledIf = { prefs.nightMode.screensaverNightMode.asFlow().collectAsState().value },
                                )
                                ListPreference(
                                    listPref = prefs.nightMode.screensaverClockNightModeColor,
                                    shapes = ListItemDefaults.segmentedShapes(2, count),
                                    title = stringResource(R.string.clock_color_title),
                                    entries = ClockColour.toListPrefEntries(),
                                    enabledIf = { prefs.nightMode.screensaverNightMode.asFlow().collectAsState().value },
                                )
                                DialogSliderPreference(
                                    pref = prefs.nightMode.screensaverNightModeBrightness,
                                    shapes = ListItemDefaults.segmentedShapes(3, count),
                                    title = stringResource(R.string.night_mode_brightness_text_title),
                                    min = 0,
                                    max = 100,
                                    stepIncrement = 1,
                                    enabledIf = { prefs.nightMode.screensaverNightMode.asFlow().collectAsState().value },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
